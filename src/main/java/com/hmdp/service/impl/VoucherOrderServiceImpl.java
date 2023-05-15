package com.hmdp.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
*
* @author : mj
* @since 2023/5/6 15:33
*/
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisWorker redisWorker;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

//    private class VoucherOrderHandler implements Runnable{
//
//        @Override
//        public void run() {
//            while (true){
//                try {
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("处理订单异常", e);
//                }
//            }
//        }
//    }

    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while (true){
                try {
                    // 获取消息队列中的订单
                    // XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream,orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );

                    // 判断消息是否获取成功
                    if (list == null || list.isEmpty()){
                        continue;
                    }
                    // 解析消息，下单
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // ack确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true){
                try {
                    // 获取消息队列中的订单
                    // XREADGROUP GROUP g1 c1 COUNT 1 STREAMS stream,orders 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );

                    // 判断消息是否获取成功
                    if (list == null || list.isEmpty()){
                        break;
                    }
                    // 解析消息，下单
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    // ack确认 SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pending-list异常", e);
                }
            }
        }
    }

    private IVoucherOrderService proxy;

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getUserId();

        // 创建锁对象
        RLock lock = redissonClient.getLock("lock:order" + userId);

        boolean isLock = lock.tryLock();

        if (!isLock){
            log.error("不允许重复下单");
            return;
        }

        try {
            proxy.createVoucher(voucherOrder);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 获取订单id
        Long orderId = redisWorker.nextId("order");
        // 执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        // 判断结果是否为0
        int r = result.intValue();
        if (r != 0){
            // 没有资格下单
            return Result.fail( r == 1 ? "库存不足" : "不能重复下单");
        }

//        阻塞队列代码
//        // 有下单资格，把下单资格保存到消息队列中
//        VoucherOrder voucherOrder = new VoucherOrder();
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherId);
//        orderTasks.add(voucherOrder);

        // 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 返回订单id
        return Result.success(orderId);
    }


//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 查询秒杀券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//
//        // 判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始!");
//        }
//
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束!");
//        }
//
//        // 判断库存是否充足
//        if (voucher.getStock() < 1){
//            return Result.fail("库存不足!");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//
//        // 创建锁对象
//        RLock lock = redissonClient.getLock("lock:order" + userId);
//
//        boolean isLock = lock.tryLock();
//
//        if (!isLock){
//            return Result.fail("不允许重复下单");
//        }
//
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucher(voucherId);
//        } finally {
//            lock.unlock();
//        }
//
////        synchronized (userId.toString().intern()){
////            // 获取代理对象
////            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
////            return proxy.createVoucher(voucherId);
////        }
//
//
//    }

    @Override
    @Transactional
    public void createVoucher(VoucherOrder voucherOrder) {
        // 一人一单
        long userId = voucherOrder.getId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0){
            log.error("用户已经购买过一次");
            return;
        }

        // 扣减库存
        boolean success = seckillVoucherService
                .update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0) // 乐观锁解决超卖
                .update();
        if (!success){
            log.error("库存不足");
            return;
        }

        save(voucherOrder);

    }
}

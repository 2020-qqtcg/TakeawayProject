package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author : mj
 * @since 2023/5/4 20:21
 */

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 写入redis并设置过期时间
     * @param key 键
     * @param value 值
     * @param time 时间
     * @param unit 单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 写入redis，设置逻辑过期时间，主要用于热点key的缓存击穿问题
     * @param key 键
     * @param value 值
     * @param time 时间
     * @param unit 单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        // 写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 解决缓存穿透
     * @param keyPrefix key的前缀
     * @param id 查询id
     * @param type 查询数据的类型
     * @param dbFallBack 数据库查询逻辑
     * @param time 时间
     * @param unit 单位
     * @return 对应数据
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 从redis查询
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            // 缓存中存在直接返回
            return JSONUtil.toBean(json, type);
        }

        // 如果是空值，说明redis里面缓存了空值，直接返回失败
        if (json != null){
            return null;
        }



        // 从数据库查询
        R r = dbFallBack.apply(id);

        if (r != null) {
            // 如果存在写入redis
            this.set(key, r, time, unit);
        }
        else {
            // 如果不存在redis写入空值，防止缓存穿透
            this.set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
        }

        return r;
    }

    /**
     * 利用互斥锁解决缓存击穿
     * @param keyPrefix key的前缀
     * @param id 查询id
     * @param type 查询数据的类型
     * @param dbFallBack 数据库查询逻辑
     * @param time 时间
     * @param unit 单位
     * @return 对应数据
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        while(true){
            // 从redis查询
            String json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)){
                // 缓存中存在直接返回
                return JSONUtil.toBean(json, type);
            }

            // 如果是空值，说明redis里面缓存了空值，直接返回失败
            if (json != null){
                return null;
            }

            // 实现缓存重建
            String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
            try {
                boolean isLock = tryLock(lockKey);
                if (!isLock){
                    Thread.sleep(50);
                    continue;
                }


                // 从数据库查询
                R r = dbFallBack.apply(id);

                if (r != null) {
                    // 如果存在写入redis
                    this.set(key, r, time, unit);
                }
                else {
                    // 如果不存在redis写入空值，防止缓存穿透
                    this.set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                }

                // 返回
                return r;

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                unLock(lockKey);
            }
        }

    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 利用逻辑过期解决缓存击穿
     * @param keyPrefix key的前缀
     * @param id 查询id
     * @param type 查询数据的类型
     * @param dbFallBack 数据库查询逻辑
     * @param time 时间
     * @param unit 单位
     * @return 对应数据
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 从redis查询
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)){
            // 未命中直接返回null
            return null;
        }

        // 命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            // 未过期，直接返回
            return r;
        }

        // 已过期，需要缓存重建
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock){
            // 成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = dbFallBack.apply(id);

                    // 写入redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e){
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }


        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}

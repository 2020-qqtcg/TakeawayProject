package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
* 
* @author : mj
* @since 2023/4/25 15:50
*/
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Shop queryById(Long id) {
//        // 缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(
//                RedisConstants.CACHE_SHOP_KEY,
//                id,
//                Shop.class,
//                this::getById,
//                RedisConstants.CACHE_SHOP_TTL,
//                TimeUnit.MINUTES);

        // 缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(
                RedisConstants.CACHE_SHOP_KEY,
                id,
                Shop.class,
                this::getById,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES);
        return shop;
    }





    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId() == null){
            return Result.fail("商品id为空");
        }

        // 更新数据库
        updateById(shop);

        // 删除缓存
        String key = RedisConstants.CACHE_SHOP_KEY + shop.getId();
        stringRedisTemplate.delete(key);
        return Result.success();
    }


}

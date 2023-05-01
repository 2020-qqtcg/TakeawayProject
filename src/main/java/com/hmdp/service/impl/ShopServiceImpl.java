package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    @Override
    public Shop queryById(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 从redis查询
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)){
            // 缓存中存在直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 如果是空值，说明redis里面缓存了空值，直接返回失败
        if (shopJson != null){
            return null;
        }

        // 从数据库查询
        Shop shop = getById(id);


        if (shop != null) {
            // 如果存在写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
        else {
            // 如果不存在redis写入空值，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
        }
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

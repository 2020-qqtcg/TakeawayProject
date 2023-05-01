package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
*
* @author : mj
* @since 2023/4/25 16:40
*/
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryShopTypeList() {
        String key = RedisConstants.CACHE_SHOP_KEY + "shop-type";
        // 查缓存
        List<String> cacheList = stringRedisTemplate.opsForList().range(key, 0, -1);
        stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
        if (cacheList != null && !cacheList.isEmpty()) {
            // 如果有就返回
            Stream<ShopType> stream = cacheList.stream().map((String s) -> JSONUtil.toBean(s, ShopType.class));
            return stream.collect(Collectors.toList());
        }

        // 没有从数据库中查
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 写入缓存中
        Stream<String> stream = shopTypes.stream().map(JSONUtil::toJsonStr);
        List<String> shopTypesStr = stream.collect(Collectors.toList());
        Collections.reverse(shopTypesStr);
        stringRedisTemplate.opsForList().leftPushAll(key, shopTypesStr);



        return shopTypes;
    }
}

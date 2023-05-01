package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
*
* @author : mj
* @since 2023/4/25 15:44
*/
public interface IShopService extends IService<Shop> {

    /**
     * 根据商品id查找商品信息
     * @param id 商品id
     * @return 商品信息,null表示没有
     */
    Shop queryById(Long id);

    /**
     * 更新商品
     * @param shop 商品
     * @return 更新结果
     */
     Result update(Shop shop);
}

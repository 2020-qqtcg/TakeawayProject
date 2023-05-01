package com.hmdp.service;

import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* 
* @author : mj
* @since 2023/4/25 16:40
*/
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询商品种类列表
     * @return 商品种类列表
     */
    List<ShopType> queryShopTypeList();
}

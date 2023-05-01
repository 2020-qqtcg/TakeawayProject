package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
*
* @author : mj
* @since 2023/4/25 16:20
*/
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {

        List<ShopType> typeList = typeService.queryShopTypeList();
//        List<ShopType> typeList = typeService.query().orderByAsc("sort").list();
        return Result.success(typeList);
    }
}

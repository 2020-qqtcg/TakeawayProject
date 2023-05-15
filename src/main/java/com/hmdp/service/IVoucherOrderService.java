package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
*
* @author : mj
* @since 2023/5/6 15:33
*/
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 优惠券秒杀
     * @param voucherId 优惠券id
     * @return 秒杀结果/成功返回订单id
     */
    Result seckillVoucher(Long voucherId);


    void createVoucher(VoucherOrder voucherOrder);
}

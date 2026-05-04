package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.PaymentOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付订单Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 payment_order 表的CRUD操作。
 * </p>
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderEntity> {
}

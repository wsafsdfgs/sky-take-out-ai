package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

/**
 * 批量插入订单详情列表
 * @param orderDetailList 订单详情列表，包含需要批量插入的所有订单详情信息
 */
    void insertBatch(List<OrderDetail> orderDetailList);
    /**
 * 根据订单id查询订单详情列表
 * @param orderId 订单id
 * @return 订单详情列表
 */
@Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}

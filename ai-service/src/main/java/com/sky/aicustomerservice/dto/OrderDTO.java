package com.sky.aicustomerservice.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单数据传输对象(OrderDTO)
 * 用于在系统各层之间传递订单相关信息
 */
@Data  // Lombok注解，自动生成getter、setter、toString等方法
public class OrderDTO {
    private Long id;                    // 订单ID
    private String number;             // 订单编号
    private Integer status;            // 订单状态
    private Long userId;               // 用户ID
    private String userName;           // 用户名
    private String phone;              // 联系电话
    private BigDecimal amount;         // 订单金额
    // 主项目返回格式是 "yyyy-MM-dd HH:mm"（如 2026-10-08 16:47），
    // 直接当字符串接收，避免 LocalDateTime 反序列化格式不匹配报错
    private String estimatedDeliveryTime;  // 预计送达时间
    private String address;            // 配送地址
    private String consignee;          // 收货人
}

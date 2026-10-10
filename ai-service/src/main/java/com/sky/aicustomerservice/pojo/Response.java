package com.sky.aicustomerservice.pojo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Response {
    private String orderNo;                 // 订单号
    private String orderStatus;             // 状态文字
    private BigDecimal amount;              // 实收金额
    private String consignee;               // 收货人
    private String estimatedDeliveryTime;   // 预计送达
    private String msg;                     // 查询失败提示

    public Response() {}

    // 失败提示用
    public Response(String msg) {
        this.msg = msg;
    }

    // 成功用
    public Response(String orderNo, String orderStatus, BigDecimal amount,
                    String consignee, String estimatedDeliveryTime) {
        this.orderNo = orderNo;
        this.orderStatus = orderStatus;
        this.amount = amount;
        this.consignee = consignee;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }
}

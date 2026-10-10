package com.sky.aicustomerservice.pojo;

import org.springframework.ai.tool.annotation.ToolParam;

public class Request {

    @ToolParam(description = "用户提供的订单编号")
    private String orderNo;

    // 无参构造，SpringAI反射实例化必须要有
    public Request() {
    }

    public Request(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}

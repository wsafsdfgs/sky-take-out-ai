package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.JwtUtil;
import com.sky.vo.OrderVO;
import com.sky.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/order")  // 路径保持不变！
@Tag(name = "内部订单API（给AI服务调用）")
@Slf4j
public class InnerOrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private JwtProperties jwtProperties;

    @GetMapping("/innerDetailByNumber/{number}")
    @Operation(summary = "内部查询订单详情（按订单号，含归属校验）")
    public ResponseEntity<Result<OrderVO>> innerDetailByNumber(@PathVariable("number") String number) {
        OrderVO orderVO = orderService.getByNumber(number);
        if (orderVO == null) {
            return ResponseEntity.status(403).body(Result.error("订单不存在"));
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (!currentUserId.equals(orderVO.getUserId())) {
            return ResponseEntity.status(403).body(Result.error("无权访问该订单"));
        }
        return ResponseEntity.ok(Result.success(orderVO));
    }
    @GetMapping("/innerMyList")
    public ResponseEntity<Result<PageResult>> innerMyList() {
        // 只查最近5条，状态限定为未完成（1待付款 2待接单 3已接单 4派送中）
        // null 表示不限状态，但我们只要未完成的，所以传 null 然后 pageSize=5
        PageResult pageResult = orderService.pageQuery4User(1, 3, null);
        return ResponseEntity.ok(Result.success(pageResult));
    }



}


package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Tag(name = "管理端订单接口")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;
    //订单搜索
    @GetMapping("/conditionSearch")
    @Operation(summary = "订单搜索")
    public Result<PageResult>  conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("查询订单，参数为:{}",ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }
    @GetMapping("/statistics")
    @Operation(summary = "订单统计")
    public Result<OrderStatisticsVO> statistics(){
        log.info("统计各个状态订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }
    //查询订单详情
    @GetMapping("/details/{id}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        log.info("用户查询订单详情，参数为:{}",id);
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }
    //接单（将订单状态修改为已接单）
    //请求头String,Body参数 id
    @PutMapping("/confirm")
    @Operation(summary = "接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单，参数为:{}",ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }
    //拒单
   /* 业务规则：
         - 商家拒单其实就是将订单状态修改为“已取消”
         - 只有订单处于“待接单”状态时可以执行拒单操作
         - 商家拒单时需要指定拒单原因
         - 商家拒单时，如果用户已经完成了支付，需要为用户退款*/
    @PutMapping("/rejection")
    @Operation(summary = "拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO)throws Exception{
        log.info("拒单，参数为:{}",ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }
    //商家取消订单
    @PutMapping("/cancel")
    @Operation(summary = "取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        orderService.Cancel(ordersCancelDTO);
        return Result.success();
    }
    //派送订单
    @PutMapping("/delivery/{id}")
    @Operation(summary = "派送订单")
    public Result delivery(@PathVariable("id") Long id){
        log.info("派送订单，参数为:{}",id);
        orderService.delivery(id);
        return Result.success();
    }
    //完成订单
    @PutMapping("/complete/{id}")
    @Operation(summary = "完成订单")
    public Result complete(@PathVariable("id") Long id){
        log.info("完成订单，参数为:{}",id);
        orderService.complete(id);
        return Result.success();
    }
}

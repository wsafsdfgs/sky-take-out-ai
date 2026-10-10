package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
    * 用户下单
    * @param ordersSubmitDTO
     * *@ return
    * */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);



/**
 * 分页查询用户信息的接口方法
 * @param page 当前页码，从1开始计数
 * @param pageSize 每页显示的记录数
 * @param status 用户状态，可为null表示不限制状态
 * @return 返回分页查询结果对象，包含数据列表和分页信息
 */
    PageResult pageQuery4User(int page, int pageSize, Integer status);


/**
 * 处理重复项的方法
 * @param id Long类型的唯一标识符，用于识别需要处理的重复项
 */
    void repetition(Long id);

/**
 * 根据订单ID查询订单详细信息
 * 
 * @param id 订单ID，用于唯一标识一个订单
 * @return 返回OrderVO对象，包含订单的详细信息
 */
    OrderVO detail(Long id);

 /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id) throws Exception;


/**
 * 根据条件进行分页查询订单信息
 *
 * @param ordersPageQueryDTO 订单分页查询条件数据传输对象，包含分页信息和查询条件
 * @return PageResult 分页查询结果，包含订单数据列表和分页信息
 */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);


/**
 * 获取订单统计信息的方法
 * 该方法用于返回订单相关的统计数据，通常包含订单总数、已完成订单数、待处理订单数等信息
 * 
 * @return OrderStatisticsVO 包含订单统计信息的视图对象，其中可能包含各种统计指标
 */
    OrderStatisticsVO statistics();

/**
 * 接单的方法
 * @param ordersConfirmDTO 订单确认数据传输对象，包含订单确认所需的信息
 */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);


/**
 * 订单拒绝方法
 * @param ordersRejectionDTO 订单拒绝数据传输对象，包含拒绝订单所需的相关信息
 */
    void rejection(OrdersRejectionDTO ordersRejectionDTO)throws Exception;

/**
 * 完成订单的方法
 * @param id 任务的唯一标识符，使用Long类型以保证大数表示
 */
    void complete(Long id);

/**
 * 商家取消订单的方法
 * @param ordersCancelDTO 订单取消数据传输对象，包含取消订单所需的相关信息
 */
    void Cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

/**
 * 配送订单方法
 * @param id 配送订单的ID标识
 */
    void delivery(Long id);



    void reminder(Long id);

    OrderVO getByNumber(String number);

    /**
     * 查询当前用户未支付（待付款）订单数量
     * @return 未支付订单数
     */
    Integer unPayOrderCount();
}

package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
/**
 * 向数据库中插入订单信息的方法
 * @param orders 包含订单信息的对象，将被插入到数据库中
 */
    void insert(Orders orders);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

/**
 * 根据用户ID查询历史订单列表
 * @param ordersPageQueryDTO 订单分页查询条件对象，包含查询参数和分页信息
 * @return 返回订单分页结果对象，包含订单列表和分页信息
 */
@Select("select * from orders where user_id = #{userId}")
    Page<Orders> getHistoryOrderByid(OrdersPageQueryDTO ordersPageQueryDTO);

/**
 * 分页查询订单信息
 *
 * @param ordersPageQueryDTO 订单分页查询条件数据传输对象，包含分页参数和查询条件
 * @return Page<Orders> 返回分页查询结果，包含订单列表和分页信息
 */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);
/**
 * 根据订单状态查询订单数量
 * @param status 订单状态
 * @return 符合条件的订单数量
 */
    @Select("select count(*) from orders where status = #{status}")
    Integer countByStatus(Integer toBeConfirmed);

    /**
     * 查询指定用户指定状态的订单数量（用于"未支付订单数"校验）
     * @param userId 用户id
     * @param status 订单状态
     * @return 订单数量
     */
    @Select("select count(*) from orders where user_id = #{userId} and status = #{status}")
    Integer countByUserIdAndStatus(Long userId, Integer status);
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime time);

/**
 * 计算Map中所有Double类型数值的总和
 * @param map 包含Double类型数值的Map集合，键值类型不限
 * @return Map中所有Double类型数值的总和
 */
    Double sumByMap(Map map);
    /**
     * 根据Map中的条件查询订单数量
     * @param map 包含查询条件的Map集合，键值类型不限
     * @return 符合条件的订单数量
     */

    Integer countByMap(Map map);
/**
 * 获取指定时间范围内的热销商品列表
 *
 * @param begin 开始时间，包含该时间点
 * @param end 结束时间，包含该时间点
 * @return 返回热销商品DTO列表，按销量排序
 */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);


}

package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

/**
 * 获取指定时间范围内的营业额统计数据
 *
 * @param begin 开始日期，包含该日期
 * @param end 结束日期，包含该日期
 * @return TurnoverReportVO 包含营业额统计数据的视图对象
 */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

/**
 * 根据指定的时间范围获取用户统计数据
 *
 * @param begin 统计开始日期，包含该日期
 * @param end 统计结束日期，包含该日期
 * @return UserReportVO 包含用户统计数据的视图对象，可能包括新增用户数、活跃用户数等信息
 */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

/**
 * 获取指定时间段内的订单统计数据
 * 
 * @param begin 统计开始日期，包含该日期
 * @param end 统计结束日期，包含该日期
 * @return OrderReportVO 包含订单统计信息的结果对象
 */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

/**
 * 获取指定时间段内的销售排行榜前10名数据
 *
 * @param begin 开始日期，包含该日期
 * @param end 结束日期，包含该日期
 * @return SalesTop10ReportVO 包含销售排行榜前10名数据的视图对象
 */
    SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end);

/**
 * 导出业务数据的方法
 * 该方法用于将业务数据导出到客户端，通过HttpServletResponse对象实现数据传输
 *
 * @param response HttpServletResponse对象，用于向客户端返回数据
 *              通过该参数可以将导出的数据写入到HTTP响应中，供客户端下载或使用
 */
    void exportBusinessData(HttpServletResponse response);
}

package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@Tag(name = "数据统计相关接口")
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {
    @Autowired
    private ReportService reportService;
/**
 * 营业额统计接口
 * 用于获取指定时间范围内的营业额统计数据
 *
 * @param begin 开始日期，格式为yyyy-MM-dd
 * @param end 结束日期，格式为yyyy-MM-dd
 * @return 返回TurnoverReportVO类型的营业额统计结果
 */
    @Operation(summary = "营业额统计")
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额统计{}，{}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }
    @Operation(summary = "用户统计")
    @GetMapping("/userStatistics")
    public  Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户数量统计{}，{}", begin, end);
        return Result.success(reportService.getUserStatistics(begin,end));
    }
    @Operation(summary = "订单统计")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("订单数量统计{}，{}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin,end));
    }
    @Operation(summary = "销量排名统计")
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("销量排名Top10{}，{}", begin, end);
        return Result.success(reportService.getTop10(begin,end));

    }
    @GetMapping("/export")
    @Operation(summary = "导出运营报表")
    public void export(HttpServletResponse response){

           reportService.exportBusinessData(response);
    }
}

package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 获取指定时间范围内的营业额统计数据
     *
     * @param begin 开始日期，包含该日期
     * @param end 结束日期，包含该日期
     * @return TurnoverReportVO 包含营业额统计数据的视图对象
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        //存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额，营业额是指，状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
         //封装返回结果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }


/**
 * 获取用户统计数据
 * @param begin 开始日期
 * @param end 结束日期
 * @return 返回包含日期列表、新用户数量列表和总用户数量列表的UserReportVO对象
 */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        //存放每天的新用户数量，总用户数量
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        //查询date日期对应的新用户数量，总用户数量
        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end",endTime);
            Integer  totalUser = userMapper.countByMap(map);

            map.put("begin",beginTime);
            Integer  newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        //封装返回结果
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }
/**
 * 获取指定时间范围内的订单统计数据
 * @param begin 开始日期
 * @param end 结束日期
 * @return 返回包含订单统计信息的OrderReportVO对象
 */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        // 获取日期范围内的所有日期列表
        List<LocalDate> dateList = getDateList(begin, end);
        //存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        //存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 遍历日期列表，统计每天的订单数据

        for (LocalDate date : dateList) {
           // 获取当天的开始时间（0点）
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
           // 获取当天的结束时间（23:59:59）
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询date日期对应的订单数量，状态为“已完成”的订单数量
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //查询date日期对应的订单数量，状态为“已完成”且支付方式为“在线支付”的订单数量
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            validOrderCountList.add(validOrderCount);
            orderCountList.add(orderCount);
        }
        //遍历集合计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //遍历集合计算时间区间内的有效订单总数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //计算订单完成率
        Double orderCompletionRate =0.0;
        if(totalOrderCount!=0){
             orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount ;
        }
        //封装返回结果
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
/**
 * 获取指定时间范围内的销售排行榜前10名数据
 *
 * @param begin 开始日期，包含此日期
 * @param end 结束日期，包含此日期
 * @return SalesTop10ReportVO 包含销售排行榜前10名数据的视图对象，目前返回null，实际实现中应返回具体数据
 */

    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        // 获取当天的开始时间（0点）
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        // 获取当天的结束时间（23:59:59）
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names,",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers,",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
        }

/**
 * 导出业务数据的方法
 * 该方法用于将业务数据导出并返回给客户端
 * @param response HTTP响应对象，用于将导出的数据写入响应流中
 */
    public void exportBusinessData(HttpServletResponse response) {
        //查询数据库获取运行数据（近30天数据）
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO =
                workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));

        //通过POI将数据写入Excel文件

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        //基于模板文件创建Excel文件
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取数据表格标签页
            XSSFSheet sheet = excel.getSheet("sheet1");

            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至"+ dateEnd);

            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for(int i = 0; i < 30; i++) {
               LocalDate date= dateBegin.plusDays(i);
               //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN)
                        , LocalDateTime.of(date, LocalTime.MAX));
                //获取某一行
                row = sheet.getRow(7+i);
                //填充数据
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }

            //将Excel文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    /**
     * 生成从开始日期到结束日期的日期列表
     * @param begin 开始日期（包含）
     * @param end 结束日期（包含）
     * @return 日期列表
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        return dateList;
    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);
        return orderMapper.countByMap(map);

    }
}

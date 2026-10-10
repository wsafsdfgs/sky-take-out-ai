package com.sky.aicustomerservice.function;

import com.sky.aicustomerservice.properties.WaimaiProperties;
import com.sky.aicustomerservice.result.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 商家端经营助手工具类
 *
 * 将主项目商家端（admin）接口封装成 Spring AI 工具，供大模型调用，
 * 帮助商家查看经营数据（今日营业、订单概况、销量排行、营业状态）。
 *
 * 关键点：
 * - 商家端透传 token 用 header「token」（admin 拦截器取 admin-token-name=token），
 *   与用户端（authentication）不同。
 * - 商家 token 是 empId 签发的 admin JWT。
 */
@Slf4j
@Component
public class BusinessFunction {

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private WaimaiProperties waimaiProperties;

    /**
     * 大模型工具：查询今日营业数据
     *
     * 调用主项目工作台接口获取今日营业额、订单量、客单价等经营数据。
     */
    @Tool(description = "查询店铺今日营业数据（营业额、订单量、客单价、新增用户数、平均客单价）")
    public String queryBusinessData(ToolContext toolContext) {
        try {
            String adminToken = (String) toolContext.getContext().get("adminToken");
            String url = waimaiProperties.getMainUrl() + "/admin/workspace/businessData";
            String body = get(url, adminToken);
            log.info("AI商家端查今日营业数据: {}", url);
            return body;
        } catch (HttpStatusCodeException e) {
            log.error("商家端查营业数据HTTP错误: {}", e.getStatusCode());
            return "查询今日营业数据失败：" + e.getStatusCode();
        } catch (Exception e) {
            log.error("商家端查营业数据失败", e);
            return "查询今日营业数据失败，请稍后再试";
        }
    }

    /**
     * 大模型工具：查询订单管理概况
     *
     * 调用主项目工作台订单总览接口，获取待接单、待派送、已完成等各状态订单数量。
     */
    @Tool(description = "查询店铺订单管理概况（待接单、待派送、派送中、已完成、已取消等各状态订单数量）")
    public String queryOrderOverview(ToolContext toolContext) {
        try {
            String adminToken = (String) toolContext.getContext().get("adminToken");
            String url = waimaiProperties.getMainUrl() + "/admin/workspace/overviewOrders";
            String body = get(url, adminToken);
            log.info("AI商家端查订单概况: {}", url);
            return body;
        } catch (HttpStatusCodeException e) {
            log.error("商家端查订单概况HTTP错误: {}", e.getStatusCode());
            return "查询订单概况失败：" + e.getStatusCode();
        } catch (Exception e) {
            log.error("商家端查订单概况失败", e);
            return "查询订单概况失败，请稍后再试";
        }
    }

    /**
     * 大模型工具：查询销量Top10
     *
     * 调用主项目报表接口获取指定时间范围内的销量排行。
     *
     * @param begin 开始日期，格式 yyyy-MM-dd
     * @param end   结束日期，格式 yyyy-MM-dd
     */
    @Tool(description = "查询销量排名Top10菜品，需要传开始日期和结束日期（格式yyyy-MM-dd），如查询今日可传当天日期")
    public String queryTop10(
            @ToolParam(description = "开始日期，格式yyyy-MM-dd") String begin,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd") String end,
            ToolContext toolContext) {
        try {
            String adminToken = (String) toolContext.getContext().get("adminToken");
            String url = waimaiProperties.getMainUrl() + "/admin/report/top10?begin=" + begin + "&end=" + end;
            String body = get(url, adminToken);
            log.info("AI商家端查销量Top10: {}", url);
            return body;
        } catch (HttpStatusCodeException e) {
            log.error("商家端查销量Top10 HTTP错误: {}", e.getStatusCode());
            return "查询销量Top10失败：" + e.getStatusCode();
        } catch (Exception e) {
            log.error("商家端查销量Top10失败", e);
            return "查询销量Top10失败，请稍后再试";
        }
    }

    /**
     * 大模型工具：查询营业状态
     *
     * 调用主项目接口查询当前店铺是否营业。
     */
    @Tool(description = "查询店铺当前营业状态（是否营业）")
    public String queryShopStatus(ToolContext toolContext) {
        try {
            String adminToken = (String) toolContext.getContext().get("adminToken");
            String url = waimaiProperties.getMainUrl() + "/admin/shop/status";
            String body = get(url, adminToken);
            log.info("AI商家端查营业状态: {}", url);
            // 主项目返回 Result<Integer>，1=营业中，0=已打烊
            return body;
        } catch (HttpStatusCodeException e) {
            log.error("商家端查营业状态HTTP错误: {}", e.getStatusCode());
            return "查询营业状态失败：" + e.getStatusCode();
        } catch (Exception e) {
            log.error("商家端查营业状态失败", e);
            return "查询营业状态失败，请稍后再试";
        }
    }

    /**
     * 通用 GET 请求，透传商家端 admin token
     */
    private String get(String url, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        // 商家端透传 token 用 header「token」（主项目 admin 拦截器取 admin-token-name=token）
        headers.set("token", adminToken);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<Result> resp = restTemplate.exchange(url, HttpMethod.GET, httpEntity, Result.class);
        if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
            return "查询失败或无权限访问";
        }
        Result result = resp.getBody();
        if (result.getCode() != 1 || result.getData() == null) {
            return result.getMsg() == null ? "无数据" : result.getMsg();
        }
        return result.getData().toString();
    }
}

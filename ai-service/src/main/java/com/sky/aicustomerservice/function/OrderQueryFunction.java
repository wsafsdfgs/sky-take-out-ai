package com.sky.aicustomerservice.function;



import com.sky.aicustomerservice.dto.OrderDTO;
import com.sky.aicustomerservice.pojo.Request;
import com.sky.aicustomerservice.pojo.Response;
import com.sky.aicustomerservice.properties.JwtProperties;
import com.sky.aicustomerservice.properties.WaimaiProperties;
import com.sky.aicustomerservice.result.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;   // Spring的，不是config的


/**
 * 订单查询功能类
 * 提供两个主要功能：
 * 1. 根据订单号查询订单详情
 * 2. 查询用户的所有订单列表
 * 
 * 该类被 Spring AI 框架识别为工具，供大模型调用，用于获取订单相关信息
 */
@Slf4j
@Component
public class OrderQueryFunction {
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private WaimaiProperties waimaiProperties;
    /**
     * 大模型工具：根据订单号查订单
     * request    : 大模型根据用户说的订单号自动填的
     * toolContext: Spring AI 注入的上下文，里面放着前端传的用户token
     */
    /**
     * 根据订单号查询订单详情的工具方法
     * 
     * 该方法被 Spring AI 框架识别为工具，供大模型调用，用于获取特定订单的详细信息。
     * 工作流程：
     * 1. 从工具上下文中获取用户token
     * 2. 构造请求URL，拼接主项目地址和订单详情接口
     * 3. 添加认证头，将用户token传递给主项目
     * 4. 发送HTTP GET请求获取订单数据
     * 5. 处理响应，将订单状态码转换为可读文本
     * 6. 构造返回对象，包含订单号、状态、金额、收货人和预计送达时间
     * 
     * @param request 包含订单号的请求对象，由大模型根据用户输入自动填充
     * @param toolContext Spring AI 注入的上下文，包含用户token等信息
     * @return Response 包含订单详情的响应对象
     */
    @Tool(description = "根据订单号查询外卖订单详情，获取订单状态、金额、收货人、预计送达时间")
    public Response queryOrder(Request request, ToolContext toolContext) {
        try {
            // 从工具上下文取用户token（不经过大模型，安全）
            String userToken = (String) toolContext.getContext().get("userToken");
            // 拼主项目URL
            // mainUrl + orderDetailUrl + 订单号
            // 例：http://localhost:8080/user/order/innerDetailByNumber/123456
            String url = waimaiProperties.getMainUrl()
                    + waimaiProperties.getOrderDetailUrl()
                    + request.getOrderNo();
            log.info("AI调用主项目查单: {}", url);
            // 构造请求头，透传用户token给主项目
            // 主项目拦截器从 authentication 头拿token做鉴权+归属校验
            HttpHeaders headers = new HttpHeaders();
            headers.set("authentication", userToken);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            //发HTTP GET请求
            // 主项目返回 Result<OrderDTO>，泛型要用 ParameterizedTypeReference 才能反序列化
            ResponseEntity<Result<OrderDTO>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, httpEntity,
                    new ParameterizedTypeReference<>() {});
            //  解析结果
            // 主项目 403（无权访问/订单不存在）时 HTTP 状态码不是200
            if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
                return new Response("订单查询失败或无权访问");
            }
            Result<OrderDTO> result = resp.getBody();
            // code=1成功，code=0失败
            if (result.getCode() != 1 || result.getData() == null) {
                return new Response(result.getMsg() == null ? "订单不存在" : result.getMsg());
            }
            OrderDTO order = result.getData();
            //订单状态码转文字（给大模型更好理解）
            String statusText = switch (order.getStatus()) {
                case 1 -> "待付款";
                case 2 -> "待接单";
                case 3 -> "已接单";
                case 4 -> "派送中";
                case 5 -> "已完成";
                case 6 -> "已取消";
                case 7 -> "退款";
                default -> "未知";
            };
            //组装Response返回给大模型
            // 大模型拿到这些字段，用自然语言回复用户
            return new Response(
                    order.getNumber(),
                    statusText,
                    order.getAmount(),
                    order.getConsignee(),
                    order.getEstimatedDeliveryTime() == null
                            ? "未知"
                            : order.getEstimatedDeliveryTime());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // 主项目返回了4xx/5xx（403无权访问/订单不存在等），RestTemplate默认把它当异常抛出，这里专门处理
            log.error("主项目返回HTTP错误: {}", e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                // 403：无权访问 或 订单不存在
                return new Response("订单不存在或无权访问该订单");
            }
            return new Response("订单查询失败，请稍后再试");
        } catch (Exception e) {
            log.error("AI查询订单失败", e);
            return new Response("订单查询失败，请稍后再试");
        }
    }
    /**
     * 大模型工具：查询我的订单列表（不需要订单号）
     */
    /**
     * 查询用户所有订单列表的工具方法
     * 
     * 该方法被 Spring AI 框架识别为工具，供大模型调用，用于获取用户的所有订单列表。
     * 与 queryOrder 方法的区别：此方法不需要订单号，直接返回用户的所有订单。
     * 工作流程：
     * 1. 从工具上下文中获取用户token
     * 2. 构造请求URL，指向主项目的订单列表接口
     * 3. 添加认证头，将用户token传递给主项目
     * 4. 发送HTTP GET请求获取订单列表数据
     * 5. 处理响应，将结果格式化为字符串返回给大模型
     * 
     * @param toolContext Spring AI 注入的上下文，包含用户token等信息
     * @return Response 包含订单列表信息的响应对象
     */
    @Tool(description = "查询当前用户的所有外卖订单列表，获取订单号、状态、金额")
    public Response queryMyOrders(ToolContext toolContext) {
        try {
            // 1. 从上下文取token（和queryOrder一样）
            String userToken = (String) toolContext.getContext().get("userToken");
            // 2. 拼主项目URL（注意是innerMyList）
            String url = waimaiProperties.getMainUrl() + "/user/order/innerMyList";
            log.info("AI调用主项目查我的订单列表: {}", url);
            // 3. 构造请求头透传token
            HttpHeaders headers = new HttpHeaders();
            headers.set("authentication", userToken);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            // 4. 发请求，返回Result<PageResult>
            // 注意：这里返回的是分页结果，不是单个订单
            ResponseEntity<Result> resp = restTemplate.exchange(
                    url, HttpMethod.GET, httpEntity,
                    Result.class);
            // 5. 处理结果
            if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
                return new Response("查询订单列表失败");
            }
            // 把订单列表拼成一段文字返回给大模型
            // 大模型拿到这段文字，自然语言回复用户
            String result = "您的订单列表：\n" + resp.getBody().getData().toString();
            return new Response(result);
        } catch (HttpStatusCodeException e) {
            log.error("查我的订单列表HTTP错误: {}", e.getStatusCode());
            return new Response("查询订单列表失败，请稍后再试");
        } catch (Exception e) {
            log.error("查我的订单列表失败", e);
            return new Response("查询订单列表失败，请稍后再试");
        }
    }
}



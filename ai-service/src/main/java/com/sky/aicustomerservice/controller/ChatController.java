package com.sky.aicustomerservice.controller;

import com.sky.aicustomerservice.context.UserContext;
import com.sky.aicustomerservice.entiy.ChatMemoryRecord;
import com.sky.aicustomerservice.function.OrderQueryFunction;
import com.sky.aicustomerservice.function.KnowledgeBaseFunction;
import com.sky.aicustomerservice.function.BusinessFunction;
import com.sky.aicustomerservice.properties.WaimaiProperties;
import com.sky.aicustomerservice.result.Result;
import com.sky.aicustomerservice.service.ChatService;
import com.sky.aicustomerservice.service.RagService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * AI客服控制器
 * 
 * 提供与AI客服交互的RESTful API，包括：
 * 1. 用户登录接口（生成JWT令牌）
 * 2. 非流式对话接口（简单测试用）
 * 3. 流式对话接口（主要功能）
 * 4. RAG增强的流式对话接口
 * 5. 会话历史查询接口
 * 
 * 该控制器整合了Spring AI框架，支持大模型调用工具函数，实现智能客服功能。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class ChatController {
    @Resource
    private ChatClient chatClient;
    @Resource
    private ChatService chatService;
    @Resource
    private OrderQueryFunction orderQueryFunction;
    @Resource
    private KnowledgeBaseFunction knowledgeBaseFunction;
    @Resource
    private BusinessFunction businessFunction;
    @Resource
    private WaimaiProperties waimaiProperties;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RagService ragService;
    @Value("${sky.shop.phone}")
    private String shopPhone;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Resource
    private com.sky.aicustomerservice.properties.JwtProperties jwtProperties;

    private static final String BASE_SYS_PROMPT = """
你是外卖平台AI客服，严肃专业。
可用工具：【查询订单详情】【查询我的订单列表】【检索售后知识库】
路由规则：
- 用户查询订单状态、骑手位置、预计送达、订单列表 → 调用对应的订单查询工具。
- 用户询问退款、售后、投诉、赔付等流程规则 → 调用【检索售后知识库】工具，基于检索结果回答。
- 其他闲聊、寒暄、平台介绍 → 直接自然回答，无需调用工具。
规则：
1. 用户查询订单状态、骑手位置、预计送达，调用查询订单工具，禁止编造信息；需要订单号时，向用户询问订单号。
2. 当用户表达【退款、申请赔付、订单投诉】意图：
    禁止调用任何修改订单的工具。请按以下顺序回复用户：
    ① 告知用户可在【订单详情页】点击"申请退款"自助提交退款申请；
    ② 如需协助，可拨打本店联系电话联系商家协商（电话见下方系统配置，不要编造）；
    ③ 若协商不满意，即将为您转接人工客服。
    并在末尾输出标记 [transferHuman]
3. 当用户提出催单诉求：
    优先告知用户可以联系商家协商，并提供商家联系渠道；同时告知用户，如果协商不满意，可以转接人工客服。
4. 拿到订单数据后，使用自然语言回答，不要返回原始JSON。
5. 不要承诺退款、补偿，所有资金相关业务一律交给人工。
规则补充：
- 用户说"查我的订单/最近有什么单/我的订单列表"时，调用【查询我的订单列表】工具，不需要用户提供订单号。
- 用户提供了具体订单号（如"帮我查订单 178xxx"）时，调用【根据订单号查询订单详情】工具。
- 用户询问"怎么退款/怎么申请售后/售后规则/投诉流程"等 → 调用【检索售后知识库】工具，从检索结果整理答案；知识库没有则告知转人工。
""";
    private static final int MAX_STREAM_CONTENT_LEN = 4000;

    /** 商家端经营助手系统提示词（%s 会被替换为今天日期 yyyy-MM-dd） */
    private static final String BUSINESS_SYS_PROMPT = """
你是外卖店铺的AI经营助手，帮助商家查看和分析经营数据。严肃专业，用数据说话。
今天是 %s，请以这个日期作为"今天/今日"的基准日期。
可用工具：【查今日营业数据】【查订单管理概况】【查销量Top10】【查营业状态】
路由规则：
- 用户问今日营业额/订单量/客单价/新增用户 → 调用【查今日营业数据】工具。
- 用户问待接单/待派送/已完成等订单情况 → 调用【查订单管理概况】工具。
- 用户问卖得好的菜/销量排行 → 调用【查销量Top10】工具（需要询问要查询的日期范围，默认用今天 %s）。
- 用户问是否营业/营业状态 → 调用【查营业状态】工具。
规则：
1. 拿到数据后，用自然语言清晰汇报，突出关键数字，不要返回原始JSON。
2. 查询销量Top10时，若用户没说日期，默认用今天（%s），并告知用户可查询指定日期。
3. 商家没有订单查询工具，不要回答具体某张订单的详情。
4. 数据为空或查询失败时，如实告知。
""";

    @GetMapping("/login")
    public Result<Map<String,Object>> login(@RequestParam Long userId) {
        // 调主项目本地登录接口拿真实 token
        Map<String, Long> body = new HashMap<>();
        body.put("userId", userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Long>> req = new HttpEntity<>(body, headers);

        // 主项目返回 Result<UserLoginVO>，里面有 token
        ResponseEntity<Result> resp = restTemplate.postForEntity(
                waimaiProperties.getMainUrl() + "/user/user/innerLogin",
                req, Result.class);
        Result result = resp.getBody();
        if (result == null || result.getCode() != 1 || result.getData() == null) {
            return Result.error("登录失败");
        }
        // 从 data 里取 token
        // data 是 LinkedHashMap，取 "token" 字段
        Map<String, Object> data = (Map<String, Object>) result.getData();
        String token = (String) data.get("token");
        return Result.success(Map.of("token", token));
    }

    /**
     * 商家端登录接口
     *
     * 透传主项目商家端员工登录接口（/admin/employee/login），用账号密码换 admin token。
     * 返回 Result<Map>：{code, msg, data:{token:"..."}}
     */
    @GetMapping("/businessLogin")
    public Result<Map<String, Object>> businessLogin(
            @RequestParam String username,
            @RequestParam String password) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

            // 主项目返回 Result<EmployeeLoginVO>，token 在 data.token
            ResponseEntity<Result> resp = restTemplate.postForEntity(
                    waimaiProperties.getMainUrl() + "/admin/employee/login",
                    req, Result.class);
            Result result = resp.getBody();
            if (result == null || result.getCode() != 1 || result.getData() == null) {
                return Result.error("登录失败，请检查账号密码");
            }
            Map<String, Object> data = (Map<String, Object>) result.getData();
            String token = (String) data.get("token");
            return Result.success(Map.of("token", token));
        } catch (Exception e) {
            log.error("商家端登录失败", e);
            return Result.error("登录失败，请检查账号密码");
        }
    }
    /**
     * 构建系统提示词
     * 
     * 将基础系统提示词与店铺信息（电话、地址）和用户ID组合，形成完整的系统提示词。
     * 系统提示词用于指导AI的行为和回答方式，包含以下内容：
     * 1. 基础客服角色定义和规则
     * 2. 可用工具说明
     * 3. 特定场景处理规则（如退款、投诉等）
     * 4. 店铺联系信息
     * 5. 当前用户ID（用于订单查询工具）
     * 
     * @param userId 当前用户ID
     * @return 组装后的系统提示词字符串
     */
    private String buildSysPrompt(Long userId) {
        return BASE_SYS_PROMPT
                + "\n本店联系电话：" + shopPhone + "，店铺地址：" + shopAddress
                + "\n调用订单查询工具时，userId参数固定使用：" + userId;
    }

    /**
     * 非流式对话接口
     * 
     * 提供简单的AI对话功能，用于测试和调试。此接口不保存会话历史，每次都是全新对话。
     * 工作流程：
     * 1. 获取当前用户ID
     * 2. 构建包含店铺信息和用户ID的系统提示词
     * 3. 调用Spring AI的ChatClient进行对话
     * 4. 将订单查询工具传递给AI
     * 5. 返回AI的回复内容
     * 
     * @param message 用户输入的消息
     * @return AI的回复内容
     */
    @GetMapping("/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "你好") String message) {
        try {
            Long userId = UserContext.getUserId();
            String fullSysPrompt = buildSysPrompt(userId);
            return chatClient.prompt()
                    .system(fullSysPrompt)
                    .user(message)
                    .tools(orderQueryFunction, knowledgeBaseFunction)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 非流式调用失败", e);
            return "AI 服务暂时不可用";
        }
    }

    /**
     * SSE流式对话接口
     * 
     * 提供基于Server-Sent Events(SSE)的流式AI对话功能，支持实时响应和会话记忆。
     * 这是主要的AI客服交互接口，工作流程：
     * 1. 设置SSE响应头
     * 2. 获取当前用户ID和令牌
     * 3. 构建包含店铺信息和用户ID的系统提示词
     * 4. 保存用户消息到会话历史
     * 5. 构建消息列表（包含历史对话）
     * 6. 调用Spring AI的流式ChatClient进行对话
     * 7. 将订单查询工具和用户上下文传递给AI
     * 8. 实时返回AI的回复内容
     * 9. AI回复完成后，异步保存回复到会话历史
     * 
     * @param message 用户输入的消息
     * @param sessionId 会话ID，用于区分不同对话会话
     * @param response HTTP响应对象
     * @return Flux<String> 流式返回AI的回复内容
     */
    @GetMapping(value = "/generateStream", produces = "text/event-stream")
    public Flux<String> generateStream(
            @RequestParam(value = "message", defaultValue = "你好") String message,
            @RequestParam(value = "sessionId", defaultValue = "1") String sessionId,
            HttpServletResponse response) {

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            Long userId = UserContext.getUserId();
            String userToken = UserContext.getToken();
            // ========= 拼接系统提示词：注入店铺信息 + 当前userId =========
            String currentSysPrompt = buildSysPrompt(userId);

            // ==========【1.保存用户消息】==========
            chatService.saveChatRecord(sessionId, "user", message, System.currentTimeMillis(), userId);

            // ✅ 修改这里，传入userId
            List<Message> messages = buildMessages(sessionId, currentSysPrompt, userId);
            StringBuilder responseBuilder = new StringBuilder();

            return chatClient.prompt()
                    .messages(messages)
                    .tools(orderQueryFunction, knowledgeBaseFunction)
                    .toolContext(Map.of("userToken", userToken, "userId", userId))
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        responseBuilder.append(chunk);
                    })
                    // ==========【2.AI输出完毕，异步保存assistant消息】==========
                    .doOnComplete(() -> {
                        String fullContent = responseBuilder.toString();
                        saveAssistantRecordAsync(sessionId, fullContent, userId);
                    })
                    .onErrorResume(e -> {
                        log.error("AI调用异常", e);
                        return Flux.just("当前订单系统繁忙，请稍后重试。");
                    });
        } catch (Exception e) {
            log.error("接口异常", e);
            return Flux.just("AI服务暂时不可用。");
        }
    }

    /**
     * 商家端 SSE 流式对话接口（AI经营助手）
     *
     * 与用户端 generateStream 对应，但：
     * - 使用商家端经营助手提示词 + BusinessFunction 工具
     * - admin token 由前端从 /ai/businessLogin 获取，通过 URL 参数传入
     * - AI 项目不解析商家 token，原样透传给主项目校验（WebConfig 已放行本接口）
     */
    @GetMapping(value = "/businessStream", produces = "text/event-stream")
    public Flux<String> businessStream(
            @RequestParam(value = "message", defaultValue = "你好") String message,
            @RequestParam(value = "sessionId", defaultValue = "biz001") String sessionId,
            @RequestParam(value = "token", required = false) String adminToken,
            HttpServletResponse response) {

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            if (adminToken == null || adminToken.isBlank()) {
                return Flux.just("未登录，请先完成商家登录");
            }
            // 组装商家端提示词 + 历史（注入今天日期，避免AI瞎编日期）+ 历史
            List<Message> messages = new ArrayList<>();
            String today = java.time.LocalDate.now().toString(); // yyyy-MM-dd
            messages.add(new SystemMessage(String.format(BUSINESS_SYS_PROMPT, today, today, today)));
            // 读取历史（商家端不按 userId 隔离，直接用 sessionId 查）
            List<ChatMemoryRecord> history = chatService.getChatHistory(sessionId, 0L);
            if (history != null) {
                for (ChatMemoryRecord record : history) {
                    if ("user".equals(record.getRole())) {
                        messages.add(new UserMessage(record.getContent()));
                    } else if ("assistant".equals(record.getRole())) {
                        messages.add(new AssistantMessage(record.getContent()));
                    }
                }
            }
            messages.add(new UserMessage(message));
            chatService.saveChatRecord(sessionId, "user", message, System.currentTimeMillis(), 0L);

            StringBuilder responseBuilder = new StringBuilder();
            return chatClient.prompt()
                    .messages(messages)
                    .tools(businessFunction)
                    .toolContext(Map.of("adminToken", adminToken))
                    .stream()
                    .content()
                    .doOnNext(chunk -> responseBuilder.append(chunk))
                    .doOnComplete(() -> {
                        String fullContent = responseBuilder.toString();
                        saveAssistantRecordAsync(sessionId, fullContent, 0L);
                    })
                    .onErrorResume(e -> {
                        log.error("商家端AI调用异常", e);
                        return Flux.just("经营数据服务繁忙，请稍后重试。");
                    });
        } catch (Exception e) {
            log.error("商家端接口异常", e);
            return Flux.just("AI服务暂时不可用。");
        }
    }

    /**
     * 构建消息列表
     * 
     * 根据会话ID从数据库中获取历史对话记录，并构建包含系统提示词和对话历史的消息列表。
     * 该列表将传递给AI模型，使AI能够理解上下文并提供连贯的回答。
     * 工作流程：
     * 1. 从数据库获取指定会话的历史对话记录
     * 2. 添加系统提示词作为第一条消息
     * 3. 遍历历史记录，将用户和助手的消息转换为Message对象
     * 4. 返回构建完成的消息列表
     * 
     * @param sessionId 会话ID，用于获取历史对话
     * @param sysPrompt 系统提示词
     * @param userId 用户ID
     * @return 包含系统提示词和对话历史的消息列表
     */
    private List<Message> buildMessages(String sessionId, String sysPrompt, Long userId) {
        List<ChatMemoryRecord> historyList = chatService.getChatHistory(sessionId, userId);
        log.info("读取session={} 的历史消息：{}", sessionId, historyList);
        List<Message> messages = new ArrayList<>();
        // 使用动态拼接的系统提示词（携带当前userId）
        messages.add(new SystemMessage(sysPrompt));

        if (historyList != null && !historyList.isEmpty()) {
            for (ChatMemoryRecord record : historyList) {
                log.info("单条记录 role={}, content={}", record.getRole(), record.getContent());
                if ("user".equals(record.getRole())) {
                    messages.add(new UserMessage(record.getContent()));
                } else if ("assistant".equals(record.getRole())) {
                    messages.add(new AssistantMessage(record.getContent()));
                }
            }
        }
        return messages;
    }

    /**
     * 查询会话历史
     * 
     * 根据会话ID和当前用户ID查询该会话的历史对话记录。
     * 用于前端展示对话历史或实现会话恢复功能。
     * 
     * @param sessionId 会话ID
     * @return 包含历史对话记录的Result对象
     */
    @GetMapping("/history")
    public Result<List<ChatMemoryRecord>> getChatHistory(
            @RequestParam(value = "sessionId", defaultValue = "1") String sessionId) {
        Long userId = UserContext.getUserId();
        List<ChatMemoryRecord> history = chatService.getChatHistory(sessionId, userId);
        return Result.success(history);
    }

    /**
     * 异步保存AI回复
     * 
     * 将AI的回复内容异步保存到数据库，避免阻塞主线程影响用户体验。
     * 使用Reactor的Mono和Schedulers.boundedElastic实现异步操作。
     * 工作流程：
     * 1. 检查回复内容是否为空
     * 2. 创建保存任务并提交到弹性调度器执行
     * 3. 忽略成功结果，仅记录失败日志
     * 
     * @param sessionId 会话ID
     * @param fullContent AI回复的完整内容
     * @param userId 用户ID
     */
    private void saveAssistantRecordAsync(String sessionId, String fullContent, Long userId) {
        if (fullContent == null || fullContent.isBlank()) {
            log.warn("AI返回内容为空，不保存，sessionId={}", sessionId);
            return;
        }
        Mono.fromRunnable(() -> {
                    chatService.saveChatRecord(sessionId, "assistant", fullContent, System.currentTimeMillis(), userId);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        unused -> {},
                        err -> log.error("异步保存AI回答失败，sessionId={}", sessionId, err)
                );
    }

    @GetMapping(value = "/ragStream", produces = "text/event-stream")
    public Flux<String> ragStream(
            @RequestParam(value = "message", defaultValue = "你好") String message,
            @RequestParam(value = "sessionId", defaultValue = "rag001") String sessionId,
            HttpServletResponse response) {

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            // ✅ 这里定义变量，名字必须是 userId
            Long userId = UserContext.getUserId();
            // ✅ 1.先读取ChatMemoryRecord记录
            List<ChatMemoryRecord> historyRecordList = chatService.getChatHistory(sessionId, userId);
            // ✅ 2.手动转换成 Message 列表
            List<Message> historyMessages = new ArrayList<>();
            for (ChatMemoryRecord record : historyRecordList) {
                if ("user".equals(record.getRole())) {
                    historyMessages.add(new UserMessage(record.getContent()));
                } else if ("assistant".equals(record.getRole())) {
                    historyMessages.add(new AssistantMessage(record.getContent()));
                }
            }

            chatService.saveChatRecord(sessionId, "user", message, System.currentTimeMillis(), userId);
            StringBuilder responseBuilder = new StringBuilder();

            return ragService.ragStreamChat(message, historyMessages)
                    .doOnNext(chunk -> responseBuilder.append(chunk))
                    .doOnComplete(() -> {
                        String fullContent = responseBuilder.toString();
                        saveAssistantRecordAsync(sessionId, fullContent, userId);
                    })
                    .onErrorResume(e -> {
                        log.error("RAG知识库调用异常", e);
                        return Flux.just("知识库服务繁忙，请稍后重试");
                    });
        } catch (Exception e) {
            log.error("ragStream接口异常", e);
            return Flux.just("AI服务暂时不可用。");
        }
    }
}

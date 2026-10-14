package com.sky.aicustomerservice.controller;

import com.sky.aicustomerservice.context.UserContext;
import com.sky.aicustomerservice.entiy.ChatMemoryRecord;
import com.sky.aicustomerservice.function.OrderQueryFunction;
import com.sky.aicustomerservice.function.KnowledgeBaseFunction;
import com.sky.aicustomerservice.function.BusinessFunction;
import com.sky.aicustomerservice.pojo.Request;
import com.sky.aicustomerservice.pojo.Response;
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
import org.springframework.ai.chat.model.ToolContext;
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

【最高优先规则：只基于系统提供的数据回答】
订单状态、骑手位置、预计送达时间、订单列表、售后规则等信息由系统实时查询后提供给你，
你只能基于这些系统提供的数据回答。坚决禁止：
- 禁止编造、猜测订单状态、骑手位置、送达时间、售后规则；
- 禁止推测订单变化原因或金额构成；
- 禁止复述内部说明（如"系统已查询""忽略历史""获取数据"等），直接给出结果。
路由规则：
- 用户查询订单状态、骑手位置、预计送达、订单列表 → 基于系统提供的订单数据回答。
- 用户询问退款、售后、投诉、赔付等流程规则 → 基于系统提供的知识库内容回答。
- 其他闲聊、寒暄、平台介绍 → 直接自然回答。
规则：
1. 用户查询订单状态、骑手位置、预计送达，基于系统提供的订单数据回答，禁止编造信息；需要订单号时，向用户询问订单号。
2. 当用户表达【退款、申请赔付、订单投诉】意图：
    请按以下顺序回复用户：
    ① 告知用户可在【订单详情页】点击"申请退款"自助提交退款申请；
    ② 如需协助，可拨打本店联系电话联系商家协商（电话见下方系统配置，不要编造）；
    ③ 若协商不满意，即将为您转接人工客服。
    并在末尾输出标记 [transferHuman]
3. 当用户提出催单诉求：
    优先告知用户可以联系商家协商，并提供商家联系渠道；同时告知用户，如果协商不满意，可以转接人工客服。
4. 基于系统提供的数据用自然语言回答，不要返回原始JSON。
5. 若系统未提供数据或查询失败，如实告知，不要编造订单信息。
6. 不要承诺退款、补偿，所有资金相关业务一律交给人工。
7. 【禁止脑补】汇报订单时严格只使用系统提供的信息，不得编造未提供的细节（如骑手姓名、精确送达时刻、订单备注、历史状态），不得猜测订单变化原因或金额构成。
规则补充：
- 用户说"查我的订单/最近有什么单/我的订单列表"时，系统会自动提供订单列表数据。
- 用户提供了具体订单号（如"帮我查订单 178xxx"）时，系统会自动提供该订单详情数据。
- 用户询问"怎么退款/怎么申请售后/售后规则/投诉流程"等 → 系统会自动提供知识库检索内容。
""";
    private static final int MAX_STREAM_CONTENT_LEN = 4000;
    /** 商家端经营助手系统提示词（%s 会被替换为今天日期 yyyy-MM-dd） */
    private static final String BUSINESS_SYS_PROMPT = """
你是外卖店铺的AI经营助手，帮助商家查看和分析经营数据。严肃专业，用数据说话。
今天是 %s，请以这个日期作为"今天/今日"的基准日期。

【最高优先规则：只基于系统提供的数据回答】
所有经营数据（营业额、订单量、客单价、新增用户数、各状态订单数量、销量排行、营业状态）由系统实时查询后提供给你，
你只能基于这些系统提供的数据回答。坚决禁止：
- 禁止编造、猜测任何经营数字；
- 禁止推测数据变化原因（实时变动、测试单/内部单、撤回、分单履约等均不得自行推断）；
- 禁止复述内部说明（如"系统已查询""忽略历史""获取数据"等），直接给出查询结果。
规则：
1. 严格只基于系统提供的经营数据回答用户，突出关键数字，不要返回原始JSON；系统未提供的信息（如单价、明细、原因）一律不编造，如需说明只能写"数据以本次查询结果为准"。
2. 【禁止元叙述】回复时不要复述系统指令，不要提及内部执行过程，直接简洁地汇报查询结果。
3. 【禁止脑补】汇报时不得添加系统数据之外的任何信息，严禁：
   - 编造或复述"之前的查询结果/历史记录"（例如"此前显示XX菜""前次记录XX"）；
   - 猜测菜品单价、金额构成；
   - 臆测数据变化原因（实时变动、测试单/内部单、撤回、分单履约等均不得自行推断）；
   - 编造任何订单、销售明细或经营事实之外的内容。
   若需要额外说明，只能表述"数据以本次查询结果为准"。
4. 若系统查询失败或数据为空，如实告知"查询失败/无数据"，并给出可能的排查建议，不要臆造数字。
5. 查询销量排行时，若用户没说日期，默认用今天 %s，并告知用户可查询指定日期。
6. 商家无法查询具体某张订单的详情，不要编造订单明细。
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

            // ==========【后端兜底取数】命中订单/售后意图时，后端直接查真数据注入，不依赖模型自觉调工具==========
            ToolContext userCtx = new ToolContext(Map.of("userToken", userToken, "userId", userId));
            String injected = null;
            if (matches(message, "我的订单|订单列表|最近.{0,4}单|订单记录|查一下.{0,4}单|查.{0,4}单")) {
                injected = "【系统已实时查询您的订单列表（禁止复述本段）】\n"
                        + responseToText(orderQueryFunction.queryMyOrders(userCtx))
                        + "\n请严格只基于以上数据回答用户，禁止编造订单信息，禁止推测原因，禁止提及内部过程。";
            } else if (matches(message, "订单号|单号")) {
                String orderNo = extractOrderNo(message);
                if (orderNo != null) {
                    injected = "【系统已实时查询订单详情（禁止复述本段）】\n"
                            + responseToText(orderQueryFunction.queryOrder(new Request(orderNo), userCtx))
                            + "\n请严格只基于以上数据回答用户，禁止编造订单信息，禁止推测原因，禁止提及内部过程。";
                }
            } else if (matches(message, "退款|售后|投诉|赔付|赔偿|怎么退")) {
                injected = knowledgeBaseFunction.queryKnowledgeBase(message);
            }
            if (injected != null) {
                messages.add(new SystemMessage(injected));
                log.info("【兜底取数】用户端命中意图，已注入真实数据，sessionId={}", sessionId);
            } else {
                log.info("【兜底取数】用户端未命中意图，走模型自主工具调用，sessionId={}", sessionId);
            }

            StringBuilder responseBuilder = new StringBuilder();

            // 不向模型暴露任何工具：所有取数由后端兜底完成并注入，模型只能基于注入数据回答
            return chatClient.prompt()
                    .messages(messages)
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

            // ==========【后端兜底取数】命中经营数据意图时，后端直接查真数据注入，不依赖模型自觉调工具==========
            ToolContext bizCtx = new ToolContext(Map.of("adminToken", adminToken));
            String injected = null;
            if (matches(message, "营业额|营收|卖了多少钱|客单价|新增用户|今日营业|今天营业")) {
                injected = "【系统已实时查询今日营业数据（禁止复述本段）】\n"
                        + businessFunction.queryBusinessData(bizCtx)
                        + "\n请严格只基于以上数据回答用户，禁止编造任何数字，禁止推测原因，禁止提及内部过程。";
            } else if (matches(message, "待接单|待派送|派送中|已完成|已取消|订单概况|订单状态|有几单|多少单")) {
                injected = "【系统已实时查询订单管理概况（禁止复述本段）】\n"
                        + businessFunction.queryOrderOverview(bizCtx)
                        + "\n请严格只基于以上数据回答用户，禁止编造任何数字，禁止推测原因，禁止提及内部过程。";
            } else if (matches(message, "销量|排行|卖得最好|卖得好|最受欢迎|热门|top10|Top10|TOP10")) {
                injected = "【系统已实时查询销量Top10（禁止复述本段）】\n"
                        + businessFunction.queryTop10(today, today, bizCtx)
                        + "\n请严格只基于以上数据回答用户，禁止编造任何数字，禁止推测原因，禁止提及内部过程。";
            } else if (matches(message, "营业状态|是否营业|营业中|打烊|开门|关门")) {
                injected = "【系统已实时查询营业状态（禁止复述本段）】\n"
                        + businessFunction.queryShopStatus(bizCtx)
                        + "\n请严格只基于以上数据回答用户，禁止编造任何数字，禁止推测原因，禁止提及内部过程。";
            }
            if (injected != null) {
                messages.add(new SystemMessage(injected));
                log.info("【兜底取数】商家端命中意图，已注入真实数据，sessionId={}", sessionId);
            } else {
                log.info("【兜底取数】商家端未命中意图，走模型自主工具调用，sessionId={}", sessionId);
            }

            StringBuilder responseBuilder = new StringBuilder();
            // 不向模型暴露任何工具：所有取数由后端兜底完成并注入，模型只能基于注入数据回答
            return chatClient.prompt()
                    .messages(messages)
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
     * 关键词宽松匹配，用于后端意图检测（命中则后端兜底取数，不依赖模型自觉调工具）
     */
    private boolean matches(String text, String pattern) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.matches(".*(" + pattern + ").*");
    }

    /**
     * 从用户输入中提取订单号（连续8位以上数字）
     */
    private String extractOrderNo(String text) {
        if (text == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{8,}").matcher(text);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    /**
     * 订单查询 Response 转注入文本
     */
    private String responseToText(Response resp) {
        if (resp == null) {
            return "查询无结果";
        }
        if (resp.getMsg() != null) {
            return resp.getMsg();
        }
        return "订单号：" + resp.getOrderNo()
                + "，订单状态：" + resp.getOrderStatus()
                + "，实收金额：" + resp.getAmount()
                + "，收货人：" + resp.getConsignee()
                + "，预计送达时间：" + resp.getEstimatedDeliveryTime();
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
     * 商家端经营助手历史接口
     *
     * 按 sessionId 读取商家端会话历史（Redis 存，userId 记为 0L），
     * 供管理端 AI 经营助手页面重新进入时恢复历史消息。
     * 不走用户归属校验：商家端历史 userId 统一为 0L，这里传 0L 使校验通过。
     */
    @GetMapping("/businessHistory")
    public Result<List<ChatMemoryRecord>> businessHistory(
            @RequestParam(value = "sessionId", defaultValue = "biz001") String sessionId) {
        List<ChatMemoryRecord> history = chatService.getChatHistory(sessionId, 0L);
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

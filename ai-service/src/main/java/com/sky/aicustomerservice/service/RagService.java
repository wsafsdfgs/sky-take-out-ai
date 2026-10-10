package com.sky.aicustomerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // RAG系统提示词
    private static final String RAG_SYS_PROMPT = """
你是外卖平台售后知识库客服，只能基于检索到的文档内容回答用户问题。
规则：
1. 只回答退款、售后、投诉相关流程；如果知识库没有对应内容，回复：该问题无法从知识库查询，请转接人工客服。
2. 禁止查询订单、获取用户订单信息，没有订单查询权限。
3. 回答简洁清晰，分步说明操作流程。
""";

    public RagService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * RAG检索 + SSE流式，带上历史对话
     * @param userMsg 用户当前提问
     * @param historyMsg 从Redis读取并转换后的历史对话消息（SpringAI Message类型）
     * @return 流式文本块
     */
    public Flux<String> ragStreamChat(String userMsg, List<Message> historyMsg) {
        // 向量检索知识库
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMsg)
                        .topK(3)
                        .build()
        );
        StringBuilder context = new StringBuilder();
        docs.forEach(doc -> context.append(doc.getText()).append("\n"));

        // 把知识库内容拼在当前用户问题前面
        String promptWithContext = """
【知识库参考内容】
%s
【用户当前问题】
%s
""".formatted(context, userMsg);

        // 组装消息：系统提示词 + 历史对话 + 当前用户问题（新建列表，不污染传入的历史列表）
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(RAG_SYS_PROMPT));
        if (historyMsg != null && !historyMsg.isEmpty()) {
            messages.addAll(historyMsg);
        }
        messages.add(new UserMessage(promptWithContext));

        // 流式输出，不带任何Tool
        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content();
    }
}

package com.sky.aicustomerservice.function;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库检索功能类
 *
 * 将 RAG 向量检索封装成 Spring AI 工具，供大模型在统一对话中自动调用。
 * 当用户询问退款、售后、投诉等知识库相关问题时，大模型会调用本工具
 * 检索知识库，而不是走独立的 ragStream 接口。
 *
 * 这样实现了"统一入口 + AI 智能路由"：查订单调 OrderQueryFunction，
 * 问售后规则调本工具，两种能力在同一会话内由 AI 自主切换。
 */
@Slf4j
@Component
public class KnowledgeBaseFunction {

    private final VectorStore vectorStore;

    public KnowledgeBaseFunction(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 大模型工具：检索售后知识库
     *
     * 根据用户的问题，在知识库中做向量检索，返回匹配的文档内容。
     * 大模型拿到检索结果后，用自然语言回答用户。
     *
     * @param question 用户想了解的问题（如退款流程、售后规则、投诉渠道等）
     * @return 检索到的知识库文档拼接文本；没有匹配则返回提示
     */
    @Tool(description = "检索外卖平台售后知识库，获取退款、售后、投诉等流程规则。务必调用本工具检索，勿凭记忆编造售后规则")
    public String queryKnowledgeBase(
            @ToolParam(description = "用户关于退款/售后/投诉/赔付等流程的问题") String question) {
        try {
            // 向量检索知识库，取最相关的 3 条
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(3)
                            .build()
            );
            if (docs == null || docs.isEmpty()) {
                return "知识库中没有找到相关内容，请告知用户该问题无法从知识库查询，可转接人工客服。";
            }
            // 拼接文档内容返回给大模型
            StringBuilder sb = new StringBuilder();
            docs.forEach(doc -> sb.append(doc.getText()).append("\n"));
            log.info("AI调用知识库检索, question={}, 命中={}条", question, docs.size());
            return "【知识库参考内容】\n" + sb;
        } catch (Exception e) {
            log.error("AI知识库检索失败", e);
            return "知识库检索失败，请告知用户稍后再试或转接人工客服。";
        }
    }
}

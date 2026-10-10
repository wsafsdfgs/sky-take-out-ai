package com.sky.aicustomerservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目启动时自动初始化RAG知识库文档
 * <p>
 * 使用内存向量库 SimpleVectorStore，项目重启后数据清空，
 * 因此每次启动时自动重新导入售后知识文档，避免手动调用接口。
 */
@Slf4j
@Component
public class RagDataInitializer implements ApplicationRunner {

    private final VectorStore vectorStore;

    public RagDataInitializer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化RAG知识库文档...");
        try {
            List<Document> documents = new ArrayList<>();

            documents.add(new Document("""
外卖退款流程：
1. 订单未送达：订单详情页点击【申请退款】，选择退款原因，提交后等待商家审核；商家超时未处理系统自动同意。
2. 订单已送达：收货30分钟内，上传问题照片，提交退款申请，商家24小时内审核。
3. 退款资金原路退回，一般1-3个工作日到账。
"""));

            documents.add(new Document("""
外卖投诉流程：
1. 订单页面点击投诉入口，选择投诉对象（商家/骑手）。
2. 填写投诉描述，上传凭证。
3. 平台客服会在24小时内处理并短信通知结果。
"""));

            documents.add(new Document("""
外卖配送说明：
1. 超时未送达：可查看骑手实时位置，超过预计时间可申请催单。
2. 订单状态说明：已接单->制作中->配送中->已送达。
3. 配送异常（骑手长时间未动）：请联系商家协商，必要时转接人工客服。
"""));

            vectorStore.add(documents);
            log.info("RAG知识库文档初始化完成，共 {} 条", documents.size());
        } catch (Exception e) {
            log.error("RAG知识库初始化失败", e);
        }
    }
}

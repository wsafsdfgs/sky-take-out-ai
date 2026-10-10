package com.sky.aicustomerservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.aicustomerservice.context.UserContext;
import com.sky.aicustomerservice.entiy.ChatMemoryRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private static final String CHAT_HISTORY_KEY_PREFIX = "chat:history:";
    // 会话过期 30分钟
    private static final long EXPIRE_MINUTES = 30;

    /**
     * 保存单条对话记录
     */
    public void saveChatRecord(String sessionId, String role, String content, long time, Long userId) {
        if(userId == null){
            log.error("保存会话失败：userId为空");
            return;
        }
        ChatMemoryRecord record = new ChatMemoryRecord();
        record.setSessionId(sessionId);
        record.setRole(role);
        record.setContent(content);
        record.setCreateTime(time);
        record.setUserId(userId);

        String redisKey = CHAT_HISTORY_KEY_PREFIX + sessionId;
        try {
            // 对象转JSON字符串存入Redis List
            String jsonStr = objectMapper.writeValueAsString(record);
            redisTemplate.opsForList().rightPush(redisKey, jsonStr);
            // 设置过期时间30分钟
            redisTemplate.expire(redisKey, EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("保存聊天记录失败 sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取会话历史，【增加归属校验：会话必须属于当前登录用户】
     * @param sessionId 会话ID
     * @param loginUserId 当前登录用户ID（由Controller传入，禁止service内部读取UserContext）
     * @return 会话记录，越权返回空集合
     */
    public List<ChatMemoryRecord> getChatHistory(String sessionId, Long loginUserId) {
        String redisKey = CHAT_HISTORY_KEY_PREFIX + sessionId;
        List<String> jsonList = redisTemplate.opsForList().range(redisKey, 0, -1);
        List<ChatMemoryRecord> result = new ArrayList<>();
        if (jsonList == null || jsonList.isEmpty()) {
            return result;
        }
        try {
            for (String json : jsonList) {
                ChatMemoryRecord record = objectMapper.readValue(json, new TypeReference<ChatMemoryRecord>() {});
                // 归属校验：会话记录的userId必须等于当前登录userId
                if (!loginUserId.equals(record.getUserId())) {
                    log.warn("越权访问会话，sessionId={}, 会话归属userId={}, 当前用户={}",
                            sessionId, record.getUserId(), loginUserId);
                    // 一旦发现任意一条不属于当前用户，直接返回空列表，拒绝全部会话数据
                    return new ArrayList<>();
                }
                result.add(record);
            }
        } catch (Exception e) {
            log.error("读取会话历史失败 sessionId={}", sessionId, e);
        }
        return result;
    }

}

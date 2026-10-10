package com.sky.aicustomerservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "redis.chat")
public class RedisProperties {

    /**
     * 对话历史键前缀
     */
    private String historyKeyPrefix = "chat:history:";

    /**
     * 对话历史过期时间（分钟）
     */
    private long historyExpireMinutes = 30;

}

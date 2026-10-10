package com.sky.aicustomerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. Key 序列化：用 String，方便在 Redis 客户端里直接看键名
        template.setKeySerializer(new StringRedisSerializer());

        // 2. Value 序列化：用 Jackson2Json，这样可以把 AI 的对话对象直接序列化成 JSON 存储
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 3. Hash 结构的 Key 和 Value 也用同样处理
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}

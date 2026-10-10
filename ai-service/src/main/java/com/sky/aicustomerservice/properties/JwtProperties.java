package com.sky.aicustomerservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties {
    private String userSecretKey;   // 绑定 sky.jwt.user-secret-key
    private long userTtl;           // 绑定 sky.jwt.user-ttl
    private String userTokenName;   // 绑定 sky.jwt.user-token-name
}

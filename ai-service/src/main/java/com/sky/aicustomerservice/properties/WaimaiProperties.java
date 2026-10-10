package com.sky.aicustomerservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "waimai")
@Data
public class WaimaiProperties {
    private String mainUrl;           // 绑定 waimai.main-url
    private String orderDetailUrl;    // 绑定 waimai.order-detail-url
}

package com.sky.aicustomerservice.config;

import com.sky.aicustomerservice.interceptor.TokenInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Resource
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/ai/**")
                .excludePathPatterns("/ai/login")            // 用户登录（还没token）
                .excludePathPatterns("/ai/businessLogin")     // 商家登录（还没token）
                .excludePathPatterns("/ai/businessStream")     // 商家对话（商家token由主项目校验，AI不解析）
                .excludePathPatterns("/ai/businessHistory");   // 商家历史（按 sessionId 读，AI 不解析 token）
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源（前端页面）不缓存，避免改完 index.html 浏览器还是旧的
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }
}

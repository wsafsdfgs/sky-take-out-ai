package com.sky.aicustomerservice.interceptor;

import com.sky.aicustomerservice.context.UserContext;
import com.sky.aicustomerservice.properties.JwtProperties;
import com.sky.aicustomerservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT令牌拦截器
 * 
 * 负责验证和处理用户请求中的JWT令牌，实现用户身份认证。
 * 主要功能：
 * 1. 从请求头或参数中提取JWT令牌
 * 2. 验证令牌的有效性和合法性
 * 3. 从令牌中解析用户ID并存储到UserContext中
 * 4. 支持本地调试模式（通过URL参数指定用户ID）
 * 5. 请求完成后清理UserContext中的数据
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Resource
    private JwtProperties jwtProperties;

    /**
     * 前置拦截方法
     * 
     * 在请求处理之前执行，负责验证JWT令牌并设置用户上下文。
     * 处理流程：
     * 1. 检查是否为本地调试模式（通过debugUserId参数）
     * 2. 从请求头或URL参数中提取JWT令牌
     * 3. 验证令牌的有效性
     * 4. 从令牌中解析用户ID并设置到UserContext中
     * 5. 返回验证结果，true表示继续处理请求，false表示拒绝请求
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 请求处理方法
     * @return true表示继续处理请求，false表示拒绝请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        // ========== 本地调试debug分支，上线务必删除 ==========
        String userIdParam = request.getParameter("debugUserId");
        if (userIdParam != null) {
            try {
                Long userId = Long.parseLong(userIdParam);
                UserContext.setUserId(userId);
                return true;
            } catch (Exception ignored) {
            }
        }
        // ================================================

        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 兼容前端EventSource：SSE无法带header，改用URL参数传token
            token = request.getParameter("token");
        }
        if (token == null) {
            log.warn("未携带token");
            response.setStatus(401);
            return false;
        }
        UserContext.setToken(token);
// 真实解析JWT
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            log.warn("token解析失败: {}", e.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    /**
     * 请求完成后的处理方法
     * 
     * 在请求处理完成后执行，负责清理用户上下文数据。
     * 无论请求是否成功处理，都会执行此方法，确保UserContext中的数据被清除，
     * 防止内存泄漏和用户信息混淆。
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 请求处理方法
     * @param ex 处理过程中发生的异常（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}

package com.sky.aicustomerservice.handler;

import com.sky.aicustomerservice.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.SocketTimeoutException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 AI 服务超时异常
     */
    @ExceptionHandler(SocketTimeoutException.class)
    public Result<String> handleTimeoutException(SocketTimeoutException e) {
        log.error("调用 AI 服务超时：{}", e.getMessage());
        return Result.error("AI 客服当前排队人数较多，请稍后再试~");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常：{}", e.getMessage());
        return Result.error("请求参数有误，请检查后重试");
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFoundException(NoResourceFoundException e) {
        // 忽略浏览器请求 favicon.ico 导致的 404，不打印堆栈日志
        return Result.error("资源不存在");
    }

    /**
     * 兜底：拦截其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统运行异常：", e);
        return Result.error("服务器开小差了，请稍后重试");
    }
}

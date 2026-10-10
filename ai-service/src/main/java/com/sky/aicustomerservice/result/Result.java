package com.sky.aicustomerservice.result;

import lombok.Data;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;

    private Result() {}

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(1, "success", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(1, "success", data);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(1, msg, data);
    }

    /**
     * 错误响应（自定义消息）
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(0, msg, null);
    }

    /**
     * 错误响应（自定义状态码和消息）
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}

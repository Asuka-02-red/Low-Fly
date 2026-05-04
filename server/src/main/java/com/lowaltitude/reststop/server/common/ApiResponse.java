package com.lowaltitude.reststop.server.common;

/**
 * 统一API响应包装类。
 * <p>
 * 封装所有REST接口的返回格式，包含状态码、消息、数据体和时间戳，
 * 确保前端接收到的响应结构一致。
 * </p>
 *
 * @param <T> 响应数据的泛型类型
 */
public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, System.currentTimeMillis());
    }
}

package com.example.low_altitudereststop.core.network;

/**
 * API统一响应信封模型，封装业务接口的标准返回格式，包含状态码、消息、业务数据和时间戳。
 *
 * @param <T> 业务数据的类型
 */
public class ApiEnvelope<T> {
    public int code;
    public String message;
    public T data;
    public long timestamp;
}


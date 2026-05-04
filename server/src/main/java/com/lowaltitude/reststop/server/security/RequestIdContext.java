package com.lowaltitude.reststop.server.security;

/**
 * 请求标识上下文持有者。
 * <p>
 * 基于 ThreadLocal 在当前线程中存储和获取请求唯一标识（X-Request-Id），
 * 用于全链路日志追踪和审计关联，请求结束后需及时清理。
 * </p>
 */
public final class RequestIdContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private RequestIdContext() {
    }

    public static void set(String requestId) {
        HOLDER.set(requestId);
    }

    public static String get() {
        String requestId = HOLDER.get();
        return requestId == null ? "" : requestId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}


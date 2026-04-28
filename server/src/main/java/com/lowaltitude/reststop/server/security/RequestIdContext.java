package com.lowaltitude.reststop.server.security;

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


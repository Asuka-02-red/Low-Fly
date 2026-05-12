package com.example.low_altitudereststop.core.network;

import com.example.low_altitudereststop.core.session.SessionStore;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 认证拦截器，自动为HTTP请求头添加Bearer Token授权信息，
 * 从SessionStore中读取当前访问令牌并注入请求头。
 */
public class AuthInterceptor implements Interceptor {

    private final SessionStore sessionStore;

    public AuthInterceptor(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = sessionStore.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return chain.proceed(original);
        }
        Request authed = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(authed);
    }
}


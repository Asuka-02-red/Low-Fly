package com.example.low_altitudereststop.core.network;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 请求重试拦截器，在网络超时、连接失败或服务端5xx错误时自动重试请求，
 * 采用指数退避策略避免频繁重试。
 */
public class RetryInterceptor implements Interceptor {

    private static final int MAX_RETRY_COUNT = 2;
    private static final long[] BACKOFF_MS = {500L, 1000L};

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        int retryCount = 0;
        IOException lastException = null;

        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                Request.Builder requestBuilder = request.newBuilder();
                if (retryCount > 0) {
                    requestBuilder.header("X-Retry-Count", String.valueOf(retryCount));
                }
                Response response = chain.proceed(requestBuilder.build());
                if (response.isSuccessful() || response.code() < 500) {
                    return response;
                }
                response.close();
                if (retryCount >= MAX_RETRY_COUNT) {
                    return chain.proceed(request);
                }
            } catch (SocketTimeoutException | UnknownHostException | java.net.ConnectException e) {
                lastException = e;
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw lastException;
                }
            }
            retryCount++;
            try {
                Thread.sleep(BACKOFF_MS[Math.min(retryCount - 1, BACKOFF_MS.length - 1)]);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                if (lastException != null) {
                    throw lastException;
                }
                throw new IOException("Retry interrupted", ie);
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return chain.proceed(request);
    }
}

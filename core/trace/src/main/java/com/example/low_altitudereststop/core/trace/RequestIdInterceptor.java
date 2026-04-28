package com.example.low_altitudereststop.core.trace;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RequestIdInterceptor implements Interceptor {

    private final OperationLogStore logStore;

    public RequestIdInterceptor(OperationLogStore logStore) {
        this.logStore = logStore;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String requestId = logStore.newRequestId();
        String path = original.url().encodedPath();
        logStore.appendHttp(requestId, original.method(), path);
        Request withId = original.newBuilder()
                .header("X-Request-Id", requestId)
                .build();
        return chain.proceed(withId);
    }
}


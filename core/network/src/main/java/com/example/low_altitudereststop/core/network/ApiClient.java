package com.example.low_altitudereststop.core.network;

import android.content.Context;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.core.trace.RequestIdInterceptor;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API客户端单例工厂，负责创建和提供已认证与未认证的Retrofit服务实例，
 * 统一配置OkHttpClient的超时、重试、日志、认证拦截器等参数。
 */
public final class ApiClient {

    private static volatile ApiService publicService;
    private static volatile ApiService authedService;
    private static volatile boolean mockInitialized = false;

    private ApiClient() {
    }

    public static ApiService getPublicService(Context context) {
        if (publicService == null) {
            synchronized (ApiClient.class) {
                if (publicService == null) {
                    ensureMockState(context);
                    Context app = context.getApplicationContext();
                    publicService = new Retrofit.Builder()
                            .baseUrl(requireBaseUrl())
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(buildPublicClient(app))
                            .build()
                            .create(ApiService.class);
                }
            }
        }
        return publicService;
    }

    public static ApiService getAuthedService(Context context) {
        if (authedService == null) {
            synchronized (ApiClient.class) {
                if (authedService == null) {
                    ensureMockState(context);
                    SessionStore store = new SessionStore(context.getApplicationContext());
                    authedService = new Retrofit.Builder()
                            .baseUrl(requireBaseUrl())
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(buildAuthedClient(context, store))
                            .build()
                            .create(ApiService.class);
                }
            }
        }
        return authedService;
    }

    private static void ensureMockState(Context context) {
        if (mockInitialized) {
            return;
        }
        mockInitialized = true;
        try {
            Class<?> buildConfig = Class.forName(
                    context.getApplicationContext().getPackageName() + ".BuildConfig");
            java.lang.reflect.Field field = buildConfig.getField("IS_DEMO_MODE");
            boolean isDemoMode = field.getBoolean(null);
            MockInterceptor.setEnabled(isDemoMode);
        } catch (Exception ignored) {
            MockInterceptor.setEnabled(false);
        }
    }

    private static String requireBaseUrl() {
        String url = ApiConfig.getBaseUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("API baseUrl is not initialized");
        }
        return url;
    }

    private static OkHttpClient buildPublicClient(Context context) {
        OkHttpClient.Builder builder = baseBuilder(context, 8, 15, 8, 20)
                .addInterceptor(new RetryInterceptor());
        if (MockInterceptor.isEnabled()) {
            builder.addInterceptor(new MockInterceptor());
        }
        builder.addInterceptor(buildLoggingInterceptor(context));
        return builder.build();
    }

    private static OkHttpClient buildAuthedClient(Context context, SessionStore store) {
        OkHttpClient.Builder builder = baseBuilder(context, 10, 15, 10, 30)
                .addInterceptor(new RetryInterceptor());
        if (MockInterceptor.isEnabled()) {
            builder.addInterceptor(new MockInterceptor());
        }
        builder.addInterceptor(new AuthInterceptor(store))
                .authenticator(new TokenAuthenticator(context, store))
                .addInterceptor(buildLoggingInterceptor(context));
        return builder.build();
    }

    private static OkHttpClient.Builder baseBuilder(Context context,
                                                    int connectTimeoutSec,
                                                    int readTimeoutSec,
                                                    int writeTimeoutSec,
                                                    int callTimeoutSec) {
        return new OkHttpClient.Builder()
                .addInterceptor(new RequestIdInterceptor(new OperationLogStore(context)))
                .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSec, TimeUnit.SECONDS)
                .callTimeout(callTimeoutSec, TimeUnit.SECONDS);
    }

    private static HttpLoggingInterceptor buildLoggingInterceptor(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        boolean enableHttpLogging = false;
        try {
            Class<?> buildConfig = Class.forName(
                    context.getApplicationContext().getPackageName() + ".BuildConfig");
            java.lang.reflect.Field field = buildConfig.getField("ENABLE_HTTP_LOGGING");
            enableHttpLogging = field.getBoolean(null);
        } catch (Exception ignored) {
        }
        logging.setLevel(enableHttpLogging
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);
        return logging;
    }
}

package com.example.low_altitudereststop.feature.ai.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.example.low_altitudereststop.feature.ai.model.AiConversationModels;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class AiLlmRepository {

    private static final String TAG = "AiLlmRepository";
    static final int MAX_REPLY_LENGTH = 240;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface ResultCallback {
        void onSuccess(@NonNull String answer);
        void onError(@NonNull String message);
    }

    private static volatile AiLlmRepository instance;
    private final AiLlmConfig config;
    private final OkHttpClient client;
    private final Gson gson;
    @Nullable
    private final AiLlmApiService apiService;

    private AiLlmRepository() {
        this.config = AiLlmConfig.fromBuild();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        if (AiLlmConfig.PROVIDER_XFYUN_SPARK.equals(config.provider)) {
            this.apiService = null;
        } else {
            this.apiService = new retrofit2.Retrofit.Builder()
                    .baseUrl(config.baseUrl)
                    .client(client)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                    .create(AiLlmApiService.class);
        }
    }

    @NonNull
    public static AiLlmRepository getInstance() {
        if (instance == null) {
            synchronized (AiLlmRepository.class) {
                if (instance == null) {
                    instance = new AiLlmRepository();
                }
            }
        }
        return instance;
    }

    public void ask(@NonNull String prompt, @NonNull ResultCallback callback) {
        Log.d(TAG, "ask provider=" + config.provider + " interface=" + config.interfaceType + " model=" + config.modelName);
        if (!config.isRemoteConfigured()) {
            callback.onSuccess(trimReply("AI 助手已收到你的问题：" + prompt + "。当前未注入真实免费模型密钥，已返回本地占位结果。"));
            return;
        }
        if (AiLlmConfig.PROVIDER_XFYUN_SPARK.equals(config.provider)) {
            askSpark(prompt, callback);
            return;
        }
        askGeneric(prompt, callback);
    }

    private void askGeneric(@NonNull String prompt, @NonNull ResultCallback callback) {
        if (apiService == null) {
            callback.onError("AI 服务初始化失败");
            return;
        }
        AiConversationModels.LlmRequest request = new AiConversationModels.LlmRequest(config.modelName);
        request.messages.add(new AiConversationModels.ChatMessage("system", "你是低空作业调度与合规助手，请用中文给出简明可执行建议。"));
        request.messages.add(new AiConversationModels.ChatMessage("user", prompt));
        apiService.chatCompletion("Bearer " + config.apiKey, request).enqueue(new retrofit2.Callback<AiConversationModels.LlmResponse>() {
            @Override
            public void onResponse(retrofit2.Call<AiConversationModels.LlmResponse> call, retrofit2.Response<AiConversationModels.LlmResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(resolveHttpError(response.code()));
                    return;
                }
                AiConversationModels.LlmResponse body = response.body();
                if (body.error != null && body.error.message != null && !body.error.message.trim().isEmpty()) {
                    callback.onError(trimReply(body.error.message.trim()));
                    return;
                }
                String reply = body.extractReply();
                if (reply.isEmpty()) {
                    callback.onError("模型未返回有效结果");
                    return;
                }
                callback.onSuccess(trimReply(reply));
            }

            @Override
            public void onFailure(retrofit2.Call<AiConversationModels.LlmResponse> call, Throwable t) {
                callback.onError(resolveThrowableMessage(t));
            }
        });
    }

    private void askSpark(@NonNull String prompt, @NonNull ResultCallback callback) {
        if (!AiLlmConfig.INTERFACE_HTTP_OPENAPI.equals(config.interfaceType)) {
            callback.onError("当前版本暂未启用讯飞 WebSocket 协议，请先使用开放接口地址");
            return;
        }
        String model = config.resolveSparkModel();
        AiConversationModels.LlmRequest request = new AiConversationModels.LlmRequest(model);
        request.messages.add(new AiConversationModels.ChatMessage("system", "你是低空作业调度与合规助手，请用中文给出简明、可执行、符合合规要求的建议。"));
        request.messages.add(new AiConversationModels.ChatMessage("user", prompt));
        RequestBody body = RequestBody.create(gson.toJson(request), JSON);
        Request httpRequest = new Request.Builder()
                .url(config.baseUrl)
                .addHeader("Authorization", "Bearer " + config.apiPassword)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        Log.d(TAG, "Spark request model=" + model + " endpoint=" + config.baseUrl);
        client.newCall(httpRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Spark request failure", e);
                callback.onError(resolveThrowableMessage(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                try (okhttp3.ResponseBody responseBody = response.body()) {
                    Log.d(TAG, "Spark response code=" + response.code() + " model=" + model);
                    if (responseBody == null) {
                        callback.onError(resolveHttpError(response.code()));
                        return;
                    }
                    String payload = responseBody.string();
                    if (!response.isSuccessful()) {
                        callback.onError(resolveSparkHttpError(response.code(), payload));
                        return;
                    }
                    AiConversationModels.LlmResponse result = gson.fromJson(payload, AiConversationModels.LlmResponse.class);
                    if (result == null) {
                        callback.onError("AI 服务返回了无法解析的数据");
                        return;
                    }
                    if (result.hasSparkError()) {
                        String errorMessage = result.extractErrorMessage();
                        callback.onError(trimReply(errorMessage.isEmpty() ? "AI 服务暂不可用" : errorMessage));
                        return;
                    }
                    String errorMessage = result.extractErrorMessage();
                    if (!errorMessage.isEmpty() && result.extractReply().isEmpty()) {
                        callback.onError(trimReply(errorMessage));
                        return;
                    }
                    String reply = result.extractReply();
                    if (reply.isEmpty()) {
                        callback.onError("模型未返回有效结果");
                        return;
                    }
                    callback.onSuccess(trimReply(reply));
                } catch (Exception exception) {
                    callback.onError(resolveThrowableMessage(exception));
                }
            }
        });
    }

    @NonNull
    public static String trimReply(@NonNull String reply) {
        String normalized = reply.trim();
        if (normalized.length() <= MAX_REPLY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_REPLY_LENGTH) + "...";
    }

    @NonNull
    public static String resolveHttpError(int code) {
        if (code == 401 || code == 403) {
            return "AI 服务鉴权失败，请检查密钥配置";
        }
        if (code == 408 || code == 504) {
            return "AI 服务响应超时，请稍后重试";
        }
        if (code == 429) {
            return "AI 服务请求过于频繁，请稍后再试";
        }
        if (code >= 500) {
            return "AI 服务暂时繁忙，请稍后重试";
        }
        return "AI 服务暂不可用";
    }

    @NonNull
    public static String resolveThrowableMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return "模型请求失败";
        }
        String message = throwable.getMessage().toLowerCase();
        if (message.contains("timeout")) {
            return "AI 服务响应超时，请稍后重试";
        }
        if (message.contains("unable to resolve host") || message.contains("failed to connect")) {
            return "网络连接异常，请检查网络后重试";
        }
        return trimReply(throwable.getMessage());
    }

    @NonNull
    private String resolveSparkHttpError(int code, @Nullable String payload) {
        String payloadMessage = extractMessageFromPayload(payload);
        if (!payloadMessage.isEmpty()) {
            return trimReply(payloadMessage);
        }
        return resolveHttpError(code);
    }

    @NonNull
    private String extractMessageFromPayload(@Nullable String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return "";
        }
        try {
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message") && !error.get("message").isJsonNull()) {
                    String value = error.get("message").getAsString();
                    if (!value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
            if (root.has("message") && !root.get("message").isJsonNull()) {
                String value = root.get("message").getAsString();
                if (!value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to parse Spark error payload", exception);
        }
        return "";
    }
}

package com.example.low_altitudereststop.feature.ai.network;

import androidx.annotation.NonNull;
import com.example.low_altitudereststop.BuildConfig;
import com.example.low_altitudereststop.core.network.ApiConfig;
import java.util.LinkedHashSet;
import java.util.Locale;

public final class AiLlmConfig {

    public static final String PROVIDER_XFYUN_SPARK = "xfyun_spark";
    public static final String INTERFACE_HTTP_OPENAPI = "http_openapi";
    public static final String INTERFACE_WEBSOCKET = "websocket";

    public final String provider;
    public final String baseUrl;
    public final String modelName;
    public final String apiKey;
    public final String apiSecret;
    public final String apiPassword;
    public final String appId;
    public final String interfaceType;

    private AiLlmConfig(
            String provider,
            String baseUrl,
            String modelName,
            String apiKey,
            String apiSecret,
            String apiPassword,
            String appId,
            String interfaceType
    ) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiPassword = apiPassword;
        this.appId = appId;
        this.interfaceType = interfaceType;
    }

    @NonNull
    public static AiLlmConfig fromBuild() {
        return new AiLlmConfig(
                BuildConfig.LLM_PROVIDER,
                ensureTrailingSlash(ApiConfig.getLlmBaseUrl()),
                BuildConfig.LLM_MODEL_NAME,
                BuildConfig.LLM_API_KEY,
                BuildConfig.LLM_API_SECRET,
                BuildConfig.LLM_API_PASSWORD,
                BuildConfig.LLM_APP_ID,
                BuildConfig.LLM_INTERFACE_TYPE
        );
    }

    public boolean isRemoteConfigured() {
        if (PROVIDER_XFYUN_SPARK.equals(provider)) {
            if (INTERFACE_HTTP_OPENAPI.equals(interfaceType)) {
                return baseUrl.startsWith("https://")
                        && !baseUrl.contains("example.com")
                        && apiPassword != null
                        && !apiPassword.trim().isEmpty()
                        && modelName != null
                        && !modelName.trim().isEmpty();
            }
            if (INTERFACE_WEBSOCKET.equals(interfaceType)) {
                return baseUrl.startsWith("wss://")
                        && apiKey != null
                        && !apiKey.trim().isEmpty()
                        && apiSecret != null
                        && !apiSecret.trim().isEmpty()
                        && appId != null
                        && !appId.trim().isEmpty();
            }
        }
        return baseUrl.startsWith("https://")
                && !baseUrl.contains("example.com")
                && apiKey != null
                && !apiKey.trim().isEmpty();
    }

    @NonNull
    private static String ensureTrailingSlash(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "https://llm.example.com/v1/";
        }
        if (url.startsWith("https://spark-api-open.xf-yun.com/")) {
            return url;
        }
        if (url.startsWith("wss://")) {
            return url;
        }
        return url.endsWith("/") ? url : (url + "/");
    }

    @NonNull
    public String resolveSparkModel() {
        String normalized = normalizeSparkModel(modelName);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return defaultSparkModel();
    }

    @NonNull
    public String[] resolveSparkModelCandidates() {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(resolveSparkModel());
        candidates.add(defaultSparkModel());
        if (baseUrl.contains("/x2/")) {
            candidates.add("x1");
        }
        if (baseUrl.contains("/v2/")) {
            candidates.add("generalv3.5");
        }
        return candidates.toArray(new String[0]);
    }

    @NonNull
    private String normalizeSparkModel(String rawModelName) {
        if (rawModelName == null) {
            return "";
        }
        String trimmed = rawModelName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String normalized = trimmed
                .toLowerCase(Locale.US)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
        if ("sparkx".equals(normalized)
                || "spark".equals(normalized)
                || "x".equals(normalized)
                || "x1".equals(normalized)
                || "x15".equals(normalized)
                || "sparkx15".equals(normalized)) {
            return "spark-x";
        }
        if ("lite".equals(normalized) || "4.0ultra".equals(normalized) || "generalv3.5".equals(trimmed)) {
            return trimmed;
        }
        return trimmed;
    }

    @NonNull
    private String defaultSparkModel() {
        return "spark-x";
    }
}

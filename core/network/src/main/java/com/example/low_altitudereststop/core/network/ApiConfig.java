package com.example.low_altitudereststop.core.network;

public final class ApiConfig {

    private static volatile String baseUrl;
    private static volatile String llmBaseUrl;
    private static volatile String llmModelName;

    private ApiConfig() {
    }

    public static void init(String url, String llmUrl, String llmModel) {
        baseUrl = url;
        llmBaseUrl = llmUrl;
        llmModelName = llmModel;
    }

    public static String getBaseUrl() {
        String url = baseUrl;
        return url == null ? "" : url;
    }

    public static String getLlmBaseUrl() {
        String url = llmBaseUrl;
        return url == null ? "" : url;
    }

    public static String getLlmModelName() {
        String model = llmModelName;
        return model == null ? "" : model;
    }
}


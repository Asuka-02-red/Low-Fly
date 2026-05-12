package com.example.low_altitudereststop.feature.ai.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import org.junit.Test;

public class AiLlmConfigTest {

    @Test
    public void remoteConfig_requiresApiPasswordForSparkOpenApi() throws Exception {
        Constructor<AiLlmConfig> constructor = AiLlmConfig.class.getDeclaredConstructor(
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        constructor.setAccessible(true);

        AiLlmConfig invalid = constructor.newInstance(
                AiLlmConfig.PROVIDER_XFYUN_SPARK,
                "https://spark-api-open.xf-yun.com/x2/chat/completions",
                "Spark X",
                "",
                "",
                "",
                "appid",
                AiLlmConfig.INTERFACE_HTTP_OPENAPI
        );
        AiLlmConfig valid = constructor.newInstance(
                AiLlmConfig.PROVIDER_XFYUN_SPARK,
                "https://spark-api-open.xf-yun.com/x2/chat/completions",
                "Spark X",
                "key",
                "secret",
                "password",
                "appid",
                AiLlmConfig.INTERFACE_HTTP_OPENAPI
        );

        assertFalse(invalid.isRemoteConfigured());
        assertTrue(valid.isRemoteConfigured());
    }

    @Test
    public void repositoryHelpers_normalizeErrorsAndTrimReply() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            builder.append('a');
        }
        String trimmed = AiLlmRepository.trimReply(builder.toString());
        assertTrue(trimmed.endsWith("..."));
        assertTrue(AiLlmRepository.resolveHttpError(429).contains("频繁"));
        assertTrue(AiLlmRepository.resolveThrowableMessage(new RuntimeException("timeout")).contains("超时"));
    }

    @Test
    public void sparkModelDisplayName_mapsToOpenApiModelCandidates() throws Exception {
        Constructor<AiLlmConfig> constructor = AiLlmConfig.class.getDeclaredConstructor(
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        constructor.setAccessible(true);

        AiLlmConfig config = constructor.newInstance(
                AiLlmConfig.PROVIDER_XFYUN_SPARK,
                "https://spark-api-open.xf-yun.com/x2/chat/completions",
                "Spark X",
                "key",
                "secret",
                "password",
                "appid",
                AiLlmConfig.INTERFACE_HTTP_OPENAPI
        );

        assertEquals("spark-x", config.resolveSparkModel());
        String[] candidates = config.resolveSparkModelCandidates();
        assertTrue(candidates.length >= 1);
        assertEquals("spark-x", candidates[0]);
    }
}

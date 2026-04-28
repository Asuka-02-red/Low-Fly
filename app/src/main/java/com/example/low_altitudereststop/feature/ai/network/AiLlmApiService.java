package com.example.low_altitudereststop.feature.ai.network;

import com.example.low_altitudereststop.feature.ai.model.AiConversationModels;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AiLlmApiService {

    @POST("chat/completions")
    Call<AiConversationModels.LlmResponse> chatCompletion(
            @Header("Authorization") String authorization,
            @Body AiConversationModels.LlmRequest request
    );
}

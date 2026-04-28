package com.example.low_altitudereststop.feature.ai.model;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;

public class AiConversationModelsTest {

    @Test
    public void extractReply_prefersDirectReply() {
        AiConversationModels.LlmResponse response = new AiConversationModels.LlmResponse();
        response.reply = "直接答案";
        response.output_text = "备用答案";
        assertEquals("直接答案", response.extractReply());
    }

    @Test
    public void extractReply_fallsBackToChoiceMessage() {
        AiConversationModels.LlmResponse response = new AiConversationModels.LlmResponse();
        AiConversationModels.Choice choice = new AiConversationModels.Choice();
        choice.message = new AiConversationModels.ChatMessage("assistant", "候选答案");
        response.choices = Collections.singletonList(choice);
        assertEquals("候选答案", response.extractReply());
    }
}

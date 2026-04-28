package com.example.low_altitudereststop.feature.ai.model;

import java.util.ArrayList;
import java.util.List;

public final class AiConversationModels {

    public static final int SPARK_SUCCESS_CODE = 0;

    private AiConversationModels() {
    }

    public static final class ChatMessage {
        public String role;
        public String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static final class LlmRequest {
        public String model;
        public Thinking thinking;
        public List<ChatMessage> messages = new ArrayList<>();

        public LlmRequest(String model) {
            this.model = model;
            this.thinking = new Thinking("disabled");
        }
    }

    public static final class LlmResponse {
        public Integer code;
        public String message;
        public String sid;
        public String reply;
        public String output_text;
        public ErrorPayload error;
        public List<Choice> choices;

        public boolean hasSparkError() {
            return code != null && code != SPARK_SUCCESS_CODE;
        }

        public String extractErrorMessage() {
            if (error != null && error.message != null && !error.message.trim().isEmpty()) {
                return error.message.trim();
            }
            if (message != null && !message.trim().isEmpty()) {
                return message.trim();
            }
            return "";
        }

        public String extractReply() {
            if (reply != null && !reply.trim().isEmpty()) {
                return reply.trim();
            }
            if (output_text != null && !output_text.trim().isEmpty()) {
                return output_text.trim();
            }
            if (choices != null) {
                for (Choice choice : choices) {
                    if (choice == null || choice.message == null || choice.message.content == null) {
                        continue;
                    }
                    String value = choice.message.content.trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
            return "";
        }
    }

    public static final class Choice {
        public ChatMessage message;
    }

    public static final class Thinking {
        public String type;

        public Thinking(String type) {
            this.type = type;
        }
    }

    public static final class ErrorPayload {
        public String code;
        public String message;
    }
}

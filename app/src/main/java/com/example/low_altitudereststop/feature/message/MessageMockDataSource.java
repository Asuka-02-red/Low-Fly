package com.example.low_altitudereststop.feature.message;

import androidx.annotation.NonNull;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.feature.message.local.EnterpriseProfileEntity;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.feature.message.local.PilotProfileEntity;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.List;

public final class MessageMockDataSource {

    private MessageMockDataSource() {
    }

    @NonNull
    public static List<PlatformModels.MessageConversationView> buildConversationSummaries(@NonNull UserRole role) {
        return AppScenarioMapper.buildConversationSummaries(role);
    }

    @NonNull
    public static List<MessageEntity> buildMessageEntities(@NonNull UserRole role) {
        return AppScenarioMapper.buildMessageEntities(role);
    }

    @NonNull
    public static List<PilotProfileEntity> buildPilotProfiles() {
        return AppScenarioMapper.buildPilotProfiles();
    }

    @NonNull
    public static List<EnterpriseProfileEntity> buildEnterpriseProfiles() {
        return AppScenarioMapper.buildEnterpriseProfiles();
    }

    public static boolean isMockConversation(long conversationId) {
        return AppScenarioMapper.isScenarioConversation(conversationId);
    }

    @NonNull
    public static MessageEntity buildOutgoingMessage(long conversationId, @NonNull String content, @NonNull UserRole role) {
        return AppScenarioMapper.buildOutgoingMessage(conversationId, content, role);
    }
}

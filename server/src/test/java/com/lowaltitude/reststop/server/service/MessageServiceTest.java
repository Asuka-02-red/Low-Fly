package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.MessageConversationMapper;
import com.lowaltitude.reststop.server.mapper.MessageEntryMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class MessageServiceTest {

    private MessageService messageService;
    private MessageConversationMapper messageConversationMapper;
    private MessageEntryMapper messageEntryMapper;
    private UserAccountMapper userAccountMapper;
    private TaskService taskService;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        messageConversationMapper = Mockito.mock(MessageConversationMapper.class);
        messageEntryMapper = Mockito.mock(MessageEntryMapper.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        taskService = Mockito.mock(TaskService.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        messageService = new MessageService(messageConversationMapper, messageEntryMapper, userAccountMapper, taskService, auditLogService);
    }

    @Test
    public void shouldSendMessageBetweenEnterpriseAndPilot() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(101L);
        conversation.setTitle("长江沿线巡检 协同沟通");
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(java.util.List.of(
                buildUser(1L, "pilot_demo", "PILOT"),
                buildUser(2L, "enterprise_demo", "ENTERPRISE")
        ));
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setTitle("长江沿线巡检");
        task.setLocation("重庆江北区");
        task.setTaskType("INSPECTION");
        Mockito.when(taskService.getTaskById(101L)).thenReturn(task);
        Mockito.doAnswer(invocation -> {
            MessageEntryEntity entity = invocation.getArgument(0);
            entity.setId(7001L);
            return 1;
        }).when(messageEntryMapper).insert(ArgumentMatchers.any(MessageEntryEntity.class));
        Mockito.when(messageEntryMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of(
                buildMessageEntry(7001L, 5001L, 1L, "PILOT", "收到，今晚 19:00 前反馈航线确认。")
        ));

        ApiDtos.MessageThreadView thread = messageService.sendMessage(
                pilot,
                5001L,
                new ApiDtos.MessageSendRequest("收到，今晚 19:00 前反馈航线确认。")
        );

        Assertions.assertEquals(5001L, thread.conversationId());
        Assertions.assertEquals(1, thread.messages().size());
        Assertions.assertTrue(thread.messages().get(0).mine());
    }

    @Test
    public void shouldSyncReadReceiptsForConversationMembers() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        MessageEntryEntity entry = buildMessageEntry(7001L, 5001L, 2L, "ENTERPRISE", "请确认执行窗口");
        Mockito.when(messageEntryMapper.selectById(7001L)).thenReturn(entry);
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);

        ApiDtos.MessageReadReceiptResponse result = messageService.syncReadReceipts(
                pilot,
                new ApiDtos.MessageReadReceiptRequest(java.util.List.of(7001L))
        );

        Assertions.assertEquals(1, result.successCount());
        Assertions.assertEquals(7001L, result.syncedMsgIds().get(0));
    }

    @Test
    public void shouldResolvePilotAndEnterpriseProfilesByUid() {
        UserAccountEntity pilot = buildUser(1L, "pilot_demo", "PILOT");
        pilot.setRealName("张飞手");
        UserAccountEntity enterprise = buildUser(2L, "enterprise_demo", "ENTERPRISE");
        enterprise.setCompanyName("低空测试企业");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(pilot);
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(enterprise);

        ApiDtos.PilotProfileView pilotProfile = messageService.getPilotProfile(
                new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业"),
                "1"
        );
        ApiDtos.EnterpriseInfoView enterpriseInfo = messageService.getEnterpriseInfo(
                new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手"),
                "2"
        );

        Assertions.assertEquals("1", pilotProfile.uid());
        Assertions.assertEquals("张飞手", pilotProfile.name());
        Assertions.assertEquals("2", enterpriseInfo.uid());
        Assertions.assertEquals("低空测试企业", enterpriseInfo.companyName());
    }

    private UserAccountEntity buildUser(Long id, String username, String role) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("{noop}demo123");
        user.setPhone("13800138000");
        user.setRole(role);
        user.setRealName("测试用户");
        user.setCompanyName("测试企业");
        return user;
    }

    private MessageEntryEntity buildMessageEntry(Long id, Long conversationId, Long senderId, String role, String content) {
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setId(id);
        entry.setConversationId(conversationId);
        entry.setSenderUserId(senderId);
        entry.setSenderRole(role);
        entry.setContent(content);
        entry.setCreateTime(LocalDateTime.of(2026, 4, 22, 18, 30));
        return entry;
    }
}

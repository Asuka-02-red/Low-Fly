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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class MessageServiceComprehensiveTest {

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
    public void shouldListConversationsForPilot() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        MessageConversationEntity conversation = buildConversation(5001L, 2L, 1L, null, "协同沟通");
        Mockito.when(messageConversationMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(conversation));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(
                buildUser(1L, "pilot_demo", "PILOT"),
                buildUser(2L, "enterprise_demo", "ENTERPRISE")
        ));
        Mockito.when(messageEntryMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);

        List<ApiDtos.MessageConversationView> conversations = messageService.listMessageConversations(pilot);

        Assertions.assertEquals(1, conversations.size());
    }

    @Test
    public void shouldReturnEmptyForAdminConversations() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        List<ApiDtos.MessageConversationView> conversations = messageService.listMessageConversations(admin);
        Assertions.assertTrue(conversations.isEmpty());
    }

    @Test
    public void shouldSyncReadReceiptsWithEmptyList() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        ApiDtos.MessageReadReceiptResponse result = messageService.syncReadReceipts(
                pilot,
                new ApiDtos.MessageReadReceiptRequest(List.of())
        );
        Assertions.assertEquals(0, result.successCount());
    }

    @Test
    public void shouldSyncReadReceiptsWithNonExistentMsg() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        Mockito.when(messageEntryMapper.selectById(999L)).thenReturn(null);
        ApiDtos.MessageReadReceiptResponse result = messageService.syncReadReceipts(
                pilot,
                new ApiDtos.MessageReadReceiptRequest(List.of(999L))
        );
        Assertions.assertEquals(0, result.successCount());
    }

    @Test
    public void shouldGetPilotProfileByUsername() {
        SessionUser user = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        UserAccountEntity pilot = buildUser(1L, "pilot_demo", "PILOT");
        pilot.setRealName("张飞手");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(pilot);

        ApiDtos.PilotProfileView profile = messageService.getPilotProfile(user, "pilot_demo");

        Assertions.assertEquals("张飞手", profile.name());
    }

    @Test
    public void shouldGetEnterpriseInfoWithFallbackName() {
        SessionUser user = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        UserAccountEntity enterprise = buildUser(2L, "enterprise_demo", "ENTERPRISE");
        enterprise.setCompanyName("");
        enterprise.setRealName("测试企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(enterprise);

        ApiDtos.EnterpriseInfoView info = messageService.getEnterpriseInfo(user, "2");

        Assertions.assertEquals("测试企业", info.companyName());
    }

    private MessageConversationEntity buildConversation(Long id, Long enterpriseId, Long pilotId, Long taskId, String title) {
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(id);
        conversation.setEnterpriseId(enterpriseId);
        conversation.setPilotId(pilotId);
        conversation.setTaskId(taskId);
        conversation.setTitle(title);
        conversation.setLastMessageTime(LocalDateTime.now());
        return conversation;
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
}

package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
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

public class MessageServiceExtendedTest {

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
    public void shouldListConversationsForEnterprise() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        MessageConversationEntity conversation = buildConversation(5001L, 2L, 1L, 101L, "协同沟通");
        Mockito.when(messageConversationMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(conversation));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(
                buildUser(1L, "pilot_demo", "PILOT"),
                buildUser(2L, "enterprise_demo", "ENTERPRISE")
        ));
        MessageEntryEntity latest = buildMessageEntry(7001L, 5001L, 1L, "PILOT", "最新消息");
        Mockito.when(messageEntryMapper.selectOne(ArgumentMatchers.any())).thenReturn(latest);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        com.lowaltitude.reststop.server.entity.TaskEntity task = new com.lowaltitude.reststop.server.entity.TaskEntity();
        task.setId(101L);
        task.setLocation("重庆");
        task.setTaskType("INSPECTION");
        Mockito.when(taskService.getTaskById(101L)).thenReturn(task);

        List<ApiDtos.MessageConversationView> conversations = messageService.listMessageConversations(enterprise);

        Assertions.assertEquals(1, conversations.size());
        Assertions.assertEquals("最新消息", conversations.get(0).lastMessagePreview());
    }

    @Test
    public void shouldRejectAccessToOtherConversation() {
        SessionUser otherPilot = new SessionUser(99L, "other_pilot", RoleType.PILOT, "其他飞手");
        MessageConversationEntity conversation = buildConversation(5001L, 2L, 1L, 101L, "协同沟通");
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);

        Assertions.assertThrows(BizException.class, () -> messageService.getMessageThread(otherPilot, 5001L));
    }

    @Test
    public void shouldRejectSendingToOtherConversation() {
        SessionUser otherPilot = new SessionUser(99L, "other_pilot", RoleType.PILOT, "其他飞手");
        MessageConversationEntity conversation = buildConversation(5001L, 2L, 1L, 101L, "协同沟通");
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);

        Assertions.assertThrows(BizException.class, () -> messageService.sendMessage(
                otherPilot, 5001L, new ApiDtos.MessageSendRequest("测试消息")));
    }

    @Test
    public void shouldGetPilotProfile() {
        SessionUser user = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        UserAccountEntity pilot = buildUser(1L, "pilot_demo", "PILOT");
        pilot.setRealName("张飞手");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(pilot);

        ApiDtos.PilotProfileView profile = messageService.getPilotProfile(user, "1");

        Assertions.assertEquals("1", profile.uid());
        Assertions.assertEquals("张飞手", profile.name());
    }

    @Test
    public void shouldRejectPilotProfileForNonPilot() {
        SessionUser user = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        UserAccountEntity enterprise = buildUser(2L, "enterprise_demo", "ENTERPRISE");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(enterprise);

        Assertions.assertThrows(BizException.class, () -> messageService.getPilotProfile(user, "2"));
    }

    @Test
    public void shouldGetEnterpriseInfo() {
        SessionUser user = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        UserAccountEntity enterprise = buildUser(2L, "enterprise_demo", "ENTERPRISE");
        enterprise.setCompanyName("低空测试企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(enterprise);

        ApiDtos.EnterpriseInfoView info = messageService.getEnterpriseInfo(user, "2");

        Assertions.assertEquals("2", info.uid());
        Assertions.assertEquals("低空测试企业", info.companyName());
    }

    @Test
    public void shouldRejectEnterpriseInfoForNonEnterprise() {
        SessionUser user = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        UserAccountEntity pilot = buildUser(1L, "pilot_demo", "PILOT");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(pilot);

        Assertions.assertThrows(BizException.class, () -> messageService.getEnterpriseInfo(user, "1"));
    }

    @Test
    public void shouldRejectProfileForBlankUid() {
        SessionUser user = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        Assertions.assertThrows(BizException.class, () -> messageService.getPilotProfile(user, ""));
    }

    @Test
    public void shouldGetConversation() {
        MessageConversationEntity conversation = buildConversation(5001L, 2L, 1L, 101L, "协同沟通");
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);

        MessageConversationEntity result = messageService.getConversation(5001L);
        Assertions.assertEquals(5001L, result.getId());
    }

    @Test
    public void shouldRejectNonExistentConversation() {
        Mockito.when(messageConversationMapper.selectById(999L)).thenReturn(null);
        Assertions.assertThrows(BizException.class, () -> messageService.getConversation(999L));
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

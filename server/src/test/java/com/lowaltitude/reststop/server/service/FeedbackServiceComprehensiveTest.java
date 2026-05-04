package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
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

public class FeedbackServiceComprehensiveTest {

    private FeedbackService feedbackService;
    private FeedbackTicketMapper feedbackTicketMapper;
    private UserAccountMapper userAccountMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        feedbackTicketMapper = Mockito.mock(FeedbackTicketMapper.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
    }

    @Test
    public void shouldCreateFeedbackTicketWithNullContact() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));
        Mockito.doAnswer(invocation -> {
            FeedbackTicketEntity entity = invocation.getArgument(0);
            entity.setId(9002L);
            return 1;
        }).when(feedbackTicketMapper).insert(ArgumentMatchers.any(FeedbackTicketEntity.class));

        ApiDtos.FeedbackTicketView ticket = feedbackService.createFeedbackTicket(
                pilot,
                new ApiDtos.FeedbackTicketRequest(null, "测试工单内容")
        );

        Assertions.assertEquals(9002L, ticket.id());
        Assertions.assertEquals("未填写", ticket.contact());
    }

    @Test
    public void shouldReplyFeedbackTicketWithProcessingStatus() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(1L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setDetail("测试工单");
        ticket.setStatus("OPEN");
        Mockito.when(feedbackTicketMapper.selectById(1L)).thenReturn(ticket);
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));

        ApiDtos.FeedbackTicketView result = feedbackService.replyFeedbackTicket(
                admin,
                1L,
                new ApiDtos.FeedbackTicketReplyRequest("PROCESSING", "正在处理中")
        );

        Assertions.assertEquals("处理中", result.status());
        Assertions.assertNull(ticket.getCloseTime());
    }

    @Test
    public void shouldRejectReplyForNonExistentTicket() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        Mockito.when(feedbackTicketMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> feedbackService.replyFeedbackTicket(
                admin,
                999L,
                new ApiDtos.FeedbackTicketReplyRequest("PROCESSING", "回复")
        ));
    }

    @Test
    public void shouldCreateFeedbackTicketForEnterprise() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));
        Mockito.doAnswer(invocation -> {
            FeedbackTicketEntity entity = invocation.getArgument(0);
            entity.setId(9003L);
            return 1;
        }).when(feedbackTicketMapper).insert(ArgumentMatchers.any(FeedbackTicketEntity.class));

        ApiDtos.FeedbackTicketView ticket = feedbackService.createFeedbackTicket(
                enterprise,
                new ApiDtos.FeedbackTicketRequest("13900139000", "企业端反馈内容")
        );

        Assertions.assertEquals(9003L, ticket.id());
        Assertions.assertEquals("企业", ticket.submitterRole());
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

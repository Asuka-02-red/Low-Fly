package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class FeedbackServiceTest {

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
    public void shouldCreateFeedbackTicket() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));
        Mockito.doAnswer(invocation -> {
            FeedbackTicketEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return 1;
        }).when(feedbackTicketMapper).insert(ArgumentMatchers.any(FeedbackTicketEntity.class));

        ApiDtos.FeedbackTicketView ticket = feedbackService.createFeedbackTicket(
                pilot,
                new ApiDtos.FeedbackTicketRequest("13800138000", "飞行任务页面在弱网下刷新较慢，希望增加重试提示")
        );

        Assertions.assertEquals(9001L, ticket.id());
        Assertions.assertEquals("待处理", ticket.status());
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

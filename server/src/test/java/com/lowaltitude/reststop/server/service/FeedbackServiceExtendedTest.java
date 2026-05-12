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

public class FeedbackServiceExtendedTest {

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
    public void shouldListMyFeedbackTickets() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(1L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setContact("13800138000");
        ticket.setDetail("测试工单");
        ticket.setStatus("OPEN");
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        Mockito.when(feedbackTicketMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(ticket));
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));

        List<ApiDtos.FeedbackTicketView> tickets = feedbackService.listMyFeedbackTickets(pilot);

        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals("待处理", tickets.get(0).status());
    }

    @Test
    public void shouldListAdminFeedbackTickets() {
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(1L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setContact("13800138000");
        ticket.setDetail("测试工单");
        ticket.setStatus("OPEN");
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        Mockito.when(feedbackTicketMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(ticket));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(1L, "pilot_demo", "PILOT")));

        List<ApiDtos.FeedbackTicketView> tickets = feedbackService.listAdminFeedbackTickets();

        Assertions.assertEquals(1, tickets.size());
    }

    @Test
    public void shouldReplyFeedbackTicket() {
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
                new ApiDtos.FeedbackTicketReplyRequest("CLOSED", "已处理")
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals("已关闭", result.status());
        Assertions.assertEquals("已处理", result.reply());
    }

    @Test
    public void shouldRejectNonAdminReply() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        Assertions.assertThrows(BizException.class, () -> feedbackService.replyFeedbackTicket(
                pilot,
                1L,
                new ApiDtos.FeedbackTicketReplyRequest("回复", "PROCESSING")
        ));
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

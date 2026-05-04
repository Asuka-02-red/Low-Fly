package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.PaymentOrderEntity;
import com.lowaltitude.reststop.server.entity.ReportDailySummaryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.CourseEnrollmentMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.MessageConversationMapper;
import com.lowaltitude.reststop.server.mapper.MessageEntryMapper;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.mapper.ReportDailySummaryMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.security.TokenService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class DirectMethodCoverageTest {

    private UserAccountMapper userAccountMapper;
    private TaskMapper taskMapper;
    private BizOrderMapper bizOrderMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private NoFlyZoneMapper noFlyZoneMapper;
    private FeedbackTicketMapper feedbackTicketMapper;
    private CourseMapper courseMapper;
    private CourseEnrollmentMapper courseEnrollmentMapper;
    private MessageConversationMapper messageConversationMapper;
    private MessageEntryMapper messageEntryMapper;
    private ReportDailySummaryMapper reportDailySummaryMapper;
    private AuditLogService auditLogService;
    private AlertService alertService;
    private TokenService tokenService;
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        taskMapper = Mockito.mock(TaskMapper.class);
        bizOrderMapper = Mockito.mock(BizOrderMapper.class);
        paymentOrderMapper = Mockito.mock(PaymentOrderMapper.class);
        noFlyZoneMapper = Mockito.mock(NoFlyZoneMapper.class);
        feedbackTicketMapper = Mockito.mock(FeedbackTicketMapper.class);
        courseMapper = Mockito.mock(CourseMapper.class);
        courseEnrollmentMapper = Mockito.mock(CourseEnrollmentMapper.class);
        messageConversationMapper = Mockito.mock(MessageConversationMapper.class);
        messageEntryMapper = Mockito.mock(MessageEntryMapper.class);
        reportDailySummaryMapper = Mockito.mock(ReportDailySummaryMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        alertService = Mockito.mock(AlertService.class);
        tokenService = Mockito.mock(TokenService.class);
        refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
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
        user.setStatus(1);
        return user;
    }

    // ===== Direct audit() method coverage =====

    @Test
    void taskService_audit_shouldRecordLog() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        SessionUser actor = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        taskService.audit(actor, "TASK", "101", "CREATE", "测试任务");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("TASK"),
                ArgumentMatchers.eq("101"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("测试任务"));
    }

    @Test
    void orderService_audit_shouldRecordLog() {
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper,
                Mockito.mock(TaskService.class), auditLogService);
        SessionUser actor = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        orderService.audit(actor, "ORDER", "ORD123", "CREATE", "taskId=101");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("ORDER"),
                ArgumentMatchers.eq("ORD123"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("taskId=101"));
    }

    @Test
    void feedbackService_audit_shouldRecordLog() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        SessionUser actor = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        feedbackService.audit(actor, "FEEDBACK", "FBK123", "CREATE", "工单内容");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("FEEDBACK"),
                ArgumentMatchers.eq("FBK123"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("工单内容"));
    }

    @Test
    void courseService_audit_shouldRecordLog() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser actor = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        courseService.audit(actor, "COURSE", "1", "CREATE", "测试课程");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(3L),
                ArgumentMatchers.eq("INSTITUTION"), ArgumentMatchers.eq("COURSE"),
                ArgumentMatchers.eq("1"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("测试课程"));
    }

    @Test
    void messageService_audit_shouldRecordLog() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser actor = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        messageService.audit(actor, "MESSAGE", "5001", "SEND", "消息内容");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("MESSAGE"),
                ArgumentMatchers.eq("5001"), ArgumentMatchers.eq("SEND"), ArgumentMatchers.eq("消息内容"));
    }

    @Test
    void adminUserService_audit_shouldRecordLog() {
        AdminUserService adminUserService = new AdminUserService(userAccountMapper, auditLogService);
        SessionUser actor = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        adminUserService.audit(actor, "ADMIN_USER", "10", "CREATE", "username=newuser");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(4L),
                ArgumentMatchers.eq("ADMIN"), ArgumentMatchers.eq("ADMIN_USER"),
                ArgumentMatchers.eq("10"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("username=newuser"));
    }

    @Test
    void adminSettingsService_audit_shouldRecordLog() {
        com.lowaltitude.reststop.server.mapper.AdminSettingMapper adminSettingMapper = Mockito.mock(com.lowaltitude.reststop.server.mapper.AdminSettingMapper.class);
        com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper adminNotificationRuleMapper = Mockito.mock(com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper.class);
        AdminSettingsService adminSettingsService = new AdminSettingsService(adminSettingMapper, adminNotificationRuleMapper, auditLogService);
        SessionUser actor = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        adminSettingsService.audit(actor, "ADMIN_SETTINGS", "basic", "UPDATE", "station=测试");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(4L),
                ArgumentMatchers.eq("ADMIN"), ArgumentMatchers.eq("ADMIN_SETTINGS"),
                ArgumentMatchers.eq("basic"), ArgumentMatchers.eq("UPDATE"), ArgumentMatchers.eq("station=测试"));
    }

    @Test
    void authService_audit_shouldRecordLog() {
        AuthService authService = new AuthService(tokenService, userAccountMapper, refreshTokenStore, auditLogService);
        authService.audit(1L, "PILOT", "USER", "1", "REGISTER", "role=PILOT");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("USER"),
                ArgumentMatchers.eq("1"), ArgumentMatchers.eq("REGISTER"), ArgumentMatchers.eq("role=PILOT"));
    }

    // ===== Direct package-private method coverage =====

    @Test
    void feedbackService_findUsersByIds_shouldHandleEmptyIds() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        Map<Long, UserAccountEntity> result = feedbackService.findUsersByIds(java.util.Set.of());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void feedbackService_getUserById_shouldThrowWhenNotFound() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);
        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class,
                () -> feedbackService.getUserById(999L));
    }

    @Test
    void courseService_findActiveEnrollment_shouldReturnNullForNullParams() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        CourseEnrollmentEntity result = courseService.findActiveEnrollment(null, 1L);
        Assertions.assertNull(result);
    }

    @Test
    void courseService_getUserById_shouldThrowWhenNotFound() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);
        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class,
                () -> courseService.getUserById(999L));
    }

    @Test
    void messageService_getUserByUsername_shouldThrowWhenNotFound() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class,
                () -> messageService.getUserByUsername("nonexistent"));
    }

    @Test
    void messageService_countCounterpartMessages_shouldReturnCount() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        int count = messageService.countCounterpartMessages(5001L, 1L);
        Assertions.assertEquals(5, count);
    }

    @Test
    void messageService_conversationTitle_shouldHandleDashCounterpart() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        String result = messageService.conversationTitle(
                new MessageConversationEntity(), null);
        Assertions.assertEquals("协同沟通", result);
    }

    @Test
    void adminProjectService_resolvePaymentStatus_shouldReturnCorrectStatus() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        BizOrderEntity paidOrder = new BizOrderEntity();
        paidOrder.setStatus("PAID");
        BizOrderEntity pendingOrder = new BizOrderEntity();
        pendingOrder.setStatus("PENDING_PAYMENT");
        Assertions.assertEquals("部分结算", adminProjectService.resolvePaymentStatus(List.of(paidOrder, pendingOrder)));
        Assertions.assertEquals("已结算", adminProjectService.resolvePaymentStatus(List.of(paidOrder)));
        Assertions.assertEquals("待结算", adminProjectService.resolvePaymentStatus(List.of(pendingOrder)));
        Assertions.assertEquals("待结算", adminProjectService.resolvePaymentStatus(List.of()));
    }

    @Test
    void adminProjectService_findTasksByIds_shouldHandleEmptyIds() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        Map<Long, TaskEntity> result = adminProjectService.findTasksByIds(java.util.Set.of());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void adminProjectService_findLatestPayments_shouldHandleEmptyIds() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        Map<Long, PaymentOrderEntity> result = adminProjectService.findLatestPayments(java.util.Set.of());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void adminProjectService_findLatestPayments_shouldHandleMultiplePayments() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        PaymentOrderEntity p1 = new PaymentOrderEntity();
        p1.setId(1L);
        p1.setBizOrderId(501L);
        p1.setChannel("ALIPAY");
        PaymentOrderEntity p2 = new PaymentOrderEntity();
        p2.setId(2L);
        p2.setBizOrderId(501L);
        p2.setChannel("WECHAT_PAY");
        Mockito.when(paymentOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(p1, p2));

        Map<Long, PaymentOrderEntity> result = adminProjectService.findLatestPayments(java.util.Set.of(501L));
        Assertions.assertEquals(1, result.size());
    }

    @Test
    void adminDashboardService_buildServicePerformanceMetrics_shouldWorkWithNullLatest() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        AdminDashboardService adminDashboardService = new AdminDashboardService(userAccountMapper, taskMapper,
                bizOrderMapper, courseMapper, reportDailySummaryMapper, alertService, auditLogService, adminProjectService);

        List<ApiDtos.AdminServiceMetric> metrics = adminDashboardService.buildServicePerformanceMetrics(
                List.of(), 0, 0);
        Assertions.assertEquals(4, metrics.size());
    }

    @Test
    void adminDashboardService_buildServicePerformanceMetrics_shouldWorkWithLatestReport() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        AdminDashboardService adminDashboardService = new AdminDashboardService(userAccountMapper, taskMapper,
                bizOrderMapper, courseMapper, reportDailySummaryMapper, alertService, auditLogService, adminProjectService);

        ReportDailySummaryEntity latest = new ReportDailySummaryEntity();
        latest.setTaskCount(10);
        latest.setOrderCount(5);
        latest.setTrainingCount(3);
        latest.setAlertCount(2);

        List<ApiDtos.AdminServiceMetric> metrics = adminDashboardService.buildServicePerformanceMetrics(
                List.of(latest), 3, 1);
        Assertions.assertEquals(4, metrics.size());
    }

    @Test
    void orderService_getOrderDetail_shouldHandleNullCreateTime() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper, taskService, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        order.setCreateTime(null);
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        Mockito.when(taskMapper.selectById(101L)).thenReturn(new TaskEntity());
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setTitle("任务");
        task.setTaskType("INSPECTION");
        task.setLocation("重庆");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));
        Mockito.when(paymentOrderMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        ApiDtos.OrderDetailView detail = orderService.getOrderDetail(pilot, 501L);
        Assertions.assertNotNull(detail);
    }

    @Test
    void authService_register_shouldCoverFindUserByPhoneBranch() {
        AuthService authService = new AuthService(tokenService, userAccountMapper, refreshTokenStore, auditLogService);
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenAnswer(invocation -> {
            return null;
        });
        Mockito.doAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.when(refreshTokenStore.issueToken(ArgumentMatchers.any())).thenReturn("rt");
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt");

        ApiDtos.AuthPayload payload = authService.register(
                new ApiDtos.RegisterRequest("user1", "pass123", "13900139001", "PILOT", "张三", "公司"));
        Assertions.assertNotNull(payload);
    }

    @Test
    void messageService_syncReadReceipts_shouldHandleNonExistentEntry() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Mockito.when(messageEntryMapper.selectById(999L)).thenReturn(null);

        ApiDtos.MessageReadReceiptResponse result = messageService.syncReadReceipts(pilot,
                new ApiDtos.MessageReadReceiptRequest(List.of(999L)));
        Assertions.assertEquals(0, result.successCount());
    }

    @Test
    void taskService_audit_nullActor_shouldHandleNullBranches() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        taskService.audit(null, "TASK", "101", "CREATE", "测试");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("TASK"), ArgumentMatchers.eq("101"),
                ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("测试"));
    }

    @Test
    void orderService_audit_nullActor_shouldHandleNullBranches() {
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper,
                Mockito.mock(TaskService.class), auditLogService);
        orderService.audit(null, "ORDER", "ORD123", "CREATE", "taskId=101");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("ORDER"), ArgumentMatchers.eq("ORD123"),
                ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("taskId=101"));
    }

    @Test
    void feedbackService_audit_nullActor_shouldHandleNullBranches() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        feedbackService.audit(null, "FEEDBACK", "FBK123", "CREATE", "工单");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("FEEDBACK"), ArgumentMatchers.eq("FBK123"),
                ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("工单"));
    }

    @Test
    void courseService_audit_nullActor_shouldHandleNullBranches() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        courseService.audit(null, "COURSE", "1", "CREATE", "测试课程");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("COURSE"), ArgumentMatchers.eq("1"),
                ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("测试课程"));
    }

    @Test
    void messageService_audit_nullActor_shouldHandleNullBranches() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        messageService.audit(null, "MESSAGE", "5001", "SEND", "消息");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("MESSAGE"), ArgumentMatchers.eq("5001"),
                ArgumentMatchers.eq("SEND"), ArgumentMatchers.eq("消息"));
    }

    @Test
    void adminUserService_audit_nullActor_shouldHandleNullBranches() {
        AdminUserService adminUserService = new AdminUserService(userAccountMapper, auditLogService);
        adminUserService.audit(null, "ADMIN_USER", "10", "CREATE", "username=newuser");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("ADMIN_USER"), ArgumentMatchers.eq("10"),
                ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("username=newuser"));
    }

    @Test
    void adminSettingsService_audit_nullActor_shouldHandleNullBranches() {
        com.lowaltitude.reststop.server.mapper.AdminSettingMapper adminSettingMapper = Mockito.mock(com.lowaltitude.reststop.server.mapper.AdminSettingMapper.class);
        com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper adminNotificationRuleMapper = Mockito.mock(com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper.class);
        AdminSettingsService adminSettingsService = new AdminSettingsService(adminSettingMapper, adminNotificationRuleMapper, auditLogService);
        adminSettingsService.audit(null, "ADMIN_SETTINGS", "basic", "UPDATE", "station=测试");
        Mockito.verify(auditLogService).record(ArgumentMatchers.anyString(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull(),
                ArgumentMatchers.eq("ADMIN_SETTINGS"), ArgumentMatchers.eq("basic"),
                ArgumentMatchers.eq("UPDATE"), ArgumentMatchers.eq("station=测试"));
    }

    @Test
    void authService_register_phoneAlreadyExists_shouldThrow() {
        AuthService authService = new AuthService(tokenService, userAccountMapper, refreshTokenStore, auditLogService);
        UserAccountEntity existingUser = buildUser(1L, "existing", "PILOT");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)
                .thenReturn(existingUser);
        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class,
                () -> authService.register(
                        new ApiDtos.RegisterRequest("newuser", "pass123", "13900139001", "PILOT", "张三", "公司")));
    }

    @Test
    void messageService_syncReadReceipts_nullMsgId_shouldSkip() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        ApiDtos.MessageReadReceiptResponse result = messageService.syncReadReceipts(pilot,
                new ApiDtos.MessageReadReceiptRequest(java.util.Arrays.asList(null, null)));
        Assertions.assertEquals(0, result.successCount());
    }

    @Test
    void messageService_countCounterpartMessages_nullCount_shouldReturnZero() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(null);
        int count = messageService.countCounterpartMessages(5001L, 1L);
        Assertions.assertEquals(0, count);
    }
}

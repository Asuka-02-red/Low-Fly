package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.NoFlyZoneEntity;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class FinalCoverageTest {

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

    private TaskEntity buildTask(Long id, Long enterpriseId, String type, String title, String location, String status) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setEnterpriseId(enterpriseId);
        task.setTaskType(type);
        task.setTitle(title);
        task.setDescription("描述");
        task.setLocation(location);
        task.setDeadline(LocalDateTime.of(2026, 5, 1, 10, 0));
        task.setLatitude(BigDecimal.valueOf(29.56));
        task.setLongitude(BigDecimal.valueOf(106.55));
        task.setBudget(BigDecimal.valueOf(800));
        task.setStatus(status);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }

    // ===== AuthService: register() branch for phone check =====

    @Test
    void authService_shouldRegisterWithPhoneCheck() {
        AuthService authService = new AuthService(tokenService, userAccountMapper, refreshTokenStore, auditLogService);
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.when(refreshTokenStore.issueToken(ArgumentMatchers.any())).thenReturn("rt");
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt");

        ApiDtos.AuthPayload payload = authService.register(
                new ApiDtos.RegisterRequest("newuser", "password123", "13900139000", "PILOT", "张三", "公司"));

        Assertions.assertEquals("jwt", payload.token());
        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(10L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("USER"),
                ArgumentMatchers.anyString(), ArgumentMatchers.eq("REGISTER"), ArgumentMatchers.anyString());
    }

    // ===== TaskService: audit() via publishTask =====

    @Test
    void taskService_shouldAuditOnPublishTask() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "任务", "重庆", "REVIEWING");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));

        taskService.publishTask(enterprise, 101L);

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(2L),
                ArgumentMatchers.eq("ENTERPRISE"), ArgumentMatchers.eq("TASK"),
                ArgumentMatchers.eq("101"), ArgumentMatchers.eq("REPUBLISH"), ArgumentMatchers.eq("任务"));
    }

    // ===== OrderService: audit() via payOrder, getOrderDetail with null payment =====

    @Test
    void orderService_shouldAuditOnPayOrder() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper, taskService, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PENDING_PAYMENT");
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        Mockito.doAnswer(invocation -> {
            PaymentOrderEntity p = invocation.getArgument(0);
            p.setTradeNo("PAY123");
            return 1;
        }).when(paymentOrderMapper).insert(ArgumentMatchers.any(PaymentOrderEntity.class));

        orderService.payOrder(pilot, new ApiDtos.PaymentRequest(501L, "ALIPAY"));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("PAYMENT"),
                ArgumentMatchers.eq("ORD123"), ArgumentMatchers.eq("PAY_SUCCESS"), ArgumentMatchers.anyString());
    }

    @Test
    void orderService_shouldGetOrderDetailWithNullPayment() {
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
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        Mockito.when(taskMapper.selectById(101L)).thenReturn(buildTask(101L, 2L, "INSPECTION", "巡检", "重庆", "PUBLISHED"));
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));
        Mockito.when(paymentOrderMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        ApiDtos.OrderDetailView detail = orderService.getOrderDetail(pilot, 501L);
        Assertions.assertEquals("待支付", detail.paymentChannel());
        Assertions.assertEquals("待支付", detail.paymentStatus());
    }

    // ===== FeedbackService: audit() via replyFeedbackTicket, getUserById() =====

    @Test
    void feedbackService_shouldAuditOnReply() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(1L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setDetail("测试");
        ticket.setStatus("OPEN");
        Mockito.when(feedbackTicketMapper.selectById(1L)).thenReturn(ticket);
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));

        feedbackService.replyFeedbackTicket(admin, 1L, new ApiDtos.FeedbackTicketReplyRequest("PROCESSING", "正在处理"));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(4L),
                ArgumentMatchers.eq("ADMIN"), ArgumentMatchers.eq("FEEDBACK"),
                ArgumentMatchers.eq("FBK123"), ArgumentMatchers.eq("REPLY"), ArgumentMatchers.anyString());
    }

    @Test
    void feedbackService_shouldThrowWhenUserNotFound() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> feedbackService.createFeedbackTicket(
                new SessionUser(999L, "unknown", RoleType.PILOT, "未知"),
                new ApiDtos.FeedbackTicketRequest("13800138000", "工单")));
    }

    // ===== CourseService: audit() via createCourse, getUserById() =====

    @Test
    void courseService_shouldAuditOnCreateCourse() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        UserAccountEntity owner = buildUser(3L, "institution", "INSTITUTION");
        owner.setCompanyName("培训机构");
        Mockito.when(userAccountMapper.selectById(3L)).thenReturn(owner);
        Mockito.doAnswer(invocation -> {
            CourseEntity c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        }).when(courseMapper).insert(ArgumentMatchers.any(CourseEntity.class));

        courseService.createCourse(institution,
                new ApiDtos.CourseManageRequest("课程", "摘要", "内容", "OFFLINE", 30, BigDecimal.ZERO, "DRAFT"));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(3L),
                ArgumentMatchers.eq("INSTITUTION"), ArgumentMatchers.eq("COURSE"),
                ArgumentMatchers.eq("1"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("课程"));
    }

    @Test
    void courseService_shouldThrowWhenUserNotFound() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(999L, "unknown", RoleType.INSTITUTION, "未知");
        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> courseService.createCourse(institution,
                new ApiDtos.CourseManageRequest("课程", "摘要", "内容", "OFFLINE", 30, BigDecimal.ZERO, "DRAFT")));
    }

    @Test
    void courseService_shouldAuditOnEnroll() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        CourseEntity course = new CourseEntity();
        course.setId(1L);
        course.setTitle("测试课程");
        course.setCourseType("ARTICLE");
        course.setStatus("OPEN");
        course.setSeatAvailable(20);
        course.setEnrollCount(5);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.doAnswer(invocation -> {
            CourseEnrollmentEntity e = invocation.getArgument(0);
            e.setId(3001L);
            return 1;
        }).when(courseEnrollmentMapper).insert(ArgumentMatchers.any(CourseEnrollmentEntity.class));

        courseService.enrollCourse(pilot, 1L);

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("TRAINING"),
                ArgumentMatchers.eq("1"), ArgumentMatchers.eq("ENROLL"), ArgumentMatchers.anyString());
    }

    // ===== MessageService: audit(), getUserByUsername(), conversationTitle(), countCounterpartMessages() =====

    @Test
    void messageService_shouldAuditOnSendMessage() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, taskService, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(null);
        conversation.setTitle("协同沟通");
        conversation.setLastMessageTime(LocalDateTime.now());
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(
                buildUser(1L, "pilot", "PILOT"), buildUser(2L, "enterprise", "ENTERPRISE")));
        Mockito.when(messageEntryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.doAnswer(invocation -> {
            MessageEntryEntity e = invocation.getArgument(0);
            e.setId(7001L);
            return 1;
        }).when(messageEntryMapper).insert(ArgumentMatchers.any(MessageEntryEntity.class));

        messageService.sendMessage(pilot, 5001L, new ApiDtos.MessageSendRequest("消息内容"));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("MESSAGE"),
                ArgumentMatchers.eq("5001"), ArgumentMatchers.eq("SEND"), ArgumentMatchers.eq("消息内容"));
    }

    @Test
    void messageService_shouldGetPilotProfileByNumericUid() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, taskService, auditLogService);
        SessionUser user = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        UserAccountEntity pilot = buildUser(1L, "pilot", "PILOT");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(pilot);

        ApiDtos.PilotProfileView profile = messageService.getPilotProfile(user, "1");
        Assertions.assertEquals("1", profile.uid());
    }

    @Test
    void messageService_shouldRejectNonPilotProfileByUid() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, taskService, auditLogService);
        SessionUser user = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        UserAccountEntity enterprise = buildUser(2L, "enterprise", "ENTERPRISE");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(enterprise);

        Assertions.assertThrows(BizException.class, () -> messageService.getPilotProfile(user, "2"));
    }

    @Test
    void messageService_shouldRejectNonEnterpriseInfoByUid() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, taskService, auditLogService);
        SessionUser user = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        UserAccountEntity pilot = buildUser(1L, "pilot", "PILOT");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(pilot);

        Assertions.assertThrows(BizException.class, () -> messageService.getEnterpriseInfo(user, "1"));
    }

    @Test
    void messageService_shouldHandleConversationWithDashDisplayName() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, taskService, auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(null);
        conversation.setTitle("协同沟通");
        conversation.setLastMessageTime(LocalDateTime.now());
        Mockito.when(messageConversationMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(conversation));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise", "ENTERPRISE")));
        Mockito.when(messageEntryMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);

        List<ApiDtos.MessageConversationView> result = messageService.listMessageConversations(enterprise);
        Assertions.assertEquals(1, result.size());
    }

    // ===== AdminProjectService: findTasksByIds(), findLatestPayments(), toAdminOrderSummaryView() =====

    @Test
    void adminProjectService_shouldHandleOrderWithTaskAndPayment() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        PaymentOrderEntity payment = new PaymentOrderEntity();
        payment.setId(1L);
        payment.setBizOrderId(501L);
        payment.setChannel("WECHAT_PAY");
        Mockito.when(paymentOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(payment));

        List<ApiDtos.AdminOrderSummaryView> orders = adminProjectService.listAdminOrders();
        Assertions.assertEquals(1, orders.size());
        Assertions.assertEquals("微信支付", orders.get(0).paymentMethod());
    }

    @Test
    void adminProjectService_shouldHandleOrderDetailWithNullPayment() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(paymentOrderMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        ApiDtos.AdminOrderDetailView detail = adminProjectService.getAdminOrderDetail(501L);
        Assertions.assertEquals("待支付", detail.paymentMethod());
    }

    // ===== AdminDashboardService: buildServicePerformanceMetrics() with null latest report =====

    @Test
    void adminDashboardService_shouldBuildAnalyticsWithEmptyReports() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        AdminDashboardService adminDashboardService = new AdminDashboardService(userAccountMapper, taskMapper,
                bizOrderMapper, courseMapper, reportDailySummaryMapper, alertService, auditLogService, adminProjectService);

        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();
        Assertions.assertNotNull(analytics);
        Assertions.assertEquals(3, analytics.businessMetrics().size());
        Assertions.assertEquals(3, analytics.performanceMetrics().size());
        Assertions.assertEquals(4, analytics.servicePerformance().size());
    }

    // ===== AdminSettingsService: audit() via saveAdminBasicSettings =====

    @Test
    void adminSettingsService_shouldAuditOnSaveBasicSettings() {
        com.lowaltitude.reststop.server.mapper.AdminSettingMapper adminSettingMapper = Mockito.mock(com.lowaltitude.reststop.server.mapper.AdminSettingMapper.class);
        com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper adminNotificationRuleMapper = Mockito.mock(com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper.class);
        AdminSettingsService adminSettingsService = new AdminSettingsService(adminSettingMapper, adminNotificationRuleMapper, auditLogService);

        java.util.Map<String, com.lowaltitude.reststop.server.entity.AdminSettingEntity> store = new java.util.HashMap<>();
        Mockito.when(adminSettingMapper.selectById(ArgumentMatchers.anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        Mockito.when(adminSettingMapper.insertOrUpdate(ArgumentMatchers.any(com.lowaltitude.reststop.server.entity.AdminSettingEntity.class))).thenAnswer(invocation -> {
            com.lowaltitude.reststop.server.entity.AdminSettingEntity entity = invocation.getArgument(0);
            store.put(entity.getSettingKey(), entity);
            return true;
        });
        Mockito.when(adminNotificationRuleMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        adminSettingsService.saveAdminBasicSettings(admin, new ApiDtos.AdminBasicSettings("站名", "400-123", "区域", true));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(4L),
                ArgumentMatchers.eq("ADMIN"), ArgumentMatchers.eq("ADMIN_SETTINGS"),
                ArgumentMatchers.eq("basic"), ArgumentMatchers.eq("UPDATE"), ArgumentMatchers.anyString());
    }

    // ===== AdminUserService: audit() via createAdminUser =====

    @Test
    void adminUserService_shouldAuditOnCreateAdminUser() {
        AdminUserService adminUserService = new AdminUserService(userAccountMapper, auditLogService);
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.when(userAccountMapper.selectById(10L)).thenReturn(buildUser(10L, "newuser", "ENTERPRISE"));

        adminUserService.createAdminUser(admin, new ApiDtos.AdminUserCreateRequest("newuser", "Admin123", "13900139000", "test@example.com", "ENTERPRISE", "运维", "运营中心", 1));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(4L),
                ArgumentMatchers.eq("ADMIN"), ArgumentMatchers.eq("ADMIN_USER"),
                ArgumentMatchers.eq("10"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.anyString());
    }
}

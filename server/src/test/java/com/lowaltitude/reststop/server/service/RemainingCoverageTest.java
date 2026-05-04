package com.lowaltitude.reststop.server.service;

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

class RemainingCoverageTest {

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

    // ===== AuthService: register() and getUserByUsername() branches =====

    @Test
    void authService_shouldRegisterWithCompanyName() {
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
                new ApiDtos.RegisterRequest("newuser", "password123", "13900139000", "ENTERPRISE", "张三", "低空科技公司"));

        Assertions.assertEquals("ENTERPRISE", payload.userInfo().role());
        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(10L),
                ArgumentMatchers.eq("ENTERPRISE"), ArgumentMatchers.eq("USER"),
                ArgumentMatchers.anyString(), ArgumentMatchers.eq("REGISTER"), ArgumentMatchers.anyString());
    }

    @Test
    void authService_shouldThrowWhenUserNotFoundByUsername() {
        AuthService authService = new AuthService(tokenService, userAccountMapper, refreshTokenStore, auditLogService);
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.when(refreshTokenStore.requireUsernameByRefreshToken("bad-token")).thenReturn("nonexistent");

        Assertions.assertThrows(BizException.class, () -> authService.refresh(new ApiDtos.RefreshTokenRequest("bad-token")));
    }

    // ===== TaskService: listZones() and audit() =====

    @Test
    void taskService_shouldListZonesWithNullRadius() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        NoFlyZoneEntity zone = new NoFlyZoneEntity();
        zone.setId(1L);
        zone.setName("测试禁飞区");
        zone.setZoneType("MILITARY");
        zone.setCenterLat(BigDecimal.ONE);
        zone.setCenterLng(BigDecimal.TEN);
        zone.setRadius(null);
        zone.setDescription("测试");
        Mockito.when(noFlyZoneMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(zone));

        List<ApiDtos.NoFlyZoneView> zones = taskService.listZones();
        Assertions.assertEquals(0, zones.get(0).radius());
        Mockito.verify(auditLogService, Mockito.never()).record(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void taskService_shouldAuditOnCreateTask() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));
        Mockito.doAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            task.setId(101L);
            return 1;
        }).when(taskMapper).insert(ArgumentMatchers.any(TaskEntity.class));

        taskService.createTask(enterprise, new ApiDtos.TaskRequest("INSPECTION", "任务", "描述", "重庆",
                "2026-05-01 10:00", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(800)));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(2L),
                ArgumentMatchers.eq("ENTERPRISE"), ArgumentMatchers.eq("TASK"),
                ArgumentMatchers.eq("101"), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.eq("任务"));
    }

    // ===== OrderService: getOrderDetail() and audit() =====

    @Test
    void orderService_shouldGetOrderDetailWithPayment() {
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
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setTitle("巡检任务");
        task.setTaskType("INSPECTION");
        task.setLocation("重庆");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));
        PaymentOrderEntity payment = new PaymentOrderEntity();
        payment.setId(1L);
        payment.setBizOrderId(501L);
        payment.setChannel("ALIPAY");
        payment.setStatus("SUCCESS");
        Mockito.when(paymentOrderMapper.selectOne(ArgumentMatchers.any())).thenReturn(payment);

        ApiDtos.OrderDetailView detail = orderService.getOrderDetail(pilot, 501L);
        Assertions.assertEquals("ALIPAY", detail.paymentChannel());
        Assertions.assertEquals("已支付", detail.paymentStatus());
    }

    @Test
    void orderService_shouldAuditOnCreateOrder() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper, taskService, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setBudget(BigDecimal.valueOf(800));
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));
        Mockito.doAnswer(invocation -> {
            BizOrderEntity o = invocation.getArgument(0);
            o.setId(501L);
            return 1;
        }).when(bizOrderMapper).insert(ArgumentMatchers.any(BizOrderEntity.class));

        orderService.createOrder(pilot, new ApiDtos.OrderCreateRequest(101L));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("ORDER"),
                ArgumentMatchers.anyString(), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.anyString());
    }

    // ===== FeedbackService: getUserById(), findUsersByIds(), audit() =====

    @Test
    void feedbackService_shouldAuditOnCreateTicket() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));
        Mockito.doAnswer(invocation -> {
            FeedbackTicketEntity t = invocation.getArgument(0);
            t.setId(1L);
            return 1;
        }).when(feedbackTicketMapper).insert(ArgumentMatchers.any(FeedbackTicketEntity.class));

        feedbackService.createFeedbackTicket(pilot, new ApiDtos.FeedbackTicketRequest("13800138000", "工单内容"));

        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("FEEDBACK"),
                ArgumentMatchers.anyString(), ArgumentMatchers.eq("CREATE"), ArgumentMatchers.anyString());
    }

    @Test
    void feedbackService_shouldHandleNullSubmitterInAdminList() {
        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(999L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setDetail("测试");
        ticket.setStatus("OPEN");
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        Mockito.when(feedbackTicketMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(ticket));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.FeedbackTicketView> tickets = feedbackService.listAdminFeedbackTickets();
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertEquals("-", tickets.get(0).submitterName());
    }

    // ===== CourseService: listManagedCourses(), findActiveEnrollment(), resolveInstitutionName() =====

    @Test
    void courseService_shouldListManagedCoursesForInstitution() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        CourseEntity course = new CourseEntity();
        course.setId(1L);
        course.setTitle("测试课程");
        course.setSummary("摘要");
        course.setCourseType("OFFLINE");
        course.setSeatTotal(30);
        course.setSeatAvailable(20);
        course.setBrowseCount(10);
        course.setEnrollCount(5);
        course.setPrice(BigDecimal.ZERO);
        course.setStatus("OPEN");
        Mockito.when(courseMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(course));

        List<ApiDtos.CourseManageView> courses = courseService.listManagedCourses(institution);
        Assertions.assertEquals(1, courses.size());
    }

    @Test
    void courseService_shouldResolveInstitutionNameFromCompanyName() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        UserAccountEntity owner = buildUser(3L, "institution", "INSTITUTION");
        owner.setCompanyName("低空培训中心");
        Mockito.when(userAccountMapper.selectById(3L)).thenReturn(owner);
        Mockito.doAnswer(invocation -> {
            CourseEntity c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        }).when(courseMapper).insert(ArgumentMatchers.any(CourseEntity.class));

        ApiDtos.CourseManageView result = courseService.createCourse(institution,
                new ApiDtos.CourseManageRequest("新课程", "摘要", "内容", "OFFLINE", 30, BigDecimal.ZERO, "DRAFT"));

        Assertions.assertEquals(1L, result.id());
    }

    @Test
    void courseService_shouldResolveInstitutionNameFromRealNameWhenNoCompany() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业用户");
        UserAccountEntity owner = buildUser(2L, "enterprise", "ENTERPRISE");
        owner.setCompanyName(null);
        owner.setRealName("测试企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(owner);
        Mockito.doAnswer(invocation -> {
            CourseEntity c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        }).when(courseMapper).insert(ArgumentMatchers.any(CourseEntity.class));

        ApiDtos.CourseManageView result = courseService.createCourse(enterprise,
                new ApiDtos.CourseManageRequest("新课程", "摘要", "内容", "ARTICLE", 999, BigDecimal.ZERO, "DRAFT"));

        Assertions.assertEquals(1L, result.id());
    }

    @Test
    void courseService_shouldGetCourseDetailWithNullEnrollment() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        CourseEntity course = new CourseEntity();
        course.setId(1L);
        course.setTitle("测试课程");
        course.setSummary("摘要");
        course.setContent("内容");
        course.setCourseType("ARTICLE");
        course.setInstitutionName("机构");
        course.setSeatTotal(30);
        course.setSeatAvailable(20);
        course.setBrowseCount(10);
        course.setEnrollCount(5);
        course.setPrice(BigDecimal.ZERO);
        course.setStatus("OPEN");
        course.setPublishUserId(3L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        ApiDtos.CourseDetailView detail = courseService.getCourseDetail(pilot, 1L);
        Assertions.assertFalse(detail.enrolled());
        Assertions.assertNull(detail.enrollmentNo());
        Assertions.assertNull(detail.enrollmentStatus());
    }

    // ===== MessageService: syncReadReceipts(), conversationTitle(), conversationSubtitle(), getUserByUsername() =====

    @Test
    void messageService_shouldHandleConversationWithNullTitle() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(null);
        conversation.setTitle(null);
        conversation.setLastMessageTime(LocalDateTime.now());
        Mockito.when(messageConversationMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(conversation));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(
                buildUser(1L, "pilot", "PILOT"), buildUser(2L, "enterprise", "ENTERPRISE")));
        Mockito.when(messageEntryMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);

        List<ApiDtos.MessageConversationView> result = messageService.listMessageConversations(enterprise);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    void messageService_shouldHandleConversationWithTaskIdAndNonExistentTask() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, taskService, auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(999L);
        conversation.setTitle("协同沟通");
        conversation.setLastMessageTime(LocalDateTime.now());
        Mockito.when(messageConversationMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(conversation));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(
                buildUser(1L, "pilot", "PILOT"), buildUser(2L, "enterprise", "ENTERPRISE")));
        Mockito.when(messageEntryMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.when(messageEntryMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(taskMapper.selectById(999L)).thenReturn(null);

        List<ApiDtos.MessageConversationView> result = messageService.listMessageConversations(enterprise);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    void messageService_shouldSyncReadReceiptsSuccessfully() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setId(7001L);
        entry.setConversationId(5001L);
        entry.setSenderUserId(2L);
        entry.setSenderRole("ENTERPRISE");
        entry.setContent("消息");
        entry.setCreateTime(LocalDateTime.now());
        Mockito.when(messageEntryMapper.selectById(7001L)).thenReturn(entry);
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);

        ApiDtos.MessageReadReceiptResponse result = messageService.syncReadReceipts(pilot,
                new ApiDtos.MessageReadReceiptRequest(List.of(7001L)));

        Assertions.assertEquals(1, result.successCount());
        Mockito.verify(auditLogService).record(ArgumentMatchers.any(), ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("PILOT"), ArgumentMatchers.eq("MESSAGE"),
                ArgumentMatchers.anyString(), ArgumentMatchers.eq("READ_RECEIPT"), ArgumentMatchers.anyString());
    }

    @Test
    void messageService_shouldGetEnterpriseInfoByUsername() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        UserAccountEntity enterprise = buildUser(2L, "enterprise_demo", "ENTERPRISE");
        enterprise.setCompanyName("低空企业");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(enterprise);

        ApiDtos.EnterpriseInfoView info = messageService.getEnterpriseInfo(pilot, "enterprise_demo");
        Assertions.assertEquals("低空企业", info.companyName());
    }

    // ===== AdminProjectService: resolvePaymentStatus(), toAdminOrderSummaryView(), findTasksByIds() =====

    @Test
    void adminProjectService_shouldHandlePartiallyPaidOrders() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise", "ENTERPRISE")));
        BizOrderEntity paidOrder = new BizOrderEntity();
        paidOrder.setId(501L);
        paidOrder.setTaskId(101L);
        paidOrder.setStatus("PAID");
        BizOrderEntity pendingOrder = new BizOrderEntity();
        pendingOrder.setId(502L);
        pendingOrder.setTaskId(101L);
        pendingOrder.setStatus("PENDING_PAYMENT");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(paidOrder, pendingOrder));

        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();
        Assertions.assertEquals("部分结算", projects.get(0).paymentStatus());
    }

    @Test
    void adminProjectService_shouldHandleOrderWithNullTask() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(999L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.AdminOrderSummaryView> orders = adminProjectService.listAdminOrders();
        Assertions.assertEquals(1, orders.size());
        Assertions.assertEquals("未知项目", orders.get(0).projectName());
    }

    @Test
    void adminProjectService_shouldHandleOrderWithNullPayment() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(paymentOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.AdminOrderSummaryView> orders = adminProjectService.listAdminOrders();
        Assertions.assertEquals("待支付", orders.get(0).paymentMethod());
    }

    // ===== AdminDashboardService: buildServicePerformanceMetrics() =====

    @Test
    void adminDashboardService_shouldBuildAnalyticsWithReports() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        AdminDashboardService adminDashboardService = new AdminDashboardService(userAccountMapper, taskMapper,
                bizOrderMapper, courseMapper, reportDailySummaryMapper, alertService, auditLogService, adminProjectService);

        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(20L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(2L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(1L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TaskEntity> qw = ArgumentMatchers.any();
        Mockito.when(taskMapper.selectList(qw)).thenReturn(List.of());

        ReportDailySummaryEntity report = new ReportDailySummaryEntity();
        report.setStatDate(LocalDate.now());
        report.setTaskCount(10);
        report.setOrderCount(5);
        report.setTrainingCount(3);
        report.setPaymentAmount(BigDecimal.valueOf(50000));
        report.setAlertCount(2);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(report));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();
        Assertions.assertNotNull(analytics);
        Assertions.assertEquals(3, analytics.businessMetrics().size());
        Assertions.assertEquals(3, analytics.performanceMetrics().size());
        Assertions.assertEquals(4, analytics.servicePerformance().size());
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
}

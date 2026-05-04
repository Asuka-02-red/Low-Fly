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
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class FullCoverageTest {

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
    private AuditLogService auditLogService;

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
        auditLogService = Mockito.mock(AuditLogService.class);
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

    // ===== TaskService remaining branches =====

    @Test
    void taskService_shouldRejectNonOwnerUpdatingTask() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        SessionUser otherEnterprise = new SessionUser(99L, "other", RoleType.ENTERPRISE, "其他企业");
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "任务", "重庆", "REVIEWING");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);

        Assertions.assertThrows(BizException.class, () -> taskService.updateTask(
                otherEnterprise, 101L,
                new ApiDtos.TaskRequest("INSPECTION", "标题", "描述", "重庆", "2026-05-01 10:00",
                        BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(800))));
    }

    @Test
    void taskService_shouldRejectNonOwnerPublishingTask() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        SessionUser otherEnterprise = new SessionUser(99L, "other", RoleType.ENTERPRISE, "其他企业");
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "任务", "重庆", "REVIEWING");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);

        Assertions.assertThrows(BizException.class, () -> taskService.publishTask(otherEnterprise, 101L));
    }

    @Test
    void taskService_adminCanUpdateAnyTask() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "旧任务", "重庆", "REVIEWING");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise", "ENTERPRISE"));

        ApiDtos.TaskView result = taskService.updateTask(admin, 101L,
                new ApiDtos.TaskRequest("MAPPING", "新任务", "描述", "北京", "2026-05-03 12:00",
                        BigDecimal.valueOf(29.56), BigDecimal.valueOf(106.55), BigDecimal.valueOf(3200)));

        Assertions.assertEquals("新任务", result.title());
    }

    @Test
    void taskService_shouldGetUserById() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));

        UserAccountEntity result = taskService.getUserById(1L);
        Assertions.assertEquals("pilot", result.getUsername());
    }

    @Test
    void taskService_shouldRejectGetNonExistentUser() {
        TaskService taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> taskService.getUserById(999L));
    }

    // ===== OrderService remaining branches =====

    @Test
    void orderService_shouldRejectNonExistentOrderCreate() {
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper,
                new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService), auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Mockito.when(taskMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> orderService.createOrder(pilot, new ApiDtos.OrderCreateRequest(999L)));
    }

    @Test
    void orderService_adminCanPayAnyOrder() {
        OrderService orderService = new OrderService(bizOrderMapper, paymentOrderMapper,
                Mockito.mock(TaskService.class), auditLogService);
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setPilotId(1L);
        order.setStatus("PENDING_PAYMENT");
        order.setAmount(BigDecimal.valueOf(800));
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        Mockito.doAnswer(invocation -> {
            PaymentOrderEntity p = invocation.getArgument(0);
            p.setTradeNo("PAY_ADMIN");
            return 1;
        }).when(paymentOrderMapper).insert(ArgumentMatchers.any(PaymentOrderEntity.class));

        ApiDtos.PaymentResult result = orderService.payOrder(admin, new ApiDtos.PaymentRequest(501L, "WECHAT_PAY"));
        Assertions.assertEquals("PAID", result.status());
    }

    // ===== FeedbackService remaining branches =====

    @Test
    void feedbackService_shouldHandleNullContactInTicketView() {
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(1L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setContact(null);
        ticket.setDetail("测试工单");
        ticket.setStatus("OPEN");
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        ticket.setCloseTime(null);

        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot", "PILOT"));
        Mockito.doAnswer(invocation -> {
            FeedbackTicketEntity t = invocation.getArgument(0);
            t.setId(1L);
            return 1;
        }).when(feedbackTicketMapper).insert(ArgumentMatchers.any(FeedbackTicketEntity.class));

        ApiDtos.FeedbackTicketView result = feedbackService.createFeedbackTicket(pilot,
                new ApiDtos.FeedbackTicketRequest(null, "工单内容"));
        Assertions.assertEquals("未填写", result.contact());
    }

    @Test
    void feedbackService_shouldHandleClosedTicketWithCloseTime() {
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setId(1L);
        ticket.setTicketNo("FBK123");
        ticket.setSubmitUserId(1L);
        ticket.setSubmitUserRole("PILOT");
        ticket.setContact("13800138000");
        ticket.setDetail("测试工单");
        ticket.setStatus("CLOSED");
        ticket.setReply("已处理");
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        ticket.setCloseTime(LocalDateTime.now());

        FeedbackService feedbackService = new FeedbackService(feedbackTicketMapper, userAccountMapper, auditLogService);
        Mockito.when(feedbackTicketMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(ticket));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(1L, "pilot", "PILOT")));

        List<ApiDtos.FeedbackTicketView> tickets = feedbackService.listAdminFeedbackTickets();
        Assertions.assertEquals(1, tickets.size());
        Assertions.assertNotEquals("-", tickets.get(0).closedTime());
    }

    // ===== MessageService remaining branches =====

    @Test
    void messageService_shouldHandleConversationWithNullTaskId() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(null);
        conversation.setTitle("协同沟通");
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
    void messageService_shouldGetMessageThreadWithMessages() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser enterprise = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(5001L);
        conversation.setEnterpriseId(2L);
        conversation.setPilotId(1L);
        conversation.setTaskId(null);
        conversation.setTitle("协同沟通");
        Mockito.when(messageConversationMapper.selectById(5001L)).thenReturn(conversation);
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(
                buildUser(1L, "pilot", "PILOT"), buildUser(2L, "enterprise", "ENTERPRISE")));
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setId(7001L);
        entry.setConversationId(5001L);
        entry.setSenderUserId(1L);
        entry.setSenderRole("PILOT");
        entry.setContent("消息内容");
        entry.setCreateTime(LocalDateTime.now());
        Mockito.when(messageEntryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(entry));

        ApiDtos.MessageThreadView thread = messageService.getMessageThread(enterprise, 5001L);
        Assertions.assertEquals(5001L, thread.conversationId());
        Assertions.assertEquals(1, thread.messages().size());
    }

    @Test
    void messageService_shouldRejectNonExistentUserByUid() {
        MessageService messageService = new MessageService(messageConversationMapper, messageEntryMapper,
                userAccountMapper, Mockito.mock(TaskService.class), auditLogService);
        SessionUser user = new SessionUser(2L, "enterprise", RoleType.ENTERPRISE, "企业");
        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> messageService.getPilotProfile(user, "999"));
    }

    // ===== CourseService remaining branches =====

    @Test
    void courseService_shouldRejectNonExistentCourseUpdate() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        Mockito.when(courseMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> courseService.updateCourse(institution, 999L,
                new ApiDtos.CourseManageRequest("标题", "摘要", "内容", "OFFLINE", 30, BigDecimal.ZERO, "DRAFT")));
    }

    @Test
    void courseService_shouldRejectNonExistentCoursePublish() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        Mockito.when(courseMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> courseService.publishCourse(institution, 999L));
    }

    @Test
    void courseService_shouldRejectNonExistentCourseDelete() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser institution = new SessionUser(3L, "institution", RoleType.INSTITUTION, "机构");
        Mockito.when(courseMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> courseService.deleteCourse(institution, 999L));
    }

    @Test
    void courseService_shouldRejectNonExistentCourseEnroll() {
        CourseService courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Mockito.when(courseMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> courseService.enrollCourse(pilot, 999L));
    }

    // ===== AdminProjectService remaining branches =====

    @Test
    void adminProjectService_shouldHandleTaskWithNullUpdateTime() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "任务", "重庆", "REVIEWING");
        task.setUpdateTime(null);
        task.setCreateTime(LocalDateTime.of(2026, 4, 22, 10, 0));
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise", "ENTERPRISE")));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();
        Assertions.assertEquals(1, projects.size());
        Assertions.assertEquals("待复核", projects.get(0).complianceStatus());
    }

    @Test
    void adminProjectService_shouldHandleTaskWithOrders() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        TaskEntity task = buildTask(101L, 2L, "MAPPING", "测绘任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise", "ENTERPRISE")));
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setTaskId(101L);
        order.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));

        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();
        Assertions.assertEquals("正常", projects.get(0).complianceStatus());
        Assertions.assertEquals("已结算", projects.get(0).paymentStatus());
    }

    @Test
    void adminProjectService_shouldHandleCancelledTask() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "CANCELLED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise", "ENTERPRISE")));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();
        Assertions.assertEquals("已暂停", projects.get(0).status());
    }

    @Test
    void adminProjectService_shouldHandleNonExistentOrderDetail() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        Mockito.when(bizOrderMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> adminProjectService.getAdminOrderDetail(999L));
    }

    @Test
    void adminProjectService_shouldHandleOrderWithPayment() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        PaymentOrderEntity payment = new PaymentOrderEntity();
        payment.setId(1L);
        payment.setBizOrderId(501L);
        payment.setChannel("ALIPAY");
        Mockito.when(paymentOrderMapper.selectOne(ArgumentMatchers.any())).thenReturn(payment);

        ApiDtos.AdminOrderDetailView detail = adminProjectService.getAdminOrderDetail(501L);
        Assertions.assertEquals("支付宝", detail.paymentMethod());
    }

    @Test
    void adminProjectService_shouldHandleEmptyTaskIds() {
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();
        Assertions.assertTrue(projects.isEmpty());
    }

    @Test
    void adminProjectService_shouldCountNoFlyZones() {
        Mockito.when(noFlyZoneMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);

        long count = adminProjectService.countNoFlyZones();
        Assertions.assertEquals(5L, count);
    }

    @Test
    void adminProjectService_shouldCountFeedbackTickets() {
        Mockito.when(feedbackTicketMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        AdminProjectService adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper,
                noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);

        long openCount = adminProjectService.countOpenFeedbackTickets();
        long processingCount = adminProjectService.countProcessingFeedbackTickets();
        Assertions.assertEquals(3L, openCount);
        Assertions.assertEquals(3L, processingCount);
    }

    // ===== PlatformUtils remaining branches =====

    @Test
    void platformUtils_shouldHandlePermissionGroupForBlankRole() {
        Assertions.assertEquals("未分配", PlatformUtils.permissionGroupName(""));
    }

    @Test
    void platformUtils_shouldHandlePermissionGroupForNullRole() {
        Assertions.assertEquals("未分配", PlatformUtils.permissionGroupName(null));
    }

    @Test
    void platformUtils_shouldHandleMapFeedbackStatusForOpen() {
        Assertions.assertEquals("待处理", PlatformUtils.mapFeedbackStatus("OPEN"));
    }

    @Test
    void platformUtils_shouldHandleNormalizeStatusForChinese() {
        Assertions.assertEquals("CLOSED", PlatformUtils.normalizeStatus("已关闭"));
        Assertions.assertEquals("PROCESSING", PlatformUtils.normalizeStatus("处理中"));
        Assertions.assertEquals("OPEN", PlatformUtils.normalizeStatus("待处理"));
    }

    @Test
    void platformUtils_shouldHandlePaymentChannelForUnknown() {
        Assertions.assertEquals("UNKNOWN", PlatformUtils.paymentChannelLabel("UNKNOWN"));
    }

    @Test
    void platformUtils_shouldHandlePaymentChannelForNull() {
        Assertions.assertEquals("待支付", PlatformUtils.paymentChannelLabel(null));
    }

    @Test
    void platformUtils_shouldHandleMapAdminProjectStatusForUnknown() {
        Assertions.assertEquals("规划中", PlatformUtils.mapAdminProjectStatus("UNKNOWN"));
    }

    @Test
    void platformUtils_shouldHandleEstimateProjectProgressForUnknown() {
        Assertions.assertEquals(32, PlatformUtils.estimateProjectProgress("UNKNOWN"));
    }

    @Test
    void platformUtils_shouldHandleEstimateTrainingCompletionForUnknown() {
        Assertions.assertEquals(60, PlatformUtils.estimateTrainingCompletion("UNKNOWN"));
    }

    @Test
    void platformUtils_shouldHandleBuildOperationRadiusForUnknown() {
        Assertions.assertEquals(700, PlatformUtils.buildOperationRadius("UNKNOWN"));
    }

    @Test
    void platformUtils_shouldHandleNormalizeCourseTypeForOnline() {
        Assertions.assertEquals("ARTICLE", PlatformUtils.normalizeCourseType("ONLINE"));
    }

    @Test
    void platformUtils_shouldHandleNormalizeCourseStatusForUnknown() {
        Assertions.assertEquals("DRAFT", PlatformUtils.normalizeCourseStatus("UNKNOWN"));
    }

    @Test
    void platformUtils_shouldHandleResolveSeatAvailableWithNullRequest() {
        Assertions.assertEquals(0, PlatformUtils.resolveSeatAvailable(30, 25, null));
    }

    @Test
    void platformUtils_shouldHandleResolveSeatAvailableWithSameSeatTotal() {
        Assertions.assertEquals(25, PlatformUtils.resolveSeatAvailable(30, 25, 30));
    }

    @Test
    void platformUtils_shouldHandleDisplayRoleForNull() {
        Assertions.assertEquals("未定义角色", PlatformUtils.displayRole(null));
    }

    @Test
    void platformUtils_shouldHandleSafePhoneForBlankPhone() {
        UserAccountEntity user = new UserAccountEntity();
        user.setPhone("");
        Assertions.assertEquals("-", PlatformUtils.safePhone(user));
    }

    @Test
    void platformUtils_shouldHandleDisplayNameForBlankRealName() {
        UserAccountEntity user = new UserAccountEntity();
        user.setRealName("  ");
        user.setUsername("testuser");
        Assertions.assertEquals("testuser", PlatformUtils.displayName(user));
    }

    @Test
    void platformUtils_shouldHandleDefaultIfBlankForNonBlank() {
        Assertions.assertEquals("hello", PlatformUtils.defaultIfBlank("  hello  ", "fallback"));
    }

    @Test
    void platformUtils_shouldHandleNormalizeNullableForNonBlank() {
        Assertions.assertEquals("test", PlatformUtils.normalizeNullable("  test  "));
    }

    @Test
    void platformUtils_shouldHandleToUidForNull() {
        Assertions.assertEquals("", PlatformUtils.toUid(null));
    }

    @Test
    void platformUtils_shouldHandleSafeIntForNull() {
        Assertions.assertEquals(0, PlatformUtils.safeInt(null));
    }

    @Test
    void platformUtils_shouldHandleDefaultBudgetForNonNull() {
        Assertions.assertEquals(BigDecimal.TEN, PlatformUtils.defaultBudget(BigDecimal.TEN));
    }

    @Test
    void platformUtils_shouldHandleBuildOrderRemarkForMapping() {
        Assertions.assertTrue(PlatformUtils.buildOrderRemark("MAPPING").contains("校准"));
    }

    @Test
    void platformUtils_shouldHandleBuildOrderRemarkForInspection() {
        Assertions.assertTrue(PlatformUtils.buildOrderRemark("INSPECTION").contains("巡检"));
    }

    @Test
    void platformUtils_shouldHandleBuildOrderRemarkForOther() {
        Assertions.assertTrue(PlatformUtils.buildOrderRemark("OTHER").contains("预约时间"));
    }

    @Test
    void platformUtils_shouldHandleParseRoleCaseInsensitive() {
        Assertions.assertEquals(RoleType.ADMIN, PlatformUtils.parseRole("admin"));
        Assertions.assertEquals(RoleType.PILOT, PlatformUtils.parseRole("pilot"));
    }

    @Test
    void platformUtils_shouldHandleEnsureRoleForAdmin() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        Assertions.assertDoesNotThrow(() -> PlatformUtils.ensureRole(admin, RoleType.ENTERPRISE));
    }

    @Test
    void platformUtils_shouldHandleEnsureRoleForSameRole() {
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Assertions.assertDoesNotThrow(() -> PlatformUtils.ensureRole(pilot, RoleType.PILOT));
    }

    @Test
    void platformUtils_shouldRejectEnsureRoleForDifferentRole() {
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Assertions.assertThrows(BizException.class, () -> PlatformUtils.ensureRole(pilot, RoleType.ENTERPRISE));
    }

    @Test
    void platformUtils_shouldHandleResolvePaymentStatusForNoOrders() {
        Assertions.assertEquals("待结算", PlatformUtils.resolvePaymentStatus(false, 0, 0));
    }

    @Test
    void platformUtils_shouldHandleResolvePaymentStatusForPartialPayment() {
        Assertions.assertEquals("部分结算", PlatformUtils.resolvePaymentStatus(true, 1, 3));
    }

    @Test
    void platformUtils_shouldHandleResolvePaymentStatusForFullPayment() {
        Assertions.assertEquals("已结算", PlatformUtils.resolvePaymentStatus(true, 3, 3));
    }

    @Test
    void platformUtils_shouldHandleResolvePaymentStatusForZeroPaid() {
        Assertions.assertEquals("待结算", PlatformUtils.resolvePaymentStatus(true, 0, 3));
    }

    @Test
    void platformUtils_shouldHandleMapAdminUserStatusForNull() {
        Assertions.assertEquals("待审核", PlatformUtils.mapAdminUserStatus(null));
    }

    @Test
    void platformUtils_shouldHandleMapAdminUserStatusForZero() {
        Assertions.assertEquals("停用", PlatformUtils.mapAdminUserStatus(0));
    }

    @Test
    void platformUtils_shouldHandleMapAdminUserStatusForNegative() {
        Assertions.assertEquals("停用", PlatformUtils.mapAdminUserStatus(-1));
    }

    @Test
    void platformUtils_shouldHandleMapAdminUserStatusForPositive() {
        Assertions.assertEquals("启用", PlatformUtils.mapAdminUserStatus(1));
    }

    @Test
    void platformUtils_shouldHandleEstimateRiskLevelForReviewing() {
        Assertions.assertEquals("中", PlatformUtils.estimateRiskLevel(true, false));
    }

    @Test
    void platformUtils_shouldHandleEstimateRiskLevelForNoOrders() {
        Assertions.assertEquals("中", PlatformUtils.estimateRiskLevel(false, false));
    }

    @Test
    void platformUtils_shouldHandleEstimateRiskLevelForHasOrders() {
        Assertions.assertEquals("低", PlatformUtils.estimateRiskLevel(false, true));
    }
}

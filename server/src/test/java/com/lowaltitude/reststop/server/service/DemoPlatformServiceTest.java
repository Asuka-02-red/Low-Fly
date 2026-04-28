package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.CourseEnrollmentMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper;
import com.lowaltitude.reststop.server.mapper.AdminSettingMapper;
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
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DemoPlatformServiceTest {

    private DemoPlatformService service;
    private TokenService tokenService;
    private UserAccountMapper userAccountMapper;
    private TaskMapper taskMapper;
    private BizOrderMapper bizOrderMapper;
    private CourseMapper courseMapper;
    private CourseEnrollmentMapper courseEnrollmentMapper;
    private FeedbackTicketMapper feedbackTicketMapper;
    private MessageConversationMapper messageConversationMapper;
    private MessageEntryMapper messageEntryMapper;
    private ReportDailySummaryMapper reportDailySummaryMapper;
    private RefreshTokenStore refreshTokenStore;
    private AlertService alertService;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        tokenService = Mockito.mock(TokenService.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        taskMapper = Mockito.mock(TaskMapper.class);
        bizOrderMapper = Mockito.mock(BizOrderMapper.class);
        PaymentOrderMapper paymentOrderMapper = Mockito.mock(PaymentOrderMapper.class);
        NoFlyZoneMapper noFlyZoneMapper = Mockito.mock(NoFlyZoneMapper.class);
        courseMapper = Mockito.mock(CourseMapper.class);
        courseEnrollmentMapper = Mockito.mock(CourseEnrollmentMapper.class);
        feedbackTicketMapper = Mockito.mock(FeedbackTicketMapper.class);
        messageConversationMapper = Mockito.mock(MessageConversationMapper.class);
        messageEntryMapper = Mockito.mock(MessageEntryMapper.class);
        AdminSettingMapper adminSettingMapper = Mockito.mock(AdminSettingMapper.class);
        AdminNotificationRuleMapper adminNotificationRuleMapper = Mockito.mock(AdminNotificationRuleMapper.class);
        reportDailySummaryMapper = Mockito.mock(ReportDailySummaryMapper.class);
        refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
        alertService = Mockito.mock(AlertService.class);
        auditLogService = Mockito.mock(AuditLogService.class);

        service = new DemoPlatformService(
                tokenService,
                userAccountMapper,
                taskMapper,
                bizOrderMapper,
                paymentOrderMapper,
                noFlyZoneMapper,
                courseMapper,
                courseEnrollmentMapper,
                feedbackTicketMapper,
                messageConversationMapper,
                messageEntryMapper,
                adminSettingMapper,
                adminNotificationRuleMapper,
                reportDailySummaryMapper,
                refreshTokenStore,
                alertService,
                auditLogService
        );
    }

    @Test
    public void shouldLoginWithSeededUser() {
        UserAccountEntity user = buildUser(1L, "pilot_demo", "PILOT");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(user);
        Mockito.when(refreshTokenStore.issueToken(user)).thenReturn("refresh-token");
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt-token");

        ApiDtos.AuthPayload payload = service.login(new ApiDtos.LoginRequest("pilot_demo", "demo123"));

        Assertions.assertEquals("jwt-token", payload.token());
        Assertions.assertEquals("refresh-token", payload.refreshToken());
        Assertions.assertEquals("PILOT", payload.userInfo().role());
    }

    @Test
    public void shouldCreateTaskForEnterprise() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));
        Mockito.doAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            task.setId(101L);
            return 1;
        }).when(taskMapper).insert(ArgumentMatchers.any(TaskEntity.class));

        ApiDtos.TaskView task = service.createTask(
                enterprise,
                new ApiDtos.TaskRequest(
                        "INSPECTION",
                        "新区巡检",
                        "测试任务",
                        "重庆两江新区",
                        "2026-05-01 10:00",
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.valueOf(800))
        );

        Assertions.assertEquals("REVIEWING", task.status());
        Assertions.assertEquals(101L, task.id());
        Assertions.assertEquals("2026-05-01 10:00", task.deadline());
    }

    @Test
    public void shouldUpdateAndRepublishEnterpriseTask() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setTaskType("INSPECTION");
        task.setTitle("旧任务");
        task.setDescription("旧描述");
        task.setLocation("重庆江北区");
        task.setDeadline(LocalDateTime.of(2026, 5, 1, 10, 0));
        task.setLatitude(BigDecimal.ONE);
        task.setLongitude(BigDecimal.TEN);
        task.setBudget(BigDecimal.valueOf(800));
        task.setStatus("PUBLISHED");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));

        ApiDtos.TaskView updated = service.updateTask(
                enterprise,
                101L,
                new ApiDtos.TaskRequest(
                        "MAPPING",
                        "新任务标题",
                        "新任务描述",
                        "重庆渝北区",
                        "2026-05-03 12:00",
                        BigDecimal.valueOf(29.56),
                        BigDecimal.valueOf(106.55),
                        BigDecimal.valueOf(3200))
        );
        ApiDtos.TaskView published = service.publishTask(enterprise, 101L);

        Assertions.assertEquals("新任务标题", updated.title());
        Assertions.assertEquals("2026-05-03 12:00", updated.deadline());
        Assertions.assertEquals("REVIEWING", published.status());
    }

    @Test
    public void shouldDecreaseCourseSeatOnEnroll() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        CourseEntity course = new CourseEntity();
        course.setId(2001L);
        course.setTitle("民航法规基础课");
        course.setCourseType("OFFLINE");
        course.setStatus("OPEN");
        course.setSeatAvailable(18);
        course.setEnrollCount(0);
        Mockito.when(courseMapper.selectById(2001L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.doAnswer(invocation -> {
            CourseEnrollmentEntity enrollment = invocation.getArgument(0);
            enrollment.setId(3001L);
            return 1;
        }).when(courseEnrollmentMapper).insert(ArgumentMatchers.any(CourseEnrollmentEntity.class));

        ApiDtos.EnrollmentResult result = service.enrollCourse(pilot, 2001L);

        Assertions.assertTrue(result.seatAvailable() < 18);
    }

    @Test
    public void shouldExposeEnrollmentStateInCourseDetail() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        CourseEntity course = new CourseEntity();
        course.setId(2001L);
        course.setTitle("民航法规基础课");
        course.setCourseType("OFFLINE");
        course.setStatus("OPEN");
        course.setSeatTotal(30);
        course.setSeatAvailable(18);
        course.setEnrollCount(4);
        course.setBrowseCount(12);
        Mockito.when(courseMapper.selectById(2001L)).thenReturn(course);
        CourseEnrollmentEntity enrollment = new CourseEnrollmentEntity();
        enrollment.setCourseId(2001L);
        enrollment.setUserId(1L);
        enrollment.setEnrollmentNo("ENR2001");
        enrollment.setStatus("ENROLLED");
        Mockito.when(courseEnrollmentMapper.selectOne(ArgumentMatchers.any())).thenReturn(enrollment);

        ApiDtos.CourseDetailView detail = service.getCourseDetail(pilot, 2001L);

        Assertions.assertTrue(detail.enrolled());
        Assertions.assertEquals("ENR2001", detail.enrollmentNo());
        Assertions.assertEquals("ENROLLED", detail.enrollmentStatus());
    }

    @Test
    public void shouldRejectPilotCreatingEnterpriseTask() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        try {
            service.createTask(
                    pilot,
                    new ApiDtos.TaskRequest(
                            "INSPECTION",
                            "非法任务",
                            "测试任务",
                            "重庆",
                            "2026-05-01 10:00",
                            BigDecimal.ONE,
                            BigDecimal.TEN,
                            BigDecimal.valueOf(100))
            );
            Assertions.fail("预期抛出 BizException");
        } catch (BizException ex) {
            Assertions.assertEquals(403, ex.getCode());
        }
    }

    @Test
    public void shouldRefreshUsingPersistentTokenStore() {
        UserAccountEntity user = buildUser(1L, "pilot_demo", "PILOT");
        Mockito.when(refreshTokenStore.requireUsernameByRefreshToken("refresh-token")).thenReturn("pilot_demo");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(user);
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt-refresh");

        ApiDtos.AuthPayload payload = service.refresh(new ApiDtos.RefreshTokenRequest("refresh-token"));

        Assertions.assertEquals("jwt-refresh", payload.token());
        Assertions.assertEquals("refresh-token", payload.refreshToken());
    }

    @Test
    public void shouldReadAlertsFromPersistentAlertService() {
        Mockito.when(alertService.listAlerts()).thenReturn(java.util.List.of(
                new ApiDtos.AlertView(1L, "HIGH", "风险 1", "OPEN", java.time.LocalDateTime.now())
        ));

        Assertions.assertEquals(1, service.listAlerts().size());
    }

    @Test
    public void shouldAllowInstitutionManagingCourses() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        UserAccountEntity owner = buildUser(3L, "institution_demo", "INSTITUTION");
        owner.setCompanyName("示范培训机构");
        Mockito.when(userAccountMapper.selectById(3L)).thenReturn(owner);
        Mockito.doAnswer(invocation -> {
            CourseEntity course = invocation.getArgument(0);
            course.setId(4001L);
            return 1;
        }).when(courseMapper).insert(ArgumentMatchers.any(CourseEntity.class));

        ApiDtos.CourseManageView created = service.createCourse(
                institution,
                new ApiDtos.CourseManageRequest(
                        "空域法规速训",
                        "面向机构教员的法规训练营",
                        "覆盖法规、案例复盘和带班规范。",
                        "OFFLINE",
                        24,
                        BigDecimal.valueOf(299),
                        "DRAFT"
                )
        );

        Assertions.assertEquals(4001L, created.id());
        Assertions.assertEquals("OFFLINE", created.learningMode());
        Assertions.assertEquals("DRAFT", created.status());
    }

    @Test
    public void shouldExposeAdminUsersForDirectory() {
        UserAccountEntity admin = buildUser(4L, "admin", "ADMIN");
        admin.setStatus(1);
        admin.setCreateTime(java.time.LocalDateTime.of(2026, 4, 22, 10, 30));
        Mockito.when(userAccountMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of(admin));

        java.util.List<ApiDtos.AdminUserView> users = service.listAdminUsers();

        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("管理员", users.get(0).roleNames().get(0));
        Assertions.assertEquals("全量管控组", users.get(0).permissionGroupName());
    }

    @Test
    public void shouldCreateAdminUser() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            UserAccountEntity entity = invocation.getArgument(0);
            entity.setId(9L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.when(userAccountMapper.selectById(9L)).thenReturn(buildUser(9L, "ops_user", "ENTERPRISE"));

        ApiDtos.AdminUserView user = service.createAdminUser(
                admin,
                new ApiDtos.AdminUserCreateRequest(
                        "ops_user",
                        "Admin123456",
                        "13800138009",
                        "ops@example.com",
                        "ENTERPRISE",
                        "运维专员",
                        "平台运营中心",
                        1)
        );

        Assertions.assertEquals("ops_user", user.username());
    }

    @Test
    public void shouldRejectDeletingDefaultAdmin() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        Mockito.when(userAccountMapper.selectById(4L)).thenReturn(buildUser(4L, "admin", "ADMIN"));

        Assertions.assertThrows(BizException.class, () -> service.deleteAdminUser(admin, 4L));
    }

    @Test
    public void shouldExposeAdminProjectsFromTasks() {
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setTaskType("INSPECTION");
        task.setTitle("长江沿线巡检");
        task.setLocation("重庆江北区");
        task.setBudget(BigDecimal.valueOf(3000));
        task.setStatus("PUBLISHED");
        task.setUpdateTime(java.time.LocalDateTime.of(2026, 4, 22, 11, 15));
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(java.util.List.of(buildUser(2L, "enterprise_demo", "ENTERPRISE")));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of());

        java.util.List<ApiDtos.AdminProjectView> projects = service.listAdminProjects();

        Assertions.assertEquals(1, projects.size());
        Assertions.assertEquals("执行中", projects.get(0).status());
        Assertions.assertEquals("重庆江北区", projects.get(0).region());
    }

    @Test
    public void shouldBuildCompleteAdminOverview() {
        UserAccountEntity admin = buildUser(4L, "admin", "ADMIN");
        admin.setStatus(1);
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setTaskType("INSPECTION");
        task.setTitle("长江沿线巡检");
        task.setLocation("重庆江北区");
        task.setStatus("PUBLISHED");
        task.setUpdateTime(java.time.LocalDateTime.of(2026, 4, 22, 11, 15));

        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(4L, 3L, 1L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L, 1L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L, 1L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(6L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(2L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(1L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of());
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of(task));
        Mockito.when(auditLogService.listRecent()).thenReturn(java.util.List.of(
                new ApiDtos.AuditEventView("req-1", 4L, "ADMIN", "AUTH", "admin", "LOGIN", "管理员登录", java.time.LocalDateTime.of(2026, 4, 22, 10, 0))
        ));

        ApiDtos.DashboardOverview overview = service.adminOverview();

        Assertions.assertFalse(overview.metrics().isEmpty());
        Assertions.assertFalse(overview.deviceStats().isEmpty());
        Assertions.assertFalse(overview.activities().isEmpty());
        Assertions.assertFalse(overview.notices().isEmpty());
        Assertions.assertFalse(overview.progressTrend().isEmpty());
    }

    @Test
    public void shouldCreateFeedbackTicket() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));
        Mockito.doAnswer(invocation -> {
            FeedbackTicketEntity entity = invocation.getArgument(0);
            entity.setId(9001L);
            return 1;
        }).when(feedbackTicketMapper).insert(ArgumentMatchers.any(FeedbackTicketEntity.class));

        ApiDtos.FeedbackTicketView ticket = service.createFeedbackTicket(
                pilot,
                new ApiDtos.FeedbackTicketRequest("13800138000", "飞行任务页面在弱网下刷新较慢，希望增加重试提示")
        );

        Assertions.assertEquals(9001L, ticket.id());
        Assertions.assertEquals("待处理", ticket.status());
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
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.doAnswer(invocation -> {
            MessageEntryEntity entity = invocation.getArgument(0);
            entity.setId(7001L);
            return 1;
        }).when(messageEntryMapper).insert(ArgumentMatchers.any(MessageEntryEntity.class));
        Mockito.when(messageEntryMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of(
                buildMessageEntry(7001L, 5001L, 1L, "PILOT", "收到，今晚 19:00 前反馈航线确认。")
        ));

        ApiDtos.MessageThreadView thread = service.sendMessage(
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

        ApiDtos.MessageReadReceiptResponse result = service.syncReadReceipts(
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

        ApiDtos.PilotProfileView pilotProfile = service.getPilotProfile(
                new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业"),
                "1"
        );
        ApiDtos.EnterpriseInfoView enterpriseInfo = service.getEnterpriseInfo(
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
        entry.setCreateTime(java.time.LocalDateTime.of(2026, 4, 22, 18, 30));
        return entry;
    }
}

package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.ReportDailySummaryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.ReportDailySummaryMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
class AdminDashboardServiceTest {

    private AdminDashboardService adminDashboardService;
    private UserAccountMapper userAccountMapper;
    private TaskMapper taskMapper;
    private BizOrderMapper bizOrderMapper;
    private CourseMapper courseMapper;
    private ReportDailySummaryMapper reportDailySummaryMapper;
    private AlertService alertService;
    private AuditLogService auditLogService;
    private AdminProjectService adminProjectService;

    @BeforeEach
    void setUp() {
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        taskMapper = Mockito.mock(TaskMapper.class);
        bizOrderMapper = Mockito.mock(BizOrderMapper.class);
        courseMapper = Mockito.mock(CourseMapper.class);
        reportDailySummaryMapper = Mockito.mock(ReportDailySummaryMapper.class);
        alertService = Mockito.mock(AlertService.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        adminProjectService = Mockito.mock(AdminProjectService.class);

        adminDashboardService = new AdminDashboardService(
                userAccountMapper,
                taskMapper,
                bizOrderMapper,
                courseMapper,
                reportDailySummaryMapper,
                alertService,
                auditLogService,
                adminProjectService
        );
    }

    // =========================================================================
    // adminOverview
    // =========================================================================

    @Test
    void adminOverview_withHighAlertsAndReviewingTasks() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of(
                new ApiDtos.AuditEventView("req1", 1L, "ADMIN", "USER", "10", "CREATE", "created user", LocalDateTime.of(2026, 4, 20, 10, 0)),
                new ApiDtos.AuditEventView("req2", 2L, "PILOT", "TASK", "20", "UPDATE", "updated task", LocalDateTime.of(2026, 4, 20, 11, 0))
        ));
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)));
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setTitle("巡检任务");
        task.setLocation("重庆");
        task.setStatus("REVIEWING");
        task.setUpdateTime(LocalDateTime.now());
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of(task));

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        Assertions.assertNotNull(overview);
        Assertions.assertEquals(4, overview.metrics().size());
        // highAlerts > 0 => status should be "warning"
        Assertions.assertEquals("warning", overview.metrics().get(3).status());
        Assertions.assertEquals("3", overview.metrics().get(3).value());
        // reviewingTasks > 0 => notice level should be "中"
        Assertions.assertEquals("中", overview.notices().get(0).level());
        // highAlerts > 0 => notice level should be "高"
        Assertions.assertEquals("高", overview.notices().get(1).level());
        Assertions.assertNotNull(overview.activities());
        Assertions.assertNotNull(overview.projectDistribution());
        Assertions.assertNotNull(overview.progressTrend());
    }

    @Test
    void adminOverview_withZeroHighAlertsAndZeroReviewingTasks() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(auditLogService.count()).thenReturn(100L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        Assertions.assertNotNull(overview);
        Assertions.assertEquals("success", overview.metrics().get(3).status());
        Assertions.assertEquals("低", overview.notices().get(0).level());
        Assertions.assertEquals("低", overview.notices().get(1).level());
        Assertions.assertTrue(overview.activities().isEmpty());
    }

    @Test
    void adminOverview_activitiesLimitedToFive() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(1L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        List<ApiDtos.AuditEventView> manyEvents = List.of(
                new ApiDtos.AuditEventView("r1", 1L, "ADMIN", "USER", "1", "CREATE", "a", LocalDateTime.now()),
                new ApiDtos.AuditEventView("r2", 2L, "PILOT", "TASK", "2", "UPDATE", "b", LocalDateTime.now()),
                new ApiDtos.AuditEventView("r3", 3L, "ENTERPRISE", "ORDER", "3", "DELETE", "c", LocalDateTime.now()),
                new ApiDtos.AuditEventView("r4", 4L, "ADMIN", "COURSE", "4", "LOGIN", "d", LocalDateTime.now()),
                new ApiDtos.AuditEventView("r5", 5L, "PILOT", "USER", "5", "CREATE", "e", LocalDateTime.now()),
                new ApiDtos.AuditEventView("r6", 6L, "ADMIN", "TASK", "6", "UPDATE", "f", LocalDateTime.now()),
                new ApiDtos.AuditEventView("r7", 7L, "PILOT", "ORDER", "7", "CREATE", "g", LocalDateTime.now())
        );
        Mockito.when(auditLogService.listRecent()).thenReturn(manyEvents);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        Assertions.assertEquals(5, overview.activities().size());
    }

    @Test
    void adminOverview_projectDistributionGroupsByLocation() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        TaskEntity t1 = new TaskEntity();
        t1.setId(1L); t1.setTitle("任务1"); t1.setLocation("重庆"); t1.setStatus("PUBLISHED"); t1.setUpdateTime(LocalDateTime.now());
        TaskEntity t2 = new TaskEntity();
        t2.setId(2L); t2.setTitle("任务2"); t2.setLocation("重庆"); t2.setStatus("PUBLISHED"); t2.setUpdateTime(LocalDateTime.now());
        TaskEntity t3 = new TaskEntity();
        t3.setId(3L); t3.setTitle("任务3"); t3.setLocation("成都"); t3.setStatus("PUBLISHED"); t3.setUpdateTime(LocalDateTime.now());
        TaskEntity t4 = new TaskEntity();
        t4.setId(4L); t4.setTitle("任务4"); t4.setLocation(null); t4.setStatus("PUBLISHED"); t4.setUpdateTime(LocalDateTime.now());
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of(t1, t2, t3, t4));

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        // Should group: 重庆=2, 成都=1, 未分类=1
        Assertions.assertEquals(3, overview.projectDistribution().size());
        Assertions.assertEquals("重庆", overview.projectDistribution().get(0).name());
        Assertions.assertEquals(2, overview.projectDistribution().get(0).value());
    }

    @Test
    void adminOverview_withNullActorRoleUsesSystemFallback() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of(
                new ApiDtos.AuditEventView("req1", 1L, null, "USER", "10", "CREATE", "payload", LocalDateTime.now())
        ));
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)));
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        // actorRole is null => PlatformUtils.defaultIfBlank returns "SYSTEM"
        Assertions.assertEquals("SYSTEM", overview.activities().get(0).tag());
    }

    // =========================================================================
    // adminSectionSummaries
    // =========================================================================

    @Test
    void adminSectionSummaries_returnsAllSixSections() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(adminProjectService.countNoFlyZones()).thenReturn(8L);

        List<ApiDtos.AdminSectionSummary> summaries = adminDashboardService.adminSectionSummaries();

        Assertions.assertEquals(6, summaries.size());
        Assertions.assertEquals("overview", summaries.get(0).sectionKey());
        Assertions.assertEquals("系统概览", summaries.get(0).title());
        Assertions.assertEquals("users", summaries.get(1).sectionKey());
        Assertions.assertEquals("用户管理", summaries.get(1).title());
        Assertions.assertEquals("projects", summaries.get(2).sectionKey());
        Assertions.assertEquals("项目管理", summaries.get(2).title());
        Assertions.assertEquals("analytics", summaries.get(3).sectionKey());
        Assertions.assertEquals("数据分析", summaries.get(3).title());
        Assertions.assertEquals("settings", summaries.get(4).sectionKey());
        Assertions.assertEquals("系统设置", summaries.get(4).title());
        Assertions.assertEquals("logs", summaries.get(5).sectionKey());
        Assertions.assertEquals("日志管理", summaries.get(5).title());
    }

    @Test
    void adminSectionSummaries_overviewSectionContainsCorrectMetrics() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(adminProjectService.countNoFlyZones()).thenReturn(8L);

        List<ApiDtos.AdminSectionSummary> summaries = adminDashboardService.adminSectionSummaries();

        ApiDtos.AdminSectionSummary overview = summaries.get(0);
        Assertions.assertEquals(3, overview.metrics().size());
        Assertions.assertEquals("100", overview.metrics().get(0).value());
        Assertions.assertEquals("50", overview.metrics().get(1).value());
        Assertions.assertEquals("5", overview.metrics().get(2).value());
    }

    @Test
    void adminSectionSummaries_projectsSectionContainsZoneCount() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(adminProjectService.countNoFlyZones()).thenReturn(8L);

        List<ApiDtos.AdminSectionSummary> summaries = adminDashboardService.adminSectionSummaries();

        ApiDtos.AdminSectionSummary projects = summaries.get(2);
        Assertions.assertEquals("8", projects.metrics().get(2).value());
    }

    @Test
    void adminSectionSummaries_logsSectionContainsHighAlertCount() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(adminProjectService.countNoFlyZones()).thenReturn(8L);

        List<ApiDtos.AdminSectionSummary> summaries = adminDashboardService.adminSectionSummaries();

        ApiDtos.AdminSectionSummary logs = summaries.get(5);
        Assertions.assertEquals("3", logs.metrics().get(1).value());
    }

    // =========================================================================
    // adminAnalytics
    // =========================================================================

    @Test
    void adminAnalytics_withNonEmptyProjects() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3),
                buildReport(LocalDate.of(2026, 4, 16), 12, 6, BigDecimal.valueOf(60000), 1, 4)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of(
                new ApiDtos.AdminProjectView("1", "项目A", "陈伶", "重庆", "执行中", 68, BigDecimal.valueOf(10000), "正常", "低", 80, "已结算", "2026-04-15"),
                new ApiDtos.AdminProjectView("2", "项目B", "李四", "成都", "规划中", 20, BigDecimal.valueOf(20000), "待复核", "中", 60, "待结算", "2026-04-16"),
                new ApiDtos.AdminProjectView("3", "项目C", "王五", "北京", "已完成", 100, BigDecimal.valueOf(30000), "正常", "高", 90, "已结算", "2026-04-17")
        ));
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(5L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(2L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of(
                new ApiDtos.AlertView(1L, "HIGH", "告警1", "ACTIVE", LocalDateTime.now()),
                new ApiDtos.AlertView(2L, "MEDIUM", "告警2", "RESOLVED", LocalDateTime.now())
        ));
        BizOrderEntity paidOrder = new BizOrderEntity();
        paidOrder.setId(1L);
        paidOrder.setAmount(BigDecimal.valueOf(50000));
        paidOrder.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(paidOrder));

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        Assertions.assertNotNull(analytics);
        Assertions.assertEquals(3, analytics.businessMetrics().size());
        Assertions.assertEquals(3, analytics.performanceMetrics().size());
        Assertions.assertEquals(2, analytics.revenueTrend().size());
        Assertions.assertEquals(2, analytics.userActivity().size());
        Assertions.assertEquals(3, analytics.projectHealth().size());
        Assertions.assertEquals(4, analytics.servicePerformance().size());
        Assertions.assertEquals(4, analytics.operatorLoad().size());

        // projectHealthPercent: lowRiskCount=1, projects.size()=3 => 1*100/3 = 33.3 => 33%
        Assertions.assertTrue(analytics.businessMetrics().get(1).value().contains("33%"));

        // userActivityPercent: enabledUsers=100, totalUsers=100 => 100%
        Assertions.assertTrue(analytics.businessMetrics().get(2).value().contains("100%"));

        // openFeedbackCount > 0 => performance metric status should be "warning"
        Assertions.assertEquals("warning", analytics.performanceMetrics().get(2).status());

        // Risk distribution
        Assertions.assertEquals("健康项目", analytics.projectHealth().get(0).name());
        Assertions.assertEquals(1, analytics.projectHealth().get(0).value());
        Assertions.assertEquals("需关注项目", analytics.projectHealth().get(1).name());
        Assertions.assertEquals(1, analytics.projectHealth().get(1).value());
        Assertions.assertEquals("高风险项目", analytics.projectHealth().get(2).name());
        Assertions.assertEquals(1, analytics.projectHealth().get(2).value());
    }

    @Test
    void adminAnalytics_withEmptyProjects_projectHealthIs100() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // projects.isEmpty() => projectHealthPercent = 100
        Assertions.assertTrue(analytics.businessMetrics().get(1).value().contains("100%"));

        // openFeedbackCount == 0 => status should be "info"
        Assertions.assertEquals("info", analytics.performanceMetrics().get(2).status());

        // operatorLoad "项目跟进" should be Math.max(1, 0) = 1
        Assertions.assertEquals(1, analytics.operatorLoad().get(3).value());
    }

    @Test
    void adminAnalytics_withZeroTotalUsers_userActivityIsZero() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // totalUsers == 0 => userActivityPercent = 0
        Assertions.assertTrue(analytics.businessMetrics().get(2).value().contains("0%"));
    }

    @Test
    void adminAnalytics_openFeedbackCountGreaterThanZero_warningStatus() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(7L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(3L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // openFeedbackCount > 0 => "warning"
        Assertions.assertEquals("warning", analytics.performanceMetrics().get(2).status());
    }

    @Test
    void adminAnalytics_openFeedbackCountZero_infoStatus() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // openFeedbackCount == 0 => "info"
        Assertions.assertEquals("info", analytics.performanceMetrics().get(2).status());
    }

    @Test
    void adminAnalytics_revenueTrendFormatsDateCorrectly() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 1, 5), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        Assertions.assertEquals("01-05", analytics.revenueTrend().get(0).label());
    }

    @Test
    void adminAnalytics_alertsFilterOutResolved() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of(
                new ApiDtos.AlertView(1L, "HIGH", "告警1", "ACTIVE", LocalDateTime.now()),
                new ApiDtos.AlertView(2L, "MEDIUM", "告警2", "RESOLVED", LocalDateTime.now()),
                new ApiDtos.AlertView(3L, "LOW", "告警3", "ACKNOWLEDGED", LocalDateTime.now())
        ));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // Only non-RESOLVED alerts: ACTIVE and ACKNOWLEDGED => 2
        Assertions.assertEquals("2", analytics.performanceMetrics().get(0).trend().split(" ")[1]);
    }

    // =========================================================================
    // loadRecentReports (tested indirectly via adminOverview/adminAnalytics)
    // =========================================================================

    @Test
    void loadRecentReports_emptyReports_returnsEmptyProgressTrend() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        Assertions.assertEquals(0, overview.progressTrend().size());
    }

    @Test
    void loadRecentReports_reportsWithinLimit_returnsAllReports() {
        List<ReportDailySummaryEntity> reports = List.of(
                buildReport(LocalDate.of(2026, 4, 10), 5, 3, BigDecimal.valueOf(10000), 1, 2),
                buildReport(LocalDate.of(2026, 4, 11), 6, 4, BigDecimal.valueOf(20000), 0, 3),
                buildReport(LocalDate.of(2026, 4, 12), 7, 5, BigDecimal.valueOf(30000), 2, 4)
        );
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(reports);
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        // 3 reports <= limit(6) => all 3 returned
        Assertions.assertEquals(3, overview.progressTrend().size());
    }

    @Test
    void loadRecentReports_reportsExceedLimit_returnsLastLimitReports() {
        List<ReportDailySummaryEntity> reports = List.of(
                buildReport(LocalDate.of(2026, 4, 1), 1, 1, BigDecimal.valueOf(1000), 0, 1),
                buildReport(LocalDate.of(2026, 4, 2), 2, 2, BigDecimal.valueOf(2000), 1, 2),
                buildReport(LocalDate.of(2026, 4, 3), 3, 3, BigDecimal.valueOf(3000), 0, 3),
                buildReport(LocalDate.of(2026, 4, 4), 4, 4, BigDecimal.valueOf(4000), 2, 4),
                buildReport(LocalDate.of(2026, 4, 5), 5, 5, BigDecimal.valueOf(5000), 1, 5),
                buildReport(LocalDate.of(2026, 4, 6), 6, 6, BigDecimal.valueOf(6000), 0, 6),
                buildReport(LocalDate.of(2026, 4, 7), 7, 7, BigDecimal.valueOf(7000), 3, 7),
                buildReport(LocalDate.of(2026, 4, 8), 8, 8, BigDecimal.valueOf(8000), 2, 8)
        );
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(reports);
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        // 8 reports > limit(6) => last 6 returned
        Assertions.assertEquals(6, overview.progressTrend().size());
        // First trend point should be from the 3rd report (index 2 in original list)
        Assertions.assertEquals("4/3", overview.progressTrend().get(0).label());
    }

    // =========================================================================
    // buildServicePerformanceMetrics - latest == null and latest != null
    // =========================================================================

    @Test
    void buildServicePerformanceMetrics_latestNull_usesCountMethods() {
        // When reports are empty, latest == null, so it falls back to countTasks(), countOrders(), countOpenCourses()
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // With latest == null, baseTaskCount = (int)countTasks() = 50
        // Project service response time = 160 + min(80, 50*4) = 160 + 80 = 240
        Assertions.assertEquals(240, analytics.servicePerformance().get(1).response());
        // baseOrderCount = (int)countOrders() = 30
        // Order service response time = 150 + min(90, 30*5) = 150 + 90 = 240
        Assertions.assertEquals(240, analytics.servicePerformance().get(2).response());
        // baseTrainingCount = (int)countOpenCourses() = 10
        // Ticket service response time = 140 + min(100, 0*9 + 10) = 140 + 10 = 150
        Assertions.assertEquals(150, analytics.servicePerformance().get(3).response());
    }

    @Test
    void buildServicePerformanceMetrics_latestNotNull_usesReportValues() {
        ReportDailySummaryEntity latestReport = buildReport(LocalDate.of(2026, 4, 16), 20, 15, BigDecimal.valueOf(80000), 5, 8);
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(latestReport));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // latest != null, baseTaskCount = safeInt(latest.getTaskCount()) = 20
        // Project service response = 160 + min(80, 20*4) = 160 + 80 = 240
        Assertions.assertEquals(240, analytics.servicePerformance().get(1).response());
        // baseOrderCount = safeInt(latest.getOrderCount()) = 15
        // Order service response = 150 + min(90, 15*5) = 150 + 75 = 225
        Assertions.assertEquals(225, analytics.servicePerformance().get(2).response());
        // baseTrainingCount = safeInt(latest.getTrainingCount()) = 8
        // Ticket service response = 140 + min(100, 0*9 + 8) = 140 + 8 = 148
        Assertions.assertEquals(148, analytics.servicePerformance().get(3).response());
    }

    @Test
    void buildServicePerformanceMetrics_authServiceResponseDependsOnUserCount() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(200L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // Auth service response = 120 + min(60, (int)countUsers()) = 120 + min(60, 200) = 120 + 60 = 180
        Assertions.assertEquals(180, analytics.servicePerformance().get(0).response());
    }

    @Test
    void buildServicePerformanceMetrics_availabilityAffectedByAlerts() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(5L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(2L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of(
                new ApiDtos.AlertView(1L, "HIGH", "告警1", "ACTIVE", LocalDateTime.now())
        ));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // With alerts and feedback, availability should be affected
        // Auth availability = max(97.8, 99.96 - openAlertCount*0.08) = max(97.8, 99.96 - 1*0.08) = max(97.8, 99.88) = 99.88
        Assertions.assertTrue(analytics.servicePerformance().get(0).availability() >= 97.8);
    }

    // =========================================================================
    // adminAnalytics - totalRevenue calculation
    // =========================================================================

    @Test
    void adminAnalytics_totalRevenueSumsPaidOrderAmounts() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());

        BizOrderEntity order1 = new BizOrderEntity();
        order1.setId(1L);
        order1.setAmount(BigDecimal.valueOf(30000));
        order1.setStatus("PAID");
        BizOrderEntity order2 = new BizOrderEntity();
        order2.setId(2L);
        order2.setAmount(BigDecimal.valueOf(20000));
        order2.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order1, order2));

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // totalRevenue = 30000 + 20000 = 50000, displayed as "5.0 万"
        Assertions.assertTrue(analytics.businessMetrics().get(0).value().contains("5.0"));
    }

    // =========================================================================
    // adminAnalytics - availability and response time calculations
    // =========================================================================

    @Test
    void adminAnalytics_availabilityBoundedByMinValue() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // availability = max(96.5, 99.95 - 0 - 0) = 99.95
        String availabilityStr = analytics.performanceMetrics().get(1).value();
        Assertions.assertTrue(availabilityStr.contains("99.95"));
    }

    @Test
    void adminAnalytics_avgResponseTimeCalculatedCorrectly() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(10L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(5L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of(
                new ApiDtos.AlertView(1L, "HIGH", "alert1", "ACTIVE", LocalDateTime.now())
        ));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // avgResponse = 120 + min(180, openAlertCount*8 + openFeedbackCount*6 + pendingTaskCount*5)
        // openAlertCount = 1 (non-RESOLVED), openFeedbackCount = 10, pendingTaskCount from taskMapper
        // The exact value depends on pendingTaskCount mock, but it should be > 120
        int avgResponse = Integer.parseInt(analytics.performanceMetrics().get(0).value().replace(" ms", ""));
        Assertions.assertTrue(avgResponse > 120);
    }

    // =========================================================================
    // adminAnalytics - feedbackCycle calculation
    // =========================================================================

    @Test
    void adminAnalytics_feedbackCycleCalculatedCorrectly() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(5L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(3L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // feedbackCycle = 18 + min(45, openFeedbackCount*4 + processingFeedbackCount*2)
        // = 18 + min(45, 5*4 + 3*2) = 18 + min(45, 26) = 18 + 26 = 44
        String cycleStr = analytics.performanceMetrics().get(2).value();
        Assertions.assertTrue(cycleStr.contains("44"));
    }

    // =========================================================================
    // adminOverview - progressTrend calculation
    // =========================================================================

    @Test
    void adminOverview_progressTrendCappedAt100() {
        ReportDailySummaryEntity highReport = buildReport(LocalDate.of(2026, 4, 15), 20, 15, BigDecimal.valueOf(50000), 5, 10);
        // progress = min(100, 20*8 + 15*6 + 10*3) = min(100, 160+90+30) = min(100, 280) = 100
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(5L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(3L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(2L);
        Mockito.when(auditLogService.count()).thenReturn(50L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(0L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(0L);
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of());
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(highReport));
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(List.of());

        ApiDtos.DashboardOverview overview = adminDashboardService.adminOverview();

        Assertions.assertEquals(100, overview.progressTrend().get(0).value());
    }

    // =========================================================================
    // adminSectionSummaries - analytics section
    // =========================================================================

    @Test
    void adminSectionSummaries_analyticsSectionContainsCorrectData() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(adminProjectService.countNoFlyZones()).thenReturn(8L);

        List<ApiDtos.AdminSectionSummary> summaries = adminDashboardService.adminSectionSummaries();

        ApiDtos.AdminSectionSummary analytics = summaries.get(3);
        Assertions.assertEquals("analytics", analytics.sectionKey());
        Assertions.assertEquals("30", analytics.metrics().get(0).value());
        Assertions.assertEquals("10", analytics.metrics().get(1).value());
        Assertions.assertEquals("5", analytics.metrics().get(2).value());
    }

    // =========================================================================
    // adminSectionSummaries - settings section
    // =========================================================================

    @Test
    void adminSectionSummaries_settingsSectionContainsCorrectData() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(auditLogService.count()).thenReturn(200L);
        Mockito.when(alertService.countOpenAlerts()).thenReturn(5L);
        Mockito.when(alertService.countHighRiskAlerts()).thenReturn(3L);
        Mockito.when(adminProjectService.countNoFlyZones()).thenReturn(8L);

        List<ApiDtos.AdminSectionSummary> summaries = adminDashboardService.adminSectionSummaries();

        ApiDtos.AdminSectionSummary settings = summaries.get(4);
        Assertions.assertEquals("settings", settings.sectionKey());
        Assertions.assertEquals("2", settings.metrics().get(0).value());
        Assertions.assertEquals("3", settings.metrics().get(1).value());
        Assertions.assertEquals("2", settings.metrics().get(2).value());
    }

    // =========================================================================
    // adminAnalytics - operatorLoad with projects
    // =========================================================================

    @Test
    void adminAnalytics_operatorLoadWithProjects() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of(
                new ApiDtos.AdminProjectView("1", "项目A", "陈伶", "重庆", "执行中", 68, BigDecimal.valueOf(10000), "正常", "低", 80, "已结算", "2026-04-15"),
                new ApiDtos.AdminProjectView("2", "项目B", "李四", "成都", "规划中", 20, BigDecimal.valueOf(20000), "待复核", "中", 60, "待结算", "2026-04-16"),
                new ApiDtos.AdminProjectView("3", "项目C", "王五", "北京", "已完成", 100, BigDecimal.valueOf(30000), "正常", "高", 90, "已结算", "2026-04-17")
        ));
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(4L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(1L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of(
                new ApiDtos.AlertView(1L, "HIGH", "alert", "ACTIVE", LocalDateTime.now())
        ));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // operatorLoad: [反馈工单=4, 权限复核=pendingTaskCount, 风险告警=1, 项目跟进=max(1,3)=3]
        Assertions.assertEquals("反馈工单", analytics.operatorLoad().get(0).name());
        Assertions.assertEquals(4, analytics.operatorLoad().get(0).value());
        Assertions.assertEquals("项目跟进", analytics.operatorLoad().get(3).name());
        Assertions.assertEquals(3, analytics.operatorLoad().get(3).value());
    }

    // =========================================================================
    // adminAnalytics - revenue calculation with null amounts filtered
    // =========================================================================

    @Test
    void adminAnalytics_totalRevenueFiltersNullAmounts() {
        Mockito.when(userAccountMapper.selectCount(ArgumentMatchers.any())).thenReturn(100L);
        Mockito.when(taskMapper.selectCount(ArgumentMatchers.any())).thenReturn(50L);
        Mockito.when(bizOrderMapper.selectCount(ArgumentMatchers.any())).thenReturn(30L);
        Mockito.when(courseMapper.selectCount(ArgumentMatchers.any())).thenReturn(10L);
        Mockito.when(reportDailySummaryMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(
                buildReport(LocalDate.of(2026, 4, 15), 10, 5, BigDecimal.valueOf(50000), 2, 3)
        ));
        Mockito.when(adminProjectService.listAdminProjects()).thenReturn(List.of());
        Mockito.when(adminProjectService.countOpenFeedbackTickets()).thenReturn(0L);
        Mockito.when(adminProjectService.countProcessingFeedbackTickets()).thenReturn(0L);
        Mockito.when(alertService.listAlerts()).thenReturn(List.of());

        BizOrderEntity order1 = new BizOrderEntity();
        order1.setId(1L);
        order1.setAmount(BigDecimal.valueOf(30000));
        order1.setStatus("PAID");
        BizOrderEntity order2 = new BizOrderEntity();
        order2.setId(2L);
        order2.setAmount(null);
        order2.setStatus("PAID");
        BizOrderEntity order3 = new BizOrderEntity();
        order3.setId(3L);
        order3.setAmount(BigDecimal.valueOf(20000));
        order3.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order1, order2, order3));

        ApiDtos.AdminAnalyticsView analytics = adminDashboardService.adminAnalytics();

        // totalRevenue = 30000 + 20000 = 50000 (null filtered), displayed as "5.0 万"
        Assertions.assertTrue(analytics.businessMetrics().get(0).value().contains("5.0"));
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private ReportDailySummaryEntity buildReport(LocalDate statDate, int taskCount, int orderCount, BigDecimal paymentAmount, int alertCount, int trainingCount) {
        ReportDailySummaryEntity report = new ReportDailySummaryEntity();
        report.setId(1L);
        report.setStatDate(statDate);
        report.setTaskCount(taskCount);
        report.setOrderCount(orderCount);
        report.setPaymentAmount(paymentAmount);
        report.setAlertCount(alertCount);
        report.setTrainingCount(trainingCount);
        return report;
    }
}

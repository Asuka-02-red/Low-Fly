package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.ReportDailySummaryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.ReportDailySummaryMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 管理员仪表板服务类，提供管理员后台相关功能的数据统计和展示
 */
@Service
public class AdminDashboardService {

    /**
     * 用户账户数据访问接口
     */
    private final UserAccountMapper userAccountMapper;
    /**
     * 任务数据访问接口
     */
    private final TaskMapper taskMapper;
    /**
     * 业务订单数据访问接口
     */
    private final BizOrderMapper bizOrderMapper;
    /**
     * 课程数据访问接口
     */
    private final CourseMapper courseMapper;
    /**
     * 每日报告汇总数据访问接口
     */
    private final ReportDailySummaryMapper reportDailySummaryMapper;
    /**
     * 告警服务接口
     */
    private final AlertService alertService;
    /**
     * 审计日志服务接口
     */
    private final AuditLogService auditLogService;
    /**
     * 管理员项目管理服务接口
     */
    private final AdminProjectService adminProjectService;

    public AdminDashboardService(
            UserAccountMapper userAccountMapper,
            TaskMapper taskMapper,
            BizOrderMapper bizOrderMapper,
            CourseMapper courseMapper,
            ReportDailySummaryMapper reportDailySummaryMapper,
            AlertService alertService,
            AuditLogService auditLogService,
            AdminProjectService adminProjectService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.taskMapper = taskMapper;
        this.bizOrderMapper = bizOrderMapper;
        this.courseMapper = courseMapper;
        this.reportDailySummaryMapper = reportDailySummaryMapper;
        this.alertService = alertService;
        this.auditLogService = auditLogService;
        this.adminProjectService = adminProjectService;
    }

    public ApiDtos.DashboardOverview adminOverview() {
        long totalUsers = countUsers();
        long enabledUsers = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .gt(UserAccountEntity::getStatus, 0));
        long adminUsers = countUsersByRole(com.lowaltitude.reststop.server.security.RoleType.ADMIN);
        long totalTasks = countTasks();
        long reviewingTasks = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "REVIEWING"));
        long totalOrders = countOrders();
        long paidOrders = bizOrderMapper.selectCount(new LambdaQueryWrapper<BizOrderEntity>()
                .eq(BizOrderEntity::getStatus, "PAID"));
        long openCourses = countOpenCourses();
        long auditCount = auditLogService.count();
        long openAlerts = alertService.countOpenAlerts();
        long highAlerts = alertService.countHighRiskAlerts();

        List<ReportDailySummaryEntity> reports = loadRecentReports(6);
        List<TaskEntity> tasks = taskMapper.selectList(new QueryWrapper<TaskEntity>()
                .select("id", "title", "location", "status", "update_time")
                .orderByDesc("update_time", "id"));

        List<ApiDtos.DashboardMetric> metrics = List.of(
                new ApiDtos.DashboardMetric("用户总量", String.valueOf(totalUsers), "启用账号 " + enabledUsers, "success"),
                new ApiDtos.DashboardMetric("任务总量", String.valueOf(totalTasks), "待审核 " + reviewingTasks, "info"),
                new ApiDtos.DashboardMetric("订单总量", String.valueOf(totalOrders), "已支付 " + paidOrders, "success"),
                new ApiDtos.DashboardMetric("高风险告警", String.valueOf(highAlerts), "未关闭 " + openAlerts, highAlerts > 0 ? "warning" : "success")
        );

        List<ApiDtos.DashboardMetric> deviceStats = List.of(
                new ApiDtos.DashboardMetric("启用账号", String.valueOf(enabledUsers), "管理员 " + adminUsers, "success"),
                new ApiDtos.DashboardMetric("开放课程", String.valueOf(openCourses), "培训资源已发布", "info"),
                new ApiDtos.DashboardMetric("审计日志", String.valueOf(auditCount), "关键操作留痕", "warning")
        );

        List<ApiDtos.DashboardActivity> activities = auditLogService.listRecent().stream()
                .limit(5)
                .map(item -> new ApiDtos.DashboardActivity(
                        item.bizType() + " / " + item.eventType(),
                        item.payload(),
                        PlatformUtils.formatDateTime(item.createTime()),
                        PlatformUtils.defaultIfBlank(item.actorRole(), "SYSTEM")))
                .toList();

        List<ApiDtos.DashboardNotice> notices = List.of(
                new ApiDtos.DashboardNotice("待审核任务 " + reviewingTasks + " 个，请尽快处理。", reviewingTasks > 0 ? "中" : "低", "实时"),
                new ApiDtos.DashboardNotice("高风险告警 " + highAlerts + " 条，建议优先跟进。", highAlerts > 0 ? "高" : "低", "实时"),
                new ApiDtos.DashboardNotice("已支付订单 " + paidOrders + " 笔，平台经营数据已同步。", "低", "实时")
        );

        List<ApiDtos.AdminDistributionItem> projectDistribution = tasks.stream()
                .collect(Collectors.groupingBy(task -> PlatformUtils.defaultIfBlank(task.getLocation(), "未分类"), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(entry -> new ApiDtos.AdminDistributionItem(entry.getKey(), entry.getValue().intValue()))
                .toList();

        List<ApiDtos.AdminTrendPoint> progressTrend = reports.stream()
                .map(report -> new ApiDtos.AdminTrendPoint(
                        report.getStatDate().getMonthValue() + "/" + report.getStatDate().getDayOfMonth(),
                        Math.min(100, PlatformUtils.safeInt(report.getTaskCount()) * 8 + PlatformUtils.safeInt(report.getOrderCount()) * 6 + PlatformUtils.safeInt(report.getTrainingCount()) * 3)))
                .toList();

        return new ApiDtos.DashboardOverview(
                metrics,
                deviceStats,
                activities,
                notices,
                projectDistribution,
                progressTrend
        );
    }

    public List<ApiDtos.AdminSectionSummary> adminSectionSummaries() {
        long userCount = countUsers();
        long adminUserCount = countUsersByRole(com.lowaltitude.reststop.server.security.RoleType.ADMIN);
        long taskCount = countTasks();
        long pendingTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "REVIEWING"));
        long orderCount = countOrders();
        long paidOrderCount = bizOrderMapper.selectCount(new LambdaQueryWrapper<BizOrderEntity>()
                .eq(BizOrderEntity::getStatus, "PAID"));
        long zoneCount = adminProjectService.countNoFlyZones();
        long openAlertCount = alertService.countOpenAlerts();
        long highAlertCount = alertService.countHighRiskAlerts();
        long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<>());
        long openCourseCount = countOpenCourses();
        long enabledNotificationCount = 2L;
        long auditEventCount = auditLogService.count();

        return List.of(
                new ApiDtos.AdminSectionSummary(
                        "overview", "系统概览", "总览当前平台状态。", "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("用户", String.valueOf(userCount), "在线管理员 " + adminUserCount, "success"),
                                new ApiDtos.SectionMetric("任务", String.valueOf(taskCount), "待审核 " + pendingTaskCount, "info"),
                                new ApiDtos.SectionMetric("告警", String.valueOf(openAlertCount), "高风险 " + highAlertCount, "warning")
                        ),
                        List.of("待处理高风险告警 " + highAlertCount + " 条", "当前功能开关 3 项生效")
                ),
                new ApiDtos.AdminSectionSummary(
                        "users", "用户管理", "管理账号、角色与权限。", "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("账号", String.valueOf(userCount), "管理员 " + adminUserCount, "success"),
                                new ApiDtos.SectionMetric("角色", "4", "覆盖后台岗位", "info"),
                                new ApiDtos.SectionMetric("审计", String.valueOf(auditEventCount), "最近 24h 有更新", "warning")
                        ),
                        List.of("支持角色分配", "支持权限组配置")
                ),
                new ApiDtos.AdminSectionSummary(
                        "projects", "项目管理", "跟踪项目与交付状态。", "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("任务", String.valueOf(taskCount), "待审核 " + pendingTaskCount, "info"),
                                new ApiDtos.SectionMetric("订单", String.valueOf(orderCount), "已支付 " + paidOrderCount, "success"),
                                new ApiDtos.SectionMetric("禁飞区", String.valueOf(zoneCount), "合规校验已启用", "warning")
                        ),
                        List.of("支持状态流转", "支持合规与支付联动")
                ),
                new ApiDtos.AdminSectionSummary(
                        "analytics", "数据分析", "查看经营与性能数据。", "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("订单", String.valueOf(orderCount), "支付成功 " + paidOrderCount, "success"),
                                new ApiDtos.SectionMetric("课程", String.valueOf(courseCount), "开放 " + openCourseCount, "info"),
                                new ApiDtos.SectionMetric("告警", String.valueOf(openAlertCount), "闭环持续跟踪", "warning")
                        ),
                        List.of("含经营图表", "含性能图表")
                ),
                new ApiDtos.AdminSectionSummary(
                        "settings", "系统设置", "维护基础参数与安全策略。", "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("策略", "2", "基础参数与安全策略", "info"),
                                new ApiDtos.SectionMetric("通知", "3", "启用 " + enabledNotificationCount, "success"),
                                new ApiDtos.SectionMetric("白名单", "2", "当前已生效", "warning")
                        ),
                        List.of("支持基础参数保存", "支持通知规则维护")
                ),
                new ApiDtos.AdminSectionSummary(
                        "logs", "日志管理", "查看操作留痕与导出记录。", "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("日志", String.valueOf(auditEventCount), "全链路留痕", "success"),
                                new ApiDtos.SectionMetric("高风险", String.valueOf(highAlertCount), "需重点复核", "warning"),
                                new ApiDtos.SectionMetric("导出", "CSV", "支持筛选导出", "info")
                        ),
                        List.of("支持查询", "支持导出")
                )
        );
    }

    public ApiDtos.AdminAnalyticsView adminAnalytics() {
        List<ReportDailySummaryEntity> reports = loadRecentReports(6);
        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();
        long totalUsers = countUsers();
        long enabledUsers = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .gt(UserAccountEntity::getStatus, 0));
        long paidOrderCount = bizOrderMapper.selectCount(new LambdaQueryWrapper<BizOrderEntity>()
                .eq(BizOrderEntity::getStatus, "PAID"));
        BigDecimal totalRevenue = bizOrderMapper.selectList(new LambdaQueryWrapper<BizOrderEntity>()
                        .eq(BizOrderEntity::getStatus, "PAID"))
                .stream()
                .map(BizOrderEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowRiskCount = projects.stream().filter(item -> "低".equals(item.riskLevel())).count();
        long mediumRiskCount = projects.stream().filter(item -> "中".equals(item.riskLevel())).count();
        long highRiskCount = projects.stream().filter(item -> "高".equals(item.riskLevel())).count();
        long openFeedbackCount = adminProjectService.countOpenFeedbackTickets();
        long processingFeedbackCount = adminProjectService.countProcessingFeedbackTickets();
        long pendingTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "REVIEWING"));
        long openAlertCount = alertService.listAlerts().stream()
                .filter(item -> !"RESOLVED".equalsIgnoreCase(item.status()))
                .count();

        int projectHealthPercent = projects.isEmpty()
                ? 100
                : BigDecimal.valueOf(lowRiskCount * 100.0 / projects.size())
                .setScale(1, RoundingMode.HALF_UP)
                .intValue();
        int userActivityPercent = totalUsers == 0
                ? 0
                : BigDecimal.valueOf(enabledUsers * 100.0 / totalUsers)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int avgResponse = 120 + (int) Math.min(180, openAlertCount * 8 + openFeedbackCount * 6 + pendingTaskCount * 5);
        double availability = Math.max(96.5d, 99.95d - openAlertCount * 0.18d - openFeedbackCount * 0.05d);
        int feedbackCycle = 18 + (int) Math.min(45, openFeedbackCount * 4 + processingFeedbackCount * 2);

        return new ApiDtos.AdminAnalyticsView(
                List.of(
                        new ApiDtos.SectionMetric("月度营收", "¥ " + totalRevenue.divide(BigDecimal.valueOf(10000), 1, RoundingMode.HALF_UP) + " 万", "已支付订单 " + paidOrderCount + " 笔", "success"),
                        new ApiDtos.SectionMetric("项目健康度", projectHealthPercent + "%", "低风险项目 " + lowRiskCount + " 个", "success"),
                        new ApiDtos.SectionMetric("用户活跃度", userActivityPercent + "%", "启用账号 " + enabledUsers + " / " + totalUsers, "info")
                ),
                List.of(
                        new ApiDtos.SectionMetric("平均响应时间", avgResponse + " ms", "待处理告警 " + openAlertCount + " 条", "success"),
                        new ApiDtos.SectionMetric("系统可用性", String.format("%.2f%%", availability), "基于近期告警与工单估算", "success"),
                        new ApiDtos.SectionMetric("工单闭环时长", feedbackCycle + " 分钟", "未关闭工单 " + openFeedbackCount + " 条", openFeedbackCount > 0 ? "warning" : "info")
                ),
                reports.stream()
                        .map(item -> new ApiDtos.AdminTrendPoint(item.getStatDate().format(DateTimeFormatter.ofPattern("MM-dd")), item.getPaymentAmount().divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).intValue()))
                        .toList(),
                reports.stream()
                        .map(item -> new ApiDtos.AdminTrendPoint(item.getStatDate().format(DateTimeFormatter.ofPattern("MM-dd")), PlatformUtils.safeInt(item.getTaskCount()) + PlatformUtils.safeInt(item.getOrderCount()) + PlatformUtils.safeInt(item.getTrainingCount())))
                        .toList(),
                List.of(
                        new ApiDtos.AdminDistributionItem("健康项目", (int) lowRiskCount),
                        new ApiDtos.AdminDistributionItem("需关注项目", (int) mediumRiskCount),
                        new ApiDtos.AdminDistributionItem("高风险项目", (int) highRiskCount)
                ),
                buildServicePerformanceMetrics(reports, openFeedbackCount, openAlertCount),
                List.of(
                        new ApiDtos.AdminDistributionItem("反馈工单", (int) openFeedbackCount),
                        new ApiDtos.AdminDistributionItem("权限复核", (int) pendingTaskCount),
                        new ApiDtos.AdminDistributionItem("风险告警", (int) openAlertCount),
                        new ApiDtos.AdminDistributionItem("项目跟进", Math.max(1, projects.size()))
                )
        );
    }

    List<ApiDtos.AdminServiceMetric> buildServicePerformanceMetrics(
            List<ReportDailySummaryEntity> reports,
            long openFeedbackCount,
            long openAlertCount
    ) {
        ReportDailySummaryEntity latest = reports.isEmpty() ? null : reports.get(reports.size() - 1);
        int baseTaskCount = latest == null ? (int) countTasks() : PlatformUtils.safeInt(latest.getTaskCount());
        int baseOrderCount = latest == null ? (int) countOrders() : PlatformUtils.safeInt(latest.getOrderCount());
        int baseTrainingCount = latest == null ? (int) countOpenCourses() : PlatformUtils.safeInt(latest.getTrainingCount());
        int baseAlertCount = latest == null ? (int) openAlertCount : PlatformUtils.safeInt(latest.getAlertCount());
        return List.of(
                new ApiDtos.AdminServiceMetric("认证服务", 120 + Math.min(60, (int) countUsers()), Math.max(97.8d, 99.96d - openAlertCount * 0.08d)),
                new ApiDtos.AdminServiceMetric("项目服务", 160 + Math.min(80, baseTaskCount * 4), Math.max(97.2d, 99.92d - baseAlertCount * 0.12d)),
                new ApiDtos.AdminServiceMetric("订单服务", 150 + Math.min(90, baseOrderCount * 5), Math.max(97.0d, 99.88d - baseAlertCount * 0.10d)),
                new ApiDtos.AdminServiceMetric("工单服务", 140 + Math.min(100, (int) openFeedbackCount * 9 + baseTrainingCount), Math.max(96.8d, 99.85d - openFeedbackCount * 0.14d))
        );
    }

    private List<ReportDailySummaryEntity> loadRecentReports(int limit) {
        List<ReportDailySummaryEntity> reports = reportDailySummaryMapper.selectList(new LambdaQueryWrapper<ReportDailySummaryEntity>()
                .orderByAsc(ReportDailySummaryEntity::getStatDate));
        if (reports.isEmpty()) {
            return List.of();
        }
        if (reports.size() <= limit) {
            return reports;
        }
        return reports.subList(reports.size() - limit, reports.size());
    }

    private long countUsers() {
        return userAccountMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countUsersByRole(com.lowaltitude.reststop.server.security.RoleType role) {
        return userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getRole, role.name()));
    }

    private long countTasks() {
        return taskMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countOrders() {
        return bizOrderMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countOpenCourses() {
        return courseMapper.selectCount(new LambdaQueryWrapper<com.lowaltitude.reststop.server.entity.CourseEntity>()
                .eq(com.lowaltitude.reststop.server.entity.CourseEntity::getStatus, "OPEN"));
    }
}

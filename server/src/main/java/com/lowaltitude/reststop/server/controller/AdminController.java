package com.lowaltitude.reststop.server.controller;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.ApiResponse;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.service.AdminDashboardService;
import com.lowaltitude.reststop.server.service.AdminProjectService;
import com.lowaltitude.reststop.server.service.AdminSettingsService;
import com.lowaltitude.reststop.server.service.AdminUserService;
import com.lowaltitude.reststop.server.service.AmapWeatherService;
import com.lowaltitude.reststop.server.service.FeedbackService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;
    private final AdminProjectService adminProjectService;
    private final AdminSettingsService adminSettingsService;
    private final FeedbackService feedbackService;
    private final AmapWeatherService amapWeatherService;

    public AdminController(
            AdminDashboardService adminDashboardService,
            AdminUserService adminUserService,
            AdminProjectService adminProjectService,
            AdminSettingsService adminSettingsService,
            FeedbackService feedbackService,
            AmapWeatherService amapWeatherService
    ) {
        this.adminDashboardService = adminDashboardService;
        this.adminUserService = adminUserService;
        this.adminProjectService = adminProjectService;
        this.adminSettingsService = adminSettingsService;
        this.feedbackService = feedbackService;
        this.amapWeatherService = amapWeatherService;
    }

    @GetMapping("/overview")
    public ApiResponse<ApiDtos.DashboardOverview> overview() {
        return ApiResponse.success("查询成功", adminDashboardService.adminOverview());
    }

    @GetMapping("/weather/realtime")
    public ApiResponse<ApiDtos.AdminRealtimeWeatherView> realtimeWeather(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude
    ) {
        return ApiResponse.success("查询成功", amapWeatherService.getRealtimeWeather(longitude, latitude));
    }

    @GetMapping("/sections-summary")
    public ApiResponse<List<ApiDtos.AdminSectionSummary>> sectionsSummary() {
        return ApiResponse.success("查询成功", adminDashboardService.adminSectionSummaries());
    }

    @GetMapping("/audit-events")
    public ApiResponse<List<ApiDtos.AuditEventView>> auditEvents() {
        return ApiResponse.success("查询成功", adminUserService.listAuditEvents());
    }

    @GetMapping("/users")
    public ApiResponse<List<ApiDtos.AdminUserView>> users() {
        return ApiResponse.success("查询成功", adminUserService.listAdminUsers());
    }

    @PostMapping("/users")
    public ApiResponse<ApiDtos.AdminUserView> createUser(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminUserCreateRequest request
    ) {
        return ApiResponse.success("用户已创建", adminUserService.createAdminUser(admin, request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<ApiDtos.AdminUserView> updateUser(
            @AuthenticationPrincipal SessionUser admin,
            @PathVariable Long userId,
            @Valid @RequestBody ApiDtos.AdminUserUpdateRequest request
    ) {
        return ApiResponse.success("用户已更新", adminUserService.updateAdminUser(admin, userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(
            @AuthenticationPrincipal SessionUser admin,
            @PathVariable Long userId
    ) {
        adminUserService.deleteAdminUser(admin, userId);
        return ApiResponse.success("用户已删除", null);
    }

    @GetMapping("/projects")
    public ApiResponse<List<ApiDtos.AdminProjectView>> projects() {
        return ApiResponse.success("查询成功", adminProjectService.listAdminProjects());
    }

    @GetMapping("/orders")
    public ApiResponse<List<ApiDtos.AdminOrderSummaryView>> orders() {
        return ApiResponse.success("查询成功", adminProjectService.listAdminOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<ApiDtos.AdminOrderDetailView> orderDetail(@PathVariable Long orderId) {
        return ApiResponse.success("查询成功", adminProjectService.getAdminOrderDetail(orderId));
    }

    @GetMapping("/analytics")
    public ApiResponse<ApiDtos.AdminAnalyticsView> analytics() {
        return ApiResponse.success("查询成功", adminDashboardService.adminAnalytics());
    }

    @GetMapping("/settings")
    public ApiResponse<ApiDtos.AdminSettingsView> settings() {
        return ApiResponse.success("查询成功", adminSettingsService.getAdminSettings());
    }

    @PutMapping("/settings/basic")
    public ApiResponse<ApiDtos.AdminSettingsView> saveBasicSettings(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminBasicSettings request
    ) {
        return ApiResponse.success("基础参数已保存", adminSettingsService.saveAdminBasicSettings(admin, request));
    }

    @PutMapping("/settings/security")
    public ApiResponse<ApiDtos.AdminSettingsView> saveSecuritySettings(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminSecuritySettings request
    ) {
        return ApiResponse.success("安全策略已保存", adminSettingsService.saveAdminSecuritySettings(admin, request));
    }

    @PutMapping("/settings/notifications")
    public ApiResponse<ApiDtos.AdminSettingsView> saveNotificationRules(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminNotificationRulesRequest request
    ) {
        return ApiResponse.success("通知规则已保存", adminSettingsService.saveAdminNotificationRules(admin, request));
    }

    @GetMapping("/feedback-tickets")
    public ApiResponse<List<ApiDtos.FeedbackTicketView>> feedbackTickets() {
        return ApiResponse.success("查询成功", feedbackService.listAdminFeedbackTickets());
    }

    @PutMapping("/feedback-tickets/{ticketId}")
    public ApiResponse<ApiDtos.FeedbackTicketView> replyFeedbackTicket(
            @AuthenticationPrincipal SessionUser admin,
            @PathVariable Long ticketId,
            @Valid @RequestBody ApiDtos.FeedbackTicketReplyRequest request
    ) {
        return ApiResponse.success("工单已更新", feedbackService.replyFeedbackTicket(admin, ticketId, request));
    }
}

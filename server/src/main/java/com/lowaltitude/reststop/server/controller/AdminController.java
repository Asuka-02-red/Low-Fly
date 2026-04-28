package com.lowaltitude.reststop.server.controller;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.ApiResponse;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.service.AmapWeatherService;
import com.lowaltitude.reststop.server.service.DemoPlatformService;
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

    private final DemoPlatformService platformService;
    private final AmapWeatherService amapWeatherService;

    public AdminController(DemoPlatformService platformService, AmapWeatherService amapWeatherService) {
        this.platformService = platformService;
        this.amapWeatherService = amapWeatherService;
    }

    @GetMapping("/overview")
    public ApiResponse<ApiDtos.DashboardOverview> overview() {
        return ApiResponse.success("查询成功", platformService.adminOverview());
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
        return ApiResponse.success("查询成功", platformService.adminSectionSummaries());
    }

    @GetMapping("/audit-events")
    public ApiResponse<List<ApiDtos.AuditEventView>> auditEvents() {
        return ApiResponse.success("查询成功", platformService.listAuditEvents());
    }

    @GetMapping("/users")
    public ApiResponse<List<ApiDtos.AdminUserView>> users() {
        return ApiResponse.success("查询成功", platformService.listAdminUsers());
    }

    @PostMapping("/users")
    public ApiResponse<ApiDtos.AdminUserView> createUser(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminUserCreateRequest request
    ) {
        return ApiResponse.success("用户已创建", platformService.createAdminUser(admin, request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<ApiDtos.AdminUserView> updateUser(
            @AuthenticationPrincipal SessionUser admin,
            @PathVariable Long userId,
            @Valid @RequestBody ApiDtos.AdminUserUpdateRequest request
    ) {
        return ApiResponse.success("用户已更新", platformService.updateAdminUser(admin, userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(
            @AuthenticationPrincipal SessionUser admin,
            @PathVariable Long userId
    ) {
        platformService.deleteAdminUser(admin, userId);
        return ApiResponse.success("用户已删除", null);
    }

    @GetMapping("/projects")
    public ApiResponse<List<ApiDtos.AdminProjectView>> projects() {
        return ApiResponse.success("查询成功", platformService.listAdminProjects());
    }

    @GetMapping("/orders")
    public ApiResponse<List<ApiDtos.AdminOrderSummaryView>> orders() {
        return ApiResponse.success("查询成功", platformService.listAdminOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<ApiDtos.AdminOrderDetailView> orderDetail(@PathVariable Long orderId) {
        return ApiResponse.success("查询成功", platformService.getAdminOrderDetail(orderId));
    }

    @GetMapping("/analytics")
    public ApiResponse<ApiDtos.AdminAnalyticsView> analytics() {
        return ApiResponse.success("查询成功", platformService.adminAnalytics());
    }

    @GetMapping("/settings")
    public ApiResponse<ApiDtos.AdminSettingsView> settings() {
        return ApiResponse.success("查询成功", platformService.getAdminSettings());
    }

    @PutMapping("/settings/basic")
    public ApiResponse<ApiDtos.AdminSettingsView> saveBasicSettings(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminBasicSettings request
    ) {
        return ApiResponse.success("基础参数已保存", platformService.saveAdminBasicSettings(admin, request));
    }

    @PutMapping("/settings/security")
    public ApiResponse<ApiDtos.AdminSettingsView> saveSecuritySettings(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminSecuritySettings request
    ) {
        return ApiResponse.success("安全策略已保存", platformService.saveAdminSecuritySettings(admin, request));
    }

    @PutMapping("/settings/notifications")
    public ApiResponse<ApiDtos.AdminSettingsView> saveNotificationRules(
            @AuthenticationPrincipal SessionUser admin,
            @Valid @RequestBody ApiDtos.AdminNotificationRulesRequest request
    ) {
        return ApiResponse.success("通知规则已保存", platformService.saveAdminNotificationRules(admin, request));
    }

    @GetMapping("/feedback-tickets")
    public ApiResponse<List<ApiDtos.FeedbackTicketView>> feedbackTickets() {
        return ApiResponse.success("查询成功", platformService.listAdminFeedbackTickets());
    }

    @PutMapping("/feedback-tickets/{ticketId}")
    public ApiResponse<ApiDtos.FeedbackTicketView> replyFeedbackTicket(
            @AuthenticationPrincipal SessionUser admin,
            @PathVariable Long ticketId,
            @Valid @RequestBody ApiDtos.FeedbackTicketReplyRequest request
    ) {
        return ApiResponse.success("工单已更新", platformService.replyFeedbackTicket(admin, ticketId, request));
    }
}

package com.lowaltitude.reststop.server.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank @Size(max = 20) String role,
            String realName,
            String companyName
    ) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record SessionInfo(Long id, String username, String role, String realName, String companyName) {
    }

    public record AuthPayload(String token, String refreshToken, SessionInfo userInfo) {
    }

    public record TaskRequest(
            @NotBlank String taskType,
            @NotBlank @Size(max = 100) String title,
            @NotBlank String description,
            @NotBlank @Size(max = 255) String location,
            @NotBlank @Size(max = 16) String deadline,
            @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
            @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
            @NotNull @DecimalMin("0.01") BigDecimal budget
    ) {
    }

    public record TaskView(Long id, String title, String taskType, String location, String deadline, BigDecimal budget, String status, String ownerName) {
    }

    public record TaskDetailView(
            Long id,
            String title,
            String taskType,
            String description,
            String location,
            String deadline,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal routeStartLatitude,
            BigDecimal routeStartLongitude,
            Integer operationRadiusMeters,
            BigDecimal budget,
            String status,
            String ownerName
    ) {
    }

    public record OrderCreateRequest(@NotNull Long taskId) {
    }

    public record OrderView(Long id, String orderNo, Long taskId, Long pilotId, Long enterpriseId, BigDecimal amount, String status) {
    }

    public record OrderDetailView(
            Long id,
            String orderNo,
            String status,
            BigDecimal amount,
            Long taskId,
            String taskTitle,
            String taskType,
            String location,
            String pilotName,
            String enterpriseName,
            String contactName,
            String contactPhone,
            String paymentChannel,
            String paymentStatus,
            String createdAt,
            String appointmentTime,
            String remark
    ) {
    }

    public record PaymentRequest(@NotNull Long orderId, @NotBlank @Size(max = 20) String channel) {
    }

    public record PaymentResult(String tradeNo, String status, BigDecimal amount) {
    }

    public record NoFlyZoneView(
            Long id,
            String name,
            String zoneType,
            BigDecimal centerLat,
            BigDecimal centerLng,
            int radius,
            String description
    ) {
    }

    public record FlightApplicationRequest(
            @NotBlank @Size(max = 255) String location,
            @NotBlank @Size(max = 50) String flightTime,
            @NotBlank @Size(max = 255) String purpose
    ) {
    }

    public record FlightApplicationView(String applicationNo, String status, String location, String approvalHint) {
    }

    public record FeedbackTicketRequest(
            @Size(max = 100) String contact,
            @NotBlank @Size(min = 10, max = 2000) String detail
    ) {
    }

    public record FeedbackTicketReplyRequest(
            @NotBlank @Size(max = 20) String status,
            @Size(max = 1000) String reply
    ) {
    }

    public record FeedbackTicketView(
            Long id,
            String ticketNo,
            String submitterName,
            String submitterRole,
            String contact,
            String detail,
            String status,
            String reply,
            String createTime,
            String updateTime,
            String closedTime
    ) {
    }

    public record MessageSendRequest(
            @NotBlank @Size(min = 1, max = 1000) String content
    ) {
    }

    public record MessageReadReceiptRequest(
            @NotNull @Size(min = 1, max = 200) List<Long> msgIds
    ) {
    }

    public record MessageReadReceiptResponse(
            int successCount,
            List<Long> syncedMsgIds
    ) {
    }

    public record MessageConversationView(
            Long id,
            String title,
            String subtitle,
            String counterpartName,
            String counterpartUid,
            String counterpartRole,
            String pilotUid,
            String enterpriseUid,
            String lastMessagePreview,
            String lastMessageTime,
            int unreadCount
    ) {
    }

    public record MessageEntryView(
            Long id,
            Long senderId,
            String pilotUid,
            String enterpriseUid,
            String senderName,
            String senderRole,
            String content,
            String createTime,
            boolean isRead,
            boolean mine
    ) {
    }

    public record MessageThreadView(
            Long conversationId,
            String title,
            String subtitle,
            String pilotUid,
            String enterpriseUid,
            List<MessageEntryView> messages
    ) {
    }

    public record PilotProfileView(String uid, String name) {
    }

    public record EnterpriseInfoView(String uid, String companyName) {
    }

    public record AlertView(Long id, String level, String content, String status, LocalDateTime createTime) {
    }

    public record CourseView(
            Long id,
            String title,
            String summary,
            String learningMode,
            String institutionName,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            BigDecimal price,
            String status
    ) {
    }

    public record CourseDetailView(
            Long id,
            String title,
            String summary,
            String content,
            String learningMode,
            String institutionName,
            int seatTotal,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            BigDecimal price,
            String status,
            boolean enrolled,
            String enrollmentNo,
            String enrollmentStatus
    ) {
    }

    public record CourseManageRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 255) String summary,
            @NotBlank String content,
            @NotBlank @Size(max = 20) String learningMode,
            @NotNull @Min(1) Integer seatTotal,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @NotBlank @Size(max = 20) String status
    ) {
    }

    public record CourseManageView(
            Long id,
            String title,
            String summary,
            String learningMode,
            int seatTotal,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            BigDecimal price,
            String status
    ) {
    }

    public record EnrollmentResult(String enrollmentNo, String status, String courseTitle, int seatAvailable) {
    }

    public record DashboardMetric(String label, String value, String trend, String status) {
    }

    public record DashboardActivity(String title, String content, String time, String tag) {
    }

    public record DashboardNotice(String title, String level, String time) {
    }

    public record DashboardOverview(
            List<DashboardMetric> metrics,
            List<DashboardMetric> deviceStats,
            List<DashboardActivity> activities,
            List<DashboardNotice> notices,
            List<AdminDistributionItem> projectDistribution,
            List<AdminTrendPoint> progressTrend
    ) {
    }

    public record AdminFlightConditionCheck(String label, String currentValue, String threshold, boolean passed) {
    }

    public record AdminFlightSuitabilityView(
            String result,
            String level,
            String summary,
            List<AdminFlightConditionCheck> checks,
            List<String> conditionNotes,
            List<String> recommendations
    ) {
    }

    public record AdminRealtimeWeatherView(
            String serviceName,
            String locationName,
            String adcode,
            String weather,
            String reportTime,
            String fetchedAt,
            String refreshInterval,
            double temperature,
            int humidity,
            String windDirection,
            String windPower,
            double windSpeed,
            double visibility,
            int precipitationProbability,
            double precipitationIntensity,
            String thunderstormRisk,
            String sourceNote,
            AdminFlightSuitabilityView suitability
    ) {
    }

    public record SectionMetric(String label, String value, String trend, String status) {
    }

    public record AdminSectionSummary(
            String sectionKey,
            String title,
            String headline,
            String buttonLabel,
            List<SectionMetric> metrics,
            List<String> highlights
    ) {
    }

    public record AuditEventView(
            String requestId,
            Long actorUserId,
            String actorRole,
            String bizType,
            String bizId,
            String eventType,
            String payload,
            LocalDateTime createTime
    ) {
    }

    public record AdminUserView(
            String id,
            String username,
            String email,
            String name,
            String organization,
            String phone,
            String status,
            String createTime,
            List<String> roleNames,
            String permissionGroupName,
            String roleCode
    ) {
    }

    public record AdminUserCreateRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Size(max = 100) String email,
            @NotBlank @Size(max = 20) String role,
            @NotBlank @Size(max = 20) String realName,
            @Size(max = 100) String companyName,
            @NotNull @Min(0) Integer status
    ) {
    }

    public record AdminUserUpdateRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @Size(min = 8, max = 64) String password,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Size(max = 100) String email,
            @NotBlank @Size(max = 20) String role,
            @NotBlank @Size(max = 20) String realName,
            @Size(max = 100) String companyName,
            @NotNull @Min(0) Integer status
    ) {
    }

    public record AdminProjectView(
            String id,
            String name,
            String owner,
            String region,
            String status,
            int progress,
            BigDecimal budget,
            String complianceStatus,
            String riskLevel,
            int trainingCompletion,
            String paymentStatus,
            String updatedAt
    ) {
    }

    public record AdminTrendPoint(String label, int value) {
    }

    public record AdminDistributionItem(String name, int value) {
    }

    public record AdminServiceMetric(String label, int response, double availability) {
    }

    public record AdminAnalyticsView(
            List<SectionMetric> businessMetrics,
            List<SectionMetric> performanceMetrics,
            List<AdminTrendPoint> revenueTrend,
            List<AdminTrendPoint> userActivity,
            List<AdminDistributionItem> projectHealth,
            List<AdminServiceMetric> servicePerformance,
            List<AdminDistributionItem> operatorLoad
    ) {
    }

    public record AdminBasicSettings(
            @NotBlank @Size(max = 100) String stationName,
            @NotBlank @Size(max = 30) String serviceHotline,
            @NotBlank @Size(max = 50) String defaultRegion,
            boolean mobileDashboardEnabled
    ) {
    }

    public record AdminSecuritySettings(
            @NotNull @Min(1) Integer passwordValidityDays,
            @NotNull @Min(1) Integer loginRetryLimit,
            @Size(max = 500) String ipWhitelist,
            boolean mfaRequired
    ) {
    }

    public record AdminNotificationRule(
            String id,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 20) String channel,
            boolean enabled,
            @NotBlank @Size(max = 255) String trigger
    ) {
    }

    public record AdminNotificationRulesRequest(@NotNull List<AdminNotificationRule> rules) {
    }

    public record AdminSettingsView(
            AdminBasicSettings basic,
            AdminSecuritySettings security,
            List<AdminNotificationRule> notifications
    ) {
    }

    public record AdminOrderSummaryView(
            String id,
            String orderNo,
            String projectName,
            BigDecimal amount,
            String status,
            String createTime,
            String paymentMethod,
            String details
    ) {
    }

    public record AdminOrderDetailView(
            String id,
            String orderNo,
            String projectName,
            BigDecimal amount,
            String status,
            String createTime,
            String paymentMethod,
            String details
    ) {
    }
}

package com.example.low_altitudereststop.core.model;

import java.math.BigDecimal;

/**
 * 平台业务数据模型集合，包含任务、订单、合规、消息、培训、天气等所有业务模块的请求和响应数据结构。
 */
public final class PlatformModels {

    private PlatformModels() {
    }

    public static class TaskRequest {
        public String taskType;
        public String title;
        public String description;
        public String location;
        public String deadline;
        public BigDecimal latitude;
        public BigDecimal longitude;
        public BigDecimal budget;
    }

    public static class TaskView {
        public Long id;
        public String title;
        public String taskType;
        public String location;
        public String deadline;
        public BigDecimal budget;
        public String status;
        public String ownerName;
    }

    public static class TaskDetailView {
        public Long id;
        public String title;
        public String taskType;
        public String description;
        public String location;
        public String deadline;
        public BigDecimal latitude;
        public BigDecimal longitude;
        public BigDecimal routeStartLatitude;
        public BigDecimal routeStartLongitude;
        public Integer operationRadiusMeters;
        public BigDecimal budget;
        public String status;
        public String ownerName;
    }

    public static class OrderCreateRequest {
        public Long taskId;
    }

    public static class OrderView {
        public Long id;
        public String orderNo;
        public Long taskId;
        public Long pilotId;
        public Long enterpriseId;
        public BigDecimal amount;
        public String status;
    }

    public static class OrderDetailView {
        public Long id;
        public String orderNo;
        public String status;
        public BigDecimal amount;
        public Long taskId;
        public String taskTitle;
        public String taskType;
        public String location;
        public String pilotName;
        public String enterpriseName;
        public String contactName;
        public String contactPhone;
        public String paymentChannel;
        public String paymentStatus;
        public String createdAt;
        public String appointmentTime;
        public String remark;
    }

    public static class PaymentRequest {
        public Long orderId;
        public String channel;
    }

    public static class PaymentResult {
        public String tradeNo;
        public String status;
        public BigDecimal amount;
    }

    public static class NoFlyZoneView {
        public Long id;
        public String name;
        public String zoneType;
        public BigDecimal centerLat;
        public BigDecimal centerLng;
        public int radius;
        public String description;
    }

    public static class FlightApplicationRequest {
        public String location;
        public String flightTime;
        public String purpose;
    }

    public static class FlightApplicationView {
        public String applicationNo;
        public String status;
        public String location;
        public String approvalHint;
    }

    public static class FeedbackTicketRequest {
        public String contact;
        public String detail;
    }

    public static class FeedbackTicketReplyRequest {
        public String status;
        public String reply;
    }

    public static class FeedbackTicketView {
        public Long id;
        public String ticketNo;
        public String submitterName;
        public String submitterRole;
        public String contact;
        public String detail;
        public String status;
        public String reply;
        public String createTime;
        public String updateTime;
        public String closedTime;
    }

    public static class MessageSendRequest {
        public String content;
    }

    public static class MessageReadReceiptRequest {
        public java.util.List<Long> msgIds;
    }

    public static class MessageReadReceiptResponse {
        public int successCount;
        public java.util.List<Long> syncedMsgIds;
    }

    public static class MessageConversationView {
        public Long id;
        public String title;
        public String subtitle;
        public String counterpartName;
        public String counterpartUid;
        public String counterpartRole;
        public String pilotUid;
        public String enterpriseUid;
        public String lastMessagePreview;
        public String lastMessageTime;
        public int unreadCount;
    }

    public static class MessageEntryView {
        public Long id;
        public Long senderId;
        public String pilotUid;
        public String enterpriseUid;
        public String senderName;
        public String senderRole;
        public String content;
        public String createTime;
        public boolean isRead;
        public boolean mine;
    }

    public static class MessageThreadView {
        public Long conversationId;
        public String title;
        public String subtitle;
        public String pilotUid;
        public String enterpriseUid;
        public java.util.List<MessageEntryView> messages;
    }

    public static class PilotProfileView {
        public String uid;
        public String name;
    }

    public static class EnterpriseInfoView {
        public String uid;
        public String companyName;
    }

    public static class AlertView {
        public Long id;
        public String level;
        public String content;
        public String status;
        public String createTime;
    }

    public static class CourseView {
        public Long id;
        public String title;
        public String summary;
        public String category;
        public String learningMode;
        public String institutionName;
        public int seatAvailable;
        public int browseCount;
        public int enrollCount;
        public BigDecimal price;
        public String status;
        public boolean enrolled;
    }

    public static class CourseDetailView {
        public Long id;
        public String title;
        public String summary;
        public String content;
        public String category;
        public String learningMode;
        public String institutionName;
        public int seatTotal;
        public int seatAvailable;
        public int browseCount;
        public int enrollCount;
        public BigDecimal price;
        public String status;
        public boolean enrolled;
        public String enrollmentNo;
        public String enrollmentStatus;
    }

    public static class CourseManageRequest {
        public String title;
        public String summary;
        public String content;
        public String learningMode;
        public Integer seatTotal;
        public BigDecimal price;
        public String status;
    }

    public static class CourseManageView {
        public Long id;
        public String title;
        public String summary;
        public String learningMode;
        public int seatTotal;
        public int seatAvailable;
        public int browseCount;
        public int enrollCount;
        public BigDecimal price;
        public String status;
    }

    public static class EnrollmentResult {
        public String enrollmentNo;
        public String status;
        public String courseTitle;
        public int seatAvailable;
    }

    public static class FlightConditionCheck {
        public String label;
        public String currentValue;
        public String threshold;
        public boolean passed;
    }

    public static class FlightSuitabilityView {
        public String result;
        public String level;
        public String summary;
        public java.util.List<FlightConditionCheck> checks;
        public java.util.List<String> conditionNotes;
        public java.util.List<String> recommendations;
    }

    public static class RealtimeWeatherView {
        public String serviceName;
        public String locationName;
        public String adcode;
        public String weather;
        public String weatherIconType;
        public String reportTime;
        public String fetchedAt;
        public String refreshInterval;
        public double temperature;
        public int humidity;
        public String windDirection;
        public String windPower;
        public double windSpeed;
        public double visibility;
        public int precipitationProbability;
        public double precipitationIntensity;
        public String thunderstormRisk;
        public int thunderstormRiskLevel;
        public String thunderstormRiskLabel;
        public String thunderstormRiskHint;
        public String thunderstormProtectionAdvice;
        public String sourceNote;
        public FlightSuitabilityView suitability;
    }
}

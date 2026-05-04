package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.security.RoleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PlatformUtils {

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PlatformUtils() {
    }

    public static String formatTime(LocalDateTime time) {
        return TIME_FORMATTER.format(time);
    }

    public static String formatDateTime(LocalDateTime time) {
        if (time == null) {
            return "-";
        }
        return TIME_FORMATTER.format(time);
    }

    public static LocalDateTime parseTaskDeadline(String deadline) {
        try {
            return LocalDateTime.parse(deadline, TIME_FORMATTER);
        } catch (Exception ex) {
            throw new com.lowaltitude.reststop.server.common.BizException(400, "任务截止时间格式不正确");
        }
    }

    public static String displayName(UserAccountEntity user) {
        if (user == null || isBlank(user.getRealName())) {
            return user == null ? "-" : user.getUsername();
        }
        return user.getRealName();
    }

    public static String safePhone(UserAccountEntity user) {
        return user == null || isBlank(user.getPhone()) ? "-" : user.getPhone();
    }

    public static String displayRole(String role) {
        if (isBlank(role)) {
            return "未定义角色";
        }
        try {
            return switch (RoleType.valueOf(role)) {
                case ADMIN -> "管理员";
                case ENTERPRISE -> "企业";
                case PILOT -> "飞手";
                case INSTITUTION -> "机构";
            };
        } catch (IllegalArgumentException ex) {
            return role;
        }
    }

    public static String permissionGroupName(String role) {
        if (isBlank(role)) {
            return "未分配";
        }
        return switch (RoleType.valueOf(role)) {
            case ADMIN -> "全量管控组";
            case ENTERPRISE -> "项目运营组";
            case PILOT -> "执行协同组";
            case INSTITUTION -> "培训管理组";
        };
    }

    public static String mapFeedbackStatus(String status) {
        if ("PROCESSING".equalsIgnoreCase(status)) {
            return "处理中";
        }
        if ("CLOSED".equalsIgnoreCase(status)) {
            return "已关闭";
        }
        return "待处理";
    }

    public static String normalizeStatus(String status) {
        if ("已关闭".equals(status) || "CLOSED".equalsIgnoreCase(status)) {
            return "CLOSED";
        }
        if ("处理中".equals(status) || "PROCESSING".equalsIgnoreCase(status)) {
            return "PROCESSING";
        }
        return "OPEN";
    }

    public static BigDecimal defaultBudget(BigDecimal budget) {
        return budget == null ? BigDecimal.ZERO : budget;
    }

    public static String defaultIfBlank(String text, String fallback) {
        return isBlank(text) ? fallback : text.trim();
    }

    public static String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    public static String toUid(Long userId) {
        return userId == null ? "" : String.valueOf(userId);
    }

    public static String paymentChannelLabel(String channel) {
        if ("ALIPAY".equalsIgnoreCase(channel)) {
            return "支付宝";
        }
        if ("WECHAT".equalsIgnoreCase(channel) || "WECHAT_PAY".equalsIgnoreCase(channel)) {
            return "微信支付";
        }
        if ("BANK_TRANSFER".equalsIgnoreCase(channel)) {
            return "银行转账";
        }
        return defaultIfBlank(channel, "待支付");
    }

    public static String mapAdminUserStatus(Integer status) {
        if (status == null) {
            return "待审核";
        }
        if (status <= 0) {
            return "停用";
        }
        return "启用";
    }

    public static String mapAdminProjectStatus(String status) {
        if ("REVIEWING".equalsIgnoreCase(status)) {
            return "规划中";
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return "执行中";
        }
        if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            return "已完成";
        }
        if ("CANCELLED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return "已暂停";
        }
        return "规划中";
    }

    public static int estimateProjectProgress(String status) {
        if ("REVIEWING".equalsIgnoreCase(status)) {
            return 20;
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return 68;
        }
        if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            return 100;
        }
        if ("CANCELLED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return 56;
        }
        return 32;
    }

    public static String estimateRiskLevel(boolean isReviewing, boolean hasOrders) {
        if (isReviewing) {
            return "中";
        }
        if (!hasOrders) {
            return "中";
        }
        return "低";
    }

    public static int estimateTrainingCompletion(String taskType) {
        if ("MAPPING".equalsIgnoreCase(taskType)) {
            return 72;
        }
        if ("INSPECTION".equalsIgnoreCase(taskType)) {
            return 88;
        }
        return 60;
    }

    public static String resolvePaymentStatus(boolean hasOrders, long paidCount, int totalOrders) {
        if (!hasOrders) {
            return "待结算";
        }
        if (paidCount == 0) {
            return "待结算";
        }
        if (paidCount == totalOrders) {
            return "已结算";
        }
        return "部分结算";
    }

    public static String buildOrderRemark(String taskType) {
        if ("MAPPING".equalsIgnoreCase(taskType)) {
            return "请提前校准测绘设备，并在预约时间前 30 分钟到场。";
        }
        if ("INSPECTION".equalsIgnoreCase(taskType)) {
            return "请优先完成重点航段巡检，并同步回传关键风险点照片。";
        }
        return "请按预约时间到场执行，并在完工后及时回传结果。";
    }

    public static int buildOperationRadius(String taskType) {
        if ("MAPPING".equalsIgnoreCase(taskType)) {
            return 1200;
        }
        if ("INSPECTION".equalsIgnoreCase(taskType)) {
            return 900;
        }
        return 700;
    }

    public static String normalizeCourseType(String learningMode) {
        if ("OFFLINE".equalsIgnoreCase(learningMode)) {
            return "OFFLINE";
        }
        return "ARTICLE";
    }

    public static String normalizeCourseStatus(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            return "OPEN";
        }
        if ("CLOSED".equalsIgnoreCase(status)) {
            return "CLOSED";
        }
        return "DRAFT";
    }

    public static int resolveSeatAvailable(Integer originalSeatTotal, Integer originalSeatAvailable, Integer requestSeatTotal) {
        int total = Math.max(0, requestSeatTotal == null ? 0 : requestSeatTotal);
        if (originalSeatTotal == null) {
            return total;
        }
        int oldTotal = safeInt(originalSeatTotal);
        int oldAvailable = safeInt(originalSeatAvailable);
        int used = Math.max(0, oldTotal - oldAvailable);
        return Math.max(0, total - used);
    }

    public static RoleType parseRole(String role) {
        try {
            return RoleType.valueOf(role.toUpperCase());
        } catch (Exception ex) {
            throw new com.lowaltitude.reststop.server.common.BizException(400, "角色不合法");
        }
    }

    public static void ensureRole(com.lowaltitude.reststop.server.security.SessionUser user, RoleType expected) {
        if (user.role() != expected && user.role() != RoleType.ADMIN) {
            throw new com.lowaltitude.reststop.server.common.BizException(403, "当前角色无权执行该操作");
        }
    }
}

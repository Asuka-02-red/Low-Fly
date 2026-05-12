package com.example.low_altitudereststop.feature.ai;

import androidx.annotation.NonNull;

/**
 * AI助手悬浮球显示协调器，决定悬浮球的可用状态。
 * <p>
 * 根据用户开关、悬浮窗权限和无障碍服务三个条件综合判断悬浮球的可用性级别
 * （已禁用、悬浮窗就绪、无障碍降级、需授权），供主界面统一调度显示策略。
 * </p>
 */
public final class AiBallDisplayCoordinator {

    public enum Availability {
        DISABLED,
        OVERLAY_READY,
        ACCESSIBILITY_FALLBACK,
        PERMISSION_REQUIRED
    }

    private AiBallDisplayCoordinator() {
    }

    @NonNull
    public static Availability resolve(boolean enabled, boolean overlayGranted, boolean accessibilityEnabled) {
        if (!enabled) {
            return Availability.DISABLED;
        }
        if (overlayGranted) {
            return Availability.OVERLAY_READY;
        }
        if (accessibilityEnabled) {
            return Availability.ACCESSIBILITY_FALLBACK;
        }
        return Availability.PERMISSION_REQUIRED;
    }

    public static boolean shouldAttachOverlayHost(@NonNull Availability availability) {
        return availability == Availability.OVERLAY_READY;
    }

    public static boolean shouldUseAccessibilityFallback(@NonNull Availability availability) {
        return availability == Availability.ACCESSIBILITY_FALLBACK;
    }
}

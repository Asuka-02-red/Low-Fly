package com.example.low_altitudereststop.feature.ai;

import androidx.annotation.NonNull;

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

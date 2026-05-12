package com.example.low_altitudereststop.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/**
 * 无障碍显示模式枚举，定义应用支持的视觉辅助模式。
 * <p>
 * 提供标准模式（STANDARD）和高对比度模式（HIGH_CONTRAST）两种选项，
 * 高对比度模式增强文字与背景的对比度，提升视觉障碍用户的可读性。
 * </p>
 */
public final class AccessibilityDisplayMode {

    public static final String PREF = AppThemeMode.PREF;
    public static final String KEY_HIGH_CONTRAST = "high_contrast_enabled";

    private AccessibilityDisplayMode() {
    }

    public static boolean isHighContrastEnabled(@NonNull Context context) {
        SharedPreferences preferences =
                context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    public static void persistHighContrast(@NonNull Context context, boolean enabled) {
        context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HIGH_CONTRAST, enabled)
                .apply();
    }
}

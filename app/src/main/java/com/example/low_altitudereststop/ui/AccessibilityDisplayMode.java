package com.example.low_altitudereststop.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

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

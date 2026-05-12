package com.example.low_altitudereststop.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * 应用主题模式枚举，定义应用支持的深色/浅色主题选项。
 * <p>
 * 提供浅色模式（LIGHT）、深色模式（DARK）和跟随系统（SYSTEM）三种选项，
 * 通过SharedPreferences持久化用户选择，供SettingsActivity和全局主题配置使用。
 * </p>
 */
public final class AppThemeMode {

    public static final String PREF = "profile_settings";
    public static final String KEY_DARK_MODE = "follow_dark_mode";

    private AppThemeMode() {
    }

    public static void applyFromPreferences(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void persistAndApply(@NonNull Context context, boolean followSystem) {
        context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DARK_MODE, false)
                .apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void apply(boolean followSystem) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}

package com.example.low_altitudereststop.feature.ai;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/**
 * AI助手用户偏好设置存储类。
 * <p>
 * 基于SharedPreferences持久化存储AI助手功能的启用/禁用状态，
 * 供主界面和设置页读写用户开关偏好。
 * </p>
 */
public final class AiBallSettingsStore {

    private static final String PREF = "ai_ball_settings";
    private static final String KEY_ENABLED = "enabled";

    private final SharedPreferences preferences;

    public AiBallSettingsStore(@NonNull Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return preferences.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}

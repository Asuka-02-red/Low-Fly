package com.example.low_altitudereststop.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

public class UsageAnalyticsStore {

    private static final String PREF = "usage_analytics";
    private static final String PREFIX_FEATURE = "feature_";
    private static final String PREFIX_ROLE = "role_";
    private static final String PREFIX_GUIDE = "guide_";

    private final SharedPreferences preferences;

    public UsageAnalyticsStore(@NonNull Context context) {
        preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void trackRoleLanding(@NonNull UserRole role) {
        increment(PREFIX_ROLE + role.name());
    }

    public void trackFeature(@NonNull UserRole role, @NonNull String featureKey) {
        increment(PREFIX_FEATURE + role.name() + "_" + featureKey);
    }

    public int featureCount(@NonNull UserRole role, @NonNull String featureKey) {
        return preferences.getInt(PREFIX_FEATURE + role.name() + "_" + featureKey, 0);
    }

    public boolean shouldShowGuide(@NonNull UserRole role) {
        return !preferences.getBoolean(PREFIX_GUIDE + role.name(), false);
    }

    public void markGuideShown(@NonNull UserRole role) {
        preferences.edit().putBoolean(PREFIX_GUIDE + role.name(), true).apply();
    }

    public String summary(@NonNull UserRole role) {
        int task = featureCount(role, "task");
        int compliance = featureCount(role, "compliance");
        int profile = featureCount(role, "profile");
        return "使用统计：任务 " + task + " 次 · 合规 " + compliance + " 次 · 个人中心 " + profile + " 次";
    }

    private void increment(String key) {
        int current = preferences.getInt(key, 0);
        preferences.edit().putInt(key, current + 1).apply();
    }
}

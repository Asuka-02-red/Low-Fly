package com.example.low_altitudereststop.ui;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

public final class AppThemeMode {

    public static final String PREF = "profile_settings";
    public static final String KEY_DARK_MODE = "follow_dark_mode";

    private AppThemeMode() {
    }

    public static void applyFromPreferences(@NonNull Context context) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        apply(preferences.getBoolean(KEY_DARK_MODE, true));
    }

    public static void persistAndApply(@NonNull Context context, boolean followSystem) {
        context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DARK_MODE, followSystem)
                .apply();
        apply(followSystem);
    }

    public static void apply(boolean followSystem) {
        AppCompatDelegate.setDefaultNightMode(
                followSystem
                        ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}

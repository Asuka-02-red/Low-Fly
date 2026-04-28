package com.example.low_altitudereststop.feature.ai;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.example.low_altitudereststop.feature.ai.service.AiBallAccessibilityService;

public final class AiBallAccessibilityHelper {

    private AiBallAccessibilityHelper() {
    }

    @NonNull
    public static Intent createAccessibilitySettingsIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    public static boolean isServiceEnabled(@NonNull Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException ignored) {
        }
        if (accessibilityEnabled != 1) {
            return false;
        }
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        String target = new ComponentName(context, AiBallAccessibilityService.class).flattenToString();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            if (target.equalsIgnoreCase(splitter.next())) {
                return true;
            }
        }
        return false;
    }
}

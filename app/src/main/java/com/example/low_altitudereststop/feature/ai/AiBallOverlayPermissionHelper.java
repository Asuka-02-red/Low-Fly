package com.example.low_altitudereststop.feature.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;

/**
 * AI助手悬浮窗权限辅助工具类。
 * <p>
 * 封装查询系统悬浮窗（SYSTEM_ALERT_WINDOW）权限是否已授予，
 * 以及构造跳转到系统悬浮窗权限管理页面的Intent。
 * </p>
 */
public final class AiBallOverlayPermissionHelper {

    private AiBallOverlayPermissionHelper() {
    }

    public static boolean canDrawOverlays(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return Settings.canDrawOverlays(context);
    }

    @NonNull
    public static Intent createManageOverlayPermissionIntent(@NonNull Context context) {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName())
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}

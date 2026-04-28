package com.example.low_altitudereststop.feature.ai;

import android.app.BackgroundServiceStartNotAllowedException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.low_altitudereststop.BuildConfig;
import com.example.low_altitudereststop.feature.ai.service.AiBallBinderService;

public final class AiBallServiceLauncher {

    private static final String TAG = "AiBallServiceLauncher";

    private AiBallServiceLauncher() {
    }

    public static void ensureStarted(@NonNull Context context) {
        if (!BuildConfig.ENABLE_AI_BALL) {
            return;
        }
        Intent serviceIntent = new Intent(context, AiBallBinderService.class);
        try {
            context.startService(serviceIntent);
        } catch (RuntimeException exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && exception instanceof BackgroundServiceStartNotAllowedException) {
                Log.w(TAG, "Skip starting AI ball service in background-restricted state", exception);
                return;
            }
            throw exception;
        }
    }
}

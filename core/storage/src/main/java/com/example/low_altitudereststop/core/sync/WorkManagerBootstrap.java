package com.example.low_altitudereststop.core.sync;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.WorkManager;

public final class WorkManagerBootstrap {

    private WorkManagerBootstrap() {
    }

    @Nullable
    public static WorkManager getOrNull(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            return WorkManager.getInstance(appContext);
        } catch (IllegalStateException ignored) {
            try {
                WorkManager.initialize(appContext, new Configuration.Builder().build());
                return WorkManager.getInstance(appContext);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }
}

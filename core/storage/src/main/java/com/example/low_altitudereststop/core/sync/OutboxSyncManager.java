package com.example.low_altitudereststop.core.sync;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.low_altitudereststop.core.storage.OperationOutboxStore;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.google.gson.Gson;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class OutboxSyncManager {

    private static final String PERIODIC_NAME = "operation_outbox_periodic_sync";
    private static final String ONCE_NAME = "operation_outbox_once_sync";

    private OutboxSyncManager() {
    }

    public static void enqueue(Context context, String bizType, Map<String, Object> payload) {
        Context app = context.getApplicationContext();
        String requestId = new OperationLogStore(app).newRequestId();
        String json = new Gson().toJson(payload);
        new OperationOutboxStore(app).enqueue(bizType, requestId, json);
        scheduleNow(app);
    }

    public static void schedulePeriodic(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(OperationReplayWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager workManager = WorkManagerBootstrap.getOrNull(context);
        if (workManager == null) {
            new OperationLogStore(context.getApplicationContext()).appendCrash("WORK_MANAGER", "outbox periodic init failed");
            return;
        }
        workManager.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, work);
    }

    public static void scheduleNow(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OperationReplayWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager workManager = WorkManagerBootstrap.getOrNull(context);
        if (workManager == null) {
            new OperationLogStore(context.getApplicationContext()).appendCrash("WORK_MANAGER", "outbox one-time init failed");
            return;
        }
        workManager.enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work);
    }
}


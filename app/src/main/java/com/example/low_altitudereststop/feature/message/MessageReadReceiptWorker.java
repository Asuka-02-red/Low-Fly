package com.example.low_altitudereststop.feature.message;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.sync.WorkManagerBootstrap;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.message.local.MessageDao;
import com.example.low_altitudereststop.feature.message.local.MessageLocalDatabase;
import java.util.List;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;

public class MessageReadReceiptWorker extends Worker {

    private static final String ONCE_NAME = "message_read_receipt_once";
    private static final String PERIODIC_NAME = "message_read_receipt_periodic";
    private static final int RETRY_LIMIT = 3;

    private final Context appContext;
    private final MessageDao messageDao;

    public MessageReadReceiptWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.appContext = context.getApplicationContext();
        this.messageDao = MessageLocalDatabase.get(appContext).messageDao();
    }

    public static void enqueueNow(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MessageReadReceiptWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager workManager = WorkManagerBootstrap.getOrNull(context);
        if (workManager == null) {
            new OperationLogStore(context.getApplicationContext()).appendCrash("WORK_MANAGER", "message once init failed");
            return;
        }
        workManager.enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    public static void schedulePeriodic(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(MessageReadReceiptWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager workManager = WorkManagerBootstrap.getOrNull(context);
        if (workManager == null) {
            new OperationLogStore(context.getApplicationContext()).appendCrash("WORK_MANAGER", "message periodic init failed");
            return;
        }
        workManager.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<Long> msgIds = messageDao.listPendingReceiptIds(RETRY_LIMIT);
        if (msgIds.isEmpty()) {
            if (messageDao.countFailedReceiptSync(RETRY_LIMIT) > 0) {
                new OperationLogStore(appContext).appendCrash("MESSAGE_READ_RECEIPT", "同步失败");
                MessageSyncNotifier.notifySyncFailed();
            }
            return Result.success();
        }
        try {
            PlatformModels.MessageReadReceiptRequest request = new PlatformModels.MessageReadReceiptRequest();
            request.msgIds = msgIds;
            Response<ApiEnvelope<PlatformModels.MessageReadReceiptResponse>> response =
                    ApiClient.getAuthedService(appContext).sendReadReceipt(request).execute();
            if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                List<Long> synced = response.body().data.syncedMsgIds == null || response.body().data.syncedMsgIds.isEmpty()
                        ? msgIds
                        : response.body().data.syncedMsgIds;
                messageDao.markReceiptsSynced(synced, System.currentTimeMillis());
                return Result.success();
            }
            return handleFailure(msgIds, "服务器返回失败");
        } catch (Exception e) {
            return handleFailure(msgIds, e.getMessage());
        }
    }

    private Result handleFailure(List<Long> msgIds, String errorMessage) {
        messageDao.increaseReceiptRetry(msgIds);
        List<Long> failedIds = messageDao.listFailedReceiptIds(RETRY_LIMIT);
        if (!failedIds.isEmpty()) {
            new OperationLogStore(appContext).appendCrash("MESSAGE_READ_RECEIPT", errorMessage == null ? "同步失败" : errorMessage);
            MessageSyncNotifier.notifySyncFailed();
            return Result.success();
        }
        return Result.retry();
    }
}

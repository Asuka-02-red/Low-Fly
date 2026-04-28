package com.example.low_altitudereststop.core.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.storage.OperationOutboxEntity;
import com.example.low_altitudereststop.core.storage.OperationOutboxStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.List;
import retrofit2.Response;

public class OperationReplayWorker extends Worker {

    private final OperationOutboxStore outboxStore;
    private final Gson gson = new Gson();

    public OperationReplayWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.outboxStore = new OperationOutboxStore(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<OperationOutboxEntity> items = outboxStore.listReady(20);
        for (OperationOutboxEntity item : items) {
            try {
                boolean ok = replay(item.bizType, item.payload);
                if (ok) {
                    outboxStore.markSuccess(item.id);
                } else if (item.retryCount >= 5) {
                    outboxStore.markFailed(item.id, "服务器返回失败");
                } else {
                    outboxStore.retryLater(item.id, item.retryCount + 1, "服务器返回失败");
                }
            } catch (Exception e) {
                if (item.retryCount >= 5) {
                    outboxStore.markFailed(item.id, e.getMessage());
                } else {
                    outboxStore.retryLater(item.id, item.retryCount + 1, e.getMessage());
                }
            }
        }
        return Result.success();
    }

    private boolean replay(String bizType, String payloadJson) throws Exception {
        JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);
        Context ctx = getApplicationContext();
        if ("PAY_ORDER".equals(bizType)) {
            PlatformModels.PaymentRequest req = new PlatformModels.PaymentRequest();
            req.orderId = payload.get("orderId").getAsLong();
            req.channel = payload.has("channel") ? payload.get("channel").getAsString() : "demo";
            Response<ApiEnvelope<PlatformModels.PaymentResult>> r = ApiClient.getAuthedService(ctx).payOrder(req).execute();
            return r.isSuccessful() && r.body() != null && r.body().data != null;
        }
        if ("ENROLL_COURSE".equals(bizType)) {
            long courseId = payload.get("courseId").getAsLong();
            Response<ApiEnvelope<PlatformModels.EnrollmentResult>> r = ApiClient.getAuthedService(ctx).enroll(courseId).execute();
            return r.isSuccessful() && r.body() != null && r.body().data != null;
        }
        if ("SUBMIT_FLIGHT".equals(bizType)) {
            PlatformModels.FlightApplicationRequest req = new PlatformModels.FlightApplicationRequest();
            req.location = str(payload, "location");
            req.flightTime = str(payload, "flightTime");
            req.purpose = str(payload, "purpose");
            Response<ApiEnvelope<PlatformModels.FlightApplicationView>> r = ApiClient.getAuthedService(ctx).submitFlightApplication(req).execute();
            return r.isSuccessful() && r.body() != null && r.body().data != null;
        }
        if ("CREATE_TASK".equals(bizType)) {
            PlatformModels.TaskRequest req = new PlatformModels.TaskRequest();
            req.taskType = str(payload, "taskType");
            req.title = str(payload, "title");
            req.description = str(payload, "description");
            req.location = str(payload, "location");
            req.deadline = str(payload, "deadline");
            req.latitude = new BigDecimal(str(payload, "latitude"));
            req.longitude = new BigDecimal(str(payload, "longitude"));
            req.budget = new BigDecimal(str(payload, "budget"));
            Response<ApiEnvelope<PlatformModels.TaskView>> r = ApiClient.getAuthedService(ctx).createTask(req).execute();
            return r.isSuccessful() && r.body() != null && r.body().data != null;
        }
        if ("UPDATE_TASK_REPUBLISH".equals(bizType)) {
            long taskId = payload.get("taskId").getAsLong();
            PlatformModels.TaskRequest req = new PlatformModels.TaskRequest();
            req.taskType = str(payload, "taskType");
            req.title = str(payload, "title");
            req.description = str(payload, "description");
            req.location = str(payload, "location");
            req.deadline = str(payload, "deadline");
            req.latitude = new BigDecimal(str(payload, "latitude"));
            req.longitude = new BigDecimal(str(payload, "longitude"));
            req.budget = new BigDecimal(str(payload, "budget"));
            Response<ApiEnvelope<PlatformModels.TaskView>> updateResponse = ApiClient.getAuthedService(ctx).updateTask(taskId, req).execute();
            if (!updateResponse.isSuccessful() || updateResponse.body() == null || updateResponse.body().data == null) {
                return false;
            }
            Response<ApiEnvelope<PlatformModels.TaskView>> publishResponse = ApiClient.getAuthedService(ctx).publishTask(taskId).execute();
            return publishResponse.isSuccessful() && publishResponse.body() != null && publishResponse.body().data != null;
        }
        return true;
    }

    private String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
}


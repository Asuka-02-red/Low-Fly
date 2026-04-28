package com.example.low_altitudereststop.core.storage;

import android.content.Context;
import java.util.List;

public class OperationOutboxStore {

    private final OperationOutboxDao dao;

    public OperationOutboxStore(Context context) {
        this.dao = OperationOutboxDatabase.get(context).outboxDao();
    }

    public long enqueue(String bizType, String requestId, String payload) {
        OperationOutboxEntity entity = new OperationOutboxEntity();
        entity.bizType = bizType == null ? "" : bizType;
        entity.requestId = requestId == null ? "" : requestId;
        entity.payload = payload == null ? "" : payload;
        entity.status = "PENDING";
        entity.retryCount = 0;
        entity.nextRetryAt = System.currentTimeMillis();
        entity.createTime = System.currentTimeMillis();
        return dao.insert(entity);
    }

    public List<OperationOutboxEntity> listAll() {
        return dao.listAll();
    }

    public List<OperationOutboxEntity> listReady(int limit) {
        return dao.listReady(System.currentTimeMillis(), limit);
    }

    public void markSuccess(long id) {
        dao.markStatus(id, "SUCCESS", null);
    }

    public void markFailed(long id, String message) {
        dao.markStatus(id, "FAILED", message);
    }

    public void retryLater(long id, int retryCount, String message) {
        long backoff = Math.min(5 * 60_000L, (long) Math.pow(2, Math.min(retryCount, 8)) * 1000L);
        dao.increaseRetry(id, System.currentTimeMillis() + backoff, message);
    }

    public void retryNow(long id) {
        dao.retryNow(id, System.currentTimeMillis());
    }

    public void clearFinished() {
        dao.deleteFinished();
    }
}


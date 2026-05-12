package com.example.low_altitudereststop.core.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 操作发件箱实体，表示待重放的离线操作记录的Room数据表结构，
 * 包含业务类型、请求负载、重试状态和错误信息等字段。
 */
@Entity(tableName = "operation_outbox")
public class OperationOutboxEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String bizType = "";

    @NonNull
    public String requestId = "";

    @NonNull
    public String payload = "";

    @NonNull
    public String status = "PENDING";

    public int retryCount = 0;

    public long nextRetryAt = 0L;

    public long createTime = 0L;

    public String lastError;
}


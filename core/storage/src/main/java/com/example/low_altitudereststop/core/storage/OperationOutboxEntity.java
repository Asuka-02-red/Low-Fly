package com.example.low_altitudereststop.core.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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


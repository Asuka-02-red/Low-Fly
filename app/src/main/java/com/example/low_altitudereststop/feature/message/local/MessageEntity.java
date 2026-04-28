package com.example.low_altitudereststop.feature.message.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    public long msgId;

    public long conversationId;

    @NonNull
    public String content = "";

    @NonNull
    public String senderName = "";

    @NonNull
    public String senderRole = "";

    @NonNull
    public String pilotUid = "";

    @NonNull
    public String enterpriseUid = "";

    @NonNull
    public String counterpartTitle = "";

    @NonNull
    public String createTime = "";

    public long createTimeMillis;

    public boolean mine;

    public boolean isRead;

    public boolean readReceiptPending;

    public int receiptRetryCount;

    public long receiptSyncedAt;
}

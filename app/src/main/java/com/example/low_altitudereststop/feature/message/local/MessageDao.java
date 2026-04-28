package com.example.low_altitudereststop.feature.message.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.SkipQueryVerification;
import java.util.List;

@Dao
@SkipQueryVerification
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<MessageEntity> messages);

    @Query("SELECT * FROM messages ORDER BY createTimeMillis DESC, msgId DESC")
    LiveData<List<MessageEntity>> observeAllMessages();

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createTimeMillis ASC, msgId ASC")
    LiveData<List<MessageEntity>> observeConversation(long conversationId);

    @Query("SELECT * FROM messages WHERE msgId = :msgId LIMIT 1")
    LiveData<MessageEntity> observeMessage(long msgId);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createTimeMillis ASC, msgId ASC")
    List<MessageEntity> listConversationSync(long conversationId);

    @Query("SELECT * FROM messages WHERE msgId = :msgId LIMIT 1")
    MessageEntity findById(long msgId);

    @Query("UPDATE messages SET isRead = :isRead WHERE msgId = :msgId")
    void updateReadStatus(long msgId, boolean isRead);

    @Query("UPDATE messages SET isRead = 1, readReceiptPending = 1 WHERE conversationId = :conversationId AND mine = 0 AND isRead = 0")
    void markConversationAsRead(long conversationId);

    @Query("SELECT msgId FROM messages WHERE readReceiptPending = 1 AND receiptRetryCount < :retryLimit ORDER BY createTimeMillis ASC")
    List<Long> listPendingReceiptIds(int retryLimit);

    @Query("UPDATE messages SET readReceiptPending = 0, receiptRetryCount = 0, receiptSyncedAt = :syncedAt WHERE msgId IN (:msgIds)")
    void markReceiptsSynced(List<Long> msgIds, long syncedAt);

    @Query("UPDATE messages SET receiptRetryCount = receiptRetryCount + 1 WHERE msgId IN (:msgIds)")
    void increaseReceiptRetry(List<Long> msgIds);

    @Query("SELECT COUNT(*) FROM messages WHERE readReceiptPending = 1 AND receiptRetryCount >= :retryLimit")
    int countFailedReceiptSync(int retryLimit);

    @Query("SELECT msgId FROM messages WHERE readReceiptPending = 1 AND receiptRetryCount >= :retryLimit ORDER BY createTimeMillis ASC")
    List<Long> listFailedReceiptIds(int retryLimit);

    @Query("DELETE FROM messages")
    void clearAll();
}

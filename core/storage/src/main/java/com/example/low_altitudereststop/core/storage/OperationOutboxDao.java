package com.example.low_altitudereststop.core.storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.SkipQueryVerification;
import java.util.List;

/**
 * 操作发件箱数据访问对象，定义对操作发件箱表的Room数据库增删改查操作，
 * 支持按状态查询、重试计数更新和已完成记录清理。
 */
@Dao
@SkipQueryVerification
public interface OperationOutboxDao {

    @Insert
    long insert(OperationOutboxEntity entity);

    @Query("SELECT * FROM operation_outbox ORDER BY createTime DESC, id DESC")
    List<OperationOutboxEntity> listAll();

    @Query("SELECT * FROM operation_outbox WHERE status = 'PENDING' AND nextRetryAt <= :now ORDER BY id ASC LIMIT :limit")
    List<OperationOutboxEntity> listReady(long now, int limit);

    @Query("UPDATE operation_outbox SET status = :status, lastError = :lastError WHERE id = :id")
    void markStatus(long id, String status, String lastError);

    @Query("UPDATE operation_outbox SET retryCount = retryCount + 1, nextRetryAt = :nextRetryAt, lastError = :lastError WHERE id = :id")
    void increaseRetry(long id, long nextRetryAt, String lastError);

    @Query("UPDATE operation_outbox SET status = 'PENDING', nextRetryAt = :nextRetryAt, lastError = NULL WHERE id = :id")
    void retryNow(long id, long nextRetryAt);

    @Query("DELETE FROM operation_outbox WHERE status IN ('SUCCESS', 'FAILED')")
    void deleteFinished();
}


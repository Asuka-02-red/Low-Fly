package com.example.low_altitudereststop.core.storage;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.SkipQueryVerification;

/**
 * 操作发件箱Room数据库，提供发件箱持久化的单例数据库实例，
 * 包含操作发件箱实体的数据表定义。
 */
@Database(entities = {OperationOutboxEntity.class}, version = 1, exportSchema = false)
@SkipQueryVerification
public abstract class OperationOutboxDatabase extends RoomDatabase {

    private static volatile OperationOutboxDatabase INSTANCE;

    public abstract OperationOutboxDao outboxDao();

    public static OperationOutboxDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (OperationOutboxDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    OperationOutboxDatabase.class,
                                    "operation_outbox.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}


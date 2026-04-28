package com.example.low_altitudereststop.feature.message.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.SkipQueryVerification;

@Database(
        entities = {
                MessageEntity.class,
                PilotProfileEntity.class,
                EnterpriseProfileEntity.class
        },
        version = 1,
        exportSchema = false
)
@SkipQueryVerification
public abstract class MessageLocalDatabase extends RoomDatabase {

    private static volatile MessageLocalDatabase INSTANCE;

    public abstract MessageDao messageDao();

    public abstract PilotProfileDao pilotProfileDao();

    public abstract EnterpriseProfileDao enterpriseProfileDao();

    public static MessageLocalDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (MessageLocalDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MessageLocalDatabase.class,
                                    "message_local.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

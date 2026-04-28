package com.example.low_altitudereststop.feature.message.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.SkipQueryVerification;

@Dao
@SkipQueryVerification
public interface PilotProfileDao {

    @Query("SELECT * FROM pilot_profile WHERE uid = :uid LIMIT 1")
    PilotProfileEntity findByUid(String uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PilotProfileEntity entity);
}

package com.example.low_altitudereststop.feature.message.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pilot_profile")
public class PilotProfileEntity {

    @PrimaryKey
    @NonNull
    public String uid = "";

    @NonNull
    public String name = "";

    public long updateTimeMillis;
}

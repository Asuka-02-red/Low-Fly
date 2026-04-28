package com.example.low_altitudereststop.feature.message.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "enterprise_profile")
public class EnterpriseProfileEntity {

    @PrimaryKey
    @NonNull
    public String uid = "";

    @NonNull
    public String companyName = "";

    public long updateTimeMillis;
}

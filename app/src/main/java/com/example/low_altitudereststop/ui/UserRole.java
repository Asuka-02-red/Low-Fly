package com.example.low_altitudereststop.ui;

import androidx.annotation.NonNull;

public enum UserRole {
    PILOT,
    ENTERPRISE,
    ADMIN,
    UNKNOWN;

    @NonNull
    public static UserRole from(String rawRole) {
        if (rawRole == null) {
            return UNKNOWN;
        }
        if ("PILOT".equalsIgnoreCase(rawRole)) {
            return PILOT;
        }
        if ("ENTERPRISE".equalsIgnoreCase(rawRole)) {
            return ENTERPRISE;
        }
        if ("INSTITUTION".equalsIgnoreCase(rawRole)) {
            return ENTERPRISE;
        }
        if ("ADMIN".equalsIgnoreCase(rawRole)) {
            return ADMIN;
        }
        return UNKNOWN;
    }
}

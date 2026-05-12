package com.example.low_altitudereststop.ui;

import androidx.annotation.NonNull;

/**
 * 用户角色枚举，定义应用支持的两种用户角色。
 * <p>
 * 提供飞手（PILOT）和企业（ENTERPRISE）两种角色，
 * 每种角色关联不同的显示名称和角色编码，
 * 供权限控制、UI差异化、数据过滤等场景使用。
 * </p>
 */
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

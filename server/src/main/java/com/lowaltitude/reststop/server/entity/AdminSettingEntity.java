package com.lowaltitude.reststop.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 管理员系统设置实体。
 * <p>
 * 对应 admin_setting 表，以键值对形式存储后台系统配置参数，
 * 包括基础参数、安全策略等设置项。
 * </p>
 */
@TableName("admin_setting")
public class AdminSettingEntity {

    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    @TableField("setting_value")
    private String settingValue;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

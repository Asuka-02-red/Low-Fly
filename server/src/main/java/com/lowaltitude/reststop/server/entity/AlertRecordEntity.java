package com.lowaltitude.reststop.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 告警记录实体。
 * <p>
 * 对应 alert_record 表，存储风险告警信息，
 * 包括关联飞手、告警级别、告警内容及处理状态。
 * </p>
 */
@TableName("alert_record")
public class AlertRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("pilot_id")
    private Long pilotId;

    @TableField("level")
    private String level;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPilotId() {
        return pilotId;
    }

    public void setPilotId(Long pilotId) {
        this.pilotId = pilotId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

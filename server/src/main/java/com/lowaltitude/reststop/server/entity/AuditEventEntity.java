package com.lowaltitude.reststop.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 审计事件实体。
 * <p>
 * 对应 audit_event 表，记录系统中所有关键操作的审计日志，
 * 包括请求标识、操作者信息、业务类型、事件类型及负载内容，
 * 支持操作留痕与安全审计。
 * </p>
 */
@TableName("audit_event")
public class AuditEventEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("request_id")
    private String requestId;

    @TableField("actor_user_id")
    private Long actorUserId;

    @TableField("actor_role")
    private String actorRole;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private String bizId;

    @TableField("event_type")
    private String eventType;

    @TableField("event_payload")
    private String eventPayload;

    @TableField("replayable")
    private Integer replayable;

    @TableField("create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(String eventPayload) {
        this.eventPayload = eventPayload;
    }

    public Integer getReplayable() {
        return replayable;
    }

    public void setReplayable(Integer replayable) {
        this.replayable = replayable;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

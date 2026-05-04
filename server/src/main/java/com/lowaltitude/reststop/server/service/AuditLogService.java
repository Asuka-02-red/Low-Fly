package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.AuditEventEntity;
import com.lowaltitude.reststop.server.mapper.AuditEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务。
 * <p>
 * 提供审计事件的记录与查询功能，包括写入审计事件、
 * 统计事件总数及查询最近的审计事件列表，用于操作留痕与安全审计。
 * </p>
 */
@Service
public class AuditLogService {

    private final AuditEventMapper auditEventMapper;

    public AuditLogService(AuditEventMapper auditEventMapper) {
        this.auditEventMapper = auditEventMapper;
    }

    public void record(String requestId, Long actorUserId, String actorRole, String bizType, String bizId, String eventType, String payload) {
        AuditEventEntity event = new AuditEventEntity();
        event.setRequestId(requestId);
        event.setActorUserId(actorUserId);
        event.setActorRole(actorRole);
        event.setBizType(bizType);
        event.setBizId(bizId);
        event.setEventType(eventType);
        event.setEventPayload(payload);
        event.setReplayable(1);
        event.setCreateTime(LocalDateTime.now());
        auditEventMapper.insert(event);
    }

    public long count() {
        return auditEventMapper.selectCount(new LambdaQueryWrapper<>());
    }

    public List<ApiDtos.AuditEventView> listRecent() {
        return auditEventMapper.selectList(new LambdaQueryWrapper<AuditEventEntity>()
                        .orderByDesc(AuditEventEntity::getCreateTime))
                .stream()
                .map(event -> new ApiDtos.AuditEventView(
                        event.getRequestId(),
                        event.getActorUserId(),
                        event.getActorRole(),
                        event.getBizType(),
                        event.getBizId(),
                        event.getEventType(),
                        event.getEventPayload(),
                        event.getCreateTime()))
                .toList();
    }
}

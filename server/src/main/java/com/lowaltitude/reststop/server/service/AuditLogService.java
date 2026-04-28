package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.AuditEventEntity;
import com.lowaltitude.reststop.server.mapper.AuditEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

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

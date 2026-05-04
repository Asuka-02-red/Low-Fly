package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.AuditEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计事件Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 audit_event 表的CRUD操作。
 * </p>
 */
@Mapper
public interface AuditEventMapper extends BaseMapper<AuditEventEntity> {
}

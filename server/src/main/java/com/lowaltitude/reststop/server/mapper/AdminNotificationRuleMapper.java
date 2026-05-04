package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.AdminNotificationRuleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员通知规则Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 admin_notification_rule 表的CRUD操作。
 * </p>
 */
@Mapper
public interface AdminNotificationRuleMapper extends BaseMapper<AdminNotificationRuleEntity> {
}

package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.AdminSettingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员系统设置Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 admin_setting 表的CRUD操作。
 * </p>
 */
@Mapper
public interface AdminSettingMapper extends BaseMapper<AdminSettingEntity> {
}

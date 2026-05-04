package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.NoFlyZoneEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 禁飞区Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 no_fly_zone 表的CRUD操作。
 * </p>
 */
@Mapper
public interface NoFlyZoneMapper extends BaseMapper<NoFlyZoneEntity> {
}

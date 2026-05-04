package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.AlertRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 alert_record 表的CRUD操作。
 * </p>
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecordEntity> {
}

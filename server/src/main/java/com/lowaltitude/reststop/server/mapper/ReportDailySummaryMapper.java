package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.ReportDailySummaryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日统计报表Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 report_daily_summary 表的CRUD操作。
 * </p>
 */
@Mapper
public interface ReportDailySummaryMapper extends BaseMapper<ReportDailySummaryEntity> {
}

package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 反馈工单Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 feedback_ticket 表的CRUD操作。
 * </p>
 */
@Mapper
public interface FeedbackTicketMapper extends BaseMapper<FeedbackTicketEntity> {
}

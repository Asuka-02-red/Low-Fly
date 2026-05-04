package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息条目Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 message_entry 表的CRUD操作。
 * </p>
 */
@Mapper
public interface MessageEntryMapper extends BaseMapper<MessageEntryEntity> {
}

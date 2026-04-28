package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageEntryMapper extends BaseMapper<MessageEntryEntity> {
}

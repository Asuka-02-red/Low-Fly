package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账号Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 user_account 表的CRUD操作。
 * </p>
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {
}

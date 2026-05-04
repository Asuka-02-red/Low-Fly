package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.AuthRefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 认证刷新令牌Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 auth_refresh_token 表的CRUD操作。
 * </p>
 */
@Mapper
public interface AuthRefreshTokenMapper extends BaseMapper<AuthRefreshTokenEntity> {
}

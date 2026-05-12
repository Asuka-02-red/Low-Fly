package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.AuthRefreshTokenEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.AuthRefreshTokenMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 刷新令牌存储服务。
 * <p>
 * 管理用户刷新令牌的签发与验证，签发时删除旧令牌并创建新记录，
 * 验证时检查令牌是否存在且未过期，过期或无效时抛出401业务异常。
 * </p>
 */
@Service
public class RefreshTokenStore {

    private static final int REFRESH_TOKEN_TTL_DAYS = 7;

    private final AuthRefreshTokenMapper authRefreshTokenMapper;

    public RefreshTokenStore(AuthRefreshTokenMapper authRefreshTokenMapper) {
        this.authRefreshTokenMapper = authRefreshTokenMapper;
    }

    public String issueToken(UserAccountEntity user) {
        String token = "refresh_" + user.getUsername() + "_" + System.currentTimeMillis();
        authRefreshTokenMapper.delete(new LambdaQueryWrapper<AuthRefreshTokenEntity>()
                .eq(AuthRefreshTokenEntity::getUserId, user.getId()));

        AuthRefreshTokenEntity entity = new AuthRefreshTokenEntity();
        entity.setUserId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setRefreshToken(token);
        entity.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        authRefreshTokenMapper.insert(entity);
        return token;
    }

    public String requireUsernameByRefreshToken(String refreshToken) {
        AuthRefreshTokenEntity entity = requireEntityByRefreshToken(refreshToken);
        return entity.getUsername();
    }

    public Long requireUserIdByRefreshToken(String refreshToken) {
        AuthRefreshTokenEntity entity = requireEntityByRefreshToken(refreshToken);
        return entity.getUserId();
    }

    private AuthRefreshTokenEntity requireEntityByRefreshToken(String refreshToken) {
        AuthRefreshTokenEntity entity = authRefreshTokenMapper.selectOne(new LambdaQueryWrapper<AuthRefreshTokenEntity>()
                .eq(AuthRefreshTokenEntity::getRefreshToken, refreshToken)
                .last("limit 1"));
        if (entity == null || entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(401, "刷新令牌无效或已过期");
        }
        return entity;
    }
}

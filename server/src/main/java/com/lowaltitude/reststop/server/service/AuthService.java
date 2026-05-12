package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.security.TokenService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final TokenService tokenService;
    private final UserAccountMapper userAccountMapper;
    private final RefreshTokenStore refreshTokenStore;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public AuthService(
            TokenService tokenService,
            UserAccountMapper userAccountMapper,
            RefreshTokenStore refreshTokenStore,
            AuditLogService auditLogService
    ) {
        this.tokenService = tokenService;
        this.userAccountMapper = userAccountMapper;
        this.refreshTokenStore = refreshTokenStore;
        this.auditLogService = auditLogService;
    }

    public ApiDtos.AuthPayload login(ApiDtos.LoginRequest request) {
        UserAccountEntity user = findUserByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        return buildAuthPayload(user);
    }

    @Transactional
    public ApiDtos.AuthPayload register(ApiDtos.RegisterRequest request) {
        if (findUserByUsername(request.username()) != null) {
            throw new BizException(400, "用户名已存在");
        }
        if (findUserByPhone(request.phone()) != null) {
            throw new BizException(400, "手机号已存在");
        }
        RoleType role = PlatformUtils.parseRole(request.role());
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(role.name());
        user.setRealName(PlatformUtils.isBlank(request.realName()) ? request.username() : request.realName().trim());
        user.setCompanyName(PlatformUtils.normalizeNullable(request.companyName()));
        user.setStatus(1);
        user.setVersion(0);
        userAccountMapper.insert(user);
        audit(user.getId(), role.name(), "USER", String.valueOf(user.getId()), "REGISTER", "role=" + role.name());
        return buildAuthPayload(user);
    }

    public ApiDtos.AuthPayload refresh(ApiDtos.RefreshTokenRequest request) {
        Long userId = refreshTokenStore.requireUserIdByRefreshToken(request.refreshToken());
        UserAccountEntity user = getUserById(userId);
        SessionUser sessionUser = toSessionUser(user);
        return new ApiDtos.AuthPayload(
                tokenService.createToken(sessionUser),
                request.refreshToken(),
                toSessionInfo(user)
        );
    }

    public ApiDtos.SessionInfo currentUser(SessionUser user) {
        return toSessionInfo(getUserById(user.id()));
    }

    private ApiDtos.AuthPayload buildAuthPayload(UserAccountEntity user) {
        SessionUser sessionUser = toSessionUser(user);
        String refreshToken = refreshTokenStore.issueToken(user);
        audit(user.getId(), user.getRole(), "AUTH", user.getUsername(), "LOGIN", user.getRole());
        return new ApiDtos.AuthPayload(
                tokenService.createToken(sessionUser),
                refreshToken,
                toSessionInfo(user)
        );
    }

    private SessionUser toSessionUser(UserAccountEntity user) {
        return new SessionUser(user.getId(), user.getUsername(), RoleType.valueOf(user.getRole()), PlatformUtils.displayName(user));
    }

    private ApiDtos.SessionInfo toSessionInfo(UserAccountEntity user) {
        return new ApiDtos.SessionInfo(user.getId(), user.getUsername(), user.getRole(), user.getRealName(), user.getCompanyName());
    }

    private UserAccountEntity findUserByUsername(String username) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getUsername, username)
                .last("limit 1"));
    }

    private UserAccountEntity findUserByPhone(String phone) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getPhone, phone)
                .last("limit 1"));
    }

    private UserAccountEntity getUserByUsername(String username) {
        UserAccountEntity user = findUserByUsername(username);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    void audit(Long actorUserId, String actorRole, String bizType, String bizId, String eventType, String payload) {
        auditLogService.record(
                RequestIdContext.get(),
                actorUserId,
                actorRole,
                bizType,
                bizId,
                eventType,
                payload);
    }
}

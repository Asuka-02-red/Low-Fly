package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.security.TokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class AuthServiceTest {

    private AuthService authService;
    private TokenService tokenService;
    private UserAccountMapper userAccountMapper;
    private RefreshTokenStore refreshTokenStore;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        tokenService = Mockito.mock(TokenService.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        authService = new AuthService(tokenService, userAccountMapper, refreshTokenStore, auditLogService);
    }

    private UserAccountEntity buildUser(Long id, String username, String role) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("{noop}demo123");
        user.setPhone("13800138000");
        user.setRole(role);
        user.setRealName("测试用户");
        user.setCompanyName("测试企业");
        return user;
    }

    @Test
    void shouldLoginWithValidCredentials() {
        UserAccountEntity user = buildUser(1L, "pilot_demo", "PILOT");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(user);
        Mockito.when(refreshTokenStore.issueToken(user)).thenReturn("refresh-token");
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt-token");

        ApiDtos.AuthPayload payload = authService.login(new ApiDtos.LoginRequest("pilot_demo", "demo123"));

        Assertions.assertEquals("jwt-token", payload.token());
        Assertions.assertEquals("refresh-token", payload.refreshToken());
        Assertions.assertEquals("PILOT", payload.userInfo().role());
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
        UserAccountEntity user = buildUser(1L, "pilot_demo", "PILOT");
        user.setPasswordHash("{noop}wrong");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(user);

        Assertions.assertThrows(BizException.class, () -> authService.login(new ApiDtos.LoginRequest("pilot_demo", "demo123")));
    }

    @Test
    void shouldRejectLoginWithNonExistentUser() {
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> authService.login(new ApiDtos.LoginRequest("nonexistent", "demo123")));
    }

    @Test
    void shouldRegisterNewUser() {
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.when(refreshTokenStore.issueToken(ArgumentMatchers.any())).thenReturn("refresh-token");
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt-token");

        ApiDtos.AuthPayload payload = authService.register(new ApiDtos.RegisterRequest("newuser", "password123", "13900139000", "PILOT", "张三", "测试公司"));

        Assertions.assertEquals("jwt-token", payload.token());
        Assertions.assertEquals("PILOT", payload.userInfo().role());
    }

    @Test
    void shouldRegisterWithBlankRealName() {
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.when(refreshTokenStore.issueToken(ArgumentMatchers.any())).thenReturn("refresh-token");
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt-token");

        ApiDtos.AuthPayload payload = authService.register(new ApiDtos.RegisterRequest("newuser", "password123", "13900139000", "PILOT", "", null));

        Assertions.assertEquals("newuser", payload.userInfo().realName());
    }

    @Test
    void shouldRejectRegisterWithExistingUsername() {
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(buildUser(1L, "existing", "PILOT"));

        Assertions.assertThrows(BizException.class, () -> authService.register(new ApiDtos.RegisterRequest("existing", "password123", "13900139000", "PILOT", "张三", null)));
    }

    @Test
    void shouldRejectRegisterWithExistingPhone() {
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccountEntity> query = invocation.getArgument(0);
            return buildUser(1L, "other_user", "PILOT");
        });

        Assertions.assertThrows(BizException.class, () -> authService.register(new ApiDtos.RegisterRequest("newuser", "password123", "13800138000", "PILOT", "张三", null)));
    }

    @Test
    void shouldRefreshToken() {
        UserAccountEntity user = buildUser(1L, "pilot_demo", "PILOT");
        Mockito.when(refreshTokenStore.requireUsernameByRefreshToken("refresh-token")).thenReturn("pilot_demo");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(user);
        Mockito.when(tokenService.createToken(ArgumentMatchers.any())).thenReturn("jwt-refresh");

        ApiDtos.AuthPayload payload = authService.refresh(new ApiDtos.RefreshTokenRequest("refresh-token"));

        Assertions.assertEquals("jwt-refresh", payload.token());
        Assertions.assertEquals("refresh-token", payload.refreshToken());
    }

    @Test
    void shouldGetCurrentUser() {
        UserAccountEntity user = buildUser(1L, "pilot_demo", "PILOT");
        SessionUser sessionUser = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "测试用户");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(user);

        ApiDtos.SessionInfo info = authService.currentUser(sessionUser);

        Assertions.assertEquals("pilot_demo", info.username());
        Assertions.assertEquals("PILOT", info.role());
    }
}

package com.example.low_altitudereststop.core.model;

/**
 * 认证相关数据模型集合，包含登录、注册、令牌刷新等请求和响应的数据结构。
 */
public final class AuthModels {

    private AuthModels() {
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class RegisterRequest {
        public String username;
        public String password;
        public String phone;
        public String role;
        public String realName;
        public String companyName;
    }

    public static class RefreshTokenRequest {
        public String refreshToken;
    }

    public static class SessionInfo {
        public Long id;
        public String username;
        public String role;
        public String realName;
        public String companyName;
    }

    public static class AuthPayload {
        public String token;
        public String refreshToken;
        public SessionInfo userInfo;
    }
}


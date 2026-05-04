package com.lowaltitude.reststop.server.security;

/**
 * 会话用户信息记录。
 * <p>
 * 存储从JWT令牌解析出的当前登录用户信息，
 * 包括用户ID、用户名、角色及显示名称，作为Spring Security的认证主体。
 * </p>
 *
 * @param id          用户ID
 * @param username    用户名
 * @param role        用户角色
 * @param displayName 显示名称
 */
public record SessionUser(Long id, String username, RoleType role, String displayName) {
}

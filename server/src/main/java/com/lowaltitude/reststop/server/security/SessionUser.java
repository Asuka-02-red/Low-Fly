package com.lowaltitude.reststop.server.security;

public record SessionUser(Long id, String username, RoleType role, String displayName) {
}

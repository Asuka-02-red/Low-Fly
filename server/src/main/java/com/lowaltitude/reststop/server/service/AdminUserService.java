package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.util.List;
import java.util.Objects;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserAccountMapper userAccountMapper;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public AdminUserService(
            UserAccountMapper userAccountMapper,
            AuditLogService auditLogService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.auditLogService = auditLogService;
    }

    public List<ApiDtos.AuditEventView> listAuditEvents() {
        return auditLogService.listRecent();
    }

    public List<ApiDtos.AdminUserView> listAdminUsers() {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountEntity>()
                        .orderByDesc(UserAccountEntity::getCreateTime)
                        .orderByDesc(UserAccountEntity::getId))
                .stream()
                .map(this::toAdminUserView)
                .toList();
    }

    @Transactional
    public ApiDtos.AdminUserView createAdminUser(SessionUser admin, ApiDtos.AdminUserCreateRequest request) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        validateUserUniqueness(request.username(), request.phone(), PlatformUtils.normalizeNullable(request.email()), null);

        RoleType role = PlatformUtils.parseRole(request.role());
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone().trim());
        user.setEmail(PlatformUtils.normalizeNullable(request.email()));
        user.setRole(role.name());
        user.setRealName(request.realName().trim());
        user.setCompanyName(PlatformUtils.normalizeNullable(request.companyName()));
        user.setStatus(request.status());
        user.setVersion(0);
        userAccountMapper.insert(user);
        audit(admin, "ADMIN_USER", String.valueOf(user.getId()), "CREATE", "username=" + user.getUsername());
        return toAdminUserView(getUserById(user.getId()));
    }

    @Transactional
    public ApiDtos.AdminUserView updateAdminUser(SessionUser admin, Long userId, ApiDtos.AdminUserUpdateRequest request) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        UserAccountEntity user = getUserById(userId);
        validateAdminOperationTarget(user);
        validateUserUniqueness(request.username(), request.phone(), PlatformUtils.normalizeNullable(request.email()), userId);

        RoleType role = PlatformUtils.parseRole(request.role());
        user.setUsername(request.username().trim());
        if (!PlatformUtils.isBlank(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }
        user.setPhone(request.phone().trim());
        user.setEmail(PlatformUtils.normalizeNullable(request.email()));
        user.setRole(role.name());
        user.setRealName(request.realName().trim());
        user.setCompanyName(PlatformUtils.normalizeNullable(request.companyName()));
        user.setStatus(request.status());
        userAccountMapper.updateById(user);
        audit(admin, "ADMIN_USER", String.valueOf(user.getId()), "UPDATE", "username=" + user.getUsername());
        return toAdminUserView(getUserById(userId));
    }

    @Transactional
    public void deleteAdminUser(SessionUser admin, Long userId) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        UserAccountEntity user = getUserById(userId);
        validateAdminOperationTarget(user);
        userAccountMapper.deleteById(userId);
        audit(admin, "ADMIN_USER", String.valueOf(userId), "DELETE", "username=" + user.getUsername());
    }

    private UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private void validateUserUniqueness(String username, String phone, String email, Long excludeUserId) {
        String normalizedUsername = username.trim();
        String normalizedPhone = phone.trim();
        String normalizedEmail = PlatformUtils.normalizeNullable(email);

        UserAccountEntity sameUsername = findUserByUsername(normalizedUsername);
        if (sameUsername != null && !Objects.equals(sameUsername.getId(), excludeUserId)) {
            throw new BizException(400, "用户名已存在");
        }

        UserAccountEntity samePhone = findUserByPhone(normalizedPhone);
        if (samePhone != null && !Objects.equals(samePhone.getId(), excludeUserId)) {
            throw new BizException(400, "手机号已存在");
        }

        if (!PlatformUtils.isBlank(normalizedEmail)) {
            UserAccountEntity sameEmail = findUserByEmail(normalizedEmail);
            if (sameEmail != null && !Objects.equals(sameEmail.getId(), excludeUserId)) {
                throw new BizException(400, "邮箱已存在");
            }
        }
    }

    private void validateAdminOperationTarget(UserAccountEntity user) {
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BizException(400, "默认管理员账号不允许修改或删除");
        }
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

    private UserAccountEntity findUserByEmail(String email) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getEmail, email)
                .last("limit 1"));
    }

    private ApiDtos.AdminUserView toAdminUserView(UserAccountEntity user) {
        return new ApiDtos.AdminUserView(
                String.valueOf(user.getId()),
                user.getUsername(),
                PlatformUtils.defaultIfBlank(user.getEmail(), "未配置"),
                PlatformUtils.displayName(user),
                PlatformUtils.defaultIfBlank(user.getCompanyName(), "低空驿站运营中心"),
                PlatformUtils.defaultIfBlank(user.getPhone(), "未配置"),
                PlatformUtils.mapAdminUserStatus(user.getStatus()),
                PlatformUtils.formatDateTime(user.getCreateTime()),
                List.of(PlatformUtils.displayRole(user.getRole())),
                PlatformUtils.permissionGroupName(user.getRole()),
                PlatformUtils.defaultIfBlank(user.getRole(), "未定义"));
    }

    void audit(SessionUser actor, String bizType, String bizId, String eventType, String payload) {
        Long actorUserId = actor == null ? null : actor.id();
        String actorRole = actor == null ? null : actor.role().name();
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

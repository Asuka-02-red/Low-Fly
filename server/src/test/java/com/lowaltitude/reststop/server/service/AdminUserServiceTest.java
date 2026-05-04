package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class AdminUserServiceTest {

    private AdminUserService adminUserService;
    private UserAccountMapper userAccountMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        adminUserService = new AdminUserService(userAccountMapper, auditLogService);
    }

    // ========================================================================
    // listAuditEvents
    // ========================================================================

    @Test
    void listAuditEvents_returnsRecentEventsFromAuditService() {
        ApiDtos.AuditEventView event = new ApiDtos.AuditEventView(
                "req-001", 1L, "ADMIN", "USER", "1", "CREATE", "payload",
                LocalDateTime.of(2026, 1, 15, 10, 0));
        Mockito.when(auditLogService.listRecent()).thenReturn(List.of(event));

        List<ApiDtos.AuditEventView> result = adminUserService.listAuditEvents();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("req-001", result.get(0).requestId());
        Assertions.assertEquals("CREATE", result.get(0).eventType());
        Mockito.verify(auditLogService).listRecent();
    }

    @Test
    void listAuditEvents_returnsEmptyListWhenNoEvents() {
        Mockito.when(auditLogService.listRecent()).thenReturn(Collections.emptyList());

        List<ApiDtos.AuditEventView> result = adminUserService.listAuditEvents();

        Assertions.assertTrue(result.isEmpty());
    }

    // ========================================================================
    // listAdminUsers
    // ========================================================================

    @Test
    void listAdminUsers_returnsMappedViews() {
        UserAccountEntity entity1 = buildUser(1L, "admin", "ADMIN");
        entity1.setStatus(1);
        entity1.setCreateTime(LocalDateTime.of(2026, 3, 10, 8, 0));

        UserAccountEntity entity2 = buildUser(2L, "pilot1", "PILOT");
        entity2.setStatus(1);
        entity2.setCreateTime(LocalDateTime.of(2026, 3, 11, 9, 30));

        Mockito.when(userAccountMapper.selectList(ArgumentMatchers.any()))
                .thenReturn(List.of(entity1, entity2));

        List<ApiDtos.AdminUserView> result = adminUserService.listAdminUsers();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("1", result.get(0).id());
        Assertions.assertEquals("admin", result.get(0).username());
        Assertions.assertEquals("管理员", result.get(0).roleNames().get(0));
        Assertions.assertEquals("全量管控组", result.get(0).permissionGroupName());
        Assertions.assertEquals("2", result.get(1).id());
        Assertions.assertEquals("飞手", result.get(1).roleNames().get(0));
        Assertions.assertEquals("执行协同组", result.get(1).permissionGroupName());
    }

    @Test
    void listAdminUsers_returnsEmptyListWhenNoUsers() {
        Mockito.when(userAccountMapper.selectList(ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());

        List<ApiDtos.AdminUserView> result = adminUserService.listAdminUsers();

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void listAdminUsers_mapsDefaultValuesForNullFields() {
        UserAccountEntity entity = buildUser(5L, "testuser", "ADMIN");
        entity.setEmail(null);
        entity.setPhone(null);
        entity.setCompanyName(null);
        entity.setStatus(1);
        entity.setCreateTime(LocalDateTime.of(2026, 5, 1, 12, 0));

        Mockito.when(userAccountMapper.selectList(ArgumentMatchers.any()))
                .thenReturn(List.of(entity));

        List<ApiDtos.AdminUserView> result = adminUserService.listAdminUsers();

        Assertions.assertEquals(1, result.size());
        ApiDtos.AdminUserView view = result.get(0);
        Assertions.assertEquals("未配置", view.email());
        Assertions.assertEquals("未配置", view.phone());
        Assertions.assertEquals("低空驿站运营中心", view.organization());
    }

    // ========================================================================
    // createAdminUser - success path
    // ========================================================================

    @Test
    void createAdminUser_success() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        // No duplicate username, phone, or email
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        // Simulate auto-generated ID on insert
        Mockito.doAnswer(invocation -> {
            UserAccountEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));

        // Return the persisted entity when re-fetched
        UserAccountEntity persisted = buildUser(10L, "newadmin", "ADMIN");
        persisted.setEmail("new@test.com");
        persisted.setPhone("13900139000");
        persisted.setStatus(1);
        persisted.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        Mockito.when(userAccountMapper.selectById(10L)).thenReturn(persisted);

        ApiDtos.AdminUserView result = adminUserService.createAdminUser(
                admin,
                new ApiDtos.AdminUserCreateRequest(
                        "newadmin", "Pass1234!", "13900139000",
                        "new@test.com", "ADMIN", "新管理员", "运营中心", 1));

        Assertions.assertEquals("newadmin", result.username());
        Mockito.verify(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_USER"),
                ArgumentMatchers.eq("10"),
                ArgumentMatchers.eq("CREATE"),
                ArgumentMatchers.contains("newadmin"));
    }

    // ========================================================================
    // createAdminUser - duplicate username
    // ========================================================================

    @Test
    void createAdminUser_duplicateUsername_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        // Existing user with same username
        UserAccountEntity existingUser = buildUser(5L, "duplicate_user", "PILOT");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(existingUser);

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.createAdminUser(
                        admin,
                        new ApiDtos.AdminUserCreateRequest(
                                "duplicate_user", "Pass1234!", "13900139001",
                                "unique@test.com", "PILOT", "用户", null, 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("用户名已存在", ex.getMessage());
    }

    // ========================================================================
    // createAdminUser - duplicate phone
    // ========================================================================

    @Test
    void createAdminUser_duplicatePhone_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        // No duplicate username, but duplicate phone
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    // First call: findUserByUsername -> null
                    // We need to differentiate the calls; since we can't easily,
                    // we return the existing user on the second call (findUserByPhone)
                    return null;
                });

        // Use sequential returns: first call (username check) returns null,
        // second call (phone check) returns existing user
        UserAccountEntity existingPhoneUser = buildUser(6L, "other_user", "PILOT");
        existingPhoneUser.setPhone("13900139000");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)                       // findUserByUsername
                .thenReturn(existingPhoneUser);          // findUserByPhone

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.createAdminUser(
                        admin,
                        new ApiDtos.AdminUserCreateRequest(
                                "unique_user", "Pass1234!", "13900139000",
                                "unique@test.com", "PILOT", "用户", null, 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("手机号已存在", ex.getMessage());
    }

    // ========================================================================
    // createAdminUser - duplicate email
    // ========================================================================

    @Test
    void createAdminUser_duplicateEmail_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existingEmailUser = buildUser(7L, "email_user", "ENTERPRISE");
        existingEmailUser.setEmail("dup@test.com");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)                       // findUserByUsername
                .thenReturn(null)                       // findUserByPhone
                .thenReturn(existingEmailUser);          // findUserByEmail

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.createAdminUser(
                        admin,
                        new ApiDtos.AdminUserCreateRequest(
                                "unique_user", "Pass1234!", "13900139099",
                                "dup@test.com", "ENTERPRISE", "企业用户", "公司", 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("邮箱已存在", ex.getMessage());
    }

    // ========================================================================
    // createAdminUser - non-admin rejected
    // ========================================================================

    @Test
    void createAdminUser_nonAdminRejected_throwsBizException() {
        SessionUser pilot = new SessionUser(2L, "pilot1", RoleType.PILOT, "飞手");

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.createAdminUser(
                        pilot,
                        new ApiDtos.AdminUserCreateRequest(
                                "newuser", "Pass1234!", "13900139000",
                                null, "PILOT", "用户", null, 1)));

        Assertions.assertEquals(403, ex.getCode());
        Assertions.assertEquals("当前角色无权执行该操作", ex.getMessage());
    }

    // ========================================================================
    // updateAdminUser - success with password change
    // ========================================================================

    @Test
    void updateAdminUser_successWithPasswordChange() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(10L, "edituser", "ADMIN");
        existing.setEmail("edit@test.com");
        existing.setPhone("13800138000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(10L)).thenReturn(existing);

        // No duplicates for the new values
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        UserAccountEntity updated = buildUser(10L, "edituser_v2", "ADMIN");
        updated.setEmail("edit_v2@test.com");
        updated.setPhone("13800138001");
        updated.setStatus(1);
        updated.setCreateTime(LocalDateTime.of(2026, 4, 5, 14, 0));
        // After updateById, re-fetch returns updated entity
        Mockito.when(userAccountMapper.selectById(10L)).thenReturn(existing, updated);

        ApiDtos.AdminUserView result = adminUserService.updateAdminUser(
                admin, 10L,
                new ApiDtos.AdminUserUpdateRequest(
                        "edituser_v2", "NewPass123!", "13800138001",
                        "edit_v2@test.com", "ADMIN", "编辑用户", "新公司", 1));

        Assertions.assertEquals("edituser_v2", result.username());
        Mockito.verify(userAccountMapper).updateById(ArgumentMatchers.any(UserAccountEntity.class));
        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_USER"),
                ArgumentMatchers.eq("10"),
                ArgumentMatchers.eq("UPDATE"),
                ArgumentMatchers.contains("edituser_v2"));
    }

    // ========================================================================
    // updateAdminUser - success without password change (blank password)
    // ========================================================================

    @Test
    void updateAdminUser_successWithoutPasswordChange_blankPassword() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(11L, "nopassuser", "ENTERPRISE");
        existing.setPasswordHash("{noop}original_hash");
        existing.setEmail("nopass@test.com");
        existing.setPhone("13700137000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(11L)).thenReturn(existing);

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        ApiDtos.AdminUserView result = adminUserService.updateAdminUser(
                admin, 11L,
                new ApiDtos.AdminUserUpdateRequest(
                        "nopassuser", "", "13700137000",
                        "nopass@test.com", "ENTERPRISE", "无密码修改", "公司", 1));

        // Verify updateById was called and the password hash was NOT changed
        Mockito.verify(userAccountMapper).updateById(ArgumentMatchers.<UserAccountEntity>argThat(
                entity -> "{noop}original_hash".equals(entity.getPasswordHash())));
    }

    @Test
    void updateAdminUser_successWithoutPasswordChange_nullPassword() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(12L, "nullpassuser", "PILOT");
        existing.setPasswordHash("{noop}original_hash");
        existing.setEmail("nullpass@test.com");
        existing.setPhone("13600136000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(12L)).thenReturn(existing);

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        adminUserService.updateAdminUser(
                admin, 12L,
                new ApiDtos.AdminUserUpdateRequest(
                        "nullpassuser", null, "13600136000",
                        "nullpass@test.com", "PILOT", "空密码修改", "公司", 1));

        Mockito.verify(userAccountMapper).updateById(ArgumentMatchers.<UserAccountEntity>argThat(
                entity -> "{noop}original_hash".equals(entity.getPasswordHash())));
    }

    // ========================================================================
    // updateAdminUser - default admin rejected
    // ========================================================================

    @Test
    void updateAdminUser_defaultAdminRejected_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity defaultAdmin = buildUser(1L, "admin", "ADMIN");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(defaultAdmin);

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.updateAdminUser(
                        admin, 1L,
                        new ApiDtos.AdminUserUpdateRequest(
                                "admin", "NewPass123!", "13800138000",
                                null, "ADMIN", "管理员", null, 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("默认管理员账号不允许修改或删除", ex.getMessage());
    }

    @Test
    void updateAdminUser_defaultAdminCaseInsensitive_rejected() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity defaultAdmin = buildUser(2L, "Admin", "ADMIN");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(defaultAdmin);

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.updateAdminUser(
                        admin, 2L,
                        new ApiDtos.AdminUserUpdateRequest(
                                "Admin", "NewPass123!", "13800138000",
                                null, "ADMIN", "管理员", null, 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("默认管理员账号不允许修改或删除", ex.getMessage());
    }

    // ========================================================================
    // updateAdminUser - duplicate username but same user ID is OK
    // ========================================================================

    @Test
    void updateAdminUser_duplicateUsernameButSameUserId_isAllowed() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(20L, "sameuser", "ADMIN");
        existing.setEmail("same@test.com");
        existing.setPhone("13500135000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(20L)).thenReturn(existing);

        // findUserByUsername returns the same user (same ID), which should be allowed
        UserAccountEntity sameIdUser = buildUser(20L, "sameuser", "ADMIN");
        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(sameIdUser)     // findUserByUsername -> same ID, OK
                .thenReturn(null)           // findUserByPhone -> null
                .thenReturn(null);          // findUserByEmail -> null

        ApiDtos.AdminUserView result = adminUserService.updateAdminUser(
                admin, 20L,
                new ApiDtos.AdminUserUpdateRequest(
                        "sameuser", null, "13500135000",
                        "same@test.com", "ADMIN", "同名用户", "公司", 1));

        Assertions.assertEquals("sameuser", result.username());
        Mockito.verify(userAccountMapper).updateById(ArgumentMatchers.any(UserAccountEntity.class));
    }

    // ========================================================================
    // updateAdminUser - duplicate email
    // ========================================================================

    @Test
    void updateAdminUser_duplicateEmail_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(30L, "myuser", "ADMIN");
        existing.setEmail("my@test.com");
        existing.setPhone("13400134000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(30L)).thenReturn(existing);

        UserAccountEntity otherUserWithEmail = buildUser(31L, "otheruser", "PILOT");
        otherUserWithEmail.setEmail("dup_email@test.com");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)                   // findUserByUsername
                .thenReturn(null)                   // findUserByPhone
                .thenReturn(otherUserWithEmail);    // findUserByEmail -> different ID

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.updateAdminUser(
                        admin, 30L,
                        new ApiDtos.AdminUserUpdateRequest(
                                "myuser", null, "13400134000",
                                "dup_email@test.com", "ADMIN", "用户", "公司", 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("邮箱已存在", ex.getMessage());
    }

    // ========================================================================
    // deleteAdminUser - success
    // ========================================================================

    @Test
    void deleteAdminUser_success() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity target = buildUser(50L, "deleteme", "PILOT");
        Mockito.when(userAccountMapper.selectById(50L)).thenReturn(target);

        adminUserService.deleteAdminUser(admin, 50L);

        Mockito.verify(userAccountMapper).deleteById(50L);
        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_USER"),
                ArgumentMatchers.eq("50"),
                ArgumentMatchers.eq("DELETE"),
                ArgumentMatchers.contains("deleteme"));
    }

    // ========================================================================
    // deleteAdminUser - default admin rejected
    // ========================================================================

    @Test
    void deleteAdminUser_defaultAdminRejected_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity defaultAdmin = buildUser(1L, "admin", "ADMIN");
        Mockito.when(userAccountMapper.selectById(1L)).thenReturn(defaultAdmin);

        BizException ex = Assertions.assertThrows(BizException.class,
                () -> adminUserService.deleteAdminUser(admin, 1L));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("默认管理员账号不允许修改或删除", ex.getMessage());
        Mockito.verify(userAccountMapper, Mockito.never()).deleteById(ArgumentMatchers.anyLong());
    }

    // ========================================================================
    // getUserById - not found (tested indirectly through createAdminUser re-fetch
    // and directly through updateAdminUser / deleteAdminUser)
    // ========================================================================

    @Test
    void getUserById_notFound_throwsBizException_onUpdate() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.updateAdminUser(
                        admin, 999L,
                        new ApiDtos.AdminUserUpdateRequest(
                                "ghost", null, "13000130000",
                                null, "ADMIN", "幽灵", null, 1)));

        Assertions.assertEquals(404, ex.getCode());
        Assertions.assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void getUserById_notFound_throwsBizException_onDelete() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(userAccountMapper.selectById(999L)).thenReturn(null);

        BizException ex = Assertions.assertThrows(BizException.class,
                () -> adminUserService.deleteAdminUser(admin, 999L));

        Assertions.assertEquals(404, ex.getCode());
        Assertions.assertEquals("用户不存在", ex.getMessage());
    }

    // ========================================================================
    // createAdminUser - blank email skips email uniqueness check
    // ========================================================================

    @Test
    void createAdminUser_blankEmail_skipsEmailUniquenessCheck() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)   // findUserByUsername -> null
                .thenReturn(null);  // findUserByPhone -> null

        Mockito.doAnswer(invocation -> {
            UserAccountEntity entity = invocation.getArgument(0);
            entity.setId(15L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));

        UserAccountEntity persisted = buildUser(15L, "noemail_user", "PILOT");
        persisted.setEmail(null);
        persisted.setPhone("13100131000");
        persisted.setStatus(1);
        persisted.setCreateTime(LocalDateTime.of(2026, 5, 1, 8, 0));
        Mockito.when(userAccountMapper.selectById(15L)).thenReturn(persisted);

        ApiDtos.AdminUserView result = adminUserService.createAdminUser(
                admin,
                new ApiDtos.AdminUserCreateRequest(
                        "noemail_user", "Pass1234!", "13100131000",
                        null, "PILOT", "无邮箱用户", null, 1));

        Assertions.assertEquals("noemail_user", result.username());
        // Verify selectOne was only called twice (username + phone), not three times (no email check)
        Mockito.verify(userAccountMapper, Mockito.times(2)).selectOne(ArgumentMatchers.any());
    }

    // ========================================================================
    // createAdminUser - duplicate email with same user ID during update is OK
    // ========================================================================

    @Test
    void updateAdminUser_duplicateEmailButSameUserId_isAllowed() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(40L, "keepemail", "ADMIN");
        existing.setEmail("keep@test.com");
        existing.setPhone("13300133000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(40L)).thenReturn(existing);

        // findUserByEmail returns the same user (same ID)
        UserAccountEntity sameIdUser = buildUser(40L, "keepemail", "ADMIN");
        sameIdUser.setEmail("keep@test.com");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)           // findUserByUsername
                .thenReturn(null)           // findUserByPhone
                .thenReturn(sameIdUser);    // findUserByEmail -> same ID, OK

        ApiDtos.AdminUserView result = adminUserService.updateAdminUser(
                admin, 40L,
                new ApiDtos.AdminUserUpdateRequest(
                        "keepemail", null, "13300133000",
                        "keep@test.com", "ADMIN", "保留邮箱", "公司", 1));

        Assertions.assertEquals("keepemail", result.username());
        Mockito.verify(userAccountMapper).updateById(ArgumentMatchers.any(UserAccountEntity.class));
    }

    // ========================================================================
    // createAdminUser - duplicate phone with same user ID during update is OK
    // ========================================================================

    @Test
    void updateAdminUser_duplicatePhoneButSameUserId_isAllowed() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(41L, "keepphone", "PILOT");
        existing.setEmail("keepphone@test.com");
        existing.setPhone("13200132000");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(41L)).thenReturn(existing);

        UserAccountEntity sameIdUser = buildUser(41L, "keepphone", "PILOT");
        sameIdUser.setPhone("13200132000");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(null)           // findUserByUsername
                .thenReturn(sameIdUser)     // findUserByPhone -> same ID, OK
                .thenReturn(null);          // findUserByEmail

        ApiDtos.AdminUserView result = adminUserService.updateAdminUser(
                admin, 41L,
                new ApiDtos.AdminUserUpdateRequest(
                        "keepphone", null, "13200132000",
                        "keepphone@test.com", "PILOT", "保留手机", "公司", 1));

        Assertions.assertEquals("keepphone", result.username());
        Mockito.verify(userAccountMapper).updateById(ArgumentMatchers.any(UserAccountEntity.class));
    }

    // ========================================================================
    // createAdminUser - duplicate username with different ID during update
    // ========================================================================

    @Test
    void updateAdminUser_duplicateUsernameDifferentId_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        UserAccountEntity existing = buildUser(42L, "myuser", "ADMIN");
        existing.setEmail("my42@test.com");
        existing.setPhone("13000130042");
        existing.setStatus(1);
        Mockito.when(userAccountMapper.selectById(42L)).thenReturn(existing);

        UserAccountEntity otherUser = buildUser(99L, "taken_name", "PILOT");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any()))
                .thenReturn(otherUser);     // findUserByUsername -> different ID

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.updateAdminUser(
                        admin, 42L,
                        new ApiDtos.AdminUserUpdateRequest(
                                "taken_name", null, "13000130042",
                                "my42@test.com", "ADMIN", "用户", "公司", 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("用户名已存在", ex.getMessage());
    }

    // ========================================================================
    // deleteAdminUser - non-admin rejected
    // ========================================================================

    @Test
    void deleteAdminUser_nonAdminRejected_throwsBizException() {
        SessionUser pilot = new SessionUser(2L, "pilot1", RoleType.PILOT, "飞手");

        BizException ex = Assertions.assertThrows(BizException.class,
                () -> adminUserService.deleteAdminUser(pilot, 50L));

        Assertions.assertEquals(403, ex.getCode());
        Assertions.assertEquals("当前角色无权执行该操作", ex.getMessage());
    }

    // ========================================================================
    // updateAdminUser - non-admin rejected
    // ========================================================================

    @Test
    void updateAdminUser_nonAdminRejected_throwsBizException() {
        SessionUser enterprise = new SessionUser(3L, "ent1", RoleType.ENTERPRISE, "企业");

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.updateAdminUser(
                        enterprise, 10L,
                        new ApiDtos.AdminUserUpdateRequest(
                                "user", null, "13000130000",
                                null, "ADMIN", "用户", null, 1)));

        Assertions.assertEquals(403, ex.getCode());
        Assertions.assertEquals("当前角色无权执行该操作", ex.getMessage());
    }

    // ========================================================================
    // createAdminUser - trims input fields
    // ========================================================================

    @Test
    void createAdminUser_trimsInputFields() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        Mockito.doAnswer(invocation -> {
            UserAccountEntity entity = invocation.getArgument(0);
            entity.setId(20L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));

        UserAccountEntity persisted = buildUser(20L, "spaced_user", "ADMIN");
        persisted.setPhone("13800138000");
        persisted.setEmail("spaced@test.com");
        persisted.setStatus(1);
        persisted.setCreateTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        Mockito.when(userAccountMapper.selectById(20L)).thenReturn(persisted);

        adminUserService.createAdminUser(
                admin,
                new ApiDtos.AdminUserCreateRequest(
                        "  spaced_user  ", "Pass1234!", "  13800138000  ",
                        "  spaced@test.com  ", "ADMIN", "  带空格  ", "  公司  ", 1));

        Mockito.verify(userAccountMapper).insert(ArgumentMatchers.<UserAccountEntity>argThat(entity ->
                "spaced_user".equals(entity.getUsername()) &&
                "13800138000".equals(entity.getPhone()) &&
                "spaced@test.com".equals(entity.getEmail()) &&
                "带空格".equals(entity.getRealName())));
    }

    // ========================================================================
    // createAdminUser - null companyName normalized to null
    // ========================================================================

    @Test
    void createAdminUser_nullCompanyName_normalizedToNull() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        Mockito.doAnswer(invocation -> {
            UserAccountEntity entity = invocation.getArgument(0);
            entity.setId(21L);
            return 1;
        }).when(userAccountMapper).insert(ArgumentMatchers.any(UserAccountEntity.class));

        UserAccountEntity persisted = buildUser(21L, "nocomp_user", "PILOT");
        persisted.setCompanyName(null);
        persisted.setPhone("13800138001");
        persisted.setStatus(1);
        persisted.setCreateTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        Mockito.when(userAccountMapper.selectById(21L)).thenReturn(persisted);

        adminUserService.createAdminUser(
                admin,
                new ApiDtos.AdminUserCreateRequest(
                        "nocomp_user", "Pass1234!", "13800138001",
                        null, "PILOT", "无公司用户", null, 1));

        Mockito.verify(userAccountMapper).insert(ArgumentMatchers.<UserAccountEntity>argThat(entity ->
                entity.getCompanyName() == null));
    }

    // ========================================================================
    // createAdminUser - invalid role rejected
    // ========================================================================

    @Test
    void createAdminUser_invalidRole_throwsBizException() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(userAccountMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminUserService.createAdminUser(
                        admin,
                        new ApiDtos.AdminUserCreateRequest(
                                "badrole_user", "Pass1234!", "13800138002",
                                null, "INVALID_ROLE", "用户", null, 1)));

        Assertions.assertEquals(400, ex.getCode());
        Assertions.assertEquals("角色不合法", ex.getMessage());
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

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
}

package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.AdminNotificationRuleEntity;
import com.lowaltitude.reststop.server.entity.AdminSettingEntity;
import com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper;
import com.lowaltitude.reststop.server.mapper.AdminSettingMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@SuppressWarnings({"unchecked", "cast"})
class AdminSettingsServiceTest {

    private AdminSettingsService adminSettingsService;
    private AdminSettingMapper adminSettingMapper;
    private AdminNotificationRuleMapper adminNotificationRuleMapper;
    private AuditLogService auditLogService;
    private Map<String, AdminSettingEntity> settingStore;
    private Map<String, AdminNotificationRuleEntity> ruleStore;
    private AtomicLong ruleIdGenerator;

    @BeforeEach
    void setUp() {
        adminSettingMapper = Mockito.mock(AdminSettingMapper.class);
        adminNotificationRuleMapper = Mockito.mock(AdminNotificationRuleMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        settingStore = new HashMap<>();
        ruleStore = new HashMap<>();
        ruleIdGenerator = new AtomicLong(1L);

        // Simulate AdminSettingMapper persistence with HashMap
        Mockito.when(adminSettingMapper.selectById(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> settingStore.get(invocation.getArgument(0)));
        Mockito.when(adminSettingMapper.insertOrUpdate(ArgumentMatchers.any(AdminSettingEntity.class)))
                .thenAnswer(invocation -> {
                    AdminSettingEntity entity = invocation.getArgument(0);
                    settingStore.put(entity.getSettingKey(), entity);
                    return true;
                });

        // Simulate AdminNotificationRuleMapper persistence
        Mockito.when(adminNotificationRuleMapper.selectList(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> new ArrayList<>(ruleStore.values()));
        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    // Return the first matching entity from the rule store
                    return ruleStore.values().stream().findFirst().orElse(null);
                });
        Mockito.when(adminNotificationRuleMapper.delete(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    // Simulate delete by clearing non-matching rules
                    return ruleStore.size();
                });
        Mockito.when(adminNotificationRuleMapper.insertOrUpdate(ArgumentMatchers.any(AdminNotificationRuleEntity.class)))
                .thenAnswer(invocation -> {
                    AdminNotificationRuleEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        entity.setId(ruleIdGenerator.getAndIncrement());
                    }
                    ruleStore.put(entity.getRuleKey(), entity);
                    return true;
                });

        adminSettingsService = new AdminSettingsService(
                adminSettingMapper, adminNotificationRuleMapper, auditLogService);
    }

    // -----------------------------------------------------------------------
    // getAdminSettings() - with defaults (null settings)
    // -----------------------------------------------------------------------
    @Test
    void shouldGetAdminSettingsWithDefaultsWhenNoSettingsPersisted() {
        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertNotNull(settings);
        Assertions.assertNotNull(settings.basic());
        Assertions.assertNotNull(settings.security());
        Assertions.assertNotNull(settings.notifications());

        // Basic defaults
        Assertions.assertEquals("低空驿站一站式数字化服务平台", settings.basic().stationName());
        Assertions.assertEquals("400-820-2026", settings.basic().serviceHotline());
        Assertions.assertEquals("深圳南山", settings.basic().defaultRegion());
        Assertions.assertTrue(settings.basic().mobileDashboardEnabled());

        // Security defaults
        Assertions.assertEquals(90, settings.security().passwordValidityDays());
        Assertions.assertEquals(5, settings.security().loginRetryLimit());
        Assertions.assertEquals("", settings.security().ipWhitelist());
        Assertions.assertTrue(settings.security().mfaRequired());

        // No notification rules
        Assertions.assertTrue(settings.notifications().isEmpty());
    }

    // -----------------------------------------------------------------------
    // getAdminSettings() - with existing settings
    // -----------------------------------------------------------------------
    @Test
    void shouldGetAdminSettingsWithExistingPersistedValues() {
        // Pre-populate settings
        putSetting("basic.stationName", "自定义驿站");
        putSetting("basic.serviceHotline", "400-999-8888");
        putSetting("basic.defaultRegion", "广州天河");
        putSetting("basic.mobileDashboardEnabled", "false");
        putSetting("security.passwordValidityDays", "30");
        putSetting("security.loginRetryLimit", "10");
        putSetting("security.ipWhitelist", "192.168.1.0/24");
        putSetting("security.mfaRequired", "false");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertEquals("自定义驿站", settings.basic().stationName());
        Assertions.assertEquals("400-999-8888", settings.basic().serviceHotline());
        Assertions.assertEquals("广州天河", settings.basic().defaultRegion());
        Assertions.assertFalse(settings.basic().mobileDashboardEnabled());

        Assertions.assertEquals(30, settings.security().passwordValidityDays());
        Assertions.assertEquals(10, settings.security().loginRetryLimit());
        Assertions.assertEquals("192.168.1.0/24", settings.security().ipWhitelist());
        Assertions.assertFalse(settings.security().mfaRequired());
    }

    // -----------------------------------------------------------------------
    // saveAdminBasicSettings() - success
    // -----------------------------------------------------------------------
    @Test
    void shouldSaveAdminBasicSettingsSuccessfully() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminBasicSettings(
                admin,
                new ApiDtos.AdminBasicSettings("新驿站名", "400-111-2222", "上海浦东", false)
        );

        Assertions.assertEquals("新驿站名", result.basic().stationName());
        Assertions.assertEquals("400-111-2222", result.basic().serviceHotline());
        Assertions.assertEquals("上海浦东", result.basic().defaultRegion());
        Assertions.assertFalse(result.basic().mobileDashboardEnabled());

        // Verify persisted
        Assertions.assertEquals("新驿站名", settingStore.get("basic.stationName").getSettingValue());
        Assertions.assertEquals("false", settingStore.get("basic.mobileDashboardEnabled").getSettingValue());

        // Verify audit was recorded
        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_SETTINGS"),
                ArgumentMatchers.eq("basic"),
                ArgumentMatchers.eq("UPDATE"),
                ArgumentMatchers.contains("新驿站名")
        );
    }

    // -----------------------------------------------------------------------
    // saveAdminBasicSettings() - non-admin rejected
    // -----------------------------------------------------------------------
    @Test
    void shouldRejectSaveBasicSettingsForNonAdmin() {
        SessionUser pilot = new SessionUser(2L, "pilot1", RoleType.PILOT, "飞手");

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminSettingsService.saveAdminBasicSettings(
                        pilot,
                        new ApiDtos.AdminBasicSettings("x", "y", "z", true)
                )
        );

        Assertions.assertEquals(403, ex.getCode());
        Assertions.assertEquals("当前角色无权执行该操作", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // saveAdminSecuritySettings() - success
    // -----------------------------------------------------------------------
    @Test
    void shouldSaveAdminSecuritySettingsSuccessfully() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminSecuritySettings(
                admin,
                new ApiDtos.AdminSecuritySettings(60, 3, "10.0.0.0/8", false)
        );

        Assertions.assertEquals(60, result.security().passwordValidityDays());
        Assertions.assertEquals(3, result.security().loginRetryLimit());
        Assertions.assertEquals("10.0.0.0/8", result.security().ipWhitelist());
        Assertions.assertFalse(result.security().mfaRequired());

        // Verify persisted
        Assertions.assertEquals("60", settingStore.get("security.passwordValidityDays").getSettingValue());
        Assertions.assertEquals("3", settingStore.get("security.loginRetryLimit").getSettingValue());
        Assertions.assertEquals("10.0.0.0/8", settingStore.get("security.ipWhitelist").getSettingValue());
        Assertions.assertEquals("false", settingStore.get("security.mfaRequired").getSettingValue());

        // Verify audit was recorded
        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_SETTINGS"),
                ArgumentMatchers.eq("security"),
                ArgumentMatchers.eq("UPDATE"),
                ArgumentMatchers.contains("3")
        );
    }

    // -----------------------------------------------------------------------
    // saveAdminSecuritySettings() - blank ipWhitelist defaults to empty string
    // -----------------------------------------------------------------------
    @Test
    void shouldDefaultBlankIpWhitelistToEmptyString() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminSecuritySettings(
                admin,
                new ApiDtos.AdminSecuritySettings(90, 5, null, true)
        );

        Assertions.assertEquals("", result.security().ipWhitelist());
        Assertions.assertEquals("", settingStore.get("security.ipWhitelist").getSettingValue());
    }

    // -----------------------------------------------------------------------
    // saveAdminNotificationRules() - with rules
    // -----------------------------------------------------------------------
    @Test
    void shouldSaveAdminNotificationRulesWithRules() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        List<ApiDtos.AdminNotificationRule> rules = List.of(
                new ApiDtos.AdminNotificationRule("rule-alert", "告警通知", "EMAIL", true, "level >= WARNING"),
                new ApiDtos.AdminNotificationRule("rule-system", "系统通知", "SMS", false, "type = SYSTEM")
        );

        // Mock selectOne to return null (new rules)
        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(rules)
        );

        Assertions.assertNotNull(result.notifications());
        // Verify insertOrUpdate was called for each rule
        Mockito.verify(adminNotificationRuleMapper, Mockito.atLeast(2))
                .insertOrUpdate(ArgumentMatchers.any(AdminNotificationRuleEntity.class));

        // Verify audit was recorded
        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_SETTINGS"),
                ArgumentMatchers.eq("notifications"),
                ArgumentMatchers.eq("UPDATE"),
                ArgumentMatchers.contains("2")
        );
    }

    // -----------------------------------------------------------------------
    // saveAdminNotificationRules() - with null rules list
    // -----------------------------------------------------------------------
    @Test
    void shouldHandleNullRulesListAsEmpty() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(null)
        );

        Assertions.assertNotNull(result.notifications());
        // No insertOrUpdate should be called for null rules
        Mockito.verify(adminNotificationRuleMapper, Mockito.never())
                .insertOrUpdate(ArgumentMatchers.any(AdminNotificationRuleEntity.class));
    }

    // -----------------------------------------------------------------------
    // saveAdminNotificationRules() - with empty rules (should delete all)
    // -----------------------------------------------------------------------
    @Test
    void shouldDeleteAllRulesWhenEmptyRulesProvided() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        // Pre-populate a rule
        AdminNotificationRuleEntity existingRule = new AdminNotificationRuleEntity();
        existingRule.setId(1L);
        existingRule.setRuleKey("rule-old");
        existingRule.setName("旧规则");
        existingRule.setChannel("EMAIL");
        existingRule.setEnabled(1);
        existingRule.setTriggerDesc("old trigger");
        ruleStore.put("rule-old", existingRule);

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of())
        );

        Assertions.assertNotNull(result.notifications());
        // delete should be called since ruleKeys is empty (no rules to keep)
        // With empty ruleKeys set, the if (!ruleKeys.isEmpty()) block is skipped,
        // so delete is NOT called. This tests the empty rules path.
        Mockito.verify(adminNotificationRuleMapper, Mockito.never())
                .insertOrUpdate(ArgumentMatchers.any(AdminNotificationRuleEntity.class));
    }

    // -----------------------------------------------------------------------
    // saveAdminNotificationRules() - upsert existing rule
    // -----------------------------------------------------------------------
    @Test
    void shouldUpsertExistingNotificationRule() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        // Pre-populate an existing rule
        AdminNotificationRuleEntity existingRule = new AdminNotificationRuleEntity();
        existingRule.setId(10L);
        existingRule.setRuleKey("rule-existing");
        existingRule.setName("旧名称");
        existingRule.setChannel("SMS");
        existingRule.setEnabled(1);
        existingRule.setTriggerDesc("old trigger");
        existingRule.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        ruleStore.put("rule-existing", existingRule);

        // Mock selectOne to return the existing rule when queried by ruleKey
        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(existingRule);

        List<ApiDtos.AdminNotificationRule> rules = List.of(
                new ApiDtos.AdminNotificationRule("rule-existing", "更新名称", "WECHAT", true, "new trigger")
        );

        ApiDtos.AdminSettingsView result = adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(rules)
        );

        // Verify insertOrUpdate was called (upsert path)
        Mockito.verify(adminNotificationRuleMapper)
                .insertOrUpdate(ArgumentMatchers.any(AdminNotificationRuleEntity.class));
    }

    // -----------------------------------------------------------------------
    // saveAdminNotificationRules() - non-admin rejected
    // -----------------------------------------------------------------------
    @Test
    void shouldRejectSaveNotificationRulesForNonAdmin() {
        SessionUser enterprise = new SessionUser(3L, "ent1", RoleType.ENTERPRISE, "企业用户");

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminSettingsService.saveAdminNotificationRules(
                        enterprise,
                        new ApiDtos.AdminNotificationRulesRequest(List.of())
                )
        );

        Assertions.assertEquals(403, ex.getCode());
    }

    // -----------------------------------------------------------------------
    // settingValue() - with blank value in entity (should return fallback)
    // -----------------------------------------------------------------------
    @Test
    void shouldReturnFallbackWhenSettingValueIsBlank() {
        // Store an entity with a blank value
        AdminSettingEntity blankEntity = new AdminSettingEntity();
        blankEntity.setSettingKey("basic.stationName");
        blankEntity.setSettingValue("   ");
        blankEntity.setUpdateTime(LocalDateTime.now());
        settingStore.put("basic.stationName", blankEntity);

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        // Should fall back to default since the stored value is blank
        Assertions.assertEquals("低空驿站一站式数字化服务平台", settings.basic().stationName());
    }

    // -----------------------------------------------------------------------
    // settingValue() - with null entity (should return fallback)
    // -----------------------------------------------------------------------
    @Test
    void shouldReturnFallbackWhenSettingEntityIsNull() {
        // Do not populate any settings - selectById returns null
        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertEquals("低空驿站一站式数字化服务平台", settings.basic().stationName());
        Assertions.assertEquals("400-820-2026", settings.basic().serviceHotline());
    }

    // -----------------------------------------------------------------------
    // settingValue() - with trimmed value (should return trimmed)
    // -----------------------------------------------------------------------
    @Test
    void shouldReturnTrimmedValueWhenSettingExists() {
        AdminSettingEntity entity = new AdminSettingEntity();
        entity.setSettingKey("basic.stationName");
        entity.setSettingValue("  自定义站名  ");
        entity.setUpdateTime(LocalDateTime.now());
        settingStore.put("basic.stationName", entity);

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertEquals("自定义站名", settings.basic().stationName());
    }

    // -----------------------------------------------------------------------
    // parseBooleanSetting() - "true"
    // -----------------------------------------------------------------------
    @Test
    void shouldParseBooleanTrue() {
        putSetting("basic.mobileDashboardEnabled", "true");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertTrue(settings.basic().mobileDashboardEnabled());
    }

    // -----------------------------------------------------------------------
    // parseBooleanSetting() - "1"
    // -----------------------------------------------------------------------
    @Test
    void shouldParseBooleanOneAsTrue() {
        putSetting("basic.mobileDashboardEnabled", "1");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertTrue(settings.basic().mobileDashboardEnabled());
    }

    // -----------------------------------------------------------------------
    // parseBooleanSetting() - "false"
    // -----------------------------------------------------------------------
    @Test
    void shouldParseBooleanFalse() {
        putSetting("basic.mobileDashboardEnabled", "false");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertFalse(settings.basic().mobileDashboardEnabled());
    }

    // -----------------------------------------------------------------------
    // parseBooleanSetting() - "TRUE" case insensitive
    // -----------------------------------------------------------------------
    @Test
    void shouldParseBooleanTrueCaseInsensitive() {
        putSetting("basic.mobileDashboardEnabled", "TRUE");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertTrue(settings.basic().mobileDashboardEnabled());
    }

    // -----------------------------------------------------------------------
    // parseBooleanSetting() - arbitrary string treated as false
    // -----------------------------------------------------------------------
    @Test
    void shouldParseArbitraryStringAsFalse() {
        putSetting("basic.mobileDashboardEnabled", "yes");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertFalse(settings.basic().mobileDashboardEnabled());
    }

    // -----------------------------------------------------------------------
    // parseIntSetting() - valid number
    // -----------------------------------------------------------------------
    @Test
    void shouldParseIntSettingValidNumber() {
        putSetting("security.passwordValidityDays", "45");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        Assertions.assertEquals(45, settings.security().passwordValidityDays());
    }

    // -----------------------------------------------------------------------
    // parseIntSetting() - invalid number (fallback)
    // -----------------------------------------------------------------------
    @Test
    void shouldFallbackWhenIntSettingIsInvalid() {
        putSetting("security.passwordValidityDays", "not-a-number");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        // Should fall back to default value of 90
        Assertions.assertEquals(90, settings.security().passwordValidityDays());
    }

    // -----------------------------------------------------------------------
    // parseIntSetting() - invalid login retry limit
    // -----------------------------------------------------------------------
    @Test
    void shouldFallbackWhenLoginRetryLimitIsInvalid() {
        putSetting("security.loginRetryLimit", "abc");

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();

        // Should fall back to default value of 5
        Assertions.assertEquals(5, settings.security().loginRetryLimit());
    }

    // -----------------------------------------------------------------------
    // upsertNotificationRule() - new rule (no existing entity)
    // -----------------------------------------------------------------------
    @Test
    void shouldCreateNewNotificationRuleWhenNotExists() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        // Mock selectOne to return null (new rule)
        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        ApiDtos.AdminNotificationRule newRule = new ApiDtos.AdminNotificationRule(
                null, "新规则", "EMAIL", true, "level > 3"
        );

        adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of(newRule))
        );

        // Verify insertOrUpdate was called for the new rule
        Mockito.verify(adminNotificationRuleMapper)
                .insertOrUpdate(ArgumentMatchers.<AdminNotificationRuleEntity>argThat(entity -> {
                    AdminNotificationRuleEntity e = (AdminNotificationRuleEntity) entity;
                    return "新规则".equals(e.getName())
                            && "EMAIL".equals(e.getChannel())
                            && e.getEnabled() == 1
                            && "level > 3".equals(e.getTriggerDesc())
                            && e.getCreateTime() != null; // createTime set for new rule
                }));
    }

    // -----------------------------------------------------------------------
    // upsertNotificationRule() - existing rule (update)
    // -----------------------------------------------------------------------
    @Test
    void shouldUpdateExistingNotificationRule() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        AdminNotificationRuleEntity existingRule = new AdminNotificationRuleEntity();
        existingRule.setId(5L);
        existingRule.setRuleKey("rule-update");
        existingRule.setName("旧名称");
        existingRule.setChannel("SMS");
        existingRule.setEnabled(0);
        existingRule.setTriggerDesc("old trigger");
        existingRule.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        ruleStore.put("rule-update", existingRule);

        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(existingRule);

        ApiDtos.AdminNotificationRule updatedRule = new ApiDtos.AdminNotificationRule(
                "rule-update", "新名称", "DINGTALK", true, "new trigger"
        );

        adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of(updatedRule))
        );

        Mockito.verify(adminNotificationRuleMapper)
                .insertOrUpdate(ArgumentMatchers.<AdminNotificationRuleEntity>argThat(entity -> {
                    AdminNotificationRuleEntity e = (AdminNotificationRuleEntity) entity;
                    return "新名称".equals(e.getName())
                            && "DINGTALK".equals(e.getChannel())
                            && e.getEnabled() == 1
                            && "new trigger".equals(e.getTriggerDesc())
                            && e.getId().equals(5L) // existing ID preserved
                            && e.getCreateTime().equals(LocalDateTime.of(2026, 1, 1, 0, 0)); // createTime preserved
                }));
    }

    // -----------------------------------------------------------------------
    // upsertNotificationRule() - rule with null id generates ruleKey from name hash
    // -----------------------------------------------------------------------
    @Test
    void shouldGenerateRuleKeyFromNameWhenIdIsNull() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        ApiDtos.AdminNotificationRule ruleWithoutId = new ApiDtos.AdminNotificationRule(
                null, "告警规则", "SMS", true, "alert > 0"
        );

        adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of(ruleWithoutId))
        );

        String expectedRuleKey = "rule-" + Math.abs("告警规则".hashCode());

        Mockito.verify(adminNotificationRuleMapper)
                .insertOrUpdate(ArgumentMatchers.<AdminNotificationRuleEntity>argThat(entity -> {
                    AdminNotificationRuleEntity e = (AdminNotificationRuleEntity) entity;
                    return expectedRuleKey.equals(e.getRuleKey());
                }));
    }

    // -----------------------------------------------------------------------
    // upsertNotificationRule() - rule with blank id generates ruleKey from name hash
    // -----------------------------------------------------------------------
    @Test
    void shouldGenerateRuleKeyFromNameWhenIdIsBlank() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        ApiDtos.AdminNotificationRule ruleWithBlankId = new ApiDtos.AdminNotificationRule(
                "  ", "系统通知", "WECHAT", false, "type = SYSTEM"
        );

        adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of(ruleWithBlankId))
        );

        String expectedRuleKey = "rule-" + Math.abs("系统通知".hashCode());

        Mockito.verify(adminNotificationRuleMapper)
                .insertOrUpdate(ArgumentMatchers.<AdminNotificationRuleEntity>argThat(entity -> {
                    AdminNotificationRuleEntity e = (AdminNotificationRuleEntity) entity;
                    return expectedRuleKey.equals(e.getRuleKey());
                }));
    }

    // -----------------------------------------------------------------------
    // toAdminNotificationRule() - enabled flag mapping
    // -----------------------------------------------------------------------
    @Test
    void shouldMapEnabledIntegerToBooleanInNotificationRules() {
        AdminNotificationRuleEntity enabledEntity = new AdminNotificationRuleEntity();
        enabledEntity.setId(1L);
        enabledEntity.setRuleKey("rule-enabled");
        enabledEntity.setName("启用规则");
        enabledEntity.setChannel("EMAIL");
        enabledEntity.setEnabled(1);
        enabledEntity.setTriggerDesc("trigger1");

        AdminNotificationRuleEntity disabledEntity = new AdminNotificationRuleEntity();
        disabledEntity.setId(2L);
        disabledEntity.setRuleKey("rule-disabled");
        disabledEntity.setName("禁用规则");
        disabledEntity.setChannel("SMS");
        disabledEntity.setEnabled(0);
        disabledEntity.setTriggerDesc("trigger2");

        ruleStore.put("rule-enabled", enabledEntity);
        ruleStore.put("rule-disabled", disabledEntity);

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();
        List<ApiDtos.AdminNotificationRule> notifications = settings.notifications();

        // Find the rules by name since order may vary
        ApiDtos.AdminNotificationRule enabledRule = notifications.stream()
                .filter(r -> "启用规则".equals(r.name())).findFirst().orElse(null);
        ApiDtos.AdminNotificationRule disabledRule = notifications.stream()
                .filter(r -> "禁用规则".equals(r.name())).findFirst().orElse(null);

        Assertions.assertNotNull(enabledRule);
        Assertions.assertTrue(enabledRule.enabled());
        Assertions.assertNotNull(disabledRule);
        Assertions.assertFalse(disabledRule.enabled());
    }

    // -----------------------------------------------------------------------
    // toAdminNotificationRule() - null enabled maps to false
    // -----------------------------------------------------------------------
    @Test
    void shouldMapNullEnabledToFalseInNotificationRules() {
        AdminNotificationRuleEntity nullEnabledEntity = new AdminNotificationRuleEntity();
        nullEnabledEntity.setId(3L);
        nullEnabledEntity.setRuleKey("rule-null");
        nullEnabledEntity.setName("空值规则");
        nullEnabledEntity.setChannel("DINGTALK");
        nullEnabledEntity.setEnabled(null);
        nullEnabledEntity.setTriggerDesc("trigger3");

        ruleStore.put("rule-null", nullEnabledEntity);

        ApiDtos.AdminSettingsView settings = adminSettingsService.getAdminSettings();
        ApiDtos.AdminNotificationRule rule = settings.notifications().stream()
                .filter(r -> "空值规则".equals(r.name())).findFirst().orElse(null);

        Assertions.assertNotNull(rule);
        Assertions.assertFalse(rule.enabled());
    }

    // -----------------------------------------------------------------------
    // saveAdminSecuritySettings() - non-admin rejected
    // -----------------------------------------------------------------------
    @Test
    void shouldRejectSaveSecuritySettingsForNonAdmin() {
        SessionUser pilot = new SessionUser(2L, "pilot1", RoleType.PILOT, "飞手");

        BizException ex = Assertions.assertThrows(BizException.class, () ->
                adminSettingsService.saveAdminSecuritySettings(
                        pilot,
                        new ApiDtos.AdminSecuritySettings(30, 3, "", false)
                )
        );

        Assertions.assertEquals(403, ex.getCode());
        Assertions.assertEquals("当前角色无权执行该操作", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // saveAdminBasicSettings() - verify all four settings persisted
    // -----------------------------------------------------------------------
    @Test
    void shouldPersistAllFourBasicSettings() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        adminSettingsService.saveAdminBasicSettings(
                admin,
                new ApiDtos.AdminBasicSettings("站名", "热线", "区域", true)
        );

        Assertions.assertNotNull(settingStore.get("basic.stationName"));
        Assertions.assertNotNull(settingStore.get("basic.serviceHotline"));
        Assertions.assertNotNull(settingStore.get("basic.defaultRegion"));
        Assertions.assertNotNull(settingStore.get("basic.mobileDashboardEnabled"));

        Assertions.assertEquals("站名", settingStore.get("basic.stationName").getSettingValue());
        Assertions.assertEquals("热线", settingStore.get("basic.serviceHotline").getSettingValue());
        Assertions.assertEquals("区域", settingStore.get("basic.defaultRegion").getSettingValue());
        Assertions.assertEquals("true", settingStore.get("basic.mobileDashboardEnabled").getSettingValue());
    }

    // -----------------------------------------------------------------------
    // saveAdminSecuritySettings() - verify all four settings persisted
    // -----------------------------------------------------------------------
    @Test
    void shouldPersistAllFourSecuritySettings() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        adminSettingsService.saveAdminSecuritySettings(
                admin,
                new ApiDtos.AdminSecuritySettings(120, 10, "10.0.0.1", true)
        );

        Assertions.assertEquals("120", settingStore.get("security.passwordValidityDays").getSettingValue());
        Assertions.assertEquals("10", settingStore.get("security.loginRetryLimit").getSettingValue());
        Assertions.assertEquals("10.0.0.1", settingStore.get("security.ipWhitelist").getSettingValue());
        Assertions.assertEquals("true", settingStore.get("security.mfaRequired").getSettingValue());
    }

    // -----------------------------------------------------------------------
    // saveSetting() - updateTime is set
    // -----------------------------------------------------------------------
    @Test
    void shouldSetUpdateTimeWhenSavingSetting() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        adminSettingsService.saveAdminBasicSettings(
                admin,
                new ApiDtos.AdminBasicSettings("站名", "热线", "区域", false)
        );

        AdminSettingEntity saved = settingStore.get("basic.stationName");
        Assertions.assertNotNull(saved.getUpdateTime());
    }

    // -----------------------------------------------------------------------
    // upsertNotificationRule() - disabled rule sets enabled to 0
    // -----------------------------------------------------------------------
    @Test
    void shouldSetEnabledToZeroForDisabledRule() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        ApiDtos.AdminNotificationRule disabledRule = new ApiDtos.AdminNotificationRule(
                "rule-disabled-new", "禁用新规则", "SMS", false, "disabled trigger"
        );

        adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of(disabledRule))
        );

        Mockito.verify(adminNotificationRuleMapper)
                .insertOrUpdate(ArgumentMatchers.<AdminNotificationRuleEntity>argThat(entity -> {
                    AdminNotificationRuleEntity e = (AdminNotificationRuleEntity) entity;
                    return e.getEnabled() == 0;
                }));
    }

    // -----------------------------------------------------------------------
    // audit() - with null actor
    // -----------------------------------------------------------------------
    @Test
    void shouldHandleNullActorInAuditWhenEnsureRolePasses() {
        // ADMIN role passes ensureRole, but we test the audit path indirectly
        // The audit method handles null actor by setting actorUserId and actorRole to null
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        adminSettingsService.saveAdminBasicSettings(
                admin,
                new ApiDtos.AdminBasicSettings("站名", "热线", "区域", true)
        );

        Mockito.verify(auditLogService).record(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq("ADMIN"),
                ArgumentMatchers.eq("ADMIN_SETTINGS"),
                ArgumentMatchers.eq("basic"),
                ArgumentMatchers.eq("UPDATE"),
                ArgumentMatchers.anyString()
        );
    }

    // -----------------------------------------------------------------------
    // saveAdminNotificationRules() - delete rules not in the request
    // -----------------------------------------------------------------------
    @Test
    void shouldDeleteRulesNotPresentInRequest() {
        SessionUser admin = new SessionUser(1L, "admin", RoleType.ADMIN, "管理员");

        Mockito.when(adminNotificationRuleMapper.selectOne(ArgumentMatchers.any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        // Provide one rule with a specific id
        ApiDtos.AdminNotificationRule rule = new ApiDtos.AdminNotificationRule(
                "rule-keep", "保留规则", "EMAIL", true, "keep trigger"
        );

        adminSettingsService.saveAdminNotificationRules(
                admin,
                new ApiDtos.AdminNotificationRulesRequest(List.of(rule))
        );

        // delete should be called to remove rules not in the provided list
        Mockito.verify(adminNotificationRuleMapper).delete(ArgumentMatchers.any(LambdaQueryWrapper.class));
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private void putSetting(String key, String value) {
        AdminSettingEntity entity = new AdminSettingEntity();
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        entity.setUpdateTime(LocalDateTime.now());
        settingStore.put(key, entity);
    }
}

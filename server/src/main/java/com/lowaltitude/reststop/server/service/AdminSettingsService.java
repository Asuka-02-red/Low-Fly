package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.AdminNotificationRuleEntity;
import com.lowaltitude.reststop.server.entity.AdminSettingEntity;
import com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper;
import com.lowaltitude.reststop.server.mapper.AdminSettingMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSettingsService {

    private final AdminSettingMapper adminSettingMapper;
    private final AdminNotificationRuleMapper adminNotificationRuleMapper;
    private final AuditLogService auditLogService;

    public AdminSettingsService(
            AdminSettingMapper adminSettingMapper,
            AdminNotificationRuleMapper adminNotificationRuleMapper,
            AuditLogService auditLogService
    ) {
        this.adminSettingMapper = adminSettingMapper;
        this.adminNotificationRuleMapper = adminNotificationRuleMapper;
        this.auditLogService = auditLogService;
    }

    public ApiDtos.AdminSettingsView getAdminSettings() {
        return new ApiDtos.AdminSettingsView(
                new ApiDtos.AdminBasicSettings(
                        settingValue("basic.stationName", "低空驿站一站式数字化服务平台"),
                        settingValue("basic.serviceHotline", "400-820-2026"),
                        settingValue("basic.defaultRegion", "深圳南山"),
                        parseBooleanSetting("basic.mobileDashboardEnabled", true)
                ),
                new ApiDtos.AdminSecuritySettings(
                        parseIntSetting("security.passwordValidityDays", 90),
                        parseIntSetting("security.loginRetryLimit", 5),
                        settingValue("security.ipWhitelist", ""),
                        parseBooleanSetting("security.mfaRequired", true)
                ),
                adminNotificationRuleMapper.selectList(new LambdaQueryWrapper<AdminNotificationRuleEntity>()
                                .orderByAsc(AdminNotificationRuleEntity::getId))
                        .stream()
                        .map(this::toAdminNotificationRule)
                        .toList()
        );
    }

    @Transactional
    public ApiDtos.AdminSettingsView saveAdminBasicSettings(SessionUser admin, ApiDtos.AdminBasicSettings request) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        saveSetting("basic.stationName", request.stationName());
        saveSetting("basic.serviceHotline", request.serviceHotline());
        saveSetting("basic.defaultRegion", request.defaultRegion());
        saveSetting("basic.mobileDashboardEnabled", String.valueOf(request.mobileDashboardEnabled()));
        audit(admin, "ADMIN_SETTINGS", "basic", "UPDATE", "station=" + request.stationName());
        return getAdminSettings();
    }

    @Transactional
    public ApiDtos.AdminSettingsView saveAdminSecuritySettings(SessionUser admin, ApiDtos.AdminSecuritySettings request) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        saveSetting("security.passwordValidityDays", String.valueOf(request.passwordValidityDays()));
        saveSetting("security.loginRetryLimit", String.valueOf(request.loginRetryLimit()));
        saveSetting("security.ipWhitelist", PlatformUtils.defaultIfBlank(request.ipWhitelist(), ""));
        saveSetting("security.mfaRequired", String.valueOf(request.mfaRequired()));
        audit(admin, "ADMIN_SETTINGS", "security", "UPDATE", "retryLimit=" + request.loginRetryLimit());
        return getAdminSettings();
    }

    @Transactional
    public ApiDtos.AdminSettingsView saveAdminNotificationRules(SessionUser admin, ApiDtos.AdminNotificationRulesRequest request) {
        PlatformUtils.ensureRole(admin, RoleType.ADMIN);
        List<ApiDtos.AdminNotificationRule> rules = request.rules() == null ? List.of() : request.rules();
        Set<String> ruleKeys = rules.stream()
                .map(rule -> PlatformUtils.defaultIfBlank(rule.id(), "rule-" + Math.abs(rule.name().hashCode())))
                .collect(Collectors.toSet());
        if (!ruleKeys.isEmpty()) {
            adminNotificationRuleMapper.delete(new LambdaQueryWrapper<AdminNotificationRuleEntity>()
                    .notIn(AdminNotificationRuleEntity::getRuleKey, ruleKeys));
        }
        for (ApiDtos.AdminNotificationRule rule : rules) {
            upsertNotificationRule(rule);
        }
        audit(admin, "ADMIN_SETTINGS", "notifications", "UPDATE", "rules=" + rules.size());
        return getAdminSettings();
    }

    private String settingValue(String key, String fallback) {
        AdminSettingEntity entity = adminSettingMapper.selectById(key);
        return entity == null || PlatformUtils.isBlank(entity.getSettingValue()) ? fallback : entity.getSettingValue().trim();
    }

    private boolean parseBooleanSetting(String key, boolean fallback) {
        String value = settingValue(key, String.valueOf(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private int parseIntSetting(String key, int fallback) {
        try {
            return Integer.parseInt(settingValue(key, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void saveSetting(String key, String value) {
        AdminSettingEntity entity = new AdminSettingEntity();
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        entity.setUpdateTime(LocalDateTime.now());
        adminSettingMapper.insertOrUpdate(entity);
    }

    private void upsertNotificationRule(ApiDtos.AdminNotificationRule rule) {
        String ruleKey = PlatformUtils.defaultIfBlank(rule.id(), "rule-" + Math.abs(rule.name().hashCode()));
        AdminNotificationRuleEntity entity = adminNotificationRuleMapper.selectOne(new LambdaQueryWrapper<AdminNotificationRuleEntity>()
                .eq(AdminNotificationRuleEntity::getRuleKey, ruleKey)
                .last("limit 1"));
        if (entity == null) {
            entity = new AdminNotificationRuleEntity();
            entity.setRuleKey(ruleKey);
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setName(rule.name().trim());
        entity.setChannel(rule.channel().trim());
        entity.setEnabled(rule.enabled() ? 1 : 0);
        entity.setTriggerDesc(rule.trigger().trim());
        entity.setUpdateTime(LocalDateTime.now());
        adminNotificationRuleMapper.insertOrUpdate(entity);
    }

    private ApiDtos.AdminNotificationRule toAdminNotificationRule(AdminNotificationRuleEntity entity) {
        return new ApiDtos.AdminNotificationRule(
                entity.getRuleKey(),
                entity.getName(),
                entity.getChannel(),
                PlatformUtils.safeInt(entity.getEnabled()) > 0,
                entity.getTriggerDesc()
        );
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

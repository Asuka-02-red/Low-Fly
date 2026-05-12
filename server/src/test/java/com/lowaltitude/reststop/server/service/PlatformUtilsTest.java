package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlatformUtilsTest {

    @Test
    public void shouldFormatTime() {
        LocalDateTime time = LocalDateTime.of(2026, 5, 1, 10, 30);
        Assertions.assertEquals("2026-05-01 10:30", PlatformUtils.formatTime(time));
    }

    @Test
    public void shouldFormatDateTimeWithNull() {
        Assertions.assertEquals("-", PlatformUtils.formatDateTime(null));
    }

    @Test
    public void shouldFormatDateTimeWithValue() {
        LocalDateTime time = LocalDateTime.of(2026, 5, 1, 10, 30);
        Assertions.assertEquals("2026-05-01 10:30", PlatformUtils.formatDateTime(time));
    }

    @Test
    public void shouldParseTaskDeadline() {
        LocalDateTime result = PlatformUtils.parseTaskDeadline("2026-05-01 10:00");
        Assertions.assertEquals(LocalDateTime.of(2026, 5, 1, 10, 0), result);
    }

    @Test
    public void shouldRejectInvalidDeadline() {
        Assertions.assertThrows(BizException.class, () -> PlatformUtils.parseTaskDeadline("invalid"));
    }

    @Test
    public void shouldDisplayName() {
        UserAccountEntity user = new UserAccountEntity();
        user.setRealName("陈伶");
        user.setUsername("zhangsan");
        Assertions.assertEquals("陈伶", PlatformUtils.displayName(user));
    }

    @Test
    public void shouldFallbackToUsernameWhenRealNameBlank() {
        UserAccountEntity user = new UserAccountEntity();
        user.setRealName("");
        user.setUsername("zhangsan");
        Assertions.assertEquals("zhangsan", PlatformUtils.displayName(user));
    }

    @Test
    public void shouldReturnDashForNullUser() {
        Assertions.assertEquals("-", PlatformUtils.displayName(null));
    }

    @Test
    public void shouldSafePhone() {
        UserAccountEntity user = new UserAccountEntity();
        user.setPhone("13800138000");
        Assertions.assertEquals("13800138000", PlatformUtils.safePhone(user));
    }

    @Test
    public void shouldSafePhoneReturnDashForNull() {
        Assertions.assertEquals("-", PlatformUtils.safePhone(null));
    }

    @Test
    public void shouldDisplayRole() {
        Assertions.assertEquals("管理员", PlatformUtils.displayRole("ADMIN"));
        Assertions.assertEquals("企业", PlatformUtils.displayRole("ENTERPRISE"));
        Assertions.assertEquals("飞手", PlatformUtils.displayRole("PILOT"));
        Assertions.assertEquals("机构", PlatformUtils.displayRole("INSTITUTION"));
    }

    @Test
    public void shouldDisplayRoleForBlank() {
        Assertions.assertEquals("未定义角色", PlatformUtils.displayRole(""));
    }

    @Test
    public void shouldDisplayRoleForUnknown() {
        Assertions.assertEquals("UNKNOWN", PlatformUtils.displayRole("UNKNOWN"));
    }

    @Test
    public void shouldPermissionGroupName() {
        Assertions.assertEquals("全量管控组", PlatformUtils.permissionGroupName("ADMIN"));
        Assertions.assertEquals("项目运营组", PlatformUtils.permissionGroupName("ENTERPRISE"));
        Assertions.assertEquals("执行协同组", PlatformUtils.permissionGroupName("PILOT"));
        Assertions.assertEquals("培训管理组", PlatformUtils.permissionGroupName("INSTITUTION"));
    }

    @Test
    public void shouldMapFeedbackStatus() {
        Assertions.assertEquals("处理中", PlatformUtils.mapFeedbackStatus("PROCESSING"));
        Assertions.assertEquals("已关闭", PlatformUtils.mapFeedbackStatus("CLOSED"));
        Assertions.assertEquals("待处理", PlatformUtils.mapFeedbackStatus("OPEN"));
    }

    @Test
    public void shouldNormalizeStatus() {
        Assertions.assertEquals("CLOSED", PlatformUtils.normalizeStatus("已关闭"));
        Assertions.assertEquals("PROCESSING", PlatformUtils.normalizeStatus("处理中"));
        Assertions.assertEquals("OPEN", PlatformUtils.normalizeStatus("待处理"));
    }

    @Test
    public void shouldDefaultBudget() {
        Assertions.assertEquals(BigDecimal.ZERO, PlatformUtils.defaultBudget(null));
        Assertions.assertEquals(BigDecimal.TEN, PlatformUtils.defaultBudget(BigDecimal.TEN));
    }

    @Test
    public void shouldDefaultIfBlank() {
        Assertions.assertEquals("fallback", PlatformUtils.defaultIfBlank(null, "fallback"));
        Assertions.assertEquals("fallback", PlatformUtils.defaultIfBlank("  ", "fallback"));
        Assertions.assertEquals("hello", PlatformUtils.defaultIfBlank(" hello ", "fallback"));
    }

    @Test
    public void shouldNormalizeNullable() {
        Assertions.assertNull(PlatformUtils.normalizeNullable(null));
        Assertions.assertNull(PlatformUtils.normalizeNullable(""));
        Assertions.assertNull(PlatformUtils.normalizeNullable("  "));
        Assertions.assertEquals("test", PlatformUtils.normalizeNullable(" test "));
    }

    @Test
    public void shouldIsBlank() {
        Assertions.assertTrue(PlatformUtils.isBlank(null));
        Assertions.assertTrue(PlatformUtils.isBlank(""));
        Assertions.assertTrue(PlatformUtils.isBlank("  "));
        Assertions.assertFalse(PlatformUtils.isBlank("test"));
    }

    @Test
    public void shouldSafeInt() {
        Assertions.assertEquals(0, PlatformUtils.safeInt(null));
        Assertions.assertEquals(5, PlatformUtils.safeInt(5));
    }

    @Test
    public void shouldToUid() {
        Assertions.assertEquals("123", PlatformUtils.toUid(123L));
        Assertions.assertEquals("", PlatformUtils.toUid(null));
    }

    @Test
    public void shouldPaymentChannelLabel() {
        Assertions.assertEquals("支付宝", PlatformUtils.paymentChannelLabel("ALIPAY"));
        Assertions.assertEquals("微信支付", PlatformUtils.paymentChannelLabel("WECHAT"));
        Assertions.assertEquals("微信支付", PlatformUtils.paymentChannelLabel("WECHAT_PAY"));
        Assertions.assertEquals("银行转账", PlatformUtils.paymentChannelLabel("BANK_TRANSFER"));
        Assertions.assertEquals("待支付", PlatformUtils.paymentChannelLabel(null));
    }

    @Test
    public void shouldMapAdminUserStatus() {
        Assertions.assertEquals("待审核", PlatformUtils.mapAdminUserStatus(null));
        Assertions.assertEquals("停用", PlatformUtils.mapAdminUserStatus(0));
        Assertions.assertEquals("停用", PlatformUtils.mapAdminUserStatus(-1));
        Assertions.assertEquals("启用", PlatformUtils.mapAdminUserStatus(1));
    }

    @Test
    public void shouldMapAdminProjectStatus() {
        Assertions.assertEquals("规划中", PlatformUtils.mapAdminProjectStatus("REVIEWING"));
        Assertions.assertEquals("执行中", PlatformUtils.mapAdminProjectStatus("PUBLISHED"));
        Assertions.assertEquals("已完成", PlatformUtils.mapAdminProjectStatus("COMPLETED"));
        Assertions.assertEquals("已完成", PlatformUtils.mapAdminProjectStatus("PAID"));
        Assertions.assertEquals("已暂停", PlatformUtils.mapAdminProjectStatus("CANCELLED"));
        Assertions.assertEquals("已暂停", PlatformUtils.mapAdminProjectStatus("CLOSED"));
    }

    @Test
    public void shouldEstimateProjectProgress() {
        Assertions.assertEquals(20, PlatformUtils.estimateProjectProgress("REVIEWING"));
        Assertions.assertEquals(68, PlatformUtils.estimateProjectProgress("PUBLISHED"));
        Assertions.assertEquals(100, PlatformUtils.estimateProjectProgress("COMPLETED"));
        Assertions.assertEquals(56, PlatformUtils.estimateProjectProgress("CANCELLED"));
        Assertions.assertEquals(32, PlatformUtils.estimateProjectProgress("UNKNOWN"));
    }

    @Test
    public void shouldEstimateRiskLevel() {
        Assertions.assertEquals("中", PlatformUtils.estimateRiskLevel(true, false));
        Assertions.assertEquals("中", PlatformUtils.estimateRiskLevel(false, false));
        Assertions.assertEquals("低", PlatformUtils.estimateRiskLevel(false, true));
    }

    @Test
    public void shouldEstimateTrainingCompletion() {
        Assertions.assertEquals(72, PlatformUtils.estimateTrainingCompletion("MAPPING"));
        Assertions.assertEquals(88, PlatformUtils.estimateTrainingCompletion("INSPECTION"));
        Assertions.assertEquals(60, PlatformUtils.estimateTrainingCompletion("OTHER"));
    }

    @Test
    public void shouldResolvePaymentStatus() {
        Assertions.assertEquals("待结算", PlatformUtils.resolvePaymentStatus(false, 0, 0));
        Assertions.assertEquals("待结算", PlatformUtils.resolvePaymentStatus(true, 0, 3));
        Assertions.assertEquals("部分结算", PlatformUtils.resolvePaymentStatus(true, 1, 3));
        Assertions.assertEquals("已结算", PlatformUtils.resolvePaymentStatus(true, 3, 3));
    }

    @Test
    public void shouldBuildOrderRemark() {
        Assertions.assertTrue(PlatformUtils.buildOrderRemark("MAPPING").contains("校准"));
        Assertions.assertTrue(PlatformUtils.buildOrderRemark("INSPECTION").contains("巡检"));
        Assertions.assertTrue(PlatformUtils.buildOrderRemark("OTHER").contains("预约时间"));
    }

    @Test
    public void shouldBuildOperationRadius() {
        Assertions.assertEquals(1200, PlatformUtils.buildOperationRadius("MAPPING"));
        Assertions.assertEquals(900, PlatformUtils.buildOperationRadius("INSPECTION"));
        Assertions.assertEquals(700, PlatformUtils.buildOperationRadius("OTHER"));
    }

    @Test
    public void shouldNormalizeCourseType() {
        Assertions.assertEquals("OFFLINE", PlatformUtils.normalizeCourseType("OFFLINE"));
        Assertions.assertEquals("ARTICLE", PlatformUtils.normalizeCourseType("ONLINE"));
    }

    @Test
    public void shouldNormalizeCourseStatus() {
        Assertions.assertEquals("OPEN", PlatformUtils.normalizeCourseStatus("OPEN"));
        Assertions.assertEquals("CLOSED", PlatformUtils.normalizeCourseStatus("CLOSED"));
        Assertions.assertEquals("DRAFT", PlatformUtils.normalizeCourseStatus("DRAFT"));
    }

    @Test
    public void shouldResolveSeatAvailable() {
        Assertions.assertEquals(30, PlatformUtils.resolveSeatAvailable(null, null, 30));
        Assertions.assertEquals(25, PlatformUtils.resolveSeatAvailable(30, 25, 30));
        Assertions.assertEquals(0, PlatformUtils.resolveSeatAvailable(30, 25, 3));
    }

    @Test
    public void shouldParseRole() {
        Assertions.assertEquals(RoleType.ADMIN, PlatformUtils.parseRole("ADMIN"));
        Assertions.assertEquals(RoleType.PILOT, PlatformUtils.parseRole("pilot"));
    }

    @Test
    public void shouldRejectInvalidRole() {
        Assertions.assertThrows(BizException.class, () -> PlatformUtils.parseRole("INVALID"));
    }

    @Test
    public void shouldEnsureRole() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        SessionUser pilot = new SessionUser(1L, "pilot", RoleType.PILOT, "飞手");
        Assertions.assertDoesNotThrow(() -> PlatformUtils.ensureRole(admin, RoleType.ENTERPRISE));
        Assertions.assertDoesNotThrow(() -> PlatformUtils.ensureRole(pilot, RoleType.PILOT));
        Assertions.assertThrows(BizException.class, () -> PlatformUtils.ensureRole(pilot, RoleType.ENTERPRISE));
    }
}

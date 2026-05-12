package com.example.low_altitudereststop.ui;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
/**
 * 角色UI配置类，根据用户角色提供差异化的界面配置。
 * <p>
 * 根据飞手（PILOT）和企业（ENTERPRISE）角色返回不同的底部导航项、
 * 快捷操作列表、运营指标标签和合规功能描述，
 * 实现角色驱动的UI差异化展示。
 * </p>
 */
public final class RoleUiConfig {

    public final String displayName;
    public final String dashboardLabel;
    public final String taskLabel;
    public final String messageLabel;
    public final String trainingLabel;
    public final String complianceLabel;
    public final String profileLabel;
    public final String heroModeLabel;
    public final String taskHeadline;
    public final String complianceHeadline;
    @ColorRes public final int accentColorRes;
    @ColorRes public final int accentSurfaceRes;

    private RoleUiConfig(
            String displayName,
            String dashboardLabel,
            String taskLabel,
            String messageLabel,
            String trainingLabel,
            String complianceLabel,
            String profileLabel,
            String heroModeLabel,
            String taskHeadline,
            String complianceHeadline,
            int accentColorRes,
            int accentSurfaceRes
    ) {
        this.displayName = displayName;
        this.dashboardLabel = dashboardLabel;
        this.taskLabel = taskLabel;
        this.messageLabel = messageLabel;
        this.trainingLabel = trainingLabel;
        this.complianceLabel = complianceLabel;
        this.profileLabel = profileLabel;
        this.heroModeLabel = heroModeLabel;
        this.taskHeadline = taskHeadline;
        this.complianceHeadline = complianceHeadline;
        this.accentColorRes = accentColorRes;
        this.accentSurfaceRes = accentSurfaceRes;
    }

    @NonNull
    public static RoleUiConfig from(UserRole role) {
        switch (role) {
            case ENTERPRISE:
                return new RoleUiConfig(
                        "企业端",
                        "总览",
                        "项目",
                        "消息",
                        "课程",
                        "飞行",
                        "经营",
                        "企业调度模式",
                        "任务发布与项目监控",
                        "飞行申请与空域管控",
                        com.example.low_altitudereststop.core.ui.R.color.ui_enterprise_accent,
                        com.example.low_altitudereststop.core.ui.R.color.ui_enterprise_surface
                );
            case PILOT:
                return new RoleUiConfig(
                        "飞手端",
                        "首页",
                        "执行",
                        "消息",
                        "学习",
                        "飞行",
                        "我的",
                        "飞手执行模式",
                        "任务接单与飞行执行",
                        "飞行申请与风险合规",
                        com.example.low_altitudereststop.core.ui.R.color.ui_pilot_accent,
                        com.example.low_altitudereststop.core.ui.R.color.ui_pilot_surface
                );
            default:
                return new RoleUiConfig(
                        "访客模式",
                        "首页",
                        "任务",
                        "消息",
                        "课程",
                        "合规",
                        "我的",
                        "标准模式",
                        "任务工作台",
                        "合规工作台",
                        com.example.low_altitudereststop.core.ui.R.color.ui_info,
                        com.example.low_altitudereststop.core.ui.R.color.ui_surface_emphasis
                );
        }
    }
}

package com.example.low_altitudereststop.feature.risk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.core.model.PlatformModels;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警场景仓库，根据用户角色（飞手/企业）生成模拟告警场景数据及对应的处置建议，
 * 为告警列表和详情页提供数据支撑。
 */
final class AlertScenarioRepository {

    private AlertScenarioRepository() {
    }

    @NonNull
    static List<AlertScenario> buildAlerts(@Nullable String roleName) {
        List<PlatformModels.AlertView> source = buildMockViews(roleName);
        List<AlertScenario> items = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            PlatformModels.AlertView alert = source.get(i);
            items.add(buildScenario(roleName, alert, i));
        }
        return items;
    }

    @Nullable
    static AlertScenario findById(@Nullable String roleName, long alertId) {
        for (AlertScenario item : buildAlerts(roleName)) {
            if (item.alert.id != null && item.alert.id == alertId) {
                return item;
            }
        }
        return null;
    }

    @NonNull
    private static AlertScenario buildScenario(@Nullable String roleName, @NonNull PlatformModels.AlertView alert, int index) {
        String level = safe(alert.level);
        String title = level + "告警";
        String occurredAt = safe(alert.createTime);
        String riskDescription = safe(alert.content);
        String advice;
        if (isEnterprise(roleName)) {
            advice = index % 2 == 0
                    ? "建议立即核对项目计划、订单节点与调度资源，并在 30 分钟内完成复核。"
                    : "建议同步财务与项目负责人，确认交付材料齐备后再推进后续排班。";
        } else {
            advice = index % 2 == 0
                    ? "建议复核空域、天气和返航点配置，确认飞行窗口后再执行任务。"
                    : "建议检查图传链路、电池余量和备降点，必要时切换至保守航线。";
        }
        return new AlertScenario(alert, title, occurredAt, riskDescription, advice);
    }

    @NonNull
    private static List<PlatformModels.AlertView> buildMockViews(@Nullable String roleName) {
        List<PlatformModels.AlertView> items = new ArrayList<>();
        if (isEnterprise(roleName)) {
            items.add(buildView(9000L, "高风险", "需处置", "企业订单回款节点临近，当前项目交付材料仍待补充，请优先核对合同与验收清单。", "2026-04-24 09:20"));
            items.add(buildView(9001L, "中风险", "待复核", "桥梁巡检项目出现气象窗口波动，建议重新评估执行时段与人员排班。", "2026-04-24 08:35"));
            items.add(buildView(9002L, "高风险", "需处置", "夜航巡查项目现场存在临时空域限制，请在重新起飞前完成审批复核。", "2026-04-24 07:55"));
            items.add(buildView(9003L, "中风险", "待复核", "山地投送演练返航点通信质量下降，建议准备备用链路与安全返航预案。", "2026-04-24 07:10"));
        } else {
            items.add(buildView(8000L, "高风险", "需处置", "飞行空域边界出现临时管制调整，请在起飞前重新核对航线与返航点。", "2026-04-24 09:18"));
            items.add(buildView(8001L, "中风险", "待复核", "实时天气风速持续抬升，建议缩短执行窗口并预留备降时间。", "2026-04-24 08:40"));
            items.add(buildView(8002L, "高风险", "需处置", "图传链路存在抖动，建议在任务开始前检查天线方向与备用通信链路。", "2026-04-24 08:05"));
            items.add(buildView(8003L, "中风险", "待复核", "山地区域低空补给点周边能见度下降，请关注落点安全范围与飞行节奏。", "2026-04-24 07:25"));
        }
        return items;
    }

    @NonNull
    private static PlatformModels.AlertView buildView(
            long id,
            @NonNull String level,
            @NonNull String status,
            @NonNull String content,
            @NonNull String createTime
    ) {
        PlatformModels.AlertView view = new PlatformModels.AlertView();
        view.id = id;
        view.level = level;
        view.status = status;
        view.content = content;
        view.createTime = createTime;
        return view;
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static boolean isEnterprise(@Nullable String roleName) {
        return "ENTERPRISE".equalsIgnoreCase(safe(roleName));
    }

    static final class AlertScenario {
        final PlatformModels.AlertView alert;
        final String detailTitle;
        final String occurredAt;
        final String riskDescription;
        final String suggestion;

        AlertScenario(
                @NonNull PlatformModels.AlertView alert,
                @NonNull String detailTitle,
                @NonNull String occurredAt,
                @NonNull String riskDescription,
                @NonNull String suggestion
        ) {
            this.alert = alert;
            this.detailTitle = detailTitle;
            this.occurredAt = occurredAt;
            this.riskDescription = riskDescription;
            this.suggestion = suggestion;
        }
    }
}

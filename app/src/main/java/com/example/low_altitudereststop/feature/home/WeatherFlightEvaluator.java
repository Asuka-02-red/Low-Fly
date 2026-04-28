package com.example.low_altitudereststop.feature.home;

import com.example.low_altitudereststop.core.model.PlatformModels;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class WeatherFlightEvaluator {

    private static final Map<Integer, Double> WIND_SPEED_LEVELS = buildWindLevels();
    private static final String[] THUNDERSTORM_RISK_LABELS = new String[]{
            "极低", "很低", "偏低", "中等", "偏高", "高", "很高", "严重", "极高"
    };

    private WeatherFlightEvaluator() {
    }

    static DerivedMetrics deriveMetrics(String liveWeather, String forecastDayWeather, String forecastNightWeather, String windPowerText) {
        String combinedWeather = String.join(" ", normalizeWeather(liveWeather), normalizeWeather(forecastDayWeather), normalizeWeather(forecastNightWeather))
                .toLowerCase(Locale.ROOT);
        double windSpeed = round(parseWindSpeed(windPowerText));
        boolean thunderstorm = containsAny(combinedWeather, "雷", "强对流", "飑", "冰雹");

        double visibility = 12.0;
        int precipitationProbability = 15;
        double precipitationIntensity = 0.0;
        String thunderstormRisk = thunderstorm ? "高" : "低";

        if (containsAny(combinedWeather, "暴雨", "大暴雨", "特大暴雨")) {
            visibility = 1.0;
            precipitationProbability = 98;
            precipitationIntensity = 8.0;
        } else if (containsAny(combinedWeather, "大雨")) {
            visibility = 2.0;
            precipitationProbability = 92;
            precipitationIntensity = 3.0;
        } else if (containsAny(combinedWeather, "中雨")) {
            visibility = 4.0;
            precipitationProbability = 82;
            precipitationIntensity = 1.0;
        } else if (containsAny(combinedWeather, "小雨", "阵雨", "毛毛雨", "冻雨")) {
            visibility = 6.0;
            precipitationProbability = 62;
            precipitationIntensity = 0.3;
        } else if (containsAny(combinedWeather, "雨夹雪", "雪")) {
            visibility = 4.0;
            precipitationProbability = 58;
            precipitationIntensity = 0.4;
        } else if (containsAny(combinedWeather, "雾", "霾", "沙尘")) {
            visibility = 3.0;
            precipitationProbability = 18;
            precipitationIntensity = 0.0;
            thunderstormRisk = "低";
        } else if (containsAny(combinedWeather, "阴")) {
            visibility = 10.0;
            precipitationProbability = 25;
        } else if (containsAny(combinedWeather, "多云")) {
            visibility = 14.0;
            precipitationProbability = 12;
        } else if (containsAny(combinedWeather, "晴")) {
            visibility = 18.0;
            precipitationProbability = 5;
        }

        if (thunderstorm) {
            thunderstormRisk = "高";
            precipitationProbability = Math.max(precipitationProbability, 90);
            precipitationIntensity = Math.max(precipitationIntensity, 2.0);
            visibility = Math.min(visibility, 2.0);
        }

        RiskDescriptor riskDescriptor = buildThunderstormRiskDescriptor(
                thunderstorm,
                round(windSpeed),
                round(visibility),
                precipitationProbability,
                round(precipitationIntensity)
        );

        return new DerivedMetrics(
                round(windSpeed),
                round(visibility),
                precipitationProbability,
                round(precipitationIntensity),
                riskDescriptor.level,
                riskDescriptor.label,
                riskDescriptor.hint,
                riskDescriptor.protectionAdvice
        );
    }

    static PlatformModels.FlightSuitabilityView buildSuitabilityView(DerivedMetrics metrics) {
        boolean windOk = metrics.windSpeed < 10.0;
        boolean visibilityOk = metrics.visibility > 5.0;
        boolean precipitationOk = metrics.precipitationIntensity < 0.5;
        boolean thunderstormOk = metrics.thunderstormRiskLevel <= 3;
        boolean suitable = windOk && visibilityOk && precipitationOk && thunderstormOk;

        PlatformModels.FlightSuitabilityView view = new PlatformModels.FlightSuitabilityView();
        view.result = suitable ? "适宜飞行" : "不适宜飞行";
        view.level = suitable ? "绿色窗口" : "风险预警";
        view.summary = suitable
                ? "风速、能见度、降水强度与雷暴风险均满足当前飞行安全阈值。"
                : "至少一项关键气象指标超出飞行安全阈值，当前不建议执行飞行任务。";
        view.checks = Arrays.asList(
                buildCheck("风速", formatMetric(metrics.windSpeed, "m/s"), "< 10m/s", windOk),
                buildCheck("能见度", formatMetric(metrics.visibility, "km"), "> 5km", visibilityOk),
                buildCheck("降水强度", formatMetric(metrics.precipitationIntensity, "mm/h"), "< 0.5mm/h", precipitationOk),
                buildCheck("雷暴风险", formatRisk(metrics), "1-3级", thunderstormOk)
        );
        view.conditionNotes = Arrays.asList(
                windOk ? "风速处于安全阈值内。" : "当前风速超过 10m/s，存在姿态控制风险。",
                visibilityOk ? "能见度满足基本目视飞行要求。" : "能见度低于 5km，不满足稳妥起飞条件。",
                precipitationOk ? "降水强度较低，对起降影响可控。" : "降水强度高于 0.5mm/h，建议暂停任务。",
                thunderstormOk ? "雷暴等级处于低风险窗口，可继续观测云团变化。" : metrics.thunderstormRiskHint
        );
        view.recommendations = suitable
                ? Arrays.asList(
                        "当前气象窗口可执行常规飞行任务，建议起飞前复核空域审批与电池状态。",
                        "持续关注首页天气模块，系统会每 15 分钟自动刷新一次气象判断。"
                )
                : Arrays.asList(
                        windOk ? "保持低空待命并继续观测近地风变化。" : "建议等待风速回落到 10m/s 以下后再评估起飞。",
                        visibilityOk ? "保持视距飞行边界，不建议扩大作业半径。" : "建议延后任务，待能见度恢复至 5km 以上。",
                        precipitationOk ? "如需紧急作业，请同步确认机体防水等级。" : "建议暂停飞行，避免降水影响机体与图传链路。",
                        thunderstormOk ? "继续关注云团与地面站告警。" : metrics.thunderstormProtectionAdvice
                );
        return view;
    }

    private static RiskDescriptor buildThunderstormRiskDescriptor(
            boolean thunderstorm,
            double windSpeed,
            double visibility,
            int precipitationProbability,
            double precipitationIntensity
    ) {
        int score = 1;
        if (thunderstorm) {
            score += 4;
        }
        if (precipitationProbability >= 90) {
            score += 2;
        } else if (precipitationProbability >= 55) {
            score += 1;
        }
        if (precipitationIntensity >= 2.0) {
            score += 2;
        } else if (precipitationIntensity >= 0.5) {
            score += 1;
        }
        if (visibility <= 2.0) {
            score += 1;
        } else if (visibility <= 5.0) {
            score += 1;
        }
        if (windSpeed >= 17.0) {
            score += 2;
        } else if (windSpeed >= 10.0) {
            score += 1;
        }
        int level = Math.max(1, Math.min(9, score));
        String label = THUNDERSTORM_RISK_LABELS[level - 1];
        return new RiskDescriptor(level, label, buildRiskHint(level), buildProtectionAdvice(level));
    }

    private static String buildRiskHint(int level) {
        if (level <= 2) {
            return "雷暴信号极弱，可继续例行监测。";
        }
        if (level <= 4) {
            return "存在轻微对流扰动，建议缩短观察周期并复核预报。";
        }
        if (level <= 6) {
            return "对流条件开始增强，任务执行前需结合空域和备降条件复核。";
        }
        if (level <= 8) {
            return "强对流触发概率较高，建议暂停起飞并将设备撤离暴露区域。";
        }
        return "极端雷暴风险窗口，严禁起飞并立即执行人员与设备防护。";
    }

    private static String buildProtectionAdvice(int level) {
        if (level <= 2) {
            return "保持常规值守，继续关注近 30 分钟内雷达回波与云团发展。";
        }
        if (level <= 4) {
            return "缩短复测周期，起飞前再次确认风速和回波强度。";
        }
        if (level <= 6) {
            return "优先安排地面准备，必要时收紧飞行半径并准备备降点。";
        }
        if (level <= 8) {
            return "暂停飞行，人员和设备应远离空旷暴露区域并切断非必要作业。";
        }
        return "立即停飞并执行防雷避险，全部设备应断电入库并等待风险解除。";
    }

    private static String formatRisk(DerivedMetrics metrics) {
        return metrics.thunderstormRiskLevel + "级 " + metrics.thunderstormRisk;
    }

    private static PlatformModels.FlightConditionCheck buildCheck(String label, String currentValue, String threshold, boolean passed) {
        PlatformModels.FlightConditionCheck check = new PlatformModels.FlightConditionCheck();
        check.label = label;
        check.currentValue = currentValue;
        check.threshold = threshold;
        check.passed = passed;
        return check;
    }

    private static String normalizeWeather(String weather) {
        return weather == null ? "" : weather.trim();
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static double parseWindSpeed(String windPowerText) {
        if (windPowerText == null || windPowerText.trim().isEmpty()) {
            return 0.0;
        }
        String normalized = windPowerText.replace("≤", "").replace("级", "").trim();
        String[] segments = normalized.split("-");
        try {
            int level = Integer.parseInt(segments[segments.length - 1].trim());
            Double value = WIND_SPEED_LEVELS.get(level);
            return value == null ? 0.0 : value;
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private static String formatMetric(double value, String unit) {
        return String.format(Locale.ROOT, "%.1f%s", value, unit);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static Map<Integer, Double> buildWindLevels() {
        Map<Integer, Double> levels = new HashMap<>();
        levels.put(0, 0.2);
        levels.put(1, 1.5);
        levels.put(2, 3.3);
        levels.put(3, 5.4);
        levels.put(4, 7.9);
        levels.put(5, 10.7);
        levels.put(6, 13.8);
        levels.put(7, 17.1);
        levels.put(8, 20.7);
        levels.put(9, 24.4);
        levels.put(10, 28.4);
        levels.put(11, 32.6);
        levels.put(12, 36.9);
        return levels;
    }

    static final class DerivedMetrics {
        final double windSpeed;
        final double visibility;
        final int precipitationProbability;
        final double precipitationIntensity;
        final String thunderstormRisk;
        final int thunderstormRiskLevel;
        final String thunderstormRiskHint;
        final String thunderstormProtectionAdvice;

        DerivedMetrics(
                double windSpeed,
                double visibility,
                int precipitationProbability,
                double precipitationIntensity,
                int thunderstormRiskLevel,
                String thunderstormRisk,
                String thunderstormRiskHint,
                String thunderstormProtectionAdvice
        ) {
            this.windSpeed = windSpeed;
            this.visibility = visibility;
            this.precipitationProbability = precipitationProbability;
            this.precipitationIntensity = precipitationIntensity;
            this.thunderstormRiskLevel = thunderstormRiskLevel;
            this.thunderstormRisk = thunderstormRisk;
            this.thunderstormRiskHint = thunderstormRiskHint;
            this.thunderstormProtectionAdvice = thunderstormProtectionAdvice;
        }
    }

    private static final class RiskDescriptor {
        final int level;
        final String label;
        final String hint;
        final String protectionAdvice;

        RiskDescriptor(int level, String label, String hint, String protectionAdvice) {
            this.level = level;
            this.label = label;
            this.hint = hint;
            this.protectionAdvice = protectionAdvice;
        }
    }
}

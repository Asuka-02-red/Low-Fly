package com.example.low_altitudereststop.feature.home;

import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;

final class WeatherVisuals {

    private WeatherVisuals() {
    }

    @DrawableRes
    static int resolveWeatherIcon(@NonNull PlatformModels.RealtimeWeatherView weather) {
        return resolveWeatherIcon(weather.weatherIconType);
    }

    @DrawableRes
    static int resolveWeatherIcon(String iconType) {
        String safeType = iconType == null ? "" : iconType;
        switch (safeType) {
            case "thunderstorm":
                return R.drawable.ic_weather_thunderstorm;
            case "rain":
                return R.drawable.ic_weather_rain;
            case "snow":
                return R.drawable.ic_weather_snow;
            case "fog":
                return R.drawable.ic_weather_fog;
            case "cloudy":
                return R.drawable.ic_weather_cloudy;
            default:
                return R.drawable.ic_weather_clear;
        }
    }

    @ColorInt
    static int resolveRiskColor(@NonNull Context context, int level) {
        return ContextCompat.getColor(context, resolveRiskColorRes(level));
    }

    @ColorInt
    static int resolveRiskForegroundColor(@NonNull Context context, int level) {
        return ContextCompat.getColor(
                context,
                clampLevel(level) >= 6 ? android.R.color.white : R.color.ui_text_primary
        );
    }

    static int resolveRiskColorRes(int level) {
        switch (clampLevel(level)) {
            case 1:
                return R.color.weather_risk_level_1;
            case 2:
                return R.color.weather_risk_level_2;
            case 3:
                return R.color.weather_risk_level_3;
            case 4:
                return R.color.weather_risk_level_4;
            case 5:
                return R.color.weather_risk_level_5;
            case 6:
                return R.color.weather_risk_level_6;
            case 7:
                return R.color.weather_risk_level_7;
            case 8:
                return R.color.weather_risk_level_8;
            default:
                return R.color.weather_risk_level_9;
        }
    }

    @NonNull
    static String formatRiskLevel(@NonNull PlatformModels.RealtimeWeatherView weather) {
        return clampLevel(weather.thunderstormRiskLevel) + "级";
    }

    @NonNull
    static String formatRiskSummary(@NonNull PlatformModels.RealtimeWeatherView weather) {
        String label = weather.thunderstormRiskLabel == null || weather.thunderstormRiskLabel.trim().isEmpty()
                ? "未知"
                : weather.thunderstormRiskLabel;
        return formatRiskLevel(weather) + " " + label;
    }

    @NonNull
    static String formatWeatherHeadline(@NonNull PlatformModels.RealtimeWeatherView weather) {
        String current = weather.weather == null ? "" : weather.weather.trim();
        if (current.isEmpty()) {
            return "天气状态";
        }
        return current;
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(9, level));
    }
}

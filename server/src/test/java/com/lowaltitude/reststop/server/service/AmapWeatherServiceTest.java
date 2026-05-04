package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AmapWeatherServiceTest {

    @Test
    public void shouldMarkSunnyLightWindAsSuitable() {
        AmapWeatherService.DerivedMetrics metrics = AmapWeatherService.deriveMetrics("晴", "晴", "多云", "3");

        ApiDtos.AdminFlightSuitabilityView suitability = AmapWeatherService.buildSuitabilityView(metrics);

        Assertions.assertEquals("适宜飞行", suitability.result());
        Assertions.assertTrue(metrics.windSpeed() < 10.0);
        Assertions.assertTrue(metrics.visibility() > 5.0);
        Assertions.assertTrue(metrics.precipitationIntensity() < 0.5);
        Assertions.assertEquals("低", metrics.thunderstormRisk());
    }

    @Test
    public void shouldRejectThunderstormWeather() {
        AmapWeatherService.DerivedMetrics metrics = AmapWeatherService.deriveMetrics("雷阵雨", "雷阵雨", "中雨", "4");

        ApiDtos.AdminFlightSuitabilityView suitability = AmapWeatherService.buildSuitabilityView(metrics);

        Assertions.assertEquals("不适宜飞行", suitability.result());
        Assertions.assertEquals("高", metrics.thunderstormRisk());
        Assertions.assertTrue(metrics.precipitationIntensity() >= 0.5);
    }

    @Test
    public void shouldRejectStrongWindEvenWithoutRain() {
        AmapWeatherService.DerivedMetrics metrics = AmapWeatherService.deriveMetrics("多云", "多云", "晴", "5");

        ApiDtos.AdminFlightSuitabilityView suitability = AmapWeatherService.buildSuitabilityView(metrics);

        Assertions.assertEquals("不适宜飞行", suitability.result());
        Assertions.assertTrue(metrics.windSpeed() >= 10.0);
    }

    @Test
    public void shouldTranslateProviderKeyMismatchMessage() {
        String translated = AmapWeatherService.translateProviderMessage("INVALID HOST", "默认错误");

        Assertions.assertEquals("和风天气 API Host 无效，请在控制台设置中复制正确的 API Host。", translated);
    }
}

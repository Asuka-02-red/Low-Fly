package com.example.low_altitudereststop.feature.home;

import com.example.low_altitudereststop.core.model.PlatformModels;
import org.junit.Assert;
import org.junit.Test;

public class WeatherFlightEvaluatorTest {

    @Test
    public void shouldMarkFlightSuitableWhenMetricsMeetThresholds() {
        WeatherFlightEvaluator.DerivedMetrics metrics =
                WeatherFlightEvaluator.deriveMetrics("晴", "晴", "多云", "3级");

        PlatformModels.FlightSuitabilityView result = WeatherFlightEvaluator.buildSuitabilityView(metrics);

        Assert.assertEquals("适宜飞行", result.result);
        Assert.assertEquals(4, result.checks.size());
        Assert.assertTrue(result.checks.get(0).passed);
        Assert.assertTrue(result.checks.get(1).passed);
        Assert.assertTrue(result.checks.get(2).passed);
        Assert.assertTrue(result.checks.get(3).passed);
    }

    @Test
    public void shouldMarkFlightUnsafeWhenThunderstormAndHeavyRainPresent() {
        WeatherFlightEvaluator.DerivedMetrics metrics =
                WeatherFlightEvaluator.deriveMetrics("雷阵雨", "大雨", "雷阵雨", "6级");

        PlatformModels.FlightSuitabilityView result = WeatherFlightEvaluator.buildSuitabilityView(metrics);

        Assert.assertEquals("不适宜飞行", result.result);
        Assert.assertEquals("极高", metrics.thunderstormRisk);
        Assert.assertTrue(metrics.precipitationIntensity >= 2.0);
        Assert.assertFalse(result.checks.get(0).passed);
        Assert.assertFalse(result.checks.get(1).passed);
        Assert.assertFalse(result.checks.get(2).passed);
        Assert.assertFalse(result.checks.get(3).passed);
    }

    @Test
    public void shouldTreatHeavyWindAsUnsafeEvenWithoutRain() {
        WeatherFlightEvaluator.DerivedMetrics metrics =
                WeatherFlightEvaluator.deriveMetrics("晴", "晴", "晴", "7级");

        PlatformModels.FlightSuitabilityView result = WeatherFlightEvaluator.buildSuitabilityView(metrics);

        Assert.assertEquals("不适宜飞行", result.result);
        Assert.assertTrue(metrics.windSpeed >= 12.0d);
        Assert.assertFalse(result.checks.get(0).passed);
    }

    @Test
    public void shouldTreatFogAsUnsafeBecauseVisibilityIsLow() {
        WeatherFlightEvaluator.DerivedMetrics metrics =
                WeatherFlightEvaluator.deriveMetrics("雾", "阴", "雾", "2级");

        PlatformModels.FlightSuitabilityView result = WeatherFlightEvaluator.buildSuitabilityView(metrics);

        Assert.assertEquals("不适宜飞行", result.result);
        Assert.assertTrue(metrics.visibility <= 3.0d);
        Assert.assertFalse(result.checks.get(1).passed);
    }
}

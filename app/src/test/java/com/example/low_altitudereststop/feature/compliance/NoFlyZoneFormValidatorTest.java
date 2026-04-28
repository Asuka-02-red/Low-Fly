package com.example.low_altitudereststop.feature.compliance;

import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Test;

public class NoFlyZoneFormValidatorTest {

    @Test
    public void shouldValidateCompleteZoneRecord() {
        FlightManagementModels.NoFlyZoneRecord record = new FlightManagementModels.NoFlyZoneRecord();
        record.name = "测试禁飞区";
        record.zoneType = "FORBIDDEN";
        record.centerLat = BigDecimal.valueOf(29.56);
        record.centerLng = BigDecimal.valueOf(106.55);
        record.radius = 800;
        record.effectiveStart = "2026-04-22 08:00";
        record.effectiveEnd = "2026-04-30 18:00";
        record.reason = "活动保障";

        NoFlyZoneFormValidator.Result result = NoFlyZoneFormValidator.validate(record);

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void shouldRejectInvalidCoordinatesAndTimeRange() {
        FlightManagementModels.NoFlyZoneRecord record = new FlightManagementModels.NoFlyZoneRecord();
        record.name = "测试禁飞区";
        record.zoneType = "FORBIDDEN";
        record.centerLat = BigDecimal.valueOf(120);
        record.centerLng = BigDecimal.valueOf(200);
        record.radius = 50;
        record.effectiveStart = "2026-04-30 18:00";
        record.effectiveEnd = "2026-04-22 08:00";
        record.reason = "";

        NoFlyZoneFormValidator.Result result = NoFlyZoneFormValidator.validate(record);

        Assert.assertFalse(result.isValid());
        Assert.assertEquals("纬度范围应在 -90 到 90 之间", result.errorFor("centerLat"));
        Assert.assertEquals("经度范围应在 -180 到 180 之间", result.errorFor("centerLng"));
        Assert.assertEquals("管控半径至少为 100 米", result.errorFor("radius"));
        Assert.assertEquals("结束时间必须晚于开始时间", result.errorFor("effectiveRange"));
        Assert.assertEquals("请填写禁飞原因说明", result.errorFor("reason"));
    }
}

package com.example.low_altitudereststop.feature.compliance;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NoFlyZoneFormValidator {

    private NoFlyZoneFormValidator() {
    }

    public static Result validate(FlightManagementModels.NoFlyZoneRecord record) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (record == null) {
            errors.put("record", "禁飞区信息不能为空");
            return new Result(errors);
        }
        if (isBlank(record.name)) {
            errors.put("name", "请填写禁飞区名称");
        }
        if (isBlank(record.zoneType)) {
            errors.put("zoneType", "请填写限制类型");
        }
        if (record.centerLat == null || record.centerLat.compareTo(BigDecimal.valueOf(-90)) < 0
                || record.centerLat.compareTo(BigDecimal.valueOf(90)) > 0) {
            errors.put("centerLat", "纬度范围应在 -90 到 90 之间");
        }
        if (record.centerLng == null || record.centerLng.compareTo(BigDecimal.valueOf(-180)) < 0
                || record.centerLng.compareTo(BigDecimal.valueOf(180)) > 0) {
            errors.put("centerLng", "经度范围应在 -180 到 180 之间");
        }
        if (record.radius < 100) {
            errors.put("radius", "管控半径至少为 100 米");
        }
        if (isBlank(record.effectiveStart)) {
            errors.put("effectiveStart", "请填写生效开始时间");
        }
        if (isBlank(record.effectiveEnd)) {
            errors.put("effectiveEnd", "请填写生效结束时间");
        }
        if (!isBlank(record.effectiveStart) && !isBlank(record.effectiveEnd)
                && record.effectiveStart.compareTo(record.effectiveEnd) > 0) {
            errors.put("effectiveRange", "结束时间必须晚于开始时间");
        }
        if (isBlank(record.reason)) {
            errors.put("reason", "请填写禁飞原因说明");
        }
        return new Result(errors);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class Result {
        private final Map<String, String> errors;

        Result(Map<String, String> errors) {
            this.errors = errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public String errorFor(String field) {
            return errors.get(field);
        }
    }
}

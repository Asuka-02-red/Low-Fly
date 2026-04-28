package com.lowaltitude.reststop.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AmapWeatherService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<Integer, Double> WIND_SPEED_LEVELS = Map.ofEntries(
            Map.entry(0, 0.2),
            Map.entry(1, 1.5),
            Map.entry(2, 3.3),
            Map.entry(3, 5.4),
            Map.entry(4, 7.9),
            Map.entry(5, 10.7),
            Map.entry(6, 13.8),
            Map.entry(7, 17.1),
            Map.entry(8, 20.7),
            Map.entry(9, 24.4),
            Map.entry(10, 28.4),
            Map.entry(11, 32.6),
            Map.entry(12, 36.9)
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final String apiKey;
    private final Duration cacheTtl;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public AmapWeatherService(
            ObjectMapper objectMapper,
            @Value("${weather.amap.service-name:实时天气}") String serviceName,
            @Value("${weather.amap.realtime-key:}") String apiKey,
            @Value("${weather.amap.cache-minutes:15}") long cacheMinutes
    ) {
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        this.apiKey = apiKey;
        this.cacheTtl = Duration.ofMinutes(Math.max(cacheMinutes, 1));
        this.restClient = RestClient.builder()
                .baseUrl("https://restapi.amap.com")
                .build();
    }

    public ApiDtos.AdminRealtimeWeatherView getRealtimeWeather(BigDecimal longitude, BigDecimal latitude) {
        validateCoordinates(longitude, latitude);
        ensureApiKey();

        ResolvedLocation resolvedLocation = resolveLocation(longitude, latitude);
        CachedWeather cachedWeather = cache.get(resolvedLocation.adcode());
        if (cachedWeather != null && !cachedWeather.isExpired(cacheTtl)) {
            return cachedWeather.payload();
        }

        JsonNode live = loadLiveWeather(resolvedLocation.adcode());
        JsonNode forecast = loadForecastWeather(resolvedLocation.adcode());
        ApiDtos.AdminRealtimeWeatherView payload = mapWeatherPayload(resolvedLocation, live, forecast);
        cache.put(resolvedLocation.adcode(), new CachedWeather(payload, LocalDateTime.now()));
        return payload;
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

        return new DerivedMetrics(
                round(windSpeed),
                round(visibility),
                precipitationProbability,
                round(precipitationIntensity),
                thunderstormRisk
        );
    }

    static ApiDtos.AdminFlightSuitabilityView buildSuitabilityView(DerivedMetrics metrics) {
        boolean windOk = metrics.windSpeed() < 10.0;
        boolean visibilityOk = metrics.visibility() > 5.0;
        boolean precipitationOk = metrics.precipitationIntensity() < 0.5;
        boolean thunderstormOk = "低".equals(metrics.thunderstormRisk());
        boolean suitable = windOk && visibilityOk && precipitationOk && thunderstormOk;

        List<ApiDtos.AdminFlightConditionCheck> checks = List.of(
                new ApiDtos.AdminFlightConditionCheck("风速", formatMetric(metrics.windSpeed(), "m/s"), "< 10m/s", windOk),
                new ApiDtos.AdminFlightConditionCheck("能见度", formatMetric(metrics.visibility(), "km"), "> 5km", visibilityOk),
                new ApiDtos.AdminFlightConditionCheck("降水强度", formatMetric(metrics.precipitationIntensity(), "mm/h"), "< 0.5mm/h", precipitationOk),
                new ApiDtos.AdminFlightConditionCheck("雷暴风险", metrics.thunderstormRisk(), "低风险", thunderstormOk)
        );

        List<String> reasons = List.of(
                windOk ? "风速处于安全阈值内。" : "当前风速超过 10m/s，存在姿态控制风险。",
                visibilityOk ? "能见度满足基本目视飞行要求。" : "能见度低于 5km，不满足稳妥起飞条件。",
                precipitationOk ? "降水强度较低，对起降影响可控。" : "降水强度高于 0.5mm/h，建议暂停任务。",
                thunderstormOk ? "未识别到显著雷暴风险。" : "识别到雷暴或强对流信号，应立即停止飞行。"
        );

        List<String> recommendations = suitable
                ? List.of(
                        "当前气象窗口可执行常规飞行任务，建议起飞前复核空域审批与电池状态。",
                        "持续关注首页天气模块，系统会每 15 分钟自动刷新一次气象判断。"
                )
                : List.of(
                        windOk ? "保持低空待命并继续观测近地风变化。" : "建议等待风速回落到 10m/s 以下后再评估起飞。",
                        visibilityOk ? "保持视距飞行边界，不建议扩大作业半径。" : "建议延后任务，待能见度恢复至 5km 以上。",
                        precipitationOk ? "如需紧急作业，请同步确认机体防水等级。" : "建议暂停飞行，避免降水影响机体与图传链路。",
                        thunderstormOk ? "继续关注云团与地面站告警。" : "存在雷暴风险时严禁起飞，人员和设备应远离空旷暴露区域。"
                );

        String summary = suitable
                ? "风速、能见度、降水强度与雷暴风险均满足当前飞行安全阈值。"
                : "至少一项关键气象指标超出飞行安全阈值，当前不建议执行飞行任务。";

        return new ApiDtos.AdminFlightSuitabilityView(
                suitable ? "适宜飞行" : "不适宜飞行",
                suitable ? "绿色窗口" : "红色预警",
                summary,
                checks,
                reasons,
                recommendations
        );
    }

    private ApiDtos.AdminRealtimeWeatherView mapWeatherPayload(ResolvedLocation location, JsonNode live, JsonNode forecast) {
        String liveWeather = textValue(live, "weather");
        String dayWeather = "";
        String nightWeather = "";

        JsonNode casts = forecast.path("casts");
        if (casts.isArray() && !casts.isEmpty()) {
            JsonNode today = casts.get(0);
            dayWeather = textValue(today, "dayweather");
            nightWeather = textValue(today, "nightweather");
        }

        DerivedMetrics derivedMetrics = deriveMetrics(liveWeather, dayWeather, nightWeather, textValue(live, "windpower"));
        ApiDtos.AdminFlightSuitabilityView suitability = buildSuitabilityView(derivedMetrics);

        return new ApiDtos.AdminRealtimeWeatherView(
                serviceName,
                location.displayName(),
                location.adcode(),
                liveWeather,
                textValue(live, "reporttime"),
                TIME_FORMATTER.format(LocalDateTime.now()),
                "15 分钟自动刷新",
                parseDouble(textValue(live, "temperature")),
                parseInteger(textValue(live, "humidity")),
                textValue(live, "winddirection"),
                textValue(live, "windpower"),
                derivedMetrics.windSpeed(),
                derivedMetrics.visibility(),
                derivedMetrics.precipitationProbability(),
                derivedMetrics.precipitationIntensity(),
                derivedMetrics.thunderstormRisk(),
                "温度、湿度、风向和风力来自高德实时天气；能见度、降水概率、降水强度与雷暴风险依据高德天气现象和当日预报进行推导。",
                suitability
        );
    }

    private ResolvedLocation resolveLocation(BigDecimal longitude, BigDecimal latitude) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/geocode/regeo")
                            .queryParam("key", apiKey)
                            .queryParam("location", longitude + "," + latitude)
                            .queryParam("extensions", "base")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = readTree(response, "地理反编码结果解析失败");
            ensureSuccess(root, "定位当前位置失败，请稍后重试。");

            JsonNode regeocode = root.path("regeocode");
            JsonNode addressComponent = regeocode.path("addressComponent");
            String adcode = textValue(addressComponent, "adcode");
            if (adcode.isBlank()) {
                throw new BizException(502, "当前位置缺少区域编码，暂时无法查询天气。");
            }

            String province = textValue(addressComponent, "province");
            String city = normalizeCity(addressComponent.path("city"), province);
            String district = textValue(addressComponent, "district");
            String displayName = String.join(" ", province, city, district).trim().replaceAll("\\s+", " ");

            return new ResolvedLocation(
                    adcode,
                    displayName.isBlank() ? "当前位置" : displayName
            );
        } catch (RestClientException exception) {
            throw new BizException(502, "定位当前位置失败，请检查网络后重试。");
        }
    }

    private JsonNode loadLiveWeather(String adcode) {
        return loadWeatherNode(adcode, "base", "lives", "实时天气服务暂时不可用，请稍后重试。");
    }

    private JsonNode loadForecastWeather(String adcode) {
        return loadWeatherNode(adcode, "all", "casts", "天气预报服务暂时不可用，请稍后重试。");
    }

    private JsonNode loadWeatherNode(String adcode, String extensions, String nodeName, String errorMessage) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/weather/weatherInfo")
                            .queryParam("key", apiKey)
                            .queryParam("city", adcode)
                            .queryParam("extensions", extensions)
                            .queryParam("output", "JSON")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = readTree(response, "天气结果解析失败");
            ensureSuccess(root, errorMessage);
            JsonNode nodes = root.path(nodeName);
            if (!nodes.isArray() || nodes.isEmpty()) {
                throw new BizException(502, errorMessage);
            }
            return nodes.get(0);
        } catch (RestClientException exception) {
            throw new BizException(502, errorMessage);
        }
    }

    private JsonNode readTree(String body, String fallbackMessage) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception exception) {
            throw new BizException(502, fallbackMessage);
        }
    }

    private void ensureSuccess(JsonNode root, String defaultMessage) {
        if (!"1".equals(textValue(root, "status"))) {
            String message = textValue(root, "info");
            throw new BizException(502, translateProviderMessage(message, defaultMessage));
        }
    }

    static String translateProviderMessage(String providerMessage, String defaultMessage) {
        String message = providerMessage == null ? "" : providerMessage.trim();
        if (message.isBlank()) {
            return defaultMessage;
        }
        return switch (message) {
            case "USERKEY_PLAT_NOMATCH" -> "实时天气服务鉴权失败，请检查高德 Key 的平台类型配置。";
            case "INVALID_USER_KEY" -> "实时天气服务密钥无效，请联系管理员检查配置。";
            case "SERVICE_NOT_AVAILABLE" -> "实时天气服务暂时不可用，请稍后重试。";
            case "DAILY_QUERY_OVER_LIMIT", "ACCESS_TOO_FREQUENT" -> "实时天气服务请求过于频繁，请稍后再试。";
            default -> message;
        };
    }

    private void validateCoordinates(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new BizException(400, "缺少定位坐标，无法查询天气。");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BizException(400, "经度超出合法范围。");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BizException(400, "纬度超出合法范围。");
        }
    }

    private void ensureApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(500, "实时天气服务未配置，请联系管理员。");
        }
    }

    private static String normalizeWeather(String weather) {
        return weather == null ? "" : weather.trim();
    }

    private static String normalizeCity(JsonNode cityNode, String province) {
        if (cityNode == null || cityNode.isMissingNode() || cityNode.isNull()) {
            return province;
        }
        if (cityNode.isArray()) {
            return cityNode.isEmpty() ? province : cityNode.get(0).asText(province);
        }
        String value = cityNode.asText("");
        return value.isBlank() ? province : value;
    }

    private static String textValue(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? "" : field.asText("");
    }

    private static double parseWindSpeed(String windPowerText) {
        if (windPowerText == null || windPowerText.isBlank()) {
            return 0.0;
        }

        String normalized = windPowerText.replace("≤", "").replace("级", "").trim();
        String[] segments = normalized.split("-");
        try {
            int level = Integer.parseInt(segments[segments.length - 1].trim());
            return WIND_SPEED_LEVELS.getOrDefault(level, 0.0);
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return round(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private static int parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String formatMetric(double value, String unit) {
        return String.format(Locale.ROOT, "%.1f%s", value, unit);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    record DerivedMetrics(
            double windSpeed,
            double visibility,
            int precipitationProbability,
            double precipitationIntensity,
            String thunderstormRisk
    ) {
    }

    private record ResolvedLocation(String adcode, String displayName) {
    }

    private record CachedWeather(ApiDtos.AdminRealtimeWeatherView payload, LocalDateTime cachedAt) {
        private boolean isExpired(Duration ttl) {
            return cachedAt.plus(ttl).isBefore(LocalDateTime.now());
        }
    }
}

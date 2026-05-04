package com.lowaltitude.reststop.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 和风天气服务。
 * <p>
 * 封装和风天气API的调用逻辑，提供实时天气查询功能，
 * 包括天气实况获取、分钟级降水查询、地理编码解析、
 * 飞行适宜性评估及天气数据缓存管理。
 * </p>
 */
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
    private static final String DEFAULT_SOURCE_NOTE = "天气、温度、湿度、风向、风速与能见度来自和风天气实时天气；降水风险依据和风分钟级降水与实时天气现象综合推导。";
    private static final String REALTIME_FALLBACK_SOURCE_NOTE = "天气、温度、湿度、风向、风速与能见度来自和风天气实时天气；当前分钟级降水不可用时，降水风险依据实时天气现象推导。";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final String apiHost;
    private final String apiKey;
    private final String credentialId;
    private final Duration cacheTtl;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public AmapWeatherService(
            ObjectMapper objectMapper,
            @Value("${weather.qweather.service-name:和风天气}") String serviceName,
            @Value("${weather.qweather.api-host:}") String apiHost,
            @Value("${weather.qweather.api-key:}") String apiKey,
            @Value("${weather.qweather.credential-id:}") String credentialId,
            @Value("${weather.qweather.cache-minutes:15}") long cacheMinutes
    ) {
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        this.apiHost = stripTrailingSlash(apiHost);
        this.apiKey = apiKey;
        this.credentialId = credentialId;
        this.cacheTtl = Duration.ofMinutes(Math.max(cacheMinutes, 1));
        this.restClient = RestClient.builder().build();
    }

    public ApiDtos.AdminRealtimeWeatherView getRealtimeWeather(BigDecimal longitude, BigDecimal latitude) {
        validateCoordinates(longitude, latitude);
        ensureConfiguration();

        String locationParam = buildLocationParam(longitude, latitude);
        CachedWeather cachedWeather = cache.get(locationParam);
        if (cachedWeather != null && !cachedWeather.isExpired(cacheTtl)) {
            return cachedWeather.payload();
        }

        ResolvedLocation resolvedLocation = resolveLocation(locationParam);
        JsonNode weatherRoot = requestJson(
                "/v7/weather/now",
                Map.of("location", locationParam, "lang", "zh", "unit", "m"),
                "和风实时天气服务暂时不可用，请稍后重试。",
                false,
                false
        );
        JsonNode minutelyRoot = requestJson(
                "/v7/minutely/5m",
                Map.of("location", locationParam, "lang", "zh"),
                "和风分钟级降水服务暂时不可用，请稍后重试。",
                true,
                true
        );

        ApiDtos.AdminRealtimeWeatherView payload = mapWeatherPayload(resolvedLocation, locationParam, weatherRoot, minutelyRoot);
        cache.put(locationParam, new CachedWeather(payload, LocalDateTime.now()));
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

    private ApiDtos.AdminRealtimeWeatherView mapWeatherPayload(
            ResolvedLocation location,
            String locationParam,
            JsonNode weatherRoot,
            JsonNode minutelyRoot
    ) {
        JsonNode now = weatherRoot.path("now");
        String liveWeather = textValue(now, "text");
        DerivedMetrics heuristicMetrics = deriveMetrics(liveWeather, liveWeather, liveWeather, textValue(now, "windScale"));
        MinutelyMetrics minutelyMetrics = extractMinutelyMetrics(minutelyRoot, liveWeather, parseDouble(textValue(now, "precip")));

        double actualWindSpeed = parseDouble(textValue(now, "windSpeed"));
        double windSpeedMetersPerSecond = actualWindSpeed > 0.0 ? round(actualWindSpeed / 3.6d) : heuristicMetrics.windSpeed();
        double visibility = parseDouble(textValue(now, "vis"));
        double finalVisibility = visibility > 0.0 ? round(visibility) : heuristicMetrics.visibility();
        double precipitationIntensity = minutelyMetrics == null
                ? Math.max(round(parseDouble(textValue(now, "precip"))), heuristicMetrics.precipitationIntensity())
                : minutelyMetrics.precipitationIntensity();
        int precipitationProbability = minutelyMetrics == null
                ? heuristicMetrics.precipitationProbability()
                : minutelyMetrics.precipitationProbability();
        String thunderstormRisk = resolveThunderstormRisk(liveWeather, minutelyMetrics, heuristicMetrics.thunderstormRisk());
        DerivedMetrics finalMetrics = new DerivedMetrics(
                windSpeedMetersPerSecond,
                finalVisibility,
                precipitationProbability,
                precipitationIntensity,
                thunderstormRisk
        );
        ApiDtos.AdminFlightSuitabilityView suitability = buildSuitabilityView(finalMetrics);

        return new ApiDtos.AdminRealtimeWeatherView(
                serviceName,
                location.displayName(),
                location.adcode().isBlank() ? locationParam : location.adcode(),
                liveWeather,
                formatProviderTime(textValue(now, "obsTime")),
                TIME_FORMATTER.format(LocalDateTime.now()),
                "15 分钟自动刷新",
                parseDouble(textValue(now, "temp")),
                parseInteger(textValue(now, "humidity")),
                textValue(now, "windDir"),
                formatWindScale(textValue(now, "windScale")),
                finalMetrics.windSpeed(),
                finalMetrics.visibility(),
                finalMetrics.precipitationProbability(),
                finalMetrics.precipitationIntensity(),
                finalMetrics.thunderstormRisk(),
                minutelyMetrics == null ? REALTIME_FALLBACK_SOURCE_NOTE : DEFAULT_SOURCE_NOTE,
                suitability
        );
    }

    private ResolvedLocation resolveLocation(String locationParam) {
        try {
            JsonNode root = requestJson(
                    "/geo/v2/city/lookup",
                    Map.of("location", locationParam, "number", "1", "lang", "zh"),
                    "当前位置解析失败，已回退为坐标显示。",
                    true,
                    true
            );
            if (root == null) {
                return new ResolvedLocation("", formatCoordinateLabel(locationParam));
            }
            JsonNode locations = root.path("location");
            if (!locations.isArray() || locations.isEmpty()) {
                return new ResolvedLocation("", formatCoordinateLabel(locationParam));
            }
            JsonNode item = locations.get(0);
            String id = textValue(item, "id");
            return new ResolvedLocation(id, buildLocationDisplayName(item, locationParam));
        } catch (BizException exception) {
            return new ResolvedLocation("", formatCoordinateLabel(locationParam));
        }
    }

    private JsonNode requestJson(
            String path,
            Map<String, String> queryParameters,
            String defaultErrorMessage,
            boolean allowNoData,
            boolean optionalFeature
    ) {
        try {
            ProviderResponse response = restClient.get()
                    .uri(buildRequestUri(path, queryParameters))
                    .header("X-QW-Api-Key", apiKey)
                    .header("Accept", "application/json")
                    .header("Accept-Encoding", "identity")
                    .exchange((request, clientResponse) -> new ProviderResponse(
                            clientResponse.getStatusCode().value(),
                            readResponseBody(clientResponse.getBody(), clientResponse.getHeaders().get("Content-Encoding"))
                    ));
            JsonNode root = readTree(response.body(), defaultErrorMessage);
            if (response.statusCode() >= 400) {
                if (optionalFeature && isIgnorableOptionalError(root, response.statusCode())) {
                    return null;
                }
                throw new BizException(502, translateProviderHttpError(root, response.statusCode(), defaultErrorMessage));
            }
            String code = textValue(root, "code");
            if (!code.isBlank() && !"200".equals(code)) {
                if ("204".equals(code) && allowNoData) {
                    return null;
                }
                if (optionalFeature && isIgnorableOptionalCode(code)) {
                    return null;
                }
                throw new BizException(502, translateProviderMessage(code, defaultErrorMessage));
            }
            return root;
        } catch (BizException exception) {
            throw exception;
        } catch (RestClientException | IllegalStateException exception) {
            if (optionalFeature) {
                return null;
            }
            throw new BizException(502, defaultErrorMessage);
        }
    }

    static String translateProviderMessage(String providerMessage, String defaultMessage) {
        String message = providerMessage == null ? "" : providerMessage.trim();
        if (message.isBlank()) {
            return defaultMessage;
        }
        String normalized = message.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "INVALID HOST", "INVALID_HOST" -> "和风天气 API Host 无效，请在控制台设置中复制正确的 API Host。";
            case "UNAUTHORIZED", "401" -> "和风天气 API Key 无效，请检查 API Key 或鉴权方式。";
            case "NO CREDIT", "402" -> "和风天气可用额度不足，请检查订阅或账户余额。";
            case "FORBIDDEN", "403", "SECURITY RESTRICTION" -> "和风天气请求被拒绝，请检查 Host、凭据安全限制或接口权限。";
            case "TOO MANY REQUESTS", "429", "OVER MONTHLY LIMIT" -> "和风天气请求过于频繁或已超限，请稍后重试。";
            case "NO SUCH LOCATION", "404" -> "和风天气未识别当前位置，请检查坐标格式或改用附近有效坐标。";
            case "DATA NOT AVAILABLE", "204" -> "当前位置暂时没有可用的天气数据。";
            default -> message;
        };
    }

    private String translateProviderHttpError(JsonNode root, int statusCode, String defaultMessage) {
        JsonNode error = root.path("error");
        String title = textValue(error, "title");
        String detail = textValue(error, "detail");
        String message = translateProviderMessage(title, defaultMessage);
        if (message.equals(title) && !detail.isBlank()) {
            message = detail;
        }
        if (!message.equals(defaultMessage) && !detail.isBlank() && !detail.equalsIgnoreCase(title)) {
            return message + " " + detail;
        }
        if (!message.equals(defaultMessage)) {
            return message;
        }
        return switch (statusCode) {
            case 401 -> "和风天气鉴权失败，请检查 API Key。";
            case 403 -> "和风天气请求被拒绝，请检查 API Host、账号权限或安全限制。";
            case 404 -> "和风天气接口路径无效，请检查 API Host 和请求路径。";
            case 429 -> "和风天气请求过于频繁，请稍后再试。";
            default -> defaultMessage;
        };
    }

    private boolean isIgnorableOptionalError(JsonNode root, int statusCode) {
        if (statusCode == 404) {
            return true;
        }
        JsonNode error = root.path("error");
        String title = textValue(error, "title").toUpperCase(Locale.ROOT);
        return statusCode == 400
                || "DATA NOT AVAILABLE".equals(title)
                || "NO SUCH LOCATION".equals(title)
                || "FORBIDDEN".equals(title);
    }

    private boolean isIgnorableOptionalCode(String code) {
        return "204".equals(code) || "403".equals(code) || "404".equals(code);
    }

    private String buildRequestUri(String path, Map<String, String> queryParameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiHost).path(path);
        queryParameters.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                builder.queryParam(key, value);
            }
        });
        return builder.build(true).toUriString();
    }

    private static String readResponseBody(InputStream body, List<String> contentEncodings) {
        try {
            byte[] bytes = StreamUtils.copyToByteArray(body);
            boolean gzipEncoded = contentEncodings != null
                    && contentEncodings.stream().anyMatch(value -> value != null && value.toLowerCase(Locale.ROOT).contains("gzip"));
            if (!gzipEncoded) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                return StreamUtils.copyToString(gzipInputStream, StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decode weather response", exception);
        }
    }

    private MinutelyMetrics extractMinutelyMetrics(JsonNode minutelyRoot, String weatherText, double realtimePrecipitation) {
        if (minutelyRoot == null || minutelyRoot.isMissingNode()) {
            return null;
        }
        JsonNode items = minutelyRoot.path("minutely");
        double maxPrecip = 0.0;
        boolean hasSnow = false;
        boolean hasRain = false;
        if (items.isArray()) {
            for (JsonNode item : items) {
                maxPrecip = Math.max(maxPrecip, parseDouble(textValue(item, "precip")));
                String type = textValue(item, "type");
                hasSnow = hasSnow || "snow".equalsIgnoreCase(type);
                hasRain = hasRain || "rain".equalsIgnoreCase(type);
            }
        }
        String summary = textValue(minutelyRoot, "summary");
        double precipitationIntensity = maxPrecip > 0.0 ? round(maxPrecip * 12.0d) : round(realtimePrecipitation);
        int precipitationProbability;
        if (maxPrecip > 0.0) {
            precipitationProbability = 90;
        } else if (hasRain || hasSnow) {
            precipitationProbability = 50;
        } else if (containsAny(summary, "雨", "雪") || containsAny(weatherText, "雨", "雪")) {
            precipitationProbability = 65;
        } else if (containsAny(weatherText, "阴")) {
            precipitationProbability = 25;
        } else if (containsAny(weatherText, "多云")) {
            precipitationProbability = 12;
        } else {
            precipitationProbability = 5;
        }
        return new MinutelyMetrics(
                precipitationProbability,
                precipitationIntensity,
                summary
        );
    }

    private String resolveThunderstormRisk(String weatherText, MinutelyMetrics minutelyMetrics, String fallbackRisk) {
        String summary = minutelyMetrics == null ? "" : minutelyMetrics.summary();
        if (containsAny(weatherText, "雷", "强对流") || containsAny(summary, "雷", "强对流")) {
            return "高";
        }
        return fallbackRisk;
    }

    private String buildLocationDisplayName(JsonNode item, String locationParam) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        addIfPresent(parts, textValue(item, "adm1"));
        addIfPresent(parts, textValue(item, "adm2"));
        addIfPresent(parts, textValue(item, "name"));
        String displayName = String.join(" ", parts).trim();
        return displayName.isBlank() ? formatCoordinateLabel(locationParam) : displayName;
    }

    private void addIfPresent(LinkedHashSet<String> parts, String value) {
        String text = value == null ? "" : value.trim();
        if (!text.isBlank()) {
            parts.add(text);
        }
    }

    private String buildLocationParam(BigDecimal longitude, BigDecimal latitude) {
        return String.format(Locale.ROOT, "%.2f,%.2f", longitude.doubleValue(), latitude.doubleValue());
    }

    private String formatCoordinateLabel(String locationParam) {
        return "当前位置(" + locationParam + ")";
    }

    private String formatProviderTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return TIME_FORMATTER.format(LocalDateTime.now());
        }
        try {
            return TIME_FORMATTER.format(OffsetDateTime.parse(rawTime).toLocalDateTime());
        } catch (Exception ignored) {
            return rawTime.replace('T', ' ').replace("+08:00", "");
        }
    }

    private String formatWindScale(String windScale) {
        String value = windScale == null ? "" : windScale.trim();
        if (value.isBlank()) {
            return "";
        }
        return value.endsWith("级") ? value : value + "级";
    }

    private JsonNode readTree(String body, String fallbackMessage) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception exception) {
            throw new BizException(502, fallbackMessage);
        }
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

    private void ensureConfiguration() {
        if (apiHost == null || apiHost.isBlank()) {
            throw new BizException(500, "和风天气 API Host 未配置，请在控制台设置中复制 API Host 后再启动服务。");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(500, "和风天气 API Key 未配置，请联系管理员。");
        }
        if (credentialId == null || credentialId.isBlank()) {
            return;
        }
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String result = url.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String normalizeWeather(String weather) {
        return weather == null ? "" : weather.trim();
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
        String content = text == null ? "" : text;
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
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

    private record ProviderResponse(int statusCode, String body) {
    }

    private record MinutelyMetrics(int precipitationProbability, double precipitationIntensity, String summary) {
    }
}

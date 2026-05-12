package com.example.low_altitudereststop.feature.home;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiConfig;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.network.ApiService;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;
import retrofit2.Response;

/** 天气数据仓库。 */
public class WeatherRepository {

    private static final String TAG = "WeatherRepository";

    static final long CACHE_TTL_MS = 15 * 60 * 1000L;
    private static final String WEATHER_CACHE_PREFIX = "weather_api_";
    private static final double MIN_TEMPERATURE = -20.0d;
    private static final double MAX_TEMPERATURE = 45.0d;
    private static final int MIN_HUMIDITY = 0;
    private static final int MAX_HUMIDITY = 100;
    private static final double MIN_VISIBILITY = 0.5d;
    private static final double MAX_VISIBILITY = 20.0d;
    private static final int MIN_PRECIPITATION_PROBABILITY = 0;
    private static final int MAX_PRECIPITATION_PROBABILITY = 100;
    private static final double MIN_PRECIPITATION_INTENSITY = 0.0d;
    private static final double MAX_PRECIPITATION_INTENSITY = 50.0d;
    private static final String[] WIND_DIRECTIONS = new String[]{
            "北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"
    };
    private static final WeatherProfile[] WEATHER_PROFILES = new WeatherProfile[]{
            new WeatherProfile("晴", "晴", "少云", -10.0d, 38.0d, 15, 68, "少云"),
            new WeatherProfile("少云", "多云", "晴", -8.0d, 35.0d, 20, 72, "多云"),
            new WeatherProfile("多云", "多云", "阴", -12.0d, 33.0d, 25, 82, "多云"),
            new WeatherProfile("阴", "阴", "多云", -15.0d, 30.0d, 30, 88, "阴"),
            new WeatherProfile("阵雨", "小雨", "阴", 0.0d, 30.0d, 45, 98, "小雨"),
            new WeatherProfile("雷阵雨", "中雨", "雷阵雨", 10.0d, 34.0d, 55, 100, "雷阵雨"),
            new WeatherProfile("小雪", "小雪", "阴", -20.0d, 2.0d, 30, 96, "小雪"),
            new WeatherProfile("雾", "阴", "雾", -5.0d, 18.0d, 55, 100, "雾")
    };

    private final FileCache fileCache;
    private final Gson gson;
    private final TimeProvider timeProvider;
    private final ApiService apiService;
    private final QWeatherClient qWeatherClient;

    public WeatherRepository(@NonNull Context context) {
        this(new FileCache(context), new Gson(), System::currentTimeMillis, ApiClient.getPublicService(context), new QWeatherClient());
    }

    WeatherRepository(@NonNull FileCache fileCache, @NonNull Gson gson, @NonNull TimeProvider timeProvider, @NonNull ApiService apiService) {
        this(fileCache, gson, timeProvider, apiService, new QWeatherClient());
    }

    WeatherRepository(@NonNull FileCache fileCache, @NonNull Gson gson, @NonNull TimeProvider timeProvider, @NonNull ApiService apiService, @NonNull QWeatherClient qWeatherClient) {
        this.fileCache = fileCache;
        this.gson = gson;
        this.timeProvider = timeProvider;
        this.apiService = apiService;
        this.qWeatherClient = qWeatherClient;
    }

    @NonNull
    public PlatformModels.RealtimeWeatherView getRealtimeWeather(double longitude, double latitude) {
        return getRealtimeWeather(longitude, latitude, false);
    }

    @NonNull
    public PlatformModels.RealtimeWeatherView getRealtimeWeather(double longitude, double latitude, boolean forceRefresh) {
        LocationBucket bucket = buildLocationBucket(longitude, latitude);
        long now = timeProvider.now();

        // 先读取分桶缓存，避免同一区域在短时间内重复命中远端接口。
        if (!forceRefresh) {
            CacheEntry cachedEntry = readCache(bucket.cacheKey);
            if (cachedEntry != null
                    && now - cachedEntry.generatedAt <= CACHE_TTL_MS
                    && cachedEntry.weather != null) {
                PlatformModels.RealtimeWeatherView cachedWeather = cachedEntry.weather;
                Log.d(TAG, "using cached weather for " + bucket.cacheKey);
                if (isBlank(cachedWeather.locationName)) {
                    cachedWeather.locationName = bucket.displayName;
                }
                if (isBlank(cachedWeather.adcode)) {
                    cachedWeather.adcode = bucket.weatherDescriptor.adcode;
                }
                cachedWeather.sourceNote = buildCachedSourceNote(cachedWeather.sourceNote, bucket.weatherDescriptor.sourceNote);
                return cachedWeather;
            }
        }

        // 优先直连和风，保证移动端在后端链路受限时仍可拿到实时天气。
        if (qWeatherClient.isConfigured()) {
            Log.d(TAG, "trying QWeather direct API for " + bucket.cacheKey);
            PlatformModels.RealtimeWeatherView qweatherResult = qWeatherClient.fetchRealtimeWeather(longitude, latitude);
            if (qweatherResult != null) {
                Log.d(TAG, "QWeather direct API success for " + bucket.cacheKey + " location=" + qweatherResult.locationName);
                if (isBlank(qweatherResult.locationName)) {
                    qweatherResult.locationName = bucket.displayName;
                }
                CacheEntry qweatherCacheEntry = new CacheEntry();
                qweatherCacheEntry.generatedAt = now;
                qweatherCacheEntry.weather = qweatherResult;
                fileCache.write(WEATHER_CACHE_PREFIX + bucket.cacheKey, gson.toJson(qweatherCacheEntry));
                return qweatherResult;
            }
            Log.w(TAG, "QWeather direct API returned null for " + bucket.cacheKey);
        } else {
            Log.w(TAG, "QWeather not configured (missing API key or host) for " + bucket.cacheKey);
        }

        // 后端可用时再回落到服务端聚合接口，复用已有安全校验和治理逻辑。
        if (isBackendApiReachable()) {
            PlatformModels.RealtimeWeatherView weather = fetchFromServer(longitude, latitude);
            if (weather != null) {
                Log.d(TAG, "backend server success for " + bucket.cacheKey);
                if (isBlank(weather.locationName)) {
                    weather.locationName = bucket.displayName;
                }
                if (isBlank(weather.adcode)) {
                    weather.adcode = bucket.weatherDescriptor.adcode;
                }
                weather.sourceNote = isBlank(weather.sourceNote)
                        ? "温度、湿度、风向、风速和能见度来自和风天气实时天气；降水概率、降水强度与雷暴风险依据和风天气实时数据综合推导。"
                        : weather.sourceNote.trim();
                CacheEntry cacheEntry = new CacheEntry();
                cacheEntry.generatedAt = now;
                cacheEntry.weather = weather;
                fileCache.write(WEATHER_CACHE_PREFIX + bucket.cacheKey, gson.toJson(cacheEntry));
                return weather;
            }
        } else {
            Log.d(TAG, "backend API not configured or unreachable, skipping for " + bucket.cacheKey);
        }
        Log.w(TAG, "backend server failed, falling back to local simulation for " + bucket.cacheKey);
        return generateFallbackWeather(bucket, now);
    }

    @Nullable
    private PlatformModels.RealtimeWeatherView fetchFromServer(double longitude, double latitude) {
        try {
            retrofit2.Call<ApiEnvelope<PlatformModels.RealtimeWeatherView>> call = apiService.getRealtimeWeather(longitude, latitude);
            Response<ApiEnvelope<PlatformModels.RealtimeWeatherView>> response = call.execute();
            if (!response.isSuccessful()) {
                Log.w(TAG, "fetchFromServer: HTTP " + response.code() + " for lon=" + longitude + " lat=" + latitude);
                return null;
            }
            ApiEnvelope<PlatformModels.RealtimeWeatherView> envelope = response.body();
            if (envelope == null) {
                Log.w(TAG, "fetchFromServer: empty response body for lon=" + longitude + " lat=" + latitude);
                return null;
            }
            if (envelope.code != 200) {
                Log.w(TAG, "fetchFromServer: envelope code=" + envelope.code + " for lon=" + longitude + " lat=" + latitude);
                return null;
            }
            if (envelope.data != null) {
                PlatformModels.RealtimeWeatherView view = envelope.data;
                // 服务端返回的是原始天气值，客户端在这里补齐展示层派生字段。
                view.weatherIconType = resolveWeatherIconType(view.weather);
                view.refreshInterval = "15 分钟自动刷新";
                WeatherFlightEvaluator.DerivedMetrics metrics = WeatherFlightEvaluator.deriveFromServerData(
                        view.windSpeed,
                        view.visibility,
                        view.precipitationProbability,
                        view.precipitationIntensity,
                        view.thunderstormRisk
                );
                view.thunderstormRiskLevel = metrics.thunderstormRiskLevel;
                view.thunderstormRiskLabel = metrics.thunderstormRisk;
                view.thunderstormRiskHint = metrics.thunderstormRiskHint;
                view.thunderstormProtectionAdvice = metrics.thunderstormProtectionAdvice;
                return view;
            }
            return null;
        } catch (java.net.SocketTimeoutException e) {
            Log.w(TAG, "fetchFromServer: timeout for lon=" + longitude + " lat=" + latitude + " - " + e.getMessage());
        } catch (java.net.UnknownHostException e) {
            Log.w(TAG, "fetchFromServer: unknown host for lon=" + longitude + " lat=" + latitude + " - " + e.getMessage());
        } catch (java.net.ConnectException e) {
            Log.w(TAG, "fetchFromServer: connection refused for lon=" + longitude + " lat=" + latitude + " - " + e.getMessage());
        } catch (java.io.IOException e) {
            Log.w(TAG, "fetchFromServer: IO error for lon=" + longitude + " lat=" + latitude + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "fetchFromServer: error for lon=" + longitude + " lat=" + latitude + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }

    private PlatformModels.RealtimeWeatherView generateFallbackWeather(@NonNull LocationBucket bucket, long seed) {
        // 离线模式按位置桶和时间片生成稳定样本，避免刷新一次变一套天气。
        Random random = new Random(mixSeed(bucket.cacheKey, seed));
        WeatherProfile profile = WEATHER_PROFILES[random.nextInt(WEATHER_PROFILES.length)];
        String windDirection = WIND_DIRECTIONS[random.nextInt(WIND_DIRECTIONS.length)];
        String windPower = resolveWindPower(profile.liveWeather, random);
        WeatherFlightEvaluator.DerivedMetrics metrics =
                WeatherFlightEvaluator.deriveMetrics(profile.liveWeather, profile.dayWeather, profile.nightWeather, windPower);
        PlatformModels.RealtimeWeatherView view = new PlatformModels.RealtimeWeatherView();
        view.serviceName = "天气评估服务（离线模式）";
        view.locationName = bucket.displayName;
        view.adcode = bucket.weatherDescriptor.adcode;
        view.weather = profile.liveWeather;
        view.weatherIconType = resolveWeatherIconType(profile.liveWeather);
        view.reportTime = formatTime(seed);
        view.fetchedAt = formatTime(timeProvider.now());
        view.refreshInterval = "15 分钟自动刷新";
        view.temperature = clamp(roundToOneDecimal(randomInRange(random, profile.minTemperature, profile.maxTemperature)), MIN_TEMPERATURE, MAX_TEMPERATURE);
        view.humidity = (int) clamp(Math.round(randomInRange(random, profile.minHumidity, profile.maxHumidity)), MIN_HUMIDITY, MAX_HUMIDITY);
        view.windDirection = windDirection;
        view.windPower = windPower;
        view.windSpeed = metrics.windSpeed;
        view.visibility = clamp(metrics.visibility, MIN_VISIBILITY, MAX_VISIBILITY);
        view.precipitationProbability = (int) clamp(metrics.precipitationProbability, MIN_PRECIPITATION_PROBABILITY, MAX_PRECIPITATION_PROBABILITY);
        view.precipitationIntensity = clamp(roundToOneDecimal(metrics.precipitationIntensity), MIN_PRECIPITATION_INTENSITY, MAX_PRECIPITATION_INTENSITY);
        view.thunderstormRisk = metrics.thunderstormRisk;
        view.thunderstormRiskLevel = metrics.thunderstormRiskLevel;
        view.thunderstormRiskLabel = metrics.thunderstormRisk;
        view.thunderstormRiskHint = metrics.thunderstormRiskHint;
        view.thunderstormProtectionAdvice = metrics.thunderstormProtectionAdvice;
        view.sourceNote = "服务暂不可用，已切换到本地模拟推演模式。请检查网络或联系管理员。";
        view.suitability = WeatherFlightEvaluator.buildSuitabilityView(metrics);
        return view;
    }

    @NonNull
    private String resolveWeatherIconType(@NonNull String liveWeather) {
        if (liveWeather == null || liveWeather.isEmpty()) {
            return "clear";
        }
        if (liveWeather.contains("雷")) {
            return "thunderstorm";
        }
        if (liveWeather.contains("雨")) {
            return "rain";
        }
        if (liveWeather.contains("雪")) {
            return "snow";
        }
        if (liveWeather.contains("雾") || liveWeather.contains("霾")) {
            return "fog";
        }
        if (liveWeather.contains("阴") || liveWeather.contains("云")) {
            return "cloudy";
        }
        return "clear";
    }

    private CacheEntry readCache(@NonNull String cacheKey) {
        try {
            String content = fileCache.read(WEATHER_CACHE_PREFIX + cacheKey);
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            return gson.fromJson(content, CacheEntry.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    static LocationBucket buildLocationBucket(double longitude, double latitude) {
        double safeLongitude = Double.isNaN(longitude) || Double.isInfinite(longitude) ? 106.55156d : longitude;
        double safeLatitude = Double.isNaN(latitude) || Double.isInfinite(latitude) ? 29.56301d : latitude;
        // 缓存键只保留一位小数，兼顾同区域复用和跨区差异。
        double roundedLongitude = Math.round(safeLongitude * 10.0d) / 10.0d;
        double roundedLatitude = Math.round(safeLatitude * 10.0d) / 10.0d;
        AppScenarioMapper.WeatherDescriptor descriptor = AppScenarioMapper.describeWeatherLocation(safeLatitude, safeLongitude);
        String cacheKey = String.format(Locale.US, "SIM-%d-%d",
                Math.round((roundedLatitude + 90.0d) * 10.0d),
                Math.round((roundedLongitude + 180.0d) * 10.0d));
        String displayName = descriptor.locationName;
        return new LocationBucket(cacheKey, displayName, descriptor);
    }

    private String resolveWindPower(@NonNull String weather, @NonNull Random random) {
        int maxLevel;
        if ("雷阵雨".equals(weather)) {
            maxLevel = 7;
        } else if ("阵雨".equals(weather) || "小雪".equals(weather)) {
            maxLevel = 5;
        } else if ("雾".equals(weather) || "阴".equals(weather)) {
            maxLevel = 4;
        } else {
            maxLevel = 3;
        }
        int level = 1 + random.nextInt(maxLevel);
        return level + "级";
    }

    private long alignToWindow(long timestamp) {
        return (timestamp / CACHE_TTL_MS) * CACHE_TTL_MS;
    }

    private long mixSeed(@NonNull String cacheKey, long seed) {
        return seed ^ (cacheKey.hashCode() * 1103515245L);
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp);
    }

    private double randomInRange(@NonNull Random random, double minValue, double maxValue) {
        return minValue + (maxValue - minValue) * random.nextDouble();
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }

    private double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isOfflineWeather(@Nullable PlatformModels.RealtimeWeatherView weather) {
        if (weather == null) {
            return true;
        }
        String serviceName = weather.serviceName == null ? "" : weather.serviceName;
        String sourceNote = weather.sourceNote == null ? "" : weather.sourceNote;
        String adcode = weather.adcode == null ? "" : weather.adcode;
        return serviceName.contains("离线模式")
                || sourceNote.contains("本地模拟")
                || sourceNote.contains("模拟推演")
                || adcode.startsWith("CQ-");
    }

    private static boolean isBackendApiReachable() {
        String baseUrl = ApiConfig.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return false;
        }
        // 演示环境和本机回环地址不走服务端天气，直接避免无效请求。
        String url = baseUrl.trim().toLowerCase(Locale.ROOT);
        return !url.contains("example.com")
                && !url.contains("localhost")
                && !url.contains("127.0.0.1");
    }

    public void clearCache() {
        fileCache.deleteByPrefix(WEATHER_CACHE_PREFIX);
        Log.d(TAG, "clearCache: cleared all weather cache files");
    }

    @NonNull
    private String buildCachedSourceNote(@Nullable String currentNote, @NonNull String fallbackNote) {
        String source = isBlank(currentNote) ? fallbackNote : currentNote.trim();
        if (source.contains("缓存命中")) {
            return source;
        }
        return source + "（缓存命中，已减少重复计算并提升刷新性能。）";
    }

    interface TimeProvider {
        long now();
    }

    static final class LocationBucket {
        final String cacheKey;
        final String displayName;
        final AppScenarioMapper.WeatherDescriptor weatherDescriptor;

        LocationBucket(String cacheKey, String displayName, AppScenarioMapper.WeatherDescriptor weatherDescriptor) {
            this.cacheKey = cacheKey;
            this.displayName = displayName;
            this.weatherDescriptor = weatherDescriptor;
        }
    }

    private static final class CacheEntry {
        long generatedAt;
        PlatformModels.RealtimeWeatherView weather;
    }

    private static final class WeatherProfile {
        final String liveWeather;
        final String dayWeather;
        final String nightWeather;
        final double minTemperature;
        final double maxTemperature;
        final int minHumidity;
        final int maxHumidity;
        final String summaryLabel;

        WeatherProfile(
                String liveWeather,
                String dayWeather,
                String nightWeather,
                double minTemperature,
                double maxTemperature,
                int minHumidity,
                int maxHumidity,
                String summaryLabel
        ) {
            this.liveWeather = liveWeather;
            this.dayWeather = dayWeather;
            this.nightWeather = nightWeather;
            this.minTemperature = minTemperature;
            this.maxTemperature = maxTemperature;
            this.minHumidity = minHumidity;
            this.maxHumidity = maxHumidity;
            this.summaryLabel = summaryLabel;
        }
    }
}

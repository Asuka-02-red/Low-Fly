package com.example.low_altitudereststop.feature.home;

import android.content.Context;
import android.content.ContextWrapper;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class WeatherRepositoryTest {

    @Test
    public void shouldGenerateWeatherWithinConfiguredRanges() {
        InMemoryFileCache cache = new InMemoryFileCache();
        WeatherRepository repository = new WeatherRepository(cache, new Gson(), () -> 1_000L);

        PlatformModels.RealtimeWeatherView weather = repository.getRealtimeWeather(106.55156d, 29.56301d, true);

        Assert.assertTrue(weather.temperature >= -20.0d && weather.temperature <= 45.0d);
        Assert.assertTrue(weather.humidity >= 0 && weather.humidity <= 100);
        Assert.assertTrue(weather.windSpeed >= 0.0d && weather.windSpeed <= 20.0d);
        Assert.assertTrue(weather.visibility >= 0.5d && weather.visibility <= 20.0d);
        Assert.assertTrue(weather.precipitationProbability >= 0 && weather.precipitationProbability <= 100);
        Assert.assertTrue(weather.precipitationIntensity >= 0.0d && weather.precipitationIntensity <= 50.0d);
        Assert.assertNotNull(weather.suitability);
        Assert.assertEquals("模拟天气服务", weather.serviceName);
    }

    @Test
    public void shouldReuseCachedWeatherWithinTtl() {
        InMemoryFileCache cache = new InMemoryFileCache();
        MutableTimeProvider timeProvider = new MutableTimeProvider(10_000L);
        WeatherRepository repository = new WeatherRepository(cache, new Gson(), timeProvider);

        PlatformModels.RealtimeWeatherView first = repository.getRealtimeWeather(106.5d, 29.5d, false);
        timeProvider.now = 10_000L + 60_000L;
        PlatformModels.RealtimeWeatherView second = repository.getRealtimeWeather(106.5d, 29.5d, false);

        Assert.assertEquals(first.reportTime, second.reportTime);
        Assert.assertEquals(first.temperature, second.temperature, 0.0d);
        Assert.assertTrue(second.sourceNote.contains("缓存命中"));
    }

    @Test
    public void shouldBypassCacheWhenForceRefreshRequested() {
        InMemoryFileCache cache = new InMemoryFileCache();
        MutableTimeProvider timeProvider = new MutableTimeProvider(100_000L);
        WeatherRepository repository = new WeatherRepository(cache, new Gson(), timeProvider);

        PlatformModels.RealtimeWeatherView first = repository.getRealtimeWeather(106.6d, 29.6d, false);
        timeProvider.now = 101_234L;
        PlatformModels.RealtimeWeatherView refreshed = repository.getRealtimeWeather(106.6d, 29.6d, true);

        Assert.assertNotEquals(first.reportTime, refreshed.reportTime);
    }

    @Test
    public void shouldRefreshAfterCacheExpires() {
        InMemoryFileCache cache = new InMemoryFileCache();
        MutableTimeProvider timeProvider = new MutableTimeProvider(100_000L);
        WeatherRepository repository = new WeatherRepository(cache, new Gson(), timeProvider);

        PlatformModels.RealtimeWeatherView first = repository.getRealtimeWeather(106.7d, 29.7d, false);
        timeProvider.now = 100_000L + WeatherRepository.CACHE_TTL_MS + 1L;
        PlatformModels.RealtimeWeatherView refreshed = repository.getRealtimeWeather(106.7d, 29.7d, false);

        Assert.assertNotEquals(first.reportTime, refreshed.reportTime);
    }

    @Test
    public void shouldBuildStableLocationBucketForNearbyCoordinates() {
        WeatherRepository.LocationBucket first = WeatherRepository.buildLocationBucket(106.55156d, 29.56301d);
        WeatherRepository.LocationBucket second = WeatherRepository.buildLocationBucket(106.55888d, 29.56999d);

        Assert.assertEquals(first.cacheKey, second.cacheKey);
    }

    private static final class MutableTimeProvider implements WeatherRepository.TimeProvider {
        long now;

        MutableTimeProvider(long now) {
            this.now = now;
        }

        @Override
        public long now() {
            return now;
        }
    }

    private static final class InMemoryFileCache extends FileCache {
        private final Map<String, String> content = new HashMap<>();

        InMemoryFileCache() {
            super(new ContextWrapper(null) {
                @Override
                public Context getApplicationContext() {
                    return this;
                }
            });
        }

        @Override
        public void write(String name, String value) {
            content.put(name, value);
        }

        @Override
        public String read(String name) {
            return content.get(name);
        }
    }
}

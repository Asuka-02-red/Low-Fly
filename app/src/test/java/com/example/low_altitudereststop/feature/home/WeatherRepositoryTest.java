package com.example.low_altitudereststop.feature.home;

import com.example.low_altitudereststop.core.model.PlatformModels;
import org.junit.Assert;
import org.junit.Test;

public class WeatherRepositoryTest {

    @Test
    public void shouldBuildStableLocationBucketForNearbyCoordinates() {
        WeatherRepository.LocationBucket first = WeatherRepository.buildLocationBucket(106.55156d, 29.56301d);
        WeatherRepository.LocationBucket second = WeatherRepository.buildLocationBucket(106.55888d, 29.56999d);

        Assert.assertEquals(first.cacheKey, second.cacheKey);
    }

    @Test
    public void shouldBuildDifferentBucketForDistantCoordinates() {
        WeatherRepository.LocationBucket first = WeatherRepository.buildLocationBucket(106.55156d, 29.56301d);
        WeatherRepository.LocationBucket second = WeatherRepository.buildLocationBucket(116.4074d, 39.9042d);

        Assert.assertNotEquals(first.cacheKey, second.cacheKey);
    }
}

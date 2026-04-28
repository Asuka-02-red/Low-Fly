package com.example.low_altitudereststop;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomeWeatherInstrumentedTest {

    @Before
    public void seedSession() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AuthModels.AuthPayload payload = new AuthModels.AuthPayload();
        payload.token = "weather-ui-token";
        payload.refreshToken = "weather-ui-refresh";
        payload.userInfo = new AuthModels.SessionInfo();
        payload.userInfo.username = "pilot_weather";
        payload.userInfo.realName = "天气测试飞手";
        payload.userInfo.role = "PILOT";
        payload.userInfo.companyName = "低空演示";
        new SessionStore(context).saveAuth(payload);
    }

    @Test
    public void homeWeatherShowsSimulatedDataAndSupportsRefresh() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
                bottomNav.setSelectedItemId(R.id.homeFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            waitForWeatherBinding(scenario);
            scenario.onActivity(activity -> {
                TextView meta = activity.findViewById(R.id.tv_weather_meta);
                TextView location = activity.findViewById(R.id.tv_weather_location);
                TextView temperature = activity.findViewById(R.id.tv_weather_temperature);
                assertTrue(location.getText().toString().contains("演示区域"));
                assertTrue(meta.getText().toString().contains("模拟天气数据"));
                assertTrue(!"-".contentEquals(temperature.getText()));
                activity.findViewById(R.id.btn_weather_refresh).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            waitForWeatherBinding(scenario);
            scenario.onActivity(activity -> {
                TextView meta = activity.findViewById(R.id.tv_weather_meta);
                assertTrue(meta.getText().toString().contains("模拟天气数据"));
            });
        }
    }

    private void waitForWeatherBinding(ActivityScenario<MainActivity> scenario) {
        long deadline = System.currentTimeMillis() + 4000L;
        while (System.currentTimeMillis() < deadline) {
            final boolean[] ready = {false};
            scenario.onActivity(activity -> {
                TextView meta = activity.findViewById(R.id.tv_weather_meta);
                ready[0] = meta.getText() != null && meta.getText().toString().contains("模拟天气数据");
            });
            if (ready[0]) {
                return;
            }
            SystemClock.sleep(200L);
        }
    }
}

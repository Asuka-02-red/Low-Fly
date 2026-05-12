package com.example.low_altitudereststop;

import android.app.Application;
import android.util.Log;
import com.example.low_altitudereststop.core.network.ApiConfig;
import com.example.low_altitudereststop.core.network.NetworkMonitor;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.ai.service.AiBallServiceFacade;
import com.example.low_altitudereststop.feature.message.MessageNetworkMonitor;
import com.example.low_altitudereststop.ui.AppThemeMode;
import com.google.android.material.color.DynamicColors;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 应用程序入口Application类，负责全局初始化工作。
 * <p>
 * 在应用启动时完成以下初始化：崩溃日志记录、Material动态颜色、应用主题、
 * 高德地图SDK隐私合规、API配置、AI助手服务上下文绑定、
 * 消息网络监听器注册以及全局网络状态监听。
 * </p>
 */
public class LowAltitudeApp extends Application {

    private static final String TAG = "LowAltitudeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashLogger();
        DynamicColors.applyToActivitiesIfAvailable(this);
        AppThemeMode.applyFromPreferences(this);
        initAmapSdk();
        ApiConfig.init(BuildConfig.API_BASE_URL, BuildConfig.LLM_BASE_URL, BuildConfig.LLM_MODEL_NAME);
        runSafeInit("AiBallServiceFacade.attachContext", () ->
                AiBallServiceFacade.getInstance().attachContext(this));
        runSafeInit("MessageNetworkMonitor.register", () ->
                MessageNetworkMonitor.register(this));
        runSafeInit("NetworkMonitor.register", () ->
                NetworkMonitor.register(this));
    }

    private void initAmapSdk() {
        try {
            com.amap.api.location.AMapLocationClient.updatePrivacyShow(this, true, true);
            com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true);
            Log.i(TAG, "AMap SDK privacy agreement initialized");
        } catch (Throwable exception) {
            Log.e(TAG, "AMap SDK init failed", exception);
        }
    }

    private void runSafeInit(String step, Runnable action) {
        try {
            action.run();
        } catch (Throwable exception) {
            Log.e(TAG, "Startup init failed: " + step, exception);
        }
    }

    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter writer = new StringWriter();
                throwable.printStackTrace(new PrintWriter(writer));
                new OperationLogStore(this).appendCrash("UNCAUGHT", thread.getName() + "\n" + writer);
            } catch (Throwable ignored) {
                Log.e(TAG, "Unable to persist uncaught crash", ignored);
            }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            }
        });
    }
}

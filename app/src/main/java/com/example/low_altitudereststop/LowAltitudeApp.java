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

public class LowAltitudeApp extends Application {

    private static final String TAG = "LowAltitudeApp";

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashLogger();
        DynamicColors.applyToActivitiesIfAvailable(this);
        AppThemeMode.applyFromPreferences(this);
        ApiConfig.init(BuildConfig.API_BASE_URL, BuildConfig.LLM_BASE_URL, BuildConfig.LLM_MODEL_NAME);
        runSafeInit("AiBallServiceFacade.attachContext", () ->
                AiBallServiceFacade.getInstance().attachContext(this));
        runSafeInit("MessageNetworkMonitor.register", () ->
                MessageNetworkMonitor.register(this));
        runSafeInit("NetworkMonitor.register", () ->
                NetworkMonitor.register(this));
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

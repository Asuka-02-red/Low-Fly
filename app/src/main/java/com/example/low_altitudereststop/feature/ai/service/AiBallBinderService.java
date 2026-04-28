package com.example.low_altitudereststop.feature.ai.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.ai.IAiBallCallback;
import com.example.low_altitudereststop.ai.IAiBallService;
import com.example.low_altitudereststop.feature.ai.AiBallAccessibilityHelper;
import com.example.low_altitudereststop.feature.ai.AiBallDisplayCoordinator;
import com.example.low_altitudereststop.feature.ai.AiBallOverlayPermissionHelper;
import com.example.low_altitudereststop.feature.ai.AiBallSettingsStore;
import com.example.low_altitudereststop.feature.ai.widget.AiBallOverlayController;

public class AiBallBinderService extends Service {

    private static final String TAG = "AiBallBinderService";
    private AiBallOverlayController overlayController;

    private final IAiBallService.Stub binder = new IAiBallService.Stub() {
        @Override
        public void bindSession(String clientId, IAiBallCallback callback) {
            AiBallServiceFacade.getInstance().bindSession(callback);
        }

        @Override
        public void unbindSession(String clientId, IAiBallCallback callback) {
            AiBallServiceFacade.getInstance().unbindSession(callback);
        }

        @Override
        public void showBall() {
            handleShowBall();
        }

        @Override
        public void hideBall() {
            handleHideBall();
        }

        @Override
        public void startVoiceWakeup() {
            AiBallServiceFacade.getInstance().startVoiceWakeup();
        }

        @Override
        public void stopVoiceWakeup() {
            AiBallServiceFacade.getInstance().stopVoiceWakeup();
        }

        @Override
        public void submitText(String query) {
            AiBallServiceFacade.getInstance().submitText(query);
        }

        @Override
        public void cancelCurrentTask() {
            AiBallServiceFacade.getInstance().cancelCurrentTask();
        }

        @Override
        public void updateUiMode(String mode) {
            AiBallServiceFacade.getInstance().updateUiMode(mode);
        }

        @Override
        public String getLastResult() {
            return AiBallServiceFacade.getInstance().getLastResult();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        AiBallServiceFacade.getInstance().attachContext(this);
        syncOverlayHost();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        syncOverlayHost();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        syncOverlayHost();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        detachOverlayHost();
        super.onDestroy();
    }

    private void syncOverlayHost() {
        AiBallSettingsStore settingsStore = new AiBallSettingsStore(this);
        AiBallDisplayCoordinator.Availability availability = AiBallDisplayCoordinator.resolve(
                settingsStore.isEnabled(),
                AiBallOverlayPermissionHelper.canDrawOverlays(this),
                AiBallAccessibilityHelper.isServiceEnabled(this)
        );
        Log.d(TAG, "syncOverlayHost availability=" + availability);
        if (AiBallDisplayCoordinator.shouldAttachOverlayHost(availability)) {
            ensureOverlayHost();
            if (AiBallServiceFacade.getInstance().isVisible()) {
                overlayController.show();
            } else if (overlayController != null) {
                overlayController.hide();
            }
            return;
        }
        detachOverlayHost();
        if (availability == AiBallDisplayCoordinator.Availability.DISABLED) {
            AiBallServiceFacade.getInstance().hideBall();
        }
    }

    private void ensureOverlayHost() {
        try {
            if (overlayController == null) {
                overlayController = new AiBallOverlayController(
                        this,
                        AiBallOverlayController.OverlayWindowType.APPLICATION_OVERLAY
                );
            }
            overlayController.attach();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to ensure overlay host", exception);
        }
    }

    private void detachOverlayHost() {
        if (overlayController == null) {
            return;
        }
        overlayController.detach();
        overlayController = null;
    }

    private void handleShowBall() {
        AiBallServiceFacade.getInstance().showBall();
        syncOverlayHost();
    }

    private void handleHideBall() {
        AiBallServiceFacade.getInstance().hideBall();
        syncOverlayHost();
    }
}

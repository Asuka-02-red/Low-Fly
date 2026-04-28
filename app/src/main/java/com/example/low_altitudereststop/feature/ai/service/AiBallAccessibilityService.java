package com.example.low_altitudereststop.feature.ai.service;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.NonNull;
import com.example.low_altitudereststop.feature.ai.AiBallAccessibilityHelper;
import com.example.low_altitudereststop.feature.ai.AiBallDisplayCoordinator;
import com.example.low_altitudereststop.feature.ai.AiBallOverlayPermissionHelper;
import com.example.low_altitudereststop.feature.ai.AiBallSettingsStore;
import com.example.low_altitudereststop.feature.ai.widget.AiBallOverlayController;

public class AiBallAccessibilityService extends AccessibilityService {

    private static final String TAG = "AiBallAccessibility";
    private AiBallOverlayController overlayController;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            Log.d(TAG, "Accessibility service connected");
            AiBallServiceFacade.getInstance().attachContext(this);
            syncAccessibilityOverlay();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to initialize accessibility overlay", exception);
        }
    }

    @Override
    public void onAccessibilityEvent(@NonNull AccessibilityEvent event) {
        if (overlayController == null) {
            return;
        }
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            syncAccessibilityOverlay();
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        detachOverlayController();
        super.onDestroy();
    }

    private void syncAccessibilityOverlay() {
        AiBallDisplayCoordinator.Availability availability = AiBallDisplayCoordinator.resolve(
                new AiBallSettingsStore(this).isEnabled(),
                AiBallOverlayPermissionHelper.canDrawOverlays(this),
                AiBallAccessibilityHelper.isServiceEnabled(this)
        );
        if (!AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability)) {
            detachOverlayController();
            return;
        }
        try {
            if (overlayController == null) {
                overlayController = new AiBallOverlayController(
                        this,
                        AiBallOverlayController.OverlayWindowType.ACCESSIBILITY_OVERLAY
                );
            }
            overlayController.attach();
            if (AiBallServiceFacade.getInstance().isVisible()) {
                overlayController.show();
            } else {
                overlayController.hide();
            }
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to sync accessibility overlay", exception);
        }
    }

    private void detachOverlayController() {
        if (overlayController == null) {
            return;
        }
        overlayController.detach();
        overlayController = null;
    }
}

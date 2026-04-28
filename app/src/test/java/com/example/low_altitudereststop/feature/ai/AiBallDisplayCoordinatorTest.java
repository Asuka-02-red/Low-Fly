package com.example.low_altitudereststop.feature.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AiBallDisplayCoordinatorTest {

    @Test
    public void disabledState_blocksAllDisplayHosts() {
        AiBallDisplayCoordinator.Availability availability =
                AiBallDisplayCoordinator.resolve(false, true, true);

        assertEquals(AiBallDisplayCoordinator.Availability.DISABLED, availability);
        assertFalse(AiBallDisplayCoordinator.shouldAttachOverlayHost(availability));
        assertFalse(AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability));
    }

    @Test
    public void overlayPermission_aloneIsEnoughToShowBall() {
        AiBallDisplayCoordinator.Availability availability =
                AiBallDisplayCoordinator.resolve(true, true, false);

        assertEquals(AiBallDisplayCoordinator.Availability.OVERLAY_READY, availability);
        assertTrue(AiBallDisplayCoordinator.shouldAttachOverlayHost(availability));
        assertFalse(AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability));
    }

    @Test
    public void accessibilityIsOnlyFallbackWhenOverlayPermissionMissing() {
        AiBallDisplayCoordinator.Availability availability =
                AiBallDisplayCoordinator.resolve(true, false, true);

        assertEquals(AiBallDisplayCoordinator.Availability.ACCESSIBILITY_FALLBACK, availability);
        assertFalse(AiBallDisplayCoordinator.shouldAttachOverlayHost(availability));
        assertTrue(AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability));
    }

    @Test
    public void permissionIsRequiredWhenNoDisplayCapabilityExists() {
        AiBallDisplayCoordinator.Availability availability =
                AiBallDisplayCoordinator.resolve(true, false, false);

        assertEquals(AiBallDisplayCoordinator.Availability.PERMISSION_REQUIRED, availability);
        assertFalse(AiBallDisplayCoordinator.shouldAttachOverlayHost(availability));
        assertFalse(AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability));
    }
}

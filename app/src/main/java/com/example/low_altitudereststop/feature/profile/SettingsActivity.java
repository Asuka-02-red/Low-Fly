package com.example.low_altitudereststop.feature.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import com.example.low_altitudereststop.BuildConfig;
import com.example.low_altitudereststop.MainActivity;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.feature.ai.AiBallAccessibilityHelper;
import com.example.low_altitudereststop.feature.ai.AiBallDisplayCoordinator;
import com.example.low_altitudereststop.feature.ai.AiBallOverlayPermissionHelper;
import com.example.low_altitudereststop.feature.ai.AiBallServiceLauncher;
import com.example.low_altitudereststop.feature.ai.AiBallSettingsStore;
import com.example.low_altitudereststop.feature.ai.service.AiBallServiceFacade;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivitySettingsBinding;
import com.example.low_altitudereststop.ui.AccessibilityDisplayMode;
import com.example.low_altitudereststop.ui.AppThemeMode;
import java.io.File;

public class SettingsActivity extends NavigableEdgeToEdgeActivity {

    private static final String PREF = AppThemeMode.PREF;
    private static final String KEY_NOTIFICATION = "notification_enabled";
    private static final String KEY_DARK_MODE = AppThemeMode.KEY_DARK_MODE;
    private ActivitySettingsBinding binding;
    private AiBallSettingsStore aiBallSettingsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        aiBallSettingsStore = new AiBallSettingsStore(this);

        SharedPreferences preferences = getSharedPreferences(PREF, MODE_PRIVATE);
        binding.switchNotifications.setChecked(preferences.getBoolean(KEY_NOTIFICATION, true));
        binding.switchDarkMode.setChecked(preferences.getBoolean(KEY_DARK_MODE, true));
        binding.switchHighContrast.setChecked(AccessibilityDisplayMode.isHighContrastEnabled(this));
        binding.switchAiBall.setChecked(aiBallSettingsStore.isEnabled());
        renderCacheStatus();
        renderAiBallStatus();

        binding.btnSave.setOnClickListener(v -> {
            boolean shouldRefresh =
                    binding.switchDarkMode.isChecked() != preferences.getBoolean(KEY_DARK_MODE, true)
                            || binding.switchHighContrast.isChecked()
                            != AccessibilityDisplayMode.isHighContrastEnabled(this);
            preferences.edit()
                    .putBoolean(KEY_NOTIFICATION, binding.switchNotifications.isChecked())
                    .apply();
            AppThemeMode.persistAndApply(this, binding.switchDarkMode.isChecked());
            AccessibilityDisplayMode.persistHighContrast(this, binding.switchHighContrast.isChecked());
            aiBallSettingsStore.setEnabled(binding.switchAiBall.isChecked());
            maybeStartAiBallService();
            syncAiBallVisibility();
            renderAiBallStatus();
            maybePromptOverlayPermission();
            Snackbar.make(binding.getRoot(), R.string.settings_saved, Snackbar.LENGTH_LONG).show();
            if (shouldRefresh) {
                binding.getRoot().post(this::recreate);
            }
        });
        binding.btnClearCache.setOnClickListener(v -> {
            preferences.edit().remove(KEY_NOTIFICATION).remove(KEY_DARK_MODE).apply();
            binding.switchNotifications.setChecked(true);
            binding.switchDarkMode.setChecked(true);
            binding.switchHighContrast.setChecked(false);
            binding.switchAiBall.setChecked(false);
            AppThemeMode.apply(true);
            AccessibilityDisplayMode.persistHighContrast(this, false);
            aiBallSettingsStore.setEnabled(false);
            syncAiBallVisibility();
            clearAppCache(getCacheDir());
            clearAppCache(getFilesDir());
            renderCacheStatus();
            renderAiBallStatus();
            maybePromptOverlayPermission();
            Snackbar.make(binding.getRoot(), R.string.settings_reset_done, Snackbar.LENGTH_LONG).show();
        });
        binding.btnOpenMessages.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_TARGET_DESTINATION, com.example.low_altitudereststop.R.id.messageFragment)));
        binding.btnOpenHelp.setOnClickListener(v ->
                startActivity(new Intent(this, HelpFeedbackActivity.class)));
        binding.btnOpenOverlayPermission.setOnClickListener(v ->
                startActivity(AiBallOverlayPermissionHelper.createManageOverlayPermissionIntent(this)));
        binding.btnOpenAccessibility.setOnClickListener(v ->
                startActivity(AiBallAccessibilityHelper.createAccessibilitySettingsIntent()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeStartAiBallService();
        renderAiBallStatus();
        syncAiBallVisibility();
    }

    private void renderCacheStatus() {
        long cacheKb = Math.max(12L, directorySize(getCacheDir()) / 1024L);
        binding.tvCacheStatus.setText("当前本地缓存约 " + cacheKb + " KB，支持弱网读取与失败重试。");
    }

    private long directorySize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0L;
        }
        if (dir.isFile()) {
            return dir.length();
        }
        long total = 0L;
        File[] files = dir.listFiles();
        if (files == null) {
            return 0L;
        }
        for (File file : files) {
            total += directorySize(file);
        }
        return total;
    }

    private void clearAppCache(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                clearAppCache(file);
            }
            file.delete();
        }
    }

    private void renderAiBallStatus() {
        AiBallDisplayCoordinator.Availability availability = AiBallDisplayCoordinator.resolve(
                binding.switchAiBall.isChecked(),
                AiBallOverlayPermissionHelper.canDrawOverlays(this),
                AiBallAccessibilityHelper.isServiceEnabled(this)
        );
        if (availability == AiBallDisplayCoordinator.Availability.DISABLED) {
            binding.tvAiBallStatus.setText(R.string.settings_ai_ball_status_off);
            return;
        }
        if (availability == AiBallDisplayCoordinator.Availability.OVERLAY_READY) {
            binding.tvAiBallStatus.setText(R.string.settings_ai_ball_status_enabled);
            return;
        }
        if (availability == AiBallDisplayCoordinator.Availability.ACCESSIBILITY_FALLBACK) {
            binding.tvAiBallStatus.setText(R.string.settings_ai_ball_status_accessibility_fallback);
            return;
        }
        binding.tvAiBallStatus.setText(R.string.settings_ai_ball_status_permission_required);
    }

    private void syncAiBallVisibility() {
        if (!BuildConfig.ENABLE_AI_BALL) {
            return;
        }
        AiBallDisplayCoordinator.Availability availability = AiBallDisplayCoordinator.resolve(
                binding.switchAiBall.isChecked(),
                AiBallOverlayPermissionHelper.canDrawOverlays(this),
                AiBallAccessibilityHelper.isServiceEnabled(this)
        );
        if (availability == AiBallDisplayCoordinator.Availability.DISABLED) {
            AiBallServiceFacade.getInstance().collapsePanel();
            AiBallServiceFacade.getInstance().hideBall();
            return;
        }
        if (AiBallDisplayCoordinator.shouldAttachOverlayHost(availability)
                || AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability)) {
            AiBallServiceLauncher.ensureStarted(this);
        }
        AiBallServiceFacade.getInstance().showBall();
    }

    private void maybeStartAiBallService() {
        if (!BuildConfig.ENABLE_AI_BALL || !binding.switchAiBall.isChecked()) {
            return;
        }
        AiBallDisplayCoordinator.Availability availability = AiBallDisplayCoordinator.resolve(
                true,
                AiBallOverlayPermissionHelper.canDrawOverlays(this),
                AiBallAccessibilityHelper.isServiceEnabled(this)
        );
        if (AiBallDisplayCoordinator.shouldAttachOverlayHost(availability)
                || AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability)) {
            AiBallServiceLauncher.ensureStarted(this);
        }
    }

    private void maybePromptOverlayPermission() {
        if (!binding.switchAiBall.isChecked() || AiBallOverlayPermissionHelper.canDrawOverlays(this)) {
            return;
        }
        Snackbar.make(binding.getRoot(), R.string.ai_ball_enable_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.ai_ball_enable_action, v ->
                        startActivity(AiBallOverlayPermissionHelper.createManageOverlayPermissionIntent(this)))
                .show();
    }
}

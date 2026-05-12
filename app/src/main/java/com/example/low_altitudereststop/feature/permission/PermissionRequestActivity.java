package com.example.low_altitudereststop.feature.permission;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.example.low_altitudereststop.R;
import com.google.android.material.snackbar.Snackbar;
import java.util.Map;

/**
 * 权限请求透明Activity，用于在权限被拒绝后重新请求。
 * <p>
 * 作为透明的中间Activity接收权限组和说明信息，
 * 展示权限说明Snackbar后发起系统权限请求，
 * 处理完成后通知AppPermissionManager状态变更并自动关闭。
 * </p>
 */
public class PermissionRequestActivity extends FragmentActivity {

    public static final String EXTRA_PERMISSION_GROUP = "permission_group";
    public static final String EXTRA_REQUEST_CODE = "request_code";
    public static final String EXTRA_RATIONALE_MESSAGE = "rationale_message";

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);
    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                AppPermissionManager manager = AppPermissionManager.getInstance();
                String permissionGroup = getIntent().getStringExtra(EXTRA_PERMISSION_GROUP);
                if (permissionGroup != null) {
                    manager.notifyStateChanged(this, permissionGroup);
                }
                finish();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = new View(this);
        view.setId(View.generateViewId());
        setContentView(view);
        if (savedInstanceState == null) {
            maybeShowRationaleAndRequest();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String permissionGroup = getIntent().getStringExtra(EXTRA_PERMISSION_GROUP);
        if (permissionGroup != null) {
            AppPermissionManager.getInstance().notifyStateChanged(this, permissionGroup);
        }
    }

    private void maybeShowRationaleAndRequest() {
        AppPermissionManager manager = AppPermissionManager.getInstance();
        String permissionGroup = getIntent().getStringExtra(EXTRA_PERMISSION_GROUP);
        if (permissionGroup == null) {
            finish();
            return;
        }
        AppPermissionManager.PermissionState state = manager.getState(this, permissionGroup);
        String rationaleMessage = getIntent().getStringExtra(EXTRA_RATIONALE_MESSAGE);
        if (state.shouldShowRationale && rationaleMessage != null && !rationaleMessage.trim().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), rationaleMessage, Snackbar.LENGTH_LONG)
                    .setAction(R.string.permission_action_continue, v -> {
                        manager.markRequested(this, permissionGroup);
                        permissionLauncher.launch(manager.resolvePermissions(permissionGroup));
                    })
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != DISMISS_EVENT_ACTION) {
                                manager.markRequested(PermissionRequestActivity.this, permissionGroup);
                                permissionLauncher.launch(manager.resolvePermissions(permissionGroup));
                            }
                        }
                    })
                    .show();
            return;
        }
        manager.markRequested(this, permissionGroup);
        permissionLauncher.launch(manager.resolvePermissions(permissionGroup));
    }

    private void onPermissionResult(@NonNull Map<String, Boolean> result) {
        int[] grantResults = new int[result.size()];
        int index = 0;
        for (Boolean granted : result.values()) {
            grantResults[index++] = Boolean.TRUE.equals(granted) ? android.content.pm.PackageManager.PERMISSION_GRANTED : android.content.pm.PackageManager.PERMISSION_DENIED;
        }
        int requestCode = getIntent().getIntExtra(EXTRA_REQUEST_CODE, -1);
        AppPermissionManager manager = AppPermissionManager.getInstance();
        manager.dispatchPermissionResult(this, requestCode, grantResults);
        String permissionGroup = getIntent().getStringExtra(EXTRA_PERMISSION_GROUP);
        if (permissionGroup == null) {
            finish();
            return;
        }
        AppPermissionManager.PermissionState state = manager.getState(this, permissionGroup);
        AppPermissionManager.PermissionRequestPayload payload = AppPermissionManager.GROUP_RECORD_AUDIO.equals(permissionGroup)
                ? manager.buildRecordAudioPayload()
                : manager.buildLocationPayload();
        if (state.permanentlyDenied) {
            Snackbar.make(findViewById(android.R.id.content), payload.permanentDeniedMessage, Snackbar.LENGTH_LONG)
                    .setAction(payload.settingsActionLabel, v -> settingsLauncher.launch(manager.createAppSettingsIntent(this)))
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != DISMISS_EVENT_ACTION) {
                                finish();
                            }
                        }
                    })
                    .show();
            return;
        }
        if (!state.granted) {
            Snackbar.make(findViewById(android.R.id.content), payload.deniedMessage, Snackbar.LENGTH_LONG)
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            finish();
                        }
                    })
                    .show();
            return;
        }
        finish();
    }
}

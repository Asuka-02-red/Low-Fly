package com.example.low_altitudereststop.feature.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.example.low_altitudereststop.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 应用权限管理器，统一管理运行时权限的查询、请求和状态监听。
 * <p>
 * 采用单例模式，支持定位、录音等权限组的查询与请求，
 * 处理权限说明展示、永久拒绝跳转设置页、权限状态变更通知，
 * 以及构造系统设置和应用设置页的跳转Intent。
 * </p>
 */
public final class AppPermissionManager {

    public static final String GROUP_RECORD_AUDIO = "record_audio";
    public static final String GROUP_LOCATION = "location";

    private static volatile AppPermissionManager instance;

    public interface PermissionListener {
        void onPermissionStateChanged(@NonNull String permissionGroup, @NonNull PermissionState state);
    }

    public static final class PermissionState {
        public final boolean granted;
        public final boolean permanentlyDenied;
        public final boolean shouldShowRationale;

        PermissionState(boolean granted, boolean permanentlyDenied, boolean shouldShowRationale) {
            this.granted = granted;
            this.permanentlyDenied = permanentlyDenied;
            this.shouldShowRationale = shouldShowRationale;
        }
    }

    public static final class PermissionRequestPayload {
        public final String permissionGroup;
        public final String[] permissions;
        public final int requestCode;
        public final String rationaleMessage;
        public final String deniedMessage;
        public final String permanentDeniedMessage;
        public final String settingsActionLabel;

        PermissionRequestPayload(
                @NonNull String permissionGroup,
                @NonNull String[] permissions,
                int requestCode,
                @NonNull String rationaleMessage,
                @NonNull String deniedMessage,
                @NonNull String permanentDeniedMessage,
                @NonNull String settingsActionLabel
        ) {
            this.permissionGroup = permissionGroup;
            this.permissions = permissions;
            this.requestCode = requestCode;
            this.rationaleMessage = rationaleMessage;
            this.deniedMessage = deniedMessage;
            this.permanentDeniedMessage = permanentDeniedMessage;
            this.settingsActionLabel = settingsActionLabel;
        }
    }

    public interface PermissionResultCallback {
        void onGranted();
        void onDenied(@NonNull PermissionState state, @NonNull PermissionRequestPayload payload);
    }

    private final Map<Integer, PermissionRequestRecord> pendingRequests = new HashMap<>();
    private final Map<String, LinkedHashSet<WeakReference<PermissionListener>>> listeners = new HashMap<>();

    private AppPermissionManager() {
    }

    @NonNull
    public static AppPermissionManager getInstance() {
        if (instance == null) {
            synchronized (AppPermissionManager.class) {
                if (instance == null) {
                    instance = new AppPermissionManager();
                }
            }
        }
        return instance;
    }

    @NonNull
    public PermissionRequestPayload buildRecordAudioPayload() {
        return new PermissionRequestPayload(
                GROUP_RECORD_AUDIO,
                new String[]{Manifest.permission.RECORD_AUDIO},
                4101,
                "语音提问需要麦克风权限，用于识别你的语音指令。",
                "未授予麦克风权限，当前无法使用语音提问。",
                "麦克风权限已被永久拒绝，请前往设置手动开启后再使用语音提问。",
                "去设置"
        );
    }

    @NonNull
    public PermissionRequestPayload buildLocationPayload() {
        return new PermissionRequestPayload(
                GROUP_LOCATION,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                4102,
                "天气评估需要定位权限，用于获取当前位置并查询实时天气。",
                "未授予定位权限，当前无法获取当前位置天气。",
                "定位权限已被永久拒绝，请前往设置手动开启后再刷新天气。",
                "去设置"
        );
    }

    public boolean isGranted(@NonNull Context context, @NonNull String permissionGroup) {
        for (String permission : resolvePermissions(permissionGroup)) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public PermissionState getState(@NonNull Activity activity, @NonNull String permissionGroup) {
        String[] permissions = resolvePermissions(permissionGroup);
        boolean granted = false;
        boolean shouldShowRationale = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                granted = true;
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
            }
        }
        boolean permanentlyDenied = !granted && !shouldShowRationale && hasRequestedBefore(activity, permissionGroup);
        return new PermissionState(granted, permanentlyDenied, shouldShowRationale);
    }

    public void requestPermissions(
            @NonNull FragmentActivity activity,
            @NonNull PermissionRequestPayload payload,
            @NonNull PermissionResultCallback callback
    ) {
        PermissionState state = getState(activity, payload.permissionGroup);
        if (state.granted) {
            callback.onGranted();
            notifyStateChanged(activity, payload.permissionGroup);
            return;
        }
        pendingRequests.put(payload.requestCode, new PermissionRequestRecord(payload, callback));
        activity.startActivity(createPermissionProxyIntent(activity, payload));
    }

    public void requestPermissions(
            @NonNull Fragment fragment,
            @NonNull PermissionRequestPayload payload,
            @NonNull PermissionResultCallback callback
    ) {
        FragmentActivity activity = fragment.getActivity();
        if (activity == null) {
            callback.onDenied(new PermissionState(false, false, false), payload);
            return;
        }
        requestPermissions(activity, payload, callback);
    }

    public void requestPermissions(
            @NonNull Context context,
            @NonNull PermissionRequestPayload payload,
            @NonNull PermissionResultCallback callback
    ) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            PermissionState state = getState(activity, payload.permissionGroup);
            if (state.granted) {
                callback.onGranted();
                notifyStateChanged(activity, payload.permissionGroup);
                return;
            }
        } else if (isGranted(context, payload.permissionGroup)) {
            callback.onGranted();
            return;
        }
        pendingRequests.put(payload.requestCode, new PermissionRequestRecord(payload, callback));
        context.startActivity(createPermissionProxyIntent(context, payload));
    }

    public void dispatchPermissionResult(
            @NonNull FragmentActivity activity,
            int requestCode,
            @Nullable int[] grantResults
    ) {
        PermissionRequestRecord record = pendingRequests.remove(requestCode);
        if (record == null) {
            return;
        }
        PermissionState state = getState(activity, record.payload.permissionGroup);
        notifyStateChanged(activity, record.payload.permissionGroup);
        if (isGrantedByResults(grantResults) || state.granted) {
            record.callback.onGranted();
            return;
        }
        record.callback.onDenied(state, record.payload);
    }

    @NonNull
    public Intent createAppSettingsIntent(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @NonNull
    public Intent createLocationSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public boolean isLocationServiceEnabled(@NonNull Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void registerListener(@NonNull String permissionGroup, @NonNull PermissionListener listener) {
        LinkedHashSet<WeakReference<PermissionListener>> groupListeners = listeners.get(permissionGroup);
        if (groupListeners == null) {
            groupListeners = new LinkedHashSet<>();
            listeners.put(permissionGroup, groupListeners);
        }
        groupListeners.add(new WeakReference<>(listener));
    }

    public void unregisterListener(@NonNull PermissionListener listener) {
        for (LinkedHashSet<WeakReference<PermissionListener>> groupListeners : listeners.values()) {
            List<WeakReference<PermissionListener>> stale = new ArrayList<>();
            for (WeakReference<PermissionListener> reference : groupListeners) {
                PermissionListener value = reference.get();
                if (value == null || value == listener) {
                    stale.add(reference);
                }
            }
            groupListeners.removeAll(stale);
        }
    }

    public void notifyStateChanged(@NonNull Activity activity, @NonNull String permissionGroup) {
        PermissionState state = getState(activity, permissionGroup);
        LinkedHashSet<WeakReference<PermissionListener>> groupListeners = listeners.get(permissionGroup);
        if (groupListeners == null) {
            return;
        }
        List<WeakReference<PermissionListener>> stale = new ArrayList<>();
        for (WeakReference<PermissionListener> reference : groupListeners) {
            PermissionListener listener = reference.get();
            if (listener == null) {
                stale.add(reference);
                continue;
            }
            listener.onPermissionStateChanged(permissionGroup, state);
        }
        groupListeners.removeAll(stale);
    }

    public int resolvePermissionLabelRes(@NonNull String permissionGroup) {
        if (GROUP_RECORD_AUDIO.equals(permissionGroup)) {
            return R.string.permission_label_microphone;
        }
        return R.string.permission_label_location;
    }

    @NonNull
    String[] resolvePermissions(@NonNull String permissionGroup) {
        if (GROUP_RECORD_AUDIO.equals(permissionGroup)) {
            return new String[]{Manifest.permission.RECORD_AUDIO};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    @NonNull
    private Intent createPermissionProxyIntent(@NonNull Context context, @NonNull PermissionRequestPayload payload) {
        Intent intent = new Intent(context, PermissionRequestActivity.class)
                .putExtra(PermissionRequestActivity.EXTRA_PERMISSION_GROUP, payload.permissionGroup)
                .putExtra(PermissionRequestActivity.EXTRA_REQUEST_CODE, payload.requestCode)
                .putExtra(PermissionRequestActivity.EXTRA_RATIONALE_MESSAGE, payload.rationaleMessage);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    private boolean isGrantedByResults(@Nullable int[] grantResults) {
        if (grantResults == null) {
            return false;
        }
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    void markRequested(@NonNull Context context, @NonNull String permissionGroup) {
        context.getSharedPreferences("app_permission_manager", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("requested_" + permissionGroup, true)
                .apply();
    }

    private boolean hasRequestedBefore(@NonNull Context context, @NonNull String permissionGroup) {
        return context.getSharedPreferences("app_permission_manager", Context.MODE_PRIVATE)
                .getBoolean("requested_" + permissionGroup, false);
    }

    private static final class PermissionRequestRecord {
        final PermissionRequestPayload payload;
        final PermissionResultCallback callback;

        PermissionRequestRecord(@NonNull PermissionRequestPayload payload, @NonNull PermissionResultCallback callback) {
            this.payload = payload;
            this.callback = callback;
        }
    }
}

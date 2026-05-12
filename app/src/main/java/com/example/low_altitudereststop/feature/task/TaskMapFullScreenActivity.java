package com.example.low_altitudereststop.feature.task;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.feature.permission.AppPermissionManager;
import com.example.low_altitudereststop.databinding.ActivityTaskMapFullScreenBinding;
import com.example.low_altitudereststop.feature.compliance.NoFlyZoneMapRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 任务地图全屏展示Activity。
 * <p>
 * 以全屏模式展示高德地图，加载任务详情中的坐标、作业半径、
 * 航线多段线和禁飞区叠加层，支持安全模式降级。
 * </p>
 */
public class TaskMapFullScreenActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";

    private ActivityTaskMapFullScreenBinding binding;
    private MapView mapView;
    private AMap aMap;
    private long taskId;
    private String taskTitle;
    private boolean demoSession;
    private boolean mapRenderingEnabled;
    private boolean offlineDemoNoticeShown;
    private TaskDetailActivity.TaskMapSnapshot snapshot;
    private List<PlatformModels.NoFlyZoneView> nearbyZones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskMapFullScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1L);
        taskTitle = getIntent().getStringExtra(EXTRA_TASK_TITLE);
        bindBackButton(binding.btnBack);

        demoSession = new com.example.low_altitudereststop.core.session.SessionStore(this).isDemoSession();

        // Request location permission for map
        AppPermissionManager permissionManager = AppPermissionManager.getInstance();
        if (!permissionManager.isGranted(this, AppPermissionManager.GROUP_LOCATION)) {
            permissionManager.requestPermissions(this, permissionManager.buildLocationPayload(), new AppPermissionManager.PermissionResultCallback() {
                @Override
                public void onGranted() {
                    initMapAfterPermission(savedInstanceState);
                }

                @Override
                public void onDenied(@NonNull AppPermissionManager.PermissionState state, @NonNull AppPermissionManager.PermissionRequestPayload payload) {
                    initMapAfterPermission(savedInstanceState);
                }
            });
        } else {
            initMapAfterPermission(savedInstanceState);
        }
    }

    private void initMapAfterPermission(Bundle savedInstanceState) {
        mapRenderingEnabled = shouldEnableMapRendering();
        initMap(savedInstanceState);

        binding.tvTitle.setText(taskTitle == null || taskTitle.trim().isEmpty() ? "任务地图" : taskTitle);
        binding.tvRouteSummary.setText("正在加载执行路线、作业范围和附近禁飞区");
        snapshot = TaskDetailActivity.TaskMapSnapshot.fromIntent(taskId, taskTitle, null, null);
        renderMap(snapshot, nearbyZones);
        loadTaskDetail();
    }

    private void initMap(Bundle savedInstanceState) {
        if (!mapRenderingEnabled) {
            binding.layoutMapPlaceholder.setVisibility(View.VISIBLE);
            return;
        }
        try {
            mapView = new MapView(this);
            mapView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            binding.mapContainer.removeAllViews();
            binding.mapContainer.addView(mapView);
            binding.layoutMapPlaceholder.setVisibility(View.GONE);
            mapView.onCreate(savedInstanceState);
            aMap = mapView.getMap();
            NoFlyZoneMapRenderer.configureBaseMap(aMap, true);
        } catch (Throwable throwable) {
            disableMapRendering();
        }
    }

    private void loadTaskDetail() {
        if (demoSession) {
            applyCachedNearbyZones();
            updateSummary();
            return;
        }
        if (taskId <= 0) {
            return;
        }
        ApiClient.getAuthedService(this).getTaskDetail(taskId).enqueue(new Callback<ApiEnvelope<PlatformModels.TaskDetailView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.TaskDetailView>> call, Response<ApiEnvelope<PlatformModels.TaskDetailView>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    showOfflineDemoToast();
                    applyCachedNearbyZones();
                    updateSummary();
                    return;
                }
                PlatformModels.TaskDetailView detail = response.body().data;
                binding.tvTitle.setText(detail.title == null || detail.title.trim().isEmpty() ? "任务地图" : detail.title);
                snapshot = TaskDetailActivity.TaskMapSnapshot.fromDetail(detail);
                updateSummary();
                renderMap(snapshot, nearbyZones);
                loadNearbyZones();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.TaskDetailView>> call, Throwable t) {
                showOfflineDemoToast();
                applyCachedNearbyZones();
                updateSummary();
            }
        });
    }

    private void loadNearbyZones() {
        if (demoSession) {
            applyCachedNearbyZones();
            updateSummary();
            return;
        }
        ApiClient.getAuthedService(this).listNoFlyZones().enqueue(new Callback<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> call, Response<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    applyCachedNearbyZones();
                    updateSummary();
                    return;
                }
                nearbyZones = filterNearbyZones(snapshot, response.body().data);
                updateSummary();
                renderMap(snapshot, nearbyZones);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> call, Throwable t) {
                applyCachedNearbyZones();
                updateSummary();
            }
        });
    }

    private List<PlatformModels.NoFlyZoneView> filterNearbyZones(TaskDetailActivity.TaskMapSnapshot currentSnapshot, List<PlatformModels.NoFlyZoneView> zones) {
        List<PlatformModels.NoFlyZoneView> result = new ArrayList<>();
        if (currentSnapshot == null || zones == null) {
            return result;
        }
        for (PlatformModels.NoFlyZoneView zone : zones) {
            if (zone == null || zone.centerLat == null || zone.centerLng == null) {
                continue;
            }
            float[] distance = new float[1];
            android.location.Location.distanceBetween(
                    currentSnapshot.target.latitude,
                    currentSnapshot.target.longitude,
                    zone.centerLat.doubleValue(),
                    zone.centerLng.doubleValue(),
                    distance
            );
            int threshold = Math.max(8000, currentSnapshot.radiusMeters + zone.radius + 3000);
            if (distance[0] <= threshold) {
                result.add(zone);
            }
        }
        return result;
    }

    private void updateSummary() {
        if (snapshot == null) {
            binding.tvRouteSummary.setText("正在加载执行路线、作业范围和附近禁飞区");
            return;
        }
        String extra = nearbyZones.isEmpty() ? "当前航线附近暂无禁飞区" : "附近识别到 " + nearbyZones.size() + " 个禁飞区";
        binding.tvRouteSummary.setText("路线约 " + snapshot.distanceLabel() + "，作业半径 " + snapshot.radiusMeters + " 米，" + extra);
        binding.tvMapPlaceholderDesc.setText("当前正在使用离线演示数据，页面展示任务路线、作业范围和禁飞区摘要。"
                + "\n路线约 " + snapshot.distanceLabel()
                + "，作业半径 " + snapshot.radiusMeters + " 米，" + extra + "。");
    }

    private void renderMap(TaskDetailActivity.TaskMapSnapshot currentSnapshot, List<PlatformModels.NoFlyZoneView> zones) {
        if (currentSnapshot == null || !mapRenderingEnabled || aMap == null) {
            return;
        }
        try {
            aMap.clear();

            aMap.addMarker(new MarkerOptions()
                    .position(currentSnapshot.routeStart)
                    .title("建议起飞点")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            aMap.addMarker(new MarkerOptions()
                    .position(currentSnapshot.target)
                    .title(currentSnapshot.title == null || currentSnapshot.title.trim().isEmpty() ? "任务中心点" : currentSnapshot.title)
                    .snippet(currentSnapshot.location == null ? "" : currentSnapshot.location)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            aMap.addPolyline(new PolylineOptions()
                    .addAll(currentSnapshot.routePoints)
                    .width(16f)
                    .color(ContextCompat.getColor(this, R.color.ui_map_route)));
            aMap.addCircle(new CircleOptions()
                    .center(currentSnapshot.target)
                    .radius(currentSnapshot.radiusMeters)
                    .strokeWidth(4f)
                    .strokeColor(ContextCompat.getColor(this, R.color.ui_map_zone_stroke))
                    .fillColor(ContextCompat.getColor(this, R.color.ui_map_zone_fill)));

            LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
            for (LatLng point : currentSnapshot.routePoints) {
                boundsBuilder.include(point);
            }
            NoFlyZoneMapRenderer.addRemoteZoneOverlays(aMap, zones, boundsBuilder);
            binding.mapContainer.post(() -> aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 140)));
        } catch (Throwable throwable) {
            disableMapRendering();
            updateSummary();
        }
    }

    private void applyCachedNearbyZones() {
        nearbyZones = filterNearbyZones(snapshot, NoFlyZoneMapRenderer.readCachedRemoteZones(this));
    }

    private boolean shouldEnableMapRendering() {
        // Always enable map rendering for production
        return true;
    }

    private void disableMapRendering() {
        mapRenderingEnabled = false;
        aMap = null;
        if (mapView != null) {
            binding.mapContainer.removeAllViews();
            mapView = null;
        }
        binding.layoutMapPlaceholder.setVisibility(View.VISIBLE);
    }

    private void showOfflineDemoToast() {
        if (offlineDemoNoticeShown) {
            return;
        }
        offlineDemoNoticeShown = true;
        android.widget.Toast.makeText(this, "正在使用离线演示数据", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapRenderingEnabled && mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapRenderingEnabled && mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapRenderingEnabled && mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (mapRenderingEnabled && mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }
}

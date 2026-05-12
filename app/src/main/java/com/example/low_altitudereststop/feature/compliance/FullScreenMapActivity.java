package com.example.low_altitudereststop.feature.compliance;

import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.feature.permission.AppPermissionManager;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.databinding.ActivityFullScreenMapBinding;
import java.util.List;
import java.util.Locale;

/**
 * 禁飞区全屏地图展示Activity。
 * <p>
 * 以全屏模式展示高德地图，加载并渲染本地和远程禁飞区数据，
 * 支持安全模式降级（地图不可用时显示缓存摘要），
 * 并在启动时请求定位权限。
 * </p>
 */
public class FullScreenMapActivity extends NavigableEdgeToEdgeActivity {

    private ActivityFullScreenMapBinding binding;
    private AMap aMap;
    private MapView mapView;
    private boolean mapRenderingEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        loadAndRenderZones();
    }

    private void initMap(Bundle savedInstanceState) {
        if (!mapRenderingEnabled) {
            binding.layoutMapPlaceholder.setVisibility(android.view.View.VISIBLE);
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
            binding.layoutMapPlaceholder.setVisibility(android.view.View.GONE);
            mapView.onCreate(savedInstanceState);
            aMap = mapView.getMap();
            NoFlyZoneMapRenderer.configureBaseMap(aMap, true);
        } catch (Throwable throwable) {
            disableMapRendering();
        }
    }

    private void loadAndRenderZones() {
        List<PlatformModels.NoFlyZoneView> zones = NoFlyZoneMapRenderer.readCachedRemoteZones(this);
        int count = zones == null ? 0 : zones.size();
        binding.tvMapPlaceholderDesc.setText(count == 0
                ? "当前设备已切换到安全模式，暂无禁飞区缓存数据。"
                : "当前设备已切换到安全模式，已缓存 " + count + " 个禁飞区摘要，不再加载高德地图实例。");
        if (zones.isEmpty()) {
            FlightManagementRepository repository = new FlightManagementRepository(this);
            if (mapRenderingEnabled && aMap != null) {
                NoFlyZoneMapRenderer.renderLocalZones(aMap, repository.listZones(""));
            }
            return;
        }
        if (mapRenderingEnabled && aMap != null) {
            NoFlyZoneMapRenderer.renderRemoteZones(aMap, zones);
        }
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
        binding.layoutMapPlaceholder.setVisibility(android.view.View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapRenderingEnabled && mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapRenderingEnabled && mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapRenderingEnabled && mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mapRenderingEnabled && mapView != null) mapView.onDestroy();
        super.onDestroy();
    }
}

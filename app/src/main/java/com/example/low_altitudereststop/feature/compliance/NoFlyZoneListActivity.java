package com.example.low_altitudereststop.feature.compliance;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.compliance.FlightManagementModels;
import com.example.low_altitudereststop.feature.compliance.FlightManagementRepository;
import com.example.low_altitudereststop.feature.compliance.NoFlyZoneMapRenderer;
import com.example.low_altitudereststop.feature.compliance.NetworkStatusHelper;
import com.example.low_altitudereststop.feature.permission.AppPermissionManager;
import com.example.low_altitudereststop.ui.UserRole;
import com.example.low_altitudereststop.databinding.ActivityNoFlyZoneListBinding;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoFlyZoneListActivity extends NavigableEdgeToEdgeActivity implements ZoneAdapter.OnZoneClickListener {

    private ActivityNoFlyZoneListBinding binding;
    private ZoneAdapter adapter;
    private FlightManagementRepository repository;
    private AMap aMap;
    private MapView mapView;
    private boolean managementEnabled;
    private boolean mapRenderingEnabled;
    private String currentKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoFlyZoneListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FlightManagementRepository(this);

        // Request location permission for map
        AppPermissionManager permissionManager = AppPermissionManager.getInstance();
        if (!permissionManager.isGranted(this, AppPermissionManager.GROUP_LOCATION)) {
            permissionManager.requestPermissions(this, permissionManager.buildLocationPayload(), new AppPermissionManager.PermissionResultCallback() {
                @Override
                public void onGranted() {
                    initMapAfterPermission();
                }

                @Override
                public void onDenied(@NonNull AppPermissionManager.PermissionState state, @NonNull AppPermissionManager.PermissionRequestPayload payload) {
                    initMapAfterPermission();
                }
            });
        } else {
            initMapAfterPermission();
        }

        adapter = new ZoneAdapter(this);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        binding.ivFullscreen.setOnClickListener(v -> {
            startActivity(new Intent(this, FullScreenMapActivity.class));
        });

        AuthModels.SessionInfo user = new SessionStore(this).getCachedUser();
        if (user != null) {
            UserRole role = UserRole.from(user.role);
            managementEnabled = role == UserRole.ENTERPRISE || "ADMIN".equalsIgnoreCase(user.role);
        }
        adapter.setManagementEnabled(managementEnabled);
        binding.btnAddZone.setVisibility(managementEnabled ? View.VISIBLE : View.GONE);
        binding.btnAddZone.setOnClickListener(v -> startActivity(new Intent(this, AddNoFlyZoneActivity.class)));
        binding.tvNetworkStatus.setText(NetworkStatusHelper.isOnline(this)
                ? "在线同步中"
                : "离线可查看");
        binding.etSearch.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                currentKeyword = textView.getText() == null ? "" : textView.getText().toString().trim();
                renderZones(repository.listZones(currentKeyword));
                return true;
            }
            return false;
        });

        binding.swipe.setOnRefreshListener(() -> {
            currentKeyword = binding.etSearch.getText() == null ? "" : binding.etSearch.getText().toString().trim();
            loadZones();
        });
        binding.swipe.setRefreshing(true);
        loadZones();
    }

    private void initMapAfterPermission() {
        mapRenderingEnabled = shouldEnableMapRendering();
        initMap(null);
    }

    @Override
    public void onZoneClick(FlightManagementModels.NoFlyZoneRecord zone) {
        if (zone != null && zone.centerLat != null && zone.centerLng != null && aMap != null) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new com.amap.api.maps.model.LatLng(zone.centerLat.doubleValue(), zone.centerLng.doubleValue()),
                    11.5f
            ));
        }
    }

    @Override
    public void onZoneEdit(FlightManagementModels.NoFlyZoneRecord zone) {
        Intent intent = new Intent(this, AddNoFlyZoneActivity.class);
        intent.putExtra(AddNoFlyZoneActivity.EXTRA_ZONE_ID, zone.id);
        startActivity(intent);
    }

    @Override
    public void onZoneDelete(FlightManagementModels.NoFlyZoneRecord zone) {
        new AlertDialog.Builder(this)
                .setTitle("删除禁飞区")
                .setMessage("确认删除 " + zone.name + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    repository.deleteZone(zone.id);
                    renderZones(repository.listZones(currentKeyword));
                    toast("禁飞区已删除");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadZones() {
        ApiClient.getAuthedService(this).listNoFlyZones().enqueue(new Callback<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> call, Response<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> response) {
                binding.swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    renderZones(repository.listZones(currentKeyword));
                    toast("平台禁飞区同步失败，已展示本地缓存");
                    return;
                }
                NoFlyZoneMapRenderer.cacheRemoteZones(NoFlyZoneListActivity.this, response.body().data);
                repository.mergeRemoteZones(response.body().data);
                renderZones(repository.listZones(currentKeyword));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> call, Throwable t) {
                binding.swipe.setRefreshing(false);
                renderZones(repository.listZones(currentKeyword));
                toast("网络异常，已展示离线缓存");
            }
        });
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
            NoFlyZoneMapRenderer.configureBaseMap(aMap, false);
        } catch (Throwable throwable) {
            disableMapRendering();
        }
    }

    private void renderZones(List<FlightManagementModels.NoFlyZoneRecord> zones) {
        adapter.submit(zones);
        renderZonesOnMap(zones);
        int count = zones == null ? 0 : zones.size();
        if (count == 0) {
            binding.tvMapHint.setText(getString(R.string.no_fly_zone_map_empty));
            binding.tvMapPlaceholderDesc.setText("当前暂无禁飞区，已为您保留安全摘要模式。");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.tvMapHint.setText(mapRenderingEnabled
                ? "地图已绘制 " + count + " 个禁飞区，支持企业坐标范围、生效时段与原因说明联动查看。"
                : "当前设备使用安全模式，已汇总 " + count + " 个禁飞区的状态与摘要信息。");
        binding.tvMapPlaceholderDesc.setText("当前设备已切换到安全模式，本页已汇总 " + count
                + " 个禁飞区，可继续查看列表、搜索、编辑和全屏摘要。");
    }

    private void renderZonesOnMap(List<FlightManagementModels.NoFlyZoneRecord> zones) {
        if (aMap == null) {
            return;
        }
        NoFlyZoneMapRenderer.renderLocalZones(aMap, zones);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (mapRenderingEnabled && mapView != null) {
            mapView.onResume();
        }
        currentKeyword = binding.etSearch.getText() == null ? "" : binding.etSearch.getText().toString().trim();
        renderZones(repository.listZones(currentKeyword));
        binding.tvNetworkStatus.setText(NetworkStatusHelper.isOnline(this)
                ? "在线同步中"
                : "离线可查看");
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


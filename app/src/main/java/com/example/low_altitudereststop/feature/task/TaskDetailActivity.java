package com.example.low_altitudereststop.feature.task;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityTaskDetailBinding;
import com.example.low_altitudereststop.feature.compliance.NoFlyZoneMapRenderer;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.example.low_altitudereststop.feature.order.OrderListActivity;
import com.example.low_altitudereststop.feature.order.PaymentActivity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 任务详情Activity，展示任务的完整信息与地图视图。
 * <p>
 * 从API或演示数据源获取任务详情，展示任务描述、坐标、作业半径、
 * 航线、禁飞区叠加层，支持接单/取消操作和全屏地图查看，
 * 企业用户可查看和管理任务状态。
 * </p>
 */
public class TaskDetailActivity extends NavigableEdgeToEdgeActivity {

    private static final LatLng DEFAULT_CENTER = new LatLng(29.56301, 106.55156);
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_TYPE = "task_type";
    public static final String EXTRA_TASK_LOCATION = "task_location";
    public static final String EXTRA_TASK_STATUS = "task_status";
    public static final String EXTRA_TASK_DEADLINE = "task_deadline";
    public static final String EXTRA_TASK_BUDGET = "task_budget";
    public static final String EXTRA_TASK_OWNER = "task_owner";

    private ActivityTaskDetailBinding binding;
    private MapView mapView;
    private AMap aMap;
    private long taskId;
    private String title;
    private String type;
    private String location;
    private String status;
    private String deadline;
    private String budget;
    private String ownerName;
    private boolean isPilot;
    private boolean isEnterprise;
    private boolean isEditing;
    private boolean demoSession;
    private boolean mapRenderingEnabled;
    private boolean offlineDemoNoticeShown;
    private TaskMapSnapshot currentSnapshot;
    private PlatformModels.TaskDetailView currentDetail;
    private List<PlatformModels.NoFlyZoneView> currentNearbyZones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        readIntentExtras();
        bindBackButton(binding.btnBack);
        demoSession = new SessionStore(this).isDemoSession();
        mapRenderingEnabled = shouldEnableMapRendering();
        initMap(savedInstanceState);
        initRoleActions();
        initStaticActions();
        bindFallbackDetail();
        loadTaskDetail();
    }

    private void readIntentExtras() {
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1L);
        title = getIntent().getStringExtra(EXTRA_TASK_TITLE);
        type = getIntent().getStringExtra(EXTRA_TASK_TYPE);
        location = getIntent().getStringExtra(EXTRA_TASK_LOCATION);
        status = getIntent().getStringExtra(EXTRA_TASK_STATUS);
        deadline = getIntent().getStringExtra(EXTRA_TASK_DEADLINE);
        budget = getIntent().getStringExtra(EXTRA_TASK_BUDGET);
        ownerName = getIntent().getStringExtra(EXTRA_TASK_OWNER);
    }

    private void initRoleActions() {
        SessionStore store = new SessionStore(this);
        AuthModels.SessionInfo user = store.getCachedUser();
        isPilot = user.role != null && user.role.equalsIgnoreCase("PILOT");
        isEnterprise = user.role != null && user.role.equalsIgnoreCase("ENTERPRISE");

        binding.btnPrimary.setVisibility(isPilot ? View.VISIBLE : View.GONE);
        binding.btnOrders.setVisibility((isPilot || isEnterprise) ? View.VISIBLE : View.GONE);
        binding.btnOrders.setText(isEnterprise ? "编辑" : "查看订单");
        binding.btnOrders.setOnClickListener(v -> {
            if (isEnterprise) {
                if (isEditing) {
                    submitEditAndRepublish();
                } else {
                    enterEditMode();
                }
                return;
            }
            startActivity(new Intent(this, OrderListActivity.class));
        });
        binding.btnPrimary.setOnClickListener(v -> {
            if (taskId <= 0) {
                toast("无效的任务 ID");
                return;
            }
            createOrderAndPay(taskId);
        });
    }

    private void initStaticActions() {
        binding.btnMapFullscreen.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskMapFullScreenActivity.class);
            intent.putExtra(TaskMapFullScreenActivity.EXTRA_TASK_ID, taskId);
            intent.putExtra(TaskMapFullScreenActivity.EXTRA_TASK_TITLE, currentDetail == null ? title : currentDetail.title);
            startActivity(intent);
        });
    }

    private void initMap(Bundle savedInstanceState) {
        if (!mapRenderingEnabled) {
            binding.layoutMapPlaceholder.setVisibility(View.VISIBLE);
            binding.btnMapFullscreen.setText("地图摘要");
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
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, 10f));
            binding.btnMapFullscreen.setText("全屏地图");
        } catch (Throwable throwable) {
            disableMapRendering();
        }
    }

    private void bindFallbackDetail() {
        PlatformModels.TaskDetailView fallback = AppScenarioMapper.findTaskDetail(taskId, title, location);
        if (fallback == null) {
            fallback = new PlatformModels.TaskDetailView();
            fallback.id = taskId > 0 ? taskId : null;
            fallback.title = title;
            fallback.taskType = type;
            fallback.description = "任务详情加载中。正在同步路线、作业区域和附近的禁飞区。";
            fallback.location = location;
            fallback.deadline = safe(deadline);
            fallback.budget = parseBudgetOrNull(budget);
            fallback.status = safe(status);
            fallback.ownerName = ownerName;
            TaskMapSnapshot snapshot = TaskMapSnapshot.fromIntent(taskId, title, type, location);
            fallback.latitude = BigDecimal.valueOf(snapshot.target.latitude);
            fallback.longitude = BigDecimal.valueOf(snapshot.target.longitude);
            fallback.routeStartLatitude = BigDecimal.valueOf(snapshot.routeStart.latitude);
            fallback.routeStartLongitude = BigDecimal.valueOf(snapshot.routeStart.longitude);
            fallback.operationRadiusMeters = snapshot.radiusMeters;
        }
        bindTaskDetail(fallback, false);
    }

    private void loadTaskDetail() {
        if (demoSession) {
            loadNearbyNoFlyZones();
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
                    loadNearbyNoFlyZones();
                    return;
                }
                bindTaskDetail(response.body().data, true);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.TaskDetailView>> call, Throwable t) {
                showOfflineDemoToast();
                loadNearbyNoFlyZones();
            }
        });
    }

    private void bindTaskDetail(PlatformModels.TaskDetailView detail, boolean loadZones) {
        currentDetail = detail;
        binding.tvTitle.setText(safe(detail.title));
        binding.tvMeta.setText(safe(detail.taskType) + " | " + safe(detail.location) + " | 截止 " + safe(detail.deadline) + " | 发布方 " + safe(detail.ownerName));
        binding.tvStatus.setText("状态: " + safe(detail.status) + " | 预算: " + amountLabel(detail.budget));
        binding.tvDescription.setText(detail.description == null || detail.description.trim().isEmpty() ? "暂无任务描述" : detail.description);
        currentSnapshot = TaskMapSnapshot.fromDetail(detail);
        binding.tvScope.setText("作业区域半径约为 " + currentSnapshot.radiusMeters + " 米，包含路线、目标区域和附近的禁飞区。");
        updateRouteSummary(currentSnapshot, currentNearbyZones.size());
        renderMap(currentSnapshot, currentNearbyZones);
        if (!isEditing) {
            fillEditorFields(detail);
        }
        if (loadZones) {
            loadNearbyNoFlyZones();
        }
    }

    private void fillEditorFields(PlatformModels.TaskDetailView detail) {
        binding.etTitle.setText(detail.title);
        binding.etDesc.setText(detail.description);
        binding.etLocation.setText(detail.location);
        binding.etDeadline.setText("-".equals(safe(detail.deadline)) ? TaskFormValidator.defaultDeadlineText() : detail.deadline);
        binding.etLat.setText(detail.latitude == null ? "" : detail.latitude.toPlainString());
        binding.etLng.setText(detail.longitude == null ? "" : detail.longitude.toPlainString());
        binding.etBudget.setText(detail.budget == null ? "" : detail.budget.toPlainString());
    }

    private void enterEditMode() {
        if (!isEnterprise) {
            return;
        }
        isEditing = true;
        fillEditorFields(currentDetail == null ? new PlatformModels.TaskDetailView() : currentDetail);
        clearEditorErrors();
        showEditFeedback("已进入编辑模式。您可以更新字段并重新发布任务。", false);
        binding.cardEditor.setVisibility(View.VISIBLE);
        binding.btnOrders.setText("保存并重新发布");
    }

    private void exitEditMode() {
        isEditing = false;
        binding.cardEditor.setVisibility(View.GONE);
        binding.btnOrders.setEnabled(true);
        binding.btnOrders.setText("编辑");
    }

    private void submitEditAndRepublish() {
        if (taskId <= 0) {
            toast("无效的任务 ID");
            return;
        }
        clearEditorErrors();
        TaskFormValidator.FormInput input = new TaskFormValidator.FormInput();
        input.taskType = currentDetail == null ? type : currentDetail.taskType;
        input.title = binding.etTitle.getText();
        input.description = binding.etDesc.getText();
        input.location = binding.etLocation.getText();
        input.deadline = binding.etDeadline.getText();
        input.latitude = binding.etLat.getText();
        input.longitude = binding.etLng.getText();
        input.budget = binding.etBudget.getText();
        TaskFormValidator.ValidationResult validationResult = TaskFormValidator.validate(input);
        if (!validationResult.isValid()) {
            applyEditorErrors(validationResult);
            showEditFeedback("提交前请修正有误的字段。", true);
            return;
        }
        binding.btnOrders.setEnabled(false);
        showEditFeedback("正在保存更新并重新发布任务...", false);
        updateTask(validationResult.request);
    }

    private void updateTask(PlatformModels.TaskRequest request) {
        ApiClient.getAuthedService(this).updateTask(taskId, request).enqueue(new Callback<ApiEnvelope<PlatformModels.TaskView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.TaskView>> call, Response<ApiEnvelope<PlatformModels.TaskView>> response) {
                ApiEnvelope<PlatformModels.TaskView> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.data == null) {
                    binding.btnOrders.setEnabled(true);
                    showEditFeedback(resolveMessage(envelope, "任务更新失败，请稍后重试。"), true);
                    return;
                }
                publishTask(request);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.TaskView>> call, Throwable t) {
                handleRepublishNetworkFailure(request, "任务更新失败并已加入重试队列：" + t.getMessage());
            }
        });
    }

    private void publishTask(PlatformModels.TaskRequest request) {
        ApiClient.getAuthedService(this).publishTask(taskId).enqueue(new Callback<ApiEnvelope<PlatformModels.TaskView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.TaskView>> call, Response<ApiEnvelope<PlatformModels.TaskView>> response) {
                ApiEnvelope<PlatformModels.TaskView> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.data == null) {
                    binding.btnOrders.setEnabled(true);
                    showEditFeedback(resolveMessage(envelope, "任务重新发布失败，请稍后重试。"), true);
                    return;
                }
                applyLocalRequest(request, envelope.data.status);
                exitEditMode();
                showEditFeedback("任务已更新并开始重新发布流程。", false);
                binding.tvEditFeedback.setVisibility(View.VISIBLE);
                setResult(RESULT_OK);
                toast("任务已更新并重新发布");
                loadTaskDetail();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.TaskView>> call, Throwable t) {
                handleRepublishNetworkFailure(request, "网络异常。任务重新发布已加入队列：" + t.getMessage());
            }
        });
    }

    private void handleRepublishNetworkFailure(PlatformModels.TaskRequest request, String message) {
        enqueueRepublish(request);
        applyLocalRequest(request, "加入重新发布队列");
        exitEditMode();
        showEditFeedback(message, true);
        binding.tvEditFeedback.setVisibility(View.VISIBLE);
        setResult(RESULT_OK);
        toast("重新发布已加入同步队列");
    }

    private void enqueueRepublish(PlatformModels.TaskRequest request) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("taskType", request.taskType);
        payload.put("title", request.title);
        payload.put("description", request.description);
        payload.put("location", request.location);
        payload.put("deadline", request.deadline);
        payload.put("latitude", request.latitude == null ? "0" : request.latitude.toPlainString());
        payload.put("longitude", request.longitude == null ? "0" : request.longitude.toPlainString());
        payload.put("budget", request.budget == null ? "0" : request.budget.toPlainString());
        OutboxSyncManager.enqueue(this, "UPDATE_TASK_REPUBLISH", payload);
    }

    private void applyLocalRequest(PlatformModels.TaskRequest request, String newStatus) {
        PlatformModels.TaskDetailView detail = currentDetail == null ? new PlatformModels.TaskDetailView() : currentDetail;
        detail.id = taskId > 0 ? taskId : detail.id;
        detail.title = request.title;
        detail.taskType = request.taskType;
        detail.description = request.description;
        detail.location = request.location;
        detail.deadline = request.deadline;
        detail.latitude = request.latitude;
        detail.longitude = request.longitude;
        detail.budget = request.budget;
        detail.status = newStatus;
        detail.ownerName = safeOwner();
        if (detail.routeStartLatitude == null && request.latitude != null) {
            detail.routeStartLatitude = request.latitude.subtract(new BigDecimal("0.018"));
        }
        if (detail.routeStartLongitude == null && request.longitude != null) {
            detail.routeStartLongitude = request.longitude.subtract(new BigDecimal("0.022"));
        }
        if (detail.operationRadiusMeters == null) {
            detail.operationRadiusMeters = "MAPPING".equalsIgnoreCase(request.taskType) ? 1200 : 900;
        }
        bindTaskDetail(detail, false);
    }

    private void clearEditorErrors() {
        binding.etTitle.setError(null);
        binding.etDesc.setError(null);
        binding.etLocation.setError(null);
        binding.etDeadline.setError(null);
        binding.etLat.setError(null);
        binding.etLng.setError(null);
        binding.etBudget.setError(null);
    }

    private void applyEditorErrors(TaskFormValidator.ValidationResult validationResult) {
        binding.etTitle.setError(validationResult.errorFor("title"));
        binding.etDesc.setError(validationResult.errorFor("description"));
        binding.etLocation.setError(validationResult.errorFor("location"));
        binding.etDeadline.setError(validationResult.errorFor("deadline"));
        binding.etLat.setError(validationResult.errorFor("latitude"));
        binding.etLng.setError(validationResult.errorFor("longitude"));
        binding.etBudget.setError(validationResult.errorFor("budget"));
    }

    private void showEditFeedback(String message, boolean error) {
        binding.tvEditFeedback.setVisibility(View.VISIBLE);
        binding.tvEditFeedback.setText(message);
        binding.tvEditFeedback.setTextColor(ContextCompat.getColor(this, error ? R.color.ui_error : R.color.ui_success));
    }

    private void loadNearbyNoFlyZones() {
        if (currentSnapshot == null) {
            return;
        }
        if (demoSession) {
            applyCachedNearbyZones();
            return;
        }
        ApiClient.getAuthedService(this).listNoFlyZones().enqueue(new Callback<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> call, Response<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    applyCachedNearbyZones();
                    return;
                }
                currentNearbyZones = filterNearbyZones(currentSnapshot, response.body().data);
                updateRouteSummary(currentSnapshot, currentNearbyZones.size());
                renderMap(currentSnapshot, currentNearbyZones);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> call, Throwable t) {
                applyCachedNearbyZones();
            }
        });
    }

    private List<PlatformModels.NoFlyZoneView> filterNearbyZones(TaskMapSnapshot snapshot, List<PlatformModels.NoFlyZoneView> zones) {
        List<PlatformModels.NoFlyZoneView> nearby = new ArrayList<>();
        if (snapshot == null || zones == null) {
            return nearby;
        }
        for (PlatformModels.NoFlyZoneView zone : zones) {
            if (zone == null || zone.centerLat == null || zone.centerLng == null) {
                continue;
            }
            double distance = TaskMapSnapshot.haversineDistance(
                    snapshot.target.latitude,
                    snapshot.target.longitude,
                    zone.centerLat.doubleValue(),
                    zone.centerLng.doubleValue()
            );
            int threshold = Math.max(8000, snapshot.radiusMeters + zone.radius + 3000);
            if (distance <= threshold) {
                nearby.add(zone);
            }
        }
        return nearby;
    }

    private void updateRouteSummary(TaskMapSnapshot snapshot, int nearbyZoneCount) {
        if (snapshot == null) {
            binding.tvRouteSummary.setText("正在加载路线概览");
            return;
        }
        String extra = nearbyZoneCount > 0 ? "，附近禁飞区数量：" + nearbyZoneCount : "，未检测到附近禁飞区";
        binding.tvRouteSummary.setText("路线从建议的起飞点进入作业区域，总距离约 " + snapshot.distanceLabel() + extra);
    }

    private void renderMap(TaskMapSnapshot snapshot, List<PlatformModels.NoFlyZoneView> zones) {
        if (snapshot == null) {
            return;
        }
        if (!mapRenderingEnabled || aMap == null) {
            updateMapPlaceholder(snapshot, zones);
            return;
        }
        try {
            aMap.clear();
            aMap.addMarker(new MarkerOptions()
                    .position(snapshot.routeStart)
                    .title("起飞点")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            aMap.addMarker(new MarkerOptions()
                    .position(snapshot.target)
                    .title(snapshot.title == null || snapshot.title.trim().isEmpty() ? "任务目标" : snapshot.title)
                    .snippet(snapshot.location == null ? "" : snapshot.location)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            aMap.addPolyline(new PolylineOptions()
                    .addAll(snapshot.routePoints)
                    .width(14f)
                    .color(ContextCompat.getColor(this, R.color.ui_map_route)));
            aMap.addCircle(new CircleOptions()
                    .center(snapshot.target)
                    .radius(snapshot.radiusMeters)
                    .strokeWidth(4f)
                    .strokeColor(ContextCompat.getColor(this, R.color.ui_map_zone_stroke))
                    .fillColor(ContextCompat.getColor(this, R.color.ui_map_zone_fill)));

            LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
            for (LatLng point : snapshot.routePoints) {
                boundsBuilder.include(point);
            }
            NoFlyZoneMapRenderer.addRemoteZoneOverlays(aMap, zones, boundsBuilder);
            LatLngBounds bounds = boundsBuilder.build();
            binding.cardMap.post(() -> aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120)));
        } catch (Throwable throwable) {
            disableMapRendering();
            updateMapPlaceholder(snapshot, zones);
        }
    }

    private void updateMapPlaceholder(TaskMapSnapshot snapshot, List<PlatformModels.NoFlyZoneView> zones) {
        TextView titleView = binding.tvMapPlaceholderTitle;
        TextView descView = binding.tvMapPlaceholderDesc;
        titleView.setText("基础航线数据");
        int zoneCount = zones == null ? 0 : zones.size();
        String zoneLabel = zoneCount > 0 ? "附近禁飞区 " + zoneCount + " 个" : "附近暂无禁飞区";
        descView.setText("当前正在使用离线演示数据。建议起飞点至目标点约 "
                + snapshot.distanceLabel()
                + "，作业半径 "
                + snapshot.radiusMeters
                + " 米，"
                + zoneLabel
                + "。");
    }

    private void createOrderAndPay(long taskId) {
        binding.btnPrimary.setEnabled(false);
        PlatformModels.OrderCreateRequest req = new PlatformModels.OrderCreateRequest();
        req.taskId = taskId;
        ApiClient.getAuthedService(this).createOrder(req).enqueue(new Callback<ApiEnvelope<PlatformModels.OrderView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.OrderView>> call, Response<ApiEnvelope<PlatformModels.OrderView>> response) {
                binding.btnPrimary.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    toast("创建订单失败");
                    return;
                }
                PlatformModels.OrderView order = response.body().data;
                Intent intent = new Intent(TaskDetailActivity.this, PaymentActivity.class);
                intent.putExtra(PaymentActivity.EXTRA_ORDER_ID, order.id == null ? -1L : order.id);
                intent.putExtra(PaymentActivity.EXTRA_SUCCESS_MESSAGE, "接单成功");
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.OrderView>> call, Throwable t) {
                binding.btnPrimary.setEnabled(true);
                toast("网络异常: " + t.getMessage());
            }
        });
    }

    private BigDecimal parseBudgetOrNull(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : new BigDecimal(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String safeOwner() {
        return currentDetail != null && currentDetail.ownerName != null ? currentDetail.ownerName : ownerName;
    }

    private String amountLabel(BigDecimal amount) {
        return amount == null ? "-" : amount.toPlainString();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String resolveMessage(ApiEnvelope<?> envelope, String fallback) {
        if (envelope == null || envelope.message == null || envelope.message.trim().isEmpty()) {
            return fallback;
        }
        return envelope.message;
    }

    private void applyCachedNearbyZones() {
        currentNearbyZones = filterNearbyZones(currentSnapshot, NoFlyZoneMapRenderer.readCachedRemoteZones(this));
        updateRouteSummary(currentSnapshot, currentNearbyZones.size());
        renderMap(currentSnapshot, currentNearbyZones);
    }

    private boolean shouldEnableMapRendering() {
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
        binding.btnMapFullscreen.setText("地图摘要");
    }

    private void showOfflineDemoToast() {
        if (offlineDemoNoticeShown) {
            return;
        }
        offlineDemoNoticeShown = true;
        toast("正在使用离线演示数据");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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

    static final class TaskMapSnapshot {
        final long taskId;
        final String title;
        final String taskType;
        final String location;
        final LatLng routeStart;
        final LatLng target;
        final int radiusMeters;
        final List<LatLng> routePoints;

        TaskMapSnapshot(long taskId, String title, String taskType, String location, LatLng routeStart, LatLng target, int radiusMeters) {
            this.taskId = taskId;
            this.title = title;
            this.taskType = taskType;
            this.location = location;
            this.routeStart = routeStart;
            this.target = target;
            this.radiusMeters = radiusMeters;
            this.routePoints = buildRoutePoints(routeStart, target, taskType);
        }

        static TaskMapSnapshot fromDetail(PlatformModels.TaskDetailView detail) {
            if (detail == null || detail.latitude == null || detail.longitude == null) {
                return fromIntent(detail == null || detail.id == null ? -1L : detail.id, detail == null ? null : detail.title, detail == null ? null : detail.taskType, detail == null ? null : detail.location);
            }
            LatLng target = new LatLng(detail.latitude.doubleValue(), detail.longitude.doubleValue());
            LatLng routeStart = detail.routeStartLatitude != null && detail.routeStartLongitude != null
                    ? new LatLng(detail.routeStartLatitude.doubleValue(), detail.routeStartLongitude.doubleValue())
                    : new LatLng(detail.latitude.doubleValue() - 0.018d, detail.longitude.doubleValue() - 0.022d);
            int radius = detail.operationRadiusMeters == null ? 800 : detail.operationRadiusMeters;
            return new TaskMapSnapshot(detail.id == null ? -1L : detail.id, detail.title, detail.taskType, detail.location, routeStart, target, radius);
        }

        static TaskMapSnapshot fromIntent(long taskId, String title, String taskType, String location) {
            LatLng target = targetFor(taskId, title, location);
            LatLng routeStart = new LatLng(target.latitude - 0.018d, target.longitude - 0.022d);
            int radius = "MAPPING".equalsIgnoreCase(taskType) ? 1200 : 900;
            return new TaskMapSnapshot(taskId, title, taskType, location, routeStart, target, radius);
        }

        String distanceLabel() {
            double distanceMeters = haversineDistance(routeStart.latitude, routeStart.longitude, target.latitude, target.longitude);
            return String.format(Locale.US, "%.1f km", distanceMeters / 1000.0);
        }

        static double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
            double r = 6371000.0;
            double dLat = Math.toRadians(lat2 - lat1);
            double dLng = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLng / 2) * Math.sin(dLng / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return r * c;
        }

        private static List<LatLng> buildRoutePoints(LatLng routeStart, LatLng target, String taskType) {
            List<LatLng> points = new ArrayList<>();
            points.add(routeStart);
            double latDelta = target.latitude - routeStart.latitude;
            double lngDelta = target.longitude - routeStart.longitude;
            double firstOffset = "MAPPING".equalsIgnoreCase(taskType) ? 0.006d : 0.004d;
            double secondOffset = "MAPPING".equalsIgnoreCase(taskType) ? 0.003d : 0.002d;
            points.add(new LatLng(routeStart.latitude + latDelta * 0.38d + firstOffset, routeStart.longitude + lngDelta * 0.28d));
            points.add(new LatLng(routeStart.latitude + latDelta * 0.72d + secondOffset, routeStart.longitude + lngDelta * 0.68d));
            points.add(target);
            return points;
        }

        private static LatLng targetFor(long taskId, String title, String location) {
            if (taskId == 101L) {
                return new LatLng(29.56376d, 106.55046d);
            }
            if (taskId == 102L) {
                return new LatLng(29.72358d, 106.63882d);
            }
            if (contains(location, "Jiangbei") || contains(title, "Yangtze")) {
                return new LatLng(29.56376d, 106.55046d);
            }
            if (contains(location, "Yubei") || contains(title, "Park")) {
                return new LatLng(29.72358d, 106.63882d);
            }
            return DEFAULT_CENTER;
        }

        private static boolean contains(String source, String keyword) {
            return source != null && source.contains(keyword);
        }
    }
}

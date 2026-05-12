package com.example.low_altitudereststop.feature.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavOptions;
import com.example.low_altitudereststop.R;
import com.amap.api.location.AMapLocation;
import com.example.low_altitudereststop.feature.location.AmapLocationProvider;
import com.example.low_altitudereststop.feature.permission.AppPermissionManager;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.core.storage.OperationOutboxEntity;
import com.example.low_altitudereststop.core.storage.OperationOutboxStore;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.example.low_altitudereststop.feature.order.OrderListActivity;
import com.example.low_altitudereststop.ui.IconRegistry;
import com.example.low_altitudereststop.ui.RoleUiConfig;
import com.example.low_altitudereststop.ui.UsageAnalyticsStore;
import com.example.low_altitudereststop.ui.UserRole;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 首页Fragment，展示用户欢迎信息、快捷操作入口、运营指标图表和天气模块。
 * <p>
 * 根据用户角色动态配置快捷操作和指标标签，集成高德定位获取当前位置，
 * 通过WeatherRepository获取实时天气和飞行适宜性评估，
 * 支持定位权限管理、天气自动刷新、安全降级等容错机制。
 * </p>
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final long WEATHER_REFRESH_INTERVAL_MS = 15 * 60 * 1000L;
    private static final long LOCATION_TIMEOUT_MS = 15_000L;
    private static final long MAX_LAST_KNOWN_AGE_MS = 10 * 60 * 1000L;
    private static final float MAX_ACCEPTABLE_LOCATION_ACCURACY_METERS = 150f;
    private static final double FALLBACK_LATITUDE = 29.56301d;
    private static final double FALLBACK_LONGITUDE = 106.55156d;
    private static volatile PlatformModels.RealtimeWeatherView sLastKnownWeather;
    private static volatile long sLastWeatherFetchedAt;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
    private final Runnable weatherRefreshRunnable = () -> refreshWeather(false, false);

    private WeatherRepository weatherRepository;
    private AmapLocationProvider amapLocationProvider;
    private AppPermissionManager permissionManager;
    private OperationLogStore operationLogStore;
    private Snackbar locationPermissionSnackbar;
    private Future<?> pendingWeatherTask;
    private long lastWeatherRefreshTime;
    private double lastWeatherRequestLat;
    private double lastWeatherRequestLon;
    private ImageView ivWeatherIcon;
    private TextView tvWeatherCondition;
    private TextView tvWeatherLocation;
    private TextView tvWeatherResult;
    private TextView tvWeatherSummary;
    private TextView tvWeatherOverview;
    private TextView tvWeatherTemperature;
    private TextView tvWeatherWind;
    private TextView tvWeatherPrecipitation;
    private TextView tvWeatherThunderstorm;
    private TextView tvWeatherThunderstormLabel;
    private TextView tvWeatherMeta;
    private TextView tvWeatherError;
    private View btnWeatherDetail;
    private LineChart chartOverview;
    private PlatformModels.RealtimeWeatherView currentWeather;
    private final AppPermissionManager.PermissionListener locationPermissionListener = (permissionGroup, state) -> {
        if (!AppPermissionManager.GROUP_LOCATION.equals(permissionGroup) || !isAdded()) {
            return;
        }
        if (state.granted) {
            dismissLocationPermissionPrompt();
            refreshWeather(false, true);
        } else {
            renderWeatherError(resolveLocationErrorMessage(), true);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        TextView tvQuickTask = view.findViewById(R.id.tv_quick_task);
        TextView tvQuickCompliance = view.findViewById(R.id.tv_quick_compliance);
        TextView tvQuickProfile = view.findViewById(R.id.tv_quick_profile);
        TextView tvTaskLabel = view.findViewById(R.id.tv_task_label);
        TextView tvAlertLabel = view.findViewById(R.id.tv_alert_label);
        TextView tvCourseLabel = view.findViewById(R.id.tv_course_label);
        TextView tvQueueLabel = view.findViewById(R.id.tv_queue_label);
        TextView tvTaskCount = view.findViewById(R.id.tv_task_count);
        TextView tvAlertCount = view.findViewById(R.id.tv_alert_count);
        TextView tvCourseCount = view.findViewById(R.id.tv_course_count);
        TextView tvQueueCount = view.findViewById(R.id.tv_queue_count);
        MaterialCardView cardHero = view.findViewById(R.id.card_home_hero);
        chartOverview = view.findViewById(R.id.chart_overview);
        View quickTaskButton = view.findViewById(R.id.btn_go_task);
        View quickComplianceButton = view.findViewById(R.id.btn_go_compliance);
        View quickProfileButton = view.findViewById(R.id.btn_go_profile);
        ImageView ivQuickTask = view.findViewById(R.id.iv_quick_task);
        ImageView ivQuickCompliance = view.findViewById(R.id.iv_quick_compliance);
        ImageView ivQuickProfile = view.findViewById(R.id.iv_quick_profile);
        ImageView ivStatTask = view.findViewById(R.id.iv_stat_task);
        ImageView ivStatAlert = view.findViewById(R.id.iv_stat_alert);
        ImageView ivStatCourse = view.findViewById(R.id.iv_stat_course);
        ImageView ivStatSync = view.findViewById(R.id.iv_stat_sync);
        ivWeatherIcon = view.findViewById(R.id.iv_weather_icon);
        tvWeatherCondition = view.findViewById(R.id.tv_weather_condition);
        tvWeatherLocation = view.findViewById(R.id.tv_weather_location);
        tvWeatherResult = view.findViewById(R.id.tv_weather_result);
        tvWeatherSummary = view.findViewById(R.id.tv_weather_summary);
        tvWeatherOverview = view.findViewById(R.id.tv_weather_overview);
        tvWeatherTemperature = view.findViewById(R.id.tv_weather_temperature);
        tvWeatherWind = view.findViewById(R.id.tv_weather_wind);
        tvWeatherPrecipitation = view.findViewById(R.id.tv_weather_precipitation);
        tvWeatherThunderstorm = view.findViewById(R.id.tv_weather_thunderstorm);
        tvWeatherThunderstormLabel = view.findViewById(R.id.tv_weather_thunderstorm_label);
        tvWeatherMeta = view.findViewById(R.id.tv_weather_meta);
        tvWeatherError = view.findViewById(R.id.tv_weather_error);
        btnWeatherDetail = view.findViewById(R.id.btn_weather_detail);
        weatherRepository = new WeatherRepository(requireContext());
        amapLocationProvider = new AmapLocationProvider(requireContext());
        permissionManager = AppPermissionManager.getInstance();
        operationLogStore = new OperationLogStore(requireContext());

        SessionStore store = new SessionStore(requireContext());
        AuthModels.SessionInfo user = store.getCachedUser();
        FileCache cache = new FileCache(requireContext());
        OperationOutboxStore outboxStore = new OperationOutboxStore(requireContext());
        UsageAnalyticsStore analyticsStore = new UsageAnalyticsStore(requireContext());
        UserRole userRole = UserRole.from(user.role);
        RoleUiConfig config = RoleUiConfig.from(userRole);
        String name = user.realName == null || user.realName.trim().isEmpty() ? (user.username == null ? "用户" : user.username) : user.realName;

        tvWelcome.setText("你好，" + name);
        tvQuickTask.setText(primaryActionLabel(userRole));
        tvQuickCompliance.setText(secondaryActionLabel(userRole));
        tvQuickProfile.setText(tertiaryActionLabel(userRole));
        tvTaskLabel.setText(metricTaskLabel(userRole));
        tvAlertLabel.setText(metricAlertLabel(userRole));
        tvCourseLabel.setText(metricCourseLabel(userRole));
        tvQueueLabel.setText(metricQueueLabel(userRole));
        int taskCount = readSize(cache, "tasks.json", new TypeToken<List<Object>>() {
        }.getType());
        if (taskCount == 0) {
            taskCount = AppScenarioMapper.buildFallbackTasks().size();
        }
        int alertCount = readSize(cache, "alerts.json", new TypeToken<List<Object>>() {
        }.getType());
        if (alertCount == 0) {
            alertCount = AppScenarioMapper.buildFallbackAlerts(userRole).size();
        }
        int courseCount = readSize(cache, "courses.json", new TypeToken<List<Object>>() {
        }.getType());
        int queueCount = countPending(outboxStore.listAll());
        tvTaskCount.setText(String.valueOf(taskCount));
        tvAlertCount.setText(String.valueOf(alertCount));
        tvCourseCount.setText(String.valueOf(courseCount));
        tvQueueCount.setText(String.valueOf(queueCount));
        cardHero.setCardBackgroundColor(ContextCompat.getColor(requireContext(), config.accentSurfaceRes));
        renderOverviewChart(taskCount, alertCount, courseCount, queueCount);
        maybeAnimateRoleGuide(cardHero, analyticsStore, userRole);
        IconRegistry.applyIcon(ivQuickTask, R.drawable.ic_quick_task, R.drawable.ic_fallback_generic);
        IconRegistry.applyIcon(ivQuickCompliance, R.drawable.ic_quick_compliance, R.drawable.ic_fallback_generic);
        IconRegistry.applyIcon(ivQuickProfile, R.drawable.ic_quick_profile, R.drawable.ic_fallback_generic);
        IconRegistry.applyIcon(ivStatTask, R.drawable.ic_stat_task, R.drawable.ic_fallback_generic);
        IconRegistry.applyIcon(ivStatAlert, R.drawable.ic_stat_alert, R.drawable.ic_fallback_alert);
        IconRegistry.applyIcon(ivStatCourse, R.drawable.ic_stat_course, R.drawable.ic_fallback_generic);
        IconRegistry.applyIcon(ivStatSync, R.drawable.ic_stat_sync, R.drawable.ic_fallback_generic);
        bindQuickActionState(quickTaskButton, ivQuickTask, tvQuickTask, true, false, true);
        bindQuickActionState(quickComplianceButton, ivQuickCompliance, tvQuickCompliance, true, false, false);
        bindQuickActionState(quickProfileButton, ivQuickProfile, tvQuickProfile, true, false, false);
        applyQuickActionSizing(quickTaskButton, quickComplianceButton, quickProfileButton, ivQuickTask);

        quickTaskButton.setOnClickListener(v -> {
            analyticsStore.trackFeature(userRole, "task");
            navigateToTopLevel(view, R.id.taskFragment);
        });
        quickComplianceButton.setOnClickListener(v -> {
            analyticsStore.trackFeature(userRole, "compliance");
            navigateToTopLevel(view, R.id.complianceFragment);
        });
        quickProfileButton.setOnClickListener(v -> {
            analyticsStore.trackFeature(userRole, "home_orders");
            startActivity(new Intent(requireContext(), OrderListActivity.class));
        });
        View btnRefresh = view.findViewById(R.id.btn_weather_refresh);
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.home_weather_refreshing, Toast.LENGTH_SHORT).show();
            refreshWeather(true, true);
        });
        btnRefresh.setOnLongClickListener(v -> {
            weatherRepository.clearCache();
            Toast.makeText(requireContext(), "天气缓存已清除，正在强制刷新", Toast.LENGTH_SHORT).show();
            refreshWeather(true, true);
            return true;
        });
        btnWeatherDetail.setOnClickListener(v -> openWeatherDetail());
        btnWeatherDetail.setEnabled(false);
        renderWeatherLoading(getString(R.string.home_weather_location_placeholder), getString(R.string.home_weather_loading_detail));
        } catch (Throwable throwable) {
            operationLogStore = new OperationLogStore(requireContext());
            operationLogStore.appendCrash("HOME", "init_failed: " + throwable.getClass().getSimpleName());
            renderSafeFallbackHome(view);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (permissionManager != null) {
            permissionManager.registerListener(AppPermissionManager.GROUP_LOCATION, locationPermissionListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissLocationPermissionPrompt();
        if (permissionManager != null) {
            permissionManager.unregisterListener(locationPermissionListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (currentWeather == null && sLastKnownWeather != null
                    && System.currentTimeMillis() - sLastWeatherFetchedAt < WEATHER_REFRESH_INTERVAL_MS) {
                currentWeather = sLastKnownWeather;
                lastWeatherRefreshTime = sLastWeatherFetchedAt;
                if (tvWeatherLocation != null) {
                    bindWeather(currentWeather);
                    scheduleNextWeatherRefresh();
                }
            }
            long now = System.currentTimeMillis();
            if (now - lastWeatherRefreshTime >= WEATHER_REFRESH_INTERVAL_MS) {
                refreshWeather(false, false);
            }
        } catch (Throwable throwable) {
            appendLocationCrash("resume_refresh_failed " + throwable.getClass().getSimpleName());
            renderSafeWeatherFallback();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mainHandler.removeCallbacks(weatherRefreshRunnable);
        if (amapLocationProvider != null) {
            amapLocationProvider.cancel();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacks(weatherRefreshRunnable);
        cancelPendingWeatherTask();
        if (amapLocationProvider != null) {
            amapLocationProvider.destroy();
            amapLocationProvider = null;
        }
        ivWeatherIcon = null;
        tvWeatherCondition = null;
        tvWeatherLocation = null;
        tvWeatherResult = null;
        tvWeatherSummary = null;
        tvWeatherOverview = null;
        tvWeatherTemperature = null;
        tvWeatherWind = null;
        tvWeatherPrecipitation = null;
        tvWeatherThunderstorm = null;
        tvWeatherThunderstormLabel = null;
        tvWeatherMeta = null;
        tvWeatherError = null;
        btnWeatherDetail = null;
        currentWeather = null;
        chartOverview = null;
    }

    private void renderOverviewChart(int taskCount, int alertCount, int courseCount, int queueCount) {
        if (chartOverview == null) {
            return;
        }
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, taskCount));
        entries.add(new Entry(1f, alertCount));
        entries.add(new Entry(2f, courseCount));
        entries.add(new Entry(3f, queueCount));

        LineDataSet dataSet = new LineDataSet(entries, "运营指标");
        int primary = ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_light_primary);
        int secondary = ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_light_secondary);
        dataSet.setColor(primary);
        dataSet.setCircleColor(secondary);
        dataSet.setCircleHoleColor(ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_light_surface));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_primary));
        dataSet.setLineWidth(2.4f);
        dataSet.setCircleRadius(4.8f);
        dataSet.setDrawFilled(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chartOverview.setData(new LineData(dataSet));
        chartOverview.setTouchEnabled(false);
        chartOverview.setPinchZoom(false);
        chartOverview.setScaleEnabled(false);
        chartOverview.setDrawGridBackground(false);
        chartOverview.setDrawBorders(false);
        chartOverview.setExtraOffsets(8f, 8f, 8f, 8f);

        Description description = new Description();
        description.setText("");
        chartOverview.setDescription(description);

        Legend legend = chartOverview.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = chartOverview.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_muted));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"任务", "告警", "课程", "待处理"}));

        YAxis axisLeft = chartOverview.getAxisLeft();
        axisLeft.setAxisMinimum(0f);
        axisLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_muted));
        axisLeft.setGridColor(ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_light_outline_variant));

        YAxis axisRight = chartOverview.getAxisRight();
        axisRight.setEnabled(false);

        chartOverview.invalidate();
    }

    private void navigateToTopLevel(@NonNull View view, int destinationId) {
        NavController navController = Navigation.findNavController(view);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();
        navController.navigate(destinationId, null, options);
    }

    private int readSize(FileCache cache, String name, Type type) {
        try {
            String json = cache.read(name);
            if (json == null || json.trim().isEmpty()) {
                return 0;
            }
            List<?> data = new Gson().fromJson(json, type);
            return data == null ? 0 : data.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int countPending(List<OperationOutboxEntity> items) {
        if (items == null) {
            return 0;
        }
        int count = 0;
        for (OperationOutboxEntity item : items) {
            if (!"SUCCESS".equals(item.status)) {
                count++;
            }
        }
        return count;
    }

    private void refreshWeather() {
        refreshWeather(false, false);
    }

    private void refreshWeather(boolean requestPermissionIfNeeded) {
        refreshWeather(requestPermissionIfNeeded, false);
    }

    private void refreshWeather(boolean requestPermissionIfNeeded, boolean forceRefresh) {
        if (!isAdded()) {
            return;
        }
        mainHandler.removeCallbacks(weatherRefreshRunnable);
        if (!hasLocationPermission()) {
            appendLocationAudit("permission_missing");
            maybePromptLocationPermission(requestPermissionIfNeeded);
            loadWeatherForLocation(createFallbackLocation(), "demo_permission_fallback", forceRefresh);
            return;
        }
        dismissLocationPermissionPrompt();

        if (!permissionManager.isLocationServiceEnabled(requireContext())) {
            appendLocationAudit("location_service_disabled");
            maybePromptLocationServiceSettings();
            loadWeatherForLocation(createFallbackLocation(), "demo_location_disabled_fallback", forceRefresh);
            return;
        }

        Location location = getBestLocation();
        if (location == null) {
            appendLocationAudit("last_known_location_missing");
            requestCurrentLocation(forceRefresh);
            return;
        }
        if (shouldRequestFreshLocation(location)) {
            appendLocationAudit("last_known_requires_refresh ageMs=" + calculateLocationAgeMs(location)
                    + " accuracy=" + readLocationAccuracy(location));
            requestCurrentLocation(forceRefresh);
            return;
        }
        loadWeatherForLocation(location, "last_known", forceRefresh);
    }

    private void loadWeatherForLocation(@NonNull Location location, @NonNull String source, boolean forceRefresh) {
        lastWeatherRequestLat = location.getLatitude();
        lastWeatherRequestLon = location.getLongitude();
        String locationDebug = String.format(Locale.getDefault(), "📍 %.5f, %.5f", lastWeatherRequestLat, lastWeatherRequestLon);
        renderWeatherLoading(
                locationDebug,
                getString(forceRefresh ? R.string.home_weather_refreshing_detail : R.string.home_weather_loading_detail)
        );
        appendLocationAudit("weather_request source=" + source
                + " lat=" + location.getLatitude()
                + " lon=" + location.getLongitude()
                + " time=" + location.getTime()
                + " forceRefresh=" + forceRefresh);
        pendingWeatherTask = weatherExecutor.submit(() -> {
            try {
                PlatformModels.RealtimeWeatherView weather = weatherRepository.getRealtimeWeather(
                        location.getLongitude(),
                        location.getLatitude(),
                        forceRefresh
                );
                pendingWeatherTask = null;
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    appendLocationAudit("weather_success source=" + source + " adcode=" + weather.adcode);
                    bindWeather(weather);
                    scheduleNextWeatherRefresh();
                });
            } catch (Exception exception) {
                String message = exception.getMessage();
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    appendLocationCrash("weather_failed source=" + source + " message=" + (message == null ? "" : message));
                    renderWeatherError(message == null || message.trim().isEmpty() ? getString(R.string.page_state_error_desc) : message, false);
                    scheduleNextWeatherRefresh();
                });
            }
        });
    }

    private boolean hasLocationPermission() {
        return permissionManager.isGranted(requireContext(), AppPermissionManager.GROUP_LOCATION);
    }

    @SuppressLint("MissingPermission")
    @Nullable
    private Location getBestLocation() {
        try {
            if (amapLocationProvider != null) {
                Location amapBest = amapLocationProvider.getBestKnownLocation();
                if (amapBest != null && hasValidCoordinates(amapBest)) {
                    appendLocationAudit("last_known_amap time=" + amapBest.getTime()
                            + " accuracy=" + readLocationAccuracy(amapBest));
                    return amapBest;
                }
            }
            appendLocationAudit("last_known_no_amap_cache");
            return null;
        } catch (Throwable throwable) {
            appendLocationCrash("get_best_location_failed " + throwable.getClass().getSimpleName());
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocation(boolean forceRefresh) {
        if (!isAdded()) {
            return;
        }
        if (amapLocationProvider == null) {
            appendLocationAudit("request_current amap_null");
            loadWeatherUsingBestKnownOrFallback("amap_null", forceRefresh);
            return;
        }
        appendLocationAudit("request_current amap requesting");
        amapLocationProvider.requestOnceLocation(new AmapLocationProvider.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull Location location, @NonNull String source) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        handleRealtimeLocationSuccess("amap", location, forceRefresh);
                    }
                });
            }

            @Override
            public void onLocationError(@NonNull String reason) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        appendLocationAudit("request_current amap_error reason=" + reason);
                        loadWeatherUsingBestKnownOrFallback("amap_" + reason, forceRefresh);
                    }
                });
            }
        }, LOCATION_TIMEOUT_MS);
    }

    @NonNull
    private Location createFallbackLocation() {
        Location location = new Location("demo-fallback");
        location.setLatitude(FALLBACK_LATITUDE);
        location.setLongitude(FALLBACK_LONGITUDE);
        location.setAccuracy(50f);
        location.setTime(System.currentTimeMillis());
        return location;
    }

    private void handleRealtimeLocationSuccess(@NonNull String provider, @NonNull Location location, boolean forceRefresh) {
        if (!hasValidCoordinates(location)) {
            appendLocationCrash("current_location_invalid_coordinates provider=" + provider);
            loadWeatherUsingBestKnownOrFallback("invalid_coordinates", forceRefresh);
            return;
        }
        appendLocationAudit("current_location_success provider=" + provider
                + " accuracy=" + readLocationAccuracy(location));
        loadWeatherForLocation(location, "current_" + provider, forceRefresh);
    }

    private void loadWeatherUsingBestKnownOrFallback(@NonNull String reason, boolean forceRefresh) {
        Location bestKnown = getBestLocation();
        if (bestKnown != null) {
            appendLocationAudit("best_known_fallback reason=" + reason
                    + " ageMs=" + calculateLocationAgeMs(bestKnown)
                    + " accuracy=" + readLocationAccuracy(bestKnown));
            loadWeatherForLocation(bestKnown, "best_known_" + reason, forceRefresh);
            return;
        }
        loadWeatherForLocation(createFallbackLocation(), "demo_" + reason + "_fallback", forceRefresh);
    }

    private boolean shouldRequestFreshLocation(@NonNull Location location) {
        return calculateLocationAgeMs(location) > MAX_LAST_KNOWN_AGE_MS
                || !isLocationAccurateEnough(location);
    }

    private boolean isLocationAccurateEnough(@NonNull Location location) {
        return !location.hasAccuracy() || location.getAccuracy() <= MAX_ACCEPTABLE_LOCATION_ACCURACY_METERS;
    }

    private long calculateLocationAgeMs(@NonNull Location location) {
        long time = location.getTime();
        if (time <= 0L) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, System.currentTimeMillis() - time);
    }

    private float readLocationAccuracy(@NonNull Location location) {
        return location.hasAccuracy() ? location.getAccuracy() : -1f;
    }

    private boolean hasValidCoordinates(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && !Double.isInfinite(latitude)
                && !Double.isInfinite(longitude)
                && latitude >= -90d
                && latitude <= 90d
                && longitude >= -180d
                && longitude <= 180d;
    }

    private void bindWeather(@NonNull PlatformModels.RealtimeWeatherView weather) {
        if (tvWeatherLocation == null) {
            return;
        }
        currentWeather = weather;
        lastWeatherRefreshTime = System.currentTimeMillis();
        sLastKnownWeather = weather;
        sLastWeatherFetchedAt = lastWeatherRefreshTime;
        String locationDisplay = safe(weather.locationName);
        if (locationDisplay.isEmpty()) {
            locationDisplay = String.format(Locale.getDefault(), "%.5f, %.5f", lastWeatherRequestLat, lastWeatherRequestLon);
        }
        tvWeatherLocation.setText(locationDisplay);
        if (ivWeatherIcon != null) {
            ivWeatherIcon.setImageResource(WeatherVisuals.resolveWeatherIcon(weather));
        }
        if (tvWeatherCondition != null) {
            tvWeatherCondition.setText(WeatherVisuals.formatWeatherHeadline(weather));
        }
        tvWeatherResult.setText(safe(weather.suitability == null ? "" : weather.suitability.result));
        tvWeatherResult.setTextColor(resolveSuitabilityColor(weather.suitability));
        tvWeatherResult.setBackgroundTintList(ColorStateList.valueOf(resolveSuitabilityBadgeColor(weather.suitability)));
        tvWeatherSummary.setText(safe(weather.suitability == null ? "" : weather.suitability.summary));
        tvWeatherOverview.setText(buildWeatherOverview(weather));
        tvWeatherTemperature.setText(String.format(Locale.getDefault(), "%.1f°C", weather.temperature));
        tvWeatherWind.setText(String.format(Locale.getDefault(), "%s | %.1fm/s", safe(weather.windDirection), weather.windSpeed));
        tvWeatherPrecipitation.setText(String.format(Locale.getDefault(), "%d%%", weather.precipitationProbability));
        tvWeatherThunderstorm.setText(WeatherVisuals.formatRiskLevel(weather));
        tvWeatherThunderstorm.setTextColor(WeatherVisuals.resolveRiskColor(requireContext(), weather.thunderstormRiskLevel));
        if (tvWeatherThunderstormLabel != null) {
            tvWeatherThunderstormLabel.setText(safe(weather.thunderstormRiskLabel));
            tvWeatherThunderstormLabel.setTextColor(WeatherVisuals.resolveRiskColor(requireContext(), weather.thunderstormRiskLevel));
        }
        tvWeatherMeta.setText("更新时间 " + safe(weather.fetchedAt) + " | " + safe(weather.refreshInterval));
        if (isOfflineWeather(weather)) {
            tvWeatherMeta.setText(tvWeatherMeta.getText() + " ⚠ 模拟数据");
            tvWeatherMeta.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_warning));
            if (tvWeatherError != null) {
                tvWeatherError.setVisibility(View.VISIBLE);
                tvWeatherError.setText("网络不可用，当前显示本地模拟推演数据，与实际天气可能存在偏差。");
            }
        } else {
            tvWeatherMeta.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_muted));
            tvWeatherError.setVisibility(View.GONE);
        }
        if (btnWeatherDetail != null) {
            btnWeatherDetail.setEnabled(true);
        }
    }

    private void renderWeatherLoading(@NonNull String locationText, @NonNull String detailText) {
        if (tvWeatherLocation == null) {
            return;
        }
        tvWeatherLocation.setText(locationText);
        if (ivWeatherIcon != null) {
            ivWeatherIcon.setImageResource(R.drawable.ic_weather_cloudy);
        }
        if (tvWeatherCondition != null) {
            tvWeatherCondition.setText(getString(R.string.home_weather_loading_short));
        }
        tvWeatherResult.setText(getString(R.string.home_weather_loading_short));
        tvWeatherResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_info));
        tvWeatherResult.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ui_info_surface)));
        tvWeatherSummary.setText(detailText);
        tvWeatherOverview.setText(getString(R.string.home_weather_overview_placeholder));
        currentWeather = null;
        tvWeatherTemperature.setText(getString(R.string.placeholder_dash));
        tvWeatherWind.setText(getString(R.string.placeholder_dash));
        tvWeatherPrecipitation.setText(getString(R.string.placeholder_dash));
        tvWeatherThunderstorm.setText(getString(R.string.placeholder_dash));
        tvWeatherThunderstorm.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_primary));
        if (tvWeatherThunderstormLabel != null) {
            tvWeatherThunderstormLabel.setText(getString(R.string.home_weather_risk_hint_placeholder));
            tvWeatherThunderstormLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_muted));
        }
        tvWeatherMeta.setText(getString(R.string.home_weather_service_note));
        tvWeatherError.setVisibility(View.GONE);
        if (btnWeatherDetail != null) {
            btnWeatherDetail.setEnabled(false);
        }
    }

    private void renderWeatherError(@NonNull String message, boolean permissionProblem) {
        if (tvWeatherLocation == null) {
            return;
        }
        if (ivWeatherIcon != null) {
            ivWeatherIcon.setImageResource(R.drawable.ic_weather_thunderstorm);
        }
        if (tvWeatherCondition != null) {
            tvWeatherCondition.setText(getString(R.string.home_weather_condition_label));
        }
        tvWeatherResult.setText("不适宜飞行");
        tvWeatherResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_error));
        tvWeatherResult.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_error_container)));
        tvWeatherSummary.setText(permissionProblem ? "缺少定位能力，暂时无法形成飞行评估。" : "天气服务异常，当前建议谨慎飞行并人工复核天气。");
        tvWeatherLocation.setText(getString(R.string.home_weather_location_placeholder));
        tvWeatherOverview.setText("当前先展示关键结论，可进入详情查看完整说明。");
        tvWeatherTemperature.setText(getString(R.string.placeholder_dash));
        tvWeatherWind.setText(getString(R.string.placeholder_dash));
        tvWeatherPrecipitation.setText(getString(R.string.placeholder_dash));
        tvWeatherThunderstorm.setText(getString(R.string.placeholder_dash));
        tvWeatherThunderstorm.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_error));
        if (tvWeatherThunderstormLabel != null) {
            tvWeatherThunderstormLabel.setText(getString(R.string.home_weather_risk_hint_placeholder));
            tvWeatherThunderstormLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.ui_text_muted));
        }
        currentWeather = null;
        tvWeatherMeta.setText(getString(R.string.home_weather_service_note));
        tvWeatherError.setVisibility(View.VISIBLE);
        tvWeatherError.setText(message);
        if (btnWeatherDetail != null) {
            btnWeatherDetail.setEnabled(false);
        }
    }

    private int resolveSuitabilityColor(@Nullable PlatformModels.FlightSuitabilityView suitability) {
        if (suitability == null || suitability.result == null) {
            return ContextCompat.getColor(requireContext(), R.color.ui_warning);
        }
        String result = suitability.result;
        if (result.contains("适宜")) {
            return ContextCompat.getColor(requireContext(), R.color.ui_success);
        }
        if (result.contains("谨慎") || result.contains("预警")) {
            return ContextCompat.getColor(requireContext(), R.color.ui_warning);
        }
        return ContextCompat.getColor(requireContext(), R.color.ui_error);
    }

    private int resolveSuitabilityBadgeColor(@Nullable PlatformModels.FlightSuitabilityView suitability) {
        if (suitability == null || suitability.result == null) {
            return ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_warning_container);
        }
        String result = suitability.result;
        if (result.contains("适宜")) {
            return ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_success_container);
        }
        if (result.contains("谨慎") || result.contains("预警")) {
            return ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_warning_container);
        }
        return ContextCompat.getColor(requireContext(), com.example.low_altitudereststop.core.ui.R.color.ui_error_container);
    }

    private String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private String buildWeatherOverview(@NonNull PlatformModels.RealtimeWeatherView weather) {
        List<String> items = new ArrayList<>();
        if (weather.precipitationProbability >= 50) {
            items.add("降水偏高");
        }
        if (weather.windSpeed >= 8f) {
            items.add("风速偏大");
        }
        if (weather.visibility <= 3f) {
            items.add("能见度受限");
        }
        if (!safe(weather.thunderstormRisk).isEmpty()) {
            items.add(WeatherVisuals.formatRiskSummary(weather));
        }
        if (items.isEmpty()) {
            items.add(String.format(Locale.getDefault(), "湿度 %d%%", weather.humidity));
        }
        return TextUtils.join(" | ", items);
    }

    private void openWeatherDetail() {
        if (!isAdded() || currentWeather == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), WeatherDetailActivity.class);
        intent.putExtra(WeatherDetailActivity.EXTRA_LOCATION, safe(currentWeather.locationName));
        intent.putExtra(WeatherDetailActivity.EXTRA_WEATHER, safe(currentWeather.weather));
        intent.putExtra(WeatherDetailActivity.EXTRA_WEATHER_ICON_TYPE, safe(currentWeather.weatherIconType));
        intent.putExtra(WeatherDetailActivity.EXTRA_RESULT, safe(currentWeather.suitability == null ? "" : currentWeather.suitability.result));
        intent.putExtra(WeatherDetailActivity.EXTRA_SUMMARY, safe(currentWeather.suitability == null ? "" : currentWeather.suitability.summary));
        intent.putExtra(WeatherDetailActivity.EXTRA_TEMPERATURE, String.format(Locale.getDefault(), "%.1f°C", currentWeather.temperature));
        intent.putExtra(WeatherDetailActivity.EXTRA_HUMIDITY, String.format(Locale.getDefault(), "%d%%", currentWeather.humidity));
        intent.putExtra(WeatherDetailActivity.EXTRA_WIND, String.format(Locale.getDefault(), "%s %.1fm/s", safe(currentWeather.windDirection), currentWeather.windSpeed));
        intent.putExtra(WeatherDetailActivity.EXTRA_VISIBILITY, String.format(Locale.getDefault(), "%.1fkm", currentWeather.visibility));
        intent.putExtra(WeatherDetailActivity.EXTRA_PRECIPITATION, String.format(Locale.getDefault(), "%d%% / %.1fmm/h", currentWeather.precipitationProbability, currentWeather.precipitationIntensity));
        intent.putExtra(WeatherDetailActivity.EXTRA_THUNDERSTORM, WeatherVisuals.formatRiskSummary(currentWeather));
        intent.putExtra(WeatherDetailActivity.EXTRA_THUNDERSTORM_LEVEL, String.valueOf(currentWeather.thunderstormRiskLevel));
        intent.putExtra(WeatherDetailActivity.EXTRA_THUNDERSTORM_LABEL, safe(currentWeather.thunderstormRiskLabel));
        intent.putExtra(WeatherDetailActivity.EXTRA_THUNDERSTORM_HINT, safe(currentWeather.thunderstormRiskHint));
        intent.putExtra(WeatherDetailActivity.EXTRA_META, "更新于 " + safe(currentWeather.fetchedAt) + " | " + safe(currentWeather.refreshInterval));
        intent.putExtra(WeatherDetailActivity.EXTRA_SOURCE_NOTE, safe(currentWeather.sourceNote));
        startActivity(intent);
    }

    private String joinLines(@Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "暂无说明";
        }
        return TextUtils.join("\n- ", lines).replaceFirst("^", "- ");
    }

    private void scheduleNextWeatherRefresh() {
        lastWeatherRefreshTime = System.currentTimeMillis();
        mainHandler.removeCallbacks(weatherRefreshRunnable);
        mainHandler.postDelayed(weatherRefreshRunnable, WEATHER_REFRESH_INTERVAL_MS);
    }

    private void maybePromptLocationPermission(boolean requestPermissionIfNeeded) {
        if (!isAdded()) {
            return;
        }
        if (requestPermissionIfNeeded) {
            permissionManager.requestPermissions(this, permissionManager.buildLocationPayload(), new AppPermissionManager.PermissionResultCallback() {
                @Override
                public void onGranted() {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.home_weather_permission_granted, Toast.LENGTH_SHORT).show();
                    dismissLocationPermissionPrompt();
                    refreshWeather(false);
                }

                @Override
                public void onDenied(@NonNull AppPermissionManager.PermissionState state, @NonNull AppPermissionManager.PermissionRequestPayload payload) {
                    if (!isAdded()) {
                        return;
                    }
                    renderWeatherError(resolveLocationErrorMessage(), true);
                }
            });
            return;
        }
        AppPermissionManager.PermissionState state = permissionManager.getState(requireActivity(), AppPermissionManager.GROUP_LOCATION);
        showLocationPermissionSnackbar(
                state.permanentlyDenied ? R.string.home_weather_permission_permanently_denied : R.string.home_weather_permission_denied,
                state.permanentlyDenied ? R.string.home_weather_permission_settings : R.string.ai_ball_enable_action,
                v -> {
                    if (state.permanentlyDenied) {
                        startActivity(permissionManager.createAppSettingsIntent(requireContext()));
                    } else {
                        maybePromptLocationPermission(true);
                    }
                }
        );
    }

    private void maybePromptLocationServiceSettings() {
        showLocationPermissionSnackbar(
                R.string.home_weather_location_unavailable,
                R.string.permission_action_settings,
                v -> startActivity(permissionManager.createLocationSettingsIntent())
        );
    }

    private void showLocationPermissionSnackbar(int messageRes, int actionRes, @NonNull View.OnClickListener actionListener) {
        View view = getView();
        if (view == null) {
            return;
        }
        dismissLocationPermissionPrompt();
        locationPermissionSnackbar = Snackbar.make(view, messageRes, Snackbar.LENGTH_LONG);
        locationPermissionSnackbar.setAction(actionRes, actionListener);
        locationPermissionSnackbar.show();
    }

    private void showLocationPermissionSnackbar(int messageRes, @NonNull CharSequence actionLabel, @NonNull View.OnClickListener actionListener) {
        View view = getView();
        if (view == null) {
            return;
        }
        dismissLocationPermissionPrompt();
        locationPermissionSnackbar = Snackbar.make(view, messageRes, Snackbar.LENGTH_LONG);
        locationPermissionSnackbar.setAction(actionLabel, actionListener);
        locationPermissionSnackbar.show();
    }

    private void dismissLocationPermissionPrompt() {
        if (locationPermissionSnackbar != null) {
            locationPermissionSnackbar.dismiss();
            locationPermissionSnackbar = null;
        }
    }

    @NonNull
    private String resolveLocationErrorMessage() {
        if (!isAdded()) {
            return "";
        }
        AppPermissionManager.PermissionState state = permissionManager.getState(requireActivity(), AppPermissionManager.GROUP_LOCATION);
        if (state.permanentlyDenied) {
            return getString(R.string.home_weather_permission_permanently_denied);
        }
        return getString(R.string.home_weather_permission_denied);
    }

    private void appendLocationAudit(@NonNull String message) {
        Log.d(TAG, message);
        if (operationLogStore != null) {
            operationLogStore.appendAudit("LOCATION", message);
        }
    }

    private void appendLocationCrash(@NonNull String message) {
        Log.w(TAG, message);
        if (operationLogStore != null) {
            operationLogStore.appendCrash("LOCATION", message);
        }
    }

    private void maybeAnimateRoleGuide(@NonNull View heroCard, @NonNull UsageAnalyticsStore analyticsStore, @NonNull UserRole role) {
        if (!analyticsStore.shouldShowGuide(role)) {
            return;
        }
        heroCard.setAlpha(0.2f);
        heroCard.setTranslationY(24f);
        heroCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320L)
                .start();
        analyticsStore.markGuideShown(role);
    }

    private void applyQuickActionSizing(
            @NonNull View quickTaskButton,
            @NonNull View quickComplianceButton,
            @NonNull View quickProfileButton,
            @NonNull ImageView quickTaskIcon
    ) {
        applyBaseQuickActionSizing(quickTaskButton);
        applyBaseQuickActionSizing(quickComplianceButton);
        applyBaseQuickActionSizing(quickProfileButton);
        updateQuickActionIconSize(quickTaskIcon, com.example.low_altitudereststop.core.ui.R.dimen.ui_quick_action_icon_size);
    }

    private void applyBaseQuickActionSizing(@NonNull View button) {
        ViewGroup.LayoutParams layoutParams = button.getLayoutParams();
        if (!(layoutParams instanceof android.widget.LinearLayout.LayoutParams)) {
            return;
        }
        button.setMinimumHeight(getDimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_quick_action_min_height));
        button.setPadding(
                getDimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_quick_action_padding_horizontal),
                getDimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_quick_action_padding_vertical),
                getDimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_quick_action_padding_horizontal),
                getDimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_quick_action_padding_vertical)
        );
        android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) layoutParams;
        params.weight = 1f;
        button.setLayoutParams(params);
    }

    private void updateQuickActionIconSize(@NonNull ImageView iconView, int dimenRes) {
        ViewGroup.LayoutParams params = iconView.getLayoutParams();
        int sizePx = getDimenPx(dimenRes);
        params.width = sizePx;
        params.height = sizePx;
        iconView.setLayoutParams(params);
    }

    private void bindQuickActionState(
            @NonNull View button,
            @NonNull ImageView iconView,
            @NonNull TextView labelView,
            boolean enabled,
            boolean loading,
            boolean primary
    ) {
        boolean interactive = enabled && !loading;
        button.setEnabled(enabled);
        button.setClickable(interactive);
        button.setFocusable(interactive);
        button.setAlpha(enabled ? (loading ? 0.92f : 1f) : 0.72f);
        button.setBackgroundResource(resolveQuickActionBackground(primary, loading));
        int foregroundColor = ContextCompat.getColor(requireContext(), resolveQuickActionForeground(primary, enabled));
        labelView.setTextColor(foregroundColor);
        iconView.setImageTintList(ColorStateList.valueOf(foregroundColor));
    }

    private int resolveQuickActionBackground(boolean primary, boolean loading) {
        if (loading) {
            return primary ? R.drawable.bg_quick_action_primary_loading : R.drawable.bg_quick_action_loading;
        }
        return primary ? R.drawable.bg_quick_action_primary : R.drawable.bg_quick_action;
    }

    private int resolveQuickActionForeground(boolean primary, boolean enabled) {
        if (!enabled) {
            return com.example.low_altitudereststop.core.ui.R.color.ui_quick_action_disabled_foreground;
        }
        return primary
                ? com.example.low_altitudereststop.core.ui.R.color.ui_quick_action_primary_foreground
                : com.example.low_altitudereststop.core.ui.R.color.ui_quick_action_secondary_foreground;
    }

    private int getDimenPx(int dimenRes) {
        return Math.round(requireContext().getResources().getDimension(dimenRes));
    }

    private void renderSafeFallbackHome(@NonNull View view) {
        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        TextView tvQuickTask = view.findViewById(R.id.tv_quick_task);
        TextView tvQuickCompliance = view.findViewById(R.id.tv_quick_compliance);
        TextView tvQuickProfile = view.findViewById(R.id.tv_quick_profile);
        TextView tvTaskLabel = view.findViewById(R.id.tv_task_label);
        TextView tvAlertLabel = view.findViewById(R.id.tv_alert_label);
        TextView tvCourseLabel = view.findViewById(R.id.tv_course_label);
        TextView tvQueueLabel = view.findViewById(R.id.tv_queue_label);
        TextView tvTaskCount = view.findViewById(R.id.tv_task_count);
        TextView tvAlertCount = view.findViewById(R.id.tv_alert_count);
        TextView tvCourseCount = view.findViewById(R.id.tv_course_count);
        TextView tvQueueCount = view.findViewById(R.id.tv_queue_count);
        tvWeatherLocation = view.findViewById(R.id.tv_weather_location);
        tvWeatherResult = view.findViewById(R.id.tv_weather_result);
        tvWeatherSummary = view.findViewById(R.id.tv_weather_summary);
        tvWeatherOverview = view.findViewById(R.id.tv_weather_overview);
        tvWeatherTemperature = view.findViewById(R.id.tv_weather_temperature);
        tvWeatherWind = view.findViewById(R.id.tv_weather_wind);
        tvWeatherPrecipitation = view.findViewById(R.id.tv_weather_precipitation);
        tvWeatherThunderstorm = view.findViewById(R.id.tv_weather_thunderstorm);
        tvWeatherMeta = view.findViewById(R.id.tv_weather_meta);
        tvWeatherError = view.findViewById(R.id.tv_weather_error);
        btnWeatherDetail = view.findViewById(R.id.btn_weather_detail);
        chartOverview = view.findViewById(R.id.chart_overview);
        if (tvWelcome != null) {
            tvWelcome.setText("你好，欢迎使用低空驿站");
        }
        if (tvQuickTask != null) {
            tvQuickTask.setText("进入任务");
        }
        if (tvQuickCompliance != null) {
            tvQuickCompliance.setText("进入合规");
        }
        if (tvQuickProfile != null) {
            tvQuickProfile.setText("我的订单");
        }
        if (tvTaskLabel != null) {
            tvTaskLabel.setText("项目数");
        }
        if (tvAlertLabel != null) {
            tvAlertLabel.setText("风险");
        }
        if (tvCourseLabel != null) {
            tvCourseLabel.setText("课程");
        }
        if (tvQueueLabel != null) {
            tvQueueLabel.setText("待同步");
        }
        if (tvTaskCount != null) {
            tvTaskCount.setText(String.valueOf(AppScenarioMapper.buildFallbackTasks().size()));
        }
        if (tvAlertCount != null) {
            tvAlertCount.setText(String.valueOf(AppScenarioMapper.buildFallbackAlerts(UserRole.UNKNOWN).size()));
        }
        if (tvCourseCount != null) {
            tvCourseCount.setText("0");
        }
        if (tvQueueCount != null) {
            tvQueueCount.setText("0");
        }
        if (chartOverview != null) {
            chartOverview.clear();
            chartOverview.setNoDataText("首页图表已进入安全模式");
            chartOverview.invalidate();
        }
        View quickTaskButton = view.findViewById(R.id.btn_go_task);
        View quickComplianceButton = view.findViewById(R.id.btn_go_compliance);
        View quickProfileButton = view.findViewById(R.id.btn_go_profile);
        if (quickTaskButton != null) {
            quickTaskButton.setOnClickListener(v -> navigateToTopLevel(view, R.id.taskFragment));
        }
        if (quickComplianceButton != null) {
            quickComplianceButton.setOnClickListener(v -> navigateToTopLevel(view, R.id.complianceFragment));
        }
        if (quickProfileButton != null) {
            quickProfileButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), OrderListActivity.class)));
        }
        renderSafeWeatherFallback();
    }

    private void renderSafeWeatherFallback() {
        if (tvWeatherLocation != null) {
            tvWeatherLocation.setText(getString(R.string.home_weather_location_placeholder));
        }
        if (tvWeatherResult != null) {
            tvWeatherResult.setText("首页已进入安全模式");
        }
        if (tvWeatherSummary != null) {
            tvWeatherSummary.setText("检测到启动异常，天气模块已关闭");
        }
        if (tvWeatherOverview != null) {
            tvWeatherOverview.setText("--");
        }
        if (tvWeatherTemperature != null) {
            tvWeatherTemperature.setText("--");
        }
        if (tvWeatherWind != null) {
            tvWeatherWind.setText("--");
        }
        if (tvWeatherPrecipitation != null) {
            tvWeatherPrecipitation.setText("--");
        }
        if (tvWeatherThunderstorm != null) {
            tvWeatherThunderstorm.setText("--");
        }
        if (tvWeatherMeta != null) {
            tvWeatherMeta.setText("请导出崩溃日志继续排查");
        }
        if (tvWeatherError != null) {
            tvWeatherError.setVisibility(View.VISIBLE);
            tvWeatherError.setText("首页初始化失败，已自动降级");
        }
        if (btnWeatherDetail != null) {
            btnWeatherDetail.setEnabled(false);
        }
    }

    private String primaryActionLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "发布任务";
        }
        return "执行任务";
    }

    private String secondaryActionLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "飞行管理";
        }
        return "飞行申请";
    }

    private String tertiaryActionLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "订单回款";
        }
        return "我的订单";
    }

    private String metricTaskLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "进行中";
        }
        return "待执行";
    }

    private String metricAlertLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "预警";
        }
        return "风险";
    }

    private String metricCourseLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "团队学习";
        }
        return "课程";
    }

    private String metricQueueLabel(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "待回款";
        }
        return "待同步";
    }

    private boolean isOfflineWeather(@Nullable PlatformModels.RealtimeWeatherView weather) {
        if (weather == null) {
            return false;
        }
        String serviceName = weather.serviceName == null ? "" : weather.serviceName;
        String sourceNote = weather.sourceNote == null ? "" : weather.sourceNote;
        String adcode = weather.adcode == null ? "" : weather.adcode;
        return serviceName.contains("离线模式")
                || sourceNote.contains("本地模拟")
                || sourceNote.contains("模拟推演")
                || adcode.startsWith("CQ-");
    }

    private void cancelPendingWeatherTask() {
        if (pendingWeatherTask != null && !pendingWeatherTask.isDone()) {
            pendingWeatherTask.cancel(true);
            pendingWeatherTask = null;
        }
    }

}

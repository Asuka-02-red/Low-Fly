package com.example.low_altitudereststop.feature.risk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.feature.risk.databinding.ActivityAlertListBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import com.example.low_altitudereststop.core.session.SessionStore;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertListActivity extends NavigableEdgeToEdgeActivity {

    private static final String PREF_RISK_ANALYTICS = "risk_analytics";
    private static final String KEY_RISK_ALERT_DETAIL = "feature_risk_alert_detail";
    private ActivityAlertListBinding binding;
    private AlertAdapter adapter;
    private String roleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roleName = new SessionStore(this).getCachedUser().role;
        adapter = new AlertAdapter(this::openDetail);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.swipe.setOnRefreshListener(this::loadAlerts);
        binding.btnCloseBanner.setOnClickListener(v -> hideOfflineBanner());
        binding.swipe.setRefreshing(true);
        loadAlerts();
    }

    private void loadAlerts() {
        FileCache cache = new FileCache(this);
        ApiClient.getAuthedService(this).listAlerts().enqueue(new Callback<ApiEnvelope<List<PlatformModels.AlertView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.AlertView>>> call, Response<ApiEnvelope<List<PlatformModels.AlertView>>> response) {
                binding.swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    List<PlatformModels.AlertView> cached = readCache(cache, "alerts.json");
                    if (cached != null) {
                        adapter.submit(cached);
                        showOfflineBanner();
                        return;
                    }
                    List<PlatformModels.AlertView> fallback = buildMockAlerts();
                    adapter.submit(fallback);
                    cache.write("alerts.json", new Gson().toJson(fallback));
                    showOfflineBanner();
                    return;
                }
                adapter.submit(response.body().data);
                cache.write("alerts.json", new Gson().toJson(response.body().data));
                hideOfflineBanner();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.AlertView>>> call, Throwable t) {
                binding.swipe.setRefreshing(false);
                List<PlatformModels.AlertView> cached = readCache(cache, "alerts.json");
                if (cached != null) {
                    adapter.submit(cached);
                    showOfflineBanner();
                    return;
                }
                List<PlatformModels.AlertView> fallback = buildMockAlerts();
                adapter.submit(fallback);
                cache.write("alerts.json", new Gson().toJson(fallback));
                showOfflineBanner();
            }
        });
    }

    private List<PlatformModels.AlertView> readCache(FileCache cache, String name) {
        try {
            String json = cache.read(name);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            Type type = new TypeToken<List<PlatformModels.AlertView>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<PlatformModels.AlertView> buildMockAlerts() {
        List<AlertScenarioRepository.AlertScenario> scenarios = AlertScenarioRepository.buildAlerts(roleName);
        List<PlatformModels.AlertView> alerts = new ArrayList<>();
        for (AlertScenarioRepository.AlertScenario scenario : scenarios) {
            alerts.add(scenario.alert);
        }
        return alerts;
    }

    private void openDetail(PlatformModels.AlertView alert) {
        if (alert == null || alert.id == null) {
            return;
        }
        trackAlertDetailClick();
        Intent intent = new Intent(this, AlertDetailActivity.class);
        intent.putExtra(AlertDetailActivity.EXTRA_ALERT_ID, alert.id);
        intent.putExtra(AlertDetailActivity.EXTRA_ROLE_NAME, roleName);
        startActivity(intent);
    }

    private void showOfflineBanner() {
        binding.layoutOfflineBanner.setVisibility(View.VISIBLE);
    }

    private void hideOfflineBanner() {
        binding.layoutOfflineBanner.setVisibility(View.GONE);
    }

    private void trackAlertDetailClick() {
        int current = getSharedPreferences(PREF_RISK_ANALYTICS, Context.MODE_PRIVATE)
                .getInt(KEY_RISK_ALERT_DETAIL, 0);
        getSharedPreferences(PREF_RISK_ANALYTICS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_RISK_ALERT_DETAIL, current + 1)
                .apply();
    }
}

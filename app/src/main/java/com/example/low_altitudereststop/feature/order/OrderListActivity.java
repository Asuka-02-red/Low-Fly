package com.example.low_altitudereststop.feature.order;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityOrderListBinding;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderListActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_FORCE_EMPTY_STATE = "extra_force_empty_state";

    private ActivityOrderListBinding binding;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new OrderAdapter(order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id == null ? -1L : order.id);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_NO, order.orderNo);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_STATUS, order.status);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_AMOUNT, order.amount == null ? "" : order.amount.toPlainString());
            startActivity(intent);
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.swipe.setOnRefreshListener(this::loadOrders);

        binding.swipe.setRefreshing(true);
        updateEmptyState(false);
        loadOrders();
    }

    private void loadOrders() {
        if (getIntent().getBooleanExtra(EXTRA_FORCE_EMPTY_STATE, false)) {
            binding.swipe.setRefreshing(false);
            adapter.submit(Collections.emptyList());
            updateEmptyState(true);
            return;
        }
        ApiClient.getAuthedService(this).listOrders().enqueue(new Callback<ApiEnvelope<List<PlatformModels.OrderView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.OrderView>>> call, Response<ApiEnvelope<List<PlatformModels.OrderView>>> response) {
                binding.swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    List<PlatformModels.OrderView> fallback = AppScenarioMapper.buildFallbackOrders();
                    adapter.submit(fallback);
                    updateEmptyState(fallback.isEmpty());
                    toast("已展示可用订单");
                    return;
                }
                List<PlatformModels.OrderView> orders = response.body().data;
                adapter.submit(orders);
                updateEmptyState(orders == null || orders.isEmpty());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.OrderView>>> call, Throwable t) {
                binding.swipe.setRefreshing(false);
                List<PlatformModels.OrderView> fallback = AppScenarioMapper.buildFallbackOrders();
                adapter.submit(fallback);
                updateEmptyState(fallback.isEmpty());
                toast("已展示可用订单");
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.layoutEmptyState.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.recycler.setVisibility(isEmpty ? android.view.View.GONE : android.view.View.VISIBLE);
    }
}


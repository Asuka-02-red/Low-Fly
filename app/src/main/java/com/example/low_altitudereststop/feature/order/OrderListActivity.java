package com.example.low_altitudereststop.feature.order;

import android.os.Bundle;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityOrderListBinding;
import com.example.low_altitudereststop.feature.order.local.OrderDao;
import com.example.low_altitudereststop.feature.order.local.OrderLocalDatabase;
import com.example.low_altitudereststop.feature.order.local.OrderEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderListActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_FORCE_EMPTY_STATE = "extra_force_empty_state";

    private ActivityOrderListBinding binding;
    private OrderAdapter adapter;
    private OrderDao orderDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderDao = OrderLocalDatabase.get(this).orderDao();
        OrderLocalDatabase.ensureSeeded(this);

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
        binding.swipe.setOnRefreshListener(this::refreshFromNetwork);

        loadFromLocal();
        refreshFromNetwork();
    }

    private void loadFromLocal() {
        List<OrderEntity> entities = orderDao.listAll();
        List<PlatformModels.OrderView> orders = new ArrayList<>();
        for (OrderEntity entity : entities) {
            orders.add(OrderLocalDatabase.toOrderView(entity));
        }
        adapter.submit(orders);
        updateEmptyState(orders.isEmpty());
    }

    private void refreshFromNetwork() {
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
                    return;
                }
                List<PlatformModels.OrderView> orders = response.body().data;
                syncToLocal(orders);
                adapter.submit(orders);
                updateEmptyState(orders == null || orders.isEmpty());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.OrderView>>> call, Throwable t) {
                binding.swipe.setRefreshing(false);
            }
        });
    }

    private void syncToLocal(List<PlatformModels.OrderView> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<OrderEntity> entities = new ArrayList<>();
        for (PlatformModels.OrderView order : orders) {
            OrderEntity existing = orderDao.findById(order.id == null ? 0L : order.id);
            OrderEntity entity = existing != null ? existing : new OrderEntity();
            entity.id = order.id == null ? 0L : order.id;
            entity.orderNo = order.orderNo == null ? "" : order.orderNo;
            entity.status = order.status == null ? "" : order.status;
            entity.amount = order.amount == null ? "" : order.amount.toPlainString();
            entity.taskId = order.taskId == null ? 0L : order.taskId;
            entity.updateTimeMillis = now;
            entities.add(entity);
        }
        orderDao.upsertAll(entities);
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.layoutEmptyState.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.recycler.setVisibility(isEmpty ? android.view.View.GONE : android.view.View.VISIBLE);
    }
}

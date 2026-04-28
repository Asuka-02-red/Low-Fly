package com.example.low_altitudereststop.feature.profile;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.storage.OperationOutboxEntity;
import com.example.low_altitudereststop.core.storage.OperationOutboxStore;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.databinding.ActivitySyncQueueBinding;
import java.util.List;

public class SyncQueueActivity extends NavigableEdgeToEdgeActivity {

    private ActivitySyncQueueBinding binding;
    private OperationOutboxStore outboxStore;
    private SyncQueueAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySyncQueueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        outboxStore = new OperationOutboxStore(this);
        adapter = new SyncQueueAdapter(item -> {
            outboxStore.retryNow(item.id);
            OutboxSyncManager.scheduleNow(this);
            render();
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.btnRefresh.setOnClickListener(v -> render());
        binding.btnSyncNow.setOnClickListener(v -> {
            List<OperationOutboxEntity> items = outboxStore.listAll();
            for (OperationOutboxEntity item : items) {
                if (!"SUCCESS".equals(item.status)) {
                    outboxStore.retryNow(item.id);
                }
            }
            OutboxSyncManager.scheduleNow(this);
            render();
        });
        binding.btnClearFinished.setOnClickListener(v -> {
            outboxStore.clearFinished();
            render();
        });
        render();
    }

    private void render() {
        List<OperationOutboxEntity> items = outboxStore.listAll();
        if (items == null || items.isEmpty()) {
            binding.tvEmpty.setVisibility(android.view.View.VISIBLE);
            binding.recycler.setVisibility(android.view.View.GONE);
            adapter.submit(null);
            return;
        }
        binding.tvEmpty.setVisibility(android.view.View.GONE);
        binding.recycler.setVisibility(android.view.View.VISIBLE);
        adapter.submit(items);
    }
}


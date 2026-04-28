package com.example.low_altitudereststop.feature.profile;

import android.os.Bundle;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.databinding.ActivityOperationLogBinding;

public class OperationLogActivity extends NavigableEdgeToEdgeActivity {

    private ActivityOperationLogBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOperationLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnRefresh.setOnClickListener(v -> render());
        render();
    }

    private void render() {
        String text = new OperationLogStore(this).readAll();
        binding.tvLog.setText(text.isEmpty() ? "暂无日志" : text);
    }
}


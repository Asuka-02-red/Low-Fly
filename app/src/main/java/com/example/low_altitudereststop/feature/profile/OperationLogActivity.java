package com.example.low_altitudereststop.feature.profile;

import android.os.Bundle;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.databinding.ActivityOperationLogBinding;

/**
 * 操作日志查看Activity，展示应用运行时的审计和崩溃日志。
 * <p>
 * 从OperationLogStore读取所有日志记录并展示，支持手动刷新，
 * 便于开发调试和问题排查。
 * </p>
 */
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


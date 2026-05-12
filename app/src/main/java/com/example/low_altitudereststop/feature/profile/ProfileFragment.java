package com.example.low_altitudereststop.feature.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.snackbar.Snackbar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.session.UserSessionManager;
import com.example.low_altitudereststop.core.storage.OperationOutboxEntity;
import com.example.low_altitudereststop.core.storage.OperationOutboxStore;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.auth.AuthActivity;
import com.example.low_altitudereststop.feature.profile.HelpFeedbackActivity;
import com.example.low_altitudereststop.feature.profile.SettingsActivity;
import com.example.low_altitudereststop.feature.order.OrderListActivity;
import com.example.low_altitudereststop.feature.risk.AlertListActivity;
import com.example.low_altitudereststop.feature.training.CourseListActivity;
import com.example.low_altitudereststop.ui.UsageAnalyticsStore;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.List;

/**
 * 个人中心Fragment，展示用户信息和功能入口列表。
 * <p>
 * 根据用户角色显示不同的菜单项（订单、告警、课程、帮助反馈、
 * 消息中心、操作日志、同步队列、设置、退出登录等），
 * 支持边到边布局适配和功能使用统计。
 * </p>
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionStore store = new SessionStore(requireContext());
        UserSessionManager sessionManager = new UserSessionManager(requireContext());
        AuthModels.SessionInfo user = store.getCachedUser();

        android.widget.TextView tvTitle = view.findViewById(R.id.tv_title);
        android.widget.TextView tvPendingSync = view.findViewById(R.id.tv_pending_sync);
        android.widget.TextView tvServiceTitle = view.findViewById(R.id.tv_service_title);
        com.google.android.material.button.MaterialButton btnOrders = view.findViewById(R.id.btn_orders);
        com.google.android.material.button.MaterialButton btnAlerts = view.findViewById(R.id.btn_alerts);
        com.google.android.material.button.MaterialButton btnCourses = view.findViewById(R.id.btn_courses);
        com.google.android.material.button.MaterialButton btnSyncQueue = view.findViewById(R.id.btn_sync_queue);
        com.google.android.material.button.MaterialButton btnSettings = view.findViewById(R.id.btn_settings);
        com.google.android.material.button.MaterialButton btnHelpFeedback = view.findViewById(R.id.btn_help_feedback);
        UsageAnalyticsStore analyticsStore = new UsageAnalyticsStore(requireContext());
        UserRole role = UserRole.from(user.role);

        tvTitle.setText(workbenchTitle(role));
        tvPendingSync.setText(String.valueOf(countPending(new OperationOutboxStore(requireContext()).listAll())));
        tvServiceTitle.setText(serviceTitle(role));
        btnOrders.setText(serviceLabel(role, 0));
        btnAlerts.setText(serviceLabel(role, 1));
        btnCourses.setText(serviceLabel(role, 2));
        btnSyncQueue.setText(serviceLabel(role, 3));

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            sessionManager.logout();
            Snackbar.make(view, "已退出账号，自动登录已关闭", Snackbar.LENGTH_LONG).show();
            startActivity(new Intent(requireContext(), AuthActivity.class));
            requireActivity().finish();
        });

        btnOrders.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "profile_orders");
            startActivity(new Intent(requireContext(), OrderListActivity.class));
        });
        btnAlerts.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "profile_alerts");
            startActivity(new Intent(requireContext(), AlertListActivity.class));
        });
        btnCourses.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "profile_courses");
            startActivity(new Intent(requireContext(), CourseListActivity.class));
        });
        btnSyncQueue.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "profile_sync");
            startActivity(new Intent(requireContext(), SyncQueueActivity.class));
        });
        btnSettings.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "profile_settings");
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });
        btnHelpFeedback.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "profile_help_feedback");
            startActivity(new Intent(requireContext(), HelpFeedbackActivity.class));
        });
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

    private String serviceTitle(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "企业核心服务";
        }
        return "飞手常用服务";
    }

    private String workbenchTitle(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "企业运营台";
        }
        return "飞手工作台";
    }

    private String serviceLabel(UserRole role, int index) {
        String[][] matrix = new String[][]{
                {"我的订单", "风险告警", "我的课程", "同步队列"},
                {"项目订单", "项目告警", "团队课程", "数据同步"}
        };
        int roleIndex = role == UserRole.ENTERPRISE ? 1 : 0;
        return matrix[roleIndex][index];
    }

}


package com.example.low_altitudereststop.feature.compliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.ui.RoleUiConfig;
import com.example.low_altitudereststop.ui.UsageAnalyticsStore;
import com.example.low_altitudereststop.ui.UserRole;

/**
 * 合规管理Fragment，作为合规功能模块的入口页。
 * <p>
 * 根据当前用户角色（飞手/企业）展示不同的合规功能入口描述和按钮标签，
 * 引导用户进入禁飞区列表或飞行申请管理页面。
 * </p>
 */
public class ComplianceFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AuthModels.SessionInfo user = new SessionStore(requireContext()).getCachedUser();
        UserRole role = UserRole.from(user.role);
        RoleUiConfig config = RoleUiConfig.from(role);
        UsageAnalyticsStore analyticsStore = new UsageAnalyticsStore(requireContext());
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        TextView tvApplicationDesc = view.findViewById(R.id.tv_application_desc);
        TextView tvZoneDesc = view.findViewById(R.id.tv_zone_desc);
        TextView tvStatus = view.findViewById(R.id.tv_status);
        com.google.android.material.button.MaterialButton btnZones = view.findViewById(R.id.btn_zones);
        com.google.android.material.button.MaterialButton btnApply = view.findViewById(R.id.btn_apply);
        tvTitle.setText(complianceTitle(role, config));
        tvHint.setText(complianceHint(role));
        tvStatus.setText(role == UserRole.ENTERPRISE
                ? "企业端已启用飞行管理入口，可处理审批、禁飞区维护和离线缓存同步。"
                : "当前为飞手视角，可快速查看禁飞区与提交飞行申请。");
        if (role == UserRole.ENTERPRISE) {
            tvApplicationDesc.setText("查看待处理、已批准、已拒绝申请，支持审批流跟踪、审批意见与批量处理。");
            tvZoneDesc.setText("维护禁飞区列表，支持查询、新增、编辑、删除以及坐标、生效时段和原因配置。");
            btnZones.setText("进入禁飞区管理");
            btnApply.setText("进入飞行申请管理");
        } else {
            tvApplicationDesc.setText("提交飞行申请并关注当前审批状态。");
            tvZoneDesc.setText("查看实时禁飞区与飞行风险提醒。");
        }
        btnZones.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, "compliance");
            startActivity(new Intent(requireContext(), NoFlyZoneListActivity.class));
        });
        btnApply.setOnClickListener(v -> {
            analyticsStore.trackFeature(role, role == UserRole.ENTERPRISE ? "flight_manage_apply" : "compliance_apply");
            startActivity(new Intent(requireContext(), role == UserRole.ENTERPRISE
                    ? FlightApplicationManageActivity.class
                    : FlightApplicationActivity.class));
        });
    }

    private String complianceHint(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "围绕飞行申请审批和禁飞区维护统一管理空域风险，确保项目飞行计划可审、可追踪、可留痕。";
        }
        return "优先处理禁飞区查询与飞行申请，避免执行任务时重复切页找入口。";
    }

    private String complianceTitle(UserRole role, RoleUiConfig config) {
        if (role == UserRole.ENTERPRISE) {
            return "企业飞行管理";
        }
        return config.complianceHeadline;
    }
}

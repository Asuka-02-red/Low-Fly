package com.example.low_altitudereststop.feature.compliance;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

/**
 * 飞行申请管理Activity（企业端）。
 * <p>
 * 企业用户查看所有飞行申请，支持按状态筛选（全部/待处理/已批准/已拒绝）、
 * 单条审批（批准/驳回并填写意见）和批量审批操作，
 * 实时显示网络状态和申请统计摘要。
 * </p>
 */
public class FlightApplicationManageActivity extends NavigableEdgeToEdgeActivity
        implements FlightApplicationManageAdapter.ActionListener {

    private FlightManagementRepository repository;
    private FlightApplicationManageAdapter adapter;
    private final List<FlightManagementModels.FlightApplicationRecord> allRecords = new ArrayList<>();
    private String currentFilter = FlightApplicationWorkflow.FILTER_ALL;
    private TextView tvSummary;
    private TextView tvNetwork;
    private TextView tvEmpty;
    private TextInputEditText etBatchOpinion;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterPending;
    private MaterialButton btnFilterApproved;
    private MaterialButton btnFilterRejected;
    private MaterialButton btnBatchApprove;
    private MaterialButton btnBatchReject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_application_manage);
        repository = new FlightManagementRepository(this);
        adapter = new FlightApplicationManageAdapter(this);

        tvSummary = findViewById(R.id.tv_summary);
        tvNetwork = findViewById(R.id.tv_network_status);
        tvEmpty = findViewById(R.id.tv_empty);
        etBatchOpinion = findViewById(R.id.et_batch_opinion);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterPending = findViewById(R.id.btn_filter_pending);
        btnFilterApproved = findViewById(R.id.btn_filter_approved);
        btnFilterRejected = findViewById(R.id.btn_filter_rejected);
        btnBatchApprove = findViewById(R.id.btn_batch_approve);
        btnBatchReject = findViewById(R.id.btn_batch_reject);

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        bindActions();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
        loadData();
    }

    private void bindActions() {
        btnFilterAll.setOnClickListener(v -> switchFilter(FlightApplicationWorkflow.FILTER_ALL));
        btnFilterPending.setOnClickListener(v -> switchFilter(FlightApplicationWorkflow.FILTER_PENDING));
        btnFilterApproved.setOnClickListener(v -> switchFilter(FlightApplicationWorkflow.FILTER_APPROVED));
        btnFilterRejected.setOnClickListener(v -> switchFilter(FlightApplicationWorkflow.FILTER_REJECTED));
        btnBatchApprove.setOnClickListener(v -> applyBatch(FlightApplicationWorkflow.FILTER_APPROVED));
        btnBatchReject.setOnClickListener(v -> applyBatch(FlightApplicationWorkflow.FILTER_REJECTED));
    }

    private void switchFilter(@NonNull String filter) {
        currentFilter = filter;
        render();
    }

    private void loadData() {
        allRecords.clear();
        allRecords.addAll(repository.listApplications());
        render();
        updateNetworkStatus();
    }

    private void render() {
        List<FlightManagementModels.FlightApplicationRecord> filtered =
                FlightApplicationWorkflow.filter(allRecords, currentFilter);
        adapter.submit(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        renderFilterState();
        renderSummary();
        renderBatchState();
    }

    private void renderFilterState() {
        setFilterChecked(btnFilterAll, FlightApplicationWorkflow.FILTER_ALL.equals(currentFilter));
        setFilterChecked(btnFilterPending, FlightApplicationWorkflow.FILTER_PENDING.equals(currentFilter));
        setFilterChecked(btnFilterApproved, FlightApplicationWorkflow.FILTER_APPROVED.equals(currentFilter));
        setFilterChecked(btnFilterRejected, FlightApplicationWorkflow.FILTER_REJECTED.equals(currentFilter));
    }

    private void renderSummary() {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        for (FlightManagementModels.FlightApplicationRecord record : allRecords) {
            String status = FlightApplicationWorkflow.normalizeFilter(record.status);
            if (FlightApplicationWorkflow.FILTER_APPROVED.equals(status)) {
                approved++;
            } else if (FlightApplicationWorkflow.FILTER_REJECTED.equals(status)) {
                rejected++;
            } else {
                pending++;
            }
        }
        tvSummary.setText("待处理 " + pending + "  |  已批准 " + approved + "  |  已拒绝 " + rejected);
    }

    private void renderBatchState() {
        int selectedCount = FlightApplicationWorkflow.selectedPendingCount(allRecords);
        btnBatchApprove.setText(selectedCount > 0 ? "批量批准（" + selectedCount + "）" : "批量批准");
        btnBatchReject.setText(selectedCount > 0 ? "批量拒绝（" + selectedCount + "）" : "批量拒绝");
    }

    private void updateNetworkStatus() {
        boolean online = NetworkStatusHelper.isOnline(this);
        tvNetwork.setText(online ? "在线同步中" : "离线可查看");
    }

    private void setFilterChecked(MaterialButton button, boolean checked) {
        button.setChecked(checked);
    }

    private void applyBatch(String targetStatus) {
        List<String> selectedApplicationNos = new ArrayList<>();
        for (FlightManagementModels.FlightApplicationRecord record : allRecords) {
            if (record.selected && FlightApplicationWorkflow.FILTER_PENDING.equals(record.status)) {
                selectedApplicationNos.add(record.applicationNo);
            }
        }
        if (selectedApplicationNos.isEmpty()) {
            toast("请先勾选待处理申请");
            return;
        }
        String opinion = textOf(etBatchOpinion);
        if (opinion.isEmpty()) {
            opinion = FlightApplicationWorkflow.FILTER_APPROVED.equals(targetStatus) ? "批量审核通过" : "批量审核驳回";
        }
        repository.updateApplicationStatus(selectedApplicationNos, targetStatus, opinion);
        etBatchOpinion.setText("");
        loadData();
        toast(FlightApplicationWorkflow.FILTER_APPROVED.equals(targetStatus) ? "已批量批准" : "已批量拒绝");
    }

    @Override
    public void onSelectionChanged(FlightManagementModels.FlightApplicationRecord record, boolean selected) {
        renderBatchState();
    }

    @Override
    public void onApprove(FlightManagementModels.FlightApplicationRecord record) {
        showDecisionDialog(record, FlightApplicationWorkflow.FILTER_APPROVED);
    }

    @Override
    public void onReject(FlightManagementModels.FlightApplicationRecord record) {
        showDecisionDialog(record, FlightApplicationWorkflow.FILTER_REJECTED);
    }

    private void showDecisionDialog(FlightManagementModels.FlightApplicationRecord record, String targetStatus) {
        EditText input = new EditText(this);
        input.setHint("填写审批意见");
        input.setText(textOf(etBatchOpinion));
        new AlertDialog.Builder(this)
                .setTitle(FlightApplicationWorkflow.FILTER_APPROVED.equals(targetStatus) ? "批准申请" : "驳回申请")
                .setMessage(record.applicationNo + "\n" + record.projectName)
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String opinion = input.getText() == null ? "" : input.getText().toString().trim();
                    List<String> ids = java.util.Collections.singletonList(record.applicationNo);
                    repository.updateApplicationStatus(ids, targetStatus, opinion);
                    loadData();
                    toast(FlightApplicationWorkflow.FILTER_APPROVED.equals(targetStatus) ? "申请已批准" : "申请已驳回");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

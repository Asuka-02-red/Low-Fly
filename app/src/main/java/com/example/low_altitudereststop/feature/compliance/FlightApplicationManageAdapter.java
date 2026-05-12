package com.example.low_altitudereststop.feature.compliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * 飞行申请管理列表适配器（企业端）。
 * <p>
 * 展示飞行申请记录的申请号、状态、申请人、项目、飞行时间、用途、
 * 审批流程和意见等信息，支持勾选、批准和驳回操作回调。
 * </p>
 */
public class FlightApplicationManageAdapter extends RecyclerView.Adapter<FlightApplicationManageAdapter.VH> {

    public interface ActionListener {
        void onSelectionChanged(FlightManagementModels.FlightApplicationRecord record, boolean selected);

        void onApprove(FlightManagementModels.FlightApplicationRecord record);

        void onReject(FlightManagementModels.FlightApplicationRecord record);
    }

    private final List<FlightManagementModels.FlightApplicationRecord> items = new ArrayList<>();
    private final ActionListener actionListener;

    public FlightApplicationManageAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submit(List<FlightManagementModels.FlightApplicationRecord> records) {
        items.clear();
        if (records != null) {
            items.addAll(records);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_application_manage, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FlightManagementModels.FlightApplicationRecord record = items.get(position);
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(record.selected);
        holder.cbSelect.setEnabled(FlightApplicationWorkflow.FILTER_PENDING.equals(record.status));
        holder.tvApplicationNo.setText(record.applicationNo);
        holder.tvStatus.setText(statusText(record.status));
        holder.tvApplicant.setText(record.applicantName + " · " + record.applicantCompany);
        holder.tvProject.setText(record.projectName + " · " + record.location);
        holder.tvFlightTime.setText("计划时间：" + safe(record.flightTime));
        holder.tvPurpose.setText("用途：" + safe(record.purpose));
        holder.tvWorkflow.setText("流程：" + safe(record.workflowStatus));
        holder.tvOpinion.setText("审批意见：" + safe(record.approvalOpinion));
        boolean pending = FlightApplicationWorkflow.FILTER_PENDING.equals(record.status);
        holder.btnApprove.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.btnReject.setVisibility(pending ? View.VISIBLE : View.GONE);
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            record.selected = isChecked;
            if (actionListener != null) {
                actionListener.onSelectionChanged(record, isChecked);
            }
        });
        holder.btnApprove.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onApprove(record);
            }
        });
        holder.btnReject.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onReject(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String statusText(String status) {
        String normalized = FlightApplicationWorkflow.normalizeFilter(status);
        if (FlightApplicationWorkflow.FILTER_APPROVED.equals(normalized)) {
            return "已批准";
        }
        if (FlightApplicationWorkflow.FILTER_REJECTED.equals(normalized)) {
            return "已拒绝";
        }
        return "待处理";
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final CheckBox cbSelect;
        final TextView tvApplicationNo;
        final TextView tvStatus;
        final TextView tvApplicant;
        final TextView tvProject;
        final TextView tvFlightTime;
        final TextView tvPurpose;
        final TextView tvWorkflow;
        final TextView tvOpinion;
        final MaterialButton btnApprove;
        final MaterialButton btnReject;

        VH(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select);
            tvApplicationNo = itemView.findViewById(R.id.tv_application_no);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvApplicant = itemView.findViewById(R.id.tv_applicant);
            tvProject = itemView.findViewById(R.id.tv_project);
            tvFlightTime = itemView.findViewById(R.id.tv_flight_time);
            tvPurpose = itemView.findViewById(R.id.tv_purpose);
            tvWorkflow = itemView.findViewById(R.id.tv_workflow);
            tvOpinion = itemView.findViewById(R.id.tv_opinion);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}

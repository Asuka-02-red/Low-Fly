package com.example.low_altitudereststop.feature.risk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.core.model.PlatformModels;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警列表适配器，将告警数据绑定到RecyclerView列表项视图，
 * 支持点击事件回调以跳转至告警详情。
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.VH> {

    private final List<PlatformModels.AlertView> items = new ArrayList<>();
    private final OnAlertClickListener listener;

    public AlertAdapter(@NonNull OnAlertClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<PlatformModels.AlertView> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlatformModels.AlertView a = items.get(position);
        holder.tvTitle.setText((a.level == null ? "-" : a.level) + " · " + (a.status == null ? "-" : a.status));
        holder.tvContent.setText(a.content == null ? "-" : a.content);
        holder.tvTime.setText(a.createTime == null ? "" : a.createTime);
        holder.itemView.setOnClickListener(v -> listener.onAlertClick(a));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvContent;
        final TextView tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }

    interface OnAlertClickListener {
        void onAlertClick(@NonNull PlatformModels.AlertView alert);
    }
}


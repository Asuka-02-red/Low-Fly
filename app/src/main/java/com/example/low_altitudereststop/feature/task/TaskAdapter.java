package com.example.low_altitudereststop.feature.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(PlatformModels.TaskView task);
    }

    private final List<PlatformModels.TaskView> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public TaskAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<PlatformModels.TaskView> data) {
        int previousSize = items.size();
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyChanges(previousSize, items.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlatformModels.TaskView task = items.get(position);
        holder.tvTitle.setText(task.title == null ? "-" : task.title);
        String subtitle = (task.taskType == null ? "-" : task.taskType)
                + " · "
                + (task.location == null ? "-" : task.location)
                + " · 截止 "
                + (task.deadline == null ? "-" : task.deadline);
        holder.tvSubtitle.setText(subtitle);
        String status = "状态：" + (task.status == null ? "-" : task.status) + " · 预算：" + (task.budget == null ? "-" : task.budget.toPlainString());
        holder.tvStatus.setText(status);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void notifyChanges(int previousSize, int newSize) {
        if (previousSize == 0 && newSize > 0) {
            notifyItemRangeInserted(0, newSize);
            return;
        }
        if (newSize == 0 && previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
            return;
        }
        int sharedCount = Math.min(previousSize, newSize);
        if (sharedCount > 0) {
            notifyItemRangeChanged(0, sharedCount);
        }
        if (newSize > previousSize) {
            notifyItemRangeInserted(previousSize, newSize - previousSize);
        } else if (previousSize > newSize) {
            notifyItemRangeRemoved(newSize, previousSize - newSize);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}


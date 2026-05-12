package com.example.low_altitudereststop.feature.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务列表适配器。
 * <p>
 * 展示任务的标题、类型、地点、截止时间、预算、状态和发布方等信息，
 * 支持点击回调跳转到任务详情，采用差异化的列表更新策略。
 * </p>
 */
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
        String statusText = task.status == null ? "-" : task.status;
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setBackgroundResource(resolveStatusBackground(statusText));
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), resolveStatusTextColor(statusText)));
        String budgetText = "预算：" + (task.budget == null ? "-" : task.budget.toPlainString());
        holder.tvBudget.setText(budgetText);
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

    private static int resolveStatusBackground(String status) {
        String s = status == null ? "" : status.trim();
        if (containsAny(s, "完成", "已完成", "FINISHED", "DONE", "SUCCESS")) {
            return com.example.low_altitudereststop.core.ui.R.drawable.bg_badge_success;
        }
        if (containsAny(s, "取消", "失败", "超时", "异常", "拒绝", "ERROR", "FAILED", "CANCEL")) {
            return com.example.low_altitudereststop.core.ui.R.drawable.bg_badge_warning;
        }
        return com.example.low_altitudereststop.core.ui.R.drawable.bg_badge_info;
    }

    private static int resolveStatusTextColor(String status) {
        String s = status == null ? "" : status.trim();
        if (containsAny(s, "完成", "已完成", "FINISHED", "DONE", "SUCCESS")) {
            return com.example.low_altitudereststop.core.ui.R.color.ui_success;
        }
        if (containsAny(s, "取消", "失败", "超时", "异常", "拒绝", "ERROR", "FAILED", "CANCEL")) {
            return com.example.low_altitudereststop.core.ui.R.color.ui_warning;
        }
        return com.example.low_altitudereststop.core.ui.R.color.ui_info;
    }

    private static boolean containsAny(String source, String... keywords) {
        if (source == null || source.isEmpty() || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && source.toUpperCase().contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvStatus;
        final TextView tvBudget;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvBudget = itemView.findViewById(R.id.tv_budget);
        }
    }
}


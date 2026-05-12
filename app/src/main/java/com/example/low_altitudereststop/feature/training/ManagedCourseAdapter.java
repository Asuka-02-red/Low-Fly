package com.example.low_altitudereststop.feature.training;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * 企业管理课程列表适配器。
 * <p>
 * 展示企业已创建课程的标题、类型、难度、报名人数和状态，
 * 支持编辑和查看详情操作回调，用于CourseListActivity的企业管理视角。
 * </p>
 */
public class ManagedCourseAdapter extends RecyclerView.Adapter<ManagedCourseAdapter.VH> {

    public interface Listener {
        void onEdit(PlatformModels.CourseManageView course);
        void onPublish(PlatformModels.CourseManageView course);
        void onDelete(PlatformModels.CourseManageView course);
    }

    private final List<PlatformModels.CourseManageView> items = new ArrayList<>();
    private final Listener listener;

    public ManagedCourseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PlatformModels.CourseManageView> data) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_managed_course, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlatformModels.CourseManageView item = items.get(position);
        holder.tvTitle.setText(item.title == null ? "-" : item.title);
        holder.tvSummary.setText(item.summary == null || item.summary.trim().isEmpty() ? "暂无课程简介" : item.summary);
        holder.tvMeta.setText(statusLabel(item.status));
        holder.tvStats.setText(displayMode(item.learningMode)
                + "  ·  名额 " + item.seatAvailable + "/" + item.seatTotal
                + "\n浏览 " + item.browseCount
                + "  ·  报名 " + item.enrollCount
                + "  ·  ￥" + (item.price == null ? "-" : item.price.toPlainString()));
        boolean published = "OPEN".equalsIgnoreCase(item.status);
        holder.btnPublish.setText(published ? "已发布" : "发布");
        holder.btnPublish.setEnabled(!published);
        holder.btnPublish.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPublish(item);
            }
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
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

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String displayMode(String value) {
        if ("OFFLINE".equalsIgnoreCase(value)) {
            return "线下报名";
        }
        return "文章学习";
    }

    private String statusLabel(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            return "已发布";
        }
        if ("DRAFT".equalsIgnoreCase(status)) {
            return "草稿";
        }
        return safe(status);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSummary;
        final TextView tvMeta;
        final TextView tvStats;
        final MaterialButton btnPublish;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSummary = itemView.findViewById(R.id.tv_summary);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvStats = itemView.findViewById(R.id.tv_stats);
            btnPublish = itemView.findViewById(R.id.btn_publish);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}

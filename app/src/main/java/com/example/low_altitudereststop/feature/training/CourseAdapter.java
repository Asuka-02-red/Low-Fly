package com.example.low_altitudereststop.feature.training;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程列表适配器（飞手端）。
 * <p>
 * 展示课程的标题、类型、时长、难度和进度信息，
 * 支持点击回调跳转到课程详情，用于飞手浏览可选课程。
 * </p>
 */
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.VH> {

    public interface OnCourseClickListener {
        void onOpenCourse(PlatformModels.CourseView course);

        void onEnrollCourse(PlatformModels.CourseView course);
    }

    private final List<PlatformModels.CourseView> items = new ArrayList<>();
    private final OnCourseClickListener listener;

    public CourseAdapter(OnCourseClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<PlatformModels.CourseView> data) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlatformModels.CourseView c = items.get(position);
        holder.tvTitle.setText(c.title == null ? "-" : c.title);
        holder.tvSubtitle.setText(c.summary == null || c.summary.trim().isEmpty() ? "暂无课程简介" : c.summary);
        holder.tvStatus.setText(statusLabel(c.status));
        holder.tvMetaPrimary.setText(safe(c.category));
        holder.tvMetaSecondary.setText(displayMode(c.learningMode));
        holder.tvMetrics.setText("进度 " + progressLabel(c.status)
                + "  ·  余位 " + c.seatAvailable
                + "  ·  浏览 " + c.browseCount
                + "  ·  订阅 " + c.enrollCount
                + "  ·  主办方 " + safe(c.institutionName));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenCourse(c);
            }
        });
        bindButtonState(holder.btnEnroll, c);
        holder.btnEnroll.setEnabled(true);
        holder.btnEnroll.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEnrollCourse(c);
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

    private String displayMode(String mode) {
        if ("OFFLINE".equalsIgnoreCase(mode)) {
            return "线下报名";
        }
        return "文章学习";
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String statusLabel(String status) {
        String value = safe(status);
        if ("OPEN".equalsIgnoreCase(value)) {
            return "可订阅";
        }
        if ("DRAFT".equalsIgnoreCase(value)) {
            return "草稿";
        }
        return value;
    }

    private String progressLabel(String status) {
        String value = safe(status);
        if ("学习中".equals(value)) {
            return "35%";
        }
        if ("即将结课".equals(value)) {
            return "80%";
        }
        if ("已完成".equals(value)) {
            return "100%";
        }
        if ("待开始".equals(value)) {
            return "0%";
        }
        if ("OPEN".equalsIgnoreCase(value)) {
            return "20%";
        }
        return "-";
    }

    private void bindButtonState(@NonNull MaterialButton button, @NonNull PlatformModels.CourseView course) {
        int background = ContextCompat.getColor(
                button.getContext(),
                course.enrolled ? R.color.ui_success : R.color.ui_info
        );
        int textColor = ContextCompat.getColor(button.getContext(), R.color.white);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(textColor);
        button.setStrokeWidth(0);
        button.setText(course.enrolled ? "查看详情" : pendingLabel(course.learningMode));
    }

    private String pendingLabel(String learningMode) {
        if ("OFFLINE".equalsIgnoreCase(learningMode)) {
            return "查看并订阅";
        }
        return "查看详情";
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvStatus;
        final TextView tvMetaPrimary;
        final TextView tvMetaSecondary;
        final TextView tvMetrics;
        final MaterialButton btnEnroll;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvMetaPrimary = itemView.findViewById(R.id.tv_meta_primary);
            tvMetaSecondary = itemView.findViewById(R.id.tv_meta_secondary);
            tvMetrics = itemView.findViewById(R.id.tv_metrics);
            btnEnroll = itemView.findViewById(R.id.btn_enroll);
        }
    }
}

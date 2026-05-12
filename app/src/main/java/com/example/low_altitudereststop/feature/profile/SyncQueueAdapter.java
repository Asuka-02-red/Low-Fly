package com.example.low_altitudereststop.feature.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.storage.OperationOutboxEntity;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 离线同步队列适配器。
 * <p>
 * 展示待同步操作的类型、载荷摘要、状态和创建时间，
 * 支持重试按钮回调，用于SyncQueueActivity的列表展示。
 * </p>
 */
public class SyncQueueAdapter extends RecyclerView.Adapter<SyncQueueAdapter.VH> {

    public interface ActionListener {
        void onRetry(OperationOutboxEntity item);
    }

    private final List<OperationOutboxEntity> items = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final ActionListener listener;

    public SyncQueueAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void submit(List<OperationOutboxEntity> data) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sync_queue, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        OperationOutboxEntity item = items.get(position);
        holder.tvTitle.setText("#" + item.id + " · " + item.bizType);
        holder.tvStatus.setText("状态：" + item.status + "  重试：" + item.retryCount);
        holder.tvRequestId.setText("requestId：" + item.requestId);
        holder.tvTime.setText("创建：" + format(item.createTime) + "  下次：" + format(item.nextRetryAt));
        holder.tvPayload.setText(item.payload == null || item.payload.isEmpty() ? "payload：-" : "payload：" + item.payload);
        holder.tvError.setText(item.lastError == null || item.lastError.trim().isEmpty() ? "错误：-" : "错误：" + item.lastError);
        boolean canRetry = !"SUCCESS".equals(item.status);
        holder.btnRetry.setEnabled(canRetry);
        holder.btnRetry.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRetry(item);
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

    private String format(long time) {
        return time <= 0L ? "-" : sdf.format(time);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvStatus;
        final TextView tvRequestId;
        final TextView tvTime;
        final TextView tvPayload;
        final TextView tvError;
        final MaterialButton btnRetry;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRequestId = itemView.findViewById(R.id.tv_request_id);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPayload = itemView.findViewById(R.id.tv_payload);
            tvError = itemView.findViewById(R.id.tv_error);
            btnRetry = itemView.findViewById(R.id.btn_retry);
        }
    }
}


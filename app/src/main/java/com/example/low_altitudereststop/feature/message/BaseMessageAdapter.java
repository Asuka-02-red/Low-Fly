package com.example.low_altitudereststop.feature.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import java.util.ArrayList;
import java.util.List;

abstract class BaseMessageAdapter extends ListAdapter<MessageEntity, BaseMessageAdapter.VH> {

    interface OnMessageClickListener {
        void onClick(MessageEntity message);
    }

    private final MessageIdentityRepository identityRepository;
    private final OnMessageClickListener listener;

    protected BaseMessageAdapter(@NonNull MessageIdentityRepository identityRepository, OnMessageClickListener listener) {
        super(DIFF_CALLBACK);
        this.identityRepository = identityRepository;
        this.listener = listener;
    }

    public void submitMessages(List<MessageEntity> messages) {
        submitList(messages == null ? new ArrayList<>() : new ArrayList<>(messages));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MessageEntity message = getItem(position);
        holder.cancelPending();
        holder.tvName.setText("\u2014\u2014");
        SkeletonTextHelper.start(holder.tvName);
        holder.tvAvatar.setText("\u2014");
        holder.tvPreview.setText(message.content.isEmpty() ? "\u6682\u65e0\u6d88\u606f" : message.content);
        holder.tvTime.setText(message.createTime.isEmpty() ? "\u521a\u521a" : message.createTime);
        bindIdentity(holder, message);
        bindReadStatus(holder, message.isRead);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(message);
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        holder.cancelPending();
        SkeletonTextHelper.stop(holder.tvName);
    }

    private void bindReadStatus(VH holder, boolean isRead) {
        holder.tvReadStatus.setText(isRead ? "\u5df2\u8bfb" : "\u672a\u8bfb");
        holder.ivReadStatus.setColorFilter(ContextCompat.getColor(
                holder.itemView.getContext(),
                isRead ? R.color.ui_status_read : R.color.ui_status_unread
        ));
        holder.ivReadStatus.setContentDescription(isRead ? "\u5df2\u8bfb\u56fe\u6807" : "\u672a\u8bfb\u56fe\u6807");
        holder.ivReadStatus.setTag(isRead ? "read" : "unread");
    }

    private void bindIdentity(VH holder, MessageEntity message) {
        holder.pendingCancelable = createCancelableRequest(message, displayName -> {
            SkeletonTextHelper.stop(holder.tvName);
            holder.tvName.setText(displayName);
            holder.tvAvatar.setText(displayName.isEmpty() ? "\u2014" : displayName.substring(0, 1));
        });
    }

    protected abstract MessageIdentityRepository.Cancelable createCancelableRequest(MessageEntity message, MessageIdentityRepository.NameCallback callback);

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvName;
        final TextView tvPreview;
        final TextView tvTime;
        final TextView tvReadStatus;
        final ImageView ivReadStatus;
        MessageIdentityRepository.Cancelable pendingCancelable;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_counterpart_name);
            tvPreview = itemView.findViewById(R.id.tv_message_preview);
            tvTime = itemView.findViewById(R.id.tv_message_time);
            tvReadStatus = itemView.findViewById(R.id.tv_read_status);
            ivReadStatus = itemView.findViewById(R.id.iv_read_status);
        }

        void cancelPending() {
            if (pendingCancelable != null) {
                pendingCancelable.cancel();
                pendingCancelable = null;
            }
        }
    }

    private static final DiffUtil.ItemCallback<MessageEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<MessageEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull MessageEntity oldItem, @NonNull MessageEntity newItem) {
            return oldItem.msgId == newItem.msgId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageEntity oldItem, @NonNull MessageEntity newItem) {
            return oldItem.isRead == newItem.isRead
                    && oldItem.mine == newItem.mine
                    && oldItem.conversationId == newItem.conversationId
                    && oldItem.content.equals(newItem.content)
                    && oldItem.createTime.equals(newItem.createTime)
                    && oldItem.counterpartTitle.equals(newItem.counterpartTitle)
                    && oldItem.pilotUid.equals(newItem.pilotUid)
                    && oldItem.enterpriseUid.equals(newItem.enterpriseUid);
        }
    };
}

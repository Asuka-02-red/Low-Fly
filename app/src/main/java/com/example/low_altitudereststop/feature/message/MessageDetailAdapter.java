package com.example.low_altitudereststop.feature.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

/**
 * 消息详情聊天适配器，以气泡样式展示会话中的消息列表。
 * <p>
 * 区分自己发送和对方发送的消息，分别使用不同的气泡样式和对齐方式，
 * 展示发送者、内容、时间和已读状态，支持差异化更新。
 * </p>
 */
public class MessageDetailAdapter extends ListAdapter<MessageEntity, MessageDetailAdapter.VH> {

    public MessageDetailAdapter() {
        super(DIFF_CALLBACK);
    }

    public void submitMessages(List<MessageEntity> messages) {
        submitList(messages == null ? new ArrayList<>() : new ArrayList<>(messages));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_detail, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MessageEntity message = getItem(position);
        holder.tvSender.setText(message.mine ? "我" : (message.senderName.isEmpty() ? "对方" : message.senderName));
        holder.tvAvatar.setText(message.mine ? "我" : holder.tvSender.getText().toString().substring(0, 1));
        holder.tvContent.setText(message.content.isEmpty() ? "-" : message.content);
        holder.tvTime.setText(message.createTime.isEmpty() ? "刚刚" : message.createTime);
        holder.tvReadStatus.setText(message.isRead ? "已读" : "未读");
        holder.ivReadStatus.setColorFilter(ContextCompat.getColor(
                holder.itemView.getContext(),
                message.isRead ? R.color.ui_status_read : R.color.ui_status_unread
        ));
        holder.ivReadStatus.setContentDescription(message.isRead ? "已读图标" : "未读图标");
        holder.layoutRow.setGravity(message.mine ? Gravity.END : Gravity.START);
        LinearLayout bubbleGroup = holder.layoutBubbleGroup;
        if (message.mine) {
            holder.layoutRow.removeAllViews();
            bubbleGroup.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            holder.tvSender.setGravity(Gravity.END);
            holder.tvContent.setBackgroundResource(R.drawable.bg_chat_bubble_mine);
            holder.layoutRow.addView(bubbleGroup);
            holder.layoutRow.addView(holder.tvAvatar);
        } else {
            holder.layoutRow.removeAllViews();
            bubbleGroup.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            holder.tvSender.setGravity(Gravity.START);
            holder.tvContent.setBackgroundResource(R.drawable.bg_chat_bubble_other);
            holder.layoutRow.addView(holder.tvAvatar);
            holder.layoutRow.addView(bubbleGroup);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final LinearLayout layoutRow;
        final LinearLayout layoutBubbleGroup;
        final TextView tvAvatar;
        final TextView tvSender;
        final TextView tvContent;
        final TextView tvTime;
        final TextView tvReadStatus;
        final ImageView ivReadStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            layoutRow = itemView.findViewById(R.id.layout_message_row);
            layoutBubbleGroup = itemView.findViewById(R.id.layout_bubble_group);
            tvAvatar = itemView.findViewById(R.id.tv_detail_avatar);
            tvSender = itemView.findViewById(R.id.tv_detail_sender);
            tvContent = itemView.findViewById(R.id.tv_detail_content);
            tvTime = itemView.findViewById(R.id.tv_detail_time);
            tvReadStatus = itemView.findViewById(R.id.tv_detail_status);
            ivReadStatus = itemView.findViewById(R.id.iv_detail_status);
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
                    && oldItem.content.equals(newItem.content)
                    && oldItem.createTime.equals(newItem.createTime)
                    && oldItem.senderName.equals(newItem.senderName);
        }
    };
}

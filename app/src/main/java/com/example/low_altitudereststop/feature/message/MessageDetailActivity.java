package com.example.low_altitudereststop.feature.message;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

/**
 * 消息详情Activity，展示单个会话的聊天记录。
 * <p>
 * 加载指定会话的所有消息，支持发送新消息、实时接收已读状态变更、
 * 自动标记会话已读、同步待处理已读回执，以及草稿内容的保存与恢复。
 * </p>
 */
public class MessageDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    private static final String STATE_DRAFT = "state_draft";

    private long conversationId;
    private MessageRepository repository;
    private MessageDetailAdapter adapter;
    private final MessageRealtimeHub.Listener realtimeListener = (msgId, isRead) -> repository.updateReadStatus(msgId, isRead);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);
        applySafeTopInset(findViewById(R.id.root_message_detail));
        conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        if (conversationId <= 0L) {
            toast("会话不存在");
            finish();
            return;
        }
        repository = MessageRepository.get(this);
        adapter = new MessageDetailAdapter();
        androidx.recyclerview.widget.RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        repository.observeConversation(conversationId).observe(this, this::renderMessages);
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        if (savedInstanceState != null) {
            ((TextInputEditText) findViewById(R.id.et_message)).setText(savedInstanceState.getString(STATE_DRAFT, ""));
        }
        repository.refreshMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MessageRealtimeHub.getInstance().connect(this);
        MessageRealtimeHub.getInstance().addListener(realtimeListener);
        repository.markConversationRead(conversationId);
        repository.syncPendingReadReceipts();
    }

    @Override
    protected void onPause() {
        MessageRealtimeHub.getInstance().removeListener(realtimeListener);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        TextInputEditText editText = findViewById(R.id.et_message);
        outState.putString(STATE_DRAFT, editText.getText() == null ? "" : editText.getText().toString());
    }

    private void renderMessages(List<MessageEntity> messages) {
        adapter.submitMessages(messages);
        TextView subtitle = findViewById(R.id.tv_subtitle);
        if (messages == null || messages.isEmpty()) {
            subtitle.setText("暂无历史消息");
            return;
        }
        MessageEntity last = messages.get(messages.size() - 1);
        ((TextView) findViewById(R.id.tv_title)).setText(last.counterpartTitle.isEmpty() ? "消息详情" : last.counterpartTitle);
        subtitle.setText("会话消息 " + messages.size() + " 条");
    }

    private void sendMessage() {
        TextInputEditText editText = findViewById(R.id.et_message);
        String content = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (content.isEmpty()) {
            toast("请输入消息内容");
            return;
        }
        findViewById(R.id.btn_send).setEnabled(false);
        repository.sendMessage(conversationId, content, (success, message) -> {
            findViewById(R.id.btn_send).setEnabled(true);
            toast(message);
            if (success) {
                editText.setText(null);
            }
        });
    }

    protected void toast(String text) {
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void applySafeTopInset(@NonNull View root) {
        final int paddingStart = root.getPaddingStart();
        final int paddingTop = root.getPaddingTop();
        final int paddingEnd = root.getPaddingEnd();
        final int paddingBottom = root.getPaddingBottom();
        final int extraTop = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                getResources().getDisplayMetrics()
        ));
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPaddingRelative(
                    paddingStart,
                    paddingTop + insets.top + extraTop,
                    paddingEnd,
                    paddingBottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}

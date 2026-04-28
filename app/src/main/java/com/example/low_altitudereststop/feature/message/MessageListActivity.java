package com.example.low_altitudereststop.feature.message;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.List;

public class MessageListActivity extends NavigableEdgeToEdgeActivity {

    private MessageRepository repository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyState;
    private RecyclerView recyclerView;
    private BaseMessageAdapter adapter;
    private final MessageRealtimeHub.Listener realtimeListener = (msgId, isRead) -> repository.updateReadStatus(msgId, isRead);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);
        applySafeTopInset(findViewById(R.id.root_message_list));
        repository = MessageRepository.get(this);
        swipeRefreshLayout = findViewById(R.id.swipe);
        emptyState = findViewById(R.id.layout_empty_state);
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        bindAdapter();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            repository.refreshMessages();
            swipeRefreshLayout.setRefreshing(false);
        });
        MessageSyncNotifier.events().observe(this, text -> {
            if (text != null && !text.isEmpty()) {
                toast(text);
            }
        });
        repository.observeAllMessages().observe(this, this::renderMessages);
        repository.refreshMessages();
        long targetConversationId = getIntent().getLongExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, -1L);
        if (targetConversationId > 0L) {
            Intent intent = new Intent(this, MessageDetailActivity.class);
            intent.putExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, targetConversationId);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MessageRealtimeHub.getInstance().connect(this);
        MessageRealtimeHub.getInstance().addListener(realtimeListener);
        repository.syncPendingReadReceipts();
    }

    @Override
    protected void onPause() {
        MessageRealtimeHub.getInstance().removeListener(realtimeListener);
        super.onPause();
    }

    private void bindAdapter() {
        UserRole role = UserRole.from(new SessionStore(this).getCachedUser().role);
        MessageIdentityRepository identityRepository = MessageIdentityRepository.get(this);
        if (role == UserRole.ENTERPRISE) {
            adapter = new EnterpriseMessageAdapter(identityRepository, this::openDetail);
            setTitle("企业消息列表");
        } else {
            adapter = new PilotMessageAdapter(identityRepository, this::openDetail);
            setTitle("飞手消息列表");
        }
        recyclerView.setAdapter(adapter);
    }

    private void renderMessages(@NonNull List<MessageEntity> messages) {
        adapter.submitMessages(messages);
        emptyState.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openDetail(MessageEntity message) {
        Intent intent = new Intent(this, MessageDetailActivity.class);
        intent.putExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, message.conversationId);
        startActivity(intent);
    }

    private void toast(String text) {
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

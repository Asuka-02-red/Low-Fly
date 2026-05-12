package com.example.low_altitudereststop.feature.message;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageListActivity extends NavigableEdgeToEdgeActivity {

    private MessageRepository repository;
    private MessageIdentityRepository identityRepository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyState;
    private View loadingMoreView;
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private boolean isLoadingMore;
    private int currentPage;
    private static final int PAGE_SIZE = 20;

    private final MessageRealtimeHub.Listener realtimeListener = (msgId, isRead) -> repository.updateReadStatus(msgId, isRead);

    private final MessageRealtimeHub.NewMessageListener newMessageListener = (conversationId, msgId) -> {
        repository.refreshMessages();
        scrollToTop();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);
        applySafeTopInset(findViewById(R.id.root_message_list));

        repository = MessageRepository.get(this);
        identityRepository = MessageIdentityRepository.get(this);

        swipeRefreshLayout = findViewById(R.id.swipe);
        emptyState = findViewById(R.id.layout_empty_state);
        loadingMoreView = findViewById(R.id.layout_loading_more);
        recyclerView = findViewById(R.id.recycler);

        setupRecyclerView();
        setupSwipeRefresh();
        setupLoadMore();

        currentPage = 0;
        isLoadingMore = false;

        MessageSyncNotifier.events().observe(this, text -> {
            if (text != null && !text.isEmpty()) {
                toast(text);
            }
        });

        repository.observeAllMessages().observe(this, this::renderConversations);
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
        MessageRealtimeHub.getInstance().addNewMessageListener(newMessageListener);
        repository.syncPendingReadReceipts();
    }

    @Override
    protected void onPause() {
        MessageRealtimeHub.getInstance().removeListener(realtimeListener);
        MessageRealtimeHub.getInstance().removeNewMessageListener(newMessageListener);
        super.onPause();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);

        UserRole role = UserRole.from(new SessionStore(this).getCachedUser().role);
        adapter = new ChatListAdapter(identityRepository, item -> {
            Intent intent = new Intent(this, MessageDetailActivity.class);
            intent.putExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, item.conversationId);
            startActivity(intent);
        });
        setTitle(role == UserRole.ENTERPRISE ? "\u4f01\u4e1a\u6d88\u606f\u5217\u8868" : "\u98de\u624b\u6d88\u606f\u5217\u8868");
        recyclerView.setAdapter(adapter);

        ChatSwipeCallback swipeCallback = new ChatSwipeCallback(adapter, new ChatListAdapter.OnSwipeActionListener() {
            @Override
            public void onMarkAsRead(@NonNull ChatConversationItem item, int position) {
                repository.markConversationRead(item.conversationId);
            }

            @Override
            public void onDelete(@NonNull ChatConversationItem item, int position) {
                repository.deleteConversation(item.conversationId);
            }
        });
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.ui_info,
                R.color.ui_status_unread
        );
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 0;
            repository.refreshMessages();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupLoadMore() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return;
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                if (!isLoadingMore
                        && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0) {
                    loadMoreConversations();
                }
            }
        });
    }

    private void renderConversations(@NonNull List<MessageEntity> messages) {
        if (isFinishing() || isDestroyed()) return;
        UserRole role = UserRole.from(new SessionStore(this).getCachedUser().role);

        Map<Long, ConversationAggregator> aggregatorMap = new HashMap<>();
        for (MessageEntity msg : messages) {
            ConversationAggregator aggregator = aggregatorMap.get(msg.conversationId);
            if (aggregator == null) {
                aggregator = new ConversationAggregator(msg.conversationId);
                aggregatorMap.put(msg.conversationId, aggregator);
            }
            aggregator.addMessage(msg);
        }

        List<ChatConversationItem> items = new ArrayList<>();
        for (ConversationAggregator aggregator : aggregatorMap.values()) {
            items.add(aggregator.build(role));
        }
        items.sort((a, b) -> Long.compare(b.lastMessageTimeMillis, a.lastMessageTimeMillis));

        adapter.submitList(items);
        emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadMoreConversations() {
        if (isLoadingMore) return;
        isLoadingMore = true;
        loadingMoreView.setVisibility(View.VISIBLE);
        currentPage++;
        recyclerView.postDelayed(() -> {
            loadingMoreView.setVisibility(View.GONE);
            isLoadingMore = false;
        }, 800);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void scrollToTop() {
        if (recyclerView != null) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
        }
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

    private static class ConversationAggregator {
        final long conversationId;
        String lastMessagePreview = "";
        String lastMessageTime = "";
        long lastMessageTimeMillis = 0;
        int unreadCount = 0;
        boolean allRead = true;
        String pilotUid = "";
        String enterpriseUid = "";
        String counterpartTitle = "";

        ConversationAggregator(long conversationId) {
            this.conversationId = conversationId;
        }

        void addMessage(@NonNull MessageEntity msg) {
            if (msg.createTimeMillis > lastMessageTimeMillis) {
                lastMessageTimeMillis = msg.createTimeMillis;
                lastMessagePreview = msg.content;
                lastMessageTime = msg.createTime;
            }
            if (!msg.counterpartTitle.isEmpty()) {
                counterpartTitle = msg.counterpartTitle;
            }
            if (!msg.pilotUid.isEmpty()) {
                pilotUid = msg.pilotUid;
            }
            if (!msg.enterpriseUid.isEmpty()) {
                enterpriseUid = msg.enterpriseUid;
            }
            if (!msg.mine && !msg.isRead) {
                unreadCount++;
                allRead = false;
            }
        }

        ChatConversationItem build(UserRole role) {
            String resolvedUid;
            String resolvedRole;
            if (role == UserRole.ENTERPRISE) {
                resolvedUid = pilotUid;
                resolvedRole = "pilot";
            } else {
                resolvedUid = enterpriseUid;
                resolvedRole = "enterprise";
            }
            return new ChatConversationItem(
                    conversationId,
                    counterpartTitle,
                    resolvedUid,
                    resolvedRole,
                    lastMessagePreview.isEmpty() ? "\u6682\u65e0\u6d88\u606f" : lastMessagePreview,
                    lastMessageTime.isEmpty() ? "\u521a\u521a" : lastMessageTime,
                    unreadCount,
                    lastMessageTimeMillis,
                    allRead
            );
        }
    }
}

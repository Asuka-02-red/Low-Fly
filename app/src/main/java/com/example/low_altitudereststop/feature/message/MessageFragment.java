package com.example.low_altitudereststop.feature.message;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.PageStateController;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageFragment extends Fragment {

    private PageStateController stateController;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private View emptyState;
    private View loadingMoreView;
    private TextView tvUnreadNotifications;
    private TextView tvUnreadChats;
    private ChatListAdapter adapter;
    private MessageRepository repository;
    private MessageIdentityRepository identityRepository;
    private boolean hasShownLocalData;
    private boolean isLoadingMore;
    private int currentPage;
    private static final int PAGE_SIZE = 20;
    private Observer<List<MessageEntity>> localObserver;

    private final MessageRealtimeHub.Listener realtimeListener = (msgId, isRead) -> {
        if (repository != null) {
            repository.updateReadStatus(msgId, isRead);
        }
    };

    private final MessageRealtimeHub.NewMessageListener newMessageListener = (conversationId, msgId) -> {
        if (repository != null) {
            repository.refreshMessages();
        }
        scrollToTop();
    };

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        stateController = new PageStateController(
                view.findViewById(R.id.layout_message_state),
                view.findViewById(R.id.card_message_header),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.progress_page_state),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_title),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_desc),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.btn_page_state_retry)
        );

        repository = MessageRepository.get(requireContext());
        identityRepository = MessageIdentityRepository.get(requireContext());

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_chat_list);
        emptyState = view.findViewById(R.id.layout_empty_state);
        loadingMoreView = view.findViewById(R.id.layout_loading_more);
        tvUnreadNotifications = view.findViewById(R.id.tv_unread_notifications);
        tvUnreadChats = view.findViewById(R.id.tv_unread_chats);

        setupRecyclerView();
        setupSwipeRefresh();
        setupLoadMore();
        hasShownLocalData = false;
        currentPage = 0;
        loadLocalThenRemote();
    }

    @Override
    public void onResume() {
        super.onResume();
        MessageRealtimeHub.getInstance().connect(requireContext());
        MessageRealtimeHub.getInstance().addListener(realtimeListener);
        MessageRealtimeHub.getInstance().addNewMessageListener(newMessageListener);
        if (repository != null) {
            repository.syncPendingReadReceipts();
        }
    }

    @Override
    public void onPause() {
        MessageRealtimeHub.getInstance().removeListener(realtimeListener);
        MessageRealtimeHub.getInstance().removeNewMessageListener(newMessageListener);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (localObserver != null && repository != null) {
            repository.observeAllMessages().removeObserver(localObserver);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);

        adapter = new ChatListAdapter(identityRepository, this::openChatDetail);
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

    private void loadLocalThenRemote() {
        localObserver = messages -> {
            if (messages != null && !messages.isEmpty() && !hasShownLocalData) {
                hasShownLocalData = true;
                renderConversations(messages);
            }
        };
        repository.observeAllMessages().observe(getViewLifecycleOwner(), localObserver);

        if (!hasShownLocalData) {
            stateController.showLoading("\u6b63\u5728\u540c\u6b65\u6d88\u606f", "\u6b63\u5728\u6c47\u603b\u4f01\u4e1a\u4e0e\u98de\u624b\u7684\u534f\u540c\u4f1a\u8bdd\uff0c\u8bf7\u7a0d\u5019\u3002");
        }
        loadConversationsFromNetwork();
    }

    private void loadConversationsFromNetwork() {
        if (!isAdded() || getContext() == null) return;
        ApiClient.getAuthedService(requireContext()).listMessageConversations().enqueue(
                new Callback<ApiEnvelope<List<PlatformModels.MessageConversationView>>>() {
                    @Override
                    public void onResponse(
                            Call<ApiEnvelope<List<PlatformModels.MessageConversationView>>> call,
                            Response<ApiEnvelope<List<PlatformModels.MessageConversationView>>> response
                    ) {
                        if (!isAdded()) return;
                        ApiEnvelope<List<PlatformModels.MessageConversationView>> envelope = response.body();
                        if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                            if (!hasShownLocalData) {
                                showMockConversations();
                            }
                            return;
                        }
                        if (envelope.data.isEmpty()) {
                            if (!hasShownLocalData) {
                                showMockConversations();
                            }
                            return;
                        }
                        repository.refreshMessages();
                    }

                    @Override
                    public void onFailure(
                            Call<ApiEnvelope<List<PlatformModels.MessageConversationView>>> call,
                            Throwable t
                    ) {
                        if (!isAdded()) return;
                        if (!hasShownLocalData) {
                            showMockConversations();
                        }
                    }
                }
        );
    }

    private void renderConversations(@NonNull List<MessageEntity> messages) {
        if (!isAdded() || getContext() == null) return;
        UserRole role = UserRole.from(new SessionStore(requireContext()).getCachedUser().role);

        Map<Long, ConversationBuilder> builderMap = new HashMap<>();
        for (MessageEntity msg : messages) {
            ConversationBuilder builder = builderMap.get(msg.conversationId);
            if (builder == null) {
                builder = new ConversationBuilder(msg.conversationId);
                builderMap.put(msg.conversationId, builder);
            }
            builder.addMessage(msg);
        }

        List<ChatConversationItem> items = new ArrayList<>();
        int totalUnread = 0;
        for (ConversationBuilder builder : builderMap.values()) {
            ChatConversationItem item = builder.build(role);
            items.add(item);
            totalUnread += item.unreadCount;
        }

        items.sort((a, b) -> Long.compare(b.lastMessageTimeMillis, a.lastMessageTimeMillis));

        updateHeaderStats(role, totalUnread, items.size());
        adapter.submitList(items);

        emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);

        if (!items.isEmpty()) {
            stateController.showContent();
        } else {
            stateController.showEmpty("\u6682\u65e0\u4f1a\u8bdd", "\u5f53\u524d\u8fd8\u6ca1\u6709\u53ef\u7528\u7684\u4f01\u4e1a\u4e0e\u98de\u624b\u6c9f\u901a\u8bb0\u5f55\u3002", "\u91cd\u65b0\u52a0\u8f7d", () -> loadConversationsFromNetwork());
        }
    }

    private void updateHeaderStats(UserRole role, int totalUnread, int conversationCount) {
        if (!isAdded() || getView() == null) return;
        ((TextView) requireView().findViewById(R.id.tv_title)).setText(
                role == UserRole.ENTERPRISE ? "\u4f01\u4e1a\u534f\u540c\u6d88\u606f" : "\u98de\u624b\u534f\u540c\u6d88\u606f");
        tvUnreadNotifications.setText(String.valueOf(totalUnread));
        tvUnreadChats.setText(String.valueOf(conversationCount));
    }

    private void showMockConversations() {
        if (!isAdded() || getContext() == null) return;
        UserRole role = UserRole.from(new SessionStore(requireContext()).getCachedUser().role);
        List<MessageEntity> mockMessages = MessageMockDataSource.buildMessageEntities(role);
        if (!mockMessages.isEmpty()) {
            repository.replaceAllForTesting(mockMessages);
        }
        renderConversations(mockMessages);
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

    private void openChatDetail(@NonNull ChatConversationItem item) {
        if (!isAdded() || getContext() == null) return;
        Intent intent = new Intent(requireContext(), MessageDetailActivity.class);
        intent.putExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, item.conversationId);
        startActivity(intent);
    }

    private void scrollToTop() {
        if (recyclerView != null) {
            recyclerView.post(() -> {
                if (recyclerView != null) {
                    recyclerView.smoothScrollToPosition(0);
                }
            });
        }
    }

    private static class ConversationBuilder {
        final long conversationId;
        String lastMessagePreview = "";
        String lastMessageTime = "";
        long lastMessageTimeMillis = 0;
        int unreadCount = 0;
        boolean allRead = true;
        String pilotUid = "";
        String enterpriseUid = "";
        String counterpartTitle = "";

        ConversationBuilder(long conversationId) {
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

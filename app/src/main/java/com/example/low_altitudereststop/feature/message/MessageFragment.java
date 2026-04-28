package com.example.low_altitudereststop.feature.message;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.PageStateController;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageFragment extends Fragment {

    private PageStateController stateController;
    private boolean hasShownLocalData;
    private Observer<List<MessageEntity>> localObserver;

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        stateController = new PageStateController(
                view.findViewById(R.id.layout_message_state),
                view.findViewById(R.id.layout_message_content),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.progress_page_state),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_title),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_desc),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.btn_page_state_retry)
        );
        hasShownLocalData = false;
        loadLocalThenRemote(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (localObserver != null) {
            MessageRepository.get(requireContext()).observeAllMessages().removeObserver(localObserver);
        }
    }

    private void loadLocalThenRemote(@NonNull View view) {
        MessageRepository repository = MessageRepository.get(requireContext());
        localObserver = messages -> {
            if (messages != null && !messages.isEmpty() && !hasShownLocalData) {
                hasShownLocalData = true;
                renderFromLocalCache(view, messages);
            }
        };
        repository.observeAllMessages().observe(getViewLifecycleOwner(), localObserver);
        if (!hasShownLocalData) {
            stateController.showLoading("正在同步消息", "正在汇总企业与飞手的协同会话，请稍候。");
        }
        loadConversations(view);
    }

    private void renderFromLocalCache(@NonNull View view, @NonNull List<MessageEntity> messages) {
        UserRole role = UserRole.from(new SessionStore(requireContext()).getCachedUser().role);
        Set<Long> conversationIds = new HashSet<>();
        List<PlatformModels.MessageConversationView> conversations = new ArrayList<>();
        for (MessageEntity msg : messages) {
            if (!conversationIds.contains(msg.conversationId)) {
                conversationIds.add(msg.conversationId);
                PlatformModels.MessageConversationView conv = new PlatformModels.MessageConversationView();
                conv.id = msg.conversationId;
                conv.title = msg.counterpartTitle;
                conv.lastMessagePreview = msg.content;
                conv.counterpartRole = msg.senderRole;
                conv.lastMessageTime = msg.createTime;
                conversations.add(conv);
            }
        }
        int unreadCount = 0;
        for (MessageEntity msg : messages) {
            if (!msg.mine && !msg.isRead) {
                unreadCount++;
            }
        }
        if (!conversations.isEmpty()) {
            ((TextView) view.findViewById(R.id.tv_title)).setText(role == UserRole.ENTERPRISE ? "企业协同消息" : "飞手协同消息");
            ((TextView) view.findViewById(R.id.tv_hint)).setText(role == UserRole.ENTERPRISE
                    ? "查看飞手回执、执行沟通与项目安排，支持实时双向发送。"
                    : "查看企业派单、执行沟通与回执消息，支持实时双向发送。");
            ((TextView) view.findViewById(R.id.tv_unread_notifications)).setText(String.valueOf(unreadCount));
            ((TextView) view.findViewById(R.id.tv_unread_chats)).setText(String.valueOf(conversations.size()));
            bindConversation(view, R.id.card_thread_one, conversations, 0);
            bindConversation(view, R.id.card_thread_two, conversations, 1);
            bindConversation(view, R.id.card_thread_three, conversations, 2);
            stateController.showContent();
        }
    }

    private void loadConversations(@NonNull View view) {
        ApiClient.getAuthedService(requireContext()).listMessageConversations().enqueue(new Callback<ApiEnvelope<List<PlatformModels.MessageConversationView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.MessageConversationView>>> call, Response<ApiEnvelope<List<PlatformModels.MessageConversationView>>> response) {
                ApiEnvelope<List<PlatformModels.MessageConversationView>> envelope = response.body();
                if (!response.isSuccessful() || envelope == null) {
                    if (!hasShownLocalData) {
                        showMockMessages(view);
                    }
                    return;
                }
                if (envelope.code != 200 || envelope.data == null) {
                    if (!hasShownLocalData) {
                        showMockMessages(view);
                    }
                    return;
                }
                if (envelope.data.isEmpty()) {
                    if (!hasShownLocalData) {
                        showMockMessages(view);
                    }
                    return;
                }
                renderDashboard(view, envelope.data);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.MessageConversationView>>> call, Throwable t) {
                if (!hasShownLocalData) {
                    showMockMessages(view);
                }
            }
        });
    }

    private void renderDashboard(@NonNull View view, @NonNull List<PlatformModels.MessageConversationView> conversations) {
        UserRole role = UserRole.from(new SessionStore(requireContext()).getCachedUser().role);
        if (conversations.isEmpty()) {
            stateController.showEmpty("暂无会话", "当前还没有可用的企业与飞手沟通记录。", "重新加载", () -> loadConversations(view));
            return;
        }
        int unreadCount = conversations.stream().mapToInt(item -> item.unreadCount).sum();
        ((TextView) view.findViewById(R.id.tv_title)).setText(role == UserRole.ENTERPRISE ? "企业协同消息" : "飞手协同消息");
        ((TextView) view.findViewById(R.id.tv_hint)).setText(role == UserRole.ENTERPRISE
                ? "查看飞手回执、执行沟通与项目安排，支持实时双向发送。"
                : "查看企业派单、执行沟通与回执消息，支持实时双向发送。");
        ((TextView) view.findViewById(R.id.tv_unread_notifications)).setText(String.valueOf(unreadCount));
        ((TextView) view.findViewById(R.id.tv_unread_chats)).setText(String.valueOf(conversations.size()));

        bindConversation(view, R.id.card_thread_one, conversations, 0);
        bindConversation(view, R.id.card_thread_two, conversations, 1);
        bindConversation(view, R.id.card_thread_three, conversations, 2);
        stateController.showContent();
    }

    private void showMockMessages(@NonNull View view) {
        UserRole role = UserRole.from(new SessionStore(requireContext()).getCachedUser().role);
        renderDashboard(view, MessageMockDataSource.buildConversationSummaries(role));
    }

    private void bindConversation(@NonNull View root, int cardId, @NonNull List<PlatformModels.MessageConversationView> items, int index) {
        View card = root.findViewById(cardId);
        if (index >= items.size()) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        PlatformModels.MessageConversationView summary = items.get(index);
        ((TextView) card.findViewById(R.id.tv_thread_title)).setText(summary.title);
        ((TextView) card.findViewById(R.id.tv_thread_preview)).setText(summary.lastMessagePreview);
        ((TextView) card.findViewById(R.id.tv_thread_badge)).setText(summary.counterpartRole + " · " + summary.unreadCount + " 条对方消息");
        ((TextView) card.findViewById(R.id.tv_thread_time)).setText(summary.lastMessageTime);
        card.setOnClickListener(v -> openThread(summary.id));
    }

    private void openThread(@NonNull Long conversationId) {
        Intent intent = new Intent(requireContext(), MessageListActivity.class);
        intent.putExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }
}

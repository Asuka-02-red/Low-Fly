package com.example.low_altitudereststop.feature.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.message.local.EnterpriseProfileDao;
import com.example.low_altitudereststop.feature.message.local.MessageDao;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.feature.message.local.MessageLocalDatabase;
import com.example.low_altitudereststop.feature.message.local.PilotProfileDao;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Response;

/**
 * 消息数据仓库，管理消息的加载、发送、已读状态和本地持久化。
 * <p>
 * 提供消息列表查询（LiveData）、发送消息、标记会话已读、
 * 更新单条消息已读状态、同步待处理已读回执等能力，
 * 优先使用本地数据库，网络不可用时自动降级到演示数据。
 * </p>
 */
public class MessageRepository {

    public interface CompletionCallback {
        void onComplete(boolean success, String message);
    }

    private static volatile MessageRepository INSTANCE;

    private final Context appContext;
    private final MessageDao messageDao;
    private final PilotProfileDao pilotProfileDao;
    private final EnterpriseProfileDao enterpriseProfileDao;
    private final ExecutorService ioExecutor;
    private final ExecutorService threadPool;
    private final Handler mainHandler;

    public MessageRepository(@NonNull Context context) {
        this(
                context,
                MessageLocalDatabase.get(context).messageDao(),
                MessageLocalDatabase.get(context).pilotProfileDao(),
                MessageLocalDatabase.get(context).enterpriseProfileDao(),
                Executors.newSingleThreadExecutor(),
                Executors.newFixedThreadPool(4),
                createMainHandlerSafely()
        );
    }

    MessageRepository(@NonNull Context context, @NonNull MessageDao messageDao, @NonNull ExecutorService ioExecutor) {
        this(context, messageDao, null, null, ioExecutor, Executors.newFixedThreadPool(4), createMainHandlerSafely());
    }

    MessageRepository(@NonNull Context context, @NonNull MessageDao messageDao, PilotProfileDao pilotProfileDao, EnterpriseProfileDao enterpriseProfileDao, @NonNull ExecutorService ioExecutor, @NonNull ExecutorService threadPool, Handler mainHandler) {
        this.appContext = context.getApplicationContext();
        this.messageDao = messageDao;
        this.pilotProfileDao = pilotProfileDao;
        this.enterpriseProfileDao = enterpriseProfileDao;
        this.ioExecutor = ioExecutor;
        this.threadPool = threadPool;
        this.mainHandler = mainHandler;
    }

    public static MessageRepository get(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (MessageRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MessageRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<MessageEntity>> observeAllMessages() {
        return messageDao.observeAllMessages();
    }

    public LiveData<List<MessageEntity>> observeConversation(long conversationId) {
        return messageDao.observeConversation(conversationId);
    }

    public LiveData<MessageEntity> observeMessage(long msgId) {
        return messageDao.observeMessage(msgId);
    }

    public void refreshMessages() {
        ioExecutor.execute(() -> {
            try {
                Response<ApiEnvelope<List<PlatformModels.MessageConversationView>>> conversationsResponse =
                        ApiClient.getAuthedService(appContext).listMessageConversations().execute();
                if (!conversationsResponse.isSuccessful()
                        || conversationsResponse.body() == null
                        || conversationsResponse.body().data == null) {
                    applyMockMessages();
                    return;
                }
                if (conversationsResponse.body().data.isEmpty()) {
                    applyMockMessages();
                    return;
                }
                List<PlatformModels.MessageConversationView> conversations = conversationsResponse.body().data;
                List<MessageEntity> entities = new ArrayList<>();
                long fallbackMillis = System.currentTimeMillis();
                List<PlatformModels.MessageConversationView> validConversations = new ArrayList<>();
                for (PlatformModels.MessageConversationView conversation : conversations) {
                    if (conversation != null && conversation.id != null) {
                        validConversations.add(conversation);
                    }
                }
                if (validConversations.isEmpty()) {
                    applyMockMessages();
                    return;
                }
                int threadCount = validConversations.size();
                CountDownLatch latch = new CountDownLatch(threadCount);
                AtomicInteger successCount = new AtomicInteger(0);
                for (PlatformModels.MessageConversationView conversation : validConversations) {
                    threadPool.execute(() -> {
                        try {
                            Response<ApiEnvelope<PlatformModels.MessageThreadView>> threadResponse =
                                    ApiClient.getAuthedService(appContext).getMessageThread(conversation.id).execute();
                            if (threadResponse.isSuccessful()
                                    && threadResponse.body() != null
                                    && threadResponse.body().data != null
                                    && threadResponse.body().data.messages != null) {
                                PlatformModels.MessageThreadView thread = threadResponse.body().data;
                                List<MessageEntity> threadEntities = new ArrayList<>();
                                long threadFallback = fallbackMillis - successCount.get();
                                for (PlatformModels.MessageEntryView item : thread.messages) {
                                    if (item != null && item.id != null) {
                                        threadEntities.add(toEntity(thread, conversation, item, threadFallback--));
                                    }
                                }
                                synchronized (entities) {
                                    entities.addAll(threadEntities);
                                }
                                successCount.incrementAndGet();
                            }
                        } catch (Exception ignored) {
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                try {
                    latch.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (!entities.isEmpty()) {
                    messageDao.upsertAll(entities);
                    persistMockProfiles();
                } else {
                    applyMockMessages();
                }
            } catch (Exception ignored) {
                applyMockMessages();
            }
        });
    }

    public void updateReadStatus(long msgId, boolean isRead) {
        ioExecutor.execute(() -> messageDao.updateReadStatus(msgId, isRead));
    }

    public void markConversationRead(long conversationId) {
        ioExecutor.execute(() -> {
            messageDao.markConversationAsRead(conversationId);
            MessageReadReceiptWorker.enqueueNow(appContext);
        });
    }

    public void syncPendingReadReceipts() {
        MessageReadReceiptWorker.enqueueNow(appContext);
    }

    public void sendMessage(long conversationId, @NonNull String content, CompletionCallback callback) {
        ioExecutor.execute(() -> {
            boolean success = false;
            String message = "发送失败，请稍后重试";
            try {
                PlatformModels.MessageSendRequest request = new PlatformModels.MessageSendRequest();
                request.content = content;
                Response<ApiEnvelope<PlatformModels.MessageThreadView>> response =
                        ApiClient.getAuthedService(appContext).sendMessage(conversationId, request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    PlatformModels.MessageThreadView thread = response.body().data;
                    List<MessageEntity> entities = new ArrayList<>();
                    long fallbackMillis = System.currentTimeMillis();
                    if (thread.messages != null) {
                        for (PlatformModels.MessageEntryView item : thread.messages) {
                            if (item != null && item.id != null) {
                                entities.add(toEntity(thread, null, item, fallbackMillis--));
                            }
                        }
                    }
                    if (!entities.isEmpty()) {
                        messageDao.upsertAll(entities);
                    }
                    success = true;
                    message = "已发送";
                } else if (MessageMockDataSource.isMockConversation(conversationId)) {
                    messageDao.upsertAll(java.util.Collections.singletonList(
                            MessageMockDataSource.buildOutgoingMessage(conversationId, content, currentRole())
                    ));
                    success = true;
                    message = "已发送";
                }
            } catch (Exception ignored) {
                if (MessageMockDataSource.isMockConversation(conversationId)) {
                    messageDao.upsertAll(java.util.Collections.singletonList(
                            MessageMockDataSource.buildOutgoingMessage(conversationId, content, currentRole())
                    ));
                    success = true;
                    message = "已发送";
                }
            }
            if (callback != null) {
                boolean finalSuccess = success;
                String finalMessage = message;
                if (mainHandler != null) {
                    mainHandler.post(() -> callback.onComplete(finalSuccess, finalMessage));
                } else {
                    callback.onComplete(finalSuccess, finalMessage);
                }
            }
        });
    }

    public void replaceAllForTesting(@NonNull List<MessageEntity> entities) {
        messageDao.clearAll();
        messageDao.upsertAll(entities);
    }

    public void deleteConversation(long conversationId) {
        ioExecutor.execute(() -> messageDao.deleteConversation(conversationId));
    }

    private void applyMockMessages() {
        messageDao.clearAll();
        messageDao.upsertAll(MessageMockDataSource.buildMessageEntities(currentRole()));
        persistMockProfiles();
    }

    private void persistMockProfiles() {
        if (pilotProfileDao != null) {
            for (com.example.low_altitudereststop.feature.message.local.PilotProfileEntity entity : MessageMockDataSource.buildPilotProfiles()) {
                pilotProfileDao.upsert(entity);
            }
        }
        if (enterpriseProfileDao != null) {
            for (com.example.low_altitudereststop.feature.message.local.EnterpriseProfileEntity entity : MessageMockDataSource.buildEnterpriseProfiles()) {
                enterpriseProfileDao.upsert(entity);
            }
        }
    }

    @NonNull
    private UserRole currentRole() {
        return UserRole.from(new SessionStore(appContext).getCachedUser().role);
    }

    private MessageEntity toEntity(
            PlatformModels.MessageThreadView thread,
            PlatformModels.MessageConversationView conversation,
            PlatformModels.MessageEntryView item,
            long fallbackMillis
    ) {
        MessageEntity entity = new MessageEntity();
        entity.msgId = item.id;
        entity.conversationId = thread.conversationId == null ? 0L : thread.conversationId;
        entity.content = safe(item.content);
        entity.senderName = safe(item.senderName);
        entity.senderRole = safe(item.senderRole);
        entity.pilotUid = pickUid(item.pilotUid, thread.pilotUid, conversation == null ? null : conversation.pilotUid, item.senderId);
        entity.enterpriseUid = pickUid(item.enterpriseUid, thread.enterpriseUid, conversation == null ? null : conversation.enterpriseUid, null);
        entity.counterpartTitle = conversation != null ? safe(conversation.title) : safe(thread.title);
        entity.createTime = safe(item.createTime);
        entity.createTimeMillis = fallbackMillis;
        entity.mine = item.mine;
        entity.isRead = item.isRead || item.mine;
        entity.readReceiptPending = false;
        entity.receiptRetryCount = 0;
        entity.receiptSyncedAt = 0L;
        return entity;
    }

    private String pickUid(String primary, String secondary, String tertiary, Long senderId) {
        if (!safe(primary).isEmpty()) {
            return primary;
        }
        if (!safe(secondary).isEmpty()) {
            return secondary;
        }
        if (!safe(tertiary).isEmpty()) {
            return tertiary;
        }
        return senderId == null ? "" : String.valueOf(senderId);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static Handler createMainHandlerSafely() {
        try {
            Looper mainLooper = Looper.getMainLooper();
            return mainLooper == null ? null : new Handler(mainLooper);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}

package com.example.low_altitudereststop.feature.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.example.low_altitudereststop.feature.message.local.MessageDao;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MessageRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeMessageDao messageDao;
    private MessageRepository repository;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        Context context = new ContextWrapper(null) {
            @Override
            public Context getApplicationContext() {
                return this;
            }
        };
        messageDao = new FakeMessageDao();
        executorService = Executors.newSingleThreadExecutor();
        repository = new MessageRepository(context, messageDao, executorService);

        MessageEntity entity = new MessageEntity();
        entity.msgId = 1001L;
        entity.conversationId = 88L;
        entity.content = "待同步消息";
        entity.senderName = "测试用户";
        entity.createTime = "10:00";
        messageDao.upsertAll(Collections.singletonList(entity));
    }

    @After
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void updateReadStatusAfterLiveDataEmitsNewValue() throws Exception {
        repository.updateReadStatus(1001L, true);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        MessageEntity updated = getOrAwaitValue(repository.observeMessage(1001L));
        assertNotNull(updated);
        assertEquals(true, updated.isRead);
    }

    @Test
    public void deleteConversationRemovesAllMessagesInConversation() throws Exception {
        MessageEntity entity2 = new MessageEntity();
        entity2.msgId = 1002L;
        entity2.conversationId = 88L;
        entity2.content = "第二条消息";
        entity2.senderName = "测试用户";
        entity2.createTime = "10:01";
        messageDao.upsertAll(Collections.singletonList(entity2));

        MessageEntity otherEntity = new MessageEntity();
        otherEntity.msgId = 2001L;
        otherEntity.conversationId = 99L;
        otherEntity.content = "其他会话消息";
        otherEntity.senderName = "其他用户";
        otherEntity.createTime = "10:02";
        messageDao.upsertAll(Collections.singletonList(otherEntity));

        repository.deleteConversation(88L);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        List<MessageEntity> remaining = getOrAwaitValue(repository.observeAllMessages());
        assertNotNull(remaining);
        assertEquals(1, remaining.size());
        assertEquals(99L, remaining.get(0).conversationId);
    }

    @Test
    public void markConversationReadUpdatesAllUnreadMessages() throws Exception {
        MessageEntity unread1 = new MessageEntity();
        unread1.msgId = 1002L;
        unread1.conversationId = 88L;
        unread1.content = "未读消息1";
        unread1.mine = false;
        unread1.isRead = false;
        unread1.createTime = "10:01";
        messageDao.upsertAll(Collections.singletonList(unread1));

        repository.markConversationRead(88L);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        assertEquals(0, messageDao.countUnreadInConversation(88L));
    }

    private static <T> T getOrAwaitValue(LiveData<T> liveData) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] holder = new Object[1];
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                holder[0] = value;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        latch.await(2, TimeUnit.SECONDS);
        return (T) holder[0];
    }

    private static final class FakeMessageDao implements MessageDao {
        private final Map<Long, MessageEntity> store = new HashMap<>();
        private final MutableLiveData<List<MessageEntity>> allMessages = new MutableLiveData<>(Collections.emptyList());
        private final Map<Long, MutableLiveData<MessageEntity>> messageLiveData = new HashMap<>();

        @Override
        public void upsertAll(List<MessageEntity> messages) {
            for (MessageEntity message : messages) {
                store.put(message.msgId, copy(message));
            }
            publish();
        }

        @Override
        public LiveData<List<MessageEntity>> observeAllMessages() {
            return allMessages;
        }

        @Override
        public LiveData<List<MessageEntity>> observeConversation(long conversationId) {
            MutableLiveData<List<MessageEntity>> liveData = new MutableLiveData<>(Collections.emptyList());
            liveData.setValue(Collections.emptyList());
            return liveData;
        }

        @Override
        public LiveData<MessageEntity> observeMessage(long msgId) {
            MutableLiveData<MessageEntity> liveData = messageLiveData.get(msgId);
            if (liveData == null) {
                liveData = new MutableLiveData<>(copy(store.get(msgId)));
                messageLiveData.put(msgId, liveData);
            }
            return liveData;
        }

        @Override
        public List<MessageEntity> listConversationSync(long conversationId) {
            return Collections.emptyList();
        }

        @Override
        public MessageEntity findById(long msgId) {
            return copy(store.get(msgId));
        }

        @Override
        public void updateReadStatus(long msgId, boolean isRead) {
            MessageEntity entity = store.get(msgId);
            if (entity == null) {
                return;
            }
            entity.isRead = isRead;
            MutableLiveData<MessageEntity> liveData = messageLiveData.get(msgId);
            if (liveData != null) {
                liveData.postValue(copy(entity));
            }
            publish();
        }

        @Override
        public void markConversationAsRead(long conversationId) {
            for (MessageEntity entity : store.values()) {
                if (entity.conversationId == conversationId && !entity.mine) {
                    entity.isRead = true;
                }
            }
            publish();
        }

        @Override
        public List<Long> listPendingReceiptIds(int retryLimit) {
            return Collections.emptyList();
        }

        @Override
        public void markReceiptsSynced(List<Long> msgIds, long syncedAt) {
        }

        @Override
        public void increaseReceiptRetry(List<Long> msgIds) {
        }

        @Override
        public int countFailedReceiptSync(int retryLimit) {
            return 0;
        }

        @Override
        public List<Long> listFailedReceiptIds(int retryLimit) {
            return Collections.emptyList();
        }

        @Override
        public void clearAll() {
            store.clear();
            publish();
        }

        @Override
        public void deleteConversation(long conversationId) {
            store.entrySet().removeIf(entry -> entry.getValue().conversationId == conversationId);
            publish();
        }

        @Override
        public int countUnreadInConversation(long conversationId) {
            int count = 0;
            for (MessageEntity entity : store.values()) {
                if (entity.conversationId == conversationId && !entity.mine && !entity.isRead) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public List<Long> listConversationIds() {
            return store.values().stream()
                    .map(e -> e.conversationId)
                    .distinct()
                    .toList();
        }

        @Override
        public MessageEntity findLastMessageInConversation(long conversationId) {
            return store.values().stream()
                    .filter(e -> e.conversationId == conversationId)
                    .max((a, b) -> Long.compare(a.createTimeMillis, b.createTimeMillis))
                    .orElse(null);
        }

        private void publish() {
            allMessages.postValue(Collections.unmodifiableList(store.values().stream().map(FakeMessageDao::copy).toList()));
            for (Map.Entry<Long, MutableLiveData<MessageEntity>> entry : messageLiveData.entrySet()) {
                entry.getValue().postValue(copy(store.get(entry.getKey())));
            }
        }

        private static MessageEntity copy(MessageEntity source) {
            if (source == null) {
                return null;
            }
            MessageEntity entity = new MessageEntity();
            entity.msgId = source.msgId;
            entity.conversationId = source.conversationId;
            entity.content = source.content;
            entity.senderName = source.senderName;
            entity.senderRole = source.senderRole;
            entity.pilotUid = source.pilotUid;
            entity.enterpriseUid = source.enterpriseUid;
            entity.counterpartTitle = source.counterpartTitle;
            entity.createTime = source.createTime;
            entity.createTimeMillis = source.createTimeMillis;
            entity.mine = source.mine;
            entity.isRead = source.isRead;
            entity.readReceiptPending = source.readReceiptPending;
            entity.receiptRetryCount = source.receiptRetryCount;
            entity.receiptSyncedAt = source.receiptSyncedAt;
            return entity;
        }
    }
}

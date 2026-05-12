package com.example.low_altitudereststop.feature.message;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class MessageRealtimeHub {

    public interface Listener {
        void onReadStatusChanged(long msgId, boolean isRead);
    }

    public interface NewMessageListener {
        void onNewMessage(long conversationId, long msgId);
    }

    private static final MessageRealtimeHub INSTANCE = new MessageRealtimeHub();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Set<NewMessageListener> newMessageListeners = new CopyOnWriteArraySet<>();

    private MessageRealtimeHub() {
    }

    public static MessageRealtimeHub getInstance() {
        return INSTANCE;
    }

    public void connect(@NonNull Context context) {
        context.getApplicationContext();
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void addNewMessageListener(@NonNull NewMessageListener listener) {
        newMessageListeners.add(listener);
    }

    public void removeNewMessageListener(@NonNull NewMessageListener listener) {
        newMessageListeners.remove(listener);
    }

    public void dispatchReadStatusChanged(long msgId, boolean isRead) {
        for (Listener listener : listeners) {
            listener.onReadStatusChanged(msgId, isRead);
        }
    }

    public void dispatchNewMessage(long conversationId, long msgId) {
        for (NewMessageListener listener : newMessageListeners) {
            listener.onNewMessage(conversationId, msgId);
        }
    }

    @VisibleForTesting
    public static void emitReadStatusForTest(long msgId, boolean isRead) {
        INSTANCE.dispatchReadStatusChanged(msgId, isRead);
    }

    @VisibleForTesting
    public static void emitNewMessageForTest(long conversationId, long msgId) {
        INSTANCE.dispatchNewMessage(conversationId, msgId);
    }
}

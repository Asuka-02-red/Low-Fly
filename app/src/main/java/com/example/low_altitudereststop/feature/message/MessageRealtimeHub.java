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

    private static final MessageRealtimeHub INSTANCE = new MessageRealtimeHub();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

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

    public void dispatchReadStatusChanged(long msgId, boolean isRead) {
        for (Listener listener : listeners) {
            listener.onReadStatusChanged(msgId, isRead);
        }
    }

    @VisibleForTesting
    public static void emitReadStatusForTest(long msgId, boolean isRead) {
        INSTANCE.dispatchReadStatusChanged(msgId, isRead);
    }
}

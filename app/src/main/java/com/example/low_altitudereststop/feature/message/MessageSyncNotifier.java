package com.example.low_altitudereststop.feature.message;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 消息同步通知器，通过LiveData广播同步事件。
 * <p>
 * 提供全局的同步事件LiveData，当消息同步失败时发送通知，
 * 供UI层观察并展示同步状态提示。
 * </p>
 */
public final class MessageSyncNotifier {

    private static final MutableLiveData<String> EVENTS = new MutableLiveData<>();

    private MessageSyncNotifier() {
    }

    public static LiveData<String> events() {
        return EVENTS;
    }

    public static void notifySyncFailed() {
        EVENTS.postValue("同步失败");
    }
}

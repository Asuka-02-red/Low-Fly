package com.example.low_altitudereststop.feature.message;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

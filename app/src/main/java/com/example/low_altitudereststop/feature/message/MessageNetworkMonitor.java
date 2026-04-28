package com.example.low_altitudereststop.feature.message;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

public final class MessageNetworkMonitor {

    private static volatile boolean registered;

    private MessageNetworkMonitor() {
    }

    public static void register(Context context) {
        if (registered) {
            return;
        }
        synchronized (MessageNetworkMonitor.class) {
            if (registered) {
                return;
            }
            ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
            if (manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                registered = true;
                return;
            }
            manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    MessageReadReceiptWorker.enqueueNow(context.getApplicationContext());
                }
            });
            registered = true;
        }
    }
}

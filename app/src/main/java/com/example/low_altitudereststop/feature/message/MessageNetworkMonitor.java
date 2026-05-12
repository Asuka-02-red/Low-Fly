package com.example.low_altitudereststop.feature.message;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

/**
 * 消息网络状态监听器，在应用启动时注册全局网络回调。
 * <p>
 * 通过ConnectivityManager注册默认网络回调，监听网络可用性变化，
 * 供消息模块判断是否需要切换离线模式或触发数据同步。
 * </p>
 */
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

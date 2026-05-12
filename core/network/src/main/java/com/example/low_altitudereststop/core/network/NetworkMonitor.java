package com.example.low_altitudereststop.core.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 网络状态监控器，通过系统ConnectivityManager实时监听网络连接变化，
 * 以LiveData形式暴露在线状态供UI层观察。
 */
public final class NetworkMonitor {

    private static final MutableLiveData<Boolean> ONLINE = new MutableLiveData<>();
    private static volatile boolean registered = false;

    private NetworkMonitor() {
    }

    public static LiveData<Boolean> online() {
        return ONLINE;
    }

    public static boolean isOnline() {
        Boolean value = ONLINE.getValue();
        return value != null && value;
    }

    public static void register(@NonNull Context context) {
        if (registered) {
            return;
        }
        synchronized (NetworkMonitor.class) {
            if (registered) {
                return;
            }
            Context app = context.getApplicationContext();
            ConnectivityManager manager = app.getSystemService(ConnectivityManager.class);
            if (manager == null) {
                ONLINE.postValue(false);
                registered = true;
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        ONLINE.postValue(true);
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        ONLINE.postValue(false);
                    }
                });
                android.net.Network active = manager.getActiveNetwork();
                ONLINE.postValue(active != null
                        && manager.getNetworkCapabilities(active) != null);
            } else {
                ONLINE.postValue(checkLegacy(manager));
            }
            registered = true;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean checkLegacy(@NonNull ConnectivityManager manager) {
        android.net.NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}

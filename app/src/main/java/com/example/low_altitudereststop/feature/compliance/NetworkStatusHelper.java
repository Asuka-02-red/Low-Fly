package com.example.low_altitudereststop.feature.compliance;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * 网络状态检测工具类。
 * <p>
 * 通过ConnectivityManager判断当前设备是否具有可用的互联网连接，
 * 供合规管理等模块在界面上展示在线/离线状态。
 * </p>
 */
public final class NetworkStatusHelper {

    private NetworkStatusHelper() {
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}

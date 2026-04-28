package com.example.low_altitudereststop.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import com.example.low_altitudereststop.BuildConfig;
import com.example.low_altitudereststop.R;
import com.google.android.material.navigation.NavigationBarView;

public final class IconRegistry {

    private static final String TAG = "IconRegistry";

    private IconRegistry() {
    }

    public static boolean verifyCriticalIcons(@NonNull Context context) {
        int[][] criticalIcons = new int[][]{
                {R.drawable.ic_nav_home, R.drawable.ic_fallback_generic},
                {R.drawable.ic_nav_task, R.drawable.ic_fallback_generic},
                {R.drawable.ic_nav_message, R.drawable.ic_fallback_generic},
                {R.drawable.ic_nav_training, R.drawable.ic_fallback_generic},
                {R.drawable.ic_nav_compliance, R.drawable.ic_fallback_generic},
                {R.drawable.ic_nav_profile, R.drawable.ic_fallback_generic},
                {R.drawable.ic_quick_task, R.drawable.ic_fallback_generic},
                {R.drawable.ic_quick_compliance, R.drawable.ic_fallback_generic},
                {R.drawable.ic_quick_profile, R.drawable.ic_fallback_generic},
                {R.drawable.ic_stat_task, R.drawable.ic_fallback_generic},
                {R.drawable.ic_stat_alert, R.drawable.ic_fallback_alert}
        };
        boolean healthy = true;
        for (int[] iconPair : criticalIcons) {
            if (resolveDrawable(context, iconPair[0], iconPair[1]) == null) {
                healthy = false;
                debugLog("Missing critical icon: " + iconPair[0], null);
            }
        }
        return healthy;
    }

    public static void applyBottomNavIcons(@NonNull NavigationBarView navView) {
        Context context = navView.getContext();
        navView.getMenu().findItem(R.id.homeFragment)
                .setIcon(resolveDrawable(context, R.drawable.ic_nav_home, R.drawable.ic_fallback_generic));
        navView.getMenu().findItem(R.id.taskFragment)
                .setIcon(resolveDrawable(context, R.drawable.ic_nav_task, R.drawable.ic_fallback_generic));
        if (navView.getMenu().findItem(R.id.messageFragment) != null) {
            navView.getMenu().findItem(R.id.messageFragment)
                    .setIcon(resolveDrawable(context, R.drawable.ic_nav_message, R.drawable.ic_fallback_generic));
        }
        if (navView.getMenu().findItem(R.id.trainingFragment) != null) {
            navView.getMenu().findItem(R.id.trainingFragment)
                    .setIcon(resolveDrawable(context, R.drawable.ic_nav_training, R.drawable.ic_fallback_generic));
        }
        navView.getMenu().findItem(R.id.profileFragment)
                .setIcon(resolveDrawable(context, R.drawable.ic_nav_profile, R.drawable.ic_fallback_generic));
    }

    public static void applyIcon(@NonNull ImageView view, @DrawableRes int primaryIcon, @DrawableRes int fallbackIcon) {
        view.setImageDrawable(resolveDrawable(view.getContext(), primaryIcon, fallbackIcon));
    }

    public static Drawable resolveDrawable(@NonNull Context context, @DrawableRes int primaryIcon, @DrawableRes int fallbackIcon) {
        try {
            Drawable primary = AppCompatResources.getDrawable(context, primaryIcon);
            if (primary != null) {
                return primary;
            }
        } catch (Exception e) {
            debugLog("Primary icon load failed: " + primaryIcon, e);
        }
        try {
            return AppCompatResources.getDrawable(context, fallbackIcon);
        } catch (Exception e) {
            debugLog("Fallback icon load failed: " + fallbackIcon, e);
            return null;
        }
    }

    private static void debugLog(@NonNull String message, Throwable throwable) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (throwable == null) {
            android.util.Log.w(TAG, message);
        } else {
            android.util.Log.w(TAG, message, throwable);
        }
    }
}

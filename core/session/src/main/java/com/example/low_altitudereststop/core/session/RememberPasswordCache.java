package com.example.low_altitudereststop.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.CopyOnWriteArraySet;

public class RememberPasswordCache {

    private static final String PREF = "remember_password_cache";
    private static final String KEY_ENABLED = "remember_password_enabled";
    private static final String KEY_USERNAME = "remembered_username";
    private static final String KEY_PASSWORD = "remembered_password";
    private static final Object LOCK = new Object();
    private static final CopyOnWriteArraySet<Listener> LISTENERS = new CopyOnWriteArraySet<>();

    private static RememberedCredentials cachedSnapshot;

    private final SharedPreferences sp;

    public RememberPasswordCache(Context context) {
        Context appContext = context.getApplicationContext();
        this.sp = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public RememberedCredentials getSnapshot() {
        synchronized (LOCK) {
            if (cachedSnapshot == null) {
                cachedSnapshot = readFromDisk();
            }
            return cachedSnapshot;
        }
    }

    public Subscription subscribe(Listener listener) {
        LISTENERS.add(listener);
        listener.onChanged(getSnapshot());
        return () -> LISTENERS.remove(listener);
    }

    public void update(String username, String password, boolean rememberPassword) {
        RememberedCredentials next = rememberPassword
                ? new RememberedCredentials(true, username, password)
                : RememberedCredentials.empty();
        synchronized (LOCK) {
            cachedSnapshot = next;
            persist(next);
        }
        notifyListeners(next);
    }

    private RememberedCredentials readFromDisk() {
        boolean rememberPassword = sp.getBoolean(KEY_ENABLED, false);
        if (!rememberPassword) {
            return RememberedCredentials.empty();
        }
        return new RememberedCredentials(
                true,
                sp.getString(KEY_USERNAME, ""),
                sp.getString(KEY_PASSWORD, "")
        );
    }

    private void persist(RememberedCredentials credentials) {
        SharedPreferences.Editor editor = sp.edit();
        if (credentials.rememberPassword) {
            editor.putBoolean(KEY_ENABLED, true);
            editor.putString(KEY_USERNAME, credentials.username);
            editor.putString(KEY_PASSWORD, credentials.password);
        } else {
            editor.putBoolean(KEY_ENABLED, false);
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_PASSWORD);
        }
        editor.apply();
    }

    private void notifyListeners(RememberedCredentials credentials) {
        for (Listener listener : LISTENERS) {
            listener.onChanged(credentials);
        }
    }

    public interface Listener {
        void onChanged(RememberedCredentials credentials);
    }

    public interface Subscription {
        void dispose();
    }

    public static final class RememberedCredentials {
        public final boolean rememberPassword;
        public final String username;
        public final String password;

        public RememberedCredentials(boolean rememberPassword, String username, String password) {
            this.rememberPassword = rememberPassword;
            this.username = username == null ? "" : username;
            this.password = password == null ? "" : password;
        }

        public static RememberedCredentials empty() {
            return new RememberedCredentials(false, "", "");
        }
    }
}

package com.example.low_altitudereststop.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.example.low_altitudereststop.core.model.AuthModels;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SessionStore {

    private static final String PREF = "session_store";
    private static final String PREF_AUTO_LOGIN = "session_store_auto_login";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REAL_NAME = "real_name";
    private static final String KEY_COMPANY = "company_name";
    private static final String KEY_AUTO_ENABLED = "auto_login_enabled";
    private static final String KEY_AUTO_USERNAME = "auto_login_username";
    private static final String KEY_AUTO_REFRESH = "auto_login_refresh";
    private static final String KEY_AUTO_EXPIRES_AT = "auto_login_expires_at";
    private static final String KEY_ALIAS = "low_altitude_auto_login_key";
    private static final long AUTO_LOGIN_TTL_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    private final SharedPreferences sp;
    private final SharedPreferences autoLoginSp;

    public SessionStore(Context context) {
        Context appContext = context.getApplicationContext();
        this.sp = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        this.autoLoginSp = appContext.getSharedPreferences(PREF_AUTO_LOGIN, Context.MODE_PRIVATE);
    }

    public String getAccessToken() {
        return sp.getString(KEY_ACCESS, null);
    }

    public String getRefreshToken() {
        return sp.getString(KEY_REFRESH, null);
    }

    public boolean isLoggedIn() {
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty();
    }

    public boolean isDemoSession() {
        String token = getAccessToken();
        if (token != null && token.startsWith("mock_")) {
            return true;
        }
        String username = sp.getString(KEY_USERNAME, null);
        return username != null && username.contains("测试账号");
    }

    public void clearAuth() {
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(KEY_ACCESS);
        editor.remove(KEY_REFRESH);
        editor.remove(KEY_ROLE);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_REAL_NAME);
        editor.remove(KEY_COMPANY);
        editor.apply();
    }

    public void setAutoLoginEnabled(String username, String refreshToken, boolean enabled) {
        if (!enabled || refreshToken == null || refreshToken.trim().isEmpty()) {
            clearAutoLogin();
            return;
        }
        try {
            String encryptedRefresh = encrypt(refreshToken);
            autoLoginSp.edit()
                    .putBoolean(KEY_AUTO_ENABLED, true)
                    .putString(KEY_AUTO_USERNAME, username)
                    .putString(KEY_AUTO_REFRESH, encryptedRefresh)
                    .putLong(KEY_AUTO_EXPIRES_AT, System.currentTimeMillis() + AUTO_LOGIN_TTL_MILLIS)
                    .apply();
        } catch (GeneralSecurityException e) {
            clearAutoLogin();
        }
    }

    public AutoLoginSnapshot getAutoLoginSnapshot() {
        boolean enabled = autoLoginSp.getBoolean(KEY_AUTO_ENABLED, false);
        String encryptedRefresh = autoLoginSp.getString(KEY_AUTO_REFRESH, null);
        long expiresAt = autoLoginSp.getLong(KEY_AUTO_EXPIRES_AT, 0L);
        String username = autoLoginSp.getString(KEY_AUTO_USERNAME, "");
        boolean expired = expiresAt <= System.currentTimeMillis();
        if (!enabled || encryptedRefresh == null || encryptedRefresh.trim().isEmpty()) {
            return AutoLoginSnapshot.disabled();
        }
        if (expired) {
            return new AutoLoginSnapshot(false, true, username, null, expiresAt);
        }
        try {
            String refreshToken = decrypt(encryptedRefresh);
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return AutoLoginSnapshot.disabled();
            }
            return new AutoLoginSnapshot(true, false, username, refreshToken, expiresAt);
        } catch (GeneralSecurityException e) {
            clearAutoLogin();
            return AutoLoginSnapshot.disabled();
        }
    }

    public void clearAutoLogin() {
        autoLoginSp.edit()
                .remove(KEY_AUTO_ENABLED)
                .remove(KEY_AUTO_USERNAME)
                .remove(KEY_AUTO_REFRESH)
                .remove(KEY_AUTO_EXPIRES_AT)
                .apply();
    }

    public void clear() {
        sp.edit().clear().apply();
        clearAutoLogin();
    }

    public void saveAuth(AuthModels.AuthPayload payload) {
        if (payload == null) {
            return;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_ACCESS, payload.token);
        editor.putString(KEY_REFRESH, payload.refreshToken);
        if (payload.userInfo != null) {
            editor.putString(KEY_ROLE, payload.userInfo.role);
            editor.putString(KEY_USERNAME, payload.userInfo.username);
            editor.putString(KEY_REAL_NAME, payload.userInfo.realName);
            editor.putString(KEY_COMPANY, payload.userInfo.companyName);
        }
        editor.apply();
    }

    public AuthModels.SessionInfo getCachedUser() {
        AuthModels.SessionInfo info = new AuthModels.SessionInfo();
        info.username = sp.getString(KEY_USERNAME, null);
        info.role = sp.getString(KEY_ROLE, null);
        info.realName = sp.getString(KEY_REAL_NAME, null);
        info.companyName = sp.getString(KEY_COMPANY, null);
        return info;
    }

    private String encrypt(String plainText) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private String decrypt(String cipherText) throws GeneralSecurityException {
        byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);
        if (combined.length < 13) {
            throw new GeneralSecurityException("Cipher payload too short");
        }
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateSecretKey() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        try {
            keyStore.load(null);
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to load AndroidKeyStore", e);
        }
        SecretKey existing = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        if (existing != null) {
            return existing;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return keyGenerator.generateKey();
    }

    public static final class AutoLoginSnapshot {
        public final boolean enabled;
        public final boolean expired;
        public final String username;
        public final String refreshToken;
        public final long expiresAt;

        public AutoLoginSnapshot(boolean enabled, boolean expired, String username, String refreshToken, long expiresAt) {
            this.enabled = enabled;
            this.expired = expired;
            this.username = username == null ? "" : username;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
        }

        public static AutoLoginSnapshot disabled() {
            return new AutoLoginSnapshot(false, false, "", null, 0L);
        }
    }
}


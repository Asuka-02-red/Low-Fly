package com.example.low_altitudereststop.feature.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import androidx.annotation.NonNull;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.feature.message.local.EnterpriseProfileDao;
import com.example.low_altitudereststop.feature.message.local.EnterpriseProfileEntity;
import com.example.low_altitudereststop.feature.message.local.MessageLocalDatabase;
import com.example.low_altitudereststop.feature.message.local.PilotProfileDao;
import com.example.low_altitudereststop.feature.message.local.PilotProfileEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 消息身份解析仓库，负责异步解析消息发送者的显示名称。
 * <p>
 * 维护飞手和企业身份的内存缓存与本地数据库缓存，
 * 缓存未命中时通过API查询并持久化，支持可取消的异步回调机制，
 * 供消息列表适配器按角色解析对话方名称。
 * </p>
 */
public class MessageIdentityRepository {

    public interface NameCallback {
        void onResolved(String displayName);
    }

    public interface Cancelable {
        void cancel();
    }

    private static final long CACHE_WINDOW_MS = 5 * 60 * 1000L;
    private static volatile MessageIdentityRepository INSTANCE;

    private final Context appContext;
    private final PilotProfileDao pilotProfileDao;
    private final EnterpriseProfileDao enterpriseProfileDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, CacheItem> pilotCache = new LruCache<>(64);
    private final LruCache<String, CacheItem> enterpriseCache = new LruCache<>(64);
    private final Map<String, Long> pilotFetchAt = new HashMap<>();
    private final Map<String, Long> enterpriseFetchAt = new HashMap<>();
    private final Map<String, PendingPilotRequest> pendingPilotRequests = new HashMap<>();
    private final Map<String, PendingEnterpriseRequest> pendingEnterpriseRequests = new HashMap<>();

    public MessageIdentityRepository(@NonNull Context context) {
        MessageLocalDatabase db = MessageLocalDatabase.get(context);
        this.appContext = context.getApplicationContext();
        this.pilotProfileDao = db.pilotProfileDao();
        this.enterpriseProfileDao = db.enterpriseProfileDao();
    }

    public static MessageIdentityRepository get(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (MessageIdentityRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MessageIdentityRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public Cancelable resolvePilotName(@NonNull String uid, @NonNull NameCallback callback) {
        String normalizedUid = uid.trim();
        CacheItem cached = pilotCache.get(normalizedUid);
        if (cached != null && cached.isValid()) {
            callback.onResolved(cached.value);
            return () -> { };
        }
        ioExecutor.execute(() -> {
            PilotProfileEntity local = pilotProfileDao.findByUid(normalizedUid);
            if (local != null && !local.name.trim().isEmpty()) {
                CacheItem item = new CacheItem(local.name, System.currentTimeMillis());
                pilotCache.put(normalizedUid, item);
                mainHandler.post(() -> callback.onResolved(item.value));
                return;
            }
            fetchPilotName(normalizedUid, callback);
        });
        return () -> unregisterPilotCallback(normalizedUid, callback);
    }

    public Cancelable resolveEnterpriseName(@NonNull String uid, @NonNull NameCallback callback) {
        String normalizedUid = uid.trim();
        CacheItem cached = enterpriseCache.get(normalizedUid);
        if (cached != null && cached.isValid()) {
            callback.onResolved(cached.value);
            return () -> { };
        }
        ioExecutor.execute(() -> {
            EnterpriseProfileEntity local = enterpriseProfileDao.findByUid(normalizedUid);
            if (local != null && !local.companyName.trim().isEmpty()) {
                CacheItem item = new CacheItem(local.companyName, System.currentTimeMillis());
                enterpriseCache.put(normalizedUid, item);
                mainHandler.post(() -> callback.onResolved(item.value));
                return;
            }
            fetchEnterpriseName(normalizedUid, callback);
        });
        return () -> unregisterEnterpriseCallback(normalizedUid, callback);
    }

    private synchronized void fetchPilotName(String uid, NameCallback callback) {
        PendingPilotRequest pending = pendingPilotRequests.get(uid);
        if (pending != null) {
            pending.callbacks.add(callback);
            return;
        }
        long now = System.currentTimeMillis();
        Long lastFetchAt = pilotFetchAt.get(uid);
        if (lastFetchAt != null && now - lastFetchAt < CACHE_WINDOW_MS) {
            mainHandler.post(() -> callback.onResolved(fallbackPilot(uid)));
            return;
        }
        pilotFetchAt.put(uid, now);
        Call<ApiEnvelope<PlatformModels.PilotProfileView>> call = ApiClient.getAuthedService(appContext).getPilotProfile(uid);
        PendingPilotRequest request = new PendingPilotRequest(call);
        request.callbacks.add(callback);
        pendingPilotRequests.put(uid, request);
        call.enqueue(new Callback<ApiEnvelope<PlatformModels.PilotProfileView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.PilotProfileView>> call, Response<ApiEnvelope<PlatformModels.PilotProfileView>> response) {
                String resolved = fallbackPilot(uid);
                ApiEnvelope<PlatformModels.PilotProfileView> body = response.body();
                if (response.isSuccessful()
                        && body != null
                        && body.data != null
                        && body.data.name != null
                        && !body.data.name.trim().isEmpty()) {
                    resolved = body.data.name.trim();
                    PilotProfileEntity entity = new PilotProfileEntity();
                    entity.uid = uid;
                    entity.name = resolved;
                    entity.updateTimeMillis = System.currentTimeMillis();
                    ioExecutor.execute(() -> pilotProfileDao.upsert(entity));
                    pilotCache.put(uid, new CacheItem(resolved, System.currentTimeMillis()));
                }
                dispatchPilot(uid, resolved);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.PilotProfileView>> call, Throwable t) {
                dispatchPilot(uid, fallbackPilot(uid));
            }
        });
    }

    private synchronized void fetchEnterpriseName(String uid, NameCallback callback) {
        PendingEnterpriseRequest pending = pendingEnterpriseRequests.get(uid);
        if (pending != null) {
            pending.callbacks.add(callback);
            return;
        }
        long now = System.currentTimeMillis();
        Long lastFetchAt = enterpriseFetchAt.get(uid);
        if (lastFetchAt != null && now - lastFetchAt < CACHE_WINDOW_MS) {
            mainHandler.post(() -> callback.onResolved(fallbackEnterprise(uid)));
            return;
        }
        enterpriseFetchAt.put(uid, now);
        Call<ApiEnvelope<PlatformModels.EnterpriseInfoView>> call = ApiClient.getAuthedService(appContext).getEnterpriseInfo(uid);
        PendingEnterpriseRequest request = new PendingEnterpriseRequest(call);
        request.callbacks.add(callback);
        pendingEnterpriseRequests.put(uid, request);
        call.enqueue(new Callback<ApiEnvelope<PlatformModels.EnterpriseInfoView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.EnterpriseInfoView>> call, Response<ApiEnvelope<PlatformModels.EnterpriseInfoView>> response) {
                String resolved = fallbackEnterprise(uid);
                ApiEnvelope<PlatformModels.EnterpriseInfoView> body = response.body();
                if (response.isSuccessful()
                        && body != null
                        && body.data != null
                        && body.data.companyName != null
                        && !body.data.companyName.trim().isEmpty()) {
                    resolved = body.data.companyName.trim();
                    EnterpriseProfileEntity entity = new EnterpriseProfileEntity();
                    entity.uid = uid;
                    entity.companyName = resolved;
                    entity.updateTimeMillis = System.currentTimeMillis();
                    ioExecutor.execute(() -> enterpriseProfileDao.upsert(entity));
                    enterpriseCache.put(uid, new CacheItem(resolved, System.currentTimeMillis()));
                }
                dispatchEnterprise(uid, resolved);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.EnterpriseInfoView>> call, Throwable t) {
                dispatchEnterprise(uid, fallbackEnterprise(uid));
            }
        });
    }

    private synchronized void dispatchPilot(String uid, String displayName) {
        PendingPilotRequest pending = pendingPilotRequests.remove(uid);
        if (pending == null) {
            return;
        }
        for (NameCallback callback : new ArrayList<>(pending.callbacks)) {
            mainHandler.post(() -> callback.onResolved(displayName));
        }
    }

    private synchronized void dispatchEnterprise(String uid, String displayName) {
        PendingEnterpriseRequest pending = pendingEnterpriseRequests.remove(uid);
        if (pending == null) {
            return;
        }
        for (NameCallback callback : new ArrayList<>(pending.callbacks)) {
            mainHandler.post(() -> callback.onResolved(displayName));
        }
    }

    private synchronized void unregisterPilotCallback(String uid, NameCallback callback) {
        PendingPilotRequest pending = pendingPilotRequests.get(uid);
        if (pending == null) {
            return;
        }
        pending.callbacks.remove(callback);
        if (pending.callbacks.isEmpty()) {
            pending.call.cancel();
            pendingPilotRequests.remove(uid);
        }
    }

    private synchronized void unregisterEnterpriseCallback(String uid, NameCallback callback) {
        PendingEnterpriseRequest pending = pendingEnterpriseRequests.get(uid);
        if (pending == null) {
            return;
        }
        pending.callbacks.remove(callback);
        if (pending.callbacks.isEmpty()) {
            pending.call.cancel();
            pendingEnterpriseRequests.remove(uid);
        }
    }

    private String fallbackPilot(String uid) {
        return "飞手#" + tail(uid);
    }

    private String fallbackEnterprise(String uid) {
        return "企业#" + tail(uid);
    }

    private String tail(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return "----";
        }
        String text = uid.trim();
        return text.length() <= 4 ? text : text.substring(text.length() - 4);
    }

    private static final class CacheItem {
        final String value;
        final long fetchAt;

        CacheItem(String value, long fetchAt) {
            this.value = value;
            this.fetchAt = fetchAt;
        }

        boolean isValid() {
            return System.currentTimeMillis() - fetchAt < CACHE_WINDOW_MS && value != null && !value.trim().isEmpty();
        }
    }

    private static final class PendingPilotRequest {
        final Call<ApiEnvelope<PlatformModels.PilotProfileView>> call;
        final List<NameCallback> callbacks = new ArrayList<>();

        PendingPilotRequest(Call<ApiEnvelope<PlatformModels.PilotProfileView>> call) {
            this.call = call;
        }
    }

    private static final class PendingEnterpriseRequest {
        final Call<ApiEnvelope<PlatformModels.EnterpriseInfoView>> call;
        final List<NameCallback> callbacks = new ArrayList<>();

        PendingEnterpriseRequest(Call<ApiEnvelope<PlatformModels.EnterpriseInfoView>> call) {
            this.call = call;
        }
    }
}

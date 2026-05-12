package com.example.low_altitudereststop.core.network;

import android.content.Context;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * Token认证器，当访问令牌过期导致请求返回401时，
 * 自动使用刷新令牌获取新的访问令牌并重发原始请求。
 */
public class TokenAuthenticator implements Authenticator {

    private final Context appContext;
    private final SessionStore sessionStore;

    public TokenAuthenticator(Context context, SessionStore sessionStore) {
        this.appContext = context.getApplicationContext();
        this.sessionStore = sessionStore;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= 2) {
            return null;
        }
        String refreshToken = sessionStore.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty() || sessionStore.isRefreshTokenExpired()) {
            return null;
        }
        if (response.request().url().encodedPath().endsWith("/auth/refresh")) {
            return null;
        }

        synchronized (this) {
            String currentToken = sessionStore.getAccessToken();
            if (currentToken != null && !currentToken.trim().isEmpty()) {
                String requestHeader = response.request().header("Authorization");
                if (requestHeader == null || !requestHeader.endsWith(currentToken)) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + currentToken)
                            .build();
                }
            }

            ApiService api = ApiClient.getPublicService(appContext);
            AuthModels.RefreshTokenRequest req = new AuthModels.RefreshTokenRequest();
            req.refreshToken = refreshToken;
            Call<ApiEnvelope<AuthModels.AuthPayload>> call = api.refresh(req);
            retrofit2.Response<ApiEnvelope<AuthModels.AuthPayload>> refreshResp = call.execute();
            if (!refreshResp.isSuccessful() || refreshResp.body() == null || refreshResp.body().data == null) {
                if (refreshResp.code() == 401) {
                    sessionStore.clear();
                }
                return null;
            }
            if (refreshResp.body().code == 401) {
                sessionStore.clear();
                return null;
            }
            sessionStore.saveAuth(refreshResp.body().data);
            String newToken = sessionStore.getAccessToken();
            if (newToken == null || newToken.trim().isEmpty()) {
                return null;
            }
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newToken)
                    .build();
        }
    }

    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}


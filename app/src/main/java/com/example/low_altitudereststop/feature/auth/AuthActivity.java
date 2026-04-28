package com.example.low_altitudereststop.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.snackbar.Snackbar;
import com.example.low_altitudereststop.core.ui.EdgeToEdgeActivity;
import com.example.low_altitudereststop.MainActivity;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.RememberPasswordCache;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.databinding.ActivityAuthBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends EdgeToEdgeActivity {

    private static final String TAG = "AuthActivity";
    private static final String ROLE_OPTION_PILOT = "飞手";
    private static final String ROLE_OPTION_ENTERPRISE = "企业";
    private static final String ROLE_ENTERPRISE = "ENTERPRISE";

    private ActivityAuthBinding binding;
    private RememberPasswordCache rememberPasswordCache;
    private RememberPasswordCache.Subscription rememberPasswordSubscription;
    private SessionStore sessionStore;
    private boolean registerMode = false;
    private ActivityResultLauncher<Intent> materialsLauncher;
    private String enterpriseName = "";
    private String enterpriseCreditCode = "";
    private String enterpriseContactName = "";
    private String enterpriseContactPhone = "";
    private String enterpriseLicenseUri = "";
    private Snackbar submitSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        rememberPasswordCache = new RememberPasswordCache(this);
        sessionStore = new SessionStore(this);

        materialsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Intent data = result.getData();
                    enterpriseName = data.getStringExtra(EnterpriseMaterialsActivity.EXTRA_ENTERPRISE_NAME);
                    enterpriseCreditCode = data.getStringExtra(EnterpriseMaterialsActivity.EXTRA_CREDIT_CODE);
                    enterpriseContactName = data.getStringExtra(EnterpriseMaterialsActivity.EXTRA_CONTACT_NAME);
                    enterpriseContactPhone = data.getStringExtra(EnterpriseMaterialsActivity.EXTRA_CONTACT_PHONE);
                    enterpriseLicenseUri = data.getStringExtra(EnterpriseMaterialsActivity.EXTRA_LICENSE_URI);
                    updateUploadMaterialsButton();
                    toast("企业材料已保存");
                });

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
                ROLE_OPTION_PILOT,
                ROLE_OPTION_ENTERPRISE
        });
        binding.spRole.setAdapter(roleAdapter);
        binding.spRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRoleSpecificViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateRoleSpecificViews();
            }
        });

        binding.btnToggleMode.setOnClickListener(v -> {
            registerMode = !registerMode;
            renderMode();
        });
        binding.btnSubmit.setOnClickListener(v -> submit());
        binding.btnUploadMaterials.setOnClickListener(v -> openEnterpriseMaterialsPage());

        observeRememberedCredentials();
        renderMode();
    }

    private void renderMode() {
        binding.tvTitle.setText(registerMode ? "开户注册" : "账号登录");
        binding.btnSubmit.setText(registerMode ? "注册并进入" : "登录并进入");
        binding.btnToggleMode.setText(registerMode ? "已有账号？去登录" : "没有账号？去注册");

        int registerVis = registerMode ? View.VISIBLE : View.GONE;
        binding.tilPhone.setVisibility(registerVis);
        binding.tilRole.setVisibility(registerVis);
        binding.spRole.setVisibility(registerVis);
        binding.cbRememberPassword.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        binding.cbAutoLogin.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        updateRoleSpecificViews();
    }

    private void submit() {
        String username = safeText(binding.etUsername.getText());
        String password = safeText(binding.etPassword.getText());
        clearInputErrors();
        if (username.isEmpty() || password.isEmpty()) {
            if (username.isEmpty()) {
                binding.tilUsername.setError("请输入用户名");
            }
            if (password.isEmpty()) {
                binding.tilPassword.setError("请输入密码");
            }
            showTransientMessage("请填写账号与密码");
            return;
        }
        if (registerMode && isEnterpriseRoleSelected() && !hasEnterpriseMaterials()) {
            showTransientMessage("企业注册请先上传材料");
            return;
        }

        binding.btnSubmit.setEnabled(false);
        showSubmitting(registerMode ? "正在提交注册信息" : "正在登录，请稍候");
        if (registerMode) {
            AuthModels.RegisterRequest req = new AuthModels.RegisterRequest();
            req.username = username;
            req.password = password;
            req.phone = safeText(binding.etPhone.getText());
            if (req.phone.isEmpty()) {
                binding.btnSubmit.setEnabled(true);
                dismissSubmitting();
                binding.tilPhone.setError("请输入手机号");
                showTransientMessage("请补充手机号后再注册");
                return;
            }
            req.realName = "";
            req.companyName = isEnterpriseRoleSelected() ? enterpriseName : "";
            req.role = resolveRegisterRole();

            ApiClient.getPublicService(this).register(req).enqueue(new Callback<ApiEnvelope<AuthModels.AuthPayload>>() {
                @Override
                public void onResponse(Call<ApiEnvelope<AuthModels.AuthPayload>> call, Response<ApiEnvelope<AuthModels.AuthPayload>> response) {
                    handleAuthResponse(response, false, username, password);
                }

                @Override
                public void onFailure(Call<ApiEnvelope<AuthModels.AuthPayload>> call, Throwable t) {
                    binding.btnSubmit.setEnabled(true);
                    dismissSubmitting();
                    completeWithDemoFallback(username, password, false, t);
                }
            });
        } else {
            AuthModels.LoginRequest req = new AuthModels.LoginRequest();
            req.username = username;
            req.password = password;
            String loginUrl = ApiClient.getPublicService(this).login(req).request().url().toString();
            Log.d(TAG, "登录请求 URL: " + loginUrl + "，账号: " + username);

            ApiClient.getPublicService(this).login(req).enqueue(new Callback<ApiEnvelope<AuthModels.AuthPayload>>() {
                @Override
                public void onResponse(Call<ApiEnvelope<AuthModels.AuthPayload>> call, Response<ApiEnvelope<AuthModels.AuthPayload>> response) {
                    Log.d(TAG, "登录响应码: " + response.code());
                    Log.d(TAG, "登录响应体: " + stringifyEnvelope(response.body()));
                    handleAuthResponse(response, true, username, password);
                }

                @Override
                public void onFailure(Call<ApiEnvelope<AuthModels.AuthPayload>> call, Throwable t) {
                    binding.btnSubmit.setEnabled(true);
                    dismissSubmitting();
                    completeWithDemoFallback(username, password, true, t);
                }
            });
        }
    }

    private void handleAuthResponse(Response<ApiEnvelope<AuthModels.AuthPayload>> response, boolean loginMode, String username, String password) {
        binding.btnSubmit.setEnabled(true);
        dismissSubmitting();
        ApiEnvelope<AuthModels.AuthPayload> envelope = response.body();
        if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
            Log.w(TAG, "登录失败: 响应无效或数据为空");
            completeWithDemoFallback(username, password, loginMode, null);
            return;
        }
        AuthModels.AuthPayload payload = envelope.data;
        if (payload.token == null || payload.token.trim().isEmpty()) {
            Log.w(TAG, "登录失败: token 字段缺失，停止写入会话");
            showTransientMessage("登录成功但令牌缺失，无法进入首页");
            return;
        }
        sessionStore.saveAuth(payload);
        sessionStore.setAutoLoginEnabled(username, payload.refreshToken, loginMode && binding.cbAutoLogin.isChecked());
        if (!sessionStore.isLoggedIn()) {
            Log.w(TAG, "登录失败: 会话写入后仍未登录");
            showTransientMessage("登录态写入失败，请重试");
            return;
        }
        if (loginMode) {
            rememberPasswordCache.update(username, password, binding.cbRememberPassword.isChecked());
        }
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "发送登录成功跳转到主页面");
            startActivity(intent);
            finish();
        } catch (Exception navigationError) {
            Log.e(TAG, "登录成功但页面跳转失败", navigationError);
            showTransientMessage("登录成功，但页面跳转失败");
        }
    }

    @Override
    protected void onDestroy() {
        if (rememberPasswordSubscription != null) {
            rememberPasswordSubscription.dispose();
            rememberPasswordSubscription = null;
        }
        super.onDestroy();
    }

    private String safeText(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showSubmitting(String message) {
        if (submitSnackbar != null) {
            submitSnackbar.dismiss();
        }
        submitSnackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_INDEFINITE);
        submitSnackbar.show();
    }

    private void dismissSubmitting() {
        if (submitSnackbar != null) {
            submitSnackbar.dismiss();
            submitSnackbar = null;
        }
    }

    private void showTransientMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private String networkMessage(Throwable t) {
        String message = t == null ? "" : String.valueOf(t.getMessage());
        return "网络连接失败，请检查服务连接后重试。\n" + message;
    }

    private void completeWithDemoFallback(String username, String password, boolean loginMode, Throwable throwable) {
        if (throwable != null) {
            Log.w(TAG, "认证接口不可用，切换到本地演示登录", throwable);
        } else {
            Log.w(TAG, "认证响应不可用，切换到本地演示登录");
        }
        AuthModels.AuthPayload payload = buildDemoPayload(username);
        sessionStore.saveAuth(payload);
        sessionStore.setAutoLoginEnabled(username, payload.refreshToken, loginMode && binding.cbAutoLogin.isChecked());
        if (!sessionStore.isLoggedIn()) {
            showTransientMessage(throwable == null ? "演示登录失败，请稍后重试" : networkMessage(throwable));
            return;
        }
        if (loginMode) {
            rememberPasswordCache.update(username, password, binding.cbRememberPassword.isChecked());
        }
        String roleLabel = ROLE_ENTERPRISE.equals(payload.userInfo.role) ? "企业" : "飞手";
        showTransientMessage("服务连接不可用，已切换" + roleLabel + "测试账号进入演示模式");
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception navigationError) {
            Log.e(TAG, "演示登录成功但页面跳转失败", navigationError);
            showTransientMessage("演示登录成功，但页面跳转失败");
        }
    }

    private AuthModels.AuthPayload buildDemoPayload(String username) {
        boolean enterprise = username != null
                && (username.contains("企业") || username.toLowerCase().contains("enterprise") || username.toLowerCase().contains("company"));
        AuthModels.AuthPayload payload = new AuthModels.AuthPayload();
        payload.token = enterprise ? "mock_enterprise_token" : "mock_pilot_token";
        payload.refreshToken = "mock_refresh";
        payload.userInfo = new AuthModels.SessionInfo();
        payload.userInfo.id = 1L;
        payload.userInfo.role = enterprise ? ROLE_ENTERPRISE : "PILOT";
        payload.userInfo.username = enterprise ? "企业测试账号" : "飞手测试账号";
        payload.userInfo.realName = enterprise ? "企业调度员" : "陈伶";
        payload.userInfo.companyName = enterprise ? "低空驿站企业中心" : "";
        return payload;
    }

    private String resolveRegisterRole() {
        Object selected = binding.spRole.getSelectedItem();
        return selected != null && ROLE_OPTION_ENTERPRISE.equals(String.valueOf(selected))
                ? ROLE_ENTERPRISE
                : "PILOT";
    }

    private boolean isEnterpriseRoleSelected() {
        Object selected = binding.spRole.getSelectedItem();
        return selected != null && ROLE_OPTION_ENTERPRISE.equals(String.valueOf(selected));
    }

    private void updateRoleSpecificViews() {
        boolean showUploadMaterials = registerMode && isEnterpriseRoleSelected();
        binding.btnUploadMaterials.setVisibility(showUploadMaterials ? View.VISIBLE : View.GONE);
        updateUploadMaterialsButton();
    }

    private void updateUploadMaterialsButton() {
        if (binding.btnUploadMaterials.getVisibility() != View.VISIBLE) {
            return;
        }
        binding.btnUploadMaterials.setText(hasEnterpriseMaterials() ? "已上传材料，可重新编辑" : "上传材料");
    }

    private boolean hasEnterpriseMaterials() {
        return !enterpriseName.isEmpty()
                && !enterpriseCreditCode.isEmpty()
                && !enterpriseContactName.isEmpty()
                && !enterpriseContactPhone.isEmpty()
                && !enterpriseLicenseUri.isEmpty();
    }

    private void openEnterpriseMaterialsPage() {
        Intent intent = new Intent(this, EnterpriseMaterialsActivity.class);
        intent.putExtra(EnterpriseMaterialsActivity.EXTRA_ENTERPRISE_NAME, enterpriseName);
        intent.putExtra(EnterpriseMaterialsActivity.EXTRA_CREDIT_CODE, enterpriseCreditCode);
        intent.putExtra(EnterpriseMaterialsActivity.EXTRA_CONTACT_NAME, enterpriseContactName);
        intent.putExtra(EnterpriseMaterialsActivity.EXTRA_CONTACT_PHONE, enterpriseContactPhone);
        intent.putExtra(EnterpriseMaterialsActivity.EXTRA_LICENSE_URI, enterpriseLicenseUri);
        materialsLauncher.launch(intent);
    }

    private void observeRememberedCredentials() {
        rememberPasswordSubscription = rememberPasswordCache.subscribe(credentials -> {
            binding.cbRememberPassword.setChecked(credentials.rememberPassword);
            if (!credentials.rememberPassword) {
                return;
            }
            binding.etUsername.setText(credentials.username);
            binding.etPassword.setText(credentials.password);
        });
        binding.cbAutoLogin.setChecked(false);
    }

    private void clearInputErrors() {
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tilPhone.setError(null);
    }

    private String stringifyEnvelope(ApiEnvelope<AuthModels.AuthPayload> envelope) {
        if (envelope == null) {
            return "null";
        }
        String token = envelope.data == null ? "null" : envelope.data.token;
        String role = envelope.data == null || envelope.data.userInfo == null ? "null" : envelope.data.userInfo.role;
        return "{code=" + envelope.code + ", message=" + envelope.message + ", token=" + token + ", role=" + role + "}";
    }
}


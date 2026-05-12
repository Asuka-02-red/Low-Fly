package com.example.low_altitudereststop.core.ui;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;

/**
 * 可导航的边到边Activity，在EdgeToEdgeActivity基础上增加返回按钮绑定
 * 和系统返回手势支持，提供统一的页面导航行为。
 */
public abstract class NavigableEdgeToEdgeActivity extends EdgeToEdgeActivity {

    @Override
    protected void onStart() {
        super.onStart();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAfterTransition();
            }
        });
    }

    protected void bindBackButton(@NonNull MaterialButton backButton) {
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}

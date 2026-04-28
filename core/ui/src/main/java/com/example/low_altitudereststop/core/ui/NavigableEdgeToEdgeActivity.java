package com.example.low_altitudereststop.core.ui;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;

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

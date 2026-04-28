package com.example.low_altitudereststop.core.ui;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;

public class PageStateController {

    private final View stateRoot;
    private final View contentView;
    private final ProgressBar progressBar;
    private final TextView titleView;
    private final TextView descView;
    private final MaterialButton retryButton;

    public PageStateController(
            @NonNull View stateRoot,
            @NonNull View contentView,
            @NonNull ProgressBar progressBar,
            @NonNull TextView titleView,
            @NonNull TextView descView,
            @NonNull MaterialButton retryButton
    ) {
        this.stateRoot = stateRoot;
        this.contentView = contentView;
        this.progressBar = progressBar;
        this.titleView = titleView;
        this.descView = descView;
        this.retryButton = retryButton;
    }

    public void showLoading(@NonNull String title, @NonNull String desc) {
        bind(title, desc, null, null, true);
    }

    public void showEmpty(@NonNull String title, @NonNull String desc, @NonNull String actionText, @NonNull Runnable action) {
        bind(title, desc, actionText, action, false);
    }

    public void showError(@NonNull String title, @NonNull String desc, @NonNull String actionText, @NonNull Runnable action) {
        bind(title, desc, actionText, action, false);
    }

    public void showSubmit(@NonNull String title, @NonNull String desc) {
        bind(title, desc, null, null, true);
    }

    public void showContent() {
        stateRoot.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    private void bind(String title, String desc, String actionText, Runnable action, boolean loading) {
        titleView.setText(title);
        descView.setText(desc);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (actionText != null && action != null) {
            retryButton.setText(actionText);
            retryButton.setOnClickListener(v -> action.run());
            retryButton.setVisibility(View.VISIBLE);
        } else {
            retryButton.setOnClickListener(null);
            retryButton.setVisibility(View.GONE);
        }
        stateRoot.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }
}

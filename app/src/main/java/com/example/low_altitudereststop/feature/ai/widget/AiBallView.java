package com.example.low_altitudereststop.feature.ai.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AiBallView extends FrameLayout {

    public interface Listener {
        void onBubbleClicked();
        void onVoiceClicked();
        void onSendClicked(@NonNull String text);
        void onOutsideTouched();
    }

    private View panelView;
    private View bubbleView;
    private TextView stateView;
    private TextView transcriptView;
    private TextView resultView;
    private ScrollView resultScrollView;
    private View inputContainerView;
    private TextInputEditText inputView;
    private Listener listener;

    public AiBallView(@NonNull Context context) {
        this(context, null);
    }

    public AiBallView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_ai_ball, this, true);
        panelView = findViewById(R.id.layout_ai_ball_panel);
        bubbleView = findViewById(R.id.layout_ai_ball_bubble);
        stateView = findViewById(R.id.tv_ai_ball_state);
        transcriptView = findViewById(R.id.tv_ai_ball_transcript);
        resultView = findViewById(R.id.tv_ai_ball_result);
        resultScrollView = findViewById(R.id.scroll_ai_ball_result);
        inputContainerView = findViewById(R.id.layout_ai_ball_input_container);
        inputView = findViewById(R.id.et_ai_ball_input);
        MaterialButton voiceButton = findViewById(R.id.btn_ai_ball_voice);
        MaterialButton sendButton = findViewById(R.id.btn_ai_ball_send);
        bubbleView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBubbleClicked();
            }
        });
        voiceButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoiceClicked();
            }
        });
        sendButton.setOnClickListener(v -> {
            if (listener == null) {
                return;
            }
            String text = inputView.getText() == null ? "" : inputView.getText().toString().trim();
            if (text.isEmpty()) {
                inputView.requestFocus();
                return;
            }
            listener.onSendClicked(text);
            inputView.setText(null);
        });
        showCollapsed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE && listener != null) {
            listener.onOutsideTouched();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void showCollapsed() {
        panelView.setVisibility(View.GONE);
        hideKeyboard();
    }

    public void showExpanded() {
        panelView.setVisibility(View.VISIBLE);
    }

    public void showVoiceInput() {
        showVoiceInput(false);
    }

    public void showVoiceInput(boolean keepKeyboard) {
        showExpanded();
        inputContainerView.setVisibility(View.GONE);
        clearInputFocus(keepKeyboard);
    }

    public void showTextInput() {
        showTextInput(true);
    }

    public void showTextInput(boolean requestKeyboard) {
        showExpanded();
        inputContainerView.setVisibility(View.VISIBLE);
        if (!requestKeyboard) {
            clearInputFocus(false);
            return;
        }
        inputView.post(() -> {
            inputView.requestFocus();
            InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
            if (imm != null) {
                imm.showSoftInput(inputView, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    public void render(@NonNull String mode, @Nullable String transcript, @Nullable String result) {
        if (AiBallStateMachine.MODE_IDLE.equals(mode)) {
            stateView.setText(R.string.ai_ball_idle_hint);
        } else if (AiBallStateMachine.MODE_ACTIVE.equals(mode)) {
            stateView.setText(R.string.ai_ball_active);
        } else if (AiBallStateMachine.MODE_LISTENING.equals(mode)) {
            stateView.setText(R.string.ai_ball_listening);
        } else if (AiBallStateMachine.MODE_THINKING.equals(mode)) {
            stateView.setText(R.string.ai_ball_thinking);
        } else if (AiBallStateMachine.MODE_ERROR.equals(mode)) {
            stateView.setText(R.string.ai_ball_error);
        } else {
            stateView.setText(R.string.ai_ball_idle_hint);
        }
        transcriptView.setVisibility(TextUtils.isEmpty(transcript) ? View.GONE : View.VISIBLE);
        transcriptView.setText(transcript);
        resultView.setText(TextUtils.isEmpty(result) ? getResources().getString(R.string.ai_ball_idle_hint) : result);
        if (resultScrollView != null) {
            resultScrollView.post(() -> resultScrollView.fullScroll(View.FOCUS_UP));
        }
    }

    public void prepareForMeasurement(boolean expanded, boolean textInputMode) {
        if (!expanded) {
            showCollapsed();
            return;
        }
        if (textInputMode) {
            showTextInput(false);
            return;
        }
        showVoiceInput(true);
    }

    private void clearInputFocus(boolean keepKeyboard) {
        inputView.clearFocus();
        if (!keepKeyboard) {
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @NonNull
    public View getBubbleView() {
        return bubbleView;
    }
}

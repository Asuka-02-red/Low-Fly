package com.example.low_altitudereststop.feature.ai.widget;

import androidx.annotation.NonNull;

public final class AiBallStateMachine {

    public static final String MODE_IDLE = "idle";
    public static final String MODE_ACTIVE = "active";
    public static final String MODE_LISTENING = "listening";
    public static final String MODE_THINKING = "thinking";
    public static final String MODE_SPEAKING = "speaking";
    public static final String MODE_ERROR = "error";

    private String currentMode = MODE_IDLE;
    private boolean expanded;

    @NonNull
    public String currentMode() {
        return currentMode;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @NonNull
    public String update(@NonNull String nextMode) {
        currentMode = nextMode;
        return currentMode;
    }
}

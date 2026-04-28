package com.example.low_altitudereststop.ai;

import com.example.low_altitudereststop.ai.IAiBallCallback;

interface IAiBallService {
    void bindSession(String clientId, IAiBallCallback callback);
    void unbindSession(String clientId, IAiBallCallback callback);
    void showBall();
    void hideBall();
    void startVoiceWakeup();
    void stopVoiceWakeup();
    void submitText(String query);
    void cancelCurrentTask();
    void updateUiMode(String mode);
    String getLastResult();
}

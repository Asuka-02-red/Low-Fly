package com.example.low_altitudereststop.ai;

interface IAiBallCallback {
    void onStateChanged(String mode, String transcript, String response);
    void onError(String code, String message);
}

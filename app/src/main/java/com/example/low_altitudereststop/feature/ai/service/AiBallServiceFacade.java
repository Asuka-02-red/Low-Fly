package com.example.low_altitudereststop.feature.ai.service;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.low_altitudereststop.ai.IAiBallCallback;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.ai.network.AiLlmRepository;
import com.example.low_altitudereststop.feature.ai.widget.AiBallStateMachine;
import java.util.ArrayList;

public final class AiBallServiceFacade {

    private static final String TAG = "AiBallServiceFacade";

    public interface Listener {
        void onStateChanged(@NonNull String mode, String transcript, String response);
        void onVisibilityChanged(boolean visible);
    }

    private static volatile AiBallServiceFacade instance;

    private final RemoteCallbackList<IAiBallCallback> callbacks = new RemoteCallbackList<>();
    private final ArrayList<Listener> listeners = new ArrayList<>();
    private final AiBallStateMachine stateMachine = new AiBallStateMachine();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean visible = true;
    private Context appContext;
    private SpeechRecognizer speechRecognizer;
    private String lastTranscript = "";
    private String lastResult = "";
    private boolean textInputPreferred;

    private AiBallServiceFacade() {
    }

    @NonNull
    public static AiBallServiceFacade getInstance() {
        if (instance == null) {
            synchronized (AiBallServiceFacade.class) {
                if (instance == null) {
                    instance = new AiBallServiceFacade();
                }
            }
        }
        return instance;
    }

    public void attachContext(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public void bindSession(@Nullable IAiBallCallback callback) {
        if (callback == null) {
            return;
        }
        callbacks.register(callback);
        notifySingle(callback);
    }

    public void unbindSession(@Nullable IAiBallCallback callback) {
        if (callback == null) {
            return;
        }
        callbacks.unregister(callback);
    }

    public void attachOverlay(@NonNull Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        postToMain(() -> {
            listener.onStateChanged(stateMachine.currentMode(), lastTranscript, lastResult);
            listener.onVisibilityChanged(visible);
        });
    }

    public void detachOverlay(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void showBall() {
        visible = true;
        Log.d(TAG, "showBall");
        dispatchVisibility();
    }

    public void hideBall() {
        visible = false;
        Log.d(TAG, "hideBall");
        dispatchVisibility();
    }

    public void startVoiceWakeup() {
        Log.d(TAG, "startVoiceWakeup");
        if (appContext == null) {
            dispatchError("service_unavailable", "AI 服务尚未初始化");
            return;
        }
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            dispatchError("permission_denied", "请先授予麦克风权限");
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            dispatchError("speech_unavailable", "当前设备不支持系统语音识别");
            return;
        }
        ensureSpeechRecognizer();
        stateMachine.setExpanded(true);
        updateUiMode(AiBallStateMachine.MODE_LISTENING);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        speechRecognizer.startListening(intent);
    }

    public void stopVoiceWakeup() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        updateUiMode(AiBallStateMachine.MODE_IDLE);
    }

    public void submitText(@Nullable String query) {
        String prompt = query == null ? "" : query.trim();
        Log.d(TAG, "submitText length=" + prompt.length());
        if (prompt.isEmpty()) {
            dispatchError("empty_query", "请输入问题内容");
            return;
        }
        appendAiAudit("submit promptLength=" + prompt.length());
        textInputPreferred = true;
        lastTranscript = prompt;
        stateMachine.setExpanded(true);
        updateUiMode(AiBallStateMachine.MODE_THINKING);
        AiLlmRepository.getInstance().ask(prompt, new AiLlmRepository.ResultCallback() {
            @Override
            public void onSuccess(@NonNull String answer) {
                Log.d(TAG, "AI response success length=" + answer.length());
                appendAiAudit("success answerLength=" + answer.length());
                lastResult = answer;
                updateUiMode(AiBallStateMachine.MODE_ACTIVE);
            }

            @Override
            public void onError(@NonNull String message) {
                Log.w(TAG, "AI response error: " + message);
                appendAiCrash("llm_failed " + message);
                dispatchError("llm_failed", sanitizeResult(message));
            }
        });
    }

    public void cancelCurrentTask() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        lastTranscript = "";
        lastResult = "";
        textInputPreferred = false;
        stateMachine.setExpanded(false);
        updateUiMode(AiBallStateMachine.MODE_IDLE);
    }

    public void expandPanel(boolean preferTextInput) {
        textInputPreferred = preferTextInput;
        stateMachine.setExpanded(true);
        if (AiBallStateMachine.MODE_IDLE.equals(stateMachine.currentMode())) {
            stateMachine.update(AiBallStateMachine.MODE_ACTIVE);
        }
        dispatchState();
    }

    public void collapsePanel() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        stateMachine.setExpanded(false);
        stateMachine.update(AiBallStateMachine.MODE_IDLE);
        dispatchState();
    }

    public void updateUiMode(@NonNull String mode) {
        Log.d(TAG, "updateUiMode -> " + mode);
        stateMachine.update(mode);
        dispatchState();
    }

    @NonNull
    public String getLastResult() {
        return lastResult == null ? "" : lastResult;
    }

    @NonNull
    public String getCurrentMode() {
        return stateMachine.currentMode();
    }

    public boolean isExpanded() {
        return stateMachine.isExpanded();
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isTextInputPreferred() {
        return textInputPreferred;
    }

    public void setTextInputPreferred(boolean preferred) {
        textInputPreferred = preferred;
        dispatchState();
    }

    private void ensureSpeechRecognizer() {
        if (speechRecognizer != null || appContext == null) {
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(android.os.Bundle params) {
                updateUiMode(AiBallStateMachine.MODE_LISTENING);
            }

            @Override
            public void onBeginningOfSpeech() {
                updateUiMode(AiBallStateMachine.MODE_LISTENING);
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                updateUiMode(AiBallStateMachine.MODE_THINKING);
            }

            @Override
            public void onError(int error) {
                dispatchError("speech_" + error, "语音识别失败，请重试");
            }

            @Override
            public void onResults(android.os.Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    dispatchError("speech_empty", "未识别到有效语音");
                    return;
                }
                submitText(matches.get(0));
            }

            @Override
            public void onPartialResults(android.os.Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    return;
                }
                lastTranscript = matches.get(0);
                dispatchState();
            }

            @Override
            public void onEvent(int eventType, android.os.Bundle params) {
            }
        });
    }

    private void dispatchState() {
        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            notifySingle(callbacks.getBroadcastItem(i));
        }
        callbacks.finishBroadcast();
        ArrayList<Listener> listenerSnapshot = new ArrayList<>(listeners);
        String mode = stateMachine.currentMode();
        String transcript = lastTranscript;
        String result = lastResult;
        postToMain(() -> {
            for (Listener listener : listenerSnapshot) {
                listener.onStateChanged(mode, transcript, result);
            }
        });
    }

    private void dispatchVisibility() {
        ArrayList<Listener> listenerSnapshot = new ArrayList<>(listeners);
        boolean currentVisible = visible;
        postToMain(() -> {
            for (Listener listener : listenerSnapshot) {
                listener.onVisibilityChanged(currentVisible);
            }
        });
    }

    private void dispatchError(@NonNull String code, @NonNull String message) {
        Log.e(TAG, "dispatchError code=" + code + " message=" + message);
        stateMachine.setExpanded(true);
        stateMachine.update(AiBallStateMachine.MODE_ERROR);
        lastResult = sanitizeResult(message);
        int count = callbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                callbacks.getBroadcastItem(i).onError(code, lastResult);
                callbacks.getBroadcastItem(i).onStateChanged(stateMachine.currentMode(), lastTranscript, lastResult);
            } catch (RemoteException ignored) {
            }
        }
        callbacks.finishBroadcast();
        ArrayList<Listener> listenerSnapshot = new ArrayList<>(listeners);
        String mode = stateMachine.currentMode();
        String transcript = lastTranscript;
        String result = lastResult;
        postToMain(() -> {
            for (Listener listener : listenerSnapshot) {
                listener.onStateChanged(mode, transcript, result);
            }
        });
    }

    private void notifySingle(@NonNull IAiBallCallback callback) {
        try {
            callback.onStateChanged(stateMachine.currentMode(), lastTranscript, lastResult);
        } catch (RemoteException ignored) {
        }
    }

    @NonNull
    private String sanitizeResult(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "当前未返回有效结果";
        }
        return AiLlmRepository.trimReply(value);
    }

    private void appendAiAudit(@NonNull String message) {
        if (appContext != null) {
            new OperationLogStore(appContext).appendAudit("AI", message);
        }
    }

    private void appendAiCrash(@NonNull String message) {
        if (appContext != null) {
            new OperationLogStore(appContext).appendCrash("AI", message);
        }
    }

    private void postToMain(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
            return;
        }
        mainHandler.post(action);
    }
}

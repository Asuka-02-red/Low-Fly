package com.example.low_altitudereststop.feature.ai.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.feature.ai.service.AiBallServiceFacade;
import com.example.low_altitudereststop.feature.permission.AppPermissionManager;

public final class AiBallOverlayController implements AiBallServiceFacade.Listener {

    public enum OverlayWindowType {
        APPLICATION_OVERLAY,
        ACCESSIBILITY_OVERLAY
    }

    private static final String TAG = "AiBallOverlay";
    private static final int DEFAULT_EXPANDED_WIDTH_DP = 336;
    private static final int OVERLAY_MARGIN_DP = 12;
    private static final float HALF_REVEAL_FRACTION = 0.6f;
    private static final long SNAP_ANIMATION_DURATION_MS = 180L;

    private final Context context;
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private final AiBallView ballView;
    private final OverlayWindowType windowType;
    private boolean attached;
    private int screenWidth;
    private int screenHeight;
    private int insetTop;
    private int insetBottom;
    private boolean snappedToRight = true;
    private ValueAnimator snapAnimator;

    public AiBallOverlayController(@NonNull Context context) {
        this(context, OverlayWindowType.ACCESSIBILITY_OVERLAY);
    }

    public AiBallOverlayController(@NonNull Context context, @NonNull OverlayWindowType windowType) {
        this.context = context.getApplicationContext();
        this.windowType = windowType;
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        Context themedContext = new ContextThemeWrapper(this.context, R.style.Theme_LowaltitudeRestStop);
        this.ballView = new AiBallView(themedContext);
        this.layoutParams = createLayoutParams();
        this.ballView.setListener(new AiBallView.Listener() {
            @Override
            public void onBubbleClicked() {
                handleBubbleClicked();
            }

            @Override
            public void onVoiceClicked() {
                requestVoicePermissionThenStart();
            }

            @Override
            public void onSendClicked(@NonNull String text) {
                AiBallServiceFacade.getInstance().submitText(text);
            }

            @Override
            public void onOutsideTouched() {
                collapseToBubble();
            }
        });
        this.ballView.getBubbleView().setOnTouchListener(new DragTouchListener());
        refreshBounds();
        snapToEdge(false);
    }

    public void attach() {
        if (attached) {
            return;
        }
        try {
            windowManager.addView(ballView, layoutParams);
            attached = true;
            AiBallServiceFacade.getInstance().attachOverlay(this);
            ballView.post(() -> {
                refreshBounds();
                snapToEdge(false);
            });
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to attach AI ball overlay", exception);
        }
    }

    public void detach() {
        if (!attached) {
            return;
        }
        try {
            cancelSnapAnimation();
            AiBallServiceFacade.getInstance().detachOverlay(this);
            if (ballView.isAttachedToWindow()) {
                windowManager.removeView(ballView);
            }
        } catch (RuntimeException exception) {
            Log.w(TAG, "Failed to detach AI ball overlay cleanly", exception);
        } finally {
            attached = false;
        }
    }

    public void show() {
        attach();
        ballView.setVisibility(View.VISIBLE);
        refreshBounds();
        refreshOverlayPlacement(false);
        ballView.post(() -> {
            refreshBounds();
            refreshOverlayPlacement(false);
        });
    }

    public void hide() {
        if (!attached) {
            return;
        }
        cancelSnapAnimation();
        hideKeyboard();
        ballView.setVisibility(View.GONE);
    }

    private WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                resolveWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = dp(180);
        return params;
    }

    private int resolveWindowType() {
        if (windowType == OverlayWindowType.APPLICATION_OVERLAY) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
    }

    private void handleBubbleClicked() {
        AiBallServiceFacade facade = AiBallServiceFacade.getInstance();
        String currentMode = facade.getCurrentMode();
        Log.d(TAG, "handleBubbleClicked currentMode=" + currentMode + " textPreferred=" + facade.isTextInputPreferred());
        if (!facade.isExpanded()) {
            if (facade.isTextInputPreferred()) {
                facade.expandPanel(true);
                scheduleExpandedRelayout();
                return;
            }
            facade.expandPanel(false);
            scheduleExpandedRelayout();
            if (!AiBallStateMachine.MODE_THINKING.equals(currentMode)) {
                requestVoicePermissionThenStart();
            }
            return;
        }
        if (facade.isTextInputPreferred()) {
            facade.collapsePanel();
            return;
        }
        facade.expandPanel(true);
        if (AiBallStateMachine.MODE_LISTENING.equals(currentMode)) {
            facade.stopVoiceWakeup();
        }
        facade.updateUiMode(AiBallStateMachine.MODE_ACTIVE);
        scheduleExpandedRelayout();
    }

    private void collapseToBubble() {
        hideKeyboard();
        AiBallServiceFacade facade = AiBallServiceFacade.getInstance();
        facade.collapsePanel();
        ballView.showCollapsed();
        refreshOverlayPlacement(true);
    }

    private void requestVoicePermissionThenStart() {
        AppPermissionManager permissionManager = AppPermissionManager.getInstance();
        AiBallServiceFacade facade = AiBallServiceFacade.getInstance();
        if (permissionManager.isGranted(context, AppPermissionManager.GROUP_RECORD_AUDIO)) {
            facade.expandPanel(false);
            scheduleExpandedRelayout();
            facade.startVoiceWakeup();
            return;
        }
        permissionManager.requestPermissions(
                context,
                permissionManager.buildRecordAudioPayload(),
                new AppPermissionManager.PermissionResultCallback() {
                    @Override
                    public void onGranted() {
                        Toast.makeText(context, R.string.ai_ball_permission_granted, Toast.LENGTH_SHORT).show();
                        facade.expandPanel(false);
                        scheduleExpandedRelayout();
                        facade.startVoiceWakeup();
                    }

                    @Override
                    public void onDenied(@NonNull AppPermissionManager.PermissionState state, @NonNull AppPermissionManager.PermissionRequestPayload payload) {
                        int messageRes = state.permanentlyDenied
                                ? R.string.ai_ball_permission_permanently_denied
                                : R.string.ai_ball_permission_denied;
                        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show();
                        facade.expandPanel(true);
                        scheduleExpandedRelayout();
                    }
                }
        );
    }

    @SuppressWarnings("deprecation")
    private void refreshBounds() {
        WindowManager wm = windowManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            screenWidth = wm.getCurrentWindowMetrics().getBounds().width();
            screenHeight = wm.getCurrentWindowMetrics().getBounds().height();
            WindowInsets windowInsets = wm.getCurrentWindowMetrics().getWindowInsets();
            insetTop = windowInsets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars()).top;
            insetBottom = windowInsets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars()).bottom + dp(12);
            return;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        Rect visibleFrame = new Rect();
        ballView.getWindowVisibleDisplayFrame(visibleFrame);
        insetTop = Math.max(visibleFrame.top, dp(12));
        insetBottom = Math.max(screenHeight - visibleFrame.bottom, 0) + dp(12);
    }

    private void updateLayout() {
        if (!attached) {
            return;
        }
        try {
            windowManager.updateViewLayout(ballView, layoutParams);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Failed to update AI ball overlay layout", exception);
        }
    }

    private void snapToEdge(boolean animate) {
        int bubbleWidth = resolveBubbleWidth();
        int expandedWidth = resolveExpandedWidth();
        int visibleWidth = Math.round(bubbleWidth * HALF_REVEAL_FRACTION);
        int expandedOffset = dp(OVERLAY_MARGIN_DP);
        boolean expanded = AiBallServiceFacade.getInstance().isExpanded();
        applyInteractivityFlags(expanded);
        int targetX;
        if (snappedToRight) {
            targetX = expanded
                    ? Math.max(expandedOffset, screenWidth - expandedWidth - expandedOffset)
                    : screenWidth - visibleWidth;
        } else {
            targetX = expanded ? expandedOffset : -(bubbleWidth - visibleWidth);
        }
        int maxY = Math.max(insetTop + dp(24), screenHeight - insetBottom - dp(72));
        int targetY = Math.max(insetTop + dp(24), Math.min(layoutParams.y, maxY));
        if (animate && attached) {
            animateToPosition(targetX, targetY);
            return;
        }
        cancelSnapAnimation();
        layoutParams.x = targetX;
        layoutParams.y = targetY;
        updateLayout();
    }

    private int dp(int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void refreshOverlayPlacement(boolean animate) {
        if (ballView.getVisibility() != View.VISIBLE) {
            return;
        }
        snapToEdge(animate);
    }

    private void scheduleExpandedRelayout() {
        ballView.post(() -> {
            if (!attached || ballView.getVisibility() != View.VISIBLE || !AiBallServiceFacade.getInstance().isExpanded()) {
                return;
            }
            refreshBounds();
            refreshOverlayPlacement(false);
        });
    }

    private void applyInteractivityFlags(boolean expanded) {
        int desiredFlags;
        if (expanded) {
            desiredFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        } else {
            desiredFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        }
        if (layoutParams.flags != desiredFlags) {
            layoutParams.flags = desiredFlags;
            updateLayout();
        }
    }

    private void animateToPosition(int targetX, int targetY) {
        cancelSnapAnimation();
        final int startX = layoutParams.x;
        final int startY = layoutParams.y;
        if (startX == targetX && startY == targetY) {
            layoutParams.x = targetX;
            layoutParams.y = targetY;
            updateLayout();
            return;
        }
        snapAnimator = ValueAnimator.ofFloat(0f, 1f);
        snapAnimator.setDuration(SNAP_ANIMATION_DURATION_MS);
        snapAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            layoutParams.x = startX + Math.round((targetX - startX) * fraction);
            layoutParams.y = startY + Math.round((targetY - startY) * fraction);
            updateLayout();
        });
        snapAnimator.start();
    }

    private void cancelSnapAnimation() {
        if (snapAnimator != null) {
            snapAnimator.cancel();
            snapAnimator = null;
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = context.getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(ballView.getWindowToken(), 0);
        }
    }

    private int clampDragX(int desiredX) {
        int bubbleWidth = resolveBubbleWidth();
        int expandedWidth = resolveExpandedWidth();
        int visibleWidth = Math.round(bubbleWidth * HALF_REVEAL_FRACTION);
        boolean expanded = AiBallServiceFacade.getInstance().isExpanded();
        int minX = expanded ? dp(OVERLAY_MARGIN_DP) : -(bubbleWidth - visibleWidth);
        int maxX = expanded
                ? Math.max(dp(OVERLAY_MARGIN_DP), screenWidth - expandedWidth - dp(OVERLAY_MARGIN_DP))
                : screenWidth - visibleWidth;
        return Math.max(minX, Math.min(desiredX, maxX));
    }

    private int clampDragY(int desiredY) {
        int minY = insetTop + dp(24);
        int maxY = Math.max(minY, screenHeight - insetBottom - dp(72));
        return Math.max(minY, Math.min(desiredY, maxY));
    }

    @Override
    public void onStateChanged(@NonNull String mode, String transcript, String response) {
        ballView.render(mode, transcript, response);
        if (!AiBallServiceFacade.getInstance().isExpanded()) {
            ballView.showCollapsed();
        } else {
            if (AiBallServiceFacade.getInstance().isTextInputPreferred()) {
                ballView.showTextInput();
            } else {
                ballView.showVoiceInput();
            }
        }
        refreshBounds();
        refreshOverlayPlacement(true);
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (visible) {
            show();
        } else {
            hide();
        }
    }

    private int resolveBubbleWidth() {
        int measured = ballView.getBubbleView().getWidth();
        return measured > 0 ? measured : dp(56);
    }

    private int resolveExpandedWidth() {
        if (!AiBallServiceFacade.getInstance().isExpanded()) {
            return dp(DEFAULT_EXPANDED_WIDTH_DP);
        }
        int viewWidth = ballView.getWidth();
        if (viewWidth > 0) {
            return viewWidth;
        }
        int availableWidth = Math.max(dp(220), screenWidth - dp(OVERLAY_MARGIN_DP * 2));
        ballView.prepareForMeasurement(true, AiBallServiceFacade.getInstance().isTextInputPreferred());
        int widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST);
        ballView.measure(widthSpec, heightSpec);
        int measuredWidth = ballView.getMeasuredWidth();
        if (measuredWidth > 0) {
            return Math.min(measuredWidth, availableWidth);
        }
        return Math.min(dp(DEFAULT_EXPANDED_WIDTH_DP), availableWidth);
    }

    private final class DragTouchListener implements View.OnTouchListener {

        private int startX;
        private int startY;
        private float downRawX;
        private float downRawY;
        private boolean dragging;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelSnapAnimation();
                    refreshBounds();
                    dragging = false;
                    startX = layoutParams.x;
                    startY = layoutParams.y;
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = Math.round(event.getRawX() - downRawX);
                    int deltaY = Math.round(event.getRawY() - downRawY);
                    if (Math.abs(deltaX) > dp(4) || Math.abs(deltaY) > dp(4)) {
                        dragging = true;
                    }
                    if (!dragging) {
                        return true;
                    }
                    layoutParams.x = clampDragX(startX + deltaX);
                    layoutParams.y = clampDragY(startY + deltaY);
                    snappedToRight = layoutParams.x > screenWidth / 2;
                    updateLayout();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging) {
                        v.performClick();
                        return true;
                    }
                    snappedToRight = layoutParams.x > screenWidth / 2;
                    snapToEdge(true);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    if (dragging) {
                        snappedToRight = layoutParams.x > screenWidth / 2;
                        snapToEdge(true);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}

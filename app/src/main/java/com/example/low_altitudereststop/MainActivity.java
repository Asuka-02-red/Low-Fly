package com.example.low_altitudereststop;

import android.os.Bundle;
import android.graphics.Outline;
import android.graphics.Rect;
import android.content.Intent;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.ai.AiBallAccessibilityHelper;
import com.example.low_altitudereststop.feature.ai.AiBallDisplayCoordinator;
import com.example.low_altitudereststop.feature.ai.AiBallOverlayPermissionHelper;
import com.example.low_altitudereststop.feature.ai.AiBallServiceLauncher;
import com.example.low_altitudereststop.feature.ai.AiBallSettingsStore;
import com.example.low_altitudereststop.feature.ai.service.AiBallServiceFacade;
import com.example.low_altitudereststop.ui.IconRegistry;
import com.example.low_altitudereststop.ui.RoleUiConfig;
import com.example.low_altitudereststop.ui.UsageAnalyticsStore;
import com.example.low_altitudereststop.ui.UserRole;
import com.example.low_altitudereststop.ui.AccessibilityDisplayMode;
import com.example.low_altitudereststop.feature.auth.AuthActivity;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends com.example.low_altitudereststop.core.ui.EdgeToEdgeActivity {

    public static final String EXTRA_TARGET_DESTINATION = "target_destination";
    private static final long BOTTOM_NAV_MOTION_DURATION_MS = 150L;
    private static final float BOTTOM_NAV_SELECTED_SCALE = 1.08f;
    private static final float BOTTOM_NAV_UNSELECTED_SCALE = 1.0f;
    private static final float BOTTOM_NAV_SELECTED_ALPHA = 1.0f;
    private static final float BOTTOM_NAV_UNSELECTED_ALPHA = 0.74f;
    private static final int BOTTOM_NAV_SELECTED_LIFT_DP = 0;
    private static final int BOTTOM_NAV_CONTENT_OFFSET_DP = 5;
    private static final int BOTTOM_NAV_SHELL_CORNER_RADIUS_DP = 32;
    private Snackbar aiBallSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionStore sessionStore = new SessionStore(this);
        if (!sessionStore.isLoggedIn()) {
            SessionStore.AutoLoginSnapshot snapshot = sessionStore.getAutoLoginSnapshot();
            if (snapshot.expired) {
                sessionStore.clear();
            }
            if (snapshot.enabled && !snapshot.expired) {
                trySilentRefresh(sessionStore, snapshot.refreshToken);
                return;
            }
            startAuth();
            return;
        }
        try {
            setContentView(R.layout.activity_main);

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment == null) {
                return;
            }

            NavController navController = navHostFragment.getNavController();
            View root = findViewById(R.id.main_root);
            View host = findViewById(R.id.nav_host_fragment);
            View bottomNavShell = findViewById(R.id.bottom_nav_shell);
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            UserRole userRole = UserRole.from(sessionStore.getCachedUser().role);
            RoleUiConfig config = RoleUiConfig.from(userRole);
            UsageAnalyticsStore analyticsStore = new UsageAnalyticsStore(this);
            analyticsStore.trackRoleLanding(userRole);
            configureBottomNavShell(bottomNavShell);
            applyMainInsets(root, host, bottomNavShell, bottomNav);
            IconRegistry.verifyCriticalIcons(this);
            IconRegistry.applyBottomNavIcons(bottomNav);
            applyRoleNavigation(bottomNavShell, bottomNav, config, userRole);
            NavigationUI.setupWithNavController(bottomNav, navController);
            safeSetupBottomNavMotion(bottomNav);
            ensureAiBallService();
            bottomNav.setOnItemSelectedListener(item -> {
                trackBottomTab(analyticsStore, userRole, item.getItemId());
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .build();
                try {
                    navController.navigate(resolveDestinationId(userRole, item.getItemId()), null, options);
                    bottomNav.post(() -> safeSyncBottomNavSelectionState(bottomNav, true));
                    return true;
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            });
            bottomNav.setOnItemReselectedListener(item -> {
                if (navController.getCurrentDestination() == null) {
                    return;
                }
                if (item.getItemId() == R.id.homeFragment && navController.getCurrentDestination().getId() != R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false);
                }
            });
            navigateByIntent(bottomNav);
            bottomNav.post(() -> safeSyncBottomNavSelectionState(bottomNav, false));
        } catch (Throwable throwable) {
            new OperationLogStore(this).appendCrash("MAIN_ACTIVITY", "startup_failed: " + throwable.getClass().getSimpleName());
            sessionStore.clearAuth();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void applyMainInsets(
            @NonNull View root,
            @NonNull View host,
            @NonNull View bottomNavShell,
            @NonNull BottomNavigationView bottomNav
    ) {
        final int rootLeft = root.getPaddingLeft();
        final int rootTop = root.getPaddingTop();
        final int rootRight = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();
        final int hostLeft = host.getPaddingLeft();
        final int hostTop = host.getPaddingTop();
        final int hostRight = host.getPaddingRight();
        final int hostBottom = host.getPaddingBottom();
        final ViewGroup.MarginLayoutParams shellLayoutParams =
                (ViewGroup.MarginLayoutParams) bottomNavShell.getLayoutParams();
        final int shellMarginStart = shellLayoutParams.getMarginStart();
        final int shellMarginEnd = shellLayoutParams.getMarginEnd();
        final int shellMarginBottom = shellLayoutParams.bottomMargin;
        final int navLeft = bottomNav.getPaddingLeft();
        final int navTop = bottomNav.getPaddingTop();
        final int navRight = bottomNav.getPaddingRight();
        final int navBottom = bottomNav.getPaddingBottom();
        final int extraTop = dpToPx(8);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(rootLeft, rootTop + systemBars.top + extraTop, rootRight, rootBottom);
            host.setPadding(hostLeft + systemBars.left, hostTop, hostRight + systemBars.right, hostBottom);
            shellLayoutParams.setMarginStart(shellMarginStart + systemBars.left);
            shellLayoutParams.setMarginEnd(shellMarginEnd + systemBars.right);
            shellLayoutParams.bottomMargin = shellMarginBottom + systemBars.bottom;
            bottomNavShell.setLayoutParams(shellLayoutParams);
            bottomNav.setPadding(
                    navLeft,
                    navTop,
                    navRight,
                    navBottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void applyRoleNavigation(
            @NonNull View bottomNavShell,
            @NonNull BottomNavigationView bottomNav,
            @NonNull RoleUiConfig config,
            @NonNull UserRole userRole
    ) {
        bottomNav.getMenu().findItem(R.id.homeFragment).setTitle(config.dashboardLabel);
        bottomNav.getMenu().findItem(R.id.taskFragment).setTitle(config.taskLabel);
        bottomNav.getMenu().findItem(R.id.messageFragment).setTitle(config.messageLabel);
        bottomNav.getMenu().findItem(R.id.trainingFragment).setTitle(config.trainingLabel);
        bottomNav.getMenu().findItem(R.id.trainingFragment).setIcon(IconRegistry.resolveDrawable(
                this,
                R.drawable.ic_nav_training,
                R.drawable.ic_fallback_generic
        ));
        bottomNav.getMenu().findItem(R.id.profileFragment).setTitle(config.profileLabel);
        boolean highContrastEnabled = AccessibilityDisplayMode.isHighContrastEnabled(this);
        int accent = ContextCompat.getColor(
                this,
                highContrastEnabled ? R.color.ui_nav_hc_active_text : R.color.ui_nav_active_text
        );
        int normal = ContextCompat.getColor(
                this,
                highContrastEnabled ? R.color.ui_nav_hc_inactive_text : R.color.ui_nav_inactive_text
        );
        ColorStateList tint = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{accent, normal}
        );
        bottomNavShell.setBackgroundResource(
                highContrastEnabled
                        ? R.drawable.bg_bottom_nav_high_contrast
                        : R.drawable.bg_bottom_nav
        );
        bottomNav.setItemBackgroundResource(
                highContrastEnabled
                        ? com.example.low_altitudereststop.core.ui.R.drawable.bg_bottom_nav_item_high_contrast
                        : com.example.low_altitudereststop.core.ui.R.drawable.bg_bottom_nav_item
        );
        bottomNav.setItemActiveIndicatorEnabled(false);
        bottomNav.setItemIconTintList(tint);
        bottomNav.setItemTextColor(tint);
    }

    private void configureBottomNavShell(@NonNull View bottomNavShell) {
        bottomNavShell.setClipToOutline(true);
        bottomNavShell.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                    return;
                }
                outline.setRoundRect(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        dpToPx(BOTTOM_NAV_SHELL_CORNER_RADIUS_DP)
                );
            }
        });
    }

    private void safeSetupBottomNavMotion(@NonNull BottomNavigationView bottomNav) {
        try {
            bottomNav.setItemActiveIndicatorEnabled(false);
            safeSyncBottomNavSelectionState(bottomNav, false);
        } catch (RuntimeException ignored) {
            // Some Material internal item hierarchies differ by device/theme; keep navigation usable.
        }
    }

    private void safeSyncBottomNavSelectionState(@NonNull BottomNavigationView bottomNav, boolean animate) {
        try {
            View child = bottomNav.getChildAt(0);
            if (!(child instanceof ViewGroup)) {
                return;
            }
            ViewGroup menuView = (ViewGroup) child;
            int itemCount = Math.min(menuView.getChildCount(), bottomNav.getMenu().size());
            for (int i = 0; i < itemCount; i++) {
                boolean checked = bottomNav.getMenu().getItem(i).isChecked();
                View itemView = menuView.getChildAt(i);
                normalizeBottomNavItemLayout(itemView);
                applyNavItemVisualState(itemView, checked, animate);
            }
        } catch (RuntimeException ignored) {
            // Fall back to plain Material selection state instead of crashing on launch.
        }
    }

    private void normalizeBottomNavItemLayout(@NonNull View view) {
        if (view.getId() == com.google.android.material.R.id.navigation_bar_item_icon_view) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
                marginLayoutParams.bottomMargin = dpToPx(1);
                view.setLayoutParams(marginLayoutParams);
            }
            view.setPadding(0, 0, 0, 0);
            view.setTranslationY(0f);
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setGravity(Gravity.CENTER);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setSingleLine(true);
            textView.setIncludeFontPadding(false);
            textView.setMinLines(1);
            ViewGroup.LayoutParams params = textView.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) params;
                marginLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                marginLayoutParams.topMargin = 0;
                marginLayoutParams.bottomMargin = 0;
                textView.setLayoutParams(marginLayoutParams);
            } else if (params != null) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                textView.setLayoutParams(params);
            }
            ViewParent parent = textView.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) parent;
                group.setPadding(0, 0, 0, 0);
                group.setClipChildren(false);
                group.setClipToPadding(false);
                if (group.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams groupParams =
                            (ViewGroup.MarginLayoutParams) group.getLayoutParams();
                    groupParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    group.setLayoutParams(groupParams);
                }
            }
            textView.setPadding(0, 0, 0, 0);
            textView.setTranslationY(0f);
            return;
        }
        if (view instanceof ImageView) {
            view.setPadding(0, 0, 0, 0);
            view.setTranslationY(0f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            group.setClipChildren(false);
            group.setClipToPadding(false);
            if (group instanceof LinearLayout) {
                ((LinearLayout) group).setGravity(Gravity.CENTER);
            }
            if (isBottomNavItemContainer(group)) {
                group.setMinimumHeight(dimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_bottom_nav_height));
                int horizontalPadding = dimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_nav_padding_horizontal);
                int topPadding = dimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_nav_item_padding_top);
                int bottomPadding = dimenPx(com.example.low_altitudereststop.core.ui.R.dimen.ui_nav_item_padding_bottom);
                group.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);
            } else {
                group.setPadding(0, 0, 0, 0);
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                normalizeBottomNavItemLayout(group.getChildAt(i));
            }
        }
    }

    private void applyNavItemVisualState(@NonNull View view, boolean checked, boolean animate) {
        float centerCorrection = resolveBottomNavItemCenterCorrection(view);
        float targetTranslationY = centerCorrection + (checked ? -dpToPx(BOTTOM_NAV_SELECTED_LIFT_DP) : 0f);
        if (animate) {
            view.animate()
                    .translationY(targetTranslationY)
                    .setDuration(BOTTOM_NAV_MOTION_DURATION_MS)
                    .start();
        } else {
            view.setTranslationY(targetTranslationY);
        }
        applyNavItemContentState(view, checked, animate);
    }

    private float resolveBottomNavItemCenterCorrection(@NonNull View itemView) {
        if (itemView.getWidth() == 0 || itemView.getHeight() == 0) {
            return 0f;
        }
        Rect contentBounds = new Rect();
        if (!collectBottomNavContentBounds(itemView, itemView, contentBounds)) {
            return 0f;
        }
        float itemCenter = itemView.getHeight() / 2f;
        float contentCenter = (contentBounds.top + contentBounds.bottom) / 2f;
        return Math.round(itemCenter - contentCenter) + dpToPx(BOTTOM_NAV_CONTENT_OFFSET_DP);
    }

    private boolean collectBottomNavContentBounds(@NonNull View root, @NonNull View view, @NonNull Rect outBounds) {
        boolean found = false;
        if (view.getVisibility() == View.VISIBLE && view.getWidth() > 0 && view.getHeight() > 0) {
            if (view instanceof TextView || view.getId() == com.google.android.material.R.id.navigation_bar_item_icon_view) {
                int[] rootLocation = new int[2];
                int[] viewLocation = new int[2];
                root.getLocationOnScreen(rootLocation);
                view.getLocationOnScreen(viewLocation);
                Rect currentBounds = new Rect(
                        viewLocation[0] - rootLocation[0],
                        viewLocation[1] - rootLocation[1],
                        viewLocation[0] - rootLocation[0] + view.getWidth(),
                        viewLocation[1] - rootLocation[1] + view.getHeight()
                );
                if (outBounds.isEmpty()) {
                    outBounds.set(currentBounds);
                } else {
                    outBounds.union(currentBounds);
                }
                found = true;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                found |= collectBottomNavContentBounds(root, group.getChildAt(i), outBounds);
            }
        }
        return found;
    }

    private void applyNavItemContentState(@NonNull View view, boolean checked, boolean animate) {
        float targetScale = checked ? BOTTOM_NAV_SELECTED_SCALE : BOTTOM_NAV_UNSELECTED_SCALE;
        float targetAlpha = checked ? BOTTOM_NAV_SELECTED_ALPHA : BOTTOM_NAV_UNSELECTED_ALPHA;
        if (view instanceof TextView || view instanceof ImageView) {
            if (animate) {
                view.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .alpha(targetAlpha)
                        .setDuration(BOTTOM_NAV_MOTION_DURATION_MS)
                        .start();
            } else {
                view.setScaleX(targetScale);
                view.setScaleY(targetScale);
                view.setAlpha(targetAlpha);
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyNavItemContentState(group.getChildAt(i), checked, animate);
            }
        }
    }

    private void trackBottomTab(@NonNull UsageAnalyticsStore analyticsStore, @NonNull UserRole role, int itemId) {
        if (itemId == R.id.homeFragment) {
            analyticsStore.trackFeature(role, "home");
        } else if (itemId == R.id.taskFragment) {
            analyticsStore.trackFeature(role, "task");
        } else if (itemId == R.id.messageFragment) {
            analyticsStore.trackFeature(role, "message");
        } else if (itemId == R.id.trainingFragment) {
            analyticsStore.trackFeature(role, "training_manage");
        } else if (itemId == R.id.profileFragment) {
            analyticsStore.trackFeature(role, "profile");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionStore store = new SessionStore(this);
        if (!store.isLoggedIn()) {
            startAuth();
            return;
        }
        syncAiBallVisibility();
    }

    private void trySilentRefresh(@NonNull SessionStore sessionStore, @NonNull String refreshToken) {
        AuthModels.RefreshTokenRequest request = new AuthModels.RefreshTokenRequest();
        request.refreshToken = refreshToken;
        ApiClient.getPublicService(this).refresh(request).enqueue(new Callback<ApiEnvelope<AuthModels.AuthPayload>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AuthModels.AuthPayload>> call, Response<ApiEnvelope<AuthModels.AuthPayload>> response) {
                ApiEnvelope<AuthModels.AuthPayload> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                    sessionStore.clear();
                    startAuth();
                    return;
                }
                sessionStore.saveAuth(envelope.data);
                sessionStore.setAutoLoginEnabled(sessionStore.getCachedUser().username, envelope.data.refreshToken, true);
                recreate();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AuthModels.AuthPayload>> call, Throwable t) {
                sessionStore.clear();
                startAuth();
            }
        });
    }

    private void startAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void navigateByIntent(@NonNull BottomNavigationView bottomNav) {
        int destinationId = getIntent().getIntExtra(EXTRA_TARGET_DESTINATION, View.NO_ID);
        if (destinationId != View.NO_ID && bottomNav.getMenu().findItem(destinationId) != null) {
            bottomNav.setSelectedItemId(destinationId);
        }
    }

    private int dpToPx(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    private boolean isBottomNavItemContainer(@NonNull ViewGroup group) {
        String simpleName = group.getClass().getSimpleName();
        return "NavigationBarItemView".equals(simpleName)
                || "BottomNavigationItemView".equals(simpleName);
    }

    private int dimenPx(int resId) {
        return getResources().getDimensionPixelSize(resId);
    }

    private int resolveDestinationId(@NonNull UserRole role, int itemId) {
        return itemId;
    }

    private void ensureAiBallService() {
        if (!BuildConfig.ENABLE_AI_BALL) {
            return;
        }
        syncAiBallVisibility();
    }

    private void syncAiBallVisibility() {
        if (!BuildConfig.ENABLE_AI_BALL) {
            return;
        }
        AiBallSettingsStore settingsStore = new AiBallSettingsStore(this);
        AiBallDisplayCoordinator.Availability availability = AiBallDisplayCoordinator.resolve(
                settingsStore.isEnabled(),
                AiBallOverlayPermissionHelper.canDrawOverlays(this),
                AiBallAccessibilityHelper.isServiceEnabled(this)
        );
        if (availability == AiBallDisplayCoordinator.Availability.DISABLED) {
            dismissAiBallPrompt();
            AiBallServiceFacade.getInstance().collapsePanel();
            AiBallServiceFacade.getInstance().hideBall();
            return;
        }
        if (AiBallDisplayCoordinator.shouldAttachOverlayHost(availability)
                || AiBallDisplayCoordinator.shouldUseAccessibilityFallback(availability)) {
            AiBallServiceLauncher.ensureStarted(this);
        }
        AiBallServiceFacade.getInstance().showBall();
        maybePromptAiBallPermission(availability);
    }

    private void maybePromptAiBallPermission(@NonNull AiBallDisplayCoordinator.Availability availability) {
        if (availability == AiBallDisplayCoordinator.Availability.OVERLAY_READY
                || availability == AiBallDisplayCoordinator.Availability.ACCESSIBILITY_FALLBACK) {
            dismissAiBallPrompt();
            return;
        }
        View root = findViewById(R.id.main_root);
        if (root == null) {
            return;
        }
        if (aiBallSnackbar != null && aiBallSnackbar.isShown()) {
            return;
        }
        aiBallSnackbar = Snackbar.make(root, R.string.ai_ball_enable_message, Snackbar.LENGTH_INDEFINITE);
        aiBallSnackbar.setAction(R.string.ai_ball_enable_action, v ->
                startActivity(AiBallOverlayPermissionHelper.createManageOverlayPermissionIntent(this)));
        aiBallSnackbar.show();
    }

    private void dismissAiBallPrompt() {
        if (aiBallSnackbar != null) {
            aiBallSnackbar.dismiss();
            aiBallSnackbar = null;
        }
    }
}

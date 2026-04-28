package com.example.low_altitudereststop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.order.OrderDetailActivity;
import com.example.low_altitudereststop.feature.order.OrderListActivity;
import com.example.low_altitudereststop.feature.profile.SettingsActivity;
import com.example.low_altitudereststop.feature.task.TaskDetailActivity;
import com.example.low_altitudereststop.ui.IconRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainNavigationInstrumentedTest {

    @Before
    public void seedSession() {
        saveSession("PILOT");
    }

    @Test
    public void bottomNavigationCanReturnHomeAndCaptureScreenshots() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
                bottomNav.setSelectedItemId(R.id.taskFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                NavController navController = navController(activity);
                assertEquals(R.id.taskFragment, navController.getCurrentDestination().getId());
                TextView title = activity.findViewById(R.id.tv_title);
                assertNotNull(title);
                assertTrue(title.isShown());
            });
            capture(scenario, "task");

            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
                bottomNav.setSelectedItemId(R.id.profileFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                NavController navController = navController(activity);
                assertEquals(R.id.profileFragment, navController.getCurrentDestination().getId());
                TextView title = activity.findViewById(R.id.tv_title);
                assertNotNull(title);
                assertTrue(title.isShown());
            });
            capture(scenario, "profile");

            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
                bottomNav.setSelectedItemId(R.id.homeFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                NavController navController = navController(activity);
                assertEquals(R.id.homeFragment, navController.getCurrentDestination().getId());
                assertTrue(requireView(activity, R.id.tv_welcome).isShown());
                assertTrue(requireView(activity, R.id.btn_go_task).isShown());
                assertTrue(requireView(activity, R.id.btn_go_compliance).isShown());
                assertTrue(requireView(activity, R.id.btn_go_profile).isShown());
                TextView quickTask = activity.findViewById(R.id.tv_quick_task);
                TextView quickCompliance = activity.findViewById(R.id.tv_quick_compliance);
                TextView quickProfile = activity.findViewById(R.id.tv_quick_profile);
                assertNotNull(quickTask);
                assertNotNull(quickCompliance);
                assertNotNull(quickProfile);
                assertTrue(quickTask.getLineCount() <= 1);
                assertTrue(quickCompliance.getLineCount() <= 1);
                assertTrue(quickProfile.getLineCount() <= 1);
            });
            capture(scenario, "home");
        }
    }

    @Test
    public void enterpriseHomePrimaryActionIsLargerThanPeerActions() {
        saveSession("ENTERPRISE");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
                bottomNav.setSelectedItemId(R.id.homeFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View quickTaskButton = requireView(activity, R.id.btn_go_task);
                View quickComplianceButton = requireView(activity, R.id.btn_go_compliance);
                ImageView quickTaskIcon = activity.findViewById(R.id.iv_quick_task);
                ImageView quickComplianceIcon = activity.findViewById(R.id.iv_quick_compliance);
                TextView quickTask = activity.findViewById(R.id.tv_quick_task);
                TextView quickCompliance = activity.findViewById(R.id.tv_quick_compliance);
                TextView quickProfile = activity.findViewById(R.id.tv_quick_profile);
                assertNotNull(quickTaskIcon);
                assertNotNull(quickComplianceIcon);
                assertNotNull(quickTask);
                assertNotNull(quickCompliance);
                assertNotNull(quickProfile);
                assertTrue(quickTaskButton.getMinimumHeight() > quickComplianceButton.getMinimumHeight());
                assertTrue(quickTaskIcon.getLayoutParams().width > quickComplianceIcon.getLayoutParams().width);
                assertTrue(quickTask.getLineCount() <= 1);
                assertTrue(quickCompliance.getLineCount() <= 1);
                assertTrue(quickProfile.getLineCount() <= 1);
            });
            capture(scenario, "enterprise-home");
        }
    }

    @Test
    public void criticalIconsLoadSuccessfully() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(IconRegistry.verifyCriticalIcons(context));
    }

    @Test
    public void taskDetailSectionsAreVisible() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, 101L);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_TITLE, "长江沿线巡检");
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_TYPE, "INSPECTION");
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_LOCATION, "重庆江北区");
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_STATUS, "PUBLISHED");
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_DEADLINE, "2026-04-30 10:00");
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_BUDGET, "3000");
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_OWNER, "Low Altitude Lab");
        try (ActivityScenario<TaskDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView title = activity.findViewById(R.id.tv_title);
                assertNotNull(title);
                assertTrue(title.isShown());
            });
        }
    }

    @Test
    public void orderDetailSectionsAreVisible() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, 1001L);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_NO, "ORD-DEMO-1001");
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_STATUS, "PENDING_PAYMENT");
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_AMOUNT, "3000");
        try (ActivityScenario<OrderDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertTrue(requireView(activity, R.id.tv_title).isShown());
                assertTrue(requireView(activity, R.id.card_task).isShown());
                assertTrue(requireView(activity, R.id.card_payment).isShown());
            });
        }
    }

    @Test
    public void orderListShowsEmptyStateWhenNoData() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrderListActivity.class);
        intent.putExtra(OrderListActivity.EXTRA_FORCE_EMPTY_STATE, true);
        try (ActivityScenario<OrderListActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertTrue(requireView(activity, R.id.iv_empty_state).isShown());
                TextView title = activity.findViewById(R.id.tv_empty_state);
                assertNotNull(title);
                assertTrue(title.isShown());
            });
        }
    }

    @Test
    public void settingsScreenShowsAiBallControls() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(requireView(activity, R.id.switch_ai_ball).isShown());
                assertTrue(requireView(activity, R.id.btn_open_overlay_permission).isShown());
                assertTrue(requireView(activity, R.id.btn_open_accessibility).isShown());
                TextView status = activity.findViewById(R.id.tv_ai_ball_status);
                assertNotNull(status);
                assertTrue(status.isShown());
                assertTrue(status.getText().toString().startsWith("当前状态："));
            });
        }
    }

    private NavController navController(MainActivity activity) {
        NavHostFragment navHostFragment =
                (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assertNotNull(navHostFragment);
        return navHostFragment.getNavController();
    }

    private void saveSession(String role) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AuthModels.AuthPayload payload = new AuthModels.AuthPayload();
        payload.token = "ui-test-token";
        payload.refreshToken = "ui-test-refresh";
        payload.userInfo = new AuthModels.SessionInfo();
        payload.userInfo.username = "ui_demo";
        payload.userInfo.realName = "UI Demo";
        payload.userInfo.role = role;
        payload.userInfo.companyName = "Low Altitude Lab";
        new SessionStore(context).saveAuth(payload);
    }

    private View requireView(android.app.Activity activity, int id) {
        View view = activity.findViewById(id);
        assertNotNull(view);
        return view;
    }

    private void capture(ActivityScenario<MainActivity> scenario, String name) {
        scenario.onActivity(activity -> {
            View root = activity.getWindow().getDecorView().getRootView();
            root.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
            root.draw(new android.graphics.Canvas(bitmap));
            File dir = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "ui-regression-" + name + ".png");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } catch (Exception ignored) {
            }
        });
    }
}

package com.example.low_altitudereststop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.compliance.FlightApplicationManageActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FlightManagementInstrumentedTest {

    @Before
    public void seedEnterpriseSession() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AuthModels.AuthPayload payload = new AuthModels.AuthPayload();
        payload.token = "ui-flight-token";
        payload.refreshToken = "ui-flight-refresh";
        payload.userInfo = new AuthModels.SessionInfo();
        payload.userInfo.username = "enterprise_demo";
        payload.userInfo.realName = "企业演示账号";
        payload.userInfo.role = "ENTERPRISE";
        payload.userInfo.companyName = "低空企业";
        new SessionStore(context).saveAuth(payload);
    }

    @Test
    public void enterpriseBottomNavigationRoutesToCourseManagement() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
                MenuItem item = bottomNav.getMenu().findItem(R.id.trainingFragment);
                assertEquals("课程", item.getTitle());
                bottomNav.setSelectedItemId(R.id.trainingFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                NavHostFragment hostFragment = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                NavController navController = hostFragment.getNavController();
                assertEquals(R.id.trainingFragment, navController.getCurrentDestination().getId());
                TextView title = activity.findViewById(R.id.tv_title);
                assertEquals("团队课程", title.getText().toString());
                assertTrue(activity.findViewById(R.id.btn_add_course).isShown());
            });
        }
    }

    @Test
    public void flightApplicationManageScreenShowsCoreActions() {
        try (ActivityScenario<FlightApplicationManageActivity> scenario = ActivityScenario.launch(FlightApplicationManageActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.btn_batch_approve).isShown());
                assertTrue(activity.findViewById(R.id.btn_batch_reject).isShown());
                assertTrue(activity.findViewById(R.id.recycler).isShown());
            });
        }
    }
}

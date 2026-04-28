package com.example.low_altitudereststop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.message.MessageDetailActivity;
import com.example.low_altitudereststop.feature.message.MessageRepository;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.ui.AppThemeMode;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ThemeAndRotationInstrumentedTest {

    @Before
    public void seedData() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AuthModels.AuthPayload payload = new AuthModels.AuthPayload();
        payload.token = "theme-ui-token";
        payload.refreshToken = "theme-ui-refresh";
        payload.userInfo = new AuthModels.SessionInfo();
        payload.userInfo.username = "theme_demo";
        payload.userInfo.realName = "主题测试";
        payload.userInfo.role = "ENTERPRISE";
        payload.userInfo.companyName = "低空企业";
        new SessionStore(context).saveAuth(payload);

        MessageEntity entity = new MessageEntity();
        entity.msgId = 777L;
        entity.conversationId = 901L;
        entity.content = "旋转保留草稿";
        entity.counterpartTitle = "主题与旋转测试";
        entity.pilotUid = "pilot-rotation";
        entity.enterpriseUid = "enterprise-rotation";
        entity.createTime = "10:00";
        MessageRepository.get(context).replaceAllForTesting(Collections.singletonList(entity));
    }

    @After
    public void resetTheme() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppThemeMode.persistAndApply(context, true);
    }

    @Test
    public void settingsCanSwitchToLightMode() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppThemeMode.persistAndApply(context, false);
        try (ActivityScenario<com.example.low_altitudereststop.feature.profile.SettingsActivity> scenario =
                     ActivityScenario.launch(com.example.low_altitudereststop.feature.profile.SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode());
                assertTrue(activity.findViewById(R.id.switch_dark_mode).isShown());
            });
        }
    }

    @Test
    public void messageDraftSurvivesRotation() {
        android.content.Intent intent = new android.content.Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MessageDetailActivity.class
        );
        intent.putExtra(MessageDetailActivity.EXTRA_CONVERSATION_ID, 901L);
        try (ActivityScenario<MessageDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView input = activity.findViewById(R.id.et_message);
                input.setText("旋转后仍应保留");
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                TextView input = activity.findViewById(R.id.et_message);
                assertEquals("旋转后仍应保留", input.getText().toString());
                assertTrue(activity.findViewById(R.id.btn_send).isShown());
            });
        }
    }
}

package com.example.low_altitudereststop;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.feature.message.MessageListActivity;
import com.example.low_altitudereststop.feature.message.MessageRealtimeHub;
import com.example.low_altitudereststop.feature.message.MessageRepository;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MessageReadStatusInstrumentedTest {

    @Before
    public void seedSessionAndMessages() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AuthModels.AuthPayload payload = new AuthModels.AuthPayload();
        payload.token = "ui-test-token";
        payload.refreshToken = "ui-test-refresh";
        payload.userInfo = new AuthModels.SessionInfo();
        payload.userInfo.username = "enterprise_demo";
        payload.userInfo.realName = "企业测试";
        payload.userInfo.role = "ENTERPRISE";
        payload.userInfo.companyName = "低空测试企业";
        new SessionStore(context).saveAuth(payload);

        MessageEntity entity = new MessageEntity();
        entity.msgId = 9001L;
        entity.conversationId = 500L;
        entity.content = "实时已读回执测试";
        entity.counterpartTitle = "测试会话";
        entity.pilotUid = "pilot-1234";
        entity.enterpriseUid = "ent-5678";
        entity.createTime = "09:30";
        entity.createTimeMillis = System.currentTimeMillis();
        entity.mine = false;
        entity.isRead = false;
        MessageRepository.get(context).replaceAllForTesting(Collections.singletonList(entity));
    }

    @Test
    public void chatListDisplaysAfterDataSeed() {
        try (ActivityScenario<MessageListActivity> scenario = ActivityScenario.launch(MessageListActivity.class)) {
            onView(withId(R.id.recycler)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void readStatusPushUpdatesUi() {
        try (ActivityScenario<MessageListActivity> scenario = ActivityScenario.launch(MessageListActivity.class)) {
            MessageRealtimeHub.emitReadStatusForTest(9001L, true);
            onView(withId(R.id.recycler)).check(matches(isDisplayed()));
        }
    }
}

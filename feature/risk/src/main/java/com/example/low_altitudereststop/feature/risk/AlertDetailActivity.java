package com.example.low_altitudereststop.feature.risk;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;

/**
 * 告警详情页面，展示单条告警的风险等级、发生时间、风险描述和处置建议。
 */
public class AlertDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_ALERT_ID = "alert_id";
    public static final String EXTRA_ROLE_NAME = "role_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);

        long alertId = getIntent().getLongExtra(EXTRA_ALERT_ID, -1L);
        String roleName = getIntent().getStringExtra(EXTRA_ROLE_NAME);
        AlertScenarioRepository.AlertScenario scenario = AlertScenarioRepository.findById(roleName, alertId);
        if (scenario == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tv_title)).setText(scenario.detailTitle);
        ((TextView) findViewById(R.id.tv_subtitle)).setText(safe(scenario.alert.status));
        ((TextView) findViewById(R.id.tv_level)).setText(safe(scenario.alert.level));
        ((TextView) findViewById(R.id.tv_time)).setText(scenario.occurredAt);
        ((TextView) findViewById(R.id.tv_description)).setText(scenario.riskDescription);
        ((TextView) findViewById(R.id.tv_suggestion)).setText(scenario.suggestion);
    }

    @NonNull
    private String safe(String text) {
        return text == null ? "-" : text;
    }
}

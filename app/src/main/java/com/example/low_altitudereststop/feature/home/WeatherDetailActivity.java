package com.example.low_altitudereststop.feature.home;

import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.databinding.ActivityWeatherDetailBinding;

/**
 * 天气详情展示Activity。
 * <p>
 * 从Intent中接收天气数据参数，展示完整的天气信息包括位置、天气状况、
 * 飞行适宜性结论与摘要、雷暴风险等级与防护建议、温度、湿度、
 * 风向风速、能见度和降水等详细指标。
 * </p>
 */
public class WeatherDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_WEATHER = "weather";
    public static final String EXTRA_WEATHER_ICON_TYPE = "weather_icon_type";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_SUMMARY = "summary";
    public static final String EXTRA_TEMPERATURE = "temperature";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_VISIBILITY = "visibility";
    public static final String EXTRA_PRECIPITATION = "precipitation";
    public static final String EXTRA_THUNDERSTORM = "thunderstorm";
    public static final String EXTRA_THUNDERSTORM_LEVEL = "thunderstorm_level";
    public static final String EXTRA_THUNDERSTORM_LABEL = "thunderstorm_label";
    public static final String EXTRA_THUNDERSTORM_HINT = "thunderstorm_hint";
    public static final String EXTRA_META = "meta";
    public static final String EXTRA_SOURCE_NOTE = "source_note";

    private ActivityWeatherDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWeatherDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvLocation.setText(safe(getIntent().getStringExtra(EXTRA_LOCATION)));
        binding.ivWeatherIcon.setImageResource(
                WeatherVisuals.resolveWeatherIcon(getIntent().getStringExtra(EXTRA_WEATHER_ICON_TYPE))
        );
        binding.tvWeather.setText(safe(getIntent().getStringExtra(EXTRA_WEATHER)));
        binding.tvResult.setText(safe(getIntent().getStringExtra(EXTRA_RESULT)));
        binding.tvResult.setTextColor(resolveResultColor(binding.tvResult.getText().toString()));
        binding.tvSummary.setText(safe(getIntent().getStringExtra(EXTRA_SUMMARY)));
        bindRiskSection();
        binding.tvTemperature.setText(safe(getIntent().getStringExtra(EXTRA_TEMPERATURE)));
        binding.tvHumidity.setText(safe(getIntent().getStringExtra(EXTRA_HUMIDITY)));
        binding.tvWind.setText(safe(getIntent().getStringExtra(EXTRA_WIND)));
        binding.tvVisibility.setText(safe(getIntent().getStringExtra(EXTRA_VISIBILITY)));
        binding.tvPrecipitation.setText(safe(getIntent().getStringExtra(EXTRA_PRECIPITATION)));
    }

    private void bindRiskSection() {
        String summary = safe(getIntent().getStringExtra(EXTRA_THUNDERSTORM));
        String levelText = safe(getIntent().getStringExtra(EXTRA_THUNDERSTORM_LEVEL));
        String label = safe(getIntent().getStringExtra(EXTRA_THUNDERSTORM_LABEL));
        String hint = safe(getIntent().getStringExtra(EXTRA_THUNDERSTORM_HINT));
        int level = parseLevel(levelText);
        int riskColor = WeatherVisuals.resolveRiskColor(this, level);
        int riskForeground = WeatherVisuals.resolveRiskForegroundColor(this, level);

        binding.tvThunderstorm.setText(summary);
        binding.tvThunderstorm.setTextColor(riskColor);
        binding.tvRiskLevel.setText("L" + level);
        binding.tvRiskLevel.setTextColor(riskForeground);
        binding.tvRiskLevel.setBackgroundTintList(ColorStateList.valueOf(riskColor));
        binding.tvRiskHint.setText(label + " | " + hint);
        binding.tvThunderstormMetric.setText(summary);
        binding.tvThunderstormMetric.setTextColor(riskColor);
    }

    private int parseLevel(String value) {
        try {
            return Math.max(1, Math.min(9, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private int resolveResultColor(String result) {
        if (result.contains("适宜")) {
            return ContextCompat.getColor(this, R.color.ui_success);
        }
        if (result.contains("谨慎") || result.contains("预警")) {
            return ContextCompat.getColor(this, R.color.ui_warning);
        }
        return ContextCompat.getColor(this, R.color.ui_error);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}

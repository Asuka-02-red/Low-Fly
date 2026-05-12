package com.example.low_altitudereststop.feature.compliance;

import android.os.Bundle;
import android.widget.Toast;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.math.BigDecimal;

/**
 * 新增/编辑禁飞区Activity。
 * <p>
 * 提供禁飞区名称、类型、中心坐标、管控半径、生效时段、原因等字段的表单录入，
 * 支持新建和编辑两种模式，提交前通过NoFlyZoneFormValidator进行校验。
 * </p>
 */
public class AddNoFlyZoneActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_ZONE_ID = "zone_id";

    private FlightManagementRepository repository;
    private TextInputEditText etName;
    private TextInputEditText etType;
    private TextInputEditText etRadius;
    private TextInputEditText etCenterLat;
    private TextInputEditText etCenterLng;
    private TextInputEditText etEffectiveStart;
    private TextInputEditText etEffectiveEnd;
    private TextInputEditText etReason;
    private TextInputEditText etDesc;
    private String editingZoneId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_no_fly_zone);
        repository = new FlightManagementRepository(this);

        etName = findViewById(R.id.et_zone_name);
        etType = findViewById(R.id.et_zone_type);
        etRadius = findViewById(R.id.et_radius);
        etCenterLat = findViewById(R.id.et_center_lat);
        etCenterLng = findViewById(R.id.et_center_lng);
        etEffectiveStart = findViewById(R.id.et_effective_start);
        etEffectiveEnd = findViewById(R.id.et_effective_end);
        etReason = findViewById(R.id.et_reason);
        etDesc = findViewById(R.id.et_description);
        MaterialButton btnSubmit = findViewById(R.id.btn_submit);
        editingZoneId = getIntent().getStringExtra(EXTRA_ZONE_ID);
        bindEditMode();

        btnSubmit.setOnClickListener(v -> {
            FlightManagementModels.NoFlyZoneRecord record = buildRecord();
            NoFlyZoneFormValidator.Result result = NoFlyZoneFormValidator.validate(record);
            if (!result.isValid()) {
                Toast.makeText(this, firstError(result), Toast.LENGTH_SHORT).show();
                return;
            }
            repository.saveZone(record);
            Toast.makeText(this, editingZoneId == null ? "禁飞区添加成功" : "禁飞区更新成功", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void bindEditMode() {
        if (editingZoneId == null || editingZoneId.trim().isEmpty()) {
            return;
        }
        FlightManagementModels.NoFlyZoneRecord zone = repository.getZone(editingZoneId);
        if (zone == null) {
            return;
        }
        etName.setText(zone.name);
        etType.setText(zone.zoneType);
        etRadius.setText(String.valueOf(zone.radius));
        etCenterLat.setText(zone.centerLat == null ? "" : zone.centerLat.toPlainString());
        etCenterLng.setText(zone.centerLng == null ? "" : zone.centerLng.toPlainString());
        etEffectiveStart.setText(zone.effectiveStart);
        etEffectiveEnd.setText(zone.effectiveEnd);
        etReason.setText(zone.reason);
        etDesc.setText(zone.description);
    }

    private FlightManagementModels.NoFlyZoneRecord buildRecord() {
        FlightManagementModels.NoFlyZoneRecord record = new FlightManagementModels.NoFlyZoneRecord();
        FlightManagementModels.NoFlyZoneRecord existing = repository.getZone(editingZoneId);
        record.id = editingZoneId;
        record.builtIn = existing != null && existing.builtIn;
        record.name = text(etName);
        record.zoneType = text(etType);
        record.radius = parseInt(text(etRadius));
        record.centerLat = parseDecimal(text(etCenterLat));
        record.centerLng = parseDecimal(text(etCenterLng));
        record.effectiveStart = text(etEffectiveStart);
        record.effectiveEnd = text(etEffectiveEnd);
        record.reason = text(etReason);
        record.description = text(etDesc);
        return record;
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstError(NoFlyZoneFormValidator.Result result) {
        String[] fields = new String[]{"name", "zoneType", "centerLat", "centerLng", "radius", "effectiveStart", "effectiveEnd", "effectiveRange", "reason"};
        for (String field : fields) {
            String error = result.errorFor(field);
            if (error != null) {
                return error;
            }
        }
        return "请检查禁飞区信息";
    }
}

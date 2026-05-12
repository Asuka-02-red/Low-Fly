package com.example.low_altitudereststop.feature.auth;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.low_altitudereststop.core.ui.EdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityEnterpriseMaterialsBinding;

/**
 * 企业资质材料上传Activity。
 * <p>
 * 企业注册流程中的子页面，用于填写企业名称、统一信用代码、
 * 联系人信息，并选择上传营业执照文件（图片或PDF），
 * 填写完成后将数据通过Intent回传给注册页。
 * </p>
 */
public class EnterpriseMaterialsActivity extends EdgeToEdgeActivity {

    public static final String EXTRA_ENTERPRISE_NAME = "extra_enterprise_name";
    public static final String EXTRA_CREDIT_CODE = "extra_credit_code";
    public static final String EXTRA_CONTACT_NAME = "extra_contact_name";
    public static final String EXTRA_CONTACT_PHONE = "extra_contact_phone";
    public static final String EXTRA_LICENSE_URI = "extra_license_uri";

    private ActivityEnterpriseMaterialsBinding binding;
    private ActivityResultLauncher<Intent> licensePickerLauncher;
    private String selectedLicenseUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEnterpriseMaterialsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        licensePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null || result.getData().getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    selectedLicenseUri = uri.toString();
                    binding.tvLicenseFile.setText(resolveDisplayName(uri));
                });

        bindIntentData();
        binding.btnPickLicense.setOnClickListener(v -> openLicensePicker());
        binding.btnSaveMaterials.setOnClickListener(v -> saveMaterials());
    }

    private void bindIntentData() {
        Intent intent = getIntent();
        binding.etEnterpriseName.setText(intent.getStringExtra(EXTRA_ENTERPRISE_NAME));
        binding.etCreditCode.setText(intent.getStringExtra(EXTRA_CREDIT_CODE));
        binding.etContactName.setText(intent.getStringExtra(EXTRA_CONTACT_NAME));
        binding.etContactPhone.setText(intent.getStringExtra(EXTRA_CONTACT_PHONE));
        selectedLicenseUri = safeText(intent.getStringExtra(EXTRA_LICENSE_URI));
        if (!selectedLicenseUri.isEmpty()) {
            binding.tvLicenseFile.setText(resolveDisplayName(Uri.parse(selectedLicenseUri)));
        }
    }

    private void openLicensePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        licensePickerLauncher.launch(Intent.createChooser(intent, "选择营业执照"));
    }

    private void saveMaterials() {
        String enterpriseName = safeText(binding.etEnterpriseName.getText());
        String creditCode = safeText(binding.etCreditCode.getText());
        String contactName = safeText(binding.etContactName.getText());
        String contactPhone = safeText(binding.etContactPhone.getText());
        if (enterpriseName.isEmpty() || creditCode.isEmpty() || contactName.isEmpty() || contactPhone.isEmpty()) {
            toast("请填写完整企业材料");
            return;
        }
        if (selectedLicenseUri.isEmpty()) {
            toast("请上传营业执照");
            return;
        }

        Intent data = new Intent();
        data.putExtra(EXTRA_ENTERPRISE_NAME, enterpriseName);
        data.putExtra(EXTRA_CREDIT_CODE, creditCode);
        data.putExtra(EXTRA_CONTACT_NAME, contactName);
        data.putExtra(EXTRA_CONTACT_PHONE, contactPhone);
        data.putExtra(EXTRA_LICENSE_URI, selectedLicenseUri);
        setResult(RESULT_OK, data);
        finish();
    }

    private String resolveDisplayName(Uri uri) {
        if (uri == null) {
            return "未选择文件";
        }
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex >= 0) {
                        return cursor.getString(columnIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        String segment = uri.getLastPathSegment();
        return segment == null || segment.isEmpty() ? "已选择营业执照" : segment;
    }

    private String safeText(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

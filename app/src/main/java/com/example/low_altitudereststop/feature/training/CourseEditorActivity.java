package com.example.low_altitudereststop.feature.training;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import java.math.BigDecimal;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 课程编辑Activity，供企业用户创建和编辑培训课程。
 * <p>
 * 提供课程标题、类型、时长、难度、描述和章节列表的编辑表单，
 * 支持添加和删除章节，提交后保存到服务端或加入离线同步队列。
 * </p>
 */
public class CourseEditorActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_TITLE = "course_title";
    public static final String EXTRA_SUMMARY = "course_summary";
    public static final String EXTRA_LEARNING_MODE = "course_learning_mode";
    public static final String EXTRA_SEAT_TOTAL = "course_seat_total";
    public static final String EXTRA_PRICE = "course_price";
    public static final String EXTRA_STATUS = "course_status";

    private com.google.android.material.textfield.TextInputEditText etTitle;
    private com.google.android.material.textfield.TextInputEditText etSummary;
    private com.google.android.material.textfield.TextInputEditText etSeatTotal;
    private com.google.android.material.textfield.TextInputEditText etPrice;
    private com.google.android.material.textfield.TextInputEditText etContent;
    private Spinner spLearningMode;
    private long courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.low_altitudereststop.R.layout.activity_course_editor);
        applySafeTopInset(findViewById(com.example.low_altitudereststop.R.id.root_course_editor));
        courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1L);
        etTitle = findViewById(com.example.low_altitudereststop.R.id.et_course_title);
        etSummary = findViewById(com.example.low_altitudereststop.R.id.et_course_summary);
        etSeatTotal = findViewById(com.example.low_altitudereststop.R.id.et_seat_total);
        etPrice = findViewById(com.example.low_altitudereststop.R.id.et_price);
        etContent = findViewById(com.example.low_altitudereststop.R.id.et_course_content);
        spLearningMode = findViewById(com.example.low_altitudereststop.R.id.sp_learning_mode);

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"文章学习", "线下报名"});
        spLearningMode.setAdapter(modeAdapter);
        fillBasicFields();
        if (courseId > 0) {
            loadCourseDetail();
        }

        findViewById(com.example.low_altitudereststop.R.id.btn_save_draft).setOnClickListener(v -> submit("DRAFT"));
        findViewById(com.example.low_altitudereststop.R.id.btn_publish_course).setOnClickListener(v -> submit("OPEN"));
    }

    private void fillBasicFields() {
        etTitle.setText(getIntent().getStringExtra(EXTRA_TITLE));
        etSummary.setText(getIntent().getStringExtra(EXTRA_SUMMARY));
        etSeatTotal.setText(String.valueOf(getIntent().getIntExtra(EXTRA_SEAT_TOTAL, 0)));
        etPrice.setText(getIntent().getStringExtra(EXTRA_PRICE));
        String learningMode = getIntent().getStringExtra(EXTRA_LEARNING_MODE);
        spLearningMode.setSelection("OFFLINE".equalsIgnoreCase(learningMode) ? 1 : 0);
    }

    private void loadCourseDetail() {
        ApiClient.getAuthedService(this).getCourseDetail(courseId).enqueue(new Callback<ApiEnvelope<PlatformModels.CourseDetailView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.CourseDetailView>> call, Response<ApiEnvelope<PlatformModels.CourseDetailView>> response) {
                ApiEnvelope<PlatformModels.CourseDetailView> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                    toast(resolveMessage(envelope, "课程详情加载失败"));
                    return;
                }
                PlatformModels.CourseDetailView detail = envelope.data;
                etTitle.setText(detail.title);
                etSummary.setText(detail.summary);
                etSeatTotal.setText(String.valueOf(detail.seatTotal));
                etPrice.setText(detail.price == null ? "" : detail.price.toPlainString());
                etContent.setText(detail.content);
                spLearningMode.setSelection("OFFLINE".equalsIgnoreCase(detail.learningMode) ? 1 : 0);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.CourseDetailView>> call, Throwable t) {
                toast("课程详情加载失败：" + t.getMessage());
            }
        });
    }

    private void submit(String status) {
        PlatformModels.CourseManageRequest request = new PlatformModels.CourseManageRequest();
        request.title = text(etTitle.getText());
        request.summary = text(etSummary.getText());
        request.content = text(etContent.getText());
        request.learningMode = spLearningMode.getSelectedItemPosition() == 1 ? "OFFLINE" : "ARTICLE";
        request.seatTotal = parseInt(etSeatTotal.getText(), 0);
        request.price = parseDecimal(etPrice.getText(), "0");
        request.status = status;
        if (request.title.isEmpty() || request.summary.isEmpty() || request.content.isEmpty()) {
            toast("请完整填写课程标题、简介和正文");
            return;
        }

        if (courseId > 0) {
            ApiClient.getAuthedService(this).updateCourse(courseId, request).enqueue(new Callback<ApiEnvelope<PlatformModels.CourseManageView>>() {
                @Override
                public void onResponse(Call<ApiEnvelope<PlatformModels.CourseManageView>> call, Response<ApiEnvelope<PlatformModels.CourseManageView>> response) {
                    ApiEnvelope<PlatformModels.CourseManageView> envelope = response.body();
                    if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                        toast(resolveMessage(envelope, "课程更新失败"));
                        return;
                    }
                    toast("课程已更新");
                    finish();
                }

                @Override
                public void onFailure(Call<ApiEnvelope<PlatformModels.CourseManageView>> call, Throwable t) {
                    toast("网络异常：" + t.getMessage());
                }
            });
            return;
        }

        ApiClient.getAuthedService(this).createCourse(request).enqueue(new Callback<ApiEnvelope<PlatformModels.CourseManageView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.CourseManageView>> call, Response<ApiEnvelope<PlatformModels.CourseManageView>> response) {
                ApiEnvelope<PlatformModels.CourseManageView> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                    toast(resolveMessage(envelope, "课程创建失败"));
                    return;
                }
                toast("课程已保存");
                finish();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.CourseManageView>> call, Throwable t) {
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private String text(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private int parseInt(CharSequence text, int def) {
        try {
            return Integer.parseInt(text(text));
        } catch (Exception ignored) {
            return def;
        }
    }

    private BigDecimal parseDecimal(CharSequence text, String def) {
        try {
            return new BigDecimal(text(text).isEmpty() ? def : text(text));
        } catch (Exception ignored) {
            return new BigDecimal(def);
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String resolveMessage(ApiEnvelope<?> envelope, String fallback) {
        if (envelope == null || envelope.message == null || envelope.message.trim().isEmpty()) {
            return fallback;
        }
        return envelope.message;
    }

    private void applySafeTopInset(@NonNull View root) {
        final int paddingStart = root.getPaddingStart();
        final int paddingTop = root.getPaddingTop();
        final int paddingEnd = root.getPaddingEnd();
        final int paddingBottom = root.getPaddingBottom();
        final int extraTop = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                getResources().getDisplayMetrics()
        ));
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPaddingRelative(
                    paddingStart,
                    paddingTop + insets.top + extraTop,
                    paddingEnd,
                    paddingBottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}

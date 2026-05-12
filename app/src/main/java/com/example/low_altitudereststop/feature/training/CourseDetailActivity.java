package com.example.low_altitudereststop.feature.training;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.google.android.material.button.MaterialButton;
import com.example.low_altitudereststop.ui.UserRole;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 课程详情Activity，展示课程的完整信息。
 * <p>
 * 从API或演示数据源获取课程详情，展示课程标题、类型、时长、
 * 难度、描述、章节列表和进度，支持报名/开始学习操作，
 * 企业用户可查看已报名学员列表。
 * </p>
 */
public class CourseDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_COURSE_PREVIEW_JSON = "course_preview_json";

    private long courseId;
    private PlatformModels.CourseDetailView courseDetail;
    private MaterialButton btnAction;
    private TrainingRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);
        courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1L);
        btnAction = findViewById(R.id.btn_action);
        repository = TrainingRepository.getInstance(this);
        btnAction.setOnClickListener(v -> toggleSubscription());
        loadDetail();
    }

    public static void putCoursePreview(Intent intent, PlatformModels.CourseView course) {
        if (intent == null || course == null) {
            return;
        }
        intent.putExtra(EXTRA_COURSE_PREVIEW_JSON, new Gson().toJson(course));
    }

    private void loadDetail() {
        PlatformModels.CourseDetailView demoDetail = findDemoDetailOrNull();
        PlatformModels.CourseDetailView previewDetail = readPreviewDetail();
        PlatformModels.CourseDetailView cachedDetail = courseId > 0 ? repository.getCourseDetail(courseId) : null;
        if (courseId <= 0) {
            PlatformModels.CourseDetailView localDetail = demoDetail != null ? demoDetail : previewDetail;
            if (localDetail == null) {
                toast("课程ID无效");
                finish();
                return;
            }
            courseDetail = localDetail;
            render();
            return;
        }
        PlatformModels.CourseDetailView immediateDetail = demoDetail != null
                ? demoDetail
                : (cachedDetail != null ? cachedDetail : previewDetail);
        if (immediateDetail != null) {
            courseDetail = immediateDetail;
            render();
        }
        ApiClient.getAuthedService(this).getCourseDetail(courseId).enqueue(new Callback<ApiEnvelope<PlatformModels.CourseDetailView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.CourseDetailView>> call, Response<ApiEnvelope<PlatformModels.CourseDetailView>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    if (courseDetail != null) {
                        return;
                    }
                    PlatformModels.CourseDetailView fallback = findAnyDemoDetailOrNull();
                    if (fallback != null) {
                        courseDetail = courseId > 0 ? repository.getCourseDetail(courseId) : fallback;
                        if (courseDetail == null) {
                            courseDetail = fallback;
                        }
                        render();
                        return;
                    }
                    toast("课程详情加载失败");
                    finish();
                    return;
                }
                repository.applyRemoteDetail(response.body().data);
                courseDetail = repository.getCourseDetail(courseId);
                render();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.CourseDetailView>> call, Throwable t) {
                if (courseDetail != null) {
                    return;
                }
                PlatformModels.CourseDetailView fallback = findAnyDemoDetailOrNull();
                if (fallback != null) {
                    courseDetail = fallback;
                    render();
                    return;
                }
                toast("网络异常：" + t.getMessage());
                finish();
            }
        });
    }

    private void render() {
        boolean pilotRole = isPilotRole();
        ((TextView) findViewById(R.id.tv_title)).setText(courseDetail.title == null ? "-" : courseDetail.title);
        ((TextView) findViewById(R.id.tv_summary)).setText(courseDetail.summary == null ? "暂无课程简介" : courseDetail.summary);
        ((TextView) findViewById(R.id.tv_mode)).setText(safe(courseDetail.category));
        ((TextView) findViewById(R.id.tv_status)).setText("状态：" + displayStatus(courseDetail.status));
        ((TextView) findViewById(R.id.tv_institution)).setText("开课单位：" + safe(courseDetail.institutionName));
        ((TextView) findViewById(R.id.tv_price)).setText(courseDetail.price == null ? "免费" : "￥" + courseDetail.price.toPlainString());
        ((TextView) findViewById(R.id.tv_seat)).setText(courseDetail.seatAvailable + "/" + courseDetail.seatTotal);
        ((TextView) findViewById(R.id.tv_meta)).setText(displayMode(courseDetail.learningMode)
                + "    浏览量 " + courseDetail.browseCount
                + "    订阅数 " + courseDetail.enrollCount
                + "    我的状态 " + enrollmentLabel());
        ((TextView) findViewById(R.id.tv_content)).setText(courseDetail.content == null ? "暂无课程正文" : courseDetail.content);
        updateActionButton();
    }

    private void toggleSubscription() {
        if (courseDetail == null) {
            return;
        }
        Long detailId = courseDetail.id == null ? courseId : courseDetail.id;
        if (detailId == null || detailId <= 0L) {
            toast("课程ID无效");
            return;
        }
        TrainingRepository.SubscriptionToggleResult result = repository.toggleSubscription(detailId);
        if (!result.success || result.detail == null) {
            toast(result.message);
            return;
        }
        courseDetail = result.detail;
        render();
        toast(result.message);
    }

    private String displayMode(String learningMode) {
        if ("OFFLINE".equalsIgnoreCase(learningMode)) {
            return "线下学习";
        }
        return "文章学习";
    }

    private String displayStatus(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            return isPilotRole() ? "可订阅" : "报名中";
        }
        if ("DRAFT".equalsIgnoreCase(status)) {
            return "草稿";
        }
        if ("LEARNING".equalsIgnoreCase(status)) {
            return "学习中";
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "已完成";
        }
        return safe(status);
    }

    private String enrollmentLabel() {
        if (courseDetail == null || !courseDetail.enrolled) {
            return "未订阅";
        }
        if (courseDetail.enrollmentStatus == null || courseDetail.enrollmentStatus.trim().isEmpty()) {
            return "已订阅";
        }
        if ("ENROLLED".equalsIgnoreCase(courseDetail.enrollmentStatus)) {
            return "已订阅";
        }
        if ("LEARNING".equalsIgnoreCase(courseDetail.enrollmentStatus)) {
            return "学习中";
        }
        if ("COMPLETED".equalsIgnoreCase(courseDetail.enrollmentStatus)) {
            return "已完成";
        }
        return courseDetail.enrollmentStatus;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private PlatformModels.CourseDetailView findDemoDetailOrNull() {
        return DemoCourseCatalog.findDemoDetail(courseId);
    }

    private PlatformModels.CourseDetailView findAnyDemoDetailOrNull() {
        PlatformModels.CourseDetailView exact = DemoCourseCatalog.findDemoDetail(courseId);
        if (exact != null) {
            return exact;
        }
        return DemoCourseCatalog.findFirstDemoDetail();
    }

    private PlatformModels.CourseDetailView readPreviewDetail() {
        try {
            String json = getIntent().getStringExtra(EXTRA_COURSE_PREVIEW_JSON);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            PlatformModels.CourseView preview = new Gson().fromJson(json, PlatformModels.CourseView.class);
            if (preview == null) {
                return null;
            }
            PlatformModels.CourseDetailView detail = new PlatformModels.CourseDetailView();
            detail.id = preview.id;
            detail.title = preview.title;
            detail.summary = preview.summary;
            detail.content = preview.summary == null || preview.summary.trim().isEmpty()
                    ? "正在同步课程正文..."
                    : preview.summary;
            detail.learningMode = preview.learningMode;
            detail.institutionName = preview.institutionName;
            detail.seatAvailable = preview.seatAvailable;
            detail.enrollCount = preview.enrollCount;
            detail.browseCount = preview.browseCount;
            detail.price = preview.price;
            detail.status = preview.status;
            detail.seatTotal = Math.max(preview.seatAvailable, preview.seatAvailable + preview.enrollCount);
            detail.enrolled = false;
            return detail;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isPilotRole() {
        return UserRole.from(new SessionStore(this).getCachedUser().role) == UserRole.PILOT;
    }

    private String enrolledActionLabel(boolean pilotRole) {
        return pilotRole ? "已订阅，再次点击取消" : "已加入学习";
    }

    private String pendingActionLabel(boolean offline, boolean pilotRole) {
        if (!pilotRole) {
            return offline ? "报名线下学习" : "加入文章学习";
        }
        return offline ? "订阅线下课程" : "订阅课程";
    }

    private void updateActionButton() {
        boolean subscribed = courseDetail != null && courseDetail.enrolled;
        int background = ContextCompat.getColor(this, subscribed ? R.color.ui_success : R.color.ui_info);
        btnAction.setBackgroundTintList(ColorStateList.valueOf(background));
        btnAction.setTextColor(ContextCompat.getColor(this, R.color.white));
        btnAction.setText(subscribed
                ? enrolledActionLabel(isPilotRole())
                : pendingActionLabel("OFFLINE".equalsIgnoreCase(courseDetail.learningMode), isPilotRole()));
        btnAction.setEnabled(true);
    }
}

package com.example.low_altitudereststop.feature.training;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.google.android.material.button.MaterialButton;
import com.example.low_altitudereststop.ui.UserRole;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_COURSE_ID = "course_id";

    private long courseId;
    private PlatformModels.CourseDetailView courseDetail;
    private MaterialButton btnAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);
        courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1L);
        btnAction = findViewById(R.id.btn_action);
        btnAction.setOnClickListener(v -> enroll());
        loadDetail();
    }

    private void loadDetail() {
        PlatformModels.CourseDetailView demoDetail = findDemoDetailOrNull();
        if (courseId <= 0) {
            if (demoDetail == null) {
                toast("课程ID无效");
                finish();
                return;
            }
            courseDetail = demoDetail;
            render();
            return;
        }
        if (demoDetail != null) {
            courseDetail = demoDetail;
            render();
        }
        ApiClient.getAuthedService(this).getCourseDetail(courseId).enqueue(new Callback<ApiEnvelope<PlatformModels.CourseDetailView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.CourseDetailView>> call, Response<ApiEnvelope<PlatformModels.CourseDetailView>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    if (courseDetail != null) {
                        return;
                    }
                    if (demoDetail != null) {
                        courseDetail = demoDetail;
                        render();
                        return;
                    }
                    toast("课程详情加载失败");
                    finish();
                    return;
                }
                courseDetail = response.body().data;
                render();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.CourseDetailView>> call, Throwable t) {
                if (courseDetail != null) {
                    return;
                }
                if (demoDetail != null) {
                    courseDetail = demoDetail;
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
        ((TextView) findViewById(R.id.tv_mode)).setText(displayMode(courseDetail.learningMode));
        ((TextView) findViewById(R.id.tv_status)).setText("状态：" + displayStatus(courseDetail.status));
        ((TextView) findViewById(R.id.tv_institution)).setText("开课单位：" + safe(courseDetail.institutionName));
        ((TextView) findViewById(R.id.tv_price)).setText(courseDetail.price == null ? "免费" : "￥" + courseDetail.price.toPlainString());
        ((TextView) findViewById(R.id.tv_seat)).setText(courseDetail.seatAvailable + "/" + courseDetail.seatTotal);
        ((TextView) findViewById(R.id.tv_meta)).setText("浏览量 " + courseDetail.browseCount
                + "    " + (pilotRole ? "订阅数 " : "报名数 ") + courseDetail.enrollCount
                + "    我的状态 " + enrollmentLabel());
        ((TextView) findViewById(R.id.tv_content)).setText(courseDetail.content == null ? "暂无课程正文" : courseDetail.content);
        boolean offline = "OFFLINE".equalsIgnoreCase(courseDetail.learningMode);
        if (courseDetail.enrolled) {
            btnAction.setText(courseDetail.enrollmentNo == null || courseDetail.enrollmentNo.trim().isEmpty()
                    ? enrolledActionLabel(pilotRole)
                    : enrolledActionLabel(pilotRole) + " · " + courseDetail.enrollmentNo);
            btnAction.setEnabled(false);
            return;
        }
        btnAction.setText(pendingActionLabel(offline, pilotRole));
        btnAction.setEnabled(!offline || courseDetail.seatAvailable > 0);
    }

    private void enroll() {
        if (courseDetail == null) {
            return;
        }
        boolean pilotRole = isPilotRole();
        if (courseId <= 0) {
            if (pilotRole) {
                markCourseAsSubscribed();
                toast("订阅成功");
                return;
            }
            toast("当前场景课程不提交真实报名");
            return;
        }
        ApiClient.getAuthedService(this).enroll(courseId).enqueue(new Callback<ApiEnvelope<PlatformModels.EnrollmentResult>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.EnrollmentResult>> call, Response<ApiEnvelope<PlatformModels.EnrollmentResult>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    if (pilotRole) {
                        markCourseAsSubscribed();
                        toast("订阅成功");
                        return;
                    }
                    toast("加入学习失败");
                    return;
                }
                PlatformModels.EnrollmentResult result = response.body().data;
                if (pilotRole) {
                    courseDetail.enrolled = true;
                    courseDetail.enrollmentStatus = "ENROLLED";
                    courseDetail.enrollmentNo = result.enrollmentNo;
                    render();
                    toast("订阅成功");
                    return;
                }
                toast("操作成功：" + (result.enrollmentNo == null ? "" : result.enrollmentNo));
                loadDetail();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.EnrollmentResult>> call, Throwable t) {
                if (pilotRole) {
                    markCourseAsSubscribed();
                    toast("订阅成功");
                    return;
                }
                toast("网络异常：" + t.getMessage());
            }
        });
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
        boolean pilotRole = isPilotRole();
        if (courseDetail == null || !courseDetail.enrolled) {
            return pilotRole ? "未订阅" : "未报名";
        }
        if (courseDetail.enrollmentStatus == null || courseDetail.enrollmentStatus.trim().isEmpty()) {
            return pilotRole ? "已订阅" : "已报名";
        }
        if ("ENROLLED".equalsIgnoreCase(courseDetail.enrollmentStatus)) {
            return pilotRole ? "已订阅" : "已报名";
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

    private boolean isPilotRole() {
        return UserRole.from(new SessionStore(this).getCachedUser().role) == UserRole.PILOT;
    }

    private String enrolledActionLabel(boolean pilotRole) {
        return pilotRole ? "已订阅课程" : "已加入学习";
    }

    private String pendingActionLabel(boolean offline, boolean pilotRole) {
        if (!pilotRole) {
            return offline ? "报名线下学习" : "加入文章学习";
        }
        return offline ? "订阅线下课程" : "订阅课程";
    }

    private void markCourseAsSubscribed() {
        if (courseDetail == null) {
            return;
        }
        courseDetail.enrolled = true;
        courseDetail.enrollmentStatus = "ENROLLED";
        if (courseDetail.enrollmentNo == null || courseDetail.enrollmentNo.trim().isEmpty()) {
            courseDetail.enrollmentNo = "SUB-" + System.currentTimeMillis();
        }
        render();
    }
}

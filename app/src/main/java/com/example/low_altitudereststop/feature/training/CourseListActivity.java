package com.example.low_altitudereststop.feature.training;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.ui.PageStateController;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.databinding.ActivityCourseListBinding;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 课程列表Activity，展示企业管理的课程列表。
 * <p>
 * 企业用户查看已创建的所有课程，支持添加新课程（跳转CourseEditorActivity）、
 * 编辑课程和查看课程详情，使用ManagedCourseAdapter展示管理视角的课程信息。
 * </p>
 */
public class CourseListActivity extends NavigableEdgeToEdgeActivity {

    private ActivityCourseListBinding binding;
    private CourseAdapter adapter;
    private PageStateController stateController;
    private TrainingRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = TrainingRepository.getInstance(this);

        adapter = new CourseAdapter(new CourseAdapter.OnCourseClickListener() {
            @Override
            public void onOpenCourse(PlatformModels.CourseView course) {
                openCourse(course);
            }

            @Override
            public void onEnrollCourse(PlatformModels.CourseView course) {
                enrollCourse(course);
            }
        });
        stateController = new PageStateController(
                binding.layoutCourseState,
                binding.layoutCourseContent,
                binding.layoutCourseState.findViewById(com.example.low_altitudereststop.core.ui.R.id.progress_page_state),
                binding.layoutCourseState.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_title),
                binding.layoutCourseState.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_desc),
                binding.layoutCourseState.findViewById(com.example.low_altitudereststop.core.ui.R.id.btn_page_state_retry)
        );
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.layoutCourseContent.setOnRefreshListener(this::loadCourses);

        binding.layoutCourseContent.setRefreshing(true);
        stateController.showLoading("正在加载课程", "正在同步课程目录与学习进度。");
        loadCourses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && adapter.getItemCount() == 0) {
            loadCourses();
        }
    }

    private void loadCourses() {
        renderCourses(repository.getCourses());
        ApiClient.getAuthedService(this).listCourses().enqueue(new Callback<ApiEnvelope<List<PlatformModels.CourseView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.CourseView>>> call, Response<ApiEnvelope<List<PlatformModels.CourseView>>> response) {
                binding.layoutCourseContent.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    return;
                }
                repository.applyRemoteCourses(response.body().data);
                renderCourses(repository.getCourses());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.CourseView>>> call, Throwable t) {
                binding.layoutCourseContent.setRefreshing(false);
            }
        });
    }

    private void renderCourses(List<PlatformModels.CourseView> courses) {
        adapter.submit(courses);
        binding.tvTitle.setText("课程订阅 · 已订阅 " + repository.getSubscribedCount() + " 门");
        if (courses == null || courses.isEmpty()) {
            stateController.showEmpty("暂无课程", "当前没有可展示的课程内容，请稍后重试。", "重新加载", this::loadCourses);
            return;
        }
        stateController.showContent();
    }

    private void openCourse(PlatformModels.CourseView course) {
        if (course == null || course.id == null) {
            toast("课程ID无效");
            return;
        }
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.id);
        CourseDetailActivity.putCoursePreview(intent, course);
        startActivity(intent);
    }

    private void enrollCourse(PlatformModels.CourseView course) {
        openCourse(course);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

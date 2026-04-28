package com.example.low_altitudereststop.feature.training;

import android.content.Intent;
import android.os.Bundle;
import com.example.low_altitudereststop.BuildConfig;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.ui.PageStateController;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.databinding.ActivityCourseListBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseListActivity extends NavigableEdgeToEdgeActivity {

    private ActivityCourseListBinding binding;
    private CourseAdapter adapter;
    private PageStateController stateController;
    private boolean lastRenderUsedFallbackData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new CourseAdapter(this::openCourse);
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

    private void loadCourses() {
        FileCache cache = new FileCache(this);
        ApiClient.getAuthedService(this).listCourses().enqueue(new Callback<ApiEnvelope<List<PlatformModels.CourseView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.CourseView>>> call, Response<ApiEnvelope<List<PlatformModels.CourseView>>> response) {
                binding.layoutCourseContent.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    List<PlatformModels.CourseView> cached = readCache(cache, "courses.json");
                    if (cached != null) {
                        renderCourses(applyFallbackCourses(cached));
                        return;
                    }
                    renderCourses(applyFallbackCourses(null));
                    return;
                }
                lastRenderUsedFallbackData = false;
                renderCourses(response.body().data);
                cache.write("courses.json", new Gson().toJson(response.body().data));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.CourseView>>> call, Throwable t) {
                binding.layoutCourseContent.setRefreshing(false);
                List<PlatformModels.CourseView> cached = readCache(cache, "courses.json");
                if (cached != null) {
                    renderCourses(applyFallbackCourses(cached));
                    return;
                }
                renderCourses(applyFallbackCourses(null));
            }
        });
    }

    private void renderCourses(List<PlatformModels.CourseView> courses) {
        adapter.submit(courses);
        if (courses == null || courses.isEmpty()) {
            String desc = lastRenderUsedFallbackData
                    ? "课程暂时不可用，已展示推荐课程。"
                    : "当前没有可展示的课程内容，请稍后重试。";
            stateController.showEmpty("暂无课程", desc, "重新加载", this::loadCourses);
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
        startActivity(intent);
    }

    private void enqueueEnroll(Long courseId) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("courseId", courseId == null ? -1L : courseId);
        OutboxSyncManager.enqueue(this, "ENROLL_COURSE", payload);
        toast("已加入离线同步队列");
    }

    private List<PlatformModels.CourseView> readCache(FileCache cache, String name) {
        try {
            String json = cache.read(name);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            Type type = new TypeToken<List<PlatformModels.CourseView>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<PlatformModels.CourseView> applyFallbackCourses(List<PlatformModels.CourseView> courses) {
        if (courses == null || courses.isEmpty()) {
            lastRenderUsedFallbackData = true;
            return DemoCourseCatalog.mergeWithDemo(courses);
        }
        lastRenderUsedFallbackData = false;
        return courses;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

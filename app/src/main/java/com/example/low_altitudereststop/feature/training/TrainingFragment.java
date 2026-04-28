package com.example.low_altitudereststop.feature.training;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.low_altitudereststop.BuildConfig;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.ui.UserRole;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingFragment extends Fragment {

    private static final String PILOT_COURSE_CACHE_NAME = "pilot_courses.json";
    private static final String ENTERPRISE_COURSE_CACHE_NAME = "enterprise_courses.json";

    private ManagedCourseAdapter adapter;
    private CourseAdapter pilotAdapter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;
    private TextView tvTotalCourses;
    private TextView tvTotalViews;
    private TextView tvTotalEnrolls;
    private TextView tvTitle;
    private BarChart chartTraining;
    private ActivityResultLauncher<Intent> editorLauncher;

    public TrainingFragment() {
        super(R.layout.fragment_training);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyNotchPadding(view);
        UserRole role = UserRole.from(new SessionStore(requireContext()).getCachedUser().role);
        tvTitle = view.findViewById(R.id.tv_title);
        swipe = view.findViewById(R.id.swipe);
        tvTotalCourses = view.findViewById(R.id.tv_total_courses);
        tvTotalViews = view.findViewById(R.id.tv_total_views);
        tvTotalEnrolls = view.findViewById(R.id.tv_total_enrolls);
        chartTraining = view.findViewById(R.id.chart_training);
        androidx.recyclerview.widget.RecyclerView recycler = view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (role == UserRole.PILOT) {
            bindPilotTrainingPage(view, recycler);
            return;
        }
        if (role != UserRole.ENTERPRISE && role != UserRole.ADMIN) {
            toast("当前角色暂无课程权限");
            return;
        }
        adapter = new ManagedCourseAdapter(new ManagedCourseAdapter.Listener() {
            @Override
            public void onEdit(PlatformModels.CourseManageView course) {
                openEditor(course);
            }

            @Override
            public void onPublish(PlatformModels.CourseManageView course) {
                publishCourse(course);
            }

            @Override
            public void onDelete(PlatformModels.CourseManageView course) {
                deleteCourse(course);
            }
        });
        recycler.setAdapter(adapter);
        editorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> loadCourses());
        view.findViewById(R.id.btn_add_course).setOnClickListener(v -> openEditor(null));
        swipe.setOnRefreshListener(this::loadCourses);
        swipe.setRefreshing(true);
        loadCourses();
    }

    private void bindPilotTrainingPage(@NonNull android.view.View view, @NonNull androidx.recyclerview.widget.RecyclerView recycler) {
        view.findViewById(R.id.btn_add_course).setVisibility(android.view.View.GONE);
        tvTitle.setText("我的课程");
        pilotAdapter = new CourseAdapter(this::openPilotCourse);
        recycler.setAdapter(pilotAdapter);
        swipe.setOnRefreshListener(this::loadPilotCourses);
        swipe.setRefreshing(true);
        loadPilotCourses();
    }

    private void loadPilotCourses() {
        FileCache cache = new FileCache(requireContext());
        ApiClient.getAuthedService(requireContext()).listCourses().enqueue(new Callback<ApiEnvelope<List<PlatformModels.CourseView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.CourseView>>> call, Response<ApiEnvelope<List<PlatformModels.CourseView>>> response) {
                swipe.setRefreshing(false);
                List<PlatformModels.CourseView> courses = null;
                if (response.isSuccessful() && response.body() != null) {
                    ApiEnvelope<List<PlatformModels.CourseView>> envelope = response.body();
                    if (envelope.code == 200 && envelope.data != null && !envelope.data.isEmpty()) {
                        courses = envelope.data;
                        cache.write(PILOT_COURSE_CACHE_NAME, new Gson().toJson(courses));
                    }
                }
                if (courses == null) {
                    courses = readPilotCache(cache);
                }
                renderPilotCourses(DemoCourseCatalog.mergeWithDemo(courses));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.CourseView>>> call, Throwable t) {
                swipe.setRefreshing(false);
                renderPilotCourses(DemoCourseCatalog.mergeWithDemo(readPilotCache(cache)));
            }
        });
    }

    private void renderPilotCourses(@Nullable List<PlatformModels.CourseView> items) {
        if (pilotAdapter != null) {
            pilotAdapter.submit(items);
        }
        int totalCourses = items == null ? 0 : items.size();
        int totalViews = 0;
        int totalEnrolls = 0;
        if (items != null) {
            for (PlatformModels.CourseView item : items) {
                totalViews += item.browseCount;
                totalEnrolls += item.enrollCount;
            }
        }
        tvTotalCourses.setText("课程 " + totalCourses);
        tvTotalViews.setText("浏览 " + totalViews);
        tvTotalEnrolls.setText("报名 " + totalEnrolls);
        renderTrainingChart(totalCourses, totalViews, totalEnrolls);
    }

    @Nullable
    private List<PlatformModels.CourseView> readPilotCache(@NonNull FileCache cache) {
        try {
            String json = cache.read(PILOT_COURSE_CACHE_NAME);
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

    @Nullable
    private List<PlatformModels.CourseManageView> readEnterpriseCache(@NonNull FileCache cache) {
        try {
            String json = cache.read(ENTERPRISE_COURSE_CACHE_NAME);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            Type type = new TypeToken<List<PlatformModels.CourseManageView>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void openPilotCourse(@Nullable PlatformModels.CourseView course) {
        if (course == null || course.id == null) {
            toast("课程ID无效");
            return;
        }
        Intent intent = new Intent(requireContext(), CourseDetailActivity.class);
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.id);
        startActivity(intent);
    }

    private void openEditor(@Nullable PlatformModels.CourseManageView course) {
        Intent intent = new Intent(requireContext(), CourseEditorActivity.class);
        if (course != null) {
            intent.putExtra(CourseEditorActivity.EXTRA_COURSE_ID, course.id == null ? -1L : course.id);
            intent.putExtra(CourseEditorActivity.EXTRA_TITLE, course.title);
            intent.putExtra(CourseEditorActivity.EXTRA_SUMMARY, course.summary);
            intent.putExtra(CourseEditorActivity.EXTRA_LEARNING_MODE, course.learningMode);
            intent.putExtra(CourseEditorActivity.EXTRA_SEAT_TOTAL, course.seatTotal);
            intent.putExtra(CourseEditorActivity.EXTRA_PRICE, course.price == null ? "" : course.price.toPlainString());
            intent.putExtra(CourseEditorActivity.EXTRA_STATUS, course.status);
        }
        editorLauncher.launch(intent);
    }

    private void loadCourses() {
        FileCache cache = new FileCache(requireContext());
        List<PlatformModels.CourseManageView> cached = readEnterpriseCache(cache);
        if (cached != null && !cached.isEmpty()) {
            adapter.submit(cached);
            renderMetrics(cached);
        }
        ApiClient.getAuthedService(requireContext()).listManagedCourses().enqueue(new Callback<ApiEnvelope<List<PlatformModels.CourseManageView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.CourseManageView>>> call, Response<ApiEnvelope<List<PlatformModels.CourseManageView>>> response) {
                swipe.setRefreshing(false);
                ApiEnvelope<List<PlatformModels.CourseManageView>> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                    if (cached == null || cached.isEmpty()) {
                        List<PlatformModels.CourseManageView> fallback = DemoCourseCatalog.buildManagedFallback(null);
                        adapter.submit(fallback);
                        renderMetrics(fallback);
                    }
                    return;
                }
                List<PlatformModels.CourseManageView> items = DemoCourseCatalog.buildManagedFallback(envelope.data);
                cache.write(ENTERPRISE_COURSE_CACHE_NAME, new Gson().toJson(items));
                adapter.submit(items);
                renderMetrics(items);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.CourseManageView>>> call, Throwable t) {
                swipe.setRefreshing(false);
                if (cached == null || cached.isEmpty()) {
                    List<PlatformModels.CourseManageView> fallback = DemoCourseCatalog.buildManagedFallback(null);
                    adapter.submit(fallback);
                    renderMetrics(fallback);
                }
            }
        });
    }

    private void publishCourse(@Nullable PlatformModels.CourseManageView course) {
        if (course == null || course.id == null) {
            toast("课程ID无效");
            return;
        }
        ApiClient.getAuthedService(requireContext()).publishCourse(course.id).enqueue(new Callback<ApiEnvelope<PlatformModels.CourseManageView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.CourseManageView>> call, Response<ApiEnvelope<PlatformModels.CourseManageView>> response) {
                ApiEnvelope<PlatformModels.CourseManageView> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.code != 200 || envelope.data == null) {
                    toast(resolveMessage(envelope, "发布失败"));
                    return;
                }
                toast("课程已发布");
                loadCourses();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.CourseManageView>> call, Throwable t) {
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private void deleteCourse(@Nullable PlatformModels.CourseManageView course) {
        if (course == null || course.id == null) {
            toast("课程ID无效");
            return;
        }
        ApiClient.getAuthedService(requireContext()).deleteCourse(course.id).enqueue(new Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<Void>> call, Response<ApiEnvelope<Void>> response) {
                ApiEnvelope<Void> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.code != 200) {
                    toast(resolveMessage(envelope, "删除失败"));
                    return;
                }
                toast("课程已删除");
                loadCourses();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<Void>> call, Throwable t) {
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private void renderMetrics(@Nullable List<PlatformModels.CourseManageView> items) {
        int totalCourses = items == null ? 0 : items.size();
        int totalViews = 0;
        int totalEnrolls = 0;
        if (items != null) {
            for (PlatformModels.CourseManageView item : items) {
                totalViews += item.browseCount;
                totalEnrolls += item.enrollCount;
            }
        }
        tvTotalCourses.setText("课程 " + totalCourses);
        tvTotalViews.setText("浏览 " + totalViews);
        tvTotalEnrolls.setText("报名 " + totalEnrolls);
        renderTrainingChart(totalCourses, totalViews, totalEnrolls);
    }

    private void renderTrainingChart(int totalCourses, int totalViews, int totalEnrolls) {
        if (chartTraining == null) {
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, totalCourses));
        entries.add(new BarEntry(1f, totalViews));
        entries.add(new BarEntry(2f, totalEnrolls));

        BarDataSet dataSet = new BarDataSet(entries, "课程指标");
        dataSet.setColors(
                requireContext().getColor(com.example.low_altitudereststop.core.ui.R.color.ui_light_primary),
                requireContext().getColor(com.example.low_altitudereststop.core.ui.R.color.ui_light_secondary),
                requireContext().getColor(com.example.low_altitudereststop.core.ui.R.color.ui_light_tertiary)
        );
        dataSet.setValueTextColor(requireContext().getColor(R.color.ui_text_primary));
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.52f);
        chartTraining.setData(barData);
        chartTraining.setFitBars(true);
        chartTraining.setTouchEnabled(false);
        chartTraining.setScaleEnabled(false);
        chartTraining.setExtraOffsets(8f, 6f, 8f, 2f);
        chartTraining.setMinOffset(0f);

        Description description = new Description();
        description.setText("");
        chartTraining.setDescription(description);

        Legend legend = chartTraining.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = chartTraining.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(requireContext().getColor(R.color.ui_text_muted));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"课程", "浏览", "报名"}));

        YAxis axisLeft = chartTraining.getAxisLeft();
        axisLeft.setAxisMinimum(0f);
        axisLeft.setGridColor(requireContext().getColor(com.example.low_altitudereststop.core.ui.R.color.ui_light_outline_variant));
        axisLeft.setTextColor(requireContext().getColor(R.color.ui_text_muted));
        chartTraining.getAxisRight().setEnabled(false);
        chartTraining.invalidate();
    }

    private void applyNotchPadding(@NonNull android.view.View view) {
        final int baseTop = view.getPaddingTop();
        final int start = view.getPaddingStart();
        final int end = view.getPaddingEnd();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(start, baseTop + systemBars.top, end, bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String resolveMessage(@Nullable ApiEnvelope<?> envelope, @NonNull String fallback) {
        if (envelope == null || envelope.message == null || envelope.message.trim().isEmpty()) {
            return fallback;
        }
        return envelope.message;
    }
}

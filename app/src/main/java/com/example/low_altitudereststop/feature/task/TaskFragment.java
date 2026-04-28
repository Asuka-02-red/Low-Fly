package com.example.low_altitudereststop.feature.task;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.network.ApiFailureResolver;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.core.ui.PageStateController;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.example.low_altitudereststop.ui.RoleUiConfig;
import com.example.low_altitudereststop.ui.UsageAnalyticsStore;
import com.example.low_altitudereststop.ui.UserRole;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskFragment extends Fragment {

    private static final String TASK_CACHE_NAME = "tasks.json";
    private static final long TASK_CACHE_TTL_MILLIS = 15 * 60 * 1000L;
    private ActivityResultLauncher<Intent> taskDetailLauncher;
    private TaskAdapter taskAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyNotchPadding(view);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        androidx.recyclerview.widget.RecyclerView recycler = view.findViewById(R.id.recycler);
        com.google.android.material.button.MaterialButton btnCreate = view.findViewById(R.id.btn_create_task);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        PageStateController stateController = new PageStateController(
                view.findViewById(R.id.layout_task_state),
                swipe,
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.progress_page_state),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_title),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_desc),
                view.findViewById(com.example.low_altitudereststop.core.ui.R.id.btn_page_state_retry)
        );

        SessionStore store = new SessionStore(requireContext());
        AuthModels.SessionInfo user = store.getCachedUser();
        UserRole userRole = UserRole.from(user.role);
        RoleUiConfig config = RoleUiConfig.from(userRole);
        UsageAnalyticsStore analyticsStore = new UsageAnalyticsStore(requireContext());
        boolean isEnterprise = userRole == UserRole.ENTERPRISE;
        btnCreate.setVisibility(isEnterprise ? View.VISIBLE : View.GONE);
        tvTitle.setText(taskTitle(userRole, config));
        tvHint.setText(taskHint(userRole));
        btnCreate.setText(isEnterprise ? "发布任务" : "新建记录");

        taskAdapter = new TaskAdapter(task -> {
            analyticsStore.trackFeature(userRole, "task_detail");
            Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.id == null ? -1L : task.id);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_TITLE, task.title);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_TYPE, task.taskType);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_LOCATION, task.location);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_STATUS, task.status);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_DEADLINE, task.deadline);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_BUDGET, task.budget == null ? "" : task.budget.toPlainString());
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_OWNER, task.ownerName);
            taskDetailLauncher.launch(intent);
        });

        taskDetailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                swipe.setRefreshing(true);
                loadTasks(taskAdapter, swipe, stateController);
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(taskAdapter);

        btnCreate.setOnClickListener(v -> {
            analyticsStore.trackFeature(userRole, "task_create");
            startActivity(new Intent(requireContext(), CreateTaskActivity.class));
        });

        swipe.setOnRefreshListener(() -> loadTasks(taskAdapter, swipe, stateController));
        stateController.showLoading("正在同步任务", "正在加载最新项目任务和离线缓存。");
        swipe.setRefreshing(true);
        loadTasks(taskAdapter, swipe, stateController);
    }

    private void loadTasks(TaskAdapter adapter, androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe, PageStateController stateController) {
        FileCache cache = new FileCache(requireContext());
        SessionStore sessionStore = new SessionStore(requireContext());
        if (BuildConfig.IS_DEMO_MODE || sessionStore.isDemoSession()) {
            swipe.setRefreshing(false);
            showScenarioFallbackTasks(adapter, stateController, null);
            return;
        }
        ApiClient.getAuthedService(requireContext()).listTasks().enqueue(new Callback<ApiEnvelope<List<PlatformModels.TaskView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.TaskView>>> call, Response<ApiEnvelope<List<PlatformModels.TaskView>>> response) {
                swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) {
                    if (showScenarioFallbackTasks(adapter, stateController, null)) {
                        return;
                    }
                    List<PlatformModels.TaskView> cached = readFreshCache(cache);
                    if (cached != null) {
                        adapter.submit(cached);
                        stateController.showContent();
                        toast("服务暂不可用，已回退到最近缓存");
                        return;
                    }
                    stateController.showError("任务加载失败", ApiFailureResolver.fromHttp(response.code(), "任务列表加载失败"), "重新加载", () -> loadTasks(adapter, swipe, stateController));
                    return;
                }
                ApiEnvelope<List<PlatformModels.TaskView>> envelope = response.body();
                if (envelope.code != 200 || envelope.data == null) {
                    if (showScenarioFallbackTasks(adapter, stateController, null)) {
                        return;
                    }
                    stateController.showError("任务加载失败", envelope.message == null || envelope.message.trim().isEmpty()
                            ? "任务列表返回异常"
                            : envelope.message, "重新加载", () -> loadTasks(adapter, swipe, stateController));
                    return;
                }
                List<PlatformModels.TaskView> tasks = envelope.data;
                if (tasks == null || tasks.isEmpty()) {
                    if (showScenarioFallbackTasks(adapter, stateController, null)) {
                        return;
                    }
                    adapter.submit(tasks);
                    stateController.showEmpty("暂无任务", "当前没有可展示的任务数据，请稍后刷新。", "重新加载", () -> loadTasks(adapter, swipe, stateController));
                    return;
                }
                adapter.submit(tasks);
                stateController.showContent();
                cache.write(TASK_CACHE_NAME, new Gson().toJson(tasks));
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.TaskView>>> call, Throwable t) {
                swipe.setRefreshing(false);
                if (showScenarioFallbackTasks(adapter, stateController, null)) {
                    return;
                }
                List<PlatformModels.TaskView> cached = readFreshCache(cache);
                if (cached != null) {
                    adapter.submit(cached);
                    stateController.showContent();
                    toast("网络异常，已展示 15 分钟内缓存数据");
                    return;
                }
                List<PlatformModels.TaskView> staleCache = readCache(cache, TASK_CACHE_NAME);
                if (staleCache != null) {
                    adapter.submit(staleCache);
                    stateController.showContent();
                    toast("网络异常，已展示较早缓存数据");
                    return;
                }
                stateController.showError("任务加载失败", ApiFailureResolver.fromThrowable(t), "重新加载", () -> loadTasks(adapter, swipe, stateController));
            }
        });
    }

    private boolean showScenarioFallbackTasks(TaskAdapter adapter, PageStateController stateController, String tip) {
        adapter.submit(AppScenarioMapper.buildFallbackTasks());
        stateController.showContent();
        if (tip != null && !tip.trim().isEmpty()) {
            toast(tip);
        }
        return true;
    }

    private List<PlatformModels.TaskView> readFreshCache(FileCache cache) {
        try {
            String json = cache.readFresh(TASK_CACHE_NAME, TASK_CACHE_TTL_MILLIS);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            java.lang.reflect.Type type = new TypeToken<List<PlatformModels.TaskView>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<PlatformModels.TaskView> readCache(FileCache cache, String name) {
        try {
            String json = cache.read(name);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            java.lang.reflect.Type type = new TypeToken<List<PlatformModels.TaskView>>() {
            }.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String taskHint(UserRole role) {
        if (role == UserRole.ENTERPRISE) {
            return "围绕发布、调度、执行监控和风险处理快速推进项目。";
        }
        return "优先查看可执行任务，进入详情后直接完成接单、支付和后续履约。";
    }

    private String taskTitle(UserRole role, RoleUiConfig config) {
        if (role == UserRole.ENTERPRISE) {
            return "企业项目任务台";
        }
        return config.taskHeadline;
    }

    private void applyNotchPadding(@NonNull View view) {
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
}


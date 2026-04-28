package com.example.low_altitudereststop.feature.task;

import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityCreateTaskBinding;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTaskActivity extends NavigableEdgeToEdgeActivity {

    private ActivityCreateTaskBinding binding;
    private Snackbar submitSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.etDeadline.setText(TaskFormValidator.defaultDeadlineText());
        binding.btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        clearErrors();
        TaskFormValidator.FormInput input = new TaskFormValidator.FormInput();
        input.taskType = binding.etType.getText();
        input.title = binding.etTitle.getText();
        input.description = binding.etDesc.getText();
        input.location = binding.etLocation.getText();
        input.deadline = binding.etDeadline.getText();
        input.latitude = binding.etLat.getText();
        input.longitude = binding.etLng.getText();
        input.budget = binding.etBudget.getText();
        TaskFormValidator.ValidationResult validationResult = TaskFormValidator.validate(input);
        if (!validationResult.isValid()) {
            showFieldErrors(validationResult);
            toast("请先修正任务表单中的错误");
            return;
        }
        PlatformModels.TaskRequest req = validationResult.request;

        binding.btnSubmit.setEnabled(false);
        showSubmitting();
        ApiClient.getAuthedService(this).createTask(req).enqueue(new Callback<ApiEnvelope<PlatformModels.TaskView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.TaskView>> call, Response<ApiEnvelope<PlatformModels.TaskView>> response) {
                binding.btnSubmit.setEnabled(true);
                dismissSubmitting();
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    enqueue(req);
                    toast("发布失败");
                    return;
                }
                toast("发布成功");
                finish();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.TaskView>> call, Throwable t) {
                binding.btnSubmit.setEnabled(true);
                dismissSubmitting();
                enqueue(req);
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private void enqueue(PlatformModels.TaskRequest req) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("taskType", req.taskType);
        payload.put("title", req.title);
        payload.put("description", req.description);
        payload.put("location", req.location);
        payload.put("deadline", req.deadline);
        payload.put("latitude", req.latitude == null ? "0" : req.latitude.toPlainString());
        payload.put("longitude", req.longitude == null ? "0" : req.longitude.toPlainString());
        payload.put("budget", req.budget == null ? "0" : req.budget.toPlainString());
        OutboxSyncManager.enqueue(this, "CREATE_TASK", payload);
        toast("已加入离线同步队列");
    }

    private void toast(String msg) {
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    private void showSubmitting() {
        submitSnackbar = Snackbar.make(binding.getRoot(), "正在发布任务，请稍候", Snackbar.LENGTH_INDEFINITE);
        submitSnackbar.show();
    }

    private void dismissSubmitting() {
        if (submitSnackbar != null) {
            submitSnackbar.dismiss();
            submitSnackbar = null;
        }
    }

    private void clearErrors() {
        binding.etTitle.setError(null);
        binding.etDesc.setError(null);
        binding.etLocation.setError(null);
        binding.etDeadline.setError(null);
        binding.etLat.setError(null);
        binding.etLng.setError(null);
        binding.etBudget.setError(null);
    }

    private void showFieldErrors(TaskFormValidator.ValidationResult validationResult) {
        binding.etTitle.setError(validationResult.errorFor("title"));
        binding.etDesc.setError(validationResult.errorFor("description"));
        binding.etLocation.setError(validationResult.errorFor("location"));
        binding.etDeadline.setError(validationResult.errorFor("deadline"));
        binding.etLat.setError(validationResult.errorFor("latitude"));
        binding.etLng.setError(validationResult.errorFor("longitude"));
        binding.etBudget.setError(validationResult.errorFor("budget"));
    }
}


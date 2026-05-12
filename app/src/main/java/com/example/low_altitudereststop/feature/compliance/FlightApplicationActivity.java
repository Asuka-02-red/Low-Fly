package com.example.low_altitudereststop.feature.compliance;

import android.os.Bundle;
import android.widget.Toast;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.databinding.ActivityFlightApplicationBinding;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 飞行申请提交Activity（飞手端）。
 * <p>
 * 飞手用户填写飞行地点、时间和用途后提交飞行申请，
 * 网络不可用时自动将申请加入离线同步队列，确保数据不丢失。
 * </p>
 */
public class FlightApplicationActivity extends NavigableEdgeToEdgeActivity {

    private ActivityFlightApplicationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlightApplicationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        PlatformModels.FlightApplicationRequest req = new PlatformModels.FlightApplicationRequest();
        req.location = text(binding.etLocation.getText(), "");
        req.flightTime = text(binding.etTime.getText(), "2026-04-21 10:00");
        req.purpose = text(binding.etPurpose.getText(), "巡检");
        if (req.location.isEmpty()) {
            toast("请填写飞行地点");
            return;
        }
        binding.btnSubmit.setEnabled(false);
        ApiClient.getAuthedService(this).submitFlightApplication(req).enqueue(new Callback<ApiEnvelope<PlatformModels.FlightApplicationView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.FlightApplicationView>> call, Response<ApiEnvelope<PlatformModels.FlightApplicationView>> response) {
                binding.btnSubmit.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    enqueue(req);
                    toast("提交失败");
                    return;
                }
                PlatformModels.FlightApplicationView v = response.body().data;
                binding.tvResult.setText("申请号：" + safe(v.applicationNo) + "\n状态：" + safe(v.status) + "\n地点：" + safe(v.location) + "\n提示：" + safe(v.approvalHint));
                toast("提交成功");
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.FlightApplicationView>> call, Throwable t) {
                binding.btnSubmit.setEnabled(true);
                enqueue(req);
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private void enqueue(PlatformModels.FlightApplicationRequest req) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("location", req.location);
        payload.put("flightTime", req.flightTime);
        payload.put("purpose", req.purpose);
        OutboxSyncManager.enqueue(this, "SUBMIT_FLIGHT", payload);
        toast("已加入离线同步队列");
    }

    private String text(CharSequence cs, String def) {
        String v = cs == null ? "" : cs.toString().trim();
        return v.isEmpty() ? def : v;
    }

    private String safe(String v) {
        return v == null ? "-" : v;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}


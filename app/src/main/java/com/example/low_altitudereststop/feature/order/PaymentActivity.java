package com.example.low_altitudereststop.feature.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.sync.OutboxSyncManager;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityPaymentBinding;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_SUCCESS_MESSAGE = "success_message";
    private static final String PAYMENT_CHANNEL_APP = "app";

    private ActivityPaymentBinding binding;
    private String successMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1L);
        successMessage = getIntent().getStringExtra(EXTRA_SUCCESS_MESSAGE);
        binding.tvOrderId.setText("订单ID：" + orderId);
        if ("接单成功".equals(successMessage)) {
            binding.tvTitle.setText("确认接单");
            binding.btnPay.setText("确认接单");
        }
        binding.btnOrders.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));
        binding.btnPay.setOnClickListener(v -> pay(orderId));
    }

    private void pay(long orderId) {
        if (orderId <= 0) {
            toast("订单ID无效");
            return;
        }
        binding.btnPay.setEnabled(false);
        PlatformModels.PaymentRequest req = new PlatformModels.PaymentRequest();
        req.orderId = orderId;
        req.channel = PAYMENT_CHANNEL_APP;
        ApiClient.getAuthedService(this).payOrder(req).enqueue(new Callback<ApiEnvelope<PlatformModels.PaymentResult>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.PaymentResult>> call, Response<ApiEnvelope<PlatformModels.PaymentResult>> response) {
                binding.btnPay.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    enqueuePay(orderId, req.channel);
                    toast("支付失败");
                    return;
                }
                PlatformModels.PaymentResult r = response.body().data;
                binding.tvResult.setText("交易号：" + (r.tradeNo == null ? "-" : r.tradeNo) + "\n状态：" + (r.status == null ? "-" : r.status) + "\n金额：" + (r.amount == null ? "-" : r.amount.toPlainString()));
                toast(resolveSuccessMessage());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.PaymentResult>> call, Throwable t) {
                binding.btnPay.setEnabled(true);
                enqueuePay(orderId, req.channel);
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private void enqueuePay(long orderId, String channel) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("channel", channel);
        OutboxSyncManager.enqueue(this, "PAY_ORDER", payload);
        toast("已加入离线同步队列");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String resolveSuccessMessage() {
        return successMessage == null || successMessage.trim().isEmpty() ? "支付成功" : successMessage;
    }
}

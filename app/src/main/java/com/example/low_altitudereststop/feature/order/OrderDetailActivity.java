package com.example.low_altitudereststop.feature.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityOrderDetailBinding;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_ORDER_NO = "order_no";
    public static final String EXTRA_ORDER_STATUS = "order_status";
    public static final String EXTRA_ORDER_AMOUNT = "order_amount";

    private ActivityOrderDetailBinding binding;
    private long orderId;
    private String orderNo;
    private String status;
    private String amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1L);
        orderNo = getIntent().getStringExtra(EXTRA_ORDER_NO);
        status = getIntent().getStringExtra(EXTRA_ORDER_STATUS);
        amount = getIntent().getStringExtra(EXTRA_ORDER_AMOUNT);

        renderFallback(orderNo, status, amount);
        binding.btnPay.setEnabled(orderId > 0);
        binding.btnPay.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId);
            intent.putExtra(PaymentActivity.EXTRA_SUCCESS_MESSAGE, "支付成功");
            startActivity(intent);
        });
        if (orderId > 0) {
            loadDetail(orderId);
        } else {
            binding.tvRemark.setText("未获取到订单ID，当前展示的是列表透传的基础信息。");
        }
    }

    private void loadDetail(long orderId) {
        ApiClient.getAuthedService(this).getOrderDetail(orderId).enqueue(new Callback<ApiEnvelope<PlatformModels.OrderDetailView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.OrderDetailView>> call, Response<ApiEnvelope<PlatformModels.OrderDetailView>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    PlatformModels.OrderDetailView fallback = AppScenarioMapper.findOrderDetail(orderId, orderNo);
                    if (fallback != null) {
                        bindDetail(fallback);
                        binding.tvRemark.setText("订单详情已更新。");
                        toast("订单详情已更新");
                        return;
                    }
                    binding.tvRemark.setText("详情加载失败，已展示基础信息。");
                    toast("订单详情加载失败");
                    return;
                }
                bindDetail(response.body().data);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.OrderDetailView>> call, Throwable t) {
                PlatformModels.OrderDetailView fallback = AppScenarioMapper.findOrderDetail(orderId, orderNo);
                if (fallback != null) {
                    bindDetail(fallback);
                    binding.tvRemark.setText("当前为你展示可用订单详情。");
                    toast("已展示可用订单详情");
                    return;
                }
                binding.tvRemark.setText("网络异常，已展示基础信息。");
                toast("网络异常：" + t.getMessage());
            }
        });
    }

    private void bindDetail(PlatformModels.OrderDetailView detail) {
        binding.tvOrderNo.setText("订单号：" + safe(detail.orderNo));
        binding.tvStatus.setText("订单状态：" + safe(detail.status));
        binding.tvAmount.setText("订单金额：" + safe(amountText(detail.amount)));
        binding.tvTaskInfo.setText(
                "任务标题：" + safe(detail.taskTitle) +
                        "\n任务类型：" + safe(detail.taskType) +
                        "\n执行地点：" + safe(detail.location) +
                        "\n关联任务ID：" + safe(detail.taskId == null ? null : String.valueOf(detail.taskId))
        );
        binding.tvPartyInfo.setText(
                "飞手：" + safe(detail.pilotName) +
                        "\n企业：" + safe(detail.enterpriseName) +
                        "\n联系人：" + safe(detail.contactName) +
                        "\n联系电话：" + safe(detail.contactPhone)
        );
        binding.tvPaymentInfo.setText(
                "支付状态：" + safe(detail.paymentStatus) +
                        "\n支付通道：" + safe(detail.paymentChannel)
        );
        binding.tvTimeline.setText(
                "创建时间：" + safe(detail.createdAt) +
                        "\n预约时间：" + safe(detail.appointmentTime)
        );
        binding.tvRemark.setText(safe(detail.remark));
        binding.btnPay.setEnabled(detail.id != null && !"PAID".equalsIgnoreCase(detail.status));
    }

    private void renderFallback(String orderNo, String status, String amount) {
        binding.tvOrderNo.setText("订单号：" + safe(orderNo));
        binding.tvStatus.setText("订单状态：" + safe(status));
        binding.tvAmount.setText("订单金额：" + safe(amount));
        binding.tvTaskInfo.setText("任务信息待加载");
        binding.tvPartyInfo.setText("履约信息待加载");
        binding.tvPaymentInfo.setText("支付信息待加载");
        binding.tvTimeline.setText("时间信息待加载");
        binding.tvRemark.setText("正在加载完整订单详情...");
    }

    private String amountText(java.math.BigDecimal amount) {
        return amount == null ? null : amount.toPlainString();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}


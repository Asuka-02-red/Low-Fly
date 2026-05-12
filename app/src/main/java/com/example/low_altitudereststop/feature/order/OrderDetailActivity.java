package com.example.low_altitudereststop.feature.order;

import android.content.Intent;
import android.os.Bundle;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityOrderDetailBinding;
import com.example.low_altitudereststop.feature.order.local.OrderDao;
import com.example.low_altitudereststop.feature.order.local.OrderEntity;
import com.example.low_altitudereststop.feature.order.local.OrderLocalDatabase;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends NavigableEdgeToEdgeActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_ORDER_NO = "order_no";
    public static final String EXTRA_ORDER_STATUS = "order_status";
    public static final String EXTRA_ORDER_AMOUNT = "order_amount";

    private ActivityOrderDetailBinding binding;
    private OrderDao orderDao;
    private long orderId;
    private String orderNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderDao = OrderLocalDatabase.get(this).orderDao();
        OrderLocalDatabase.ensureSeeded(this);

        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1L);
        orderNo = getIntent().getStringExtra(EXTRA_ORDER_NO);

        binding.btnPay.setEnabled(orderId > 0);
        binding.btnPay.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(PaymentActivity.EXTRA_ORDER_ID, orderId);
            intent.putExtra(PaymentActivity.EXTRA_SUCCESS_MESSAGE, "支付成功");
            startActivity(intent);
        });

        loadFromLocal();
        if (orderId > 0) {
            refreshFromNetwork(orderId);
        }
    }

    private void loadFromLocal() {
        if (orderId <= 0) {
            renderFromIntent();
            return;
        }
        OrderEntity entity = orderDao.findById(orderId);
        if (entity != null) {
            bindDetail(OrderLocalDatabase.toOrderDetailView(entity));
        } else {
            renderFromIntent();
        }
    }

    private void renderFromIntent() {
        String status = getIntent().getStringExtra(EXTRA_ORDER_STATUS);
        String amount = getIntent().getStringExtra(EXTRA_ORDER_AMOUNT);
        binding.tvOrderNo.setText("订单号：" + safe(orderNo));
        binding.tvStatus.setText("订单状态：" + safe(status));
        binding.tvAmount.setText("订单金额：" + safe(amount));
        binding.tvTaskInfo.setText("任务信息待加载");
        binding.tvPartyInfo.setText("履约信息待加载");
        binding.tvPaymentInfo.setText("支付信息待加载");
        binding.tvTimeline.setText("时间信息待加载");
        binding.tvRemark.setText("正在加载完整订单详情...");
    }

    private void refreshFromNetwork(long orderId) {
        ApiClient.getAuthedService(this).getOrderDetail(orderId).enqueue(new Callback<ApiEnvelope<PlatformModels.OrderDetailView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.OrderDetailView>> call, Response<ApiEnvelope<PlatformModels.OrderDetailView>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    return;
                }
                PlatformModels.OrderDetailView detail = response.body().data;
                syncDetailToLocal(detail);
                bindDetail(detail);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.OrderDetailView>> call, Throwable t) {
            }
        });
    }

    private void syncDetailToLocal(PlatformModels.OrderDetailView detail) {
        if (detail == null || detail.id == null) {
            return;
        }
        OrderEntity entity = OrderLocalDatabase.toEntity(detail, System.currentTimeMillis());
        orderDao.upsert(entity);
    }

    private void bindDetail(PlatformModels.OrderDetailView detail) {
        binding.tvOrderNo.setText("订单号：" + safe(detail.orderNo));
        binding.tvStatus.setText("订单状态：" + safe(detail.status));
        binding.tvAmount.setText("订单金额：" + safe(amountText(detail.amount)));
        binding.tvTaskInfo.setText(
                "任务标题：" + safe(detail.taskTitle)
                        + "\n任务类型：" + safe(detail.taskType)
                        + "\n执行地点：" + safe(detail.location)
                        + "\n关联任务ID：" + safe(detail.taskId == null ? null : String.valueOf(detail.taskId))
        );
        binding.tvPartyInfo.setText(
                "飞手：" + safe(detail.pilotName)
                        + "\n企业：" + safe(detail.enterpriseName)
                        + "\n联系人：" + safe(detail.contactName)
                        + "\n联系电话：" + safe(detail.contactPhone)
        );
        binding.tvPaymentInfo.setText(
                "支付状态：" + safe(detail.paymentStatus)
                        + "\n支付通道：" + safe(detail.paymentChannel)
        );
        binding.tvTimeline.setText(
                "创建时间：" + safe(detail.createdAt)
                        + "\n预约时间：" + safe(detail.appointmentTime)
        );
        binding.tvRemark.setText(safe(detail.remark));
        binding.btnPay.setEnabled(detail.id != null && !"PAID".equalsIgnoreCase(detail.status));
    }

    private String amountText(java.math.BigDecimal amount) {
        return amount == null ? null : amount.toPlainString();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}

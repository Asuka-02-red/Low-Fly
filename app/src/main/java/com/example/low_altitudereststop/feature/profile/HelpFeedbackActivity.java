package com.example.low_altitudereststop.feature.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import com.example.low_altitudereststop.R;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.network.ApiClient;
import com.example.low_altitudereststop.core.network.ApiEnvelope;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.core.ui.PageStateController;
import com.example.low_altitudereststop.databinding.ActivityHelpFeedbackBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 帮助与反馈Activity，提供用户提交反馈工单的功能。
 * <p>
 * 用户填写反馈内容后提交到服务端，网络不可用时加入离线同步队列，
 * 展示最近一次提交的工单编号，支持页面状态管理（加载/内容/错误）。
 * </p>
 */
public class HelpFeedbackActivity extends NavigableEdgeToEdgeActivity {

    private static final String PREF = "help_feedback";
    private static final String KEY_LAST_TICKET = "last_ticket";
    private ActivityHelpFeedbackBinding binding;
    private PageStateController ticketStateController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ticketStateController = new PageStateController(
                findViewById(R.id.layout_ticket_state),
                findViewById(R.id.layout_ticket_content),
                findViewById(com.example.low_altitudereststop.core.ui.R.id.progress_page_state),
                findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_title),
                findViewById(com.example.low_altitudereststop.core.ui.R.id.tv_page_state_desc),
                findViewById(com.example.low_altitudereststop.core.ui.R.id.btn_page_state_retry)
        );
        ticketStateController.showLoading("正在加载工单", "正在同步你最近提交的帮助与反馈记录。");
        loadTickets();

        binding.btnSubmit.setOnClickListener(v -> submitTicket());
    }

    private void submitTicket() {
        String contact = textOf(binding.etContact.getText());
        String detail = textOf(binding.etDetail.getText());
        if (detail.length() < 10) {
            binding.layoutDetail.setError("请至少填写 10 个字，便于快速定位问题。");
            return;
        }
        binding.layoutDetail.setError(null);
        PlatformModels.FeedbackTicketRequest request = new PlatformModels.FeedbackTicketRequest();
        request.contact = contact;
        request.detail = detail;
        binding.btnSubmit.setEnabled(false);
        ApiClient.getAuthedService(this).createFeedbackTicket(request).enqueue(new Callback<ApiEnvelope<PlatformModels.FeedbackTicketView>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PlatformModels.FeedbackTicketView>> call, Response<ApiEnvelope<PlatformModels.FeedbackTicketView>> response) {
                binding.btnSubmit.setEnabled(true);
                ApiEnvelope<PlatformModels.FeedbackTicketView> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.data == null) {
                    saveFallbackTicket(contact, detail);
                    Toast.makeText(HelpFeedbackActivity.this, "服务繁忙，已先保存本地工单。", Toast.LENGTH_SHORT).show();
                    return;
                }
                cacheTicket(envelope.data);
                binding.etDetail.setText(null);
                loadTickets();
                Toast.makeText(HelpFeedbackActivity.this, "工单已提交，客服将尽快回复。", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PlatformModels.FeedbackTicketView>> call, Throwable t) {
                binding.btnSubmit.setEnabled(true);
                saveFallbackTicket(contact, detail);
                Toast.makeText(HelpFeedbackActivity.this, "网络异常，已先保存本地工单。", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTickets() {
        ticketStateController.showLoading("正在加载工单", "正在同步你最近提交的帮助与反馈记录。");
        ApiClient.getAuthedService(this).listMyFeedbackTickets().enqueue(new Callback<ApiEnvelope<List<PlatformModels.FeedbackTicketView>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<PlatformModels.FeedbackTicketView>>> call, Response<ApiEnvelope<List<PlatformModels.FeedbackTicketView>>> response) {
                ApiEnvelope<List<PlatformModels.FeedbackTicketView>> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || envelope.data == null || envelope.data.isEmpty()) {
                    renderLocalFallback();
                    return;
                }
                PlatformModels.FeedbackTicketView latest = envelope.data.get(0);
                cacheTicket(latest);
                binding.tvLastTicket.setText(formatTicket(latest.ticketNo, latest.createTime, latest.contact, latest.detail, latest.status, latest.reply));
                ticketStateController.showContent();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<PlatformModels.FeedbackTicketView>>> call, Throwable t) {
                renderLocalFallback();
            }
        });
    }

    private void renderLocalFallback() {
        SharedPreferences preferences = getSharedPreferences(PREF, MODE_PRIVATE);
        String summary = preferences.getString(KEY_LAST_TICKET, null);
        if (summary == null || summary.trim().isEmpty()) {
            ticketStateController.showEmpty("暂无工单记录", "你还没有提交过工单，填写问题后即可进入处理流程。", "重新加载", this::loadTickets);
            return;
        }
        binding.tvLastTicket.setText(summary);
        ticketStateController.showContent();
    }

    private void saveFallbackTicket(String contact, String detail) {
        String ticketId = "HD-" + System.currentTimeMillis();
        String submitTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date());
        String summary = formatTicket(ticketId, submitTime, contact.isEmpty() ? "未填写" : contact, detail, "待处理", "待客服回复");
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(KEY_LAST_TICKET, summary).apply();
        renderLocalFallback();
    }

    private void cacheTicket(PlatformModels.FeedbackTicketView ticket) {
        String summary = formatTicket(ticket.ticketNo, ticket.createTime, ticket.contact, ticket.detail, ticket.status, ticket.reply);
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(KEY_LAST_TICKET, summary).apply();
    }

    private String formatTicket(String ticketNo, String createTime, String contact, String detail, String status, String reply) {
        return ticketNo + " | " + createTime
                + "\n联系方式：" + (contact == null || contact.trim().isEmpty() ? "未填写" : contact)
                + "\n状态：" + (status == null ? "-" : status)
                + "\n问题：" + (detail == null ? "-" : detail)
                + "\n回复：" + (reply == null ? "-" : reply);
    }

    private String textOf(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }
}

package com.example.low_altitudereststop.feature.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.session.SessionStore;
import com.example.low_altitudereststop.core.ui.NavigableEdgeToEdgeActivity;
import com.example.low_altitudereststop.databinding.ActivityMessageCenterBinding;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.example.low_altitudereststop.ui.UserRole;
import java.util.List;

public class MessageCenterActivity extends NavigableEdgeToEdgeActivity {

    private ActivityMessageCenterBinding binding;
    private int unreadCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessageCenterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthModels.SessionInfo user = new SessionStore(this).getCachedUser();
        UserRole role = UserRole.from(user.role);
        bindMessages(role);

        binding.btnMarkAll.setOnClickListener(v -> {
            unreadCount = 0;
            binding.tvUnreadCount.setText(String.valueOf(unreadCount));
            Toast.makeText(this, "已全部标记为已读", Toast.LENGTH_SHORT).show();
        });
        binding.btnOpenSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void bindMessages(UserRole role) {
        List<com.example.low_altitudereststop.core.model.PlatformModels.MessageConversationView> items =
                AppScenarioMapper.buildConversationSummaries(role);
        binding.tvMessageOneTitle.setText(items.get(0).title);
        binding.tvMessageOneBody.setText(items.get(0).lastMessagePreview);
        binding.tvMessageTwoTitle.setText(items.get(1).title);
        binding.tvMessageTwoBody.setText(items.get(1).lastMessagePreview);
        binding.tvMessageThreeTitle.setText(items.get(2).title);
        binding.tvMessageThreeBody.setText(items.get(2).lastMessagePreview);
        unreadCount = items.get(0).unreadCount + items.get(1).unreadCount + items.get(2).unreadCount;
        binding.tvUnreadCount.setText(String.valueOf(unreadCount));
    }
}

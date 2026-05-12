package com.example.low_altitudereststop.feature.message;

import androidx.annotation.NonNull;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;

/**
 * 飞手端消息列表适配器，展示企业发送的消息。
 * <p>
 * 继承BaseMessageAdapter，通过MessageIdentityRepository异步解析
 * 消息对应的企业名称，用于飞手角色查看与企业的对话列表。
 * </p>
 */
public class PilotMessageAdapter extends BaseMessageAdapter {

    public PilotMessageAdapter(@NonNull MessageIdentityRepository identityRepository, OnMessageClickListener listener) {
        super(identityRepository, listener);
        this.identityRepository = identityRepository;
    }

    private final MessageIdentityRepository identityRepository;

    @Override
    protected MessageIdentityRepository.Cancelable createCancelableRequest(MessageEntity message, MessageIdentityRepository.NameCallback callback) {
        return identityRepository.resolveEnterpriseName(message.enterpriseUid, callback);
    }
}

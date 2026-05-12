package com.example.low_altitudereststop.feature.message;

import androidx.annotation.NonNull;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;

/**
 * 企业端消息列表适配器，展示飞手发送的消息。
 * <p>
 * 继承BaseMessageAdapter，通过MessageIdentityRepository异步解析
 * 消息对应的飞手姓名，用于企业角色查看与飞手的对话列表。
 * </p>
 */
public class EnterpriseMessageAdapter extends BaseMessageAdapter {

    public EnterpriseMessageAdapter(@NonNull MessageIdentityRepository identityRepository, OnMessageClickListener listener) {
        super(identityRepository, listener);
        this.identityRepository = identityRepository;
    }

    private final MessageIdentityRepository identityRepository;

    @Override
    protected MessageIdentityRepository.Cancelable createCancelableRequest(MessageEntity message, MessageIdentityRepository.NameCallback callback) {
        return identityRepository.resolvePilotName(message.pilotUid, callback);
    }
}

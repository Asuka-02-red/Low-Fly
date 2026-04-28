package com.example.low_altitudereststop.feature.message;

import androidx.annotation.NonNull;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;

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

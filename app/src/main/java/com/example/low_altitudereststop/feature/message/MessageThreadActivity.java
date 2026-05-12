package com.example.low_altitudereststop.feature.message;

/**
 * 消息会话线程Activity，MessageDetailActivity的别名类。
 * <p>
 * 提供与MessageDetailActivity相同的会话详情功能，
 * 保留独立的类名以便在导航或外部跳转时区分入口。
 * </p>
 */
public class MessageThreadActivity extends MessageDetailActivity {

    public static final String EXTRA_CONVERSATION_ID = MessageDetailActivity.EXTRA_CONVERSATION_ID;
}

package com.example.low_altitudereststop.feature.message;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.TextView;

/**
 * 骨架屏文本动画辅助工具类。
 * <p>
 * 为TextView提供透明度脉冲动画效果，模拟数据加载中的骨架屏状态，
 * 支持启动和停止动画控制，用于消息列表中身份名称的异步加载占位。
 * </p>
 */
public final class SkeletonTextHelper {

    private SkeletonTextHelper() {
    }

    public static void start(TextView textView) {
        if (textView.getTag() instanceof ObjectAnimator) {
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(textView, "alpha", 0.45f, 1f);
        animator.setDuration(650L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
        textView.setTag(animator);
    }

    public static void stop(TextView textView) {
        Object tag = textView.getTag();
        if (tag instanceof ObjectAnimator) {
            ((ObjectAnimator) tag).cancel();
            textView.setTag(null);
        }
        textView.setAlpha(1f);
    }
}

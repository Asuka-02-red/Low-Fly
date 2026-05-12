package com.example.low_altitudereststop.core.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 边到边显示基类Activity，启用全屏沉浸式布局并自动处理系统状态栏和输入法窗口边衬，
 * 确保内容区域不被系统栏遮挡。
 */
public abstract class EdgeToEdgeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        View root = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (root != null) {
            applySystemBarInsets(root);
            ImeAwareScrollHelper.install(root);
        }
    }

    @Override
    public void setContentView(@NonNull View view) {
        super.setContentView(view);
        applySystemBarInsets(view);
        ImeAwareScrollHelper.install(view);
    }

    private void applySystemBarInsets(@NonNull View root) {
        final int start = root.getPaddingStart();
        final int top = root.getPaddingTop();
        final int end = root.getPaddingEnd();
        final int bottom = root.getPaddingBottom();
        final int extraTop = dpToPx(12);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, imeInsets.bottom);
            view.setPadding(
                    start + systemBars.left,
                    top + systemBars.top + extraTop,
                    end + systemBars.right,
                    bottom + bottomInset
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private int dpToPx(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }
}

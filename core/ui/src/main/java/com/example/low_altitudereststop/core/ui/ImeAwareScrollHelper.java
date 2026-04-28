package com.example.low_altitudereststop.core.ui;

import android.graphics.Rect;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

final class ImeAwareScrollHelper {

    private static final long FOCUS_SCROLL_DELAY_MS = 120L;

    private ImeAwareScrollHelper() {
    }

    static void install(@NonNull View root) {
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            if (!(newFocus instanceof EditText) || !newFocus.isShown()) {
                return;
            }
            root.postDelayed(() -> bringIntoView(root, newFocus), FOCUS_SCROLL_DELAY_MS);
        });
    }

    static void bringIntoView(@NonNull View root, @NonNull View target) {
        if (!target.isShown()) {
            return;
        }
        View scrollParent = findScrollableParent(target, root);
        if (scrollParent == null) {
            target.requestRectangleOnScreen(buildTargetRect(target), true);
            return;
        }
        Rect rect = buildTargetRect(target);
        if (scrollParent instanceof NestedScrollView) {
            ((NestedScrollView) scrollParent).offsetDescendantRectToMyCoords(target, rect);
            smoothScroll((NestedScrollView) scrollParent, rect);
            return;
        }
        if (scrollParent instanceof ScrollView) {
            ((ScrollView) scrollParent).offsetDescendantRectToMyCoords(target, rect);
            smoothScroll((ScrollView) scrollParent, rect);
            return;
        }
        if (scrollParent instanceof RecyclerView) {
            ((RecyclerView) scrollParent).requestChildRectangleOnScreen(target, rect, true);
            return;
        }
        target.requestRectangleOnScreen(rect, true);
    }

    @NonNull
    private static Rect buildTargetRect(@NonNull View target) {
        Rect rect = new Rect(0, 0, target.getWidth(), target.getHeight());
        int extra = dpToPx(target, 24);
        rect.top -= extra;
        rect.bottom += extra;
        return rect;
    }

    private static void smoothScroll(@NonNull NestedScrollView scrollView, @NonNull Rect rect) {
        int dy = computeScrollDelta(scrollView.getScrollY(), scrollView.getHeight(), rect.top, rect.bottom);
        if (dy != 0) {
            scrollView.smoothScrollBy(0, dy);
        }
    }

    private static void smoothScroll(@NonNull ScrollView scrollView, @NonNull Rect rect) {
        int dy = computeScrollDelta(scrollView.getScrollY(), scrollView.getHeight(), rect.top, rect.bottom);
        if (dy != 0) {
            scrollView.smoothScrollBy(0, dy);
        }
    }

    private static int computeScrollDelta(int currentScrollY, int viewportHeight, int childTop, int childBottom) {
        int visibleTop = currentScrollY;
        int visibleBottom = currentScrollY + viewportHeight;
        if (childTop < visibleTop) {
            return childTop - visibleTop;
        }
        if (childBottom > visibleBottom) {
            return childBottom - visibleBottom;
        }
        return 0;
    }

    private static View findScrollableParent(@NonNull View target, @NonNull View root) {
        ViewParent current = target.getParent();
        while (current instanceof View) {
            View view = (View) current;
            if (view == root) {
                break;
            }
            if (view instanceof NestedScrollView
                    || view instanceof ScrollView
                    || view instanceof RecyclerView
                    || view instanceof HorizontalScrollView) {
                return view;
            }
            if (view instanceof ViewGroup && ViewCompat.isNestedScrollingEnabled(view)) {
                return view;
            }
            current = view.getParent();
        }
        return root;
    }

    private static int dpToPx(@NonNull View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}

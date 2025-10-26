package com.example.xinqiao.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;

/**
 * 布局优化工具类
 * 提供一系列方法来优化布局性能
 */
public class LayoutOptimizer {

    /**
     * 延迟加载ViewStub
     * @param viewStub 需要延迟加载的ViewStub
     * @param inflateListener 加载完成后的回调
     */
    public static void lazyLoadViewStub(final ViewStub viewStub, final OnViewStubInflatedListener inflateListener) {
        if (viewStub != null) {
            viewStub.setOnInflateListener((stub, inflated) -> {
                if (inflateListener != null) {
                    inflateListener.onInflated(inflated);
                }
            });
        }
    }

    /**
     * 延迟初始化视图
     * @param view 需要延迟初始化的视图
     * @param initListener 初始化完成后的回调
     */
    public static void lazyInitView(final View view, final OnViewInitListener initListener) {
        if (view != null && initListener != null) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    initListener.onViewInitialized(view);
                }
            });
        }
    }

    /**
     * 延迟执行任务，避免阻塞UI线程
     * @param runnable 需要执行的任务
     * @param delayMillis 延迟时间（毫秒）
     */
    public static void postDelayed(Runnable runnable, long delayMillis) {
        if (runnable != null) {
            new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMillis);
        }
    }

    /**
     * ViewStub加载完成回调接口
     */
    public interface OnViewStubInflatedListener {
        void onInflated(View inflatedView);
    }

    /**
     * 视图初始化完成回调接口
     */
    public interface OnViewInitListener {
        void onViewInitialized(View view);
    }
}
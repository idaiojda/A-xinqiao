package com.example.xinqiao.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能监控工具类
 * 提供一系列方法来监控应用性能
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final long MONITOR_INTERVAL = 160L; // 监控间隔，约10帧
    private static final long ANR_THRESHOLD = 5000L; // ANR阈值，5秒
    
    private static PerformanceMonitor instance;
    
    private HandlerThread monitorThread;
    private Handler monitorHandler;
    private Handler mainHandler;
    
    private boolean isMonitoring = false;
    private long lastFrameTimeNanos = 0;
    private int frameCount = 0;
    private int skipFrameCount = 0;
    private long monitorStartTime = 0;
    private List<PerformanceListener> listeners = new ArrayList<>();
    
    private PerformanceMonitor() {
        // 确保在主线程创建Handler
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mainHandler = new Handler();
        } else {
            mainHandler = new Handler(Looper.getMainLooper());
        }
    }
    
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * 初始化性能监控
     * @param application 应用实例
     */
    public void init(Application application) {
        if (application == null) {
            return;
        }
        
        // 注册Activity生命周期回调
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
            
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // Activity恢复时开始监控
                startMonitor();
            }
            
            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Activity暂停时停止监控
                stopMonitor();
            }
            
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
    
    /**
     * 添加性能监听器
     * @param listener 性能监听器
     */
    public void addListener(PerformanceListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除性能监听器
     * @param listener 性能监听器
     */
    public void removeListener(PerformanceListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 开始监控
     */
    public void startMonitor() {
        if (isMonitoring) {
            return;
        }
        
        isMonitoring = true;
        frameCount = 0;
        skipFrameCount = 0;
        monitorStartTime = SystemClock.elapsedRealtime();
        
        // 创建监控线程
        monitorThread = new HandlerThread("PerformanceMonitorThread");
        monitorThread.start();
        monitorHandler = new Handler(monitorThread.getLooper());
        
        // 注册帧回调
        Choreographer.getInstance().postFrameCallback(frameCallback);
        
        // 启动ANR监控
        startANRMonitor();
    }
    
    /**
     * 停止监控
     */
    public void stopMonitor() {
        if (!isMonitoring) {
            return;
        }
        
        isMonitoring = false;
        
        // 移除帧回调
        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        } catch (Exception e) {
            Log.e(TAG, "移除帧回调失败", e);
        }
        
        // 停止监控线程
        if (monitorHandler != null) {
            try {
                monitorHandler.removeCallbacksAndMessages(null); // 移除所有待处理的消息
            } catch (Exception e) {
                Log.e(TAG, "移除消息失败", e);
            }
        }
        
        if (monitorThread != null) {
            try {
                monitorThread.quitSafely(); // 使用quitSafely确保所有消息都被处理
            } catch (Exception e) {
                Log.e(TAG, "停止监控线程失败", e);
            } finally {
                monitorThread = null;
                monitorHandler = null;
            }
        }
        
        // 计算性能指标
        final long duration = SystemClock.elapsedRealtime() - monitorStartTime;
        final float fps = frameCount * 1000f / duration;
        final int finalSkipFrameCount = skipFrameCount;
        
        // 通知监听器
        try {
            for (PerformanceListener listener : listeners) {
                if (listener != null) {
                    listener.onPerformanceReport(fps, finalSkipFrameCount, duration);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "通知监听器失败", e);
        }
    }
    
    /**
     * 释放资源
     * 应在应用退出时调用
     */
    public void release() {
        stopMonitor();
        listeners.clear();
        instance = null;
    }
    
    /**
     * 帧回调
     */
    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isMonitoring) {
                return;
            }
            
            // 计算帧间隔
            if (lastFrameTimeNanos != 0) {
                long frameInterval = (frameTimeNanos - lastFrameTimeNanos) / 1000000;
                
                // 检测跳帧（16.6ms为一帧的理想时间）
                int skippedFrames = (int) (frameInterval / 16.6f) - 1;
                if (skippedFrames > 0) {
                    skipFrameCount += skippedFrames;
                    
                    // 通知严重跳帧（降低阈值到20帧以提前预警）
                    if (skippedFrames >= 20) {
                        for (PerformanceListener listener : listeners) {
                            listener.onFrameRateWarning(skippedFrames);
                        }
                    }
                }
            }
            
            lastFrameTimeNanos = frameTimeNanos;
            frameCount++;
            
            // 继续监控下一帧
            Choreographer.getInstance().postFrameCallback(this);
        }
    };
    
    /**
     * 启动ANR监控
     */
    private void startANRMonitor() {
        if (monitorHandler == null || !isMonitoring) {
            return;
        }
        
        monitorHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) {
                    return;
                }
                
                // 发送消息到主线程
                final long startTime = SystemClock.elapsedRealtime();
                final boolean[] receivedResponse = {false};
                
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        receivedResponse[0] = true;
                    });
                } else {
                    // 如果mainHandler为null，直接标记为已响应，避免误报ANR
                    receivedResponse[0] = true;
                    Log.e(TAG, "mainHandler为空，无法检测ANR");
                }
                
                // 等待主线程响应
                try {
                    Thread.sleep(MONITOR_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 检查主线程是否响应
                if (!receivedResponse[0]) {
                    // 主线程可能阻塞，继续等待
                    try {
                        Thread.sleep(ANR_THRESHOLD - MONITOR_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    // 再次检查主线程是否响应
                    if (!receivedResponse[0]) {
                        // 主线程阻塞超过阈值，可能发生ANR
                        final long blockTime = SystemClock.elapsedRealtime() - startTime;
                        for (PerformanceListener listener : listeners) {
                            listener.onANRDetected(blockTime);
                        }
                    }
                }
                
                // 继续监控
                if (isMonitoring && monitorHandler != null) {
                    monitorHandler.postDelayed(this, MONITOR_INTERVAL);
                }
            }
        });
    }
    
    /**
     * 性能监听器接口
     */
    public interface PerformanceListener {
        /**
         * 帧率警告回调
         * @param skippedFrames 跳过的帧数
         */
        void onFrameRateWarning(int skippedFrames);
        
        /**
         * ANR检测回调
         * @param blockTime 阻塞时间（毫秒）
         */
        void onANRDetected(long blockTime);
        
        /**
         * 性能报告回调
         * @param fps 帧率
         * @param skipFrameCount 跳帧总数
         * @param duration 监控持续时间（毫秒）
         */
        void onPerformanceReport(float fps, int skipFrameCount, long duration);
    }
    
    /**
     * 性能监听器适配器（可选择实现部分方法）
     */
    public static abstract class PerformanceListenerAdapter implements PerformanceListener {
        @Override
        public void onFrameRateWarning(int skippedFrames) {
            // 默认空实现
        }
        
        @Override
        public void onANRDetected(long blockTime) {
            // 默认空实现
        }
        
        @Override
        public void onPerformanceReport(float fps, int skipFrameCount, long duration) {
            // 默认空实现
        }
    }
}
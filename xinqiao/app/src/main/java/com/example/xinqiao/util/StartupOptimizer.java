package com.example.xinqiao.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 应用启动优化工具类
 * 提供一系列方法来优化应用启动性能
 */
public class StartupOptimizer {
    private static StartupOptimizer instance;
    private final List<StartupTask> startupTasks = new ArrayList<>();
    private final Executor taskExecutor;
    private final Handler mainHandler;
    private boolean isInitialized = false;
    private long appStartTime = 0;
    
    private StartupOptimizer() {
        taskExecutor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized StartupOptimizer getInstance() {
        if (instance == null) {
            instance = new StartupOptimizer();
        }
        return instance;
    }
    
    /**
     * 初始化启动优化器
     * @param application 应用实例
     */
    public void init(Application application) {
        if (isInitialized) {
            return;
        }
        
        appStartTime = SystemClock.elapsedRealtime();
        isInitialized = true;
        
        // 注册Activity生命周期回调
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // 首个Activity创建时执行启动任务
                if (isFirstActivity(activity)) {
                    executeStartupTasks(activity);
                }
            }
            
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            
            @Override
            public void onActivityResumed(@NonNull Activity activity) {}
            
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
    
    /**
     * 添加启动任务
     * @param task 启动任务
     * @return 当前实例，用于链式调用
     */
    public StartupOptimizer addStartupTask(StartupTask task) {
        if (task != null) {
            startupTasks.add(task);
        }
        return this;
    }
    
    /**
     * 执行启动任务
     * @param context 上下文
     */
    private void executeStartupTasks(Context context) {
        // 分类任务
        List<StartupTask> mainThreadTasks = new ArrayList<>();
        List<StartupTask> backgroundTasks = new ArrayList<>();
        List<StartupTask> delayedTasks = new ArrayList<>();
        
        for (StartupTask task : startupTasks) {
            if (task.isMainThread()) {
                if (task.getDelayMillis() > 0) {
                    delayedTasks.add(task);
                } else {
                    mainThreadTasks.add(task);
                }
            } else {
                backgroundTasks.add(task);
            }
        }
        
        // 执行后台任务
        for (StartupTask task : backgroundTasks) {
            taskExecutor.execute(() -> task.execute(context));
        }
        
        // 执行主线程任务
        for (StartupTask task : mainThreadTasks) {
            task.execute(context);
        }
        
        // 执行延迟任务
        for (StartupTask task : delayedTasks) {
            mainHandler.postDelayed(() -> task.execute(context), task.getDelayMillis());
        }
    }
    
    /**
     * 判断是否是首个Activity
     * @param activity 活动实例
     * @return 如果是首个Activity返回true，否则返回false
     */
    private boolean isFirstActivity(Activity activity) {
        return activity.getClass().getSimpleName().contains("MainActivity") || 
               activity.getClass().getSimpleName().contains("SplashActivity") ||
               activity.getClass().getSimpleName().contains("LauncherActivity");
    }
    
    /**
     * 获取应用启动时间（毫秒）
     * @return 应用启动时间
     */
    public long getAppStartTime() {
        if (appStartTime == 0) {
            return 0;
        }
        return SystemClock.elapsedRealtime() - appStartTime;
    }
    
    /**
     * 启动任务接口
     */
    public interface StartupTask {
        /**
         * 执行任务
         * @param context 上下文
         */
        void execute(Context context);
        
        /**
         * 是否在主线程执行
         * @return 如果在主线程执行返回true，否则返回false
         */
        boolean isMainThread();
        
        /**
         * 获取延迟时间（毫秒）
         * @return 延迟时间
         */
        long getDelayMillis();
    }
    
    /**
     * 启动任务基类
     */
    public static abstract class BaseStartupTask implements StartupTask {
        private final boolean mainThread;
        private final long delayMillis;
        
        public BaseStartupTask(boolean mainThread, long delayMillis) {
            this.mainThread = mainThread;
            this.delayMillis = delayMillis;
        }
        
        @Override
        public boolean isMainThread() {
            return mainThread;
        }
        
        @Override
        public long getDelayMillis() {
            return delayMillis;
        }
    }
}
package com.example.xinqiao.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 线程优化工具类
 * 提供一系列方法来优化线程和异步任务
 */
public class ThreadOptimizer {
    // 单例模式
    private static ThreadOptimizer instance;
    
    // 线程池
    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;
    
    // 主线程Handler
    private final Handler mainHandler;
    
    private ThreadOptimizer() {
        diskIO = Executors.newSingleThreadExecutor();
        networkIO = Executors.newFixedThreadPool(3);
        mainThread = new MainThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized ThreadOptimizer getInstance() {
        if (instance == null) {
            instance = new ThreadOptimizer();
        }
        return instance;
    }
    
    /**
     * 在磁盘IO线程执行任务
     * @param runnable 需要执行的任务
     */
    public void executeDiskIO(Runnable runnable) {
        diskIO.execute(runnable);
    }
    
    /**
     * 在网络IO线程执行任务
     * @param runnable 需要执行的任务
     */
    public void executeNetworkIO(Runnable runnable) {
        networkIO.execute(runnable);
    }
    
    /**
     * 在主线程执行任务
     * @param runnable 需要执行的任务
     */
    public void executeMainThread(Runnable runnable) {
        mainThread.execute(runnable);
    }
    
    /**
     * 延迟在主线程执行任务
     * @param runnable 需要执行的任务
     * @param delayMillis 延迟时间（毫秒）
     */
    public void postToMainThreadDelayed(Runnable runnable, long delayMillis) {
        mainHandler.postDelayed(runnable, delayMillis);
    }
    
    /**
     * 使用WorkManager执行后台任务
     * @param context 上下文
     * @param workerClass 工作类
     * @param inputData 输入数据
     * @param requiresNetwork 是否需要网络连接
     */
    public void scheduleBackgroundWork(Context context, Class<? extends Worker> workerClass, 
                                      Data inputData, boolean requiresNetwork) {
        Constraints.Builder constraintsBuilder = new Constraints.Builder();
        if (requiresNetwork) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED);
        }
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(constraintsBuilder.build())
                .setInputData(inputData)
                .build();
        
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    /**
     * 主线程执行器
     */
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        
        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
    
    /**
     * 示例Worker类
     */
    public static class ExampleWorker extends Worker {
        public ExampleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }
        
        @NonNull
        @Override
        public Result doWork() {
            // 执行后台任务
            // ...
            return Result.success();
        }
    }
}
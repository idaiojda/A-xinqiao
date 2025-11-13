package com.example.xinqiao.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.bumptech.glide.Glide;

/**
 * 内存优化工具类
 * 提供一系列方法来优化内存使用和监控内存状态
 */
public class MemoryOptimizer {

    /**
     * 清理图片缓存
     * @param context 上下文
     */
    public static void clearImageCache(Context context) {
        if (context != null) {
            // 清理Glide内存缓存
            Glide.get(context).clearMemory();
            
            // 在后台线程中清理磁盘缓存
            new Thread(() -> {
                Glide.get(context).clearDiskCache();
            }).start();
        }
    }

    /**
     * 获取当前应用内存使用情况
     * @param context 上下文
     * @return 内存使用信息字符串
     */
    public static String getMemoryInfo(Context context) {
        if (context == null) {
            return "Context is null";
        }
        
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        // 获取Java堆内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024; // KB
        long totalMemory = runtime.totalMemory() / 1024; // KB
        long freeMemory = runtime.freeMemory() / 1024; // KB
        long usedMemory = totalMemory - freeMemory;
        
        // 获取Native堆内存使用情况
        long nativeHeapSize = Debug.getNativeHeapSize() / 1024; // KB
        long nativeHeapAllocated = Debug.getNativeHeapAllocatedSize() / 1024; // KB
        
        StringBuilder sb = new StringBuilder();
        sb.append("系统可用内存: ").append(memoryInfo.availMem / 1024 / 1024).append(" MB\n");
        sb.append("系统总内存: ").append(memoryInfo.totalMem / 1024 / 1024).append(" MB\n");
        sb.append("Java堆最大内存: ").append(maxMemory / 1024).append(" MB\n");
        sb.append("Java堆已分配内存: ").append(totalMemory / 1024).append(" MB\n");
        sb.append("Java堆已使用内存: ").append(usedMemory / 1024).append(" MB\n");
        sb.append("Native堆大小: ").append(nativeHeapSize / 1024).append(" MB\n");
        sb.append("Native堆已分配: ").append(nativeHeapAllocated / 1024).append(" MB");
        
        return sb.toString();
    }

    /**
     * 检查内存是否不足
     * @param context 上下文
     * @return 如果内存不足返回true，否则返回false
     */
    public static boolean isLowMemory(Context context) {
        if (context == null) {
            return false;
        }
        
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.lowMemory;
    }

    /**
     * 在内存不足时释放资源
     * @param context 上下文
     */
    public static void releaseMemoryOnLowMemory(Context context) {
        if (context != null && isLowMemory(context)) {
            // 清理图片缓存
            clearImageCache(context);
            
            // 建议进行垃圾回收（仅建议，不保证立即执行）
            System.gc();
            System.runFinalization();
        }
    }
}
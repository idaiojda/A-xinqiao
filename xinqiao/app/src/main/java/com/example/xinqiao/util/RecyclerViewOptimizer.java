package com.example.xinqiao.util;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

/**
 * RecyclerView优化工具类
 * 提供一系列方法来优化RecyclerView的性能
 */
public class RecyclerViewOptimizer {

    /**
     * 优化RecyclerView性能的综合方法
     * @param recyclerView 需要优化的RecyclerView
     * @param hasFixedSize 是否有固定大小
     * @param disableChangeAnimations 是否禁用change动画
     * @param cacheSize 缓存大小，建议值为2-4
     */
    public static void optimize(RecyclerView recyclerView, boolean hasFixedSize, 
                               boolean disableChangeAnimations, int cacheSize) {
        // 设置固定大小
        recyclerView.setHasFixedSize(hasFixedSize);
        
        // 增加缓存
        recyclerView.setItemViewCacheSize(cacheSize);
        
        // 禁用change动画，减少闪烁
        if (disableChangeAnimations) {
            RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }
        }
        
        // 设置绘制缓存
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(RecyclerView.DRAWING_CACHE_QUALITY_HIGH);
    }
    
    /**
     * 使用默认参数优化RecyclerView
     * @param recyclerView 需要优化的RecyclerView
     */
    public static void optimizeDefault(RecyclerView recyclerView) {
        optimize(recyclerView, true, true, 4);
    }
}
package com.example.xinqiao.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.xinqiao.R;

/**
 * 图片加载工具类
 * 封装Glide库，提供统一的图片加载方法
 */
public class ImageLoader {

    /**
     * 加载资源图片
     * @param context 上下文
     * @param resId 资源ID
     * @param imageView 目标ImageView
     */
    public static void loadImage(Context context, int resId, ImageView imageView) {
        if (context == null || imageView == null) return;
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.loading_placeholder)
                .error(R.drawable.error_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop(); // 资源型Drawable（如GradientDrawable）不进行结果缓存

        Glide.with(context)
                .load(resId)
                .apply(options)
                .into(imageView);
    }

    /**
     * 加载资源图片（带缓存策略）
     * @param context 上下文
     * @param resId 资源ID
     * @param imageView 目标ImageView
     * @param cacheStrategy 缓存策略
     */
    public static void loadImage(Context context, int resId, ImageView imageView, DiskCacheStrategy cacheStrategy) {
        if (context == null || imageView == null) return;
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.loading_placeholder)
                .error(R.drawable.error_placeholder)
                .diskCacheStrategy(cacheStrategy)
                .centerCrop(); // 保持调用者自定义，但建议对资源型使用NONE

        Glide.with(context)
                .load(resId)
                .apply(options)
                .into(imageView);
    }

    /**
     * 加载网络图片
     * @param context 上下文
     * @param url 图片URL
     * @param imageView 目标ImageView
     */
    public static void loadImageFromUrl(Context context, String url, ImageView imageView) {
        if (context == null || imageView == null) return;
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.loading_placeholder)
                .error(R.drawable.error_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .centerCrop(); // 网络图片缓存原始数据即可

        Glide.with(context)
                .load(url)
                .apply(options)
                .into(imageView);
    }

    /**
     * 加载圆形图片
     * @param context 上下文
     * @param resId 资源ID
     * @param imageView 目标ImageView
     */
    public static void loadCircleImage(Context context, int resId, ImageView imageView) {
        if (context == null || imageView == null) return;
        
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.loading_placeholder)
                .error(R.drawable.error_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .circleCrop(); // 资源型圆形头像不进行结果缓存

        Glide.with(context)
                .load(resId)
                .apply(options)
                .into(imageView);
    }

    /**
     * 预加载图片
     * @param context 上下文
     * @param resId 资源ID
     */
    public static void preloadImage(Context context, int resId) {
        if (context == null) return;
        
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE); // 预加载资源型Drawable无需磁盘缓存

        Glide.with(context)
                .load(resId)
                .apply(options)
                .preload();
    }

    /**
     * 清除内存缓存
     * @param context 上下文
     */
    public static void clearMemoryCache(Context context) {
        if (context == null) return;
        Glide.get(context).clearMemory();
    }
}
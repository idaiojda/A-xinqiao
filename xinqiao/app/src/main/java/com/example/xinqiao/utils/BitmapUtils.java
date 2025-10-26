package com.example.xinqiao.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

/**
 * 位图工具类，用于高效加载大尺寸图片，避免OOM异常
 */
public class BitmapUtils {
    private static final String TAG = "BitmapUtils";
    
    // 内存缓存，用于存储已加载的图片
    private static LruCache<String, Bitmap> mMemoryCache;
    
    static {
        // 获取可用内存的最大值，使用1/8作为缓存大小
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // 计算每个Bitmap的大小
                return bitmap.getByteCount() / 1024;
            }
        };
    }
    
    /**
     * 添加Bitmap到内存缓存
     */
    private static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null && bitmap != null) {
            mMemoryCache.put(key, bitmap);
        }
    }
    
    /**
     * 从内存缓存获取Bitmap
     */
    private static Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }
    
    /**
     * 从资源异步加载图片到ImageView
     * @param context 上下文
     * @param resId 资源ID
     * @param imageView 目标ImageView
     * @param reqWidth 请求宽度
     * @param reqHeight 请求高度
     */
    public static void loadBitmapAsync(Context context, int resId, ImageView imageView, int reqWidth, int reqHeight) {
        final String imageKey = String.valueOf(resId);
        
        // 先从内存缓存中查找
        final Bitmap bitmap = getBitmapFromMemoryCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }
        
        // 如果没有在缓存中找到，则异步加载
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                // 在后台线程中加载图片
                Bitmap bitmap = decodeSampledBitmapFromResource(context.getResources(), resId, reqWidth, reqHeight);
                if (bitmap != null) {
                    // 将加载的图片添加到缓存
                    addBitmapToMemoryCache(imageKey, bitmap);
                }
                return bitmap;
            }
            
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }.execute();
    }
    
    /**
     * 从资源加载图片，使用采样率降低内存使用
     * @param res 资源
     * @param resId 资源ID
     * @param reqWidth 请求宽度
     * @param reqHeight 请求高度
     * @return 缩小后的位图
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // 检查缓存
        final String imageKey = String.valueOf(resId);
        Bitmap cachedBitmap = getBitmapFromMemoryCache(imageKey);
        if (cachedBitmap != null) {
            return cachedBitmap;
        }
        
        try {
            // 首先只解码尺寸
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);
    
            // 计算采样率
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
    
            // 使用计算出的采样率解码位图
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // 使用RGB_565配置减少内存使用
            Bitmap bitmap = BitmapFactory.decodeResource(res, resId, options);
            
            // 添加到缓存
            if (bitmap != null) {
                addBitmapToMemoryCache(imageKey, bitmap);
            }
            
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "内存不足，尝试使用更大的采样率", e);
            // 内存不足时，使用更大的采样率
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8; // 使用更大的采样率
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeResource(res, resId, options);
        }
    }

    /**
     * 计算采样率
     * @param options BitmapFactory.Options，包含原始宽高
     * @param reqWidth 请求宽度
     * @param reqHeight 请求高度
     * @return 采样率
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 原始图片的宽高
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 计算最大的采样率，使得采样后的宽高大于等于请求的宽高
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
            
            // 额外检查，如果图片非常大，增加采样率
            long totalPixels = width * height / (inSampleSize * inSampleSize);
            final long totalReqPixelsCap = reqWidth * reqHeight * 2; // 允许的最大像素数
            
            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels = width * height / (inSampleSize * inSampleSize);
            }
            
            // 对于特别大的图片，直接使用更大的采样率
            if (width > 4000 || height > 4000) {
                inSampleSize = Math.max(inSampleSize, 8);
            }
        }

        Log.d(TAG, "Original size: " + width + "x" + height + ", Sample size: " + inSampleSize);
        return inSampleSize;
    }
    
    /**
     * 从资源加载Drawable，使用采样率降低内存使用
     * @param context 上下文
     * @param resId 资源ID
     * @param reqWidth 请求宽度
     * @param reqHeight 请求高度
     * @return 缩小后的Drawable
     */
    public static Drawable decodeSampledDrawableFromResource(Context context, int resId, int reqWidth, int reqHeight) {
        Bitmap bitmap = decodeSampledBitmapFromResource(context.getResources(), resId, reqWidth, reqHeight);
        return bitmap != null ? new BitmapDrawable(context.getResources(), bitmap) : null;
    }
}
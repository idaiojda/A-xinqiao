package com.example.xinqiao.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.util.Log;

public class BitmapLoader {
    private static final String TAG = "BitmapLoader";
    private static final int MAX_BITMAP_DIMENSION = 4096;

    /**
     * Loads a bitmap from a file path with automatic scaling to prevent OutOfMemoryError
     * and Canvas size exceptions.
     */
    public static void loadBitmap(Context context, String filePath, ImageView imageView) {
        try {
            // First decode bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            // Calculate dimensions
            int imageWidth = options.outWidth;
            int imageHeight = options.outHeight;
            int viewWidth = imageView.getWidth();
            int viewHeight = imageView.getHeight();

            // If view dimensions are not yet set, use screen dimensions
            if (viewWidth <= 0 || viewHeight <= 0) {
                viewWidth = context.getResources().getDisplayMetrics().widthPixels;
                viewHeight = context.getResources().getDisplayMetrics().heightPixels;
            }

            // Calculate sample size
            int sampleSize = calculateSampleSize(imageWidth, imageHeight, viewWidth, viewHeight);

            // Decode with calculated sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use RGB_565 to reduce memory usage

            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap != null) {
                // Scale down if still too large
                if (bitmap.getWidth() > MAX_BITMAP_DIMENSION || bitmap.getHeight() > MAX_BITMAP_DIMENSION) {
                    bitmap = scaleBitmapDown(bitmap);
                }
                imageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap: " + e.getMessage());
        }
    }

    private static int calculateSampleSize(int imageWidth, int imageHeight, int reqWidth, int reqHeight) {
        int sampleSize = 1;

        if (imageHeight > reqHeight || imageWidth > reqWidth) {
            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                sampleSize *= 2;
            }

            // Additional check for very large images
            long totalPixels = (imageWidth / sampleSize) * (imageHeight / sampleSize);
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;
            
            while (totalPixels > totalReqPixelsCap) {
                sampleSize *= 2;
                totalPixels /= 4;
            }
        }

        return sampleSize;
    }

    private static Bitmap scaleBitmapDown(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) MAX_BITMAP_DIMENSION / width, (float) MAX_BITMAP_DIMENSION / height);
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        if (scaledBitmap != bitmap) {
            bitmap.recycle(); // Recycle the original bitmap if a new one was created
        }
        return scaledBitmap;
    }
}
package com.example.xinqiao.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_SIZE = 1024; // Maximum width/height in pixels
    private static final int QUALITY = 80; // JPEG compression quality
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public static Bitmap loadAndResizeBitmap(Context context, Uri imageUri) {
        InputStream inputStream = null;
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            inputStream = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            closeQuietly(inputStream);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            closeQuietly(inputStream);
            inputStream = null;

            // Further resize if necessary
            if (bitmap != null && (bitmap.getWidth() > MAX_IMAGE_SIZE || bitmap.getHeight() > MAX_IMAGE_SIZE)) {
                bitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
            }

            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error loading image", e);
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory while loading image", e);
            return null;
        } finally {
            closeQuietly(inputStream);
        }
    }
    
    /**
     * 安全关闭流，避免资源泄漏
     */
    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.w(TAG, "Failed to close stream", e);
            }
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // 如果图片尺寸已经小于最大尺寸，直接返回
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }
        
        // 计算缩放比例
        float scaleWidth = (float) maxWidth / width;
        float scaleHeight = (float) maxHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        // 创建缩放后的图片
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        if (resizedBitmap != bitmap) {
            bitmap.recycle();
        }
        
        return resizedBitmap;
    }

    public static File compressImage(Context context, Uri imageUri, String destinationPath) {
        try {
            Bitmap bitmap = loadAndResizeBitmap(context, imageUri);
            if (bitmap == null) {
                return null;
            }

            File outputFile = new File(destinationPath);
            FileOutputStream fos = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos);
            fos.close();

            // Check if the file size is still too large
            if (outputFile.length() > MAX_FILE_SIZE) {
                // Reduce quality until file size is acceptable or quality becomes too low
                int currentQuality = QUALITY;
                while (outputFile.length() > MAX_FILE_SIZE && currentQuality > 20) {
                    currentQuality -= 10;
                    fos = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, fos);
                    fos.close();
                }
            }

            bitmap.recycle();
            return outputFile;
        } catch (IOException e) {
            Log.e(TAG, "Error compressing image", e);
            return null;
        }
    }

    public static Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    public static Bitmap byteArrayToBitmap(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) {
            return null;
        }
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static File saveBitmapToFile(Context context, Bitmap bitmap, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] uriToByteArray(Context context, Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                android.util.Log.e("ImageUtils", "无法打开图片输入流");
                return null;
            }

            // 解码图片
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                android.util.Log.e("ImageUtils", "图片解码失败");
                return null;
            }

            // 调整图片大小
            bitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);

            // 压缩图片
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outputStream);
            if (!success) {
                android.util.Log.e("ImageUtils", "图片压缩失败");
                return null;
            }

            byte[] imageData = outputStream.toByteArray();
            android.util.Log.d("ImageUtils", "图片处理成功，大小: " + imageData.length + " bytes");
            return imageData;

        } catch (Exception e) {
            android.util.Log.e("ImageUtils", "处理图片时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    android.util.Log.e("ImageUtils", "关闭输入流时发生错误: " + e.getMessage());
                }
            }
        }
    }
}
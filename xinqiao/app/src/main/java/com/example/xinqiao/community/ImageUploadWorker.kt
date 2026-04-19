package com.example.xinqiao.community

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

class ImageUploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        android.util.Log.d("ImageUploadWorker", "Starting image upload work")
        val uriString = inputData.getString(KEY_INPUT_URI) ?: run {
            android.util.Log.e("ImageUploadWorker", "No input URI provided")
            return@withContext Result.failure()
        }
        android.util.Log.d("ImageUploadWorker", "Input URI: $uriString")
        val uri = Uri.parse(uriString)
        
        try {
            // 读取文件内容
            android.util.Log.d("ImageUploadWorker", "Opening input stream")
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
                ?: run {
                    android.util.Log.e("ImageUploadWorker", "Failed to open input stream")
                    return@withContext Result.failure()
                }
            
            // 创建临时文件
            android.util.Log.d("ImageUploadWorker", "Creating temp file")
            val tempFile = File.createTempFile("upload_", ".jpg", applicationContext.cacheDir)
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            android.util.Log.d("ImageUploadWorker", "Temp file created: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
            
            // 更新进度
            setProgress(Data.Builder().putInt(KEY_PROGRESS, 30).build())
            
            // 获取baseUrl
            val baseUrl = com.example.xinqiao.network.NetworkConfig.getBaseUrl(applicationContext)
            val uploadUrl = "${baseUrl.trimEnd('/')}/api/uploads"
            android.util.Log.d("ImageUploadWorker", "Upload URL: $uploadUrl")
            
            // 构建multipart请求
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()
            
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()
            
            // 更新进度
            setProgress(Data.Builder().putInt(KEY_PROGRESS, 60).build())
            
            // 执行上传
            android.util.Log.d("ImageUploadWorker", "Executing upload request")
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            android.util.Log.d("ImageUploadWorker", "Response code: ${response.code}")
            
            // 清理临时文件
            tempFile.delete()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("ImageUploadWorker", "Response body: $responseBody")
                val json = JSONObject(responseBody ?: "{}")
                val data = json.optJSONObject("data")
                val remoteUrl = data?.optString("url") ?: ""
                
                if (remoteUrl.isNotEmpty()) {
                    android.util.Log.d("ImageUploadWorker", "Upload successful, remote URL: $remoteUrl")
                    setProgress(Data.Builder().putInt(KEY_PROGRESS, 100).build())
                    val out = Data.Builder().putString(KEY_REMOTE_URL, remoteUrl).build()
                    return@withContext Result.success(out)
                } else {
                    android.util.Log.e("ImageUploadWorker", "Remote URL is empty")
                }
            } else {
                android.util.Log.e("ImageUploadWorker", "Upload failed with code: ${response.code}, message: ${response.message}")
            }
            
            return@withContext Result.failure()
        } catch (e: Exception) {
            android.util.Log.e("ImageUploadWorker", "Exception during upload", e)
            e.printStackTrace()
            return@withContext Result.failure()
        }
    }

    companion object {
        const val KEY_INPUT_URI = "input_uri"
        const val KEY_REMOTE_URL = "remote_url"
        const val KEY_PROGRESS = "progress"
    }
}


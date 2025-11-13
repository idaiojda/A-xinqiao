package com.example.xinqiao.community

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class ImageUploadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val uri = inputData.getString(KEY_INPUT_URI) ?: return Result.failure()
        // 模拟上传：分阶段更新进度
        for (i in 1..5) {
            setProgress(Data.Builder().putInt(KEY_PROGRESS, i * 20).build())
            delay(300)
        }
        val remoteUrl = "https://example.com/images/${System.currentTimeMillis()}.jpg"
        val out = Data.Builder().putString(KEY_REMOTE_URL, remoteUrl).build()
        return Result.success(out)
    }

    companion object {
        const val KEY_INPUT_URI = "input_uri"
        const val KEY_REMOTE_URL = "remote_url"
        const val KEY_PROGRESS = "progress"
    }
}


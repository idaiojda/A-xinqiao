package com.example.xinqiao.community

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.xinqiao.notifications.NotificationUtils

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val user = inputData.getString("user") ?: ""
        NotificationUtils.showCheckinReminder(applicationContext, "打卡提醒", "晚间来社区完成今日打卡")
        return Result.success()
    }
}


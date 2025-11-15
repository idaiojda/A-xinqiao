package com.example.xinqiao.community

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleCheckinReminder(context: Context, user: String) {
    val now = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance()
    target.timeInMillis = now.timeInMillis
    val baseHour = 19 + (0..2).random()
    val minute = (0..59).random()
    target.set(java.util.Calendar.HOUR_OF_DAY, baseHour)
    target.set(java.util.Calendar.MINUTE, minute)
    target.set(java.util.Calendar.SECOND, 0)
    if (target.before(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1)
    val delay = target.timeInMillis - now.timeInMillis
    val req = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(Data.Builder().putString("user", user).build())
        .build()
    WorkManager.getInstance(context).enqueue(req)
}


package com.example.xinqiao.counselor

import android.content.Context
import com.example.xinqiao.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ScheduleRuleDto(
    val id: Long,
    val frequency: String,
    val startDate: String,
    val endDate: String,
    val startTime: String,
    val endTime: String,
    val weekdays: List<Int>
)

data class ScheduleSlotDto(
    val id: Long,
    val startTime: String,
    val endTime: String,
    val available: Boolean
)

data class ScheduleExceptionDto(
    val id: Long,
    val date: String
)

class CounselorScheduleRepository(private val context: Context) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun listRules(): Result<List<ScheduleRuleDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().listScheduleRules()
            val s = resp.body()?.string() ?: "[]"
            val arr = JSONArray(s)
            val out = mutableListOf<ScheduleRuleDto>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val w = o.optJSONArray("weekdays") ?: JSONArray()
                val wl = mutableListOf<Int>()
                for (j in 0 until w.length()) wl.add(w.optInt(j))
                out.add(
                    ScheduleRuleDto(
                        id = o.optLong("id"),
                        frequency = o.optString("frequency"),
                        startDate = o.optString("startDate"),
                        endDate = o.optString("endDate"),
                        startTime = o.optString("startTime"),
                        endTime = o.optString("endTime"),
                        weekdays = wl
                    )
                )
            }
            Result.success(out.toList())
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun createRule(frequency: String, startDate: String, endDate: String, startTime: String, endTime: String, weekdays: List<Int>): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().createScheduleRule(frequency, startDate, endDate, startTime, endTime, weekdays.joinToString(","))
            Result.success(resp.isSuccessful)
        }.getOrElse { Result.success(false) }
    }

    suspend fun addException(ruleId: Long, date: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().addScheduleException(ruleId, date)
            Result.success(resp.isSuccessful)
        }.getOrElse { Result.success(false) }
    }

    suspend fun generate(from: String, to: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().generateSlots(from, to)
            val s = resp.body()?.string()?.trim() ?: "0"
            Result.success(s.toIntOrNull() ?: 0)
        }.getOrElse { Result.success(0) }
    }

    suspend fun listSlots(from: String?, to: String?): Result<List<ScheduleSlotDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().counselorSchedule(from, to)
            val s = resp.body()?.string() ?: "[]"
            val arr = JSONArray(s)
            val out = mutableListOf<ScheduleSlotDto>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    ScheduleSlotDto(
                        id = o.optLong("id"),
                        startTime = o.optString("startTime"),
                        endTime = o.optString("endTime"),
                        available = o.optBoolean("available", true)
                    )
                )
            }
            Result.success(out.toList())
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun openSlot(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { Result.success(com.example.xinqiao.network.Http.api().openSlot(id).isSuccessful) }.getOrElse { Result.success(false) }
    }

    suspend fun closeSlot(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { Result.success(com.example.xinqiao.network.Http.api().closeSlot(id).isSuccessful) }.getOrElse { Result.success(false) }
    }

    suspend fun listExceptions(ruleId: Long): Result<List<ScheduleExceptionDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().listScheduleExceptions(ruleId)
            val s = resp.body()?.string() ?: "[]"
            val arr = JSONArray(s)
            val out = mutableListOf<ScheduleExceptionDto>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(ScheduleExceptionDto(id = o.optLong("id"), date = o.optString("date")))
            }
            Result.success(out.toList())
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun deleteRule(ruleId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { Result.success(com.example.xinqiao.network.Http.api().deleteRule(ruleId).isSuccessful) }.getOrElse { Result.success(false) }
    }

    suspend fun deleteException(exId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { Result.success(com.example.xinqiao.network.Http.api().deleteException(exId).isSuccessful) }.getOrElse { Result.success(false) }
    }
}

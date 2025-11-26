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
    private val client = OkHttpClient()
    private val baseUrl: String by lazy { NetworkConfig.getBaseUrl(context) }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun token(): String? = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE).getString("auth_token", null)

    private fun auth(req: Request.Builder): Request.Builder {
        val t = token()
        if (!t.isNullOrEmpty()) req.addHeader("Authorization", "Bearer $t")
        return req
    }

    suspend fun listRules(): Result<List<ScheduleRuleDto>> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/rules")).build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string() ?: "[]"
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
            }
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun createRule(frequency: String, startDate: String, endDate: String, startTime: String, endTime: String, weekdays: List<Int>): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/counselor/schedule/rules?frequency=$frequency&startDate=$startDate&endDate=$endDate&startTime=$startTime&endTime=$endTime&weekdays=${weekdays.joinToString(",")}"
        val req = auth(Request.Builder().url(url).post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun addException(ruleId: Long, date: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/rules/$ruleId/exceptions?date=$date").post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun generate(from: String, to: String): Result<Int> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/rules/generate?from=$from&to=$to").post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string()?.trim() ?: "0"
                Result.success(s.toIntOrNull() ?: 0)
            }
        }.getOrElse { Result.success(0) }
    }

    suspend fun listSlots(from: String?, to: String?): Result<List<ScheduleSlotDto>> = withContext(Dispatchers.IO) {
        val q = if (from != null && to != null) "?from=$from&to=$to" else ""
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule$q")).build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string() ?: "[]"
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
            }
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun openSlot(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/$id/open").post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun closeSlot(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/$id/close").post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun listExceptions(ruleId: Long): Result<List<ScheduleExceptionDto>> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/rules/$ruleId/exceptions")).build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string() ?: "[]"
                val arr = JSONArray(s)
                val out = mutableListOf<ScheduleExceptionDto>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(ScheduleExceptionDto(id = o.optLong("id"), date = o.optString("date")))
                }
                Result.success(out.toList())
            }
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun deleteRule(ruleId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/rules/$ruleId").delete(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun deleteException(exId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/counselor/schedule/rules/exceptions/$exId").delete(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }
}
package com.example.xinqiao.user

import android.content.Context
import com.example.xinqiao.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import com.example.xinqiao.network.AuthInterceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class UserAppointmentDto(
    val id: Long,
    val counselor: String,
    val startTime: String,
    val endTime: String,
    val status: String
)

class UserAppointmentRepository(private val context: Context) {
    private val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(context)).build()
    private val baseUrl: String by lazy { NetworkConfig.getBaseUrl(context) }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private fun token(): String? = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE).getString("auth_token", null)
    private fun auth(req: Request.Builder): Request.Builder { val t = token(); if (!t.isNullOrEmpty()) req.addHeader("Authorization", "Bearer $t"); return req }

    suspend fun listMine(): Result<List<UserAppointmentDto>> = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$baseUrl/api/appointments/mine")).build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string() ?: "[]"
                val arr = JSONArray(s)
                val out = mutableListOf<UserAppointmentDto>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out.add(
                        UserAppointmentDto(
                            id = o.optLong("id"),
                            counselor = o.optString("counselorUsername"),
                            startTime = o.optString("startTime"),
                            endTime = o.optString("endTime"),
                            status = o.optString("status")
                        )
                    )
                }
                Result.success(out.toList())
            }
        }.getOrElse { Result.success(emptyList()) }
    }

    suspend fun request(counselor: String, start: String, end: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/appointments?counselor=$counselor&start=$start&end=$end"
        val req = auth(Request.Builder().url(url).post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun cancel(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/appointments/$id/cancel"
        val req = auth(Request.Builder().url(url).post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }

    suspend fun reschedule(id: Long, date: String, time: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/appointments/$id/reschedule?date=$date&time=$time"
        val req = auth(Request.Builder().url(url).post(JSONObject().toString().toRequestBody(jsonMedia))).build()
        runCatching { client.newCall(req).execute().use { Result.success(it.isSuccessful) } }.getOrElse { Result.success(false) }
    }
}
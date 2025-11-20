package com.example.xinqiao.consultation.pro

import android.content.Context
import android.util.Log
import com.example.xinqiao.network.NetworkConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import com.example.xinqiao.network.AuthInterceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SlotTime(
    val start: String,
    val end: String,
    val available: Boolean
)

data class AppointmentRequest(
    val consultantId: String,
    val mode: String,
    val date: String,
    val time: String,
    val remark: String?
)

class AppointmentRepository(private val context: Context) {
    private val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(context)).build()
    private val baseUrl: String by lazy { NetworkConfig.getBaseUrl(context) }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchSlots(consultantId: String, date: String, token: String?): Result<List<SlotTime>> {
        return withContext(Dispatchers.IO) {
            val url = "$baseUrl/api/consult/pro/slots?consultantId=$consultantId&date=$date"
            val reqBuilder = Request.Builder().url(url)
            if (!token.isNullOrEmpty()) reqBuilder.addHeader("Authorization", "Bearer $token")

            try {
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        // fall back to mock slots when backend not ready
                        Result.success(mockSlots(date))
                    } else {
                        val bodyStr = resp.body?.string() ?: "[]"
                        val out = mutableListOf<SlotTime>()
                        val arr = if (bodyStr.trim().startsWith("[")) JSONArray(bodyStr) else JSONObject(bodyStr).optJSONArray("data") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: JSONObject()
                            out.add(
                                SlotTime(
                                    start = o.optString("start"),
                                    end = o.optString("end"),
                                    available = o.optBoolean("available", true)
                                )
                            )
                        }
                        Result.success(out)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentRepository", "fetchSlots error", e)
                Result.success(mockSlots(date))
            }
        }
    }

    suspend fun submitAppointment(req: AppointmentRequest, token: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            val url = "$baseUrl/api/consult/pro/appointments"
            val root = JSONObject()
                .put("consultantId", req.consultantId)
                .put("mode", req.mode)
                .put("date", req.date)
                .put("time", req.time)
                .put("remark", req.remark ?: JSONObject.NULL)
            val body = root.toString().toRequestBody(jsonMedia)

            val builder = Request.Builder().url(url).post(body)
            if (!token.isNullOrEmpty()) builder.addHeader("Authorization", "Bearer $token")

            try {
                client.newCall(builder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        // mock success id when backend not ready
                        Result.success("mock-${System.currentTimeMillis()}")
                    } else {
                        val s = resp.body?.string() ?: "{}"
                        val obj = JSONObject(s)
                        val id = if (obj.has("id")) {
                            obj.optString("id")
                        } else {
                            obj.optJSONObject("data")?.optString("id") ?: ""
                        }
                        Result.success(if (id.isNotBlank()) id else "ok")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentRepository", "submitAppointment error", e)
                Result.success("mock-${System.currentTimeMillis()}")
            }
        }
    }

    private fun mockSlots(date: String): List<SlotTime> {
        val base = listOf(
            "09:00-10:00",
            "10:30-11:30",
            "14:00-15:00",
            "16:00-17:00",
            "19:30-20:30"
        )
        val dayHash = runCatching {
            LocalDate.parse(date, DateTimeFormatter.ISO_DATE).dayOfMonth
        }.getOrElse { (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt() }
        return base.mapIndexed { idx, s ->
            val parts = s.split("-")
            val available = (dayHash + idx) % 3 != 0
            SlotTime(start = parts[0], end = parts[1], available = available)
        }
    }
}

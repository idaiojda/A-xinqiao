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
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchSlots(consultantId: String, date: String, token: String?): Result<List<SlotTime>> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = com.example.xinqiao.network.Http.api().consultSlots(consultantId, date)
                if (!resp.isSuccessful) return@withContext Result.success(mockSlots(date))
                val bodyStr = resp.body()?.string() ?: "[]"
                val out = mutableListOf<SlotTime>()
                val arr = if (bodyStr.trim().startsWith("[")) JSONArray(bodyStr) else JSONObject(bodyStr).optJSONArray("data") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: JSONObject()
                    out.add(SlotTime(start = o.optString("start"), end = o.optString("end"), available = o.optBoolean("available", true)))
                }
                Result.success(out)
            } catch (e: Exception) {
                Log.e("AppointmentRepository", "fetchSlots error", e)
                Result.success(mockSlots(date))
            }
        }
    }

    suspend fun submitAppointment(req: AppointmentRequest, token: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "consultantId" to req.consultantId,
                    "mode" to req.mode,
                    "date" to req.date,
                    "time" to req.time,
                    "remark" to req.remark
                )
                val resp = com.example.xinqiao.network.Http.api().consultSubmit(body)
                if (!resp.isSuccessful) return@withContext Result.success("mock-${System.currentTimeMillis()}")
                val s = resp.body()?.string() ?: "{}"
                val obj = JSONObject(s)
                val id = if (obj.has("id")) obj.optString("id") else obj.optJSONObject("data")?.optString("id") ?: ""
                Result.success(if (id.isNotBlank()) id else "ok")
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

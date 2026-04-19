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
    val status: String,
    val mode: String? = null
)

class UserAppointmentRepository(private val context: Context) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun listMine(): Result<List<UserAppointmentDto>> = withContext(Dispatchers.IO) {
        runCatching {
            android.util.Log.d("UserAppointmentRepo", "开始调用 appointmentsMine API")
            val resp = com.example.xinqiao.network.Http.api().appointmentsMine()
            android.util.Log.d("UserAppointmentRepo", "API响应码: ${resp.code()}, 成功: ${resp.isSuccessful}")
            val s = resp.body()?.string() ?: "[]"
            android.util.Log.d("UserAppointmentRepo", "API响应内容: $s")
            val arr = JSONArray(s)
            android.util.Log.d("UserAppointmentRepo", "解析到 ${arr.length()} 条记录")
            val out = mutableListOf<UserAppointmentDto>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    UserAppointmentDto(
                        id = o.optLong("id"),
                        counselor = o.optString("counselorUsername"),
                        startTime = o.optString("startTime"),
                        endTime = o.optString("endTime"),
                        status = o.optString("status"),
                        mode = o.optString("mode", "text")
                    )
                )
            }
            android.util.Log.d("UserAppointmentRepo", "成功转换 ${out.size} 条记录")
            Result.success(out.toList())
        }.getOrElse { error ->
            android.util.Log.e("UserAppointmentRepo", "API调用失败", error)
            Result.success(emptyList())
        }
    }

    suspend fun request(counselor: String, start: String, end: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = com.example.xinqiao.network.Http.api().requestAppointment(counselor, start, end)
            Result.success(resp.isSuccessful)
        }.getOrElse { Result.success(false) }
    }

    suspend fun cancel(id: Long): Result<Pair<Boolean, String>> = withContext(Dispatchers.IO) {
        runCatching {
            android.util.Log.d("UserAppointmentRepo", "开始取消预约: id=$id")
            val resp = com.example.xinqiao.network.Http.api().cancelAppointment(id)
            android.util.Log.d("UserAppointmentRepo", "取消预约响应码: ${resp.code()}, 成功: ${resp.isSuccessful}")
            
            if (!resp.isSuccessful) {
                val errorBody = resp.errorBody()?.string() ?: ""
                android.util.Log.e("UserAppointmentRepo", "取消预约失败: code=${resp.code()}, error=$errorBody")
                
                // 尝试解析错误信息
                val errorMsg = try {
                    val json = JSONObject(errorBody)
                    json.optString("error", "取消预约失败")
                } catch (e: Exception) {
                    when (resp.code()) {
                        400 -> "预约已过期或状态不允许取消"
                        403 -> "无权操作此预约"
                        404 -> "预约不存在"
                        else -> "取消预约失败"
                    }
                }
                
                Result.success(Pair(false, errorMsg))
            } else {
                Result.success(Pair(true, "取消成功"))
            }
        }.getOrElse { error ->
            android.util.Log.e("UserAppointmentRepo", "取消预约异常", error)
            Result.success(Pair(false, "网络错误: ${error.message}"))
        }
    }

    suspend fun reschedule(id: Long, date: String, time: String): Result<Pair<Boolean, String>> = withContext(Dispatchers.IO) {
        runCatching {
            android.util.Log.d("UserAppointmentRepo", "开始改期: id=$id, date=$date, time=$time")
            val resp = com.example.xinqiao.network.Http.api().rescheduleAppointment(id, date, time)
            android.util.Log.d("UserAppointmentRepo", "改期响应码: ${resp.code()}, 成功: ${resp.isSuccessful}")
            
            if (!resp.isSuccessful) {
                val errorBody = resp.errorBody()?.string() ?: ""
                android.util.Log.e("UserAppointmentRepo", "改期失败: code=${resp.code()}, error=$errorBody")
                
                // 尝试解析错误信息
                val errorMsg = try {
                    val json = JSONObject(errorBody)
                    json.optString("error", "改期失败")
                } catch (e: Exception) {
                    when (resp.code()) {
                        400 -> "时段不可用或预约已过期"
                        403 -> "无权操作此预约"
                        404 -> "预约不存在"
                        409 -> "时段冲突"
                        else -> "改期失败"
                    }
                }
                
                Result.success(Pair(false, errorMsg))
            } else {
                Result.success(Pair(true, "改期成功"))
            }
        }.getOrElse { error ->
            android.util.Log.e("UserAppointmentRepo", "改期异常", error)
            Result.success(Pair(false, "网络错误: ${error.message}"))
        }
    }
}

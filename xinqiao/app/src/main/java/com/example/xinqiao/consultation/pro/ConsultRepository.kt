package com.example.xinqiao.consultation.pro

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ConsultRepository(private val context: Context) {
    private val client = OkHttpClient()

    // 可根据实际部署环境调整，模拟器使用 10.0.2.2 指向宿主机
    private val baseUrl: String = "http://10.0.2.2:8080"

    fun fetchConsultants(
        field: String?,
        mode: String?,
        sort: String?,
        page: Int,
        size: Int,
        token: String?
    ): Result<List<Consultant>> {
        val url = StringBuilder("$baseUrl/api/consult/pro/list")
            .append("?page=").append(page)
            .append("&size=").append(size)
        if (!field.isNullOrEmpty()) url.append("&field=").append(field)
        if (!mode.isNullOrEmpty()) url.append("&mode=").append(mode)
        if (!sort.isNullOrEmpty()) url.append("&sort=").append(sort)

        val reqBuilder = Request.Builder().url(url.toString())
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        return try {
            val resp = client.newCall(reqBuilder.build()).execute()
            if (!resp.isSuccessful) {
                Result.failure(RuntimeException("HTTP ${resp.code}"))
            } else {
                val bodyStr = resp.body?.string() ?: "{}"
                val root = JSONObject(bodyStr)
                val dataArr: JSONArray = when {
                    root.has("data") -> root.getJSONArray("data")
                    root.has("list") -> root.getJSONArray("list")
                    else -> JSONArray()
                }
                val list = mutableListOf<Consultant>()
                for (i in 0 until dataArr.length()) {
                    val o = dataArr.getJSONObject(i)
                    list.add(
                        Consultant(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            title = o.optString("title"),
                            avatarUrl = o.optString("avatar"),
                            certified = o.optBoolean("certified", true),
                            skills = o.optJSONArray("skills")?.let { ja ->
                                List(ja.length()) { idx -> ja.optString(idx) }
                            } ?: emptyList(),
                            rating = o.optDouble("rating", 4.8),
                            consultCount = o.optInt("consultCount", 0),
                            price = o.optInt("price", 299),
                            durationMinutes = o.optInt("duration", 60),
                            defaultMode = o.optString("defaultMode", "文字咨询")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e("ConsultRepository", "fetchConsultants error", e)
            Result.failure(e)
        }
    }
}
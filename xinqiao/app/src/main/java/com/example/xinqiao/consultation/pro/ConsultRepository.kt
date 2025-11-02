package com.example.xinqiao.consultation.pro

import android.content.Context
import android.util.Log
import com.example.xinqiao.network.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class ConsultRepository(private val context: Context) {
    private val client = OkHttpClient()

    // Resolve base URL dynamically (emulator/genymotion/physical device with adb reverse),
    // and allow SP override via NetworkConfig.
    private val baseUrl: String by lazy { NetworkConfig.getBaseUrl(context) }

    private fun normalizeCity(name: String): String {
        var s = name.trim()
        if (s.endsWith("自治区")) s = s.removeSuffix("自治区")
        if (s.endsWith("特别行政区")) s = s.removeSuffix("特别行政区")
        if (s.endsWith("省")) s = s.removeSuffix("省")
        if (s.endsWith("市")) s = s.removeSuffix("市")
        return s
    }

    fun fetchConsultants(
        field: String?,
        mode: String?,
        sort: String?,
        city: String?,
        page: Int,
        size: Int,
        token: String?
    ): Result<List<Consultant>> {
        val url = StringBuilder("$baseUrl/api/consult/pro/list")
            .append("?page=").append(page)
            .append("&size=").append(size)
        // 避免发送“全部”以免后端误判为具体过滤值
        if (!field.isNullOrEmpty() && field != "全部") url.append("&field=").append(field)
        if (!mode.isNullOrEmpty() && mode != "全部") url.append("&mode=").append(mode)
        if (!sort.isNullOrEmpty()) url.append("&sort=").append(sort)
        if (!city.isNullOrBlank() && city != "全部") {
            val norm = normalizeCity(city)
            val encoded = try { URLEncoder.encode(norm, "UTF-8") } catch (e: Exception) { norm }
            url.append("&city=").append(encoded)
        }

        val reqBuilder = Request.Builder().url(url.toString())
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
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
                                defaultMode = o.optString("defaultMode", "文字咨询"),
                                city = o.optString("city", null)
                            )
                        )
                    }
                    Result.success(list)
                }
            }
        } catch (e: Exception) {
            Log.e("ConsultRepository", "fetchConsultants error", e)
            Result.failure(e)
        }
    }

    fun fetchCities(token: String?): Result<List<String>> {
        val url = "$baseUrl/api/consult/pro/cities"
        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Result.failure(RuntimeException("HTTP ${resp.code}"))
                } else {
                    val bodyStr = resp.body?.string() ?: "[]"
                    val out = mutableListOf<String>()
                    // 兼容数组或带 data 包装
                    if (bodyStr.trim().startsWith("[")) {
                        val arr = JSONArray(bodyStr)
                        for (i in 0 until arr.length()) {
                            val v = arr.optString(i)
                            if (!v.isNullOrBlank()) out.add(v)
                        }
                    } else {
                        val root = JSONObject(bodyStr)
                        val arr = when {
                            root.has("data") -> root.getJSONArray("data")
                            root.has("list") -> root.getJSONArray("list")
                            else -> JSONArray()
                        }
                        for (i in 0 until arr.length()) {
                            val v = arr.optString(i)
                            if (!v.isNullOrBlank()) out.add(v)
                        }
                    }
                    Result.success(out)
                }
            }
        } catch (e: Exception) {
            Log.e("ConsultRepository", "fetchCities error", e)
            Result.failure(e)
        }
    }

    fun fetchCityDict(token: String?): Result<CityDict> {
        val url = "$baseUrl/api/consult/pro/cityDict"
        val reqBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Result.failure(RuntimeException("HTTP ${resp.code}"))
                } else {
                    val bodyStr = resp.body?.string() ?: "{}"
                    val root = JSONObject(bodyStr)
                    val tabs = mutableListOf<CityTab>()
                    val tabsArr = root.optJSONArray("tabs") ?: JSONArray()
                    for (i in 0 until tabsArr.length()) {
                        val tObj = tabsArr.optJSONObject(i) ?: JSONObject()
                        val label = tObj.optString("label", "")
                        val groups = mutableListOf<CityGroup>()
                        val groupsArr = tObj.optJSONArray("groups") ?: JSONArray()
                        for (j in 0 until groupsArr.length()) {
                            val gObj = groupsArr.optJSONObject(j) ?: JSONObject()
                            val gLabel = gObj.optString("label", "")
                            val gCities = mutableListOf<String>()
                            val cArr = gObj.optJSONArray("cities") ?: JSONArray()
                            for (k in 0 until cArr.length()) {
                                val v = cArr.optString(k)
                                if (!v.isNullOrBlank()) gCities.add(v)
                            }
                            if (gLabel.isNotBlank()) groups.add(CityGroup(gLabel, gCities))
                        }
                        val simpleCities = mutableListOf<String>()
                        val simpleArr = tObj.optJSONArray("cities") ?: JSONArray()
                        for (j in 0 until simpleArr.length()) {
                            val v = simpleArr.optString(j)
                            if (!v.isNullOrBlank()) simpleCities.add(v)
                        }
                        tabs.add(CityTab(label = label, groups = groups, cities = simpleCities))
                    }
                    Result.success(CityDict(tabs))
                }
            }
        } catch (e: Exception) {
            Log.e("ConsultRepository", "fetchCityDict error", e)
            Result.failure(e)
        }
    }

    fun reverseGeocode(lat: Double, lon: Double): Result<String?> {
        val url = "$baseUrl/api/geo/reverse?lat=$lat&lon=$lon"
        val req = Request.Builder().url(url).build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Result.failure(RuntimeException("HTTP ${resp.code}"))
                } else {
                    val bodyStr = resp.body?.string() ?: "{}"
                    val root = JSONObject(bodyStr)
                    val ok = root.optBoolean("ok", false)
                    val city = if (ok) root.optString("city", null) else null
                    Result.success(if (city.isNullOrBlank()) null else city)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

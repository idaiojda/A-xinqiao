package com.example.xinqiao.consultation.pro

import android.content.Context
import android.util.Log
import com.example.xinqiao.network.NetworkConfig
import com.example.xinqiao.network.AMapConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    // 清洗城市名：过滤 "[]"、空值，统一去后缀
    private fun sanitizeCity(name: String?): String? {
        val raw = name?.trim()
        if (raw.isNullOrEmpty()) return null
        if (raw == "[]") return null
        val norm = normalizeCity(raw)
        return norm.takeIf { it.isNotBlank() }
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
                        // 处理头像URL：支持 base64 格式
                        var avatarUrl = o.optString("avatar")
                        Log.d("ConsultRepository", "Received avatar for ${o.optString("id")}: ${avatarUrl.take(100)}...")
                        if (!avatarUrl.startsWith("data:image/") && avatarUrl.startsWith("/api/")) {
                            // 只对 API 路径添加时间戳，base64 不需要
                            avatarUrl = "$avatarUrl?t=${System.currentTimeMillis()}"
                        }
                        list.add(
                            Consultant(
                                id = o.optString("id"),
                                name = o.optString("name"),
                                title = o.optString("title"),
                                avatarUrl = avatarUrl,
                                certified = o.optBoolean("certified", true),
                                skills = o.optJSONArray("skills")?.let { ja ->
                                    List(ja.length()) { idx -> ja.optString(idx) }
                                } ?: emptyList(),
                                rating = o.optDouble("rating", 4.8),
                                consultCount = o.optInt("consultCount", 0),
                                price = o.optInt("price", 0),
                                priceText = o.optInt("priceText", 0),
                                priceVoice = o.optInt("priceVoice", 0),
                                priceVideo = o.optInt("priceVideo", 0),
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
    
    fun fetchExpertiseTags(token: String?): Result<List<String>> {
        val url = "$baseUrl/api/consult/pro/tags"
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
            Log.e("ConsultRepository", "fetchExpertiseTags error", e)
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
        // 1) 优先使用高德逆地理解析
        val amapKey = AMapConfig.getAmapWebKey(context)
        if (!amapKey.isNullOrBlank()) {
            try {
                val aUrl = "https://restapi.amap.com/v3/geocode/regeo?output=json&location=$lon,$lat&key=$amapKey"
                val aReq = Request.Builder().url(aUrl).build()
                client.newCall(aReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string() ?: "{}"
                        val root = JSONObject(bodyStr)
                        if (root.optString("status") == "1") {
                            val reg = root.optJSONObject("regeocode") ?: JSONObject()
                            val comp = reg.optJSONObject("addressComponent") ?: JSONObject()
                            val cityRaw = comp.optString("city", null)
                            val provRaw = comp.optString("province", null)
                            val city = sanitizeCity(cityRaw) ?: sanitizeCity(provRaw)
                            if (!city.isNullOrBlank()) return Result.success(city)
                        }
                    }
                }
            } catch (_: Exception) { /* fall back to backend below */ }
        }

        // 2) 回退到后端 OSM 接口
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
                    val norm = sanitizeCity(city)
                    Result.success(norm)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 按公网 IP 推断城市（兜底，模拟器/无定位服务场景）。
     * 说明：
     * - 使用 ipapi.co 免费接口，无需密钥；
     * - 若无法访问外网或返回为空，返回 null；
     */
    fun detectCityByIp(): Result<String?> {
        // 1) 优先使用高德 IP 定位
        val amapKey = AMapConfig.getAmapWebKey(context)
        if (!amapKey.isNullOrBlank()) {
            try {
                val aUrl = "https://restapi.amap.com/v3/ip?output=json&key=$amapKey"
                val aReq = Request.Builder().url(aUrl).build()
                client.newCall(aReq).execute().use { resp ->
                    if (!resp.isSuccessful) return Result.failure(RuntimeException("HTTP ${resp.code}"))
                    val bodyStr = resp.body?.string() ?: "{}"
                    val root = JSONObject(bodyStr)
                    if (root.optString("status") == "1") {
                        val cityRaw = root.optString("city", null)
                        val provRaw = root.optString("province", null)
                        val city = sanitizeCity(cityRaw) ?: sanitizeCity(provRaw)
                        if (!city.isNullOrBlank()) return Result.success(city)
                    }
                }
            } catch (_: Exception) { /* fall through to ipapi */ }
        }

        // 2) 回退到 ipapi.co
        return try {
            val req = Request.Builder().url("https://ipapi.co/json/").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(RuntimeException("HTTP ${resp.code}"))
                }
                val bodyStr = resp.body?.byteString()?.string(StandardCharsets.UTF_8) ?: "{}"
                val root = JSONObject(bodyStr)
                val city = root.optString("city", null)
                val norm = sanitizeCity(city)
                Result.success(norm)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 获取咨询师详情
    fun fetchConsultantDetail(consultantId: String, token: String?): Result<ConsultantDetail> {
        val url = "$baseUrl/api/consult/pro/detail/$consultantId"
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
                    val o = JSONObject(bodyStr)
                    
                    val certificates = mutableListOf<Certificate>()
                    val certsArr = o.optJSONArray("certificates") ?: JSONArray()
                    for (i in 0 until certsArr.length()) {
                        val certObj = certsArr.getJSONObject(i)
                        certificates.add(
                            Certificate(
                                title = certObj.optString("title"),
                                desc = certObj.optString("desc")
                            )
                        )
                    }
                    
                    val cases = mutableListOf<CaseItem>()
                    val casesArr = o.optJSONArray("cases") ?: JSONArray()
                    for (i in 0 until casesArr.length()) {
                        val caseObj = casesArr.getJSONObject(i)
                        cases.add(
                            CaseItem(
                                title = caseObj.optString("title"),
                                time = caseObj.optString("time"),
                                summary = caseObj.optString("summary")
                            )
                        )
                    }
                    
                    val reviewDist = mutableListOf<Int>()
                    val reviewArr = o.optJSONArray("reviewDist") ?: JSONArray()
                    for (i in 0 until reviewArr.length()) {
                        reviewDist.add(reviewArr.optInt(i))
                    }
                    
                    val skills = mutableListOf<String>()
                    val skillsArr = o.optJSONArray("skills") ?: JSONArray()
                    for (i in 0 until skillsArr.length()) {
                        skills.add(skillsArr.optString(i))
                    }
                    
                    // 添加时间戳参数绕过缓存
                    var avatarUrl = o.optString("avatarUrl")
                    if (avatarUrl.startsWith("/api/")) {
                        avatarUrl = "$avatarUrl?t=${System.currentTimeMillis()}"
                    }
                    var bannerUrl = o.optString("bannerUrl")
                    if (bannerUrl.startsWith("/api/")) {
                        bannerUrl = "$bannerUrl?t=${System.currentTimeMillis()}"
                    }
                    
                    val detail = ConsultantDetail(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        title = o.optString("title"),
                        avatarUrl = avatarUrl,
                        bannerUrl = bannerUrl,
                        rating = o.optDouble("rating", 4.8),
                        consultCount = o.optInt("consultCount", 0),
                        price = o.optInt("price", 299),
                        basePrice = o.optInt("basePrice", 0),
                        priceText = o.optInt("priceText", 0),
                        priceVoice = o.optInt("priceVoice", 0),
                        priceVideo = o.optInt("priceVideo", 0),
                        defaultMode = o.optString("defaultMode", "文字咨询"),
                        city = o.optString("city"),
                        skills = skills,
                        certificates = certificates,
                        cases = cases,
                        reviewDist = reviewDist,
                        intro = o.optString("intro"),
                        education = o.optString("education", "本科"),
                        workYear = o.optString("workYear", "3 年")
                    )
                    Result.success(detail)
                }
            }
        } catch (e: Exception) {
            Log.e("ConsultRepository", "fetchConsultantDetail error", e)
            Result.failure(e)
        }
    }
}

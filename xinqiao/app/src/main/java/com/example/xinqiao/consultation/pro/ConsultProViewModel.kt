package com.example.xinqiao.consultation.pro

import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class ConsultProViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ConsultRepository(app)

    private val _consultants = MutableStateFlow<List<Consultant>>(emptyList())
    val consultants: StateFlow<List<Consultant>> = _consultants

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _cities = MutableStateFlow<List<String>>(listOf("全部"))
    val cities: StateFlow<List<String>> = _cities

    private val _cityDict = MutableStateFlow<CityDict?>(null)
    val cityDict: StateFlow<CityDict?> = _cityDict

    private val _locationCity = MutableStateFlow<String?>(null)
    val locationCity: StateFlow<String?> = _locationCity

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    private val _recentCities = MutableStateFlow<List<String>>(emptyList())
    val recentCities: StateFlow<List<String>> = _recentCities

    private var page = 1
    private val size = 10
    private var lastDetectAt = 0L

    // 过滤条件
    private val sp by lazy { app.getSharedPreferences("consult_filters", Context.MODE_PRIVATE) }
    var field: String? = sp.getString("field", "全部")
    var mode: String? = sp.getString("mode", "全部")
    var sort: String? = sp.getString("sort", "综合评分")
    var city: String? = sp.getString("city", "全部")
    init {
        // 初始化定位与最近浏览
        val initLc = sp.getString("location_city", null)
        _locationCity.value = if (initLc == "[]") null else initLc
        _recentCities.value = parseCsv(sp.getString("recent_cities", ""))
    }

    fun setFilters(field: String?, mode: String?, sort: String?) {
        this.field = field
        this.mode = mode
        this.sort = sort
        sp.edit().putString("field", field).putString("mode", mode).putString("sort", sort).apply()
    }

    fun updateCity(city: String?) {
        this.city = city
        sp.edit().putString("city", city).apply()
        // 记录最近浏览（排除“全部”与空值）
        val c = city ?: return
        if (c.isNotBlank() && c != "全部") addRecentCityInternal(c)
    }

    fun setLocationCity(city: String?) {
        val safe = if (city == "[]") null else city
        _locationCity.value = safe
        sp.edit().putString("location_city", safe).apply()
    }

    fun clearRecentCities() {
        _recentCities.value = emptyList()
        sp.edit().putString("recent_cities", "").apply()
    }

    fun refresh(token: String?) {
        page = 1
        loadInternal(reset = true, token = token)
    }

    fun loadMore(token: String?) {
        if (_loading.value) return
        page += 1
        loadInternal(reset = false, token = token)
    }

    private fun loadInternal(reset: Boolean, token: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _error.value = null
            val result = repo.fetchConsultants(normalize(field), normalize(mode), normalize(sort), normalize(city), page, size, token)
            result.onSuccess { list ->
                _consultants.value = if (reset) list else _consultants.value + list
            }.onFailure { e ->
                _error.value = e.message
                page = maxOf(1, page - 1)
            }
            _loading.value = false
        }
    }

    fun loadCityDict(token: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            // 新结构优先
            val dictRes = repo.fetchCityDict(token)
            dictRes.onSuccess { d ->
                _cityDict.value = d
                // 同步一个简单的扁平列表（用于历史逻辑/兜底）
                val merged = mutableListOf<String>("全部")
                d.tabs.forEach { tab ->
                    tab.cities.forEach { if (!merged.contains(it)) merged.add(it) }
                    tab.groups.forEach { g -> g.cities.forEach { if (!merged.contains(it)) merged.add(it) } }
                }
                _cities.value = merged
            }.onFailure {
                // 回退到旧接口（如果后端暂不支持新结构）
                val res = repo.fetchCities(token)
                res.onSuccess { list ->
                    val merged = mutableListOf<String>("全部")
                    for (c in list) if (!merged.contains(c)) merged.add(c)
                    _cities.value = merged
                }
            }
        }
    }

    // 定位并写入定位城市（需要在 UI 层已授予定位权限）
    fun detectLocationCity(hasLocationPermission: Boolean) {
        if (!hasLocationPermission) {
            _locationError.value = "未授予定位权限"
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastDetectAt < 10_000) {
            _locationError.value = "定位太频繁，请稍后再试"
            return
        }
        lastDetectAt = now
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _locationError.value = null
                val ctx = getApplication<Application>()
                // 若 Google Play 服务不可用，则回退到系统 LocationManager
                val gmsAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) == ConnectionResult.SUCCESS
                if (!gmsAvailable) {
                    val loc = getLocationViaLocationManager(ctx)
                    if (loc != null) {
                        val apiRes = repo.reverseGeocode(loc.latitude, loc.longitude)
                        var city: String? = null
                        if (apiRes.isSuccess) city = apiRes.getOrNull()?.let { normalizeCityName(it) }
                        if (city.isNullOrBlank()) city = geocodeCity(ctx, loc.latitude, loc.longitude)
                        // 仍失败则按公网 IP 兜底推断城市
                        if (city.isNullOrBlank()) {
                            val ipRes = repo.detectCityByIp()
                            if (ipRes.isSuccess) city = ipRes.getOrNull()?.let { normalizeCityName(it) }
                        }
                        if (!city.isNullOrBlank()) {
                            setLocationCity(city)
                        } else {
                            _locationError.value = "定位失败（无Google服务），请手动选择城市"
                        }
                    } else {
                        // 无最近位置与 provider 时，直接尝试 IP 城市识别
                        val ipRes = repo.detectCityByIp()
                        val city = if (ipRes.isSuccess) ipRes.getOrNull()?.let { normalizeCityName(it) } else null
                        if (!city.isNullOrBlank()) {
                            setLocationCity(city)
                        } else {
                            _locationError.value = "定位不可用（无Google服务），请手动选择城市"
                        }
                    }
                    return@launch
                }
                val fused = LocationServices.getFusedLocationProviderClient(ctx)
                // 优先尝试当前定位，其次回退到最近一次定位
                val cts = CancellationTokenSource()
                // 优先使用高精度，提升获取当前城市成功率；再回退到最近一次定位；最后低功耗兜底
                val result = withTimeoutOrNull(7_000) {
                    val highAcc = try { Tasks.await(fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)) } catch (_: Exception) { null }
                    val last = try { Tasks.await(fused.lastLocation) } catch (_: Exception) { null }
                    val lowPower = try { Tasks.await(fused.getCurrentLocation(Priority.PRIORITY_LOW_POWER, cts.token)) } catch (_: Exception) { null }
                    val loc = highAcc ?: last ?: lowPower
                    if (loc == null) return@withTimeoutOrNull null
                    // 优先使用后端 API 进行反地理解析
                    val apiRes = repo.reverseGeocode(loc.latitude, loc.longitude)
                    var city: String? = null
                    if (apiRes.isSuccess) city = apiRes.getOrNull()?.let { normalizeCityName(it) }
                    if (city.isNullOrBlank()) city = geocodeCity(ctx, loc.latitude, loc.longitude)
                    // 仍失败则按公网 IP 兜底推断城市
                    if (city.isNullOrBlank()) {
                        val ipRes = repo.detectCityByIp()
                        if (ipRes.isSuccess) city = ipRes.getOrNull()?.let { normalizeCityName(it) }
                    }
                    city
                }
                if (!result.isNullOrBlank()) setLocationCity(result) else _locationError.value = "定位失败，请稍后再试"
            } catch (e: Exception) {
                // 静默失败，不影响正常功能
                _locationError.value = "定位失败，请稍后再试"
            }
        }
    }

    // 使用系统 LocationManager 主动获取一次位置，优先 NETWORK，其次 GPS，带超时与取消
    private suspend fun getLocationViaLocationManager(ctx: Context): Location? {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // 尝试使用最近一次位置作为快速路径
        val lastNet = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        val lastGps = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        if (lastNet != null) return lastNet
        if (lastGps != null) return lastGps

        // 主动请求网络定位一次
        val netEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val netLoc = if (netEnabled) requestSingleUpdate(lm, LocationManager.NETWORK_PROVIDER, 7_000) else null
        if (netLoc != null) return netLoc
        val gpsLoc = if (gpsEnabled) requestSingleUpdate(lm, LocationManager.GPS_PROVIDER, 10_000) else null
        return gpsLoc
    }

    private suspend fun requestSingleUpdate(lm: LocationManager, provider: String, timeoutMs: Long): Location? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        if (!cont.isCompleted) cont.resume(location)
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                try {
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (_: SecurityException) {
                    lm.removeUpdates(listener)
                    if (!cont.isCompleted) cont.resume(null)
                } catch (_: Exception) {
                    lm.removeUpdates(listener)
                    if (!cont.isCompleted) cont.resume(null)
                }
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            }
        }
    }

    private fun geocodeCity(ctx: Context, lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(ctx, Locale.CHINA)
            val list = geocoder.getFromLocation(lat, lon, 1)
            if (list.isNullOrEmpty()) null else normalizeCityName(list[0].locality ?: list[0].subAdminArea ?: list[0].adminArea)
        } catch (_: Exception) { null }
    }

    private fun normalizeCityName(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw == "[]") return null
        var s = raw
        s = s.replace("自治州", "").replace("地区", "").replace("盟", "")
        if (s.endsWith("市")) s = s.substring(0, s.length - 1)
        // 直辖市 / 特别行政区修正
        val mapping = mapOf(
            "北京市" to "北京", "北京" to "北京",
            "上海市" to "上海", "上海" to "上海",
            "天津市" to "天津", "天津" to "天津",
            "重庆市" to "重庆", "重庆" to "重庆",
            "香港特别行政区" to "香港", "香港" to "香港",
            "澳门特别行政区" to "澳门", "澳门" to "澳门"
        )
        return mapping[s] ?: s
    }

    private fun normalize(s: String?): String? {
        return when (s) {
            null, "全部" -> null
            else -> s
        }
    }

    private fun addRecentCityInternal(city: String) {
        if (city.isBlank() || city == "[]") return
        val current = _recentCities.value.toMutableList()
        // 移除重复并放到最前
        current.remove(city)
        current.add(0, city)
        // 限制长度（最多 8 个）
        while (current.size > 8) current.removeLast()
        _recentCities.value = current
        sp.edit().putString("recent_cities", current.joinToString(",")).apply()
    }

    private fun parseCsv(csv: String?): List<String> {
        if (csv.isNullOrBlank()) return emptyList()
        return csv.split(',').map { it.trim() }.filter { it.isNotBlank() && it != "[]" }
    }
}

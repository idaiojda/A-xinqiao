package com.example.xinqiao.network

import android.content.Context
import android.content.SharedPreferences
import com.example.xinqiao.R

/**
 * 高德开放平台 Web 服务 API 密钥配置读取与覆盖工具。
 * 优先使用 SharedPreferences 覆盖，其次使用资源字符串值。
 */
object AMapConfig {
    private const val SP_NAME = "amap_config"
    private const val KEY_OVERRIDE = "key_override"

    @JvmStatic
    fun getAmapWebKey(context: Context): String? {
        val sp: SharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val override = sp.getString(KEY_OVERRIDE, null)
        val candidate = if (!override.isNullOrBlank()) {
            override
        } else {
            try {
                context.getString(R.string.amap_web_key)
            } catch (_: Throwable) {
                null
            }
        }
        val key = candidate?.trim()
        return if (key.isNullOrEmpty() || key.equals("YOUR_AMAP_WEB_KEY", ignoreCase = true)) null else key
    }

    @JvmStatic
    fun setAmapWebKeyOverride(context: Context, key: String?) {
        val sp: SharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        if (key.isNullOrBlank()) {
            sp.edit().remove(KEY_OVERRIDE).apply()
        } else {
            sp.edit().putString(KEY_OVERRIDE, key.trim()).apply()
        }
    }
}


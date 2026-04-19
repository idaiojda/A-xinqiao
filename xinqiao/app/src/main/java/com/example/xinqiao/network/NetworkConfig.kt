package com.example.xinqiao.network

import android.content.Context
import android.os.Build

object NetworkConfig {
    /**
     * Returns the base URL for backend depending on runtime environment.
     * Priority:
     * 1) SharedPreferences override
     * 2) BuildConfig.BACKEND_URL (默认: http://10.0.2.2:8081)
     */
    @JvmStatic
    fun getBaseUrl(context: Context): String {
        val sp = context.getSharedPreferences("network_config", Context.MODE_PRIVATE)
        val override = sp.getString("base_url_override", null)?.trim()?.removeSuffix("/")
        if (!override.isNullOrEmpty()) {
            return override
        }
        
        val forced = com.example.xinqiao.BuildConfig.BACKEND_URL.trim().removeSuffix("/")
        if (forced.isNotEmpty()) {
            return forced
        }

        // 默认使用模拟器地址
        return "http://10.0.2.2:8081"
    }

    private fun isAndroidEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        return fingerprint.contains("generic") || fingerprint.contains("emulator") ||
                model.contains("emulator") || model.contains("android sdk built for") ||
                brand.contains("google") && device.contains("generic") ||
                product.contains("sdk_gphone") || hardware.contains("ranchu") ||
                hardware.contains("goldfish")
    }

    private fun isGenymotion(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("genymotion") || brand.contains("genymotion")
    }
}

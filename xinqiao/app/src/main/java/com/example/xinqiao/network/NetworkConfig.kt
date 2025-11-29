package com.example.xinqiao.network

import android.content.Context
import android.os.Build

object NetworkConfig {
    /**
     * Returns the base URL for backend depending on runtime environment.
     * Priority:
     * 1) SharedPreferences override: SP name "network_config", key "base_url_override".
     * 2) Genymotion emulator -> http://10.0.3.2:8081
     * 3) Android Emulator (AVD) -> http://10.0.2.2:8081
     * 4) Physical device -> http://127.0.0.1:8081 (works with `adb reverse tcp:8081 tcp:8081`)
     */
    @JvmStatic
    fun getBaseUrl(context: Context): String {
        val sp = context.getSharedPreferences("network_config", Context.MODE_PRIVATE)
        val override = sp.getString("base_url_override", null)?.trim()?.removeSuffix("/")
        if (!override.isNullOrEmpty()) {
            val o = override
            val parts = o.split("://")
            val scheme = if (parts.size > 1) parts[0] else "http"
            val rest = if (parts.size > 1) parts[1] else o
            val hostAndPath = rest.split("/", limit = 2)
            val hostRaw = hostAndPath[0]
            val host = hostRaw.substringBefore(":")
            val portStr = hostRaw.substringAfter(":", "")
            val port = portStr.ifEmpty { "8081" }
            val path = if (hostAndPath.size > 1) "/" + hostAndPath[1] else ""
            val lower = host.lowercase()
            val mappedHost = when {
                lower == "127.0.0.1" || lower == "localhost" -> when {
                    isGenymotion() -> "10.0.3.2"
                    isAndroidEmulator() -> "10.0.2.2"
                    else -> "127.0.0.1"
                }
                else -> host
            }
            return "$scheme://$mappedHost:$port$path"
        }
        val forced = com.example.xinqiao.BuildConfig.BACKEND_URL.trim().removeSuffix("/")
        if (forced.isNotEmpty()) {
            val parts = forced.split("://")
            val scheme = if (parts.size > 1) parts[0] else "http"
            val rest = if (parts.size > 1) parts[1] else forced
            val hostAndPath = rest.split("/", limit = 2)
            val hostRaw = hostAndPath[0]
            val host = hostRaw.substringBefore(":")
            val portStr = hostRaw.substringAfter(":", "")
            val port = portStr.ifEmpty { "8081" }
            val path = if (hostAndPath.size > 1) "/" + hostAndPath[1] else ""
            return "$scheme://$host:$port$path"
        }

        return when {
            isGenymotion() -> "http://10.0.3.2:8081"
            isAndroidEmulator() -> "http://10.0.2.2:8081"
            else -> "http://127.0.0.1:8081" // for real device with adb reverse
        }
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

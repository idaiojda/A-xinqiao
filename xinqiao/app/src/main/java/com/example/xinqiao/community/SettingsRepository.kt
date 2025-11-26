package com.example.xinqiao.community

import android.content.Context
import com.example.xinqiao.mysql.MySQLHelper
import com.example.xinqiao.util.AnalysisUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SettingsRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appCtx: Context? = null

    fun init(context: Context) {
        CommunityLocalCache.init(context)
        appCtx = context.applicationContext
    }

    data class Setting(
        val userName: String,
        val privacyMode: String,
        val twoFactorEnabled: Boolean,
        val notificationsEnabled: Boolean,
        val appLanguage: String
    )

    fun get(userName: String?, callback: (Setting?) -> Unit) {
        val name = userName ?: ""
        val db = CommunityLocalCache.database()
        scope.launch {
            val s = db?.settingsDao()?.get(name)
            callback(s?.let { Setting(it.userName, it.privacyMode, it.twoFactorEnabled, it.notificationsEnabled, it.appLanguage) })
        }
    }

    fun updatePrivacy(userName: String?, mode: String) {
        upsert(userName, privacyMode = mode, twoFactorEnabled = null, notificationsEnabled = null, appLanguage = null)
    }

    fun updateTwoFactor(userName: String?, enabled: Boolean) {
        upsert(userName, privacyMode = null, twoFactorEnabled = enabled, notificationsEnabled = null, appLanguage = null)
    }

    fun updateNotifications(userName: String?, enabled: Boolean) {
        upsert(userName, privacyMode = null, twoFactorEnabled = null, notificationsEnabled = enabled, appLanguage = null)
    }

    fun updateLanguage(userName: String?, lang: String) {
        upsert(userName, privacyMode = null, twoFactorEnabled = null, notificationsEnabled = null, appLanguage = lang)
    }

    private fun upsert(
        userName: String?,
        privacyMode: String?,
        twoFactorEnabled: Boolean?,
        notificationsEnabled: Boolean?,
        appLanguage: String?
    ) {
        val name = userName ?: ""
        val db = CommunityLocalCache.database()
        scope.launch {
            val current = db?.settingsDao()?.get(name)
            val entity = AppSettingEntity(
                userName = name,
                privacyMode = privacyMode ?: current?.privacyMode ?: "partial",
                twoFactorEnabled = twoFactorEnabled ?: current?.twoFactorEnabled ?: false,
                notificationsEnabled = notificationsEnabled ?: current?.notificationsEnabled ?: true,
                appLanguage = appLanguage ?: current?.appLanguage ?: "zh"
            )
            try { db?.settingsDao()?.upsert(entity) } catch (_: Exception) {}
            try {
                val ctx = appCtx
                if (ctx != null) {
                    val sp = ctx.getSharedPreferences("user_settings_cache", Context.MODE_PRIVATE)
                    sp.edit().putString("privacy_mode_" + entity.userName, entity.privacyMode).apply()
                }
            } catch (_: Exception) {}
            try {
                val conn = MySQLHelper.getInstance().getConnection()
                if (conn != null) {
                    try {
                        val sqlCreate = "CREATE TABLE IF NOT EXISTS user_settings (user_name VARCHAR(64) PRIMARY KEY, privacy_mode VARCHAR(16), two_factor TINYINT(1), notifications TINYINT(1), app_language VARCHAR(8))"
                        conn.createStatement().execute(sqlCreate)
                        val sqlUpsert = "REPLACE INTO user_settings (user_name, privacy_mode, two_factor, notifications, app_language) VALUES (?, ?, ?, ?, ?)"
                        val stmt = conn.prepareStatement(sqlUpsert)
                        stmt.setString(1, entity.userName)
                        stmt.setString(2, entity.privacyMode)
                        stmt.setInt(3, if (entity.twoFactorEnabled) 1 else 0)
                        stmt.setInt(4, if (entity.notificationsEnabled) 1 else 0)
                        stmt.setString(5, entity.appLanguage)
                        stmt.executeUpdate()
                    } finally {
                        MySQLHelper.getInstance().releaseConnection(conn)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun getPrivacyModeCached(context: Context, userName: String?): String {
        return try {
            val sp = context.getSharedPreferences("user_settings_cache", Context.MODE_PRIVATE)
            sp.getString("privacy_mode_" + (userName ?: ""), null) ?: "partial"
        } catch (_: Exception) { "partial" }
    }

    fun isAnonymous(context: Context, userName: String?): Boolean {
        return getPrivacyModeCached(context, userName) == "anonymous"
    }
}

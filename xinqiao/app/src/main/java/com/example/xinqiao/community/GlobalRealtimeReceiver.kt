package com.example.xinqiao.community

import android.app.Application
import com.example.xinqiao.notifications.NotificationUtils
import com.example.xinqiao.util.AnalysisUtils

object GlobalRealtimeReceiver {
    private var subscribed = false
    private var subscribedUser: String? = null

    fun onForeground(app: Application) {
        NotificationUtils.ensureChannel(app)
        val user = AnalysisUtils.readLoginUserName(app) ?: ""
        if (user.isBlank()) return
        if (subscribed && subscribedUser?.equals(user, ignoreCase = true) == true) return
        try { RealtimeChatClient.closeAll() } catch (_: Exception) {}
        val sp = app.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
        val raw = sp.getString("joinedGroups_" + user, "[]")
        val fromSp: List<String> = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as List<String> } catch (_: Exception) { emptyList() }
        val last = sp.getString("lastActiveGroup", null)
        val db: List<String> = try { com.example.xinqiao.mysql.DBUtils.getInstance(app).getSharedGroups(user) } catch (_: Exception) { emptyList() }
        val groups = ((fromSp + db + listOfNotNull(last))).distinct().filter { it.isNotBlank() }
        if (groups.isEmpty()) return
        RealtimeChatClient.subscribeGroups(app, groups, user) { payload ->
            try {
                val obj = org.json.JSONObject(payload)
                val g = obj.optString("group", "")
                val a = obj.optString("author", "")
                val c = obj.optString("content", "")
                if (a.isNotBlank() && !a.equals(user, ignoreCase = true)) {
                    try { NotificationUtils.showMessageNotification(app, g, a, c) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        subscribedUser = user
        subscribed = true
    }

    fun onBackground() {
        try { RealtimeChatClient.closeAll() } catch (_: Exception) {}
        subscribed = false
    }
}

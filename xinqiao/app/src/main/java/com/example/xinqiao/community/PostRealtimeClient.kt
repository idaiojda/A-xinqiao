package com.example.xinqiao.community

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

object PostRealtimeClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var onEvent: ((String) -> Unit)? = null
    private var subscribedUser: String? = null

    fun subscribeUser(ctx: Context, user: String, event: (String) -> Unit) {
        if (user.isBlank()) return
        if (subscribedUser?.equals(user, ignoreCase = true) == true && ws != null) {
            onEvent = event
            return
        }
        val sp = ctx.getSharedPreferences("network_config", Context.MODE_PRIVATE)
        val base = sp.getString("base_url_override", null) ?: com.example.xinqiao.network.NetworkConfig.getBaseUrl(ctx)
        val wsUrl = (base.replace("http://", "ws://").replace("https://", "wss://").trimEnd('/') + "/ws/post_events?user=" + user)
        val req = Request.Builder().url(wsUrl).build()
        try { ws?.close(1000, null) } catch (_: Exception) {}
        onEvent = event
        subscribedUser = user
        ws = client.newWebSocket(req, object: WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { try { onEvent?.invoke(text) } catch (_: Exception) {} }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { try { onEvent?.invoke(bytes.utf8()) } catch (_: Exception) {} }
        })
    }

    fun close() { try { ws?.close(1000, null) } catch (_: Exception) {}; ws = null; subscribedUser = null; onEvent = null }
}


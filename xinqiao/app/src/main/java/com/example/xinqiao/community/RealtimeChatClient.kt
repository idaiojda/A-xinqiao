package com.example.xinqiao.community

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

object RealtimeChatClient {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    private var onEvent: ((String) -> Unit)? = null

    fun connect(ctx: Context, group: String, user: String, event: (String) -> Unit) {
        onEvent = event
        val sp = ctx.getSharedPreferences("network_config", Context.MODE_PRIVATE)
        val base = sp.getString("base_url_override", null) ?: com.example.xinqiao.network.NetworkConfig.getBaseUrl(ctx)
        val wsUrl = (base.replace("http://", "ws://").replace("https://", "wss://").trimEnd('/') + "/ws/chat?group=" + group + "&user=" + user)
        val req = Request.Builder().url(wsUrl).build()
        ws?.close(1000, null)
        ws = client.newWebSocket(req, object: WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { onEvent?.invoke(text) }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { onEvent?.invoke(bytes.utf8()) }
        })
    }

    fun send(text: String) { ws?.send(text) }
    fun close() { ws?.close(1000, null); ws = null }
}


package com.example.xinqiao.community

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

object RealtimeChatClient {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    private var onEvent: ((String) -> Unit)? = null
    private var onStatus: ((Boolean) -> Unit)? = null
    private val groupSockets = java.util.concurrent.ConcurrentHashMap<String, WebSocket>()
    private val groupListeners = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArraySet<(String) -> Unit>>()

    fun connect(ctx: Context, group: String, user: String, event: (String) -> Unit, status: ((Boolean) -> Unit)? = null) {
        onEvent = event
        onStatus = status
        val base = com.example.xinqiao.network.NetworkConfig.getBaseUrl(ctx)
        val wsUrl = (base.replace("http://", "ws://").replace("https://", "wss://").trimEnd('/') + "/ws/chat?group=" + group + "&user=" + user)
        android.util.Log.d("RealtimeChatClient", "连接 WebSocket: $wsUrl")
        val req = Request.Builder().url(wsUrl).build()
        ws?.close(1000, null)
        ws = client.newWebSocket(req, object: WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                android.util.Log.d("RealtimeChatClient", "WebSocket 连接成功")
                onStatus?.invoke(true)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                android.util.Log.e("RealtimeChatClient", "WebSocket 连接失败: ${t.message}", t)
                onStatus?.invoke(false)
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("RealtimeChatClient", "WebSocket 正在关闭: code=$code, reason=$reason")
                onStatus?.invoke(false)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("RealtimeChatClient", "WebSocket 已关闭: code=$code, reason=$reason")
                onStatus?.invoke(false)
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.d("RealtimeChatClient", "收到文本消息: ${text.take(100)}")
                onEvent?.invoke(text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                android.util.Log.d("RealtimeChatClient", "收到二进制消息")
                onEvent?.invoke(bytes.utf8())
            }
        })
    }

    fun subscribeGroups(ctx: Context, groups: List<String>, user: String, event: (String) -> Unit) {
        val sp = ctx.getSharedPreferences("network_config", Context.MODE_PRIVATE)
        val base = sp.getString("base_url_override", null) ?: com.example.xinqiao.network.NetworkConfig.getBaseUrl(ctx)
        groups.distinct().filter { it.isNotBlank() }.forEach { g ->
            val url = (base.replace("http://", "ws://").replace("https://", "wss://").trimEnd('/') + "/ws/chat?group=" + g + "&user=" + user)
            val req = Request.Builder().url(url).build()
            groupListeners.computeIfAbsent(g) { java.util.concurrent.CopyOnWriteArraySet() }.add(event)
            groupSockets[g]?.close(1000, null)
            val socket = client.newWebSocket(req, object: WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try { groupListeners[g]?.forEach { l -> l(text) } } catch (_: Exception) {}
                }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val t = bytes.utf8()
                    try { groupListeners[g]?.forEach { l -> l(t) } } catch (_: Exception) {}
                }
            })
            groupSockets[g] = socket
        }
    }

    fun isAnySubscribed(): Boolean = groupSockets.isNotEmpty()

    fun send(text: String) { ws?.send(text) }
    fun close() { ws?.close(1000, null); ws = null }
    fun closeAll() { try { ws?.close(1000, null) } catch (_: Exception) {}; ws = null; try { groupSockets.values.forEach { try { it.close(1000, null) } catch (_: Exception) {} } } catch (_: Exception) {}; groupSockets.clear(); groupListeners.clear() }
}

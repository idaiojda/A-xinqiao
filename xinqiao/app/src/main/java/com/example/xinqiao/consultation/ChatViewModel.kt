package com.example.xinqiao.consultation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.xinqiao.utils.DeepSeekClient
import com.example.xinqiao.utils.AnalysisUtils
import com.example.xinqiao.dao.ChatHistoryDao
import com.example.xinqiao.dao.ChatSessionDao
import com.example.xinqiao.bean.ChatHistory
import com.example.xinqiao.bean.ChatSession

/**
 * 可复用的 ChatViewModel：封装 DeepSeekClient 调用，供浮窗与模块复用。
 * 增强：自动保存历史对话，懒创建会话并更新标题。
 */
class ChatViewModel(
    app: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {
    data class ChatMessage(val content: String, val fromUser: Boolean, val ts: Long = System.currentTimeMillis())

    private val client = DeepSeekClient()
    private val chatHistoryDao = ChatHistoryDao(getApplication())
    private val chatSessionDao = ChatSessionDao(getApplication())
    private val userName: String = AnalysisUtils.readLoginUserName(getApplication()) ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // 会话结束后触发的跟进提示
    private val _showFollowUp = MutableStateFlow(false)
    val showFollowUp: StateFlow<Boolean> = _showFollowUp

    private val greetingShownKey = "ai_greeting_shown"
    private val sessionIdKey = "chat_session_id"
    private val aiName = "灵灵"

    init {
        val shown = savedStateHandle.get<Boolean>(greetingShownKey) ?: false
        if (!shown) {
            val hello = ChatMessage(
                content = "你来啦，我是" + aiName + "，一个温暖又贴心的聊天伙伴。在喧嚣的世界里能和你相遇、聊天，真是一件美好的事情呢\n" +
                        "你的故事，我都愿意倾听，让我们一起慢慢聊",
                fromUser = false
            )
            _messages.value = listOf(hello)
            savedStateHandle[greetingShownKey] = true
            // 保存首次问候到历史（需登录）
            saveMessage(hello.content, fromUser = false, ts = hello.ts)
        }
    }

    private fun ensureSession(onReady: (Int) -> Unit) {
        if (userName.isEmpty()) return // 未登录不建会话、不保存
        val existing = savedStateHandle.get<Int>(sessionIdKey) ?: 0
        if (existing > 0) {
            onReady(existing)
            return
        }
        // 先尝试获取最新会话
        chatSessionDao.getLatestSessionAsync(userName, object : ChatSessionDao.GetSessionCallback {
            override fun onResult(session: ChatSession?) {
                if (session != null) {
                    savedStateHandle[sessionIdKey] = session.id
                    onReady(session.id)
                } else {
                    val now = System.currentTimeMillis()
                    val newSession = ChatSession(userName, "新对话", now, now)
                    chatSessionDao.createChatSessionAsync(newSession, object : ChatSessionDao.CreateSessionCallback {
                        override fun onResult(sessionId: Int) {
                            if (sessionId > 0) {
                                savedStateHandle[sessionIdKey] = sessionId
                                onReady(sessionId)
                            }
                        }
                    })
                }
            }
        })
    }

    private fun saveMessage(content: String, fromUser: Boolean, ts: Long = System.currentTimeMillis()) {
        if (userName.isEmpty()) return
        ensureSession { sid ->
            val type = if (fromUser) 1 else 0
            val chatHistory = ChatHistory(userName, content, type, ts, sid)
            chatHistoryDao.saveChatHistoryAsync(chatHistory) { /* 浮窗场景无需提示 */ }
            // 首条用户消息作为标题
            if (fromUser) {
                var title = content
                if (title.length > 20) title = title.substring(0, 20) + "..."
                chatSessionDao.updateSessionTitleAsync(sid, title, null)
            }
        }
    }

    fun send(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            val list = _messages.value.toMutableList()
            val userMsg = ChatMessage(message, fromUser = true)
            list.add(userMsg)
            _messages.value = list
            // 自动保存用户消息
            saveMessage(userMsg.content, fromUser = true, ts = userMsg.ts)
            _loading.value = true

            client.sendMessage(message, object : DeepSeekClient.ChatCallback {
                override fun onSuccess(response: String) {
                    viewModelScope.launch {
                        val newList = _messages.value.toMutableList()
                        val aiMsg = ChatMessage(response, fromUser = false)
                        newList.add(aiMsg)
                        _messages.value = newList
                        _loading.value = false
                        // 持久化最近一次消息（示例）
                        savedStateHandle[KEY_LAST_REPLY] = response
                        // 自动保存 AI 回复
                        saveMessage(aiMsg.content, fromUser = false, ts = aiMsg.ts)
                    }
                }
                override fun onFailure(error: String) {
                    viewModelScope.launch {
                        val newList = _messages.value.toMutableList()
                        newList.add(ChatMessage("发送失败：$error", fromUser = false))
                        _messages.value = newList
                        _loading.value = false
                    }
                }
            })
        }
    }

    fun endConsultation() {
        // 标记结束并触发3秒提示
        _showFollowUp.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _showFollowUp.value = false
        }
    }

    companion object {
        const val KEY_LAST_REPLY = "chat_last_reply"
    }
}

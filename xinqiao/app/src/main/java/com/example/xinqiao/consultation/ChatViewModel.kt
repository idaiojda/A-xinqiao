package com.example.xinqiao.consultation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.xinqiao.utils.DeepSeekClient

/**
 * 可复用的 ChatViewModel：封装 DeepSeekClient 调用，供浮窗与模块复用。
 */
class ChatViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    data class ChatMessage(val content: String, val fromUser: Boolean, val ts: Long = System.currentTimeMillis())

    private val client = DeepSeekClient()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // 会话结束后触发的跟进提示
    private val _showFollowUp = MutableStateFlow(false)
    val showFollowUp: StateFlow<Boolean> = _showFollowUp

    fun send(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            val list = _messages.value.toMutableList()
            list.add(ChatMessage(message, fromUser = true))
            _messages.value = list
            _loading.value = true

            client.sendMessage(message, object : DeepSeekClient.ChatCallback {
                override fun onSuccess(response: String) {
                    viewModelScope.launch {
                        val newList = _messages.value.toMutableList()
                        newList.add(ChatMessage(response, fromUser = false))
                        _messages.value = newList
                        _loading.value = false
                        // 持久化最近一次消息（示例）
                        savedStateHandle[KEY_LAST_REPLY] = response
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
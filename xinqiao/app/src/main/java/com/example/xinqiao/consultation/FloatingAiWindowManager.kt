package com.example.xinqiao.consultation

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.math.roundToInt

/**
 * 使用 WindowManager + Compose 构建 AI 悬浮按钮与对话浮窗
 */
class FloatingAiWindowManager(private val activity: Activity) {
    private val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var btnView: ComposeView? = null
    private var chatView: ComposeView? = null

    private val metrics = DisplayMetrics().also {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(it)
    }
    private val screenW = metrics.widthPixels
    private val screenH = metrics.heightPixels

    init {
        // 绑定 Activity 生命周期，确保销毁时移除悬浮窗，避免 WindowLeaked
        (activity as? LifecycleOwner)?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                detach()
            }
        })
    }

    private fun createBtnLayoutParams(stateVM: AiFloatingStateViewModel): WindowManager.LayoutParams {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        // 初始位置：右下角（预留底部安全区 96dp）
        val density = activity.resources.displayMetrics.density
        val bottomSafePx = (96 * density).roundToInt()
        val initialX = stateVM.floatX.value.takeIf { it >= 0 } ?: (screenW - (64 * density).roundToInt())
        val initialY = stateVM.floatY.value.takeIf { it >= 0 } ?: (screenH - (64 * density).roundToInt() - bottomSafePx)
        lp.x = initialX
        lp.y = initialY
        return lp
    }

    private fun createChatLayoutParams(stateVM: AiFloatingStateViewModel): WindowManager.LayoutParams {
        val targetW = ((screenW * 0.7f).roundToInt())
        val targetH = ((screenH * 0.5f).roundToInt())
        val lp = WindowManager.LayoutParams(
            targetW,
            targetH,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.CENTER
        stateVM.setWindowSize(targetW, targetH)
        return lp
    }

    fun attach(stateVM: AiFloatingStateViewModel, chatVM: ChatViewModel) {
        // 先按钮
        if (btnView == null) {
            btnView = ComposeView(activity).apply {
                setContent {
                    FloatingButton(stateVM) { onExpand ->
                        if (onExpand) {
                            showChat(stateVM, chatVM)
                        }
                    }
                }
            }
            // 通过设置 tag 绑定生命周期和 owner，避免直接依赖 ViewTree* 类
            (activity as? LifecycleOwner)?.let { btnView!!.setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, it) }
            (activity as? ViewModelStoreOwner)?.let { btnView!!.setTag(androidx.lifecycle.viewmodel.R.id.view_tree_view_model_store_owner, it) }
            (activity as? SavedStateRegistryOwner)?.let { btnView!!.setTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner, it) }
            wm.addView(btnView, createBtnLayoutParams(stateVM))
        }
        // 状态控制
        if (stateVM.isExpanded.value) {
            showChat(stateVM, chatVM)
        }
    }

    fun detach() {
        btnView?.let { wm.removeViewImmediate(it) }
        btnView = null
        chatView?.let { wm.removeViewImmediate(it) }
        chatView = null
    }

    private fun showChat(stateVM: AiFloatingStateViewModel, chatVM: ChatViewModel) {
        if (chatView != null) return
        chatView = ComposeView(activity).apply {
            setContent {
                MaterialTheme {
                    val state by stateVM.isExpanded.collectAsState()
                    val scale by stateVM.scale.collectAsState()
                    val tfState = rememberTransformableState { zoomChange, _, _ ->
                        val newScale = (scale * zoomChange).coerceIn(0.8f, 1.4f)
                        stateVM.setScale(newScale)
                        // 动态调整窗口大小
                        val baseW = (screenW * 0.7f).roundToInt()
                        val baseH = (screenH * 0.5f).roundToInt()
                        val lp = createChatLayoutParams(stateVM)
                        lp.width = (baseW * newScale).roundToInt()
                        lp.height = (baseH * newScale).roundToInt()
                        wm.updateViewLayout(this@apply, lp)
                    }
                    Surface(modifier = Modifier
                        .fillMaxSize()
                        .transformable(tfState)) {
                        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                            // 顶部栏
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("灵灵", style = MaterialTheme.typography.titleMedium)
                                Row {
                                    IconButton(onClick = {
                                        stateVM.setExpanded(false)
                                        stateVM.setMinimized(true)
                                        removeChat()
                                    }) { Icon(Icons.Default.Minimize, contentDescription = "minimize") }
                                    IconButton(onClick = {
                                        stateVM.setExpanded(false)
                                        stateVM.setMinimized(true)
                                        removeChat()
                                        // 会话结束提示
                                        chatVM.endConsultation()
                                    }) { Icon(Icons.Default.Close, contentDescription = "close") }
                                }
                            }
                            // 内容
                            ChatContent(chatVM)
                        }
                    }

                    // 跟进提示 3 秒
                    val show by chatVM.showFollowUp.collectAsState()
                    if (show) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            Surface(color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                                Text(
                                    text = "是否需要进一步帮助？",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
        // 通过设置 tag 绑定生命周期和 owner，避免直接依赖 ViewTree* 类
        (activity as? LifecycleOwner)?.let { chatView!!.setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, it) }
            (activity as? ViewModelStoreOwner)?.let { chatView!!.setTag(androidx.lifecycle.viewmodel.R.id.view_tree_view_model_store_owner, it) }
            (activity as? SavedStateRegistryOwner)?.let { chatView!!.setTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner, it) }
        wm.addView(chatView, createChatLayoutParams(stateVM))
        stateVM.setExpanded(true)
        stateVM.setMinimized(false)
    }

    private fun removeChat() {
        chatView?.let { wm.removeViewImmediate(it) }
        chatView = null
    }

    @Composable
    private fun FloatingButton(stateVM: AiFloatingStateViewModel, onExpand: (Boolean) -> Unit) {
        val density = LocalDensity.current
        var offsetX by remember { mutableStateOf(0) }
        var offsetY by remember { mutableStateOf(0) }
        val dragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(onDragEnd = {
                // 边缘吸附，并限制底部安全区
                val bottomSafePx = with(density) { 96.dp.toPx() }.roundToInt()
                val estimatedW = with(density) { 64.dp.toPx() }.roundToInt()
                val finalX = if (offsetX + estimatedW / 2 < screenW / 2) 0 else (screenW - estimatedW)
                val maxY = (screenH - bottomSafePx - estimatedW).coerceAtLeast(0)
                val finalY = offsetY.coerceIn(0, maxY)
                updateBtnLayout(finalX, finalY, stateVM)
            }) { _, dragAmount ->
                offsetX = (offsetX + dragAmount.x).roundToInt().coerceIn(0, screenW)
                offsetY = (offsetY + dragAmount.y).roundToInt().coerceIn(0, screenH)
                updateBtnLayout(offsetX, offsetY, stateVM)
            }
        }
        Surface(
            modifier = dragModifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                IconButton(onClick = { onExpand(true) }) {
                    Icon(Icons.Default.SmartToy, contentDescription = "AI助手", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    private fun updateBtnLayout(x: Int, y: Int, stateVM: AiFloatingStateViewModel) {
        btnView?.let { view ->
            val lp = createBtnLayoutParams(stateVM)
            lp.x = x
            lp.y = y
            wm.updateViewLayout(view, lp)
            stateVM.setFloatPos(x, y)
        }
    }

    @Composable
    private fun ChatContent(chatVM: ChatViewModel) {
        val messages by chatVM.messages.collectAsState()
        val loading by chatVM.loading.collectAsState()
        var input by remember { mutableStateOf("") }
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                            Text(msg.content, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
            if (loading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("请输入咨询内容...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val msg = input.trim()
                    if (msg.isNotEmpty()) {
                        chatVM.send(msg)
                        input = ""
                    }
                }) { Text("发送") }
            }
        }
    }
}
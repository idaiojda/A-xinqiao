package com.example.xinqiao.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xinqiao.R
import com.example.xinqiao.adapter.ChatMessageAdapter
import com.example.xinqiao.api.GroupChatApiService
import com.example.xinqiao.network.RetrofitClient
import com.example.xinqiao.repository.GroupChatRepository
import com.example.xinqiao.viewmodel.GroupChatViewModel
import com.example.xinqiao.viewmodel.GroupChatViewModelFactory
import com.example.xinqiao.websocket.GroupChatWebSocketManager
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * 社区小组聊天页面
 */
class GroupChatActivity : AppCompatActivity() {
    
    private lateinit var viewModel: GroupChatViewModel
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnImage: ImageButton
    
    private var groupId: Long = 0
    private var userId: Long = 0
    private var groupName: String = ""
    
    private var webSocketManager: GroupChatWebSocketManager? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)
        
        // 获取参数 - 支持两种格式
        // 格式1: GROUP_ID + GROUP_NAME (从 GroupListActivity)
        // 格式2: group (小组名称，从 CommunityScreenNew)
        val receivedGroupId = intent.getLongExtra("GROUP_ID", 0)
        val receivedGroupName = intent.getStringExtra("GROUP_NAME") ?: intent.getStringExtra("group")
        
        if (receivedGroupName.isNullOrBlank()) {
            Toast.makeText(this, "小组信息无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        groupName = receivedGroupName
        userId = getCurrentUserId()
        
        // 如果没有 GROUP_ID，需要通过名称查询
        if (receivedGroupId == 0L) {
            // 显示加载提示
            progressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.VISIBLE
            
            // 在后台线程查询小组ID
            Thread {
                groupId = getGroupIdByName(groupName)
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    
                    if (groupId == 0L) {
                        Toast.makeText(this, "未找到小组: $groupName", Toast.LENGTH_LONG).show()
                        finish()
                        return@runOnUiThread
                    }
                    
                    // 初始化界面
                    initializeUI()
                }
            }.start()
        } else {
            groupId = receivedGroupId
            initializeUI()
        }
    }
    
    private fun initializeUI() {
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupInputArea()
        observeViewModel()
        setupWebSocket()
        
        // 加载消息列表
        viewModel.loadMessages(groupId)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = groupName
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupViewModel() {
        val apiService = RetrofitClient.groupChatApi
        val repository = GroupChatRepository(apiService)
        val factory = GroupChatViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GroupChatViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        
        adapter = ChatMessageAdapter(
            currentUserId = userId,
            onAvatarClick = { message ->
                // 点击头像，查看用户信息
                Toast.makeText(this, message.getSenderDisplayName(), Toast.LENGTH_SHORT).show()
            },
            onMessageLongClick = { message ->
                // 长按消息，显示操作菜单（撤回等）
                if (message.userId == userId) {
                    showMessageMenu(message.id)
                }
                true
            }
        )
        
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // 从底部开始显示
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        
        // 滚动监听，加载更多历史消息
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                
                if (firstVisiblePosition == 0 && !viewModel.loading.value!!) {
                    // 滚动到顶部，加载更多历史消息
                    val messages = viewModel.messages.value
                    if (!messages.isNullOrEmpty()) {
                        val oldestMessageId = messages.first().id
                        viewModel.loadMessages(groupId, limit = 20, beforeMessageId = oldestMessageId)
                    }
                }
            }
        })
    }
    
    private fun setupInputArea() {
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnImage = findViewById(R.id.btnImage)
        
        btnSend.setOnClickListener {
            sendTextMessage()
        }
        
        btnImage.setOnClickListener {
            // 选择图片
            // TODO: 实现图片选择功能
            Toast.makeText(this, "图片功能开发中", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        // 观察消息列表
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages) {
                // 滚动到最新消息
                if (messages.isNotEmpty()) {
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
        
        // 观察加载状态
        viewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察错误信息
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        // 观察成功信息
        viewModel.success.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }
    
    private fun setupWebSocket() {
        webSocketManager = GroupChatWebSocketManager(
            groupId = groupId,
            userId = userId,
            onMessageReceived = { message ->
                // 收到实时消息，添加到列表
                runOnUiThread {
                    viewModel.addRealtimeMessage(message)
                }
            },
            onConnected = {
                runOnUiThread {
                    Toast.makeText(this, "已连接到聊天室", Toast.LENGTH_SHORT).show()
                }
            },
            onDisconnected = {
                runOnUiThread {
                    Toast.makeText(this, "已断开连接", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "连接错误: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        webSocketManager?.connect()
    }
    
    private fun sendTextMessage() {
        val content = etMessage.text.toString().trim()
        if (content.isBlank()) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 通过WebSocket发送（如果已连接）
        if (webSocketManager?.isConnected() == true) {
            webSocketManager?.sendMessage(content)
            etMessage.text.clear()
        } else {
            // 通过API发送
            viewModel.sendTextMessage(groupId, content)
            etMessage.text.clear()
        }
    }
    
    private fun showMessageMenu(messageId: Long) {
        // 显示消息操作菜单
        val items = arrayOf("撤回消息")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> viewModel.recallMessage(groupId, messageId)
                }
            }
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_leave_group -> {
                showLeaveGroupDialog()
                true
            }
            R.id.action_delete_group -> {
                showDeleteGroupDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_chat, menu)
        
        // 检查用户是否是小组成员和管理员，动态显示菜单项
        checkMembershipAndUpdateMenu(menu)
        
        return true
    }
    
    private fun checkMembershipAndUpdateMenu(menu: android.view.Menu) {
        // 通过API检查用户是否是成员和管理员
        Thread {
            try {
                val apiService = RetrofitClient.groupChatApi
                val response = kotlinx.coroutines.runBlocking {
                    apiService.getMembers(groupId)
                }
                
                val currentMember = if (response.isSuccessful && response.body() != null) {
                    response.body()!!.find { it.userId == userId && it.status == 1 }
                } else {
                    null
                }
                
                val isMember = currentMember != null
                val isAdmin = currentMember?.role == "admin"
                
                runOnUiThread {
                    val leaveItem = menu.findItem(R.id.action_leave_group)
                    val deleteItem = menu.findItem(R.id.action_delete_group)
                    
                    // 只有成员才显示退出按钮
                    leaveItem?.isVisible = isMember
                    
                    // 只有管理员才显示删除按钮
                    deleteItem?.isVisible = isAdmin
                    
                    android.util.Log.d("GroupChatActivity", "菜单更新: isMember=$isMember, isAdmin=$isAdmin")
                }
            } catch (e: Exception) {
                android.util.Log.e("GroupChatActivity", "检查成员关系失败: ${e.message}", e)
            }
        }.start()
    }
    
    private fun showLeaveGroupDialog() {
        // 检查用户是否是管理员
        Thread {
            try {
                val apiService = RetrofitClient.groupChatApi
                val response = kotlinx.coroutines.runBlocking {
                    apiService.getMembers(groupId)
                }
                
                val currentMember = if (response.isSuccessful && response.body() != null) {
                    response.body()!!.find { it.userId == userId && it.status == 1 }
                } else {
                    null
                }
                
                val isAdmin = currentMember?.role == "admin"
                val adminCount = if (response.isSuccessful && response.body() != null) {
                    response.body()!!.count { it.role == "admin" && it.status == 1 }
                } else {
                    0
                }
                
                runOnUiThread {
                    if (isAdmin && adminCount == 1) {
                        // 唯一管理员，显示特殊提示
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(R.string.group_leave)
                            .setMessage("您是该小组的创建者和管理员，无法直接退出。\n\n如需退出，请先删除该小组。")
                            .setPositiveButton("知道了", null)
                            .show()
                    } else {
                        // 普通成员或有多个管理员，正常退出
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(R.string.group_leave)
                            .setMessage(R.string.group_leave_confirm)
                            .setPositiveButton(R.string.confirm) { _, _ ->
                                leaveGroup()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GroupChatActivity", "检查管理员状态失败: ${e.message}", e)
                runOnUiThread {
                    // 出错时显示普通退出对话框
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.group_leave)
                        .setMessage(R.string.group_leave_confirm)
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            leaveGroup()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }
        }.start()
    }
    
    private fun leaveGroup() {
        // 先移除之前的观察者，避免重复观察
        viewModel.success.removeObservers(this)
        viewModel.error.removeObservers(this)
        
        // 观察错误信息
        viewModel.error.observe(this) { error ->
            error?.let {
                android.util.Log.e("GroupChatActivity", "退出小组失败: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // 观察成功信息
        viewModel.success.observe(this) { message ->
            message?.let {
                if (it.contains("退出成功") || it.contains("成功")) {
                    Toast.makeText(this, R.string.group_leave_success, Toast.LENGTH_SHORT).show()
                    
                    // 通知社区页面刷新（通过广播）
                    val intent = android.content.Intent("com.example.xinqiao.GROUP_MEMBERSHIP_CHANGED")
                    sendBroadcast(intent)
                    
                    // 关闭当前页面，返回上一页
                    finish()
                }
            }
        }
        
        // 执行退出操作
        viewModel.leaveGroup(groupId)
    }
    
    private fun showDeleteGroupDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.group_delete)
            .setMessage(R.string.group_delete_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                deleteGroup()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun deleteGroup() {
        // 先移除之前的观察者，避免重复观察
        viewModel.success.removeObservers(this)
        viewModel.error.removeObservers(this)
        
        // 观察错误信息
        viewModel.error.observe(this) { error ->
            error?.let {
                android.util.Log.e("GroupChatActivity", "删除小组失败: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // 观察成功信息
        viewModel.success.observe(this) { message ->
            message?.let {
                if (it.contains("删除成功") || it.contains("成功")) {
                    Toast.makeText(this, R.string.group_delete_success, Toast.LENGTH_SHORT).show()
                    
                    // 通知社区页面刷新
                    val intent = android.content.Intent("com.example.xinqiao.GROUP_MEMBERSHIP_CHANGED")
                    sendBroadcast(intent)
                    
                    // 关闭当前页面，返回上一页
                    finish()
                }
            }
        }
        
        // 执行删除操作
        viewModel.deleteGroup(groupId)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 断开WebSocket连接
        webSocketManager?.disconnect()
    }
    
    private fun getGroupIdByName(name: String): Long {
        // 通过小组名称查询小组ID（在后台线程调用）
        return try {
            android.util.Log.d("GroupChatActivity", "Searching for group: $name")
            
            // 使用协程同步调用
            kotlinx.coroutines.runBlocking {
                val apiService = RetrofitClient.groupChatApi
                val response = apiService.getAllGroups()
                
                if (response.isSuccessful) {
                    val groups = response.body()
                    android.util.Log.d("GroupChatActivity", "Found ${groups?.size ?: 0} groups")
                    
                    val group = groups?.find { it.name == name }
                    if (group != null) {
                        android.util.Log.d("GroupChatActivity", "Found group: ${group.name} with ID: ${group.id}")
                        group.id
                    } else {
                        android.util.Log.e("GroupChatActivity", "Group not found: $name")
                        0L
                    }
                } else {
                    android.util.Log.e("GroupChatActivity", "API error: ${response.code()}")
                    0L
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupChatActivity", "Failed to get group ID: ${e.message}", e)
            0L
        }
    }
    
    private fun getCurrentUserId(): Long {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getLong("user_id", 3L)
    }
}

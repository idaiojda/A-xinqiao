package com.example.xinqiao.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsTimelineNew(controller: CommunityController, onGroupCreated: (String) -> Unit = {}) {
    val tokens = CommunityTokensInstance
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var groupCapacity by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    var dbGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var myGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // 加载用户小组的函数
    suspend fun loadUserGroups() {
        try {
            val sp = appCtx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
            val userId = sp.getLong("user_id", 3L)
            
            val fromApi = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val apiService = com.example.xinqiao.network.RetrofitClient.groupChatApi
                    val response = apiService.getUserGroups(userId)
                    if (response.isSuccessful && response.body() != null) {
                        android.util.Log.d("MyGroupsTimelineNew", "从API加载到 ${response.body()!!.size} 个小组")
                        response.body()!!.filter { it.isJoined }.map { it.name }
                    } else {
                        android.util.Log.e("MyGroupsTimelineNew", "API返回错误: ${response.code()}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MyGroupsTimelineNew", "加载用户小组失败: ${e.message}", e)
                emptyList()
            }
            
            dbGroups = fromApi
            android.util.Log.d("MyGroupsTimelineNew", "最终显示 ${dbGroups.size} 个小组")
        } catch (e: Exception) { 
            android.util.Log.e("MyGroupsTimelineNew", "初始化失败: ${e.message}", e)
            dbGroups = emptyList() 
        }
    }
    
    LaunchedEffect(Unit) {
        loadUserGroups()
    }
    
    LaunchedEffect(controller.groupsVersion) {
        android.util.Log.d("MyGroupsTimelineNew", "检测到groupsVersion变化，刷新小组列表")
        loadUserGroups()
    }
    
    // 监听页面可见性，返回时刷新
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                android.util.Log.d("MyGroupsTimelineNew", "页面恢复，刷新小组列表")
                scope.launch { loadUserGroups() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Card(
        shape = RoundedCornerShape(tokens.corner.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.elevate.Card),
        colors = CardDefaults.cardColors(containerColor = tokens.color.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.L),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.L)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "我的小组",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = tokens.type.CardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.color.Neutral900
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S), verticalAlignment = Alignment.CenterVertically) {
                    if (dbGroups.isNotEmpty()) {
                        TextButton(onClick = {
                            val intent = android.content.Intent(appCtx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                            intent.putExtra("group", dbGroups.first())
                            appCtx.startActivity(intent)
                        }) { Text("进入会话") }
                    }
                    Button(
                        onClick = { showCreateSheet = true },
                        shape = RoundedCornerShape(tokens.corner.Button)
                    ) {
                        Text("创建小组")
                    }
                }
            }
            Divider(color = tokens.color.Neutral200)
            val ctx = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(dbGroups, controller.groupsVersion) {
                try {
                    val user = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx)
                    val remote: List<String> = emptyList()
                    myGroups = (dbGroups + remote).distinct()
                } catch (_: Exception) { myGroups = dbGroups }
            }
            if (error != null) {
                Text(error!!, color = tokens.color.Danger, style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    if (myGroups.isNotEmpty()) {
                        Text("我创建或加入的小组", style = MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            myGroups.forEach { g ->
                                Surface(onClick = {
                                    val intent = android.content.Intent(ctx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                                    intent.putExtra("group", g)
                                    ctx.startActivity(intent)
                                }, shape = RoundedCornerShape(tokens.corner.Button)) {
                                    Row(modifier = Modifier.padding(tokens.spacing.M), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(g)
                                        Icon(Icons.Default.Chat, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                Text("创建小组", style = MaterialTheme.typography.titleMedium)
                TextField(value = groupName, onValueChange = { groupName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("名称") })
                TextField(value = groupDesc, onValueChange = { groupDesc = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("简介") })
                TextField(value = groupCapacity, onValueChange = { groupCapacity = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("容量，人数上限") })
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                    Button(onClick = {
                        if (groupName.isBlank()) return@Button
                        val cap = groupCapacity.toIntOrNull() ?: 0
                        creating = true
                        scope.launch {
                            try {
                                val userName = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx)
                                val res = CommunityRepositoryProvider.current.createGroup(groupName.trim(), groupDesc.trim(), "", cap, userName)
                                if (res.ok) {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            CommunityLocalCache.database()?.groupDao()?.insertOrUpdate(groupName.trim(), userName, true)
                                        } catch (_: Exception) { }
                                    }
                                    controller.setJoined(groupName.trim(), true)
                                    dbGroups = (dbGroups + groupName.trim()).distinct()
                                    myGroups = (myGroups + groupName.trim()).distinct()
                                    val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                    val key = "joinedGroups_" + userName
                                    val oldJson = sp.getString(key, "[]")
                                    val oldList = try { com.google.gson.Gson().fromJson(oldJson, java.util.ArrayList::class.java) as List<String> } catch (_: Exception) { emptyList() }
                                    val newJson = com.google.gson.Gson().toJson((oldList + groupName.trim()).distinct())
                                    sp.edit().putString(key, newJson).apply()
                                    onGroupCreated(groupName.trim())
                                    showCreateSheet = false
                                    groupName = ""; groupDesc = ""; groupCapacity = ""
                                } else {
                                    error = res.message
                                }
                            } catch (e: Exception) {
                                error = "创建失败：" + (e.message ?: "网络异常")
                            } finally {
                                creating = false
                            }
                        }
                    }, enabled = !creating) { Text(if (creating) "发布中" else "发布") }
                    TextButton(onClick = { showCreateSheet = false }) { Text("取消") }
                }
            }
        }
    }
}

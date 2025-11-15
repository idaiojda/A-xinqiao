package com.example.xinqiao.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Slider
import com.example.xinqiao.util.AnalysisUtils
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.xinqiao.community.*

private fun imageDataFrom(data: String?): Any? {
    if (data == null) return null
    return if (data.startsWith("data:image")) {
        val base64 = data.substringAfter(",")
        try { android.util.Base64.decode(base64, android.util.Base64.DEFAULT) } catch (_: Exception) { null }
    } else data
}

fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000L -> "刚刚"
        diff < 60 * 60 * 1000L -> "${diff / (60 * 1000L)}分钟前"
        diff < 24 * 60 * 60 * 1000L -> "${diff / (60 * 60 * 1000L)}小时前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun formatDateSection(timestamp: Long): String {
    val now = java.util.Calendar.getInstance()
    val ts = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    fun startOfDay(c: java.util.Calendar): Long {
        val d = c.clone() as java.util.Calendar
        d.set(java.util.Calendar.HOUR_OF_DAY, 0)
        d.set(java.util.Calendar.MINUTE, 0)
        d.set(java.util.Calendar.SECOND, 0)
        d.set(java.util.Calendar.MILLISECOND, 0)
        return d.timeInMillis
    }
    val today = startOfDay(now)
    val tday = startOfDay(ts)
    val label = when {
        tday == today -> "今天"
        tday == today - 24L * 60 * 60 * 1000 -> "昨天"
        tday == today - 2L * 24 * 60 * 60 * 1000 -> "前天"
        else -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
    val time = android.text.format.DateFormat.format("HH:mm", java.util.Date(timestamp)).toString()
    return "$label $time"
}

class GroupChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupName = intent.getStringExtra("group")?.trim() ?: ""
        setContent {
            MaterialTheme {
                GroupChatScreen(groupName = groupName)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupChatScreen(groupName: String) {
    val ctx = LocalContext.current
    val conf = LocalConfiguration.current
    val tokens = CommunityTokensInstance
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nicknames = remember { mutableStateMapOf<String, String>() }
    val avatars = remember { mutableStateMapOf<String, String?>() }
    var playingId by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var voiceProgress by remember { mutableStateOf(0f) }
    val currentUserAll = AnalysisUtils.readLoginUserName(ctx)
    
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                shape = MaterialTheme.shapes.medium.copy(
                    bottomStart = MaterialTheme.shapes.extraLarge.bottomStart,
                    bottomEnd = MaterialTheme.shapes.extraLarge.bottomEnd
                )
            ) {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                groupName, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Text(
                                "在线",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = { (ctx as? ComponentActivity)?.finish() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                androidx.compose.material3.DropdownMenuItem(text = { Text("退出群组") }, onClick = {
                                    menuOpen = false
                                    val user = AnalysisUtils.readLoginUserName(ctx)
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            com.example.xinqiao.mysql.DBUtils.getInstance(ctx).setCommunityGroupJoin(groupName.trim(), user, false)
                                        } catch (_: Exception) { }
                                        try {
                                            val dao = com.example.xinqiao.community.CommunityLocalCache.database()?.groupDao()
                                            val info = dao?.get(groupName.trim())
                                            if (info != null) {
                                                dao.upsert(info.copy(joined = false, memberCount = (info.memberCount - 1).coerceAtLeast(0)))
                                            }
                                        } catch (_: Exception) { }
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            (ctx as? ComponentActivity)?.finish()
                                        }
                                    }
                                })
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            ) 
        }
        ) { padding ->
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            var chatMessages by remember { mutableStateOf<List<GroupMessage>>(emptyList()) }
            var chatLoading by remember { mutableStateOf(false) }
            var chatInput by remember { mutableStateOf("") }
            var chatImages by remember { mutableStateOf<List<String>>(emptyList()) }
            var chatVoicePath by remember { mutableStateOf<String?>(null) }
            var chatVoiceDuration by remember { mutableStateOf<Int?>(null) }
            var recording by remember { mutableStateOf(false) }
            var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
            val pickChatImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> chatImages = uris.map { it.toString() } }
            val requestAudioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    try {
                        val file = java.io.File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.3gp")
                        val r = android.media.MediaRecorder()
                        r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                        r.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
                        r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                        r.setOutputFile(file.absolutePath)
                        r.prepare()
                        r.start()
                        recorder = r
                        recording = true
                        chatVoicePath = file.absolutePath
                        chatVoiceDuration = null
                    } catch (_: Exception) {
                        recording = false
                        recorder?.release()
                        recorder = null
                        scope.launch { snackbarHostState.showSnackbar("录音启动失败") }
                    }
                } else {
                    scope.launch { snackbarHostState.showSnackbar("未授予录音权限") }
                }
            }

            fun reloadMessages() {
                if (groupName.isBlank()) return
                chatLoading = true
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val local = try { com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.getByGroup(groupName.trim())?.map { it.toDomain() } ?: emptyList() } catch (_: Exception) { emptyList() }
                    var mergedLocal = local
                    if (mergedLocal.isEmpty()) {
                        try {
                            val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                            val raw = sp.getString("recentMessages_" + groupName.trim(), "[]")
                            var fromSp: List<GroupMessage> = emptyList()
                            try {
                                val type = com.google.gson.reflect.TypeToken.getParameterized(java.util.ArrayList::class.java, com.example.xinqiao.community.GroupMessage::class.java).type
                                fromSp = com.google.gson.Gson().fromJson(raw, type) as List<GroupMessage>
                            } catch (_: Exception) {
                                try {
                                    val arr = org.json.JSONArray(raw)
                                    val list = mutableListOf<GroupMessage>()
                                    for (i in 0 until arr.length()) {
                                        val o = arr.optJSONObject(i) ?: continue
                                        list.add(
                                            GroupMessage(
                                                id = o.optString("id", "gm" + System.currentTimeMillis()),
                                                groupName = o.optString("groupName", groupName.trim()),
                                                author = o.optString("author", ""),
                                                authorAvatar = null,
                                                content = o.optString("content", ""),
                                                images = emptyList(),
                                                mentions = emptyList(),
                                                voiceUrl = null,
                                                voiceDurationSec = null,
                                                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                                                recalled = o.optBoolean("recalled", false)
                                            )
                                        )
                                    }
                                    fromSp = list
                                } catch (_: Exception) { fromSp = emptyList() }
                            }
                            mergedLocal = fromSp
                        } catch (_: Exception) { }
                    }
                    android.util.Log.d("GroupChatDiag", "reload local group=" + groupName.trim() + " size=" + mergedLocal.size)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { chatMessages = mergedLocal }
                    try {
                        val rows = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listCommunityGroupMessages(groupName.trim())
                        val cloud = rows.map {
                            GroupMessage(
                                id = it.id,
                                groupName = it.groupName,
                                author = it.author,
                                authorAvatar = it.authorAvatar,
                                content = it.content ?: "",
                                images = try { com.google.gson.Gson().fromJson(it.imagesJson, java.util.ArrayList::class.java) as List<String> } catch (_: Exception) { emptyList() },
                                mentions = try { com.google.gson.Gson().fromJson(it.mentionsJson, java.util.ArrayList::class.java) as List<String> } catch (_: Exception) { emptyList() },
                                voiceUrl = it.voiceUrl,
                                voiceDurationSec = it.voiceDurationSec,
                                timestamp = it.timestamp,
                                recalled = it.recalled
                            )
                        }
                        val merged = (mergedLocal + cloud).distinctBy { it.id }
                        android.util.Log.d("GroupChatDiag", "reload cloud group=" + groupName.trim() + " size=" + cloud.size + " merged=" + merged.size)
                        // 回填缺失头像并缓存到本地
                        try {
                            val dao = com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()
                            val authors = merged.map { it.author }.distinct().filter { it.isNotBlank() }
                            authors.forEach { a ->
                                if (!avatars.containsKey(a)) {
                                    try {
                                        val av = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPathByNameOrNickSync(a)
                                        avatars[a] = av
                                        val updates = merged.filter { it.author == a }.map { it.copy(authorAvatar = av).toEntity() }
                                        try { dao?.upsertAll(updates) } catch (_: Exception) { }
                                    } catch (_: Exception) { }
                                }
                            }
                        } catch (_: Exception) { }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { chatMessages = merged; chatLoading = false }
                    } catch (_: Exception) {
                        val fallback = try { CommunityRepositoryProvider.current.getGroupMessages(groupName) } catch (_: Exception) { mergedLocal }
                        android.util.Log.d("GroupChatDiag", "reload fallback group=" + groupName.trim() + " size=" + fallback.size)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { chatMessages = fallback; chatLoading = false }
                    }
                }
            }
            LaunchedEffect(groupName) {
                reloadMessages()
                try {
                    com.example.xinqiao.community.RealtimeChatClient.connect(ctx, groupName, currentUserAll) { payload ->
                        try {
                            val obj = org.json.JSONObject(payload)
                            val gm = GroupMessage(
                                id = if (obj.has("id")) obj.getString("id") else ("gm" + System.currentTimeMillis()),
                                groupName = if (obj.has("group")) obj.getString("group") else groupName,
                                author = if (obj.has("author")) obj.getString("author") else "",
                                authorAvatar = null,
                                content = if (obj.has("content")) obj.getString("content") else "",
                                images = emptyList(),
                                mentions = emptyList(),
                                voiceUrl = null,
                                voiceDurationSec = null,
                                timestamp = if (obj.has("ts")) obj.getLong("ts") else System.currentTimeMillis(),
                                recalled = false
                            )
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                // 实时消息回填作者头像
                                try {
                                    val av = try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPathByNameOrNickSync(gm.author) } catch (_: Exception) { null }
                                    if (av != null) { avatars[gm.author] = av }
                                    val withAvatar = if (av != null) gm.copy(authorAvatar = av) else gm
                                    com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(withAvatar.toEntity()))
                                } catch (_: Exception) { try { com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(gm.toEntity())) } catch (_: Exception) {} }
                                try {
                                    val rec = com.example.xinqiao.mysql.DBUtils.GroupMessageRecord()
                                    rec.id = gm.id; rec.groupName = gm.groupName; rec.author = gm.author; rec.authorAvatar = gm.authorAvatar; rec.content = gm.content; rec.imagesJson = com.google.gson.Gson().toJson(gm.images); rec.mentionsJson = com.google.gson.Gson().toJson(gm.mentions); rec.voiceUrl = gm.voiceUrl; rec.voiceDurationSec = gm.voiceDurationSec; rec.timestamp = gm.timestamp; rec.recalled = false
                                    com.example.xinqiao.mysql.DBUtils.getInstance(ctx).insertCommunityGroupMessage(rec)
                                } catch (_: Exception) {}
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { chatMessages = chatMessages + gm }
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
            LaunchedEffect(currentUserAll) {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val av = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPathSync(currentUserAll)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { avatars[currentUserAll] = av }
                    } catch (_: Exception) { }
                }
            }
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
                    if (event == Lifecycle.Event.ON_RESUME) reloadMessages()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            DisposableEffect(groupName) {
                onDispose { try { com.example.xinqiao.community.RealtimeChatClient.close() } catch (_: Exception) {} }
            }

            LaunchedEffect(chatMessages) {
                try {
                    val authors = chatMessages.map { it.author }.distinct().filter { it.isNotBlank() }
                    authors.forEach { a ->
                        if (!nicknames.containsKey(a)) {
                            com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNickname(a, object: com.example.xinqiao.mysql.DBUtils.UserNicknameCallback {
                                override fun onSuccess(nickname: String?) { if (!nickname.isNullOrBlank()) nicknames[a] = nickname!! }
                                override fun onError(e: java.sql.SQLException) {}
                            })
                        }
                        if (!avatars.containsKey(a)) {
                            com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPath(a, object: com.example.xinqiao.mysql.DBUtils.AvatarPathCallback {
                                override fun onSuccess(avatarBase64: String?) { avatars[a] = avatarBase64 }
                                override fun onError(e: java.sql.SQLException) {}
                            })
                        }
                    }
                } catch (_: Exception) {}
            }

            Box(modifier = Modifier.weight(1f)) {
                if (chatLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                } else {
                    if (chatMessages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "暂无消息",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    "开始聊天吧！",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
                        ) {
                            itemsIndexed(chatMessages, key = { _, m -> m.id }) { index, m ->
                                val isMyMessage = m.author == currentUserAll
                                val showDateSeparator = index == 0 || 
                                    (m.timestamp - chatMessages[index - 1].timestamp) > 5 * 60 * 1000L
                                val prev = if (index > 0) chatMessages[index - 1] else null
                                val sameAuthorPrev = prev?.author == m.author
                                val shortIntervalPrev = prev != null && (m.timestamp - prev.timestamp) < 3 * 60 * 1000L
                                val showAvatarThis = !(sameAuthorPrev && shortIntervalPrev)
                                val topPad = if (sameAuthorPrev && shortIntervalPrev) 2.dp else 12.dp
                                LaunchedEffect(m.author) {
                                    try {
                                        if (!nicknames.containsKey(m.author)) {
                                            com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNickname(m.author, object: com.example.xinqiao.mysql.DBUtils.UserNicknameCallback {
                                                override fun onSuccess(nickname: String?) { if (nickname != null) nicknames[m.author] = nickname }
                                                override fun onError(e: java.sql.SQLException) {}
                                            })
                                        }
                                        if (!avatars.containsKey(m.author)) {
                                            com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPath(m.author, object: com.example.xinqiao.mysql.DBUtils.AvatarPathCallback {
                                                override fun onSuccess(avatarBase64: String?) { avatars[m.author] = avatarBase64 }
                                                override fun onError(e: java.sql.SQLException) {}
                                            })
                                        }
                                    } catch (_: Exception) {}
                                }
                                
                                if (showDateSeparator) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                        Surface(
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                            modifier = Modifier.align(Alignment.Center)
                                        ) {
                                            Text(
                                                text = formatDateSection(m.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = topPad),
                                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!isMyMessage) {
                                        val avatarLeft = m.authorAvatar ?: avatars[m.author]
                                        val painterLeft = if (avatarLeft != null) {
                                            rememberAsyncImagePainter(
                                                model = coil.request.ImageRequest.Builder(ctx)
                                                    .data(imageDataFrom(avatarLeft))
                                                    .allowHardware(false)
                                                    .size(128, 128)
                                                    .build()
                                            )
                                        } else painterResource(id = com.example.xinqiao.R.drawable.default_avatar)
                                        if (showAvatarThis) {
                                            Surface(shape = CircleShape, modifier = Modifier.size(36.dp).padding(end = 8.dp)) {
                                                Image(
                                                    painter = painterLeft,
                                                    contentDescription = "${m.author}的头像",
                                                    modifier = Modifier.fillMaxSize().clickable {
                                                        val intent = android.content.Intent(ctx, com.example.xinqiao.activity.UserInfoActivity::class.java)
                                                        intent.putExtra("name", m.author)
                                                        ctx.startActivity(intent)
                                                    },
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(44.dp))
                                        }
                                    }
                                    
                                    Column(
                                        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
                                    ) {
                                        if (!isMyMessage && showAvatarThis) {
                                            val name = nicknames[m.author] ?: m.author
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 4.dp, start = 44.dp)
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isMyMessage && !m.recalled && System.currentTimeMillis() - m.timestamp < 5*60*1000L) {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            try {
                                                                CommunityRepositoryProvider.current.recallGroupMessage(groupName, m.id)
                                                                chatMessages = chatMessages.map { if (it.id == m.id) it.copy(recalled = true) else it }
                                                            } catch (_: Exception) { }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Undo,
                                                        contentDescription = "撤回",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            
                                            Surface(
                                                shape = when {
                                                    isMyMessage -> MaterialTheme.shapes.large.copy(
                                                        bottomEnd = MaterialTheme.shapes.extraSmall.topEnd
                                                    )
                                                    else -> MaterialTheme.shapes.large.copy(
                                                        bottomStart = MaterialTheme.shapes.extraSmall.topStart
                                                    )
                                                },
                                                tonalElevation = if (isMyMessage) 2.dp else 1.dp,
                                                color = when {
                                                    m.recalled -> MaterialTheme.colorScheme.surfaceVariant
                                                    isMyMessage -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.surface
                                                },
                                                modifier = Modifier.widthIn(max = (conf.screenWidthDp.dp * 0.72f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (m.recalled) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Undo,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "消息已撤回",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                            )
                                                        }
                                                    } else {
                                                        if (m.content.isNotBlank()) {
                                                            Text(
                                                                text = m.content,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = if (isMyMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.padding(bottom = if (m.images.isNotEmpty() || m.voiceUrl != null || m.mentions.isNotEmpty()) 4.dp else 0.dp)
                                                            )
                                                        }
                                                        
                                                        if (m.voiceUrl != null) {
                                                            Surface(
                                                                onClick = {
                                                                    try {
                                                                        player?.release()
                                                                        val p = android.media.MediaPlayer()
                                                                        p.setDataSource(m.voiceUrl)
                                                                        p.setOnPreparedListener { it.start() }
                                                                        p.setOnCompletionListener {
                                                                            voiceProgress = 0f
                                                                            playingId = null
                                                                            it.release()
                                                                            player = null
                                                                        }
                                                                        p.prepareAsync()
                                                                        player = p
                                                                        playingId = m.id
                                                                        // 更新播放进度
                                                                        (ctx as? androidx.activity.ComponentActivity)?.let { act ->
                                                                            act.lifecycle.addObserver(object: androidx.lifecycle.DefaultLifecycleObserver {})
                                                                        }
                                                                        kotlinx.coroutines.GlobalScope.launch {
                                                                            try {
                                                                                while (playingId == m.id && player != null) {
                                                                                    val dur = player?.duration ?: 1
                                                                                    val cur = player?.currentPosition ?: 0
                                                                                    voiceProgress = (cur.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                                                                    kotlinx.coroutines.delay(100)
                                                                                }
                                                                            } catch (_: Exception) { }
                                                                        }
                                                                    } catch (_: Exception) { }
                                                                },
                                                                shape = MaterialTheme.shapes.medium,
                                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                                                modifier = Modifier.padding(top = 4.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.VolumeUp,
                                                                        contentDescription = null,
                                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                    val bars = 8
                                                                    val infinite = rememberInfiniteTransition()
                                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                                                                        repeat(bars) { i ->
                                                                            val anim by infinite.animateFloat(
                                                                                initialValue = 6f,
                                                                                targetValue = 14f,
                                                                                animationSpec = infiniteRepeatable(animation = tween(600 + i * 30, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
                                                                            )
                                                                            val h = if (playingId == m.id) anim.dp else (6 + (i % 3) * 4).dp
                                                                            Box(modifier = Modifier.width(2.dp).height(h).background(MaterialTheme.colorScheme.onSecondaryContainer))
                                                                        }
                                                                    }
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Box(modifier = Modifier.width(80.dp).height(4.dp).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))) {
                                                                        Box(modifier = Modifier.fillMaxHeight().width((80.dp * voiceProgress)).background(MaterialTheme.colorScheme.onSecondaryContainer))
                                                                    }
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Icon(
                                                                        imageVector = Icons.Default.PlayArrow,
                                                                        contentDescription = "播放",
                                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        
                                                        if (m.images.isNotEmpty()) {
                                                            val cols = 3
                                                            val cellSize = ((conf.screenWidthDp.dp * 0.72f) - 12.dp) / cols
                                                            LazyVerticalGrid(
                                                                columns = GridCells.Fixed(cols),
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.padding(top = 4.dp)
                                                            ) {
                                                                items(m.images) { url ->
                                                                    Surface(
                                                                        onClick = { previewImageUrl = url },
                                                                        shape = MaterialTheme.shapes.medium,
                                                                        modifier = Modifier.size(cellSize)
                                                                    ) {
                                                                        Image(
                                                                            painter = rememberAsyncImagePainter(
                                                                                model = coil.request.ImageRequest.Builder(ctx)
                                                                                    .data(imageDataFrom(url))
                                                                                    .allowHardware(false)
                                                                                    .size(512, 512)
                                                                                    .build()
                                                                            ),
                                                                            contentDescription = null,
                                                                            modifier = Modifier.fillMaxSize(),
                                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        
                                                        if (m.mentions.isNotEmpty()) {
                                                            FlowRow(
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.padding(top = if (m.content.isNotBlank() || m.voiceUrl != null || m.images.isNotEmpty()) 4.dp else 0.dp)
                                                            ) {
                                                                m.mentions.forEach { u ->
                                                                    Surface(
                                                                        onClick = {
                                                                            val intent = android.content.Intent(ctx, com.example.xinqiao.activity.UserInfoActivity::class.java)
                                                                            intent.putExtra("name", u)
                                                                            ctx.startActivity(intent)
                                                                        },
                                                                        shape = MaterialTheme.shapes.small,
                                                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                                                    ) {
                                                                        Text(
                                                                            text = "@$u",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    Text(
                                                        text = android.text.format.DateFormat.format("HH:mm", java.util.Date(m.timestamp)).toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.padding(top = if (m.recalled && m.content.isBlank() && m.voiceUrl == null && m.images.isEmpty() && m.mentions.isEmpty()) 0.dp else 4.dp)
                                                    )
                                                }
                                            }
                                            if (isMyMessage) {
                                                val isSending = m.id.startsWith("tmp")
                                                val rot = remember { Animatable(0f) }
                                                LaunchedEffect(isSending) {
                                                    if (isSending) rot.animateTo(360f, animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart)) else rot.snapTo(0f)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
                                                    if (isSending) {
                                                        Icon(
                                                            imageVector = Icons.Default.Autorenew,
                                                            contentDescription = "发送中",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rot.value }
                                                        )
                                                    } else {
                                                        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "已发送",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (isMyMessage) {
                                        val avatarRight = m.authorAvatar ?: avatars[m.author]
                                        val painterRight = if (avatarRight != null) {
                                            rememberAsyncImagePainter(
                                                model = coil.request.ImageRequest.Builder(ctx)
                                                    .data(imageDataFrom(avatarRight))
                                                    .allowHardware(false)
                                                    .size(128, 128)
                                                    .build()
                                            )
                                        } else painterResource(id = com.example.xinqiao.R.drawable.default_avatar)
                                        if (showAvatarThis) {
                                            Surface(shape = CircleShape, modifier = Modifier.size(36.dp).padding(start = 8.dp)) {
                                                Image(
                                                    painter = painterRight,
                                                    contentDescription = "${m.author}的头像",
                                                    modifier = Modifier.fillMaxSize().clickable {
                                                        val intent = android.content.Intent(ctx, com.example.xinqiao.activity.UserInfoActivity::class.java)
                                                        intent.putExtra("name", m.author)
                                                        ctx.startActivity(intent)
                                                    },
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(44.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                shape = MaterialTheme.shapes.large.copy(
                    topStart = MaterialTheme.shapes.extraLarge.topStart,
                    topEnd = MaterialTheme.shapes.extraLarge.topEnd
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Media Previews
                    if (chatImages.isNotEmpty()) {
                        var previewIndex by remember { mutableStateOf(0) }
                        var previewOpen by remember { mutableStateOf(false) }
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(chatImages) { i, url ->
                                Box {
                                    Surface(
                                        onClick = { previewIndex = i; previewOpen = true },
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                                                        Image(
                                                                            painter = rememberAsyncImagePainter(
                                                                                model = coil.request.ImageRequest.Builder(ctx)
                                                                                    .data(imageDataFrom(url))
                                                                                    .allowHardware(false)
                                                                                    .size(1024, 1024)
                                                                                    .build()
                                                                            ),
                                                                            contentDescription = null,
                                                                            modifier = Modifier.fillMaxSize(),
                                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                                        )
                                    }
                                    Surface(
                                        onClick = { chatImages = chatImages.filterIndexed { idx, _ -> idx != i } },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "删除",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (previewOpen) {
                            androidx.compose.ui.window.Dialog(onDismissRequest = { previewOpen = false }) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Image(
                                            painter = rememberAsyncImagePainter(chatImages[previewIndex]),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(320.dp)
                                                .clip(MaterialTheme.shapes.large),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { previewOpen = false }) {
                                                Text("关闭")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Voice Recording Preview
                    if (chatVoicePath != null && !recording) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            try {
                                                val player = android.media.MediaPlayer()
                                                player.setDataSource(chatVoicePath)
                                                player.setOnPreparedListener { it.start() }
                                                player.setOnCompletionListener { it.release() }
                                                player.prepareAsync()
                                            } catch (_: Exception) { }
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "播放",
                                            modifier = Modifier.padding(8.dp).size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "语音消息",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${chatVoiceDuration ?: 0}秒",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { chatVoicePath = null; chatVoiceDuration = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Recording Indicator
                    if (recording) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(8.dp).size(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(6.dp)
                                            ) {}
                                        }
                                    }
                                    Text(
                                        text = "正在录音...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            recorder?.stop()
                                            recorder?.release()
                                            recorder = null
                                            recording = false
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val mmr = android.media.MediaMetadataRetriever()
                                                    mmr.setDataSource(chatVoicePath)
                                                    val ms = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                                                    mmr.release()
                                                    val sec = ms?.let { (it / 1000).coerceAtLeast(1) }
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { chatVoiceDuration = sec }
                                                } catch (_: Exception) {
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { chatVoiceDuration = null }
                                                }
                                            }
                                        } catch (_: Exception) {
                                            recording = false
                                            recorder?.release()
                                            recorder = null
                                        }
                                    }
                                ) {
                                    Text(
                                        "完成",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Input Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Image Button
                        Surface(
                            onClick = { pickChatImages.launch("image/*") },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "选择图片",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Voice Button
                        Surface(
                            onClick = {
                                if (!recording) {
                                    requestAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else {
                                    try {
                                        recorder?.stop()
                                        recorder?.release()
                                        recorder = null
                                        recording = false
                                        try {
                                            val mmr = android.media.MediaMetadataRetriever()
                                            mmr.setDataSource(chatVoicePath)
                                            val ms = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                                            chatVoiceDuration = ms?.let { (it / 1000).coerceAtLeast(1) }
                                            mmr.release()
                                        } catch (_: Exception) { chatVoiceDuration = null }
                                    } catch (_: Exception) {
                                        recording = false
                                        recorder?.release()
                                        recorder = null
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = if (recording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (recording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (recording) "停止录音" else "录音",
                                    tint = if (recording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Text Input
                        TextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { 
                                Text(
                                    "输入消息...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            shape = MaterialTheme.shapes.large,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        // Send Button
                        Surface(
                            onClick = {
                                val msg = chatInput.trim()
                                if ((msg.isNotEmpty() || chatImages.isNotEmpty() || chatVoicePath != null) && groupName.isNotBlank()) {
                                    val currentUser = AnalysisUtils.readLoginUserName(ctx)
                                    val mentions = Regex("@([A-Za-z0-9_\\u4e00-\\u9fa5]+)").findAll(msg).map { it.groupValues[1] }.toList()
                                    val before = chatMessages
                                    val tmpId = "tmp" + System.currentTimeMillis()
                                    val userAvatar = avatars[currentUser]
                                    val optimisticMsg = GroupMessage(
                                        id = tmpId,
                                        groupName = groupName.trim(),
                                        author = currentUser,
                                        authorAvatar = userAvatar,
                                        content = msg,
                                        images = chatImages,
                                        mentions = mentions,
                                        voiceUrl = chatVoicePath,
                                        voiceDurationSec = chatVoiceDuration,
                                        timestamp = System.currentTimeMillis(),
                                        recalled = false
                                    )
                                    chatMessages = before + optimisticMsg
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            try {
                                                com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(optimisticMsg.toEntity()))
                                                android.util.Log.d("GroupChatDiag", "room insert tmp ok id=" + tmpId + " group=" + groupName.trim())
                                            } catch (e: Exception) {
                                                android.util.Log.e("GroupChatDiag", "room insert tmp failed: " + e.message)
                                                try {
                                                    val db = ctx.openOrCreateDatabase("community.db", android.content.Context.MODE_PRIVATE, null)
                                                    db.execSQL("CREATE TABLE IF NOT EXISTS group_messages (id TEXT PRIMARY KEY, groupName TEXT, author TEXT, authorAvatar TEXT, content TEXT, imagesJson TEXT, mentionsJson TEXT, voiceUrl TEXT, voiceDurationSec INTEGER, timestamp INTEGER, recalled INTEGER)")
                                                    val sql = "INSERT OR REPLACE INTO group_messages (id, groupName, author, authorAvatar, content, imagesJson, mentionsJson, voiceUrl, voiceDurationSec, timestamp, recalled) VALUES (?,?,?,?,?,?,?,?,?,?,?)"
                                                    val args = arrayOf(optimisticMsg.id, optimisticMsg.groupName, optimisticMsg.author, optimisticMsg.authorAvatar, optimisticMsg.content, com.google.gson.Gson().toJson(optimisticMsg.images), com.google.gson.Gson().toJson(optimisticMsg.mentions), optimisticMsg.voiceUrl, optimisticMsg.voiceDurationSec, optimisticMsg.timestamp, if (optimisticMsg.recalled) 1 else 0)
                                                    db.execSQL(sql, args)
                                                    db.close()
                                                    android.util.Log.d("GroupChatDiag", "sqlite insert tmp ok id=" + tmpId)
                                                } catch (ee: Exception) {
                                                    android.util.Log.e("GroupChatDiag", "sqlite insert tmp failed: " + ee.message)
                                                }
                                            }
                                            val rec = com.example.xinqiao.mysql.DBUtils.GroupMessageRecord()
                                            rec.id = tmpId; rec.groupName = groupName.trim(); rec.author = currentUser; rec.authorAvatar = userAvatar; rec.content = msg; rec.imagesJson = com.google.gson.Gson().toJson(chatImages); rec.mentionsJson = com.google.gson.Gson().toJson(mentions); rec.voiceUrl = chatVoicePath; rec.voiceDurationSec = chatVoiceDuration; rec.timestamp = System.currentTimeMillis(); rec.recalled = false
                                            try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).insertCommunityGroupMessage(rec) } catch (_: Exception) { }
                                            try {
                                                val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                                val raw = sp.getString("recentMessages_" + groupName.trim(), "[]")
                                                val list = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as java.util.ArrayList<com.example.xinqiao.community.GroupMessage> } catch (_: Exception) { java.util.ArrayList() }
                                                list.add(optimisticMsg)
                                                val kept = list.takeLast(50)
                                                sp.edit().putString("recentMessages_" + groupName.trim(), com.google.gson.Gson().toJson(kept)).apply()
                                            } catch (_: Exception) { }
                                        } catch (_: Exception) { }
                                    }
                                    chatInput = ""
                                    val imagesToSend = chatImages
                                    chatImages = emptyList()
                                    val voiceToSend = chatVoicePath
                                    val voiceDurationToSend = chatVoiceDuration
                                    chatVoicePath = null
                                    chatVoiceDuration = null
                                    scope.launch {
                                        try {
                                            val created = CommunityRepositoryProvider.current.postGroupMessage(groupName.trim(), msg, currentUser, imagesToSend, mentions, voiceToSend, voiceDurationToSend)
                                            val finalCreated = created.copy(authorAvatar = (avatars[currentUser] ?: created.authorAvatar))
                                            chatMessages = (before + optimisticMsg).dropLast(1) + finalCreated
                                            launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val dao = com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()
                                                    try {
                                                        dao?.delete(tmpId)
                                                        dao?.upsertAll(listOf(finalCreated.toEntity()))
                                                        android.util.Log.d("GroupChatDiag", "room insert created ok id=" + finalCreated.id)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("GroupChatDiag", "room insert created failed: " + e.message)
                                                        try {
                                                            val db = ctx.openOrCreateDatabase("community.db", android.content.Context.MODE_PRIVATE, null)
                                                            db.execSQL("CREATE TABLE IF NOT EXISTS group_messages (id TEXT PRIMARY KEY, groupName TEXT, author TEXT, authorAvatar TEXT, content TEXT, imagesJson TEXT, mentionsJson TEXT, voiceUrl TEXT, voiceDurationSec INTEGER, timestamp INTEGER, recalled INTEGER)")
                                                            val sql = "INSERT OR REPLACE INTO group_messages (id, groupName, author, authorAvatar, content, imagesJson, mentionsJson, voiceUrl, voiceDurationSec, timestamp, recalled) VALUES (?,?,?,?,?,?,?,?,?,?,?)"
                                                            val args = arrayOf(finalCreated.id, finalCreated.groupName, finalCreated.author, finalCreated.authorAvatar, finalCreated.content, com.google.gson.Gson().toJson(finalCreated.images), com.google.gson.Gson().toJson(finalCreated.mentions), finalCreated.voiceUrl, finalCreated.voiceDurationSec, finalCreated.timestamp, if (finalCreated.recalled) 1 else 0)
                                                            db.execSQL(sql, args)
                                                            db.close()
                                                            android.util.Log.d("GroupChatDiag", "sqlite insert created ok id=" + finalCreated.id)
                                                        } catch (ee: Exception) {
                                                            android.util.Log.e("GroupChatDiag", "sqlite insert created failed: " + ee.message)
                                                        }
                                                    }
                                                    try {
                                                        val db = com.example.xinqiao.mysql.DBUtils.getInstance(ctx)
                                                        db.deleteCommunityGroupMessage(tmpId)
                                                        val rec = com.example.xinqiao.mysql.DBUtils.GroupMessageRecord()
                                                        rec.id = finalCreated.id; rec.groupName = finalCreated.groupName; rec.author = finalCreated.author; rec.authorAvatar = finalCreated.authorAvatar; rec.content = finalCreated.content; rec.imagesJson = com.google.gson.Gson().toJson(finalCreated.images); rec.mentionsJson = com.google.gson.Gson().toJson(finalCreated.mentions); rec.voiceUrl = finalCreated.voiceUrl; rec.voiceDurationSec = finalCreated.voiceDurationSec; rec.timestamp = finalCreated.timestamp; rec.recalled = false
                                                        db.insertCommunityGroupMessage(rec)
                                                    } catch (_: Exception) { }
                                                    try {
                                                        val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                                        val raw = sp.getString("recentMessages_" + finalCreated.groupName.trim(), "[]")
                                                        val list = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as java.util.ArrayList<com.example.xinqiao.community.GroupMessage> } catch (_: Exception) { java.util.ArrayList() }
                                                        list.removeIf { it.id == tmpId }
                                                        list.add(finalCreated)
                                                        val kept = list.takeLast(50)
                                                        sp.edit().putString("recentMessages_" + finalCreated.groupName.trim(), com.google.gson.Gson().toJson(kept)).apply()
                                                    } catch (_: Exception) { }
                                                    try { com.example.xinqiao.community.RealtimeChatClient.send(org.json.JSONObject(mapOf("id" to finalCreated.id, "group" to finalCreated.groupName, "author" to finalCreated.author, "content" to finalCreated.content, "ts" to finalCreated.timestamp)).toString()) } catch (_: Exception) { }
                                                } catch (_: Exception) { }
                                            }
                                        } catch (_: Exception) {
                                            snackbarHostState.showSnackbar("发送失败")
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "发送",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            }

            AnimatedVisibility(visible = previewImageUrl != null, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f))
                        .clickable { previewImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                    ) {
                        Column {
                            var scale by remember { mutableStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }
                            val tstate = rememberTransformableState { zoomChange, panChange, _ ->
                                scale = (scale * zoomChange).coerceIn(1f, 4f)
                                offset = offset + panChange
                            }
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = coil.request.ImageRequest.Builder(ctx)
                                        .data(imageDataFrom(previewImageUrl))
                                        .allowHardware(false)
                                        .size(1600, 1600)
                                        .build()
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.large)
                                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                                    .transformable(tstate)
                                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero }) },
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { previewImageUrl = null }) { Text("关闭") }
                            }
                        }
                    }
                }
            }
        }
    }

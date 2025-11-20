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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.combinedClickable
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

private fun messageKey(m: com.example.xinqiao.community.GroupMessage): String {
    val c = (m.content.trim())
    val v = (m.voiceUrl ?: "")
    return listOf(m.author, c, v, m.timestamp.toString()).joinToString("|")
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

@Composable
fun VoiceWaveIcon(
    isPlaying: Boolean,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier,
    waveColor: Color = if (isMyMessage) Color.Black else MaterialTheme.colorScheme.onSurface
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    
    // 动画波浪高度
    val wave1Height by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = if (isPlaying) 8f else 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    
    val wave2Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = if (isPlaying) 12f else 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )
    
    val wave3Height by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = if (isPlaying) 10f else 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    Canvas(
        modifier = modifier.size(16.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = width / 5
        val spacing = barWidth * 0.5f
        
        // 绘制三个波浪条
        drawRect(
            color = waveColor,
            topLeft = Offset(0f, (height - wave1Height) / 2),
            size = androidx.compose.ui.geometry.Size(barWidth, wave1Height)
        )
        
        drawRect(
            color = waveColor,
            topLeft = Offset(barWidth + spacing, (height - wave2Height) / 2),
            size = androidx.compose.ui.geometry.Size(barWidth, wave2Height)
        )
        
        drawRect(
            color = waveColor,
            topLeft = Offset((barWidth + spacing) * 2, (height - wave3Height) / 2),
            size = androidx.compose.ui.geometry.Size(barWidth, wave3Height)
        )
    }
}

private fun formatDateSection(ctx: android.content.Context, timestamp: Long): String {
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
        tday == today -> ctx.getString(com.example.xinqiao.R.string.community_today)
        tday == today - 24L * 60 * 60 * 1000 -> ctx.getString(com.example.xinqiao.R.string.community_yesterday)
        tday == today - 2L * 24 * 60 * 60 * 1000 -> ctx.getString(com.example.xinqiao.R.string.community_day_before_yesterday)
        else -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
    val time = android.text.format.DateFormat.format("HH:mm", java.util.Date(timestamp)).toString()
    return "$label $time"
}

class GroupChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupName = intent.getStringExtra("group")?.trim() ?: ""
        try {
            val sp = getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
            sp.edit().putString("lastActiveGroup", groupName).apply()
        } catch (_: Exception) { }
        setContent {
            MaterialTheme {
                GroupChatScreen(groupName = groupName)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GroupChatScreen(groupName: String) {
    val ctx = LocalContext.current
    val conf = LocalConfiguration.current
    val tokens = CommunityTokensInstance
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nicknames = remember { mutableStateMapOf<String, String>() }
    val avatars = remember { mutableStateMapOf<String, String?>() }
    var contextMenuMessageId by remember { mutableStateOf<String?>(null) }
    val unreadVoices = remember { mutableStateMapOf<String, Boolean>() }
    var pressedVoiceId by remember { mutableStateOf<String?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var voiceProgress by remember { mutableStateOf(0f) }
    var playerPrepared by remember { mutableStateOf(false) }
    var progressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val currentUserAll = AnalysisUtils.readLoginUserName(ctx)
    var lastRealtimeSeq by remember { mutableStateOf(0L) }
    var chatMessages by remember { mutableStateOf<List<GroupMessage>>(emptyList()) }
    
    var realtimeConnected by remember { mutableStateOf(false) }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                groupName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (realtimeConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = if (realtimeConnected) ctx.getString(com.example.xinqiao.R.string.connected) else ctx.getString(com.example.xinqiao.R.string.disconnected),
                                tint = if (realtimeConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
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
                                contentDescription = ctx.getString(com.example.xinqiao.R.string.back),
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
                                    contentDescription = ctx.getString(com.example.xinqiao.R.string.more_label),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                androidx.compose.material3.DropdownMenuItem(text = { Text(ctx.getString(com.example.xinqiao.R.string.menu_leave_group)) }, onClick = {
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
                                androidx.compose.material3.DropdownMenuItem(text = { Text(ctx.getString(com.example.xinqiao.R.string.menu_clear_chat)) }, onClick = {
                                    menuOpen = false
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).deleteCommunityGroupMessagesByGroup(groupName.trim()) } catch (_: Exception) { }
                                        try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).deleteVoiceReadForUser(groupName.trim(), AnalysisUtils.readLoginUserName(ctx)) } catch (_: Exception) { }
                                        try { com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.deleteByGroup(groupName.trim()) } catch (_: Exception) { }
                                        try {
                                            val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                            sp.edit().remove("recentMessages_" + groupName.trim()).apply()
                                        } catch (_: Exception) { }
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            chatMessages = emptyList()
                                            snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_cleared))
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
            var chatLoading by remember { mutableStateOf(false) }
            var chatInput by remember { mutableStateOf("") }
            var chatImages by remember { mutableStateOf<List<String>>(emptyList()) }
            var chatVoicePath by remember { mutableStateOf<String?>(null) }
            var chatVoiceDuration by remember { mutableStateOf<Int?>(null) }
            var recording by remember { mutableStateOf(false) }
            var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
            var recordBars by remember { mutableStateOf(List(12) { 4f }) }
            var recordTimerLeft by remember { mutableStateOf(60) }
            var recordCancel by remember { mutableStateOf(false) }
            var recordOverlay by remember { mutableStateOf(false) }
            var recordJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            var voiceMode by remember { mutableStateOf(false) }
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
                        recordTimerLeft = 60
                        recordCancel = false
                        recordOverlay = true
                        try { recordJob?.cancel() } catch (_: Exception) {}
                        recordJob = scope.launch {
                            try {
                                while (recording && recorder != null) {
                                    try {
                                        val amp = recorder?.maxAmplitude ?: 0
                                        val v = (kotlin.math.sqrt(amp.toFloat()) / 8f).coerceIn(2f, 16f)
                                        val newBars = recordBars.toMutableList()
                                        newBars.removeAt(0)
                                        newBars.add(v)
                                        recordBars = newBars
                                    } catch (_: Exception) {}
                                    kotlinx.coroutines.delay(80)
                                }
                            } catch (_: Exception) {}
                        }
                        scope.launch {
                            try {
                                while (recording && recordTimerLeft > 0) {
                                    kotlinx.coroutines.delay(1000)
                                    recordTimerLeft -= 1
                                }
                                if (recording) {
                                    try {
                                        recorder?.stop(); recorder?.release(); recorder = null
                                        recording = false
                                        recordOverlay = false
                                    } catch (_: Exception) { recording = false; recorder?.release(); recorder = null; recordOverlay = false }
                                    try {
                                        val mmr = android.media.MediaMetadataRetriever()
                                        mmr.setDataSource(chatVoicePath)
                                        val ms = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                                        chatVoiceDuration = ms?.let { (it / 1000).coerceAtLeast(1) }
                                        mmr.release()
                                    } catch (_: Exception) { chatVoiceDuration = null }
                                    if (groupName.isNotBlank() && chatVoicePath != null) {
                                        val currentUser = AnalysisUtils.readLoginUserName(ctx)
                                        val before = chatMessages
                                        val tmpId = "tmp" + System.currentTimeMillis()
                                        val userAvatar = avatars[currentUser]
                                        val optimisticMsg = GroupMessage(
                                            id = tmpId,
                                            groupName = groupName.trim(),
                                            author = currentUser,
                                            authorAvatar = userAvatar,
                                            content = "",
                                            images = emptyList(),
                                            mentions = emptyList(),
                                            voiceUrl = chatVoicePath,
                                            voiceDurationSec = chatVoiceDuration,
                                            timestamp = System.currentTimeMillis(),
                                            recalled = false
                                        )
                                        chatMessages = before + optimisticMsg
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                try { com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(optimisticMsg.toEntity())) } catch (_: Exception) { }
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
                                        val voiceToSend = chatVoicePath
                                        val voiceDurationToSend = chatVoiceDuration
                                        chatVoicePath = null
                                        chatVoiceDuration = null
                                        scope.launch {
                                            try {
                                                val created = CommunityRepositoryProvider.current.postGroupMessage(groupName.trim(), "", currentUser, emptyList(), emptyList(), voiceToSend, voiceDurationToSend)
                                                val finalCreated = created.copy(authorAvatar = (avatars[currentUser] ?: created.authorAvatar))
                                                chatMessages = (before + optimisticMsg).dropLast(1) + finalCreated
                                                launch(kotlinx.coroutines.Dispatchers.IO) {
                                                    try {
                                                        val dao = com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()
                                                        try { dao?.delete(tmpId); dao?.upsertAll(listOf(finalCreated.toEntity())) } catch (_: Exception) { }
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
                                } catch (_: Exception) { snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_send_failed)) }
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {
                        recording = false
                        recorder?.release()
                        recorder = null
                        scope.launch { snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_record_start_failed)) }
                    }
                } else {
                    scope.launch { snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_permission_denied)) }
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
                        val mergedRaw = (mergedLocal + cloud)
                        val byId = mergedRaw.distinctBy { it.id }
                        val merged = mutableListOf<com.example.xinqiao.community.GroupMessage>()
                        byId.sortedBy { it.timestamp }.forEach { m ->
                            val dupIndex = merged.indexOfLast { existing ->
                                existing.groupName == m.groupName &&
                                existing.author == m.author &&
                                existing.content.trim() == m.content.trim() &&
                                ((existing.voiceUrl ?: "") == (m.voiceUrl ?: "")) &&
                                (existing.images == m.images) &&
                                kotlin.math.abs(existing.timestamp - m.timestamp) <= 60_000
                            }
                            if (dupIndex >= 0) {
                                val old = merged[dupIndex]
                                val pick = when {
                                    old.id.startsWith("tmp") && !m.id.startsWith("tmp") -> m
                                    !old.id.startsWith("tmp") && m.id.startsWith("tmp") -> old
                                    m.timestamp >= old.timestamp -> m
                                    else -> old
                                }
                                merged[dupIndex] = pick
                            } else {
                                merged.add(m)
                            }
                        }
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
                    try {
                        val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                        lastRealtimeSeq = sp.getLong("lastRealtimeSeq_" + groupName.trim(), 0L)
                    } catch (_: Exception) { lastRealtimeSeq = 0L }
                    com.example.xinqiao.community.RealtimeChatClient.connect(ctx, groupName, currentUserAll, { payload ->
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
                            val seq = if (obj.has("seq")) obj.getLong("seq") else gm.timestamp
                            if (seq <= lastRealtimeSeq) return@connect
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                // 实时消息回填作者头像
                                try {
                                    val av = try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPathByNameOrNickSync(gm.author) } catch (_: Exception) { null }
                                    if (av != null) { avatars[gm.author] = av }
                                    val withAvatar = if (av != null) gm.copy(authorAvatar = av) else gm
                                    com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(withAvatar.toEntity()))
                                } catch (_: Exception) { try { com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(gm.toEntity())) } catch (_: Exception) {} }
                                try { } catch (_: Exception) {}
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    val existsById = chatMessages.any { it.id == gm.id }
                                    val existsByKey = chatMessages.any { messageKey(it) == messageKey(gm) }
                                    when {
                                        existsById -> chatMessages = chatMessages.map { if (it.id == gm.id) gm else it }
                                        existsByKey -> {
                                            // 保留时间靠后的消息，替换旧条
                                            val k = messageKey(gm)
                                            chatMessages = chatMessages.map { if (messageKey(it) == k && gm.timestamp >= it.timestamp) gm else it }
                                        }
                                        else -> chatMessages = chatMessages + gm
                                    }
                                    try {
                                        if (gm.author.isNotBlank() && !gm.author.equals(currentUserAll, ignoreCase = true)) {
                                            com.example.xinqiao.notifications.NotificationUtils.showMessageNotification(ctx, gm.groupName, gm.author, gm.content ?: "")
                                        }
                                    } catch (_: Exception) {}
                                    if (seq > lastRealtimeSeq) {
                                        lastRealtimeSeq = seq
                                        try {
                                            val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                            sp.edit().putLong("lastRealtimeSeq_" + groupName.trim(), lastRealtimeSeq).apply()
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }, { ok ->
                        realtimeConnected = ok
                        if (!ok) {
                            try { scope.launch { snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_realtime_failed)) } } catch (_: Exception) {}
                        }
                    })
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
                    // 计算未读语音集合（优先服务端，IO线程+超时；失败回退本地）
                    try {
                        val readServer: List<String> = try {
                            kotlinx.coroutines.withTimeout(2000) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listVoiceRead(groupName.trim(), currentUserAll)
                                }
                            }
                        } catch (_: Exception) { emptyList() }
                        unreadVoices.clear()
                        chatMessages.filter { it.voiceUrl != null && it.author != currentUserAll }.forEach { m -> unreadVoices[m.id] = !readServer.contains(m.id) }
                    } catch (_: Exception) {
                        try {
                            val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                            val key = "voiceRead_" + groupName.trim()
                            val raw = sp.getString(key, "[]")
                            val readList = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as java.util.ArrayList<String> } catch (_: Exception) { java.util.ArrayList() }
                            unreadVoices.clear()
                            chatMessages.filter { it.voiceUrl != null && it.author != currentUserAll }.forEach { m -> unreadVoices[m.id] = !readList.contains(m.id) }
                        } catch (_: Exception) { }
                    }
                } catch (_: Exception) {}
            }
            LaunchedEffect(chatMessages.size) {
                try {
                    if (chatMessages.isNotEmpty()) {
                        listState.scrollToItem(chatMessages.size - 1)
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
                                    ctx.getString(com.example.xinqiao.R.string.no_messages),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    ctx.getString(com.example.xinqiao.R.string.start_chatting),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
                        ) {
                            itemsIndexed(chatMessages, key = { _, m -> m.id }) { index, m ->
                                val isMyMessage = m.author == currentUserAll
                                val isAnonymousMe = isMyMessage && com.example.xinqiao.community.SettingsRepository.isAnonymous(ctx, currentUserAll)
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
                                                text = formatDateSection(ctx, m.timestamp),
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
                                        val avatarLeft = if (isAnonymousMe) null else (m.authorAvatar ?: avatars[m.author])
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
                                            val avatarSize = 36.dp
                                            val avatarRadius = avatarSize * 0.2f
                                            Surface(shape = RoundedCornerShape(avatarRadius), modifier = Modifier.size(avatarSize).padding(end = 8.dp)) {
                                                Image(
                                                    painter = painterLeft,
                                                    contentDescription = ctx.getString(com.example.xinqiao.R.string.avatar_of_fmt, m.author),
                                                    modifier = Modifier.fillMaxSize().clickable {
                                                        if (!isAnonymousMe) {
                                                            val intent = android.content.Intent(ctx, com.example.xinqiao.activity.UserInfoActivity::class.java)
                                                            intent.putExtra("name", m.author)
                                                            ctx.startActivity(intent)
                                                        }
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
                                                        contentDescription = ctx.getString(com.example.xinqiao.R.string.message_recalled),
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            
                                            Surface(
                                                shape = RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isMyMessage) 12.dp else 3.dp,
                                                    bottomEnd = if (isMyMessage) 3.dp else 12.dp
                                                ),
                                                tonalElevation = 0.dp,
                                                color = when {
                                                    m.recalled -> MaterialTheme.colorScheme.surfaceVariant
                                                    isMyMessage -> Color(0xFF95EC69) // 微信绿色
                                                    else -> MaterialTheme.colorScheme.surface
                                                },
                                                modifier = Modifier
                                                    .widthIn(max = (conf.screenWidthDp.dp * 0.75f))
                                                    .combinedClickable(onLongClick = { contextMenuMessageId = m.id }, onClick = {})
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                                                text = ctx.getString(com.example.xinqiao.R.string.message_recalled),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                            )
                                                        }
                                                    } else {
                                                        if (!isMyMessage) {
                                                            val nameLocal = if (isAnonymousMe) ctx.getString(com.example.xinqiao.R.string.anonymous_user) else (nicknames[m.author] ?: m.author)
                                                            Text(
                                                                text = nameLocal,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(bottom = 2.dp)
                                                            )
                                                        }
                                                        if (m.content.isNotBlank()) {
                                                            Text(
                                                                text = m.content,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = if (isMyMessage) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.padding(bottom = if (m.images.isNotEmpty() || m.voiceUrl != null || m.mentions.isNotEmpty()) 4.dp else 0.dp)
                                                            )
                                                        }
                                                        
                                                        if (m.voiceUrl != null) {
                                                            val durSec = (m.voiceDurationSec ?: 0).coerceAtLeast(0)
                                                            val bubbleWidth = (75 + ((200 - 75) * (durSec.coerceAtMost(60) / 60f))).dp
                                                            val isUnread = unreadVoices[m.id] == true && m.author != currentUserAll
                                                            Surface(
                                                                onClick = {
                                                                    try {
                                                                        if (playingId == m.id && player?.isPlaying == true) {
                                                                            player?.pause(); playingId = null
                                                                        } else if (playingId == m.id && player != null) {
                                                                            player?.start(); playingId = m.id
                                                                        } else {
                                                                            player?.release(); player = null; playingId = null
                                                                            val p = android.media.MediaPlayer()
                                                                            p.setDataSource(m.voiceUrl)
                                                                            p.setOnPreparedListener {
                                                                                playerPrepared = true
                                                                                it.start()
                                                                                kotlinx.coroutines.GlobalScope.launch {
                                                                                    try {
                                                                                        while (playingId == m.id && playerPrepared && player != null) {
                                                                                            val dur = player?.duration ?: 0
                                                                                            val cur = player?.currentPosition ?: 0
                                                                                            if (dur > 0) voiceProgress = (cur.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                                                                            kotlinx.coroutines.delay(120)
                                                                                        }
                                                                                    } catch (_: Exception) { }
                                                                                }
                                                                            }
                                                                            p.setOnCompletionListener { voiceProgress = 0f; playingId = null; playerPrepared = false; it.release(); player = null }
                                                                            p.setOnErrorListener { mp, _, _ -> voiceProgress = 0f; playingId = null; playerPrepared = false; try { mp.release() } catch (_: Exception) {}; player = null; true }
                                                                            p.prepareAsync(); player = p; playingId = m.id
                                                                        }
                                                                        // 标记已读
                                                                        try {
                                                                            unreadVoices[m.id] = false
                                                                            val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                                                                            val key = "voiceRead_" + groupName.trim()
                                                                            val raw = sp.getString(key, "[]")
                                                                            val list = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as java.util.ArrayList<String> } catch (_: Exception) { java.util.ArrayList() }
                                                                            if (!list.contains(m.id)) { list.add(m.id); sp.edit().putString(key, com.google.gson.Gson().toJson(list)).apply() }
                                                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                                                try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).markVoiceRead(groupName.trim(), currentUserAll, m.id) } catch (_: Exception) {}
                                                                            }
                                                                        } catch (_: Exception) {}
                                                                    } catch (_: Exception) { }
                                                                },
                                                                shape = RoundedCornerShape(12.dp),
                                                                color = if (isMyMessage) Color(0xFF95EC69) else MaterialTheme.colorScheme.surface,
                                                                modifier = Modifier
                                                                    .padding(top = 4.dp)
                                                                    .width(bubbleWidth)
                                                                    .combinedClickable(onClick = {}, onLongClick = { pressedVoiceId = m.id })
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                                ) {
                                                                    VoiceWaveIcon(
                                                                        isPlaying = playingId == m.id,
                                                                        isMyMessage = isMyMessage,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                    Text(
                                                                        text = "${durSec}″",
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = if (isMyMessage) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                                    )
                                                                    if (isUnread) {
                                                                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        
                                                        if (m.images.isNotEmpty()) {
                                                            val imageSize = 120.dp
                                                            Surface(
                                                                onClick = { previewImageUrl = m.images.first() },
                                                                shape = RoundedCornerShape(12.dp),
                                                                modifier = Modifier
                                                                    .padding(top = 4.dp)
                                                                    .size(imageSize)
                                                            ) {
                                                                Image(
                                                                    painter = rememberAsyncImagePainter(
                                                                        model = coil.request.ImageRequest.Builder(ctx)
                                                                            .data(imageDataFrom(m.images.first()))
                                                                            .allowHardware(false)
                                                                            .size(256, 256)
                                                                            .build()
                                                                    ),
                                                                    contentDescription = null,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                                )
                                                            }
                                                        }
                                                        
                                                        if (m.mentions.isNotEmpty()) {
                                                            FlowRow(
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.padding(top = if (m.content.isNotBlank() || m.voiceUrl != null || m.images.isNotEmpty()) 4.dp else 0.dp)
                                                            ) {
                                                                m.mentions.forEach { u ->
                                                                    Text(
                                                                        text = "@$u",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    Text(
                                                        text = android.text.format.DateFormat.format("HH:mm", java.util.Date(m.timestamp)).toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(top = if (m.recalled && m.content.isBlank() && m.voiceUrl == null && m.images.isEmpty() && m.mentions.isEmpty()) 0.dp else 2.dp)
                                                    )
                                                }
                                            }
                                            if (isMyMessage) {
                                                val isSending = m.id.startsWith("tmp")
                                                if (isSending) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = ctx.getString(com.example.xinqiao.R.string.sending),
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(14.dp).padding(start = 4.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = ctx.getString(com.example.xinqiao.R.string.sent),
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(14.dp).padding(start = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (isMyMessage) {
                                        val avatarRight = if (isAnonymousMe) null else (m.authorAvatar ?: avatars[m.author])
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
                                            val avatarSize = 36.dp
                                            val avatarRadius = avatarSize * 0.2f
                                            Surface(shape = RoundedCornerShape(avatarRadius), modifier = Modifier.size(avatarSize).padding(start = 8.dp)) {
                                                Image(
                                                    painter = painterRight,
                                                    contentDescription = ctx.getString(com.example.xinqiao.R.string.avatar_of_fmt, m.author),
                                                    modifier = Modifier.fillMaxSize().clickable {
                                                        if (!isAnonymousMe) {
                                                            val intent = android.content.Intent(ctx, com.example.xinqiao.activity.UserInfoActivity::class.java)
                                                            intent.putExtra("name", m.author)
                                                            ctx.startActivity(intent)
                                                        }
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
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recording Indicator
                    if (recording) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                    Text(
                                        text = ctx.getString(com.example.xinqiao.R.string.recording_in_progress_fmt, recordTimerLeft),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
                                    Text(ctx.getString(com.example.xinqiao.R.string.confirm))
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
                        IconButton(
                            onClick = { pickChatImages.launch("image/*") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = ctx.getString(com.example.xinqiao.R.string.select_image),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (!voiceMode) {
                            TextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { 
                                    Text(ctx.getString(com.example.xinqiao.R.string.input_placeholder)) 
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(onPress = {
                                            if (!recording) {
                                                requestAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                            }
                                            try { awaitRelease() } catch (_: Exception) {}
                                            if (recording) {
                                                try { recorder?.stop() } catch (_: Exception) {}
                                                try { recorder?.release() } catch (_: Exception) {}
                                                recorder = null
                                                recording = false
                                                recordOverlay = false
                                                try {
                                                    val mmr = android.media.MediaMetadataRetriever()
                                                    mmr.setDataSource(chatVoicePath)
                                                    val ms = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                                                    val sec = ms?.let { (it / 1000).coerceAtLeast(1) }
                                                    chatVoiceDuration = sec
                                                    mmr.release()
                                                } catch (_: Exception) { chatVoiceDuration = null }
                                                try {
                                                    val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 70)
                                                    tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
                                                } catch (_: Exception) {}
                                                val msg = ""
                                                if (groupName.isNotBlank() && chatVoicePath != null) {
                                                    val currentUser = AnalysisUtils.readLoginUserName(ctx)
                                                    val before = chatMessages
                                                    val tmpId = "tmp" + System.currentTimeMillis()
                                                    val userAvatar = avatars[currentUser]
                                                    val optimisticMsg = GroupMessage(
                                                        id = tmpId,
                                                        groupName = groupName.trim(),
                                                        author = currentUser,
                                                        authorAvatar = userAvatar,
                                                        content = msg,
                                                        images = emptyList(),
                                                        mentions = emptyList(),
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
                                                            } catch (_: Exception) { }
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
                                                    val voiceToSend = chatVoicePath
                                                    val voiceDurationToSend = chatVoiceDuration
                                                    chatVoicePath = null
                                                    chatVoiceDuration = null
                                                    scope.launch {
                                                        try {
                                                            val created = CommunityRepositoryProvider.current.postGroupMessage(groupName.trim(), msg, currentUser, emptyList(), emptyList(), voiceToSend, voiceDurationToSend)
                                                            val finalCreated = created.copy(authorAvatar = (avatars[currentUser] ?: created.authorAvatar))
                                                            chatMessages = (before + optimisticMsg).dropLast(1) + finalCreated
                                                            launch(kotlinx.coroutines.Dispatchers.IO) {
                                                                try {
                                                                    val dao = com.example.xinqiao.community.CommunityLocalCache.database()?.groupChatDao()
                                                                    try {
                                                                        dao?.delete(tmpId)
                                                                        dao?.upsertAll(listOf(finalCreated.toEntity()))
                                                                    } catch (_: Exception) { }
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
                                                        } catch (_: Exception) { snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_send_failed)) }
                                                    }
                                                }
                                            }
                                        })
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(ctx.getString(com.example.xinqiao.R.string.press_to_speak), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

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
                                            snackbarHostState.showSnackbar(ctx.getString(com.example.xinqiao.R.string.snackbar_send_failed))
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
                                    contentDescription = ctx.getString(com.example.xinqiao.R.string.send),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = { voiceMode = !voiceMode }) { 
                            Icon(
                                imageVector = if (voiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                                contentDescription = if (voiceMode) ctx.getString(com.example.xinqiao.R.string.toggle_text_mode) else ctx.getString(com.example.xinqiao.R.string.toggle_voice_mode),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
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
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = coil.request.ImageRequest.Builder(ctx)
                                        .data(imageDataFrom(previewImageUrl))
                                        .allowHardware(false)
                                        .size(1024, 1024)
                                        .build()
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.large),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { previewImageUrl = null }) { Text(ctx.getString(com.example.xinqiao.R.string.close)) }
                            }
                        }
                    }
                }
            }
        }
    }

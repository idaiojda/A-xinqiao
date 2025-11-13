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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsTimelineNew(controller: CommunityController) {
    val tokens = CommunityTokensInstance
    val timeline = remember { mutableStateListOf<TimelineItem>() }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var groupSchedule by remember { mutableStateOf("") }
    var groupCapacity by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            timeline.clear()
            timeline.addAll(CommunityRepositoryProvider.current.getMyTimeline())
        } catch (e: Exception) {
            error = "时间线加载失败：" + (e.message ?: "网络异常")
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
                Button(
                    onClick = { showCreateSheet = true },
                    shape = RoundedCornerShape(tokens.corner.Button)
                ) {
                    Text("创建小组")
                }
            }
            Divider(color = tokens.color.Neutral200)
            if (error != null) {
                Text(error!!, color = tokens.color.Danger, style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    timeline.forEach { item ->
                        val emoji = when (item.type) {
                            "checkin" -> "🧘"
                            "share"   -> "🎵"
                            "badge"   -> "🏅"
                            else      -> "•"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, modifier = Modifier.width(24.dp))
                            Spacer(modifier = Modifier.width(tokens.spacing.S))
                            Text(item.text, style = MaterialTheme.typography.bodySmall, color = tokens.color.Neutral700)
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showCreateSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                Text("创建小组", style = MaterialTheme.typography.titleMedium)
                TextField(value = groupName, onValueChange = { groupName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("名称") })
                TextField(value = groupDesc, onValueChange = { groupDesc = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("简介") })
                TextField(value = groupSchedule, onValueChange = { groupSchedule = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("日程安排，如 每周三/五 20:00") })
                TextField(value = groupCapacity, onValueChange = { groupCapacity = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("容量，人数上限") })
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                    Button(onClick = {
                        if (groupName.isBlank()) return@Button
                        val cap = groupCapacity.toIntOrNull() ?: 0
                        creating = true
                        // 调用仓库创建
                        scope.launch {
                            try {
                                val res = CommunityRepositoryProvider.current.createGroup(groupName.trim(), groupDesc.trim(), groupSchedule.trim(), cap)
                                if (res.ok) {
                                    val now = System.currentTimeMillis()
                                    // 插入到时间线顶部
                                    timeline.add(0, TimelineItem("badge", "创建小组：${groupName.trim()}", now))
                                    // 标记加入状态（用于其它展示）
                                    controller.setJoined(groupName.trim(), true)
                                    showCreateSheet = false
                                    groupName = ""; groupDesc = ""; groupSchedule = ""; groupCapacity = ""
                                } else {
                                    error = res.message
                                }
                            } catch (e: Exception) {
                                error = "创建失败：" + (e.message ?: "网络异常")
                            } finally {
                                creating = false
                            }
                        }
                    }, enabled = !creating) { Text(if (creating) "发布中…" else "发布") }
                    TextButton(onClick = { showCreateSheet = false }) { Text("取消") }
                }
            }
        }
    }
}

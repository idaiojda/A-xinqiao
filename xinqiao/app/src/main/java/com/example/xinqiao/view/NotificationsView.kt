package com.example.xinqiao.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xinqiao.community.CommunityRepositoryProvider
import com.example.xinqiao.community.NotificationItem
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen() {
    var page by remember { mutableStateOf(0) }
    val size = 10
    var loading by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page) {
        loading = true
        try {
            val all = CommunityRepositoryProvider.current.getNotifications()
            val from = (page * size).coerceAtMost(all.size)
            val to = (from + size).coerceAtMost(all.size)
            list = all.subList(0, to)
        } finally { loading = false }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("消息中心", style = MaterialTheme.typography.titleMedium)
        if (loading && list.isEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(list, key = { _, n -> n.id }) { _, n ->
                    Surface(tonalElevation = 1.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text(n.title, style = MaterialTheme.typography.bodyLarge); Text(n.content, style = MaterialTheme.typography.bodyMedium) }
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        CommunityRepositoryProvider.current.markNotificationRead(n.id)
                                        list = list.map { if (it.id == n.id) it.copy(read = true) else it }
                                    } catch (_: Exception) {}
                                }
                            }) { Text(if (n.read) "已读" else "标记已读") }
                        }
                    }
                }
                item {
                    if (!loading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = { page += 1 }) { Text("加载更多") }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                    }
                }
            }
        }
    }
}

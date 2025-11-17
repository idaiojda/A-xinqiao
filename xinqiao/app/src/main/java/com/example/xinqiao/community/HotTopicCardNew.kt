package com.example.xinqiao.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HotTopicCardNew(controller: CommunityController) {
    val tokens = CommunityTokensInstance
    var hotTag by remember { mutableStateOf<String?>(null) }
    var hotPreview by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val list = CommunityRepositoryProvider.current.getPosts(category = null, page = 0, size = 20, q = null)
            if (list.isNotEmpty()) {
                val tagCounts = mutableMapOf<String, Int>()
                list.forEach { p -> p.tags.forEach { t -> tagCounts[t] = (tagCounts[t] ?: 0) + 1 } }
                val topTag = tagCounts.entries.maxByOrNull { it.value }?.key
                val hotPost = list.maxByOrNull { it.likeCount + it.commentCount } ?: list.first()
                hotTag = topTag ?: hotPost.tags.firstOrNull()
                hotPreview = if (hotPost.title.isNotBlank()) hotPost.title else hotPost.content.take(64)
            } else {
                hotTag = null
                hotPreview = ""
            }
        } catch (_: Exception) {
            hotTag = null
            hotPreview = ""
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
                    text = "今日热门话题",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = tokens.type.CardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.color.Neutral900
                    )
                )
                OutlinedButton(
                    onClick = { hotTag?.let { controller.updateSearch(it) } },
                    shape = RoundedCornerShape(tokens.corner.Button),
                    enabled = hotTag != null
                ) {
                    Text("参与讨论")
                }
            }
            AssistChip(
                onClick = { hotTag?.let { controller.updateSearch(it) } },
                label = { Text("# " + (hotTag ?: "暂无")) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = tokens.color.LavenderSoft,
                    labelColor = tokens.color.PrimaryDark
                ),
                shape = RoundedCornerShape(tokens.corner.Chip)
            )
            Text(
                text = if (hotPreview.isNotBlank()) hotPreview else "暂无热门预览",
                style = MaterialTheme.typography.bodyMedium.copy(color = tokens.color.Neutral700)
            )
        }
    }
}

package com.example.xinqiao.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun RecommendedGroupCardNew(
    controller: CommunityController
) {
    val tokens = CommunityTokensInstance
    var groups by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadErr by remember { mutableStateOf<String?>(null) }
    var applyMessage by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            groups = CommunityRepositoryProvider.current.getGroups()
        } catch (e: Exception) {
            loadErr = "加载推荐小组失败：" + (e.message ?: "网络异常")
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
            val name = groups.firstOrNull()
            Text(
                text = if (name != null) "推荐加入：$name" else "暂无推荐小组",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = tokens.type.CardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.color.Neutral900
                )
            )
            Text(
                text = "每日 10 分钟，科学缓解考试与工作压力",
                style = MaterialTheme.typography.bodySmall.copy(color = tokens.color.Neutral700)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.M)
            ) {
                Button(
                    onClick = {
                        if (isApplying || name == null) return@Button
                        scope.launch {
                            isApplying = true
                            try {
                                val res = CommunityRepositoryProvider.current.applyJoin(name!!)
                                applyMessage = if (res.accepted) "已申请成功：${res.message}" else "申请未通过：${res.message}"
                                if (res.accepted) controller.setJoined(name, true)
                            } catch (e: Exception) {
                                applyMessage = "申请失败：" + (e.message ?: "网络异常")
                            } finally {
                                isApplying = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(tokens.corner.Button),
                    enabled = !isApplying && name != null
                ) {
                    Text(if (isApplying) "申请中…" else "申请加入")
                }
                val ctx = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        val n = name ?: return@OutlinedButton
                        val intent = android.content.Intent(ctx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                        intent.putExtra("group", n)
                        ctx.startActivity(intent)
                    },
                    shape = RoundedCornerShape(tokens.corner.Button),
                    enabled = name != null
                ) {
                    Text("查看介绍")
                }
            }
            applyMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = tokens.color.Neutral700)
            }
            loadErr?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = tokens.color.Danger)
            }
        }
    }
}

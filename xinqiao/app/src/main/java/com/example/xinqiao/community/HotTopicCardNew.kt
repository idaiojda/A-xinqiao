package com.example.xinqiao.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HotTopicCardNew() {
    val tokens = CommunityTokensInstance
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
                    onClick = { /* TODO 参与讨论 */ },
                    shape = RoundedCornerShape(tokens.corner.Button)
                ) {
                    Text("参与讨论")
                }
            }
            AssistChip(
                onClick = { /* TODO 话题详情 */ },
                label = { Text("# 失眠困扰") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = tokens.color.LavenderSoft,
                    labelColor = tokens.color.PrimaryDark
                ),
                shape = RoundedCornerShape(tokens.corner.Chip)
            )
            Text(
                text = "考试焦虑如何缓解？匿名分享你的经验。",
                style = MaterialTheme.typography.bodyMedium.copy(color = tokens.color.Neutral700)
            )
        }
    }
}
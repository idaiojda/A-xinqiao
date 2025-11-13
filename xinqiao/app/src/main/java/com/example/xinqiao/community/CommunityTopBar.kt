package com.example.xinqiao.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTopBar(onAsk: () -> Unit, onNotifications: () -> Unit) {
    val tokens = CommunityTokensInstance
    Surface(
        color = tokens.color.Surface,
        tonalElevation = tokens.elevate.TopBar,
        shape = RoundedCornerShape(bottomStart = tokens.corner.Card, bottomEnd = tokens.corner.Card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = tokens.spacing.L),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "互助社区",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = tokens.type.Title,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.color.Neutral900
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onAsk,
                shape = RoundedCornerShape(tokens.corner.Button),
                border = BorderStroke(1.dp, tokens.color.Primary)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = tokens.color.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(tokens.spacing.S))
                Text("提问", color = tokens.color.Primary, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(tokens.spacing.M))
            FilledIconButton(onClick = onNotifications) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
        }
    }
}

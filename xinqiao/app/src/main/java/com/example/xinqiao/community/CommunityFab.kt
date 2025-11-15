package com.example.xinqiao.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun CommunityFab(
    onPost: () -> Unit,
    onMessages: () -> Unit
) {
    val tokens = CommunityTokensInstance
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)
            ) {
                SmallFloatingActionButton(
                    onClick = onMessages,
                    containerColor = tokens.color.Surface,
                    elevation = FloatingActionButtonDefaults.elevation(tokens.elevate.Card)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = tokens.color.Primary)
                }
                SmallFloatingActionButton(
                    onClick = onPost,
                    containerColor = tokens.color.Primary,
                    elevation = FloatingActionButtonDefaults.elevation(tokens.elevate.Card)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = tokens.color.Surface)
                }
            }
        }
        Spacer(modifier = Modifier.height(tokens.spacing.M))
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = tokens.color.Primary,
            elevation = FloatingActionButtonDefaults.elevation(tokens.elevate.Fab)
        ) {
            val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "fab")
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = null,
                tint = tokens.color.Surface,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

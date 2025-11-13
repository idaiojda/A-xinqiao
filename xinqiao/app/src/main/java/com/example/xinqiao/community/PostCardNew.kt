package com.example.xinqiao.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCardNew(
    post: ThemePost,
    comfortModeOn: Boolean,
    onLike: () -> Unit,
    onToggleComments: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenImage: (String) -> Unit,
    onRetrySync: () -> Unit,
    onBookmark: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val tokens = CommunityTokensInstance
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
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
            // Avatar Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(post.authorAvatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(tokens.spacing.S))
                Column {
                    Text(
                        text = post.author,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = post.time,
                        style = MaterialTheme.typography.bodySmall.copy(color = tokens.color.Neutral500)
                    )
                }
            }

            if (post.pendingSync) {
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = {}, label = { Text("未同步") })
                    TextButton(onClick = onRetrySync) { Text("重试同步") }
                }
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onOpenDetail() }
            )

            // Body
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = tokens.color.Neutral700),
                maxLines = if (comfortModeOn) 3 else 5,
                overflow = TextOverflow.Ellipsis
            )

            if (post.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                    items(post.tags.take(4)) { t ->
                        AssistChip(onClick = {}, label = { Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                    }
                }
            }

            // Images (max 3)
            if (post.images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                    items(post.images.take(3)) { url ->
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clickable { onOpenImage(url) }
                                .clip(RoundedCornerShape(tokens.corner.Button)),
                            contentScale = ContentScale.Crop,
                            colorFilter = if (comfortModeOn) ColorFilter.tint(tokens.color.Neutral500) else null
                        )
                    }
                }
            }

            // More menu
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { menuOpen = false; onEdit() }, leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) })
                }
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconToggleButton(checked = post.liked, onCheckedChange = { onLike() }) {
                        Icon(
                            imageVector = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                            contentDescription = null,
                            tint = if (post.liked) tokens.color.Danger else tokens.color.Neutral500
                        )
                    }
                    Text(post.likeCount.toString(), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onToggleComments) {
                    Icon(Icons.Outlined.Comment, contentDescription = null, tint = tokens.color.Neutral500)
                    Spacer(Modifier.width(4.dp))
                    Text(post.commentCount.toString())
                }
                IconToggleButton(checked = post.bookmarked, onCheckedChange = { onBookmark() }) {
                    Icon(
                        imageVector = if (post.bookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                        contentDescription = null,
                        tint = if (post.bookmarked) tokens.color.Primary else tokens.color.Neutral500
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = tokens.color.Neutral500)
                }
            }
        }
        }
        if (post.pendingSync) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp),
                shape = CircleShape,
                color = tokens.color.Danger,
                tonalElevation = 0.dp
            ) {}
        }
    }
}

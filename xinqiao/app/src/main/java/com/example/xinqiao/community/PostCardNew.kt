package com.example.xinqiao.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import com.example.xinqiao.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Base64

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
    val ctx = LocalContext.current
    val loginUserInitial = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx)
    var displayName by remember { mutableStateOf(if (post.author.equals("我", ignoreCase = true)) loginUserInitial else post.author) }
    var avatarUrl by remember { mutableStateOf(post.authorAvatar) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(post.author, post.isAnonymous, post.pendingSync) {
        if (!post.isAnonymous) {
            try {
                withContext(Dispatchers.IO) {
                    val loginUser = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx)
                    val lookupName = if (post.pendingSync) loginUser else if (post.author.equals("我", ignoreCase = true) || post.author.equals(loginUser, ignoreCase = true)) loginUser else post.author
                    val isSelf = post.pendingSync || post.author.equals("我", ignoreCase = true) || post.author.equals(loginUser, ignoreCase = true)
                    val profile = withTimeoutOrNull(1500L) { CommunityRepositoryProvider.current.getUserProfile(lookupName) }
                    if (profile != null) {
                        if (!profile.name.isNullOrBlank()) displayName = profile.name
                        val av = profile.avatar
                        if (!av.isNullOrBlank()) {
                            if (av.contains(",")) {
                                val b64 = av.substring(av.indexOf(',') + 1)
                                try { avatarBytes = Base64.decode(b64, Base64.DEFAULT) } catch (_: Exception) {}
                            } else {
                                avatarUrl = av
                            }
                        }
                    }
                    if (!avatarUrl.isNullOrBlank() && avatarBytes == null && avatarUrl!!.contains(",")) {
                        try {
                            val b64 = avatarUrl!!.substring(avatarUrl!!.indexOf(',') + 1)
                            avatarBytes = Base64.decode(b64, Base64.DEFAULT)
                        } catch (_: Exception) { }
                    }
                    if (avatarUrl.isNullOrEmpty() && avatarBytes == null) {
                        try {
                            val avLocal = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserAvatarPathByNameOrNickSync(if (isSelf) loginUser else lookupName)
                            if (!avLocal.isNullOrEmpty()) {
                                avatarUrl = avLocal
                                avatarBytes = null
                            }
                        } catch (_: Exception) { }
                    }
                    if (isSelf) {
                        try {
                            val nick = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNicknameSync(loginUser)
                            if (!nick.isNullOrBlank()) displayName = nick
                        } catch (_: Exception) { }
                    }
                }
            } catch (_: Exception) { }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            modifier = Modifier.fillMaxWidth()
        ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row with Avatar and Info - Simplified layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isAnonymousDisplay = post.isAnonymous || (post.author.equals(com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx), ignoreCase = true) && com.example.xinqiao.community.SettingsRepository.isAnonymous(ctx, com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx)))
                    val avatarPainter: androidx.compose.ui.graphics.painter.Painter = if (isAnonymousDisplay) {
                        painterResource(id = R.drawable.default_avatar)
                    } else {
                        if (avatarBytes != null) {
                            val bm = android.graphics.BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes!!.size)
                            androidx.compose.ui.graphics.painter.BitmapPainter(bm.asImageBitmap())
                        } else if (!avatarUrl.isNullOrEmpty()) {
                            if (avatarUrl!!.contains(",")) {
                                try {
                                    val b64 = avatarUrl!!.substring(avatarUrl!!.indexOf(',') + 1)
                                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                                    val bm = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    androidx.compose.ui.graphics.painter.BitmapPainter(bm.asImageBitmap())
                                } catch (_: Exception) {
                                    rememberAsyncImagePainter(avatarUrl)
                                }
                            } else {
                                rememberAsyncImagePainter(avatarUrl)
                            }
                        } else {
                            painterResource(id = R.drawable.default_avatar)
                        }
                    }
                    Image(
                        painter = avatarPainter,
                        contentDescription = if (isAnonymousDisplay) "匿名用户头像" else "用户头像",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isAnonymousDisplay) ctx.getString(R.string.anonymous_user) else displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = post.time,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // Options menu
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "更多选项",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { menuOpen = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = { menuOpen = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )
                    }
                }
            }

            if (post.pendingSync) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "未同步",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    TextButton(
                        onClick = onRetrySync,
                        modifier = Modifier.height(32.dp)
                    ) { 
                        Text(
                            "重试同步",
                            style = MaterialTheme.typography.labelMedium
                        ) 
                    }
                }
            }

            // Main content - simplified style matching image
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (post.content.isNotEmpty()) {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (post.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(post.tags.take(4)) { tag ->
                        Surface(
                            modifier = Modifier.padding(vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Images (max 3)
            if (post.images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(post.images.take(3)) { url ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(100.dp)
                                .clickable { onOpenImage(url) },
                            tonalElevation = 1.dp
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                colorFilter = if (comfortModeOn) ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) else null
                            )
                        }
                    }
                }
            }

            // Action Footer - Matching image layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side actions (Like, Comment) - Bottom left
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onLike,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                                contentDescription = "点赞",
                                tint = if (post.liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (post.likeCount > 0) {
                            Text(
                                text = post.likeCount.toString(), 
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (post.liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    
                    // Comment button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { onToggleComments() }
                    ) {
                        Icon(
                            Icons.Outlined.Comment, 
                            contentDescription = "评论",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        if (post.commentCount > 0) {
                            Text(
                                text = post.commentCount.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
                
                // Right side actions (Bookmark) - Bottom right
                IconToggleButton(
                    checked = post.bookmarked, 
                    onCheckedChange = { onBookmark() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (post.bookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                        contentDescription = "收藏",
                        tint = if (post.bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
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

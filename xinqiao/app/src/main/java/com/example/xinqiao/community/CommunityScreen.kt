package com.example.xinqiao.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
// stickyHeader 在当前依赖版本不可用，改用普通 item 头部
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged

/**
 * 心桥 · 互助社区主界面（Compose）
 * 参考 d:\A-xinqiao\xinqiao\community.html 与设计稿，实现主要UI骨架。
 */
// 色系（温暖治愈 · 莫兰迪）：主色与中性色统一
private val Primary = Color(0xFF7C4DFF)
private val PrimaryDark = Color(0xFF5E35B1)
private val Success = Color(0xFF00C853)
private val Danger = Color(0xFFFF3D00)
private val Neutral900 = Color(0xFF212121)
private val Neutral700 = Color(0xFF616161)
private val Neutral500 = Color(0xFF9E9E9E)
private val Neutral200 = Color(0xFFEEEEEE)
private val Neutral050 = Color(0xFFFAFAFA)
// 兼容旧命名（不改全局引用）
private val Mint = Success
private val ChipBg = Color(0xFFF5F5F5)
private val TextPrimary = Neutral900
private val TextSecondary = Neutral700
private val Bg = Neutral050
private val LavenderSoft = Color(0x1F7C4DFF)

sealed class CommunityUiState {
    object List : CommunityUiState()
    object AnonymousPost : CommunityUiState()
    data class Group(val name: String) : CommunityUiState()
    object CreateGroup : CommunityUiState()
    data class PostDetail(val post: ThemePost) : CommunityUiState()
}

data class Question(val id: String, val title: String, val content: String)
data class Comment(val id: String, val author: String, val text: String)

class CommunityController {
    var uiState: CommunityUiState by mutableStateOf(CommunityUiState.List)
    var selectedTab: Int by mutableStateOf(0)
    var searchText: String by mutableStateOf("")
    var comfortModeOn: Boolean by mutableStateOf(true)
    var showFabSheet: Boolean by mutableStateOf(false)
    var selectedCategoryIndex: Int by mutableStateOf(0)

    // 加载态与数据
    var isLoading: Boolean by mutableStateOf(false)
    var errorMessage: String? by mutableStateOf(null)
    var groups: List<String> by mutableStateOf(emptyList())
    // 简易返回栈以恢复上一个界面
    private val backStack = mutableStateListOf<CommunityUiState>()
    private fun navigate(to: CommunityUiState) {
        backStack.add(uiState)
        uiState = to
    }

    fun openAnonymousPost() { navigate(CommunityUiState.AnonymousPost) }
    fun openGroup(name: String) { navigate(CommunityUiState.Group(name)) }
    fun openCreateGroup() { navigate(CommunityUiState.CreateGroup) }
    fun openPost(post: ThemePost) { navigate(CommunityUiState.PostDetail(post)) }
    fun selectTab(index: Int) { selectedTab = index }
    fun updateSearch(text: String) { searchText = text }

    fun canGoBack(): Boolean = backStack.isNotEmpty()
    fun goBack() {
        if (backStack.isNotEmpty()) {
            uiState = backStack.removeAt(backStack.lastIndex)
        } else {
            uiState = CommunityUiState.List
        }
    }
    fun handleBackPressed(): Boolean { return if (canGoBack()) { goBack(); true } else false }

    fun toggleComfortMode() { comfortModeOn = !comfortModeOn }
    fun openFabSheet() { showFabSheet = true }
    fun closeFabSheet() { showFabSheet = false }
    fun selectCategory(index: Int) { selectedCategoryIndex = index }

    // 评论相关状态与方法（本地模拟，后续可接入真实数据）
    private val commentsByPost = androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Comment>>()
    private val commentsVisible = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()
    // 小组加入状态（本地记忆，用于展示“已加入/申请加入”）
    private val membership = androidx.compose.runtime.mutableStateMapOf<String, Boolean>()

    fun getComments(postId: String): List<Comment> = commentsByPost[postId] ?: emptyList()
    fun isCommentsVisible(postId: String): Boolean = commentsVisible[postId] == true
    fun toggleComments(postId: String) { commentsVisible[postId] = !(commentsVisible[postId] ?: false) }
    fun setComments(postId: String, list: List<Comment>) {
        val state = commentsByPost.getOrPut(postId) { androidx.compose.runtime.mutableStateListOf() }
        state.clear(); state.addAll(list)
    }

    fun addComment(postId: String, text: String, author: String = if (comfortModeOn) "匿名用户" else "我") {
        val list = commentsByPost.getOrPut(postId) { androidx.compose.runtime.mutableStateListOf() }
        val cid = "$postId-${System.currentTimeMillis()}"
        list.add(Comment(id = cid, author = author, text = text))
        // 后端接入：尝试发往仓库（成功后由远端持久化，当前本地列表已即时更新）
        kotlinx.coroutines.GlobalScope.launch {
            try {
                CommunityRepositoryProvider.current.postPostComment(postId, text, author)
            } catch (_: Exception) { /* 保持本地回退，不影响交互 */ }
        }
    }

    fun isJoined(groupName: String): Boolean = membership[groupName] == true
    fun setJoined(groupName: String, joined: Boolean) { membership[groupName] = joined }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(controller: CommunityController) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    // 主题交流区帖子状态（放在顶层，供外层 LazyColumn 直接渲染 items）
    var feedLoadingMore by remember { mutableStateOf(false) }
    // 终止标记：当加载更多失败或无更多数据时，避免重复触发失败提示
    var feedReachedEnd by remember { mutableStateOf(false) }
    var feedPage by remember { mutableStateOf(0) }
    val feedPosts = remember { androidx.compose.runtime.mutableStateListOf<ThemePost>() }
    // 分类标签，首项为“全部话题”以匹配设计图
    val categoryNames = listOf("全部话题", "夜间情绪", "社交与关系", "学习与考试", "睡眠", "自我关怀", "呼吸练习")
    Box(modifier = Modifier.fillMaxSize()) {
        // 当 Tab 或搜索词变化时，触发数据加载（移除专家问答相关加载）
        LaunchedEffect(controller.selectedTab, controller.searchText, controller.uiState) {
            if (controller.uiState is CommunityUiState.List) {
                controller.isLoading = true
                controller.errorMessage = null
                try {
                    when (controller.selectedTab) {
                        0 -> { // 互助小组
                            controller.groups = CommunityRepositoryProvider.current.getGroups()
                        }
                        else -> { /* 其它 Tab 当前为占位，无需远程加载 */ }
                    }
                } catch (e: Exception) {
                    controller.errorMessage = "加载失败，请稍后重试"
                } finally {
                    controller.isLoading = false
                }
            }
        }
        // 首次与切换分类/安心模式时加载帖子流
        LaunchedEffect(controller.uiState, controller.selectedCategoryIndex, controller.comfortModeOn) {
            if (controller.uiState is CommunityUiState.List) {
                // 返回列表时，重置加载更多状态，避免界面错乱或占位骨架残留
                feedLoadingMore = false
                feedReachedEnd = false
                val selectedCategoryRaw = categoryNames.getOrNull(controller.selectedCategoryIndex)
                val selectedCategory = if (controller.selectedCategoryIndex == 0) null else selectedCategoryRaw
                try {
                    feedPosts.clear()
                    feedPage = 0
                    val items = CommunityRepositoryProvider.current.getPosts(category = selectedCategory, page = 0, size = 10)
                    val adjusted = items.map { if (controller.comfortModeOn) it.copy(isAnonymous = true) else it }
                    feedPosts.addAll(adjusted)
                } catch (e: Exception) {
                    controller.errorMessage = "帖子加载失败，请稍后重试"
                }
            }
        }
        when (val state = controller.uiState) {
            is CommunityUiState.List -> {
                // 统一用列表状态驱动“加载更多”，避免在 item 组合期间更改数据导致测量错误
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        feedPosts.isNotEmpty() && !feedLoadingMore && lastVisibleIndex >= feedPosts.lastIndex - 1
                    }
                }
                LaunchedEffect(shouldLoadMore, controller.uiState) {
                    if (controller.uiState is CommunityUiState.List && shouldLoadMore && !feedReachedEnd) {
                        feedLoadingMore = true
                        val selectedCategoryRaw = categoryNames.getOrNull(controller.selectedCategoryIndex)
                        val selectedCategory = if (controller.selectedCategoryIndex == 0) null else selectedCategoryRaw
                        try {
                            val more = CommunityRepositoryProvider.current.getPosts(category = selectedCategory, page = feedPage + 1, size = 10)
                            if (more.isEmpty()) {
                                // 无更多内容：标记终止，避免重复触发
                                feedReachedEnd = true
                                snackbarHostState.showSnackbar("暂无更多内容")
                            } else {
                                val adjusted = more.map { if (controller.comfortModeOn) it.copy(isAnonymous = true) else it }
                                feedPosts.addAll(adjusted)
                                feedPage += 1
                            }
                        } catch (e: Exception) {
                            // 异常：标记终止，避免重复触发失败提示
                            feedReachedEnd = true
                            snackbarHostState.showSnackbar("加载更多失败")
                        } finally {
                            feedLoadingMore = false
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Bg)
                        .padding(12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Surface(
                            color = Color.White,
                            shadowElevation = 2.dp,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 0.dp, vertical = 16.dp)
                            ) {
                                CommunityTopBar(controller)
                                Spacer(modifier = Modifier.height(16.dp))
                                CommunitySearchBar(controller)
                                Spacer(modifier = Modifier.height(16.dp))
                                // 去掉顶部Tab，保留分类标签行
                                CategoryChipsRow(categories = categoryNames, controller = controller)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    item {
                        // 顶部加载/错误提示
                        if (controller.isLoading) {
                            Text(text = "加载中…", fontSize = 12.sp, color = Color(0xFF86909C))
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (controller.errorMessage != null) {
                            Text(text = controller.errorMessage ?: "", fontSize = 12.sp, color = Color(0xFFD93025))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    when (controller.selectedTab) {
                        0 -> { // 互助小组（按设计图：挑战卡 + 推荐小组 + 我的小组 + 帖子列表）
                            item { SectionTitle(title = "互助小组") }
                            item { EmotionChallengeCard() }
                            item { RecommendedGroupCard(controller) }
                            item { MyGroupsTimeline(controller) }
                            item { HotTopicCard() }
                            // 将帖子展开为外层 LazyColumn 的 items，避免内层 LazyColumn 嵌套
                            itemsIndexed(feedPosts, key = { _, item -> item.id }) { index, post ->
                                TopicFeedCard(
                                    post = post,
                                    controller = controller,
                                    comfortModeOn = controller.comfortModeOn,
                                    onActionFeedback = { msg ->
                                        kotlinx.coroutines.GlobalScope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            if (feedLoadingMore) {
                                item { SkeletonPostCard() }
                                item { SkeletonPostCard() }
                            }
                            item { CenteredAnonymousPublishButton(controller) }
                        }
                        1 -> { // 科普墙
                            item { SectionTitle(title = "科普墙") }
                            item { ScienceWallPlaceholder() }
                        }
                        2 -> { // 热榜
                            item { SectionTitle(title = "今日热榜") }
                            item { HotTopicCard() }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
            is CommunityUiState.AnonymousPost -> {
                AnonymousPostScreen(
                    onBack = { controller.goBack() }
                )
            }
            is CommunityUiState.Group -> {
                GroupDetailScreen(name = state.name, controller = controller, onBack = { controller.goBack() })
            }
            is CommunityUiState.CreateGroup -> {
                CreateGroupScreen(controller = controller, onBack = { controller.goBack() })
            }
            is CommunityUiState.PostDetail -> {
                PostDetailScreen(post = state.post, controller = controller, onBack = { controller.goBack() })
            }
        }
        

        // Snackbar 宿主（全局操作反馈）
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 88.dp)
        )

        // 底部发布面板
        if (controller.showFabSheet) {
            ModalBottomSheet(onDismissRequest = { controller.closeFabSheet() }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "选择发布方式", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            controller.closeFabSheet()
                            controller.openAnonymousPost()
                        }
                        .padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = "匿名帖子", tint = Color(0xFF3D8BFF))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "匿名发布", fontSize = 14.sp, color = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 图文发布占位：后续接入
                            controller.closeFabSheet()
                            kotlinx.coroutines.GlobalScope.launch {
                                snackbarHostState.showSnackbar("图文发布 · 敬请期待")
                            }
                        }
                        .padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "图文发布", tint = Color(0xFF4BB8A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "图文发布", fontSize = 14.sp, color = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            controller.closeFabSheet()
                            kotlinx.coroutines.GlobalScope.launch {
                                snackbarHostState.showSnackbar("语音发布 · 敬请期待")
                            }
                        }
                        .padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Headset, contentDescription = "语音发布", tint = Color(0xFFFFC53D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "语音发布", fontSize = 14.sp, color = TextPrimary)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // 已替换为列表底部的居中“匿名发布”按钮
    }
}

@Composable
private fun CategoryChipsRow(categories: List<String>, controller: CommunityController) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.forEachIndexed { index, t ->
            val selected = controller.selectedCategoryIndex == index
            val scale by animateFloatAsState(
                if (selected) 1.02f else 1.0f,
                animationSpec = tween(180),
                label = "chip-scale"
            )
            val gradient = if (selected) Brush.horizontalGradient(
                colors = listOf(Primary, PrimaryDark)
            ) else null
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { controller.selectCategory(index) },
                color = Color.Transparent,
                shadowElevation = if (selected) 2.dp else 0.dp,
                border = if (selected) null else BorderStroke(1.dp, Neutral200)
            ) {
                val chipModifier = if (gradient != null) {
                    Modifier.background(gradient, RoundedCornerShape(20.dp))
                } else {
                    Modifier.background(ChipBg, RoundedCornerShape(20.dp))
                }
                Box(modifier = chipModifier) {
                    Text(
                        text = t,
                        fontSize = 13.sp,
                        color = if (selected) Color.White else Neutral700,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}



// 旧版 TopicFeedList 已移除，避免引入内层 LazyColumn 导致嵌套滚动问题。

@Composable
private fun EmptyStateCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ImageGrid(count: Int) {
    val cols = if (count == 1) 1 else 3
    val rows = kotlin.math.ceil(count / cols.toFloat()).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(rows) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(cols) { c ->
                    val index = r * cols + c
                    if (index < count) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (cols == 1) 140.dp else 80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F3FF))
                        ) {
                            // 占位预览块（后续可接入图片库）
                            Text(text = "图片预览", color = Color(0xFF3D8BFF), fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsSection(postId: String, controller: CommunityController, onAdded: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val comments = controller.getComments(postId)
        // 展开时尝试从仓库拉取真实评论列表（若失败则保留本地状态）
        androidx.compose.runtime.LaunchedEffect(postId) {
            try {
                val remote = CommunityRepositoryProvider.current.getPostComments(postId)
                controller.setComments(postId, remote)
            } catch (_: Exception) { /* 使用已有本地状态 */ }
        }
        if (comments.isEmpty()) {
            Text(text = "暂无评论，快来抢沙发～", fontSize = 12.sp, color = TextSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comments.forEach { c ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Surface(shape = CircleShape, color = Color(0xFFF5F7FA)) {
                            Text(text = c.author.take(1), color = Color(0xFF86909C), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        Column {
                            Text(text = c.author, fontSize = 12.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = c.text, fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
        var input by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(text = "写下你的想法…", fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val canSend = input.isNotBlank()
            Surface(shape = RoundedCornerShape(16.dp), color = if (canSend) Mint else Color(0xFFE5E6EB), modifier = Modifier.clickable(enabled = canSend) {
                controller.addComment(postId, input.trim())
                input = ""
                onAdded()
            }) {
                Text(text = "发送", color = if (canSend) Color.White else Color(0xFF86909C), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun VoicePlayerBar(durationSec: Int, listenCount: Int = 0) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF5F7FA)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Headset, contentDescription = "收听语音", tint = Color(0xFF3D8BFF))
            // 简化的波形占位条
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                val bars = listOf(12, 18, 10, 16, 8, 14, 20, 12, 18)
                bars.forEach { h ->
                    Box(modifier = Modifier.width(3.dp).height(h.dp).background(Color(0xFF3D8BFF), RoundedCornerShape(2.dp)))
                }
            }
            val m = durationSec / 60
            val s = durationSec % 60
            Text(text = String.format("%02d'%02d", m, s), fontSize = 12.sp, color = TextSecondary)
            if (listenCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, ChipBg)) {
                    Text(text = "立即收听", fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonPostCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "skeleton-alpha"
    )
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.3f).background(Color(0xFFE5E6EB).copy(alpha = alpha), RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.height(14.dp).fillMaxWidth().background(Color(0xFFE5E6EB).copy(alpha = alpha), RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.height(80.dp).fillMaxWidth().background(Color(0xFFE5E6EB).copy(alpha = alpha), RoundedCornerShape(12.dp)))
        }
    }
}

@Composable
private fun TopicFeedCard(post: ThemePost, controller: CommunityController, comfortModeOn: Boolean, onActionFeedback: (String) -> Unit) {
    var liked by remember { mutableStateOf(false) }
    var collected by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val base = kotlin.math.abs(post.id.hashCode())
    var likesCount by remember { mutableStateOf(base % 200 + 20) }
    var collectsCount by remember { mutableStateOf(base % 80 + 6) }
    var repliesCount by remember { mutableStateOf(base % 60 + 5) }
    val likeScale by animateFloatAsState(if (liked) 1.1f else 1.0f, animationSpec = tween(160), label = "like-scale")
    val collectScale by animateFloatAsState(if (collected) 1.1f else 1.0f, animationSpec = tween(160), label = "collect-scale")

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarText = if (comfortModeOn || post.isAnonymous) "匿" else post.author.take(1)
                Surface(shape = CircleShape, color = if (comfortModeOn || post.isAnonymous) Color(0xFFE8F3FF) else Mint) {
                    Text(text = avatarText, color = if (comfortModeOn || post.isAnonymous) Color(0xFF3D8BFF) else Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                val primaryTag = post.tags.firstOrNull() ?: "互助"
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${if (comfortModeOn || post.isAnonymous) "匿名用户" else post.author} · $primaryTag", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(text = "${post.time} · 回复 ${repliesCount} 条", fontSize = 12.sp, color = TextSecondary)
                }
                // 顶部右侧分类标签（取第二个标签作为补充展示）
                post.tags.drop(1).firstOrNull()?.let { tag ->
                    Surface(shape = RoundedCornerShape(16.dp), color = LavenderSoft) {
                        Text(text = tag, fontSize = 12.sp, color = Color(0xFF6B6F7B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (post.voiceDurationSec == null) {
                Text(text = post.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (!expanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "更多…", fontSize = 12.sp, color = Mint, modifier = Modifier.clickable { expanded = true })
                }
                // 图片网格展示
                if (post.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ImageGrid(count = post.images.size)
                }
            }
            // 语音分享头部 + 波形展示
            post.voiceDurationSec?.let { dur ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val primaryTag = post.tags.firstOrNull() ?: "互助"
                    Text(text = "语音分享 · $primaryTag", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    val m = dur / 60
                    val s = dur % 60
                    Surface(shape = RoundedCornerShape(16.dp), color = ChipBg) {
                        Text(text = String.format("%02d'%02d", m, s), fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val listens = kotlin.math.abs(post.id.hashCode()) % 120 + 20
                VoicePlayerBar(durationSec = dur, listenCount = listens)
            }
            // 评论区（点击回复按钮展开/收起）
            if (controller.isCommentsVisible(post.id)) {
                Spacer(modifier = Modifier.height(8.dp))
                CommentsSection(postId = post.id, controller = controller, onAdded = { repliesCount += 1 })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.tags.forEach { tag ->
                    Surface(shape = RoundedCornerShape(16.dp), color = LavenderSoft) {
                        Text(text = "# $tag", fontSize = 12.sp, color = Color(0xFF6B6F7B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    liked = !liked
                    likesCount = (if (liked) likesCount + 1 else likesCount - 1).coerceAtLeast(0)
                    onActionFeedback(if (liked) "已点赞" else "已取消点赞")
                }) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = "点赞", tint = if (liked) Mint else Color(0xFF4E5969), modifier = Modifier.size(18.dp).scale(likeScale))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = likesCount.toString(), fontSize = 12.sp, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    collected = !collected
                    collectsCount = (if (collected) collectsCount + 1 else collectsCount - 1).coerceAtLeast(0)
                    onActionFeedback(if (collected) "已收藏" else "已取消收藏")
                }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "收藏", tint = if (collected) Color(0xFFFF6B6B) else Color(0xFF4E5969), modifier = Modifier.size(18.dp).scale(collectScale))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = collectsCount.toString(), fontSize = 12.sp, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    controller.toggleComments(post.id)
                }) {
                    Icon(Icons.Filled.Chat, contentDescription = "回复", tint = Color(0xFF4E5969), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = repliesCount.toString(), fontSize = 12.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "查看详情", fontSize = 12.sp, color = Mint, modifier = Modifier.clickable {
                    controller.openPost(post)
                })
            }
        }
    }
}

@Composable
private fun CenteredAnonymousPublishButton(controller: CommunityController) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = tween(120), label = "center-publish-scale")
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Mint,
            modifier = Modifier
                .scale(scale)
                .clickable(interactionSource = interaction, indication = null) { controller.openFabSheet() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "匿名发布", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AnimatedSection(delayMs: Int = 0, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(200), initialOffsetY = { it / 10 })
    ) {
        content()
    }
}

@Composable
private fun CommunityTopBar(controller: CommunityController) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "互助社区",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = Color(0xFFF5F0FF),
            shadowElevation = 0.dp,
            modifier = Modifier
                .clickable { controller.toggleComfortMode() }
                .semantics { contentDescription = "安心模式切换" }
        ) {
            Text(
                text = if (controller.comfortModeOn) "安心模式 ON" else "安心模式 OFF",
                color = PrimaryDark,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunitySearchBar(controller: CommunityController) {
    var value by remember { mutableStateOf(TextFieldValue(controller.searchText)) }
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) Primary else Color(0xFFEAEAEA)
    val iconTint = if (focused) Primary else Neutral700
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = if (focused) 4.dp else 1.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = "搜索", tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it; controller.updateSearch(it.text) },
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = Neutral700,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 16.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (value.text.isEmpty()) {
                            val placeholder = when (controller.selectedTab) {
                                0 -> "搜索话题、关键词…"
                                1 -> "搜索科普文章"
                                else -> "孤独感、考试焦虑、社交恐惧、恋爱关系 ……"
                            }
                            Text(
                                text = placeholder,
                                fontSize = 13.sp,
                                color = Neutral700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        inner()
                    }
                )
            }
            if (value.text.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "清除",
                    tint = iconTint,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { value = TextFieldValue(""); controller.updateSearch("") }
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Filled.Headset, contentDescription = null, tint = iconTint)
        }
    }
}

@Composable
private fun CommunityTabs(controller: CommunityController) {
    val tabs = listOf("互助小组", "科普墙", "热榜")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEachIndexed { index, t ->
            AssistChip(
                onClick = { controller.selectTab(index) },
                label = { Text(text = t, fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (controller.selectedTab == index) Mint else ChipBg,
                    labelColor = if (controller.selectedTab == index) Color.White else TextPrimary
                )
            )
        }
    }
}

@Composable
private fun HotTopicCard() {
    val gradient = Brush.horizontalGradient(colors = listOf(Color(0x1F4BB8A4), LavenderSoft))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "今日热门话题", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.Transparent, border = BorderStroke(1.dp, Mint), modifier = Modifier.animateContentSize()) {
                            Text(text = "参与讨论", color = Mint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = LavenderSoft) {
                        Text(text = "# 失眠困扰", fontSize = 12.sp, color = Color(0xFF645D89), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "考试焦虑如何缓解？匿名分享你的经验。", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun HealthCheckCard() {
    var statusText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val h = CommunityRepositoryProvider.current.health()
            statusText = if (h.ok) {
                "服务正常 · ${h.message}"
            } else {
                "服务异常 · ${h.message}"
            }
        } catch (e: Exception) {
            errorText = "健康检查失败：" + (e.message ?: "网络异常")
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "社区服务健康状态", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            when {
                errorText != null -> Text(text = errorText ?: "", fontSize = 13.sp, color = Color(0xFFD93025))
                statusText != null -> Text(text = statusText ?: "", fontSize = 13.sp, color = Color(0xFF4E5969))
                else -> Text(text = "检查中…", fontSize = 13.sp, color = Color(0xFF86909C))
            }
        }
    }
}

@Composable
private fun EmotionChallengeCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "21天情绪记录挑战", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            val target = 12f / 21f
            val progress by animateFloatAsState(targetValue = target, animationSpec = tween(durationMillis = 1200))
            // 渐变进度条（从主色到深主色）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Neutral200)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryDark)))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "已坚持 12/21 天，继续记录以获得徽章", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary,
                    modifier = Modifier
                        .scale(if (pressed) 1.05f else 1f)
                        .clickable(interactionSource = interaction, indication = null) { /* TODO: 跳转记录页面 */ }
                ) {
                    Text(text = "继续记录", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RecommendedGroupCard(controller: CommunityController) {
    var applyMessage by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadErr by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try {
            groups = CommunityRepositoryProvider.current.getGroups()
        } catch (e: Exception) {
            loadErr = "加载推荐小组失败：" + (e.message ?: "网络异常")
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val backendGroupName = groups.firstOrNull()
            Text(text = if (backendGroupName != null) "推荐加入：$backendGroupName" else "暂无推荐小组", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "每日 10 分钟，科学缓解考试与工作压力", fontSize = 13.sp, color = Color(0xFF888888))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val applyInteraction = remember { MutableInteractionSource() }
                val applyPressed by applyInteraction.collectIsPressedAsState()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary,
                    modifier = Modifier
                        .scale(if (applyPressed) 1.05f else 1f)
                        .clickable(interactionSource = applyInteraction, indication = null, enabled = !isApplying && backendGroupName != null) {
                    if (isApplying) return@clickable
                    scope.launch {
                        isApplying = true
                        try {
                            val name = backendGroupName ?: return@launch
                            val res = CommunityRepositoryProvider.current.applyJoin(name)
                            applyMessage = if (res.accepted) {
                                "已申请成功：${res.message}"
                            } else {
                                "申请未通过：${res.message}"
                            }
                            if (res.accepted) controller.setJoined(name, true)
                        } catch (e: Exception) {
                            applyMessage = "申请失败：" + (e.message ?: "网络异常")
                        } finally {
                            isApplying = false
                        }
                    }
                }) {
                    Text(text = if (isApplying) "申请中…" else "申请加入", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
                }
                val introInteraction = remember { MutableInteractionSource() }
                val introPressed by introInteraction.collectIsPressedAsState()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    modifier = Modifier
                        .scale(if (introPressed) 1.05f else 1f)
                        .border(BorderStroke(1.dp, Primary), RoundedCornerShape(8.dp))
                        .clickable(interactionSource = introInteraction, indication = null, enabled = backendGroupName != null) {
                    val name = backendGroupName ?: return@clickable
                    controller.openGroup(name)
                }) {
                    Text(text = "查看介绍", color = Primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
                }
            }
            if (loadErr != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = loadErr ?: "", fontSize = 12.sp, color = Color(0xFFD93025))
            }
            if (applyMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = applyMessage ?: "", fontSize = 12.sp, color = Color(0xFF4E5969))
            }
        }
    }
}

@Composable
private fun MyGroupsTimeline(controller: CommunityController) {
    val items = remember { mutableStateListOf<TimelineItem>() }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            items.clear()
            items.addAll(CommunityRepositoryProvider.current.getMyTimeline())
        } catch (e: Exception) {
            error = "时间线加载失败：" + (e.message ?: "网络异常")
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "我的小组", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Surface(shape = RoundedCornerShape(8.dp), color = Primary, modifier = Modifier.clickable { controller.openCreateGroup() }) {
                    Text(text = "创建小组", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp)
                }
            }
            Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            if (error != null) {
                Text(text = error ?: "", fontSize = 12.sp, color = Color(0xFFD93025))
                Spacer(modifier = Modifier.height(12.dp))
            }
            items.forEach { item ->
                val emoji = when (item.type) {
                    "checkin" -> "🧘"
                    "share" -> "🎵"
                    "badge" -> "🏅"
                    else -> "•"
                }
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (pressed) Color(0xFFF9F9F9) else Color.Transparent)
                        .clickable(interactionSource = interaction, indication = null) {}
                ) {
                    Text(text = emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item.text, fontSize = 13.sp, color = Color(0xFF555555))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AccentAskCard(onGoAsk: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "公开提问", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Surface(shape = RoundedCornerShape(16.dp), color = Mint, modifier = Modifier.clickable { onGoAsk() }) {
                    Text(text = "去提问", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = ChipBg) { Text(text = "匿名", color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp) }
                Surface(shape = RoundedCornerShape(12.dp), color = ChipBg) { Text(text = "AI 辅助", color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp) }
                Surface(shape = RoundedCornerShape(12.dp), color = ChipBg) { Text(text = "专家认证", color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp) }
            }
        }
    }
}


@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1D2129),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun GroupCardsRow(groups: List<String>, onGroupClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(groups.size) { idx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onGroupClick(groups[idx]) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = groups[idx], fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "活跃度较高 · 每日更新", fontSize = 12.sp, color = Color(0xFF86909C))
                }
            }
        }
    }
}


// Java桥接入口，便于在 View 中调用
@Composable
fun CommunityScreenEntry(controller: CommunityController) {
    CommunityScreenNew(controller)
}


@Composable
fun AnonymousPostScreen(onBack: () -> Unit) {
    var content by remember { mutableStateOf("") }
    var submitError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(12.dp)
    ) {
        // 顶部返回与标题
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF4E5969),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "匿名发布",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D2129))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "写下你的困扰（匿名）", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = TextFieldValue(content),
                    onValueChange = { content = it.text },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(1.dp, Color(0xFFE5E6EB), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF4E5969))
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (submitError != null) {
                    Text(text = submitError ?: "", fontSize = 12.sp, color = Color(0xFFD93025))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF3D8BFF)) {
                        Text(
                            text = "提交",
                            color = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable(enabled = !submitting) {
                                    scope.launch {
                                        submitting = true
                                        submitError = null
                                        val title = if (content.isNotBlank()) content.take(20) else "匿名提问"
                                        try {
                                            CommunityRepositoryProvider.current.createQuestion(title, content)
                                            onBack()
                                        } catch (e: Exception) {
                                            submitError = "提交失败：" + (e.message ?: "网络异常")
                                        } finally {
                                            submitting = false
                                        }
                                    }
                                },
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostDetailScreen(post: ThemePost, controller: CommunityController, onBack: () -> Unit) {
    var recommendedGroup by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val groups = CommunityRepositoryProvider.current.getGroups()
            recommendedGroup = groups.firstOrNull() ?: "考研互助小组"
        } catch (e: Exception) {
            loadError = "推荐小组加载失败"
            recommendedGroup = "考研互助小组"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF4E5969),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "帖子详情", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D2129)))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 头部作者与时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val avatarText = if (controller.comfortModeOn || post.isAnonymous) "匿" else post.author.take(1)
                    Surface(shape = CircleShape, color = if (controller.comfortModeOn || post.isAnonymous) Color(0xFFE8F3FF) else Mint) {
                        Text(text = avatarText, color = if (controller.comfortModeOn || post.isAnonymous) Color(0xFF3D8BFF) else Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (controller.comfortModeOn || post.isAnonymous) "匿名用户" else post.author, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(text = post.time, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = post.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                if (post.voiceDurationSec == null) {
                    Text(text = post.content, fontSize = 14.sp, color = TextSecondary)
                    if (post.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ImageGrid(count = post.images.size)
                    }
                } else {
                    // 语音帖子
                    val dur = post.voiceDurationSec ?: 0
                    VoicePlayerBar(durationSec = dur, listenCount = kotlin.math.abs(post.id.hashCode()) % 120 + 20)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    post.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(16.dp), color = LavenderSoft) {
                            Text(text = "# $tag", fontSize = 12.sp, color = Color(0xFF6B6F7B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "评论", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                CommentsSection(postId = post.id, controller = controller, onAdded = { /* 详情页直接刷新本地 */ })

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Mint,
                        modifier = Modifier.clickable {
                            val target = recommendedGroup ?: "考研互助小组"
                            controller.openGroup(target)
                        }
                    ) { Text(text = "加入小组", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp) }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF3D8BFF),
                        modifier = Modifier.clickable {
                            controller.openAnonymousPost()
                        }
                    ) { Text(text = "匿名发布", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp) }
                }
                if (loadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = loadError ?: "", fontSize = 12.sp, color = Color(0xFFD93025))
                }
            }
        }
    }
}

@Composable
fun GroupDetailScreen(name: String, controller: CommunityController, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(12.dp)
    ) {
        val scope = rememberCoroutineScope()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF4E5969),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = name, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D2129)))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                var applyMessage by remember { mutableStateOf<String?>(null) }
                var isApplying by remember { mutableStateOf(false) }
                val joined = controller.isJoined(name)
                Text(text = "本小组主题：缓解压力 · 同伴支持 · 每周主题分享", fontSize = 14.sp, color = Color(0xFF4E5969))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = ChipBg) {
                        Text(text = "主持人：心理咨询师 · 李老师", color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = ChipBg) {
                        Text(text = "每周三 20:00 · 主题分享", color = TextPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 人数进度
                Text(text = "当前成员：24 / 30", fontSize = 12.sp, color = TextSecondary)
                val progress by animateFloatAsState(targetValue = 24f / 30f, animationSpec = tween(durationMillis = 800))
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), color = Mint)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (joined) Color(0xFF52C41A) else Mint,
                        modifier = Modifier.clickable(enabled = !isApplying && !joined) {
                            if (joined) return@clickable
                            if (isApplying) return@clickable
                            scope.launch {
                                isApplying = true
                                try {
                                    val res = CommunityRepositoryProvider.current.applyJoin(name)
                                    applyMessage = res.message
                                    if (res.accepted) controller.setJoined(name, true)
                                } catch (e: Exception) {
                                    applyMessage = "申请失败：" + (e.message ?: "网络异常")
                                } finally {
                                    isApplying = false
                                }
                            }
                        }
                    ) {
                        Text(text = if (joined) "已加入" else if (isApplying) "申请中…" else "申请加入", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF3D8BFF), modifier = Modifier.clickable {
                        // 直接进入匿名发布，返回可回到当前小组详情
                        controller.openAnonymousPost()
                    }) {
                        Text(text = "匿名发布", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
                    }
                }
                if (applyMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = applyMessage ?: "", fontSize = 12.sp, color = Color(0xFF4E5969))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "近期活动：周三 20:00 · 主题分享与互助记录", fontSize = 13.sp, color = Color(0xFF86909C))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "小组介绍：我们关注考试与工作压力，通过同伴支持与科学方法，帮助组员建立情绪调节与放松习惯。", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun CreateGroupButton(controller: CommunityController) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF3D8BFF), modifier = Modifier.clickable {
            controller.openCreateGroup()
        }) {
            Text(text = "+ 创建小组", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
        }
    }
}

// 问答榜相关内容已移除

@Composable
private fun ScienceWallPlaceholder() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "科普墙占位：后续接入科普文章", fontSize = 14.sp, color = Color(0xFF4E5969))
        }
    }
}
@Composable
fun CreateGroupScreen(controller: CommunityController, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("每周三 20:00") }
    var capacityText by remember { mutableStateOf("30") }
    var submitting by remember { mutableStateOf(false) }
    var submitMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF4E5969),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "创建小组", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D2129)))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "小组名称", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(value = TextFieldValue(name), onValueChange = { name = it.text }, modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E6EB), RoundedCornerShape(8.dp))
                    .padding(8.dp), textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary))

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "小组介绍", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(value = TextFieldValue(description), onValueChange = { description = it.text }, modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, Color(0xFFE5E6EB), RoundedCornerShape(8.dp))
                    .padding(8.dp), textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary))

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "活动时间（示例：每周三 20:00）", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(value = TextFieldValue(schedule), onValueChange = { schedule = it.text }, modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E6EB), RoundedCornerShape(8.dp))
                    .padding(8.dp), textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary))

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "人数上限（示例：30）", fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(value = TextFieldValue(capacityText), onValueChange = { capacityText = it.text.filter { ch -> ch.isDigit() } }, modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E6EB), RoundedCornerShape(8.dp))
                    .padding(8.dp), textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary))

                if (submitMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = submitMessage ?: "", fontSize = 12.sp, color = Color(0xFF4E5969))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF3D8BFF)) {
                        Text(text = if (submitting) "创建中…" else "创建", color = Color.White, modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable(enabled = !submitting) {
                                val cap = capacityText.toIntOrNull() ?: 30
                                scope.launch {
                                    submitting = true
                                    try {
                                        val res = CommunityRepositoryProvider.current.createGroup(name.trim(), description.trim(), schedule.trim(), cap)
                                        submitMessage = res.message
                                        if (res.ok) {
                                            // 立即更新本地状态：标记已加入并返回列表
                                            controller.setJoined(name.trim(), true)
                                            onBack()
                                        }
                                    } catch (e: Exception) {
                                        submitMessage = "创建失败：" + (e.message ?: "网络异常")
                                    } finally {
                                        submitting = false
                                    }
                                }
                            }, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

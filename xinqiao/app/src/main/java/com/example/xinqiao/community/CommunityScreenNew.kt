package com.example.xinqiao.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.*
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreenNew(controller: CommunityController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val tokens = CommunityTokensInstance

    // 帖子流状态
    var feedPage by remember { mutableStateOf(0) }
    var feedReachedEnd by remember { mutableStateOf(false) }
    var feedLoadingMore by remember { mutableStateOf(false) }
    val feedPosts = remember { mutableStateListOf<ThemePost>() }

    val categoryNames = listOf("全部话题", "夜间情绪", "社交与关系", "学习与考试", "睡眠", "自我关怀", "呼吸练习", "我的收藏")

    var selectedPostForComments by remember { mutableStateOf<ThemePost?>(null) }
    var selectedPostDetail by remember { mutableStateOf<ThemePost?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentsLoading by remember { mutableStateOf(false) }
    var commentsInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var detailComments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailInput by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var createTitle by remember { mutableStateOf("") }
    var createContent by remember { mutableStateOf("") }
    var createTags by remember { mutableStateOf("") }
    var createAnonymous by remember { mutableStateOf(false) }
    val createImages = remember { mutableStateListOf<String>() }
    val pickImagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        createImages.addAll(uris.map { it.toString() })
    }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    var notificationsOpen by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var notificationsLoading by remember { mutableStateOf(false) }
    var askOpen by remember { mutableStateOf(false) }
    var askTitle by remember { mutableStateOf("") }
    var askContent by remember { mutableStateOf("") }
    var searchTabIndex by remember { mutableStateOf(0) }
    val searchTabs = listOf("帖子", "用户", "小组")
    var searchGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchGroupsLoading by remember { mutableStateOf(false) }
    var selectedUserName by remember { mutableStateOf<String?>(null) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedUserFavorites by remember { mutableStateOf<List<ThemePost>>(emptyList()) }
    var selectedUserSharedGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedGroupIntro by remember { mutableStateOf<String?>(null) }
    var selectedGroupInfo by remember { mutableStateOf<GroupInfo?>(null) }
    var editPost by remember { mutableStateOf<ThemePost?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }

    // 加载帖子
    LaunchedEffect(controller.selectedTab, controller.searchText, controller.selectedCategoryIndex) {
        if (controller.uiState is CommunityUiState.List) {
            controller.isLoading = true
            controller.errorMessage = null
            try {
                try {
                    val cached = CommunityLocalCache.database()?.postDao()?.getAll()?.map { it.toThemePost() } ?: emptyList()
                    if (cached.isNotEmpty()) {
                        feedPosts.clear()
                        feedPosts.addAll(cached)
                    }
                } catch (_: Exception) {}
                val list = CommunityRepositoryProvider.current.getPosts(
                    page = 0,
                    size = 10,
                    category = if (controller.selectedCategoryIndex == 0) null else categoryNames[controller.selectedCategoryIndex]
                )
                feedPosts.clear()
                feedPosts.addAll(list)
                feedPage = 0
                feedReachedEnd = list.isEmpty()
            } catch (e: Exception) {
                controller.errorMessage = "加载失败：" + (e.message ?: "网络异常")
            } finally {
                controller.isLoading = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        val displayPosts by remember(feedPosts, controller.searchText, controller.selectedCategoryIndex) {
            mutableStateOf(
                if (controller.searchText.isNotBlank()) {
                    val q = controller.searchText
                    feedPosts.filter { it.title.contains(q) || it.content.contains(q) || it.tags.any { t -> t.contains(q) } }
                } else if (controller.selectedCategoryIndex == categoryNames.lastIndex) {
                    feedPosts.filter { it.bookmarked }
                } else feedPosts
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.color.Neutral050)
                .padding(horizontal = tokens.spacing.L),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.L)
        ) {
            item { Spacer(modifier = Modifier.height(tokens.spacing.S)) }
            item { CommunityTopBar(onAsk = { askOpen = true }, onNotifications = { notificationsOpen = true }) }
            item {
                CommunitySearchBar(
                    searchText = controller.searchText,
                    onSearch = { controller.updateSearch(it) },
                    categories = categoryNames,
                    selectedCategory = controller.selectedCategoryIndex,
                    onSelectCategory = { controller.selectCategory(it) }
                )
            }

            when (controller.selectedTab) {
                0 -> {
                    item { EmotionChallengeCardNew() }
                    item { RecommendedGroupCardNew(controller) }
                    item { MyGroupsTimelineNew(controller) }
                    item { HotTopicCardNew() }
                }
            }

            // 帖子流
            itemsIndexed(displayPosts, key = { _, p -> p.id }) { _, post ->
                PostCardNew(
                    post = post,
                    comfortModeOn = controller.comfortModeOn,
                    onLike = {
                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                        if (idx >= 0) {
                            val cur = feedPosts[idx]
                            val liked = !cur.liked
                            val count = if (liked) cur.likeCount + 1 else (cur.likeCount - 1).coerceAtLeast(0)
                            feedPosts[idx] = cur.copy(liked = liked, likeCount = count)
                        }
                    },
                    onToggleComments = {
                        selectedPostForComments = post
                        commentsLoading = true
                        scope.launch {
                            try {
                                comments = CommunityRepositoryProvider.current.getPostComments(post.id)
                            } catch (e: Exception) {
                                comments = emptyList()
                            } finally {
                                commentsLoading = false
                            }
                            sheetState.expand()
                        }
                    },
                    onOpenDetail = {
                        selectedPostDetail = post
                        detailLoading = true
                        scope.launch {
                            try {
                                detailComments = CommunityRepositoryProvider.current.getPostComments(post.id)
                            } catch (e: Exception) {
                                detailComments = emptyList()
                            } finally {
                                detailLoading = false
                            }
                        }
                    },
                    onOpenImage = { url -> previewImageUrl = url },
                    onRetrySync = {
                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                        if (idx >= 0) {
                            scope.launch {
                                try {
                                    val created = CommunityRepositoryProvider.current.createPost(
                                        title = post.title,
                                        content = post.content,
                                        tags = post.tags,
                                        images = post.images,
                                        anonymous = post.isAnonymous
                                    )
                                    feedPosts[idx] = created
                                } catch (_: Exception) {
                                }
                            }
                        }
                    },
                    onBookmark = {
                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                        if (idx >= 0) {
                            val cur = feedPosts[idx]
                            feedPosts[idx] = cur.copy(bookmarked = !cur.bookmarked)
                        }
                    },
                    onEdit = {
                        editPost = post
                        editTitle = post.title
                        editContent = post.content
                        editTags = post.tags.joinToString(",")
                    },
                    onDelete = {
                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                        if (idx >= 0) {
                            val removed = feedPosts.removeAt(idx)
                            scope.launch { try { CommunityRepositoryProvider.current.deletePost(removed.id) } catch (_: Exception) {} }
                        }
                    },
                    onShare = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            val text = "${post.title}\n\n${post.content}"
                            intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
                            ctx.startActivity(android.content.Intent.createChooser(intent, "分享帖子"))
                        } catch (_: Exception) {}
                    }
                )
            }

            // 加载更多占位
            if (controller.searchText.isBlank() && !feedReachedEnd && feedPosts.isNotEmpty()) {
                item {
                    LaunchedEffect(Unit) {
                        if (!feedLoadingMore) {
                            feedLoadingMore = true
                            try {
                                val next = CommunityRepositoryProvider.current.getPosts(
                                    page = feedPage + 1,
                                    size = 10,
                                    category = if (controller.selectedCategoryIndex == 0) null else categoryNames[controller.selectedCategoryIndex]
                                )
                                if (next.isEmpty()) feedReachedEnd = true
                                else {
                                    feedPosts.addAll(next)
                                    feedPage += 1
                                }
                            } catch (e: Exception) {
                                /* ignore */
                            } finally {
                                feedLoadingMore = false
                            }
                        }
                    }
                    if (feedLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth()
                                .padding(tokens.spacing.L)
                        )
                    }
                }
            }

            if (controller.searchText.isNotBlank() && displayPosts.isEmpty()) {
                item {
                    Text(
                        text = "未找到相关内容",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = tokens.spacing.L),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) } // FAB 安全区
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // SpeedDial FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(tokens.spacing.L)
        ) {
            CommunityFab(
                onPost = { showCreateSheet = true },
                onAsk = { controller.openAnonymousPost() }
            )
        }

        if (controller.searchText.isNotBlank()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索结果", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { controller.updateSearch("") }) { Text("关闭") }
                    }
                    TabRow(selectedTabIndex = searchTabIndex) {
                        searchTabs.forEachIndexed { i, t ->
                            Tab(selected = searchTabIndex == i, onClick = { searchTabIndex = i }, text = { Text(t) })
                        }
                    }
                    val q = controller.searchText
                    when (searchTabIndex) {
                        0 -> {
                            val results = remember(feedPosts, q) { feedPosts.filter { it.title.contains(q) || it.content.contains(q) || it.tags.any { t -> t.contains(q) } } }
                            if (results.isEmpty()) {
                                Text("未找到相关内容", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                                    itemsIndexed(results, key = { _, p -> p.id }) { _, post ->
                                        PostCardNew(
                                            post = post,
                                            comfortModeOn = controller.comfortModeOn,
                                            onLike = {
                                                val idx = feedPosts.indexOfFirst { it.id == post.id }
                                                if (idx >= 0) {
                                                    val cur = feedPosts[idx]
                                                    val liked = !cur.liked
                                                    val count = if (liked) cur.likeCount + 1 else (cur.likeCount - 1).coerceAtLeast(0)
                                                    feedPosts[idx] = cur.copy(liked = liked, likeCount = count)
                                                }
                                            },
                                            onToggleComments = {
                                                selectedPostForComments = post
                                                commentsLoading = true
                                                scope.launch {
                                                    try { comments = CommunityRepositoryProvider.current.getPostComments(post.id) } catch (_: Exception) { comments = emptyList() } finally { commentsLoading = false }
                                                    sheetState.expand()
                                                }
                                            },
                                            onOpenDetail = { selectedPostDetail = post },
                                            onOpenImage = { url -> previewImageUrl = url },
                                            onRetrySync = {
                                                val idx = feedPosts.indexOfFirst { it.id == post.id }
                                                if (idx >= 0) {
                                                    scope.launch {
                                                        try {
                                                            val created = CommunityRepositoryProvider.current.createPost(
                                                                title = post.title,
                                                                content = post.content,
                                                                tags = post.tags,
                                                                images = post.images,
                                                                anonymous = post.isAnonymous
                                                            )
                                                            feedPosts[idx] = created
                                                        } catch (_: Exception) {
                                                        }
                                                    }
                                                }
                                            },
                                            onBookmark = {
                                                val idx = feedPosts.indexOfFirst { it.id == post.id }
                                                if (idx >= 0) {
                                                    val cur = feedPosts[idx]
                                                    feedPosts[idx] = cur.copy(bookmarked = !cur.bookmarked)
                                                }
                                            },
                                            onEdit = {
                                                editPost = post
                                                editTitle = post.title
                                                editContent = post.content
                                                editTags = post.tags.joinToString(",")
                                            },
                                            onDelete = {
                                                val idx = feedPosts.indexOfFirst { it.id == post.id }
                                                if (idx >= 0) {
                                                    val removed = feedPosts.removeAt(idx)
                                                    scope.launch { try { CommunityRepositoryProvider.current.deletePost(removed.id) } catch (_: Exception) {} }
                                                }
                                            },
                                        onShare = {}
                                    )
                                    }
                                }
                            }
                        }
                        1 -> {
                            val users = remember(feedPosts, q) { feedPosts.map { it.author }.distinct().filter { it.contains(q) } }
                            if (users.isEmpty()) {
                                Text("未找到相关用户", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                                    itemsIndexed(users) { _, name ->
                                        Surface(tonalElevation = 1.dp) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.M), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                                TextButton(onClick = {
                                                    selectedUserName = name
                                                    selectedUserProfile = null
                                                    scope.launch {
                                                        try {
                                                            selectedUserProfile = CommunityRepositoryProvider.current.getUserProfile(name)
                                                            selectedUserFavorites = CommunityRepositoryProvider.current.getUserFavorites(name)
                                                            selectedUserSharedGroups = CommunityRepositoryProvider.current.getSharedGroups(name)
                                                        } catch (_: Exception) {
                                                            selectedUserProfile = UserProfile(name, "", "热心互助，持续分享情绪管理心得。", false, 0, 0, 0)
                                                            selectedUserFavorites = emptyList()
                                                            selectedUserSharedGroups = emptyList()
                                                        }
                                                    }
                                                }) { Text("查看") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            LaunchedEffect(q) {
                                searchGroupsLoading = true
                                try {
                                    val all = CommunityRepositoryProvider.current.getGroups()
                                    searchGroups = all.filter { it.contains(q) }
                                } catch (_: Exception) {
                                    searchGroups = emptyList()
                                } finally { searchGroupsLoading = false }
                            }
                            if (searchGroupsLoading) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                            } else if (searchGroups.isEmpty()) {
                                Text("未找到相关小组", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                                    itemsIndexed(searchGroups) { _, g ->
                                        Surface(tonalElevation = 1.dp) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.M), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(g, style = MaterialTheme.typography.bodyLarge)
                                                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                                                    TextButton(onClick = {
                                                        scope.launch {
                                                            try {
                                                                val r = CommunityRepositoryProvider.current.applyJoin(g)
                                                                snackbarHostState.showSnackbar(r.message)
                                                            } catch (_: Exception) {
                                                                snackbarHostState.showSnackbar("申请失败")
                                                            }
                                                        }
                                                    }) { Text("申请加入") }
                                                    TextButton(onClick = {
                                                        selectedGroupIntro = g
                                                        selectedGroupInfo = null
                    scope.launch {
                        try {
                            selectedGroupInfo = CommunityRepositoryProvider.current.getGroupInfo(g)
                        } catch (_: Exception) {
                            selectedGroupInfo = com.example.xinqiao.community.GroupInfo(
                                name = g,
                                memberCount = 0,
                                rules = listOf("友善沟通", "禁止外传", "支持鼓励"),
                                joined = false,
                                adminName = "",
                                frequency = "",
                                schedule = ""
                            )
                        }
                    }
                                                    }) { Text("查看介绍") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedUserName != null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedUserName!!, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { selectedUserName = null }) { Text("关闭") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedUserProfile?.avatar ?: feedPosts.firstOrNull { it.author == selectedUserName }?.authorAvatar ?: ""),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(CircleShape)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(selectedUserName!!, style = MaterialTheme.typography.titleSmall)
                            Text(selectedUserProfile?.bio ?: "热心互助，持续分享情绪管理心得。", style = MaterialTheme.typography.bodySmall, color = tokens.color.Neutral700)
                            val posts = selectedUserProfile?.postsCount ?: 0
                            val fans = selectedUserProfile?.followersCount ?: 0
                            val follows = selectedUserProfile?.followingCount ?: 0
                            Text("帖子 $posts · 粉丝 $fans · 关注 $follows", style = MaterialTheme.typography.bodySmall, color = tokens.color.Neutral500)
                        }
                        Spacer(Modifier.weight(1f))
                        var following by remember(selectedUserProfile) { mutableStateOf(selectedUserProfile?.following ?: false) }
                        TextButton(onClick = {
                            val target = !following
                            following = target
                            scope.launch {
                                try { CommunityRepositoryProvider.current.setFollow(selectedUserName!!, target); snackbarHostState.showSnackbar(if (target) "已关注" else "已取消关注") }
                                catch (_: Exception) { following = !target; snackbarHostState.showSnackbar("操作失败") }
                            }
                        }) { Text(if (following) "已关注" else "关注") }
                    }
                    HorizontalDivider()
                    Text("TA的帖子", style = MaterialTheme.typography.titleSmall)
                    val postsByUser = remember(feedPosts, selectedUserName) { feedPosts.filter { it.author == selectedUserName } }
                    if (postsByUser.isEmpty()) {
                        Text("暂无内容", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                            itemsIndexed(postsByUser, key = { _, p -> p.id }) { _, post ->
                                PostCardNew(
                                    post = post,
                                    comfortModeOn = controller.comfortModeOn,
                                    onLike = {
                                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                                        if (idx >= 0) {
                                            val cur = feedPosts[idx]
                                            val liked = !cur.liked
                                            val count = if (liked) cur.likeCount + 1 else (cur.likeCount - 1).coerceAtLeast(0)
                                            feedPosts[idx] = cur.copy(liked = liked, likeCount = count)
                                        }
                                    },
                                    onToggleComments = {
                                        selectedPostForComments = post
                                        commentsLoading = true
                                        scope.launch {
                                            try { comments = CommunityRepositoryProvider.current.getPostComments(post.id) } catch (_: Exception) { comments = emptyList() } finally { commentsLoading = false }
                                            sheetState.expand()
                                        }
                                    },
                                    onOpenDetail = { selectedPostDetail = post },
                                    onOpenImage = { url -> previewImageUrl = url },
                                    onRetrySync = {},
                                    onBookmark = {
                                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                                        if (idx >= 0) {
                                            val cur = feedPosts[idx]
                                            feedPosts[idx] = cur.copy(bookmarked = !cur.bookmarked)
                                        }
                                    },
                                    onEdit = {
                                        editPost = post
                                        editTitle = post.title
                                        editContent = post.content
                                        editTags = post.tags.joinToString(",")
                                    },
                                    onDelete = {
                                        val idx = feedPosts.indexOfFirst { it.id == post.id }
                                        if (idx >= 0) {
                                            val removed = feedPosts.removeAt(idx)
                                            scope.launch { try { CommunityRepositoryProvider.current.deletePost(removed.id) } catch (_: Exception) {} }
                                        }
                                    },
                                    onShare = {}
                                )
                            }
                        }
                    }
                    Text("TA的收藏", style = MaterialTheme.typography.titleSmall)
                    if (selectedUserFavorites.isEmpty()) {
                        Text("暂无收藏", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                            itemsIndexed(selectedUserFavorites, key = { _, p -> p.id }) { _, post ->
                                PostCardNew(
                                    post = post,
                                    comfortModeOn = controller.comfortModeOn,
                                    onLike = {},
                                    onToggleComments = {},
                                    onOpenDetail = { selectedPostDetail = post },
                                    onOpenImage = { url -> previewImageUrl = url },
                                    onRetrySync = {},
                                    onBookmark = {},
                                    onEdit = {},
                                    onDelete = {},
                                    onShare = {}
                                )
                            }
                        }
                    }
                    Text("共同小组", style = MaterialTheme.typography.titleSmall)
                    if (selectedUserSharedGroups.isEmpty()) {
                        Text("暂无共同小组", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(selectedUserSharedGroups) { _, g -> AssistChip(onClick = {}, label = { Text(g) }) }
                        }
                    }
                }
            }
        }

        if (selectedGroupIntro != null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedGroupIntro!!, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { selectedGroupIntro = null }) { Text("关闭") }
                    }
                    Text("小组介绍", style = MaterialTheme.typography.titleSmall)
                    Text("成员数：${selectedGroupInfo?.memberCount ?: 0}", style = MaterialTheme.typography.bodyMedium)
                    Text("管理员：${selectedGroupInfo?.adminName ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    Text("打卡频率：${selectedGroupInfo?.frequency ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    Text("日程安排：${selectedGroupInfo?.schedule ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("规则：", style = MaterialTheme.typography.bodyMedium)
                        val rules = selectedGroupInfo?.rules ?: emptyList()
                        if (rules.isEmpty()) {
                            Text("暂无规则", style = MaterialTheme.typography.bodySmall)
                        } else {
                            rules.forEachIndexed { i, r -> Text("${i+1}. $r", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S), verticalAlignment = Alignment.CenterVertically) {
                        val joined = selectedGroupInfo?.joined ?: false
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    val ok = CommunityRepositoryProvider.current.setGroupJoin(selectedGroupIntro!!, !joined)
                                    if (ok) {
                                        selectedGroupInfo = selectedGroupInfo?.copy(joined = !joined, memberCount = if (!joined) (selectedGroupInfo?.memberCount ?: 0) + 1 else (selectedGroupInfo?.memberCount ?: 0) - 1)
                                        snackbarHostState.showSnackbar(if (!joined) "已加入" else "已退出")
                                    } else snackbarHostState.showSnackbar("操作失败")
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar("操作失败")
                                }
                            }
                        }) { Text(if (joined) "退出小组" else "申请加入") }
                        TextButton(onClick = { selectedGroupIntro = null }) { Text("关闭") }
                    }
                }
            }
        }

        if (selectedPostForComments != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    selectedPostForComments = null
                    commentsInput = ""
                },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(tokens.spacing.L),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)
                ) {
                    Text(selectedPostForComments!!.title, style = MaterialTheme.typography.titleSmall)
                    if (commentsLoading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.S)
                        ) {
                            itemsIndexed(comments) { _, c ->
                                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                                    Column(modifier = Modifier.padding(tokens.spacing.M)) {
                                        Text(c.author, style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = commentsInput,
                            onValueChange = { commentsInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("写下你的回应…") }
                        )
                        Spacer(Modifier.width(tokens.spacing.S))
                        Button(onClick = {
                            val msg = commentsInput.trim()
                            if (msg.isNotEmpty() && selectedPostForComments != null) {
                                val postId = selectedPostForComments!!.id
                                scope.launch {
                                    val before = comments
                                    val optimistic = before + Comment(id = "tmp${System.currentTimeMillis()}", author = "我", text = msg)
                                    comments = optimistic
                                    commentsInput = ""
                                    try {
                                        val created = CommunityRepositoryProvider.current.postPostComment(postId, msg)
                                        comments = optimistic.dropLast(1) + created
                                        val idx = feedPosts.indexOfFirst { it.id == postId }
                                        if (idx >= 0) {
                                            val cur = feedPosts[idx]
                                            feedPosts[idx] = cur.copy(commentCount = cur.commentCount + 1)
                                        }
                                    } catch (e: Exception) {
                                        comments = before
                                    }
                                }
                            }
                        }) { Text("发送") }
                    }
                }
            }
        }

        if (selectedPostDetail != null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(tokens.spacing.L),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("帖子详情", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { selectedPostDetail = null }) { Text("关闭") }
                    }
                    val p = selectedPostDetail!!
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                        Text(p.title, style = MaterialTheme.typography.titleSmall)
                        if (p.pendingSync) {
                            AssistChip(onClick = {}, label = { Text("未同步") })
                        }
                    }
                    Text(p.content, style = MaterialTheme.typography.bodyMedium)
                    if (p.tags.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(p.tags) { _, t ->
                                AssistChip(onClick = {}, label = { Text(t) })
                            }
                        }
                    }
                    if (p.images.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(p.images) { _, url ->
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp).clickable { previewImageUrl = url }
                                )
                            }
                        }
                    }
                    Divider()
                    Text("评论", style = MaterialTheme.typography.titleSmall)
                    if (detailLoading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.S)
                        ) {
                            itemsIndexed(detailComments) { _, c ->
                                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                                    Column(modifier = Modifier.padding(tokens.spacing.M)) {
                                        Text(c.author, style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = detailInput,
                            onValueChange = { detailInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("写下你的回应…") }
                        )
                        Spacer(Modifier.width(tokens.spacing.S))
                        Button(onClick = {
                            val msg = detailInput.trim()
                            if (msg.isNotEmpty() && selectedPostDetail != null) {
                                val postId = selectedPostDetail!!.id
                                scope.launch {
                                    val before = detailComments
                                    val optimistic = before + Comment(id = "tmp${System.currentTimeMillis()}", author = "我", text = msg)
                                    detailComments = optimistic
                                    detailInput = ""
                                    try {
                                        val created = CommunityRepositoryProvider.current.postPostComment(postId, msg)
                                        detailComments = optimistic.dropLast(1) + created
                                        val idx = feedPosts.indexOfFirst { it.id == postId }
                                        if (idx >= 0) {
                                            val cur = feedPosts[idx]
                                            feedPosts[idx] = cur.copy(commentCount = cur.commentCount + 1)
                                        }
                                    } catch (e: Exception) {
                                        detailComments = before
                                    }
                                }
                            }
                        }) { Text("发送") }
                    }
                }
            }
        }

        if (showCreateSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCreateSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    TextField(value = createTitle, onValueChange = { createTitle = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("填写标题") })
                    TextField(value = createContent, onValueChange = { createContent = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), placeholder = { Text("分享你的想法…") })
                    TextField(value = createTags, onValueChange = { createTags = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("标签，逗号或空格分隔") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { pickImagesLauncher.launch("image/*") }) { Text("选择图片") }
                    }
                    if (createImages.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(createImages) { idx, url ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    androidx.compose.foundation.Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = null,
                                        modifier = Modifier.size(96.dp)
                                    )
                                    TextButton(onClick = { createImages.removeAt(idx) }) { Text("移除") }
                                }
                            }
                        }
                    }
                    var uploading by remember { mutableStateOf(false) }
                    var uploadProgress by remember { mutableStateOf(0f) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = createAnonymous, onCheckedChange = { createAnonymous = it ?: false })
                        Spacer(Modifier.width(8.dp))
                        Text("匿名发布")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = {
                            val tags = createTags.split(',', '，', ' ').map { it.trim() }.filter { it.isNotBlank() }
                            // 基本校验：至少有内容或图片
                            val hasContent = createContent.isNotBlank() || createImages.isNotEmpty()
                            if (!hasContent) {
                                scope.launch { snackbarHostState.showSnackbar("请填写内容或选择图片") }
                                return@Button
                            }
                            scope.launch {
                                val nowId = "p" + System.currentTimeMillis()
                                val fallback = ThemePost(
                                    id = nowId,
                                    author = if (createAnonymous) "匿名用户" else "我",
                                    isAnonymous = createAnonymous,
                                    time = "刚刚",
                                    title = if (createTitle.isNotBlank()) createTitle else "未命名",
                                    content = createContent,
                                    tags = tags,
                                    images = createImages.toList(),
                                    pendingSync = true
                                )
                                var created: ThemePost? = null
                                try {
                                    var remoteImages = emptyList<String>()
                                    if (createImages.isNotEmpty()) {
                                        val wm = WorkManager.getInstance(ctx)
                                        uploading = true
                                        uploadProgress = 0f
                                        val constraints = Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build()
                                        val requests = createImages.map { uri ->
                                            OneTimeWorkRequestBuilder<ImageUploadWorker>()
                                                .setConstraints(constraints)
                                                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.SECONDS)
                                                .setInputData(Data.Builder().putString(ImageUploadWorker.KEY_INPUT_URI, uri).build())
                                                .build()
                                        }
                                        requests.forEach { wm.enqueue(it) }
                                        val urls = mutableListOf<String>()
                                        var completed = 0
                                        for (req in requests) {
                                            var progress = 0
                                            while (true) {
                                                val info = wm.getWorkInfoById(req.id).get()
                                                if (info != null) {
                                                    progress = info.progress.getInt(ImageUploadWorker.KEY_PROGRESS, progress)
                                                    uploadProgress = (completed.toFloat() / requests.size) + (progress / 100f) / requests.size
                                                    if (info.state.isFinished) {
                                                        val url = info.outputData.getString(ImageUploadWorker.KEY_REMOTE_URL)
                                                        if (url != null) urls.add(url)
                                                        if (info.state == WorkInfo.State.FAILED) {
                                                            // 保留占位，继续队列，其它成功的图片将使用远程URL
                                                        }
                                                        completed++
                                                        break
                                                    }
                                                }
                                                kotlinx.coroutines.delay(200)
                                            }
                                        }
                                        remoteImages = urls
                                        uploading = false
                                    }
                                    created = CommunityRepositoryProvider.current.createPost(
                                        title = createTitle,
                                        content = createContent,
                                        tags = tags,
                                        images = remoteImages,
                                        anonymous = createAnonymous
                                    )
                                } catch (_: Exception) {
                                    created = fallback
                                }
                                feedPosts.add(0, created!!)
                                showCreateSheet = false
                                createTitle = ""
                                createContent = ""
                                createTags = ""
                                createAnonymous = false
                                createImages.clear()
                            }
                        }) { Text("发布") }
                        if (uploading) {
                            Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(progress = uploadProgress); Spacer(Modifier.width(tokens.spacing.S)); Text("上传中 ${((uploadProgress)*100).toInt()}%") }
                        }
                    }
                }
            }
        }

        // 骨架占位：加载中且列表为空时显示
        if (controller.isLoading && feedPosts.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                repeat(3) { FeedSkeletonItem(tokens) }
            }
        }

        if (previewImageUrl != null) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                Box(Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        painter = rememberAsyncImagePainter(previewImageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    TextButton(onClick = { previewImageUrl = null }, modifier = Modifier.align(Alignment.TopEnd).padding(tokens.spacing.M)) { Text("关闭") }
                }
            }
        }

        if (notificationsOpen) {
            ModalBottomSheet(onDismissRequest = { notificationsOpen = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    Text("消息中心", style = MaterialTheme.typography.titleMedium)
                    if (notificationsLoading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(notifications, key = { _, n -> n.id }) { _, n ->
                                Surface(tonalElevation = 1.dp) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.M), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column { Text(n.title, style = MaterialTheme.typography.bodyLarge); Text(n.content, style = MaterialTheme.typography.bodyMedium) }
                                        TextButton(onClick = {
                                            scope.launch {
                                                try { CommunityRepositoryProvider.current.markNotificationRead(n.id); notifications = notifications.map { if (it.id == n.id) it.copy(read = true) else it } }
                                                catch (_: Exception) {}
                                            }
                                        }) { Text(if (n.read) "已读" else "标记已读") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                notificationsLoading = true
                try { notifications = CommunityRepositoryProvider.current.getNotifications() } catch (_: Exception) { notifications = emptyList() } finally { notificationsLoading = false }
            }
        }

        if (askOpen) {
            ModalBottomSheet(onDismissRequest = { askOpen = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    Text("匿名提问", style = MaterialTheme.typography.titleMedium)
                    TextField(value = askTitle, onValueChange = { askTitle = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("标题") })
                    TextField(value = askContent, onValueChange = { askContent = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), placeholder = { Text("描述你的问题…") })
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                        Button(onClick = {
                            scope.launch {
                                try {
                                    val created = CommunityRepositoryProvider.current.createPost(
                                        title = askTitle,
                                        content = askContent,
                                        tags = listOf("问答"),
                                        images = emptyList(),
                                        anonymous = true
                                    )
                                    feedPosts.add(0, created)
                                    askOpen = false
                                    askTitle = ""
                                    askContent = ""
                                } catch (_: Exception) {
                                    // 本地兜底
                                    val local = ThemePost(
                                        id = "p" + System.currentTimeMillis(),
                                        author = "匿名用户",
                                        isAnonymous = true,
                                        time = "刚刚",
                                        title = askTitle.ifBlank { "未命名" },
                                        content = askContent,
                                        tags = listOf("问答"),
                                        pendingSync = true
                                    )
                                    feedPosts.add(0, local)
                                    askOpen = false
                                    askTitle = ""
                                    askContent = ""
                                }
                            }
                        }) { Text("发布") }
                        TextButton(onClick = { askOpen = false }) { Text("取消") }
                    }
                }
            }
        }

        if (editPost != null) {
            ModalBottomSheet(onDismissRequest = { editPost = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                    TextField(value = editTitle, onValueChange = { editTitle = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("标题") })
                    TextField(value = editContent, onValueChange = { editContent = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), placeholder = { Text("内容") })
                    TextField(value = editTags, onValueChange = { editTags = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("标签，逗号分隔") })
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                        Button(onClick = {
                            val id = editPost!!.id
                            val tags = editTags.split(',', '，', ' ').map { it.trim() }.filter { it.isNotBlank() }
                            val idx = feedPosts.indexOfFirst { it.id == id }
                            if (idx >= 0) {
                                val updated = feedPosts[idx].copy(title = editTitle, content = editContent, tags = tags)
                                feedPosts[idx] = updated
                                scope.launch { try { CommunityRepositoryProvider.current.updatePost(id, editTitle, editContent, tags) } catch (_: Exception) {} }
                            }
                            editPost = null
                        }) { Text("保存") }
                        TextButton(onClick = { editPost = null }) { Text("取消") }
                    }
                }
            }
        }
    }
}
@Composable
private fun FeedSkeletonItem(tokens: CommunityTokens = CommunityTokensInstance) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.L), verticalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
            // avatar row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                Spacer(modifier = Modifier.width(tokens.spacing.S))
                Column {
                    Box(modifier = Modifier.height(14.dp).width(120.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(12.dp).width(80.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
            Box(modifier = Modifier.height(16.dp).fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surfaceVariant))
            Box(modifier = Modifier.height(12.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant))
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                repeat(3) { Box(modifier = Modifier.size(96.dp).background(MaterialTheme.colorScheme.surfaceVariant)) }
            }
        }
    }
}

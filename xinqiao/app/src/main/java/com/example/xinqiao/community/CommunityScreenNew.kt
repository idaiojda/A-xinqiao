package com.example.xinqiao.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    LaunchedEffect(Unit) {
        try {
            if (CommunityRepositoryProvider.current === FakeCommunityRepository) {
                val baseUrl = com.example.xinqiao.network.NetworkConfig.getBaseUrl(ctx)
                val api = com.example.xinqiao.community.CommunityServiceFactory.create(baseUrl)
                CommunityRepositoryProvider.current = RemoteCommunityRepository(api)
            }
        } catch (_: Throwable) { }
    }
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
    var selectedUserOwnedGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedUserOwnedLoading by remember { mutableStateOf(false) }
    var selectedGroupIntro by remember { mutableStateOf<String?>(null) }
    var selectedGroupInfo by remember { mutableStateOf<GroupInfo?>(null) }
    var editPost by remember { mutableStateOf<ThemePost?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    // 我的小组（新会话入口）
    var myGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    suspend fun persistJoin(name: String, joined: Boolean): Boolean {
        val ok = try { CommunityRepositoryProvider.current.setGroupJoin(name, joined) } catch (_: Exception) { false }
        try {
            val dao = CommunityLocalCache.database()?.groupDao()
            val cur = dao?.get(name)
            val rules = cur?.rulesJson ?: com.google.gson.Gson().toJson(listOf("友善沟通", "禁止外传", "支持鼓励"))
            val admin = cur?.adminName ?: ""
            val freq = cur?.frequency ?: ""
            val sched = cur?.schedule ?: ""
            val mc = cur?.memberCount ?: 0
            dao?.upsert(GroupInfoEntity(name = name, memberCount = mc, rulesJson = rules, joined = joined, adminName = admin, frequency = freq, schedule = sched))
        } catch (_: Exception) { }
        try {
            val user = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: ""
            if (user.isNotBlank()) {
                val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
                val raw = sp.getString("joinedGroups_" + user, "[]")
                val arr = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as MutableList<String> } catch (_: Exception) { mutableListOf() }
                if (joined) {
                    if (!arr.contains(name)) arr.add(name)
                } else {
                    arr.remove(name)
                }
                sp.edit().putString("joinedGroups_" + user, com.google.gson.Gson().toJson(arr)).apply()
            }
        } catch (_: Exception) { }
        return ok
    }
    suspend fun reloadMyGroups() {
        val user = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx)
        try {
            val dao = CommunityLocalCache.database()?.groupDao()
            val localJoined: List<String> = try { dao?.listJoinedNames() ?: emptyList() } catch (_: Exception) { emptyList() }
            val localByOwner: List<String> = try { dao?.listNamesByOwnerOrJoined(user) ?: emptyList() } catch (_: Exception) { emptyList() }
            val remote: List<String> = try { CommunityRepositoryProvider.current.getSharedGroups(user) } catch (_: Exception) { emptyList() }
            val sp = ctx.getSharedPreferences("loginInfo", android.content.Context.MODE_PRIVATE)
            val raw = sp.getString("joinedGroups_" + user, "[]")
            val fromSp: List<String> = try { com.google.gson.Gson().fromJson(raw, java.util.ArrayList::class.java) as List<String> } catch (_: Exception) { emptyList() }
            myGroups = (localJoined + localByOwner + remote + fromSp).distinct()
        } catch (_: Exception) {
            myGroups = emptyList()
        }
    }
    LaunchedEffect(Unit) { reloadMyGroups() }
    LaunchedEffect(controller.selectedTab) { if (controller.selectedTab == 0) reloadMyGroups() }

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
                    category = if (controller.selectedCategoryIndex == 0) null else categoryNames[controller.selectedCategoryIndex],
                    q = controller.searchText.takeIf { it.isNotBlank() }
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
                    feedPosts.filter { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it.title, q) || com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it.content, q) || it.tags.any { t -> com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(t, q) } }
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
            item { CommunityTopBar() }
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
                    if (myGroups.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                tonalElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("我创建/加入的小组", style = MaterialTheme.typography.titleMedium)
                                        TextButton(onClick = {
                                            val intent = android.content.Intent(ctx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                                            intent.putExtra("group", myGroups.first())
                                            ctx.startActivity(intent)
                                        }) { Text("进入会话") }
                                    }
                                    myGroups.forEach { g ->
                                        Surface(onClick = {
                                            val intent = android.content.Intent(ctx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                                            intent.putExtra("group", g)
                                            ctx.startActivity(intent)
                                        }, shape = MaterialTheme.shapes.medium, tonalElevation = 0.dp) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(g, style = MaterialTheme.typography.bodyMedium)
                                                Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        MyGroupsTimelineNew(controller) { _ ->
                            scope.launch { reloadMyGroups() }
                        }
                    }
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
                onMessages = { notificationsOpen = true }
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
                            val results = remember(feedPosts, q) { feedPosts.filter { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it.title, q) || com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it.content, q) || it.tags.any { t -> com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(t, q) } } }
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
                            var users by remember { mutableStateOf<List<String>>(emptyList()) }
                            LaunchedEffect(q, feedPosts) {
                                try {
                                    val base = feedPosts.map { it.author }.distinct()
                                    val dao = CommunityLocalCache.database()?.groupDao()
                                    val adminsLocal: List<String> = try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao?.listAdminNames() ?: emptyList() } } catch (_: Exception) { emptyList() }
                                    val remoteGroups: List<String> = try { kotlinx.coroutines.withTimeout(3000) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CommunityRepositoryProvider.current.getGroups(null) } } } catch (_: Exception) { emptyList() }
                                    val adminsRemote: List<String> = if (remoteGroups.isNotEmpty()) {
                                        val acc = mutableSetOf<String>()
                                        for (g in remoteGroups) {
                                            try {
                                                val info = CommunityRepositoryProvider.current.getGroupInfo(g)
                                                if (info.adminName.isNotBlank()) acc.add(info.adminName)
                                            } catch (_: Exception) { }
                                        }
                                        acc.toList()
                                    } else {
                                        try {
                                            kotlinx.coroutines.withTimeout(2000) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    val list = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listCommunityGroups()
                                                    list.mapNotNull { g ->
                                                        try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupOwnerName(g) } catch (_: Exception) { null }
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) { emptyList() }
                                    }
                                    val dbUsers: List<String> = try { kotlinx.coroutines.withTimeout(2000) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listUserNamesByKeyword(q) } } } catch (_: Exception) { emptyList() }
                                    val candidates = (base + adminsLocal + adminsRemote + dbUsers).distinct()
                                    val direct = candidates.filter { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it, q) }
                                    val needNick = candidates.filterNot { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it, q) }
                                    val nickMatches = mutableListOf<String>()
                                    for (name in needNick) {
                                        try {
                                            val nick = kotlinx.coroutines.withTimeout(1500) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNicknameSync(name)
                                                }
                                            }
                                            if (!nick.isNullOrBlank() && com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(nick, q)) {
                                                nickMatches.add(name)
                                            }
                                        } catch (_: Exception) { }
                                    }
                                    users = (direct + nickMatches).distinct()
                                } catch (_: Exception) {
                                    users = feedPosts.map { it.author }.distinct().filter { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it, q) }
                                }
                            }
                            if (users.isEmpty()) {
                                Text("未找到相关用户", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                                    itemsIndexed(users) { _, name ->
                                        Surface(tonalElevation = 1.dp) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(tokens.spacing.M), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                var displayName by remember(name) { mutableStateOf(name) }
                                                LaunchedEffect(name) {
                                                    try {
                                                        val nick = kotlinx.coroutines.withTimeout(2000) {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNicknameSync(name)
                                                            }
                                                        }
                                                        if (!nick.isNullOrBlank()) displayName = nick
                                                    } catch (_: Exception) { }
                                                }
                                                Text(displayName, style = MaterialTheme.typography.bodyLarge)
                                                TextButton(onClick = {
                                                    selectedUserName = name
                                                    selectedUserProfile = null
                                                    scope.launch {
                                                        selectedUserOwnedLoading = true
                                                        try {
                                                            selectedUserProfile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CommunityRepositoryProvider.current.getUserProfile(name) }
                                                            selectedUserFavorites = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CommunityRepositoryProvider.current.getUserFavorites(name) }
                                                            selectedUserSharedGroups = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CommunityRepositoryProvider.current.getSharedGroups(name) }
                                                            val dao = CommunityLocalCache.database()?.groupDao()
                                                            val localOwned: List<String> = try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { dao?.listNamesByOwner(name) ?: emptyList() } } catch (_: Exception) { emptyList() }
                                                            val remoteCandidates: List<String> = try { kotlinx.coroutines.withTimeout(3000) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CommunityRepositoryProvider.current.getGroups(null) } } } catch (_: Exception) { emptyList() }
                                                            val ownedRemote: List<String> = run {
                                                                val acc = mutableListOf<String>()
                                                                for (g in remoteCandidates) {
                                                                    try {
                                                                        val info = CommunityRepositoryProvider.current.getGroupInfo(g)
                                                                        if (info.adminName.equals(name, ignoreCase = true)) acc.add(g)
                                                                    } catch (_: Exception) { }
                                                                }
                                                                acc
                                                            }
                                                            val ownedFromDb: List<String> = try {
                                                                kotlinx.coroutines.withTimeout(2000) {
                                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                        val list = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listCommunityGroups()
                                                                        list.filter { g ->
                                                                            try { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupOwnerName(g).equals(name, ignoreCase = true) }
                                                                            catch (_: Exception) { false }
                                                                        }
                                                                    }
                                                                }
                                                            } catch (_: Exception) { emptyList() }
                                                            selectedUserOwnedGroups = (localOwned + ownedRemote + ownedFromDb).distinct()
                                                        } catch (_: Exception) {
                                                            selectedUserProfile = UserProfile(name, "", "热心互助，持续分享情绪管理心得。", false, 0, 0, 0)
                                                            selectedUserFavorites = emptyList()
                                                            selectedUserSharedGroups = emptyList()
                                                            selectedUserOwnedGroups = emptyList()
                                                        } finally {
                                                            selectedUserOwnedLoading = false
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
                                    val matched = try { kotlinx.coroutines.withTimeout(3000) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { CommunityRepositoryProvider.current.getGroups(q) } } } catch (_: Exception) { emptyList() }
                                    val dbList: List<String> = try { kotlinx.coroutines.withTimeout(2000) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listCommunityGroups() } } } catch (_: Exception) { emptyList() }
                                    val union = (matched + myGroups + dbList).distinct().filter { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it, q) }
                                    searchGroups = union
                                } catch (_: Exception) {
                                    val dbList: List<String> = try { kotlinx.coroutines.withTimeout(2000) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).listCommunityGroups() } } } catch (_: Exception) { emptyList() }
                                    searchGroups = (myGroups + dbList).distinct().filter { com.example.xinqiao.util.text.TextMatchUtils.containsFuzzy(it, q) }
                                } finally { searchGroupsLoading = false }
                            }
                            if (searchGroupsLoading) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                            } else if (searchGroups.isEmpty()) {
                                Text("未找到相关小组", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                                    itemsIndexed(searchGroups) { _, g ->
                                        // 获取当前小组信息以显示创建者与控制加入按钮
                                        val ctxLocal = androidx.compose.ui.platform.LocalContext.current
                    val ownerName = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctxLocal) ?: "我"
                                        var info by remember(g) { mutableStateOf<GroupInfo?>(null) }
                                        LaunchedEffect(g) {
                                            try { info = CommunityRepositoryProvider.current.getGroupInfo(g) } catch (_: Exception) { info = null }
                                        }
                                        val ownerNickname = if (info?.adminName?.isNotBlank() == true) info!!.adminName else null
                                        val isOwner = ownerNickname?.equals(ownerName, ignoreCase = true) == true

                                        Surface(tonalElevation = 1.dp) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(tokens.spacing.M),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(g, style = MaterialTheme.typography.bodyLarge)
                                                    if (!ownerNickname.isNullOrBlank()) {
                                                        Text(
                                                            text = "群主：$ownerNickname",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                                                    var joined by remember(info?.joined) { mutableStateOf(info?.joined == true) }
                                                    if (!isOwner) {
                                                        if (!joined) {
                                                            TextButton(onClick = {
                                                                scope.launch {
                                                                    val ok = persistJoin(g, true)
                                                                    if (ok) {
                                                                        joined = true
                                                                        controller.setJoined(g, true)
                                                                        reloadMyGroups()
                                                                        snackbarHostState.showSnackbar("已加入")
                                                                    } else {
                                                                        snackbarHostState.showSnackbar("加入失败")
                                                                    }
                                                                }
                                                            }) { Text("加入") }
                                                        } else {
                                                            TextButton(onClick = {
                                                                scope.launch {
                                                                    val ok = persistJoin(g, false)
                                                                    if (ok) {
                                                                        joined = false
                                                                        controller.setJoined(g, false)
                                                                        reloadMyGroups()
                                                                        snackbarHostState.showSnackbar("已退出")
                                                                    } else {
                                                                        snackbarHostState.showSnackbar("退出失败")
                                                                    }
                                                                }
                                                            }) { Text("退出") }
                                                        }
                                                    }
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
                    var displayName by remember(selectedUserName) { mutableStateOf(selectedUserName!!) }
                    LaunchedEffect(selectedUserName) {
                        try {
                            val nick = kotlinx.coroutines.withTimeout(2000) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNicknameSync(selectedUserName!!)
                                }
                            }
                            if (!nick.isNullOrBlank()) displayName = nick
                        } catch (_: Exception) { }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { selectedUserName = null }) { Text("关闭") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.M)) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedUserProfile?.avatar ?: feedPosts.firstOrNull { it.author == selectedUserName }?.authorAvatar ?: ""),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(CircleShape)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(displayName, style = MaterialTheme.typography.titleSmall)
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
                    Text("TA创建的小组", style = MaterialTheme.typography.titleSmall)
                    if (selectedUserOwnedLoading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                    } else if (selectedUserOwnedGroups.isEmpty()) {
                        Text("暂无创建的小组", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(selectedUserOwnedGroups) { _, g -> AssistChip(onClick = { selectedGroupIntro = g }, label = { Text(g) }) }
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
                    var groupInfoLoading by remember { mutableStateOf(false) }
                    // Modern header with gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ),
                                shape = MaterialTheme.shapes.large
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = selectedGroupIntro!!,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "小组介绍",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                var editDescOpen by remember { mutableStateOf(false) }
                                val editScope = rememberCoroutineScope()
                                var canEditDesc by remember { mutableStateOf(false) }
                                LaunchedEffect(selectedGroupIntro, selectedGroupInfo?.adminName) {
                                    try {
                                        val current = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: ""
                                        var admin = selectedGroupInfo?.adminName
                                        if (admin.isNullOrBlank() && selectedGroupIntro != null) {
                                            admin = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupOwnerName(selectedGroupIntro!!)
                                            }
                                        }
                                        canEditDesc = admin?.equals(current, ignoreCase = true) == true
                                    } catch (_: Exception) { canEditDesc = false }
                                }
                                var groupDesc by remember(selectedGroupIntro) { mutableStateOf("") }
                                LaunchedEffect(selectedGroupIntro) {
                                    val name = selectedGroupIntro
                                    if (name != null) {
                                        try {
                                            groupDesc = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupInfo(name)?.description ?: ""
                                            } ?: ""
                                        } catch (_: Exception) { groupDesc = "" }
                                    }
                                }
                                if (groupDesc.isBlank()) {
                                    Text(
                                        text = "群主比较懒.................",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = if (canEditDesc) Modifier.clickable { editDescOpen = true } else Modifier
                                    )
                                } else {
                                    Text(
                                        text = groupDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = if (canEditDesc) Modifier.clickable { editDescOpen = true } else Modifier
                                    )
                                }
                                if (editDescOpen) {
                                    var descEdit by remember(groupDesc, editDescOpen) { mutableStateOf(groupDesc) }
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { editDescOpen = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                val name = selectedGroupIntro ?: ""
                                                if (name.isNotBlank()) {
                                                    editScope.launch {
                                                        val ok = try {
                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                CommunityRepositoryProvider.current.updateGroupInfo(name, if (descEdit.isNotBlank()) descEdit else null, null, null)
                                                            }
                                                        } catch (_: Exception) { false }
                                                        if (!ok) {
                                                            try {
                                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                                    com.example.xinqiao.mysql.DBUtils.getInstance(ctx).updateCommunityGroupInfo(name, descEdit, null, null)
                                                                }
                                                            } catch (_: Exception) { }
                                                        }
                                                        groupDesc = descEdit
                                                        editDescOpen = false
                                                    }
                                                } else {
                                                    editDescOpen = false
                                                }
                                            }) { Text("保存") }
                                        },
                                        dismissButton = { TextButton(onClick = { editDescOpen = false }) { Text("取消") } },
                                        text = {
                                            androidx.compose.material3.TextField(value = descEdit, onValueChange = { descEdit = it }, label = { Text("编辑小组介绍") })
                                        }
                                    )
                                }
                                if (groupInfoLoading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("正在加载信息…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                // 群主昵称：优先 adminName，对应用户资料中的展示名
                                var ownerNickname by remember(selectedGroupIntro, selectedGroupInfo?.adminName) { mutableStateOf(selectedGroupInfo?.adminName ?: "") }
                                LaunchedEffect(selectedGroupIntro, selectedGroupInfo?.adminName) {
                                    val display = try {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            var admin = selectedGroupInfo?.adminName
                                            if (admin.isNullOrBlank() && selectedGroupIntro != null) {
                                                admin = CommunityLocalCache.database()?.groupDao()?.get(selectedGroupIntro!!)?.adminName ?: admin
                                                if (admin.isNullOrBlank()) {
                                                    admin = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupOwnerName(selectedGroupIntro!!)
                                                }
                                            }
                                            if (admin.isNullOrBlank() && selectedGroupIntro != null) {
                                                val me = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: ""
                                                if (me.isNotBlank()) {
                                                    com.example.xinqiao.mysql.DBUtils.getInstance(ctx).setCommunityGroupOwner(selectedGroupIntro!!, me)
                                                    admin = me
                                                }
                                            }
                                            var nickname = ""
                                            val user = admin ?: ""
                                            if (user.isNotBlank()) {
                                                try { nickname = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNicknameSync(user) ?: "" } catch (_: Exception) { }
                                            }
                                            if (nickname.isNotBlank()) nickname else user
                                        }
                                    } catch (_: Exception) { "" }
                                    ownerNickname = display
                                }
                                if (ownerNickname.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("群主：$ownerNickname") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = { selectedGroupIntro = null },
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    // Modern group info cards with better visual hierarchy
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Group stats card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var realMemberCount by remember(selectedGroupIntro, selectedGroupInfo?.memberCount) { mutableStateOf(selectedGroupInfo?.memberCount ?: 0) }
                                LaunchedEffect(selectedGroupIntro, selectedGroupInfo?.memberCount) {
                                    val count = try {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            if (selectedGroupIntro != null) com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupMemberCount(selectedGroupIntro!!) else realMemberCount
                                        }
                                    } catch (_: Exception) { selectedGroupInfo?.memberCount ?: 0 }
                                    realMemberCount = count
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$realMemberCount",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "成员数",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    var ownerNickname2 by remember(selectedGroupIntro, selectedGroupInfo?.adminName) { mutableStateOf(selectedGroupInfo?.adminName ?: "暂无") }
                                    LaunchedEffect(selectedGroupIntro, selectedGroupInfo?.adminName) {
                                        val display = try {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                var admin = selectedGroupInfo?.adminName
                                                if (admin.isNullOrBlank() && selectedGroupIntro != null) {
                                                    admin = CommunityLocalCache.database()?.groupDao()?.get(selectedGroupIntro!!)?.adminName ?: admin
                                                    if (admin.isNullOrBlank()) {
                                                        admin = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getCommunityGroupOwnerName(selectedGroupIntro!!)
                                                    }
                                                }
                                                if (admin.isNullOrBlank() && selectedGroupIntro != null) {
                                                    val me = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: ""
                                                    if (me.isNotBlank()) {
                                                        com.example.xinqiao.mysql.DBUtils.getInstance(ctx).setCommunityGroupOwner(selectedGroupIntro!!, me)
                                                        admin = me
                                                    }
                                                }
                                                val user = admin ?: ""
                                                var nickname = ""
                                                if (user.isNotBlank()) {
                                                    try { nickname = com.example.xinqiao.mysql.DBUtils.getInstance(ctx).getUserNicknameSync(user) ?: "" } catch (_: Exception) { }
                                                }
                                                if (nickname.isNotBlank()) nickname else (user.ifBlank { "暂无" })
                                            }
                                        } catch (_: Exception) { "暂无" }
                                        ownerNickname2 = display
                                    }
                                    Text(ownerNickname2, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("群主", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        
                        // 小组详情卡已移除
                    }
                    // Chat entry card placed near the top for visibility
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("会话", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = {
                                val intent = android.content.Intent(ctx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                                intent.putExtra("group", selectedGroupIntro!!)
                                ctx.startActivity(intent)
                            }) { Text("进入会话") }
                        }
                    }
                    // Modern rules section with card styling
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var rulesEditOpen by remember { mutableStateOf(false) }
                            val rulesScope = rememberCoroutineScope()
                            val currentUser2 = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: ""
                            val canEditOwnerRules = (selectedGroupInfo?.adminName?.equals(currentUser2, ignoreCase = true) == true)
                            var rulesInput by remember(selectedGroupInfo) { mutableStateOf(selectedGroupInfo?.rules?.joinToString("\n") ?: "") }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "小组规则",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            val rules = selectedGroupInfo?.rules ?: emptyList()
                            if (rules.isEmpty()) {
                                Text(
                                    text = "暂无规则",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 28.dp)
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 28.dp)
                                ) {
                                    rules.forEachIndexed { i, r -> 
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            tonalElevation = 1.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${i + 1}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = r,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (canEditOwnerRules) {
                                    TextButton(onClick = { rulesEditOpen = true }) { Text("编辑规则") }
                                }
                            }
                            if (rulesEditOpen) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { rulesEditOpen = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val name = selectedGroupInfo?.name ?: selectedGroupIntro ?: ""
                                            if (!name.isNullOrBlank()) {
                                                rulesScope.launch {
                                                    if (!canEditOwnerRules) {
                                                        snackbarHostState.showSnackbar("仅群主可编辑")
                                                        rulesEditOpen = false
                                                        return@launch
                                                    }
                                                    val rulesJson = if (rulesInput.isNotBlank()) com.google.gson.Gson().toJson(rulesInput.split('\n').map { it.trim() }.filter { it.isNotBlank() }) else null
                                                    val ok = try {
                                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            CommunityRepositoryProvider.current.updateGroupInfo(name!!, null, rulesJson, null)
                                                        }
                                                    } catch (_: Exception) { false }
                                                    if (!ok) {
                                                        try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.xinqiao.mysql.DBUtils.getInstance(ctx).updateCommunityGroupInfo(name!!, null, rulesJson, null) } } catch (_: Exception) { }
                                                    }
                                                    val newRules = if (rulesInput.isNotBlank()) rulesInput.split('\n').map { it.trim() }.filter { it.isNotBlank() } else emptyList()
                                                    selectedGroupInfo = selectedGroupInfo?.copy(rules = newRules)
                                                    rulesEditOpen = false
                                                }
                                            }
                                        }) { Text("保存") }
                                    },
                                    dismissButton = { TextButton(onClick = { rulesEditOpen = false }) { Text("取消") } },
                                    text = {
                                        androidx.compose.material3.TextField(value = rulesInput, onValueChange = { rulesInput = it }, label = { Text("小组规则(每行一条)") })
                                    }
                                )
                            }
                        }
                    }
                    // Modern group action buttons with better styling
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val joined = selectedGroupInfo?.joined ?: false
                            val ownerName = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: "我"
                            val computedCreator = when {
                                (selectedGroupInfo?.adminName?.isNotBlank() == true) -> selectedGroupInfo!!.adminName
                                selectedGroupIntro == ownerName -> ownerName
                                (selectedGroupInfo?.adminName.isNullOrBlank() && (selectedGroupInfo?.memberCount ?: 0) == 0) -> ownerName
                                else -> ""
                            }
                            val isOwner = computedCreator.isNotBlank() && computedCreator.equals(ownerName, ignoreCase = true)
                            
                            if (!isOwner) {
                                Surface(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val ok = CommunityRepositoryProvider.current.setGroupJoin(selectedGroupIntro!!, !joined)
                                                if (ok) {
                                                    selectedGroupInfo = selectedGroupInfo?.copy(
                                                        joined = !joined, 
                                                        memberCount = if (!joined) (selectedGroupInfo?.memberCount ?: 0) + 1 else (selectedGroupInfo?.memberCount ?: 0) - 1
                                                    )
                                                    snackbarHostState.showSnackbar(if (!joined) "已加入" else "已退出")
                                                } else snackbarHostState.showSnackbar("操作失败")
                                            } catch (_: Exception) {
                                                snackbarHostState.showSnackbar("操作失败")
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (joined) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (joined) "退出小组" else "申请加入",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = if (joined) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            
                            
                            
                            // Close button with minimal styling
                            TextButton(
                                onClick = { selectedGroupIntro = null },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("关闭")
                            }
                        }
                    }
                    val showEmbeddedChat = false
                    var chatMessages by remember { mutableStateOf<List<GroupMessage>>(emptyList()) }
                    var chatLoading by remember { mutableStateOf(false) }
                    var chatInput by remember { mutableStateOf("") }
                var chatVoicePath by remember { mutableStateOf<String?>(null) }
                var chatVoiceDuration by remember { mutableStateOf<Int?>(null) }
                var recording by remember { mutableStateOf(false) }
                var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
                val requestAudioPermission = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        try {
                            val file = java.io.File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.3gp")
                            val r = android.media.MediaRecorder()
                            r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                            r.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
                            r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                            r.setOutputFile(file.absolutePath)
                            r.prepare()
                            r.start()
                            recorder = r
                            recording = true
                            chatVoicePath = file.absolutePath
                            chatVoiceDuration = null
                        } catch (_: Exception) {
                            recording = false
                            recorder?.release()
                            recorder = null
                            scope.launch { snackbarHostState.showSnackbar("录音启动失败") }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("未授予录音权限") }
                    }
                }
                    // Modern chat section header
                    if (showEmbeddedChat) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "群聊",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${chatMessages.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    LaunchedEffect(selectedGroupIntro) {
                        if (selectedGroupIntro != null) {
                            chatLoading = true
                            try { chatMessages = CommunityRepositoryProvider.current.getGroupMessages(selectedGroupIntro!!) } catch (_: Exception) { chatMessages = emptyList() } finally { chatLoading = false }
                        }
                    }
                    if (chatLoading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                    } else {
                        if (chatMessages.isEmpty()) {
                            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                    Text("暂无消息", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), verticalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                            itemsIndexed(chatMessages, key = { _, m -> m.id }) { _, m ->
                                val isMyMessage = m.author == "我"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        tonalElevation = if (isMyMessage) 3.dp else 1.dp,
                                        color = if (isMyMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (!isMyMessage) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = m.author.firstOrNull()?.toString() ?: "?",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = m.author,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = android.text.format.DateFormat.format("HH:mm", java.util.Date(m.timestamp)).toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                if (m.recalled) {
                                                    Surface(
                                                        shape = MaterialTheme.shapes.small,
                                                        color = MaterialTheme.colorScheme.errorContainer
                                                    ) {
                                                        Text(
                                                            text = "已撤回",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isMyMessage && System.currentTimeMillis() - m.timestamp < 5*60*1000L) {
                                                    IconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                try {
                                                                    CommunityRepositoryProvider.current.recallGroupMessage(selectedGroupIntro!!, m.id)
                                                                    chatMessages = chatMessages.map { if (it.id == m.id) it.copy(recalled = true) else it }
                                                                } catch (_: Exception) {}
                                                            }
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Undo,
                                                            contentDescription = "撤回",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                            }
                                        }

                                        if (!m.recalled) {
                                            if (m.content.isNotBlank()) {
                                                Text(
                                                    text = m.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isMyMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (m.voiceUrl != null) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                    AssistChip(onClick = {
                                                        try {
                                                            val player = android.media.MediaPlayer()
                                                            player.setDataSource(m.voiceUrl)
                                                            player.setOnCompletionListener { it.release() }
                                                            player.prepare()
                                                            player.start()
                                                        } catch (_: Exception) {
                                                            scope.launch { snackbarHostState.showSnackbar("播放失败") }
                                                        }
                                                    }, label = { Text("播放语音${m.voiceDurationSec?.let { "(${it}s)" } ?: ""}") })
                                                }
                                            }

                                            if (m.images.isNotEmpty()) {
                                                androidx.compose.foundation.lazy.LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                        itemsIndexed(m.images) { _, url ->
                                                            androidx.compose.foundation.Image(
                                                                painter = rememberAsyncImagePainter(url),
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(120.dp)
                                                                    .clip(MaterialTheme.shapes.medium)
                                                                    .clickable { previewImageUrl = url }
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                if (m.mentions.isNotEmpty()) {
                                                    val navCtx = androidx.compose.ui.platform.LocalContext.current
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        m.mentions.forEach { u ->
                                                            Surface(
                                                                onClick = {
                                                                    val intent = android.content.Intent(navCtx, com.example.xinqiao.activity.UserInfoActivity::class.java)
                                                                    intent.putExtra("name", u)
                                                                    navCtx.startActivity(intent)
                                                                },
                                                                shape = MaterialTheme.shapes.small,
                                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                                            ) {
                                                                Text(
                                                                    text = "@$u",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                )
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
                        var chatImages by remember { mutableStateOf<List<String>>(emptyList()) }
                        val pickChatImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> chatImages = uris.map { it.toString() } }
                        if (chatImages.isNotEmpty()) {
                            var previewIndex by remember { mutableStateOf(0) }
                            var previewOpen by remember { mutableStateOf(false) }
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
                                itemsIndexed(chatImages) { i, url ->
                                    androidx.compose.foundation.layout.Box {
                                        androidx.compose.foundation.Image(painter = rememberAsyncImagePainter(url), contentDescription = null, modifier = Modifier.size(72.dp).clickable { previewIndex = i; previewOpen = true })
                                        IconButton(onClick = { chatImages = chatImages.filterIndexed { idx, _ -> idx != i } }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(
                                            Icons.Default.Close, contentDescription = null) }
                                    }
                                }
                            }
                            if (previewOpen) {
                                androidx.compose.ui.window.Dialog(onDismissRequest = { previewOpen = false }) {
                                    androidx.compose.foundation.Image(painter = rememberAsyncImagePainter(chatImages[previewIndex]), contentDescription = null, modifier = Modifier.fillMaxWidth().height(320.dp))
                                }
                            }
                        }
                        if (chatVoicePath != null && !recording) {
                            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "语音${chatVoiceDuration?.let { "(${it}s)" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            try {
                                                val player = android.media.MediaPlayer()
                                                player.setDataSource(chatVoicePath)
                                                player.setOnCompletionListener { it.release() }
                                                player.prepare()
                                                player.start()
                                            } catch (_: Exception) { scope.launch { snackbarHostState.showSnackbar("播放失败") } }
                                        }) { Text("播放") }
                                        TextButton(onClick = { chatVoicePath = null; chatVoiceDuration = null }) { Text("删除") }
                                    }
                                }
                            }
                        }
                        // Modern message input area
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 3.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (chatImages.isNotEmpty()) {
                                    var previewIndex by remember { mutableStateOf(0) }
                                    var previewOpen by remember { mutableStateOf(false) }
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        itemsIndexed(chatImages) { i, url ->
                                            Box {
                                                androidx.compose.foundation.Image(
                                                    painter = rememberAsyncImagePainter(url),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(MaterialTheme.shapes.medium)
                                                        .clickable { previewIndex = i; previewOpen = true }
                                                )
                                                IconButton(
                                                    onClick = { chatImages = chatImages.filterIndexed { idx, _ -> idx != i } },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(20.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.errorContainer,
                                                            shape = CircleShape
                                                        )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (previewOpen) {
                                        androidx.compose.ui.window.Dialog(onDismissRequest = { previewOpen = false }) {
                                            Surface(
                                                shape = MaterialTheme.shapes.large,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column {
                                                    androidx.compose.foundation.Image(
                                                        painter = rememberAsyncImagePainter(chatImages[previewIndex]),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(300.dp)
                                                            .clip(MaterialTheme.shapes.large)
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        TextButton(onClick = { previewOpen = false }) {
                                                            Text("关闭")
                                                        }
                                                        Button(
                                                            onClick = {
                                                                chatImages = chatImages.filterIndexed { idx, _ -> idx != previewIndex }
                                                                previewOpen = false
                                                            }
                                                        ) {
                                                            Text("删除")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { pickChatImages.launch("image/*") },
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            if (!recording) {
                                                requestAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                try {
                                                    recorder?.stop()
                                                    recorder?.release()
                                                    recorder = null
                                                    recording = false
                                                    try {
                                                        val mmr = android.media.MediaMetadataRetriever()
                                                        mmr.setDataSource(chatVoicePath)
                                                        val ms = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                                                        chatVoiceDuration = ms?.let { (it / 1000).coerceAtLeast(1) }
                                                        mmr.release()
                                                    } catch (_: Exception) { chatVoiceDuration = null }
                                                } catch (_: Exception) {
                                                    recording = false
                                                    recorder?.release()
                                                    recorder = null
                                                    scope.launch { snackbarHostState.showSnackbar("录音结束失败") }
                                                }
                                            }
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (recording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = if (recording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    TextField(
                                        value = chatInput,
                                        onValueChange = { chatInput = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("发送消息…") },
                                        shape = MaterialTheme.shapes.large,
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                    
                                    Surface(
                                        onClick = {
                                            val msg = chatInput.trim()
                                            if ((msg.isNotEmpty() || chatImages.isNotEmpty() || chatVoicePath != null) && selectedGroupIntro != null) {
                                                val mentions = Regex("@([A-Za-z0-9_\\u4e00-\\u9fa5]+)").findAll(msg).map { it.groupValues[1] }.toList()
                                                val before = chatMessages
                                                val optimistic = before + GroupMessage(
                                                    id = "tmp"+System.currentTimeMillis(),
                                                    groupName = selectedGroupIntro!!,
                                                    author = "我",
                                                    content = msg,
                                                    images = chatImages,
                                                    mentions = mentions,
                                                    voiceUrl = chatVoicePath,
                                                    voiceDurationSec = chatVoiceDuration,
                                                    timestamp = System.currentTimeMillis(),
                                                    recalled = false
                                                )
                                                chatMessages = optimistic
                                                chatInput = ""
                                                val imagesToSend = chatImages
                                                chatImages = emptyList()
                                                val voiceToSend = chatVoicePath
                                                val voiceDurationToSend = chatVoiceDuration
                                                chatVoicePath = null
                                                chatVoiceDuration = null
                                                scope.launch {
                                                    try {
                                                        val created = CommunityRepositoryProvider.current.postGroupMessage(selectedGroupIntro!!, msg, "我", imagesToSend, mentions, voiceToSend, voiceDurationToSend)
                                                        chatMessages = optimistic.dropLast(1) + created
                                                    } catch (_: Exception) {
                                                        chatMessages = before
                                                        scope.launch { snackbarHostState.showSnackbar("发送失败") }
                                                    }
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }
                        LaunchedEffect(selectedGroupIntro) {
                            val user = com.example.xinqiao.util.AnalysisUtils.readLoginUserName(ctx) ?: "我"
                            try { badges = CommunityRepositoryProvider.current.getBadges(user) } catch (_: Exception) { badges = emptyList() }
                            groupInfoLoading = selectedGroupInfo == null
                            if (groupInfoLoading) {
                                try { selectedGroupInfo = CommunityRepositoryProvider.current.getGroupInfo(selectedGroupIntro!!) } catch (_: Exception) { selectedGroupInfo = null } finally { groupInfoLoading = false }
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text("徽章", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                if (badges.isEmpty()) {
                                    Text("暂无徽章", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 28.dp))
                                } else {
                                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 28.dp)) {
                                        itemsIndexed(badges) { _, b -> AssistChip(onClick = {}, label = { Text(b.name) }) }
                                    }
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("会话", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = {
                                    val intent = android.content.Intent(ctx, com.example.xinqiao.activity.GroupChatActivity::class.java)
                                    intent.putExtra("group", selectedGroupIntro!!)
                                    ctx.startActivity(intent)
                                }) { Text("进入会话") }
                            }
                        }
                    }
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

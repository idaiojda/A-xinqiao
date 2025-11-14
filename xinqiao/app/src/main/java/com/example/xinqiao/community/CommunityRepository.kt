package com.example.xinqiao.community

/**
 * 社区数据接口层：后续可替换为真实后端实现。
 */
interface CommunityRepository {
    suspend fun getGroups(): List<String>
    // 新增：申请加入、创建问题、时间线、健康检查
    suspend fun applyJoin(groupName: String): GroupApplyResult
    suspend fun createGroup(name: String, description: String, schedule: String, capacity: Int, creatorName: String): GroupCreateResult
    suspend fun createQuestion(title: String, content: String): Question
    suspend fun getMyTimeline(): List<TimelineItem>
    suspend fun health(): Health
    // 新增：主题交流区帖子流（支持分类与分页）
    suspend fun getPosts(category: String? = null, page: Int = 0, size: Int = 10): List<ThemePost>
    suspend fun createPost(title: String, content: String, tags: List<String>, images: List<String> = emptyList(), anonymous: Boolean = false): ThemePost

    // 新增：帖子评论（与问答评论区分）
    suspend fun getPostComments(postId: String): List<Comment>
    suspend fun postPostComment(postId: String, content: String, author: String = "我"): Comment

    suspend fun getUserProfile(name: String): UserProfile
    suspend fun setFollow(name: String, follow: Boolean): Boolean
    suspend fun getGroupInfo(name: String): GroupInfo
    suspend fun setGroupJoin(name: String, join: Boolean): Boolean
    suspend fun getUserFavorites(name: String): List<ThemePost>
    suspend fun getSharedGroups(name: String): List<String>
    suspend fun getNotifications(): List<NotificationItem>
    suspend fun markNotificationRead(id: String): Boolean
    suspend fun updatePost(id: String, title: String, content: String, tags: List<String>): ThemePost
    suspend fun deletePost(id: String): Boolean

    // 互助小组：群聊与打卡徽章
    suspend fun getGroupMessages(groupName: String): List<GroupMessage>
    suspend fun postGroupMessage(
        groupName: String,
        content: String,
        author: String = "我",
        images: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        voiceUrl: String? = null,
        voiceDurationSec: Int? = null
    ): GroupMessage
    suspend fun checkIn(groupName: String, userName: String): BadgeAwardResult
    suspend fun getBadges(userName: String): List<Badge>
    suspend fun recallGroupMessage(groupName: String, id: String): Boolean
}

object FakeCommunityRepository : CommunityRepository {
    private val groups = mutableListOf("考研互助小组", "社恐成长圈", "恋爱关系修复")
    private val postStore: MutableList<ThemePost> = mutableListOf(
        ThemePost(
            id = "p1",
            author = "小桥",
            authorAvatar = "",
            isAnonymous = false,
            time = "刚刚",
            title = "夜深时的情绪波动怎么办？",
            content = "最近晚上总是心跳加快、脑子停不下来。尝试了呼吸练习有一点帮助，但还是会被突如其来的焦虑击中。大家有什么实用的办法吗？",
            tags = listOf("夜间情绪", "焦虑"),
            images = listOf(
                "https://trae-api-sg.mchost.guru/api/ide/v1/text_to_image?prompt=calm%20night%20city%20street&image_size=square",
                "https://trae-api-sg.mchost.guru/api/ide/v1/text_to_image?prompt=quiet%20bedroom%20lamp&image_size=square",
                "https://trae-api-sg.mchost.guru/api/ide/v1/text_to_image?prompt=soft%20pillow%20moonlight&image_size=square"
            ),
            voiceDurationSec = null,
            liked = false,
            likeCount = 12,
            commentCount = 5
        ),
        ThemePost(
            id = "p2",
            author = "明月",
            authorAvatar = "",
            isAnonymous = true,
            time = "1 小时前",
            title = "和室友相处的边界感",
            content = "室友总是会进入我的私人空间，虽然不是恶意，但我会紧张。想学习如何更自然地表达界限又不伤害关系。",
            tags = listOf("社交与关系", "边界"),
            images = emptyList(),
            voiceDurationSec = 28,
            liked = true,
            likeCount = 8,
            commentCount = 3
        ),
        ThemePost(
            id = "p3",
            author = "安然",
            authorAvatar = "",
            isAnonymous = false,
            time = "昨天",
            title = "晚间散步的声音",
            content = "录了一段路上的环境音和自己的心情，舒服。",
            tags = listOf("夜间情绪"),
            images = emptyList(),
            voiceDurationSec = 16,
            liked = false,
            likeCount = 15,
            commentCount = 7
        )
    )
    override suspend fun getGroups(): List<String> = groups

    override suspend fun applyJoin(groupName: String): GroupApplyResult {
        val accepted = groups.contains(groupName)
        val msg = if (accepted) "已申请加入：$groupName" else "未找到小组：$groupName"
        return GroupApplyResult(accepted, msg)
    }

    override suspend fun createGroup(name: String, description: String, schedule: String, capacity: Int, creatorName: String): GroupCreateResult {
        return if (name.isBlank()) {
            GroupCreateResult(false, "小组名称不能为空")
        } else if (groups.contains(name)) {
            GroupCreateResult(false, "小组已存在：$name")
        } else {
            groups.add(name)
            val defaultRules = listOf("友善沟通", "禁止外传", "支持鼓励")
            groupInfoMap[name] = GroupInfo(
                name = name,
                memberCount = 1,
                rules = defaultRules,
                joined = true,
                adminName = creatorName,
                frequency = "待设置",
                schedule = schedule
            )
            val mine = sharedGroupsMap[creatorName] ?: emptyList()
            sharedGroupsMap[creatorName] = (mine + name).distinct()
            try {
                CommunityLocalCache.database()?.groupDao()?.upsert(
                    GroupInfoEntity(
                        name = name,
                        memberCount = 1,
                        rulesJson = com.google.gson.Gson().toJson(defaultRules),
                        joined = true,
                        adminName = creatorName,
                        frequency = "待设置",
                        schedule = schedule
                    )
                )
            } catch (_: Exception) { }
            GroupCreateResult(true, "已创建：$name")
        }
    }

    override suspend fun createQuestion(title: String, content: String): Question {
        val id = "q${System.currentTimeMillis()}"
        val q = Question(id, if (title.isNotBlank()) title else "匿名提问", content)
        return q
    }

    override suspend fun getMyTimeline(): List<TimelineItem> {
        val now = System.currentTimeMillis()
        return listOf(
            TimelineItem("checkin", "今日完成冥想 10 分钟", now - 3_600_000L),
            TimelineItem("share", "与组员分享了缓解失眠的音乐", now - 7_200_000L),
            TimelineItem("badge", "获得‘坚持打卡’徽章", now - 86_400_000L)
        )
    }

    override suspend fun health(): Health = Health(true, "OK")

    override suspend fun getPosts(category: String?, page: Int, size: Int): List<ThemePost> {
        val source = if (category.isNullOrBlank()) postStore else postStore.filter { it.tags.any { t -> t.contains(category) } || it.title.contains(category) }
        val from = (page * size).coerceAtMost(source.size)
        val to = (from + size).coerceAtMost(source.size)
        return source.subList(from, to)
    }

    override suspend fun createPost(title: String, content: String, tags: List<String>, images: List<String>, anonymous: Boolean): ThemePost {
        val id = "p" + System.currentTimeMillis()
        val p = ThemePost(
            id = id,
            author = if (anonymous) "匿名用户" else "我",
            authorAvatar = "",
            isAnonymous = anonymous,
            time = "刚刚",
            title = title.ifBlank { "未命名" },
            content = content,
            tags = tags,
            images = images,
            voiceDurationSec = null,
            liked = false,
            likeCount = 0,
            commentCount = 0,
            pendingSync = false
        )
        postStore.add(0, p)
        return p
    }

    // 帖子评论本地模拟
    private val postCommentsMap: MutableMap<String, MutableList<Comment>> = mutableMapOf(
        "p1" to mutableListOf(Comment("pc1", "匿名用户", "看到你在尝试呼吸练习，很棒！")),
        "p2" to mutableListOf(Comment("pc2", "匿名用户", "语音分享好温柔～")),
        "p3" to mutableListOf()
    )
    private val followMap: MutableMap<String, Boolean> = mutableMapOf()
    private val userStats: MutableMap<String, Triple<Int, Int, Int>> = mutableMapOf(
        "小桥" to Triple(12, 108, 36),
        "明月" to Triple(9, 64, 22),
        "安然" to Triple(5, 41, 18)
    )
    private val userFavorites: MutableMap<String, List<String>> = mutableMapOf(
        "小桥" to listOf("p2", "p3"),
        "明月" to listOf("p1"),
        "安然" to listOf("p1", "p2")
    )
    private val sharedGroupsMap: MutableMap<String, List<String>> = mutableMapOf(
        "小桥" to listOf("考研互助小组", "社恐成长圈"),
        "明月" to listOf("社恐成长圈"),
        "安然" to listOf("恋爱关系修复")
    )
    private val groupInfoMap: MutableMap<String, GroupInfo> = mutableMapOf(
        "考研互助小组" to GroupInfo("考研互助小组", 128, listOf("友善沟通", "禁止外传", "支持鼓励"), joined = false, adminName = "小桥", frequency = "每周三次", schedule = "周一/周三/周五 20:00"),
        "社恐成长圈" to GroupInfo("社恐成长圈", 96, listOf("尊重彼此", "积极分享"), joined = false, adminName = "明月", frequency = "每周两次", schedule = "周二/周六 19:30"),
        "恋爱关系修复" to GroupInfo("恋爱关系修复", 74, listOf("理性讨论", "保密隐私"), joined = false, adminName = "安然", frequency = "每周一次", schedule = "周日 21:00")
    )

    override suspend fun getPostComments(postId: String): List<Comment> {
        return postCommentsMap[postId]?.toList() ?: emptyList()
    }

    override suspend fun postPostComment(postId: String, content: String, author: String): Comment {
        val list = postCommentsMap.getOrPut(postId) { mutableListOf() }
        val c = Comment("pc${System.currentTimeMillis()}", author, content)
        list.add(c)
        return c
    }

    override suspend fun getUserProfile(name: String): UserProfile {
        val avatar = ""
        val bio = "热心互助，持续分享情绪管理心得。"
        val following = followMap[name] ?: false
        val (posts, fans, follows) = userStats[name] ?: Triple(0, 0, 0)
        return UserProfile(name, avatar, bio, following, postsCount = posts, followersCount = fans, followingCount = follows)
    }

    override suspend fun setFollow(name: String, follow: Boolean): Boolean {
        followMap[name] = follow
        return true
    }

    override suspend fun getGroupInfo(name: String): GroupInfo {
        return groupInfoMap[name] ?: GroupInfo(name, 0, emptyList(), joined = false, adminName = "", frequency = "", schedule = "")
    }

    override suspend fun setGroupJoin(name: String, join: Boolean): Boolean {
        val info = groupInfoMap.getOrPut(name) { GroupInfo(name, 0, emptyList(), joined = false, adminName = "", frequency = "", schedule = "") }
        groupInfoMap[name] = info.copy(joined = join, memberCount = if (join) info.memberCount + 1 else (info.memberCount - 1).coerceAtLeast(0))
        return true
    }

    override suspend fun getUserFavorites(name: String): List<ThemePost> {
        val ids = userFavorites[name] ?: emptyList()
        return postStore.filter { ids.contains(it.id) }
    }

    override suspend fun getSharedGroups(name: String): List<String> {
        return sharedGroupsMap[name] ?: emptyList()
    }

    override suspend fun getNotifications(): List<NotificationItem> = notificationsList.toList()

    override suspend fun markNotificationRead(id: String): Boolean {
        val idx = notificationsList.indexOfFirst { it.id == id }
        if (idx >= 0) notificationsList[idx] = notificationsList[idx].copy(read = true)
        return true
    }

    override suspend fun updatePost(id: String, title: String, content: String, tags: List<String>): ThemePost {
        val idx = postStore.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val cur = postStore[idx]
            val updated = cur.copy(title = title, content = content, tags = tags)
            postStore[idx] = updated
            return updated
        }
        return ThemePost(id = id, author = "我", isAnonymous = false, time = "刚刚", title = title, content = content, tags = tags)
    }

    override suspend fun deletePost(id: String): Boolean {
        val idx = postStore.indexOfFirst { it.id == id }
        if (idx >= 0) {
            postStore.removeAt(idx)
            return true
        }
        return false
    }

    // --- Group chat & badges ---
    private val groupMessagesMap: MutableMap<String, MutableList<GroupMessage>> = mutableMapOf(
        "考研互助小组" to mutableListOf(
            GroupMessage(
                id = "gm1",
                groupName = "考研互助小组",
                author = "小桥",
                content = "欢迎加入，一起坚持打卡！",
                images = emptyList(),
                mentions = emptyList(),
                timestamp = System.currentTimeMillis() - 3600000L,
                recalled = false
            )
        ),
        "社恐成长圈" to mutableListOf(),
        "恋爱关系修复" to mutableListOf()
    )
    private val userBadgesMap: MutableMap<String, MutableList<Badge>> = mutableMapOf()
    private val userCheckinsMap: MutableMap<String, Int> = mutableMapOf()

    override suspend fun getGroupMessages(groupName: String): List<GroupMessage> {
        return groupMessagesMap[groupName]?.toList() ?: emptyList()
    }

    override suspend fun postGroupMessage(groupName: String, content: String, author: String, images: List<String>, mentions: List<String>, voiceUrl: String?, voiceDurationSec: Int?): GroupMessage {
        val list = groupMessagesMap.getOrPut(groupName) { mutableListOf() }
        val msg = GroupMessage(
            id = "gm" + System.currentTimeMillis(),
            groupName = groupName,
            author = author,
            authorAvatar = "https://trae-api-sg.mchost.guru/api/ide/v1/text_to_image?prompt=${author}头像&image_size=square",
            content = content,
            images = images,
            mentions = mentions,
            voiceUrl = voiceUrl,
            voiceDurationSec = voiceDurationSec,
            timestamp = System.currentTimeMillis(),
            recalled = false
        )
        list.add(msg)
        CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(msg.toEntity()))
        return msg
    }

    override suspend fun checkIn(groupName: String, userName: String): BadgeAwardResult {
        val c = (userCheckinsMap[userName] ?: 0) + 1
        userCheckinsMap[userName] = c
        var badge: Badge? = null
        if (c == 7) {
            badge = Badge("b7", "坚持一周", "连续 7 天打卡")
        } else if (c == 30) {
            badge = Badge("b30", "打卡达人", "30 天打卡")
        }
        if (badge != null) {
            val list = userBadgesMap.getOrPut(userName) { mutableListOf() }
            list.add(badge)
        }
        return BadgeAwardResult(ok = true, message = if (badge != null) "获得徽章：${badge.name}" else "已打卡，第 ${c} 天", badge = badge)
    }

    override suspend fun getBadges(userName: String): List<Badge> {
        val cached = CommunityLocalCache.database()?.badgeDao()?.getByUser(userName)?.map { Badge(it.id, it.name, it.description) } ?: emptyList()
        if (cached.isNotEmpty()) return cached
        return userBadgesMap[userName]?.toList() ?: emptyList()
    }

    override suspend fun recallGroupMessage(groupName: String, id: String): Boolean {
        CommunityLocalCache.database()?.groupChatDao()?.recall(id)
        val list = groupMessagesMap[groupName]
        if (list != null) {
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(recalled = true)
        }
        return true
    }
}

data class GroupApplyResult(val accepted: Boolean, val message: String)
data class GroupCreateResult(val ok: Boolean, val message: String)
data class TimelineItem(val type: String, val text: String, val timestamp: Long)
data class Health(val ok: Boolean, val message: String)
data class UserProfile(val name: String, val avatar: String, val bio: String, val following: Boolean, val postsCount: Int, val followersCount: Int, val followingCount: Int)
data class GroupInfo(val name: String, val memberCount: Int, val rules: List<String>, val joined: Boolean, val adminName: String, val frequency: String, val schedule: String)
data class NotificationItem(val id: String, val title: String, val content: String, val read: Boolean)
    private val notificationsList: MutableList<NotificationItem> = mutableListOf(
        NotificationItem("n1", "评论提醒", "有人回复了你的帖子", false),
        NotificationItem("n2", "系统消息", "社区规则更新", false)
    )

data class GroupMessage(
    val id: String,
    val groupName: String,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val images: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val voiceUrl: String? = null,
    val voiceDurationSec: Int? = null,
    val timestamp: Long,
    val recalled: Boolean = false
)
data class Badge(val id: String, val name: String, val description: String)
data class BadgeAwardResult(val ok: Boolean, val message: String, val badge: Badge?)

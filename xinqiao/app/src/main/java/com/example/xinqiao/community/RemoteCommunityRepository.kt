package com.example.xinqiao.community

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 远程仓库实现：当前以阻塞方式封装，便于后续替换为协程调用。
 */
class RemoteCommunityRepository(
    private val api: CommunityApi
) : CommunityRepository {
    override suspend fun getGroups(q: String?): List<String> {
        return try {
            val remote = api.getGroups(q)
            if (remote.isNotEmpty()) remote else {
                val dao = CommunityLocalCache.database()?.groupDao()
                try { dao?.listJoinedNames() ?: emptyList() } catch (_: Exception) { emptyList() }
            }
        } catch (_: Exception) {
            val dao = CommunityLocalCache.database()?.groupDao()
            try { dao?.listJoinedNames() ?: emptyList() } catch (_: Exception) { emptyList() }
        }
    }

    override suspend fun applyJoin(groupName: String): GroupApplyResult = api.applyJoin(groupName)

    override suspend fun createGroup(name: String, description: String, schedule: String, capacity: Int, creatorName: String): GroupCreateResult {
        return try {
            val res = api.createGroup(CreateGroupRequest(name = name, description = description, schedule = "", capacity = capacity, creator = creatorName))
            try {
                CommunityLocalCache.database()?.groupDao()?.upsert(
                    GroupInfoEntity(
                        name = name,
                        memberCount = 1,
                        rulesJson = com.google.gson.Gson().toJson(listOf("友善沟通", "禁止外传", "支持鼓励")),
                        joined = true,
                        adminName = creatorName,
                        frequency = "",
                        schedule = ""
                    )
                )
            } catch (_: Exception) { }
            res
        } catch (_: Exception) {
            return try {
                CommunityLocalCache.database()?.groupDao()?.upsert(
                    GroupInfoEntity(
                        name = name,
                        memberCount = 1,
                        rulesJson = com.google.gson.Gson().toJson(listOf("友善沟通", "禁止外传", "支持鼓励")),
                        joined = true,
                        adminName = creatorName,
                        frequency = "",
                        schedule = ""
                    )
                )
                GroupCreateResult(true, "已创建：$name")
            } catch (_: Exception) {
                GroupCreateResult(false, "创建失败")
            }
        }
    }

    override suspend fun createQuestion(title: String, content: String): Question =
        api.createQuestion(NewQuestionRequest(title = title, content = content))

    override suspend fun getMyTimeline(): List<TimelineItem> = api.getMyTimeline()

    override suspend fun health(): Health = api.health()

    override suspend fun getPosts(category: String?, page: Int, size: Int, q: String?): List<ThemePost> {
        return try {
            val list = kotlinx.coroutines.withTimeout(3000L) {
                api.getPosts(category = category, page = page, size = size, q = q).map { it.toThemePost() }
            }
            val cleaned = list.filterNot { isSamplePost(it) }
            try { CommunityLocalCache.database()?.postDao()?.upsertAll(cleaned.map { it.toEntity() }) } catch (_: Exception) { }
            cleaned
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getPostComments(postId: String): List<Comment> = try {
        val list = api.getPostComments(postId)
        CommunityLocalCache.database()?.commentDao()?.upsertAll(list.map { CommentEntity(it.id, postId, it.author, it.text, System.currentTimeMillis()) })
        list
    } catch (e: Exception) {
        CommunityLocalCache.database()?.commentDao()?.getByPost(postId)?.map { Comment(it.id, it.author, it.text) } ?: emptyList()
    }

    override suspend fun postPostComment(postId: String, content: String, author: String): Comment =
        api.postPostComment(postId, NewCommentRequest(author = author, text = content))

    override suspend fun createPost(title: String, content: String, tags: List<String>, images: List<String>, anonymous: Boolean): ThemePost {
        return try {
            val created = api.createPost(CreatePostRequest(title = title, content = content, tags = tags, images = images, anonymous = anonymous)).toThemePost()
            CommunityLocalCache.database()?.postDao()?.upsertAll(listOf(created.toEntity()))
            created
        } catch (e: Exception) {
            val local = ThemePost(
                id = "p" + System.currentTimeMillis(),
                author = if (anonymous) "匿名用户" else "我",
                authorAvatar = "",
                isAnonymous = anonymous,
                time = "刚刚",
                title = if (title.isNotBlank()) title else "未命名",
                content = content,
                tags = tags,
                images = images,
                voiceDurationSec = null,
                liked = false,
                likeCount = 0,
                commentCount = 0,
                pendingSync = true,
                bookmarked = false
            )
            CommunityLocalCache.database()?.postDao()?.upsertAll(listOf(local.toEntity()))
            local
        }
    }

    override suspend fun getUserProfile(name: String): UserProfile {
        return try {
            val dto = api.getUserProfile(name)
            val profile = UserProfile(dto.name, dto.avatar, dto.bio, dto.following, dto.postsCount, dto.followersCount, dto.followingCount)
            CommunityLocalCache.database()?.profileDao()?.upsert(UserProfileEntity(profile.name, profile.avatar, profile.bio, profile.following, profile.postsCount, profile.followersCount, profile.followingCount))
            profile
        } catch (e: Exception) {
            CommunityLocalCache.database()?.profileDao()?.get(name)?.let { UserProfile(it.name, it.avatar, it.bio, it.following, it.postsCount, it.followersCount, it.followingCount) } ?: UserProfile(name, "", "", false, 0, 0, 0)
        }
    }

    override suspend fun setFollow(name: String, follow: Boolean): Boolean = try { api.setFollow(name, follow).ok } catch (_: Exception) { false }

    override suspend fun getGroupInfo(name: String): GroupInfo {
        return try {
            val dto = api.getGroupInfo(name)
            val info = GroupInfo(dto.name, dto.memberCount, dto.rules, dto.joined, dto.adminName, dto.frequency, dto.schedule)
            CommunityLocalCache.database()?.groupDao()?.upsert(GroupInfoEntity(info.name, info.memberCount, com.google.gson.Gson().toJson(info.rules), info.joined, info.adminName, info.frequency, info.schedule))
            info
        } catch (e: Exception) {
            CommunityLocalCache.database()?.groupDao()?.get(name)?.let {
                GroupInfo(it.name, it.memberCount, try { com.google.gson.Gson().fromJson(it.rulesJson, java.util.ArrayList::class.java) as List<String> } catch (_: Exception) { emptyList() }, it.joined, it.adminName, it.frequency, it.schedule)
            } ?: GroupInfo(name, 0, emptyList(), false, "", "", "")
        }
    }

    override suspend fun setGroupJoin(name: String, join: Boolean): Boolean = try { api.setGroupJoin(name, join).ok } catch (_: Exception) { false }

    override suspend fun getUserFavorites(name: String): List<ThemePost> = try {
        api.getUserFavorites(name).map { it.toThemePost() }
    } catch (_: Exception) {
        CommunityLocalCache.database()?.postDao()?.getAll()?.map { it.toThemePost() }?.filter { it.bookmarked } ?: emptyList()
    }

    override suspend fun getSharedGroups(name: String): List<String> = try {
        val remote = api.getSharedGroups(name)
        if (remote.isNotEmpty()) remote else {
            CommunityLocalCache.database()?.groupDao()?.listNamesByOwnerOrJoined(name) ?: emptyList()
        }
    } catch (_: Exception) {
        CommunityLocalCache.database()?.groupDao()?.listNamesByOwnerOrJoined(name) ?: emptyList()
    }

    override suspend fun getNotifications(): List<NotificationItem> = try { api.getNotifications().map { NotificationItem(it.id, it.title, it.content, it.read, it.postId) } } catch (_: Exception) { emptyList() }

    override suspend fun markNotificationRead(id: String): Boolean = try { api.markNotificationRead(id).ok } catch (_: Exception) { false }

    override suspend fun updatePost(id: String, title: String, content: String, tags: List<String>): ThemePost = try {
        api.updatePost(id, UpdatePostRequest(title, content, tags)).toThemePost()
    } catch (_: Exception) {
        ThemePost(id = id, author = "我", isAnonymous = false, time = "刚刚", title = title, content = content, tags = tags, pendingSync = true)
    }

    override suspend fun deletePost(id: String): Boolean = try { api.deletePost(id).ok } catch (_: Exception) { false }
    override suspend fun updateGroupInfo(name: String, description: String?, rulesJson: String?, schedule: String?): Boolean {
        return try {
            api.updateGroup(name, UpdateGroupRequest(description, rulesJson, schedule)).ok
        } catch (_: Exception) {
            try {
                CommunityLocalCache.database()?.groupDao()?.upsert(
                    GroupInfoEntity(
                        name = name,
                        memberCount = CommunityLocalCache.database()?.groupDao()?.get(name)?.memberCount ?: 0,
                        rulesJson = rulesJson ?: CommunityLocalCache.database()?.groupDao()?.get(name)?.rulesJson ?: "[]",
                        joined = CommunityLocalCache.database()?.groupDao()?.get(name)?.joined ?: false,
                        adminName = CommunityLocalCache.database()?.groupDao()?.get(name)?.adminName ?: "",
                        frequency = CommunityLocalCache.database()?.groupDao()?.get(name)?.frequency ?: "",
                        schedule = schedule ?: CommunityLocalCache.database()?.groupDao()?.get(name)?.schedule ?: ""
                    )
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }
    override suspend fun getGroupMessages(groupName: String): List<GroupMessage> {
        return try {
            val dao = CommunityLocalCache.database()?.groupChatDao()
            dao?.getByGroup(groupName)?.map { it.toDomain() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun postGroupMessage(groupName: String, content: String, author: String, images: List<String>, mentions: List<String>, voiceUrl: String?, voiceDurationSec: Int?): GroupMessage {
        val msg = GroupMessage(
            id = "gm" + System.currentTimeMillis(),
            groupName = groupName,
            author = author,
            authorAvatar = null,
            content = content,
            images = images,
            mentions = mentions,
            voiceUrl = voiceUrl,
            voiceDurationSec = voiceDurationSec,
            timestamp = System.currentTimeMillis(),
            recalled = false
        )
        try { CommunityLocalCache.database()?.groupChatDao()?.upsertAll(listOf(msg.toEntity())) } catch (_: Exception) {}
        return msg
    }

    override suspend fun checkIn(groupName: String, userName: String): BadgeAwardResult {
        return BadgeAwardResult(ok = true, message = "已打卡", badge = null)
    }

    override suspend fun getBadges(userName: String): List<Badge> {
        return try { CommunityLocalCache.database()?.badgeDao()?.getByUser(userName)?.map { Badge(it.id, it.name, it.description) } ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    override suspend fun recallGroupMessage(groupName: String, id: String): Boolean {
        return try { CommunityLocalCache.database()?.groupChatDao()?.recall(id); true } catch (_: Exception) { false }
    }
}

/** 工厂：提供默认 Retrofit 构建，可在应用初始化时传入自定义 baseUrl。 */
object CommunityServiceFactory {
    fun create(baseUrl: String): CommunityApi {
        // Retrofit 要求 baseUrl 以 "/" 结尾，否则会抛 IllegalArgumentException
        val normalized = if (baseUrl.endsWith("/")) baseUrl else baseUrl + "/"
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(CommunityApi::class.java)
    }
}

private fun PostDto.toThemePost(): ThemePost = ThemePost(
    id = id,
    author = author,
    isAnonymous = anonymous,
    time = time,
    title = title,
    content = content,
    tags = tags,
    images = emptyList(),
    voiceDurationSec = voiceDurationSec,
    pendingSync = false
)

private fun isSamplePost(p: ThemePost): Boolean {
    val authors = setOf("小桥", "明月", "安然")
    if (authors.contains(p.author)) return true
    val patterns = listOf(
        "夜深时的情绪波动",
        "和室友相处的边界感",
        "晚间散步的声音"
    )
    return patterns.any { pat ->
        p.title.contains(pat, ignoreCase = true) || p.content.contains(pat, ignoreCase = true)
    }
}

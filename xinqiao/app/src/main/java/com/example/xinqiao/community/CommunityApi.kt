package com.example.xinqiao.community

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Query

/**
 * 远程社区接口约定（可与后端对齐后完善）。
 */
interface CommunityApi {
    @GET("community/groups")
    suspend fun getGroups(): List<String>

    // 申请加入指定小组
    @POST("community/groups/{name}/apply")
    suspend fun applyJoin(@Path("name") name: String): GroupApplyResult

    // 创建小组（后端支持后将返回创建结果）
    @POST("community/groups")
    suspend fun createGroup(@Body req: CreateGroupRequest): GroupCreateResult

    // 创建匿名问题
    @POST("community/questions")
    suspend fun createQuestion(@Body req: NewQuestionRequest): Question

    // 我的时间线
    @GET("community/timeline")
    suspend fun getMyTimeline(): List<TimelineItem>

    // 健康检查
    @GET("community/health")
    suspend fun health(): Health

    // 主题交流区帖子流
    @GET("community/posts")
    suspend fun getPosts(
        @Query("category") category: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<PostDto>

    @POST("community/posts")
    suspend fun createPost(@Body req: CreatePostRequest): PostDto

    // 新增：帖子评论端点（与问答评论区分）
    @GET("community/posts/{id}/comments")
    suspend fun getPostComments(@Path("id") id: String): List<Comment>

    @POST("community/posts/{id}/comments")
    suspend fun postPostComment(@Path("id") id: String, @Body req: NewCommentRequest): Comment
    @GET("community/users/{name}")
    suspend fun getUserProfile(@Path("name") name: String): UserProfileDto

    @POST("community/users/{name}/follow")
    suspend fun setFollow(@Path("name") name: String, @Query("on") follow: Boolean): FollowResult

    @GET("community/groups/{name}")
    suspend fun getGroupInfo(@Path("name") name: String): GroupInfoDto
    @POST("community/groups/{name}/join")
    suspend fun setGroupJoin(@Path("name") name: String, @Query("on") join: Boolean): FollowResult

    @GET("community/users/{name}/favorites")
    suspend fun getUserFavorites(@Path("name") name: String): List<PostDto>

    @GET("community/users/{name}/shared_groups")
    suspend fun getSharedGroups(@Path("name") name: String): List<String>

    @GET("community/notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("community/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): FollowResult

    @PATCH("community/posts/{id}")
    suspend fun updatePost(@Path("id") id: String, @Body req: UpdatePostRequest): PostDto

    @DELETE("community/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): FollowResult
}

data class NewCommentRequest(val author: String, val text: String)
data class NewQuestionRequest(val title: String, val content: String)
data class CreateGroupRequest(val name: String, val description: String, val schedule: String, val capacity: Int)
data class CreatePostRequest(val title: String, val content: String, val tags: List<String>, val images: List<String> = emptyList(), val anonymous: Boolean = false)
data class UpdatePostRequest(val title: String, val content: String, val tags: List<String>)
data class UserProfileDto(val name: String, val avatar: String, val bio: String, val following: Boolean, val postsCount: Int, val followersCount: Int, val followingCount: Int)
data class FollowResult(val ok: Boolean)
data class GroupInfoDto(val name: String, val memberCount: Int, val rules: List<String>, val joined: Boolean, val adminName: String, val frequency: String, val schedule: String)
data class NotificationDto(val id: String, val title: String, val content: String, val read: Boolean)

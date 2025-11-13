package com.example.xinqiao.community

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val author: String,
    val authorAvatar: String,
    val isAnonymous: Boolean,
    val time: String,
    val title: String,
    val content: String,
    val tagsJson: String,
    val imagesJson: String,
    val voiceDurationSec: Int?,
    val liked: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val pendingSync: Boolean,
    val bookmarked: Boolean
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val author: String,
    val text: String,
    val createdAt: Long
)

@Entity(tableName = "profiles")
data class UserProfileEntity(
    @PrimaryKey val name: String,
    val avatar: String,
    val bio: String,
    val following: Boolean,
    val postsCount: Int,
    val followersCount: Int,
    val followingCount: Int
)

@Entity(tableName = "groups")
data class GroupInfoEntity(
    @PrimaryKey val name: String,
    val memberCount: Int,
    val rulesJson: String,
    val joined: Boolean,
    val adminName: String,
    val frequency: String,
    val schedule: String
)

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PostEntity>)

    @Query("SELECT * FROM posts ORDER BY id DESC")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PostEntity?
}

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CommentEntity>)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    suspend fun getByPost(postId: String): List<CommentEntity>
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: UserProfileEntity)

    @Query("SELECT * FROM profiles WHERE name = :name LIMIT 1")
    suspend fun get(name: String): UserProfileEntity?
}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroupInfoEntity)

    @Query("SELECT * FROM groups WHERE name = :name LIMIT 1")
    suspend fun get(name: String): GroupInfoEntity?
}

@Database(
    entities = [PostEntity::class, CommentEntity::class, UserProfileEntity::class, GroupInfoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CommunityDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun profileDao(): ProfileDao
    abstract fun groupDao(): GroupDao
}

object CommunityLocalCache {
    @Volatile
    private var db: CommunityDatabase? = null

    fun init(context: Context) {
        if (db == null) {
            db = Room.databaseBuilder(context.applicationContext, CommunityDatabase::class.java, "community.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    fun database(): CommunityDatabase? = db
}

private fun List<String>.toJson(): String = try {
    com.google.gson.Gson().toJson(this)
} catch (_: Exception) { "[]" }

private fun String.fromJsonList(): List<String> = try {
    val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, String::class.java).type
    com.google.gson.Gson().fromJson(this, type) ?: emptyList()
} catch (_: Exception) { emptyList() }

fun ThemePost.toEntity(): PostEntity = PostEntity(
    id = id,
    author = author,
    authorAvatar = authorAvatar,
    isAnonymous = isAnonymous,
    time = time,
    title = title,
    content = content,
    tagsJson = tags.toJson(),
    imagesJson = images.toJson(),
    voiceDurationSec = voiceDurationSec,
    liked = liked,
    likeCount = likeCount,
    commentCount = commentCount,
    pendingSync = pendingSync,
    bookmarked = bookmarked
)

fun PostEntity.toThemePost(): ThemePost = ThemePost(
    id = id,
    author = author,
    authorAvatar = authorAvatar,
    isAnonymous = isAnonymous,
    time = time,
    title = title,
    content = content,
    tags = tagsJson.fromJsonList(),
    images = imagesJson.fromJsonList(),
    voiceDurationSec = voiceDurationSec,
    liked = liked,
    likeCount = likeCount,
    commentCount = commentCount,
    pendingSync = pendingSync,
    bookmarked = bookmarked
)


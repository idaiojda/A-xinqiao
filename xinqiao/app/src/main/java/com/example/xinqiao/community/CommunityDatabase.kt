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

@Entity(tableName = "group_messages")
data class GroupMessageEntity(
    @PrimaryKey val id: String,
    val groupName: String,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val imagesJson: String,
    val mentionsJson: String,
    val voiceUrl: String?,
    val voiceDurationSec: Int?,
    val timestamp: Long,
    val recalled: Boolean
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val userName: String,
    val name: String,
    val description: String,
    val awardedAt: Long
)

@Entity(tableName = "checkin_status")
data class CheckinStatusEntity(
    @PrimaryKey val userName: String,
    val lastDate: String,
    val streakDays: Int
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

    @Query("SELECT name FROM groups WHERE adminName = :user OR joined = 1")
    suspend fun listNamesByOwnerOrJoined(user: String): List<String>

    @Query("SELECT name FROM groups WHERE joined = 1")
    suspend fun listJoinedNames(): List<String>

    @Query("SELECT name FROM groups WHERE adminName = :user")
    suspend fun listNamesByOwner(user: String): List<String>

    @Query("SELECT DISTINCT adminName FROM groups WHERE adminName IS NOT NULL AND adminName <> ''")
    suspend fun listAdminNames(): List<String>
}

@Dao
interface GroupChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<GroupMessageEntity>)

    @Query("SELECT * FROM group_messages WHERE groupName = :group ORDER BY timestamp ASC")
    suspend fun getByGroup(group: String): List<GroupMessageEntity>

    @Query("UPDATE group_messages SET recalled = 1 WHERE id = :id")
    suspend fun recall(id: String): Int

    @Query("DELETE FROM group_messages WHERE id = :id")
    suspend fun delete(id: String): Int
}

@Dao
interface BadgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BadgeEntity>)

    @Query("SELECT * FROM badges WHERE userName = :user ORDER BY awardedAt DESC")
    suspend fun getByUser(user: String): List<BadgeEntity>
}

@Dao
interface CheckinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: CheckinStatusEntity)

    @Query("SELECT * FROM checkin_status WHERE userName = :user LIMIT 1")
    suspend fun get(user: String): CheckinStatusEntity?
}

@Database(
    entities = [PostEntity::class, CommentEntity::class, UserProfileEntity::class, GroupInfoEntity::class, GroupMessageEntity::class, BadgeEntity::class, CheckinStatusEntity::class],
    version = 4,
    exportSchema = false
)
abstract class CommunityDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun profileDao(): ProfileDao
    abstract fun groupDao(): GroupDao
    abstract fun groupChatDao(): GroupChatDao
    abstract fun badgeDao(): BadgeDao
    abstract fun checkinDao(): CheckinDao
}

object CommunityLocalCache {
    @Volatile
    private var db: CommunityDatabase? = null

    fun init(context: Context) {
        if (db == null) {
            val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
                override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                    try { database.execSQL("ALTER TABLE group_messages ADD COLUMN authorAvatar TEXT") } catch (_: Exception) {}
                }
            }
            db = Room.databaseBuilder(context.applicationContext, CommunityDatabase::class.java, "community.db")
                .addMigrations(MIGRATION_3_4)
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

fun GroupMessageEntity.toDomain(): GroupMessage = GroupMessage(id = id, groupName = groupName, author = author, authorAvatar = authorAvatar, content = content, images = imagesJson.fromJsonList(), mentions = mentionsJson.fromJsonList(), voiceUrl = voiceUrl, voiceDurationSec = voiceDurationSec, timestamp = timestamp, recalled = recalled)

fun GroupMessage.toEntity(): GroupMessageEntity = GroupMessageEntity(id = id, groupName = groupName, author = author, authorAvatar = authorAvatar, content = content, imagesJson = images.toJson(), mentionsJson = mentions.toJson(), voiceUrl = voiceUrl, voiceDurationSec = voiceDurationSec, timestamp = timestamp, recalled = recalled)

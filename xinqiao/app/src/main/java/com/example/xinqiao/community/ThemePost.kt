package com.example.xinqiao.community

data class ThemePost(
    val id: String,
    val author: String, // 显示名称（昵称或用户名）
    val authorUsername: String = "", // 原始用户名，用于权限判断
    val authorAvatar: String = "",
    val isAnonymous: Boolean,
    val time: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val images: List<String> = emptyList(),
    val voiceDurationSec: Int? = null,
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val pendingSync: Boolean = false,
    val bookmarked: Boolean = false,
    val reviewStatus: String = "APPROVED" // PENDING, APPROVED, REJECTED
)

package com.example.xinqiao.community

data class ThemePost(
    val id: String,
    val author: String,
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
    val bookmarked: Boolean = false
)

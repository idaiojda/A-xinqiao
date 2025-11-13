package com.example.xinqiao.community

/**
 * 后端帖子流数据模型（与后端 PostDto.java 字段一致）。
 */
data class PostDto(
    val id: String,
    val author: String,
    val anonymous: Boolean,
    val time: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val images: List<String> = emptyList(),
    val voiceDurationSec: Int? = null
)


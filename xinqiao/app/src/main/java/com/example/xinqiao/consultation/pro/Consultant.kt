package com.example.xinqiao.consultation.pro

import androidx.compose.runtime.Immutable

@Immutable
data class Consultant(
    val id: String,
    val name: String,
    val title: String,
    val avatarUrl: String?,
    val certified: Boolean,
    val skills: List<String>,
    val rating: Double,
    val consultCount: Int,
    val price: Int,
    val priceText: Int = 0,
    val priceVoice: Int = 0,
    val priceVideo: Int = 0,
    val durationMinutes: Int,
    val defaultMode: String,
    val city: String?
)

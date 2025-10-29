package com.example.xinqiao.consultation.pro

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
    val durationMinutes: Int,
    val defaultMode: String,
    val city: String?
)

package com.example.xinqiao.consultation.pro

data class CityDict(
    val tabs: List<CityTab> = emptyList()
)

data class CityTab(
    val label: String,
    val groups: List<CityGroup> = emptyList(),
    val cities: List<String> = emptyList()
)

data class CityGroup(
    val label: String,
    val cities: List<String>
)


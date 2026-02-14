package com.example.yasuwidget.domain.model

/**
 * 駅情報（駅ID + 表示名 + 座標）
 */
data class StationInfo(
    val id: String,
    val displayName: String,
    val location: GeoPoint
)

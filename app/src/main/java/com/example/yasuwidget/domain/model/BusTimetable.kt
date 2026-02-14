package com.example.yasuwidget.domain.model

/**
 * バス時刻表のドメインモデル（パース済み）
 */
data class BusTimetable(
    val routeName: String,
    val toYasu: DirectionTimetable,
    val toMurata: DirectionTimetable
)

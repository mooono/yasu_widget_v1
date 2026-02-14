package com.example.yasuwidget.domain.model

/**
 * 電車時刻表のドメインモデル（パース済み）
 */
data class TrainTimetable(
    val stations: Map<String, StationTimetable>
)

data class StationTimetable(
    val name: String,
    val lines: Map<String, LineTimetable>
)

data class LineTimetable(
    val name: String,
    val up: DirectionTimetable,
    val down: DirectionTimetable
)

data class DirectionTimetable(
    val weekday: List<Departure>,
    val holiday: List<Departure>
)

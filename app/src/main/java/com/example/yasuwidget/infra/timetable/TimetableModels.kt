package com.example.yasuwidget.infra.timetable

import kotlinx.serialization.Serializable

/**
 * JSON data structures for timetable files
 * DATA-REQ-001, DATA-REQ-002
 */

@Serializable
data class DepartureJson(
    val time: String,  // HH:mm format
    val destination: String? = null
)

@Serializable
data class DirectionTimetableJson(
    val weekday: List<DepartureJson> = emptyList(),
    val holiday: List<DepartureJson> = emptyList()
)

@Serializable
data class LineInfoJson(
    val name: String,
    val up: DirectionTimetableJson,
    val down: DirectionTimetableJson
)

@Serializable
data class StationInfoJson(
    val name: String,
    val lines: Map<String, LineInfoJson>
)

@Serializable
data class TrainTimetableJson(
    val stations: Map<String, StationInfoJson>
)

@Serializable
data class BusTimetableJson(
    val route_name: String,
    val to_yasu: DirectionTimetableJson,
    val to_murata: DirectionTimetableJson,
    val notes: List<String> = emptyList()
)

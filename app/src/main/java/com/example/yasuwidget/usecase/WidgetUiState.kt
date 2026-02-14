package com.example.yasuwidget.usecase

import com.example.yasuwidget.domain.model.DisplayMode
import kotlinx.serialization.Serializable

/**
 * Single source of truth for Widget UI state
 * Presentation layer uses only this model
 */
@Serializable
data class WidgetUiState(
    val mode: DisplayMode,
    val headerTitle: String,
    val train: TrainSection?,
    val bus: BusSection?,
    val lastUpdatedAtText: String,
    val statusMessage: String? = null
)

@Serializable
data class TrainSection(
    val lineName: String,
    val upDepartures: List<DepartureDisplay>,
    val downDepartures: List<DepartureDisplay>
)

@Serializable
data class BusSection(
    val direction: String,
    val departures: List<DepartureDisplay>
)

@Serializable
data class DepartureDisplay(
    val time: String,  // HH:mm format
    val destination: String?
)

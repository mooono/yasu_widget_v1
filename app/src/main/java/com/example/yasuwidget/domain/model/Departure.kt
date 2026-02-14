package com.example.yasuwidget.domain.model

import java.time.LocalTime

/**
 * Single departure entry
 */
data class Departure(
    val time: LocalTime,
    val destination: String? = null
)

/**
 * Train departures (up and down directions)
 * SYS-REQ-001: Display next 2 departures each direction
 */
data class TrainDepartures(
    val up: List<Departure>,
    val down: List<Departure>
)

/**
 * Bus departures (single direction)
 * SYS-REQ-002: Display next 2 departures
 */
data class BusDepartures(
    val list: List<Departure>
)

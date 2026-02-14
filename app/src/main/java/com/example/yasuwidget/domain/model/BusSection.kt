package com.example.yasuwidget.domain.model

/**
 * Widget上のバス表示セクション
 */
data class BusSection(
    val directionLabel: String,
    val departures: List<Departure>
)

package com.example.yasuwidget.domain.model

/**
 * Widget上の電車表示セクション
 */
data class TrainSection(
    val stationName: String,
    val lineName: String,
    val up: List<Departure>,
    val down: List<Departure>
)

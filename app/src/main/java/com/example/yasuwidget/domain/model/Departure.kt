package com.example.yasuwidget.domain.model

import java.time.LocalTime

/**
 * 1便の発車情報
 */
data class Departure(
    val time: LocalTime,
    val destination: String,
    val trainType: String = "",
    val via: String = ""
)

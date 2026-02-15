package com.example.yasuwidget.domain.model

import java.time.LocalTime

/**
 * Widget描画の単一入力となるUI状態モデル
 * 描画はこのモデルのみを入力として行い、描画側で業務判定を行わない
 */
data class WidgetUiState(
    val mode: DisplayMode,
    val headerTitle: String,
    val train: TrainSection?,
    val bus: BusSection?,
    val lastUpdatedAtText: String,
    val statusMessage: String?,
    val currentTime: LocalTime
)

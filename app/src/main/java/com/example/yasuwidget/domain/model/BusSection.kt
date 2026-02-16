package com.example.yasuwidget.domain.model

/**
 * Widget上のバス表示セクション（1カラム: 発時間順に全便を表示）
 *
 * - 村田発モード: 行き先（野洲駅行 / 野洲駅北口行）を表示
 * - 野洲駅発モード: 発車場所（野洲駅発 / 野洲駅北口発）を表示
 */
data class BusSection(
    val busStopName: String,
    val departures: List<BusDeparture>
)

/**
 * バス1便の表示情報（発車情報 + 表示用ラベル）
 */
data class BusDeparture(
    val departure: Departure,
    val label: String
)

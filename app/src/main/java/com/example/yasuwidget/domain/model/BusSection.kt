package com.example.yasuwidget.domain.model

/**
 * Widget上のバス表示セクション（2カラム: 野洲駅系 / 北口系）
 *
 * - 野洲駅発モード: 左=野洲駅発, 右=北口発
 * - 村田発モード:   左=野洲駅行, 右=北口行
 */
data class BusSection(
    val busStopName: String,
    val yasuLabel: String,
    val kitaguchiLabel: String,
    val yasuDepartures: List<Departure>,
    val kitaguchiDepartures: List<Departure>
)

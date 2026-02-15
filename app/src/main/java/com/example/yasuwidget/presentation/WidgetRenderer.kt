package com.example.yasuwidget.presentation

import android.view.View
import android.widget.RemoteViews
import com.example.yasuwidget.R
import com.example.yasuwidget.domain.model.Departure
import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.WidgetUiState
import com.example.yasuwidget.domain.service.MinutesUntilCalculator
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * WidgetUiState から RemoteViews への描画
 * 描画はWidgetUiStateのみを入力とし、業務判定を行わない
 * 電光掲示板風: 左=上り / 右=下り、1列車1行、種別バッジ付き
 */
object WidgetRenderer {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    /** 列車行のビューID群 */
    private data class TrainRowIds(
        val typeId: Int,
        val timeId: Int,
        val destId: Int,
        val rowId: Int,
        val minutesId: Int,
    )

    /** バス行のビューID群 */
    private data class BusRowIds(
        val timeId: Int,
        val viaId: Int,
        val rowId: Int,
        val minutesId: Int,
    )

    /** 上り各行のビューID（種別バッジ, 時刻, 行先, 行コンテナ, 残り分数） */
    private val UP_ROW_IDS = listOf(
        TrainRowIds(R.id.train_up1_type, R.id.train_up1_time, R.id.train_up1_dest, R.id.row_train_up1, R.id.train_up1_minutes),
        TrainRowIds(R.id.train_up2_type, R.id.train_up2_time, R.id.train_up2_dest, R.id.row_train_up2, R.id.train_up2_minutes),
        TrainRowIds(R.id.train_up3_type, R.id.train_up3_time, R.id.train_up3_dest, R.id.row_train_up3, R.id.train_up3_minutes),
    )

    /** 下り各行のビューID */
    private val DOWN_ROW_IDS = listOf(
        TrainRowIds(R.id.train_down1_type, R.id.train_down1_time, R.id.train_down1_dest, R.id.row_train_down1, R.id.train_down1_minutes),
        TrainRowIds(R.id.train_down2_type, R.id.train_down2_time, R.id.train_down2_dest, R.id.row_train_down2, R.id.train_down2_minutes),
        TrainRowIds(R.id.train_down3_type, R.id.train_down3_time, R.id.train_down3_dest, R.id.row_train_down3, R.id.train_down3_minutes),
    )

    /** バス野洲駅系各行のビューID（時刻, 経由, 行コンテナ, 残り分数） */
    private val BUS_YASU_IDS = listOf(
        BusRowIds(R.id.bus_yasu1_time, R.id.bus_yasu1_via, R.id.row_bus_yasu1, R.id.bus_yasu1_minutes),
        BusRowIds(R.id.bus_yasu2_time, R.id.bus_yasu2_via, R.id.row_bus_yasu2, R.id.bus_yasu2_minutes),
        BusRowIds(R.id.bus_yasu3_time, R.id.bus_yasu3_via, R.id.row_bus_yasu3, R.id.bus_yasu3_minutes),
    )

    /** バス北口系各行のビューID（時刻, 経由, 行コンテナ, 残り分数） */
    private val BUS_KITA_IDS = listOf(
        BusRowIds(R.id.bus_kita1_time, R.id.bus_kita1_via, R.id.row_bus_kita1, R.id.bus_kita1_minutes),
        BusRowIds(R.id.bus_kita2_time, R.id.bus_kita2_via, R.id.row_bus_kita2, R.id.bus_kita2_minutes),
        BusRowIds(R.id.bus_kita3_time, R.id.bus_kita3_via, R.id.row_bus_kita3, R.id.bus_kita3_minutes),
    )

    /**
     * WidgetUiState を RemoteViews に描画する
     */
    fun render(packageName: String, state: WidgetUiState): RemoteViews {
        val views = RemoteViews(packageName, R.layout.widget_transit)

        // ヘッダー
        views.setTextViewText(R.id.text_header_title, state.headerTitle)
        views.setTextViewText(R.id.text_last_updated, state.lastUpdatedAtText)

        // ステータスメッセージ（SYS-REQ-042/043）
        if (state.statusMessage != null) {
            views.setTextViewText(R.id.text_status_message, state.statusMessage)
            views.setViewVisibility(R.id.text_status_message, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.text_status_message, View.GONE)
        }

        // 表示モードに応じたセクション表示/非表示
        // バスは全モードで常に表示
        views.setViewVisibility(R.id.section_bus, View.VISIBLE)
        when (state.mode) {
            DisplayMode.TRAIN_ONLY -> {
                views.setViewVisibility(R.id.section_train, View.VISIBLE)
                views.setViewVisibility(R.id.text_line_name, View.VISIBLE)
                views.setViewVisibility(R.id.section_divider, View.VISIBLE)
            }
            DisplayMode.TRAIN_AND_BUS -> {
                views.setViewVisibility(R.id.section_train, View.VISIBLE)
                views.setViewVisibility(R.id.text_line_name, View.GONE)
                views.setViewVisibility(R.id.section_divider, View.VISIBLE)
            }
            DisplayMode.BUS_ONLY -> {
                views.setViewVisibility(R.id.section_train, View.GONE)
                views.setViewVisibility(R.id.text_line_name, View.GONE)
                views.setViewVisibility(R.id.section_divider, View.GONE)
            }
        }

        // 電車セクション: 各行に1列車ずつ描画
        val train = state.train
        if (train != null) {
            views.setTextViewText(R.id.text_line_name, train.lineName)
            renderTrainRows(views, train.up, UP_ROW_IDS, state.currentTime)
            renderTrainRows(views, train.down, DOWN_ROW_IDS, state.currentTime)
        } else {
            renderTrainRows(views, emptyList(), UP_ROW_IDS, state.currentTime)
            renderTrainRows(views, emptyList(), DOWN_ROW_IDS, state.currentTime)
        }

        // バスセクション（ヘッダー + 2カラム: 野洲駅系 / 北口系）
        val bus = state.bus
        if (bus != null) {
            views.setTextViewText(R.id.text_bus_stop_name, bus.busStopName)
            views.setTextViewText(R.id.text_bus_yasu_label, bus.yasuLabel)
            views.setTextViewText(R.id.text_bus_kita_label, bus.kitaguchiLabel)
            renderBusRows(views, bus.yasuDepartures, BUS_YASU_IDS, state.currentTime)
            renderBusRows(views, bus.kitaguchiDepartures, BUS_KITA_IDS, state.currentTime)
        } else {
            views.setTextViewText(R.id.text_bus_stop_name, "")
            renderBusRows(views, emptyList(), BUS_YASU_IDS, state.currentTime)
            renderBusRows(views, emptyList(), BUS_KITA_IDS, state.currentTime)
        }

        return views
    }

    /**
     * 列車行を描画する
     * departures のサイズに応じて行の表示/非表示を制御
     * 時刻と行先を別々のTextViewに描画し、残り分数を表示する
     */
    private fun renderTrainRows(
        views: RemoteViews,
        departures: List<Departure>,
        rowIds: List<TrainRowIds>,
        currentTime: LocalTime
    ) {
        for ((index, ids) in rowIds.withIndex()) {
            if (index < departures.size) {
                val dep = departures[index]
                views.setViewVisibility(ids.rowId, View.VISIBLE)
                views.setTextViewText(ids.typeId, dep.trainType.ifEmpty { "　" })
                views.setInt(ids.typeId, "setBackgroundResource", badgeDrawable(dep.trainType))
                views.setTextViewText(ids.timeId, dep.time.format(TIME_FORMATTER))
                views.setTextViewText(ids.destId, dep.destination.ifEmpty { "" })
                val minutesUntil = MinutesUntilCalculator.calculate(currentTime, dep.time)
                views.setTextViewText(ids.minutesId, MinutesUntilCalculator.formatText(minutesUntil))
            } else {
                views.setViewVisibility(ids.rowId, View.INVISIBLE)
            }
        }
    }

    /**
     * 列車種別に対応するバッジ背景 Drawable を返す
     * 普通=黒, 快速=オレンジ, 新快速=青
     */
    private fun badgeDrawable(trainType: String): Int = when (trainType) {
        "新快速" -> R.drawable.badge_special_rapid
        "快速" -> R.drawable.badge_rapid
        else -> R.drawable.badge_local
    }

    /**
     * バス行を描画する
     * departures のサイズに応じて行の表示/非表示を制御
     * 残り分数を表示する
     */
    private fun renderBusRows(
        views: RemoteViews,
        departures: List<Departure>,
        rowIds: List<BusRowIds>,
        currentTime: LocalTime
    ) {
        for ((index, ids) in rowIds.withIndex()) {
            if (index < departures.size) {
                val dep = departures[index]
                val time = dep.time.format(TIME_FORMATTER)
                val via = if (dep.via.isNotEmpty()) dep.via else ""
                views.setTextViewText(ids.timeId, time)
                views.setTextViewText(ids.viaId, via)
                val minutesUntil = MinutesUntilCalculator.calculate(currentTime, dep.time)
                views.setTextViewText(ids.minutesId, MinutesUntilCalculator.formatText(minutesUntil))
                views.setViewVisibility(ids.rowId, View.VISIBLE)
            } else {
                views.setTextViewText(ids.timeId, "---")
                views.setTextViewText(ids.viaId, "")
                views.setTextViewText(ids.minutesId, "")
                views.setViewVisibility(ids.rowId, View.VISIBLE)
            }
        }
    }
}

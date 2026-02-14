package com.example.yasuwidget.presentation

import android.view.View
import android.widget.RemoteViews
import com.example.yasuwidget.R
import com.example.yasuwidget.domain.model.Departure
import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.WidgetUiState
import java.time.format.DateTimeFormatter

/**
 * WidgetUiState から RemoteViews への描画
 * 描画はWidgetUiStateのみを入力とし、業務判定を行わない
 */
object WidgetRenderer {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

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
        when (state.mode) {
            DisplayMode.TRAIN_ONLY -> {
                views.setViewVisibility(R.id.section_train, View.VISIBLE)
                views.setViewVisibility(R.id.section_bus, View.GONE)
                views.setViewVisibility(R.id.text_line_name, View.VISIBLE)
            }
            DisplayMode.TRAIN_AND_BUS -> {
                views.setViewVisibility(R.id.section_train, View.VISIBLE)
                views.setViewVisibility(R.id.section_bus, View.VISIBLE)
                views.setViewVisibility(R.id.text_line_name, View.GONE)
            }
            DisplayMode.BUS_ONLY -> {
                views.setViewVisibility(R.id.section_train, View.GONE)
                views.setViewVisibility(R.id.section_bus, View.VISIBLE)
                views.setViewVisibility(R.id.text_line_name, View.GONE)
            }
        }

        // 電車セクション
        val train = state.train
        if (train != null) {
            views.setTextViewText(R.id.text_line_name, train.lineName)
            views.setTextViewText(R.id.text_train_up, formatDepartures(train.up))
            views.setTextViewText(R.id.text_train_down, formatDepartures(train.down))
        } else {
            views.setTextViewText(R.id.text_train_up, "---")
            views.setTextViewText(R.id.text_train_down, "---")
        }

        // バスセクション
        val bus = state.bus
        if (bus != null) {
            views.setTextViewText(R.id.text_bus_label, "Bus → ${bus.directionLabel}")
            views.setTextViewText(R.id.text_bus_departures, formatDepartures(bus.departures))
        } else {
            views.setTextViewText(R.id.text_bus_departures, "---")
        }

        return views
    }

    /**
     * 発車時刻リストを表示文字列にフォーマットする
     * 例: "07:12 京都 / 07:25 京都"
     */
    private fun formatDepartures(departures: List<Departure>): String {
        if (departures.isEmpty()) return "---"

        return departures.joinToString(" / ") { dep ->
            val time = dep.time.format(TIME_FORMATTER)
            val parts = buildList {
                add(time)
                if (dep.trainType.isNotEmpty()) add(dep.trainType)
                if (dep.destination.isNotEmpty()) add(dep.destination)
            }
            parts.joinToString(" ")
        }
    }
}

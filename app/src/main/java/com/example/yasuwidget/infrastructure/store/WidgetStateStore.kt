package com.example.yasuwidget.infrastructure.store

import android.content.Context
import android.content.SharedPreferences
import com.example.yasuwidget.domain.model.WidgetUiState

/**
 * Widget状態の永続化（NFR-002）
 *
 * DataStore キー:
 * - last_updated_at_epoch_millis
 * - last_rendered_ui_state_json
 * - pinned_station_id
 */
class WidgetStateStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "yasu_widget_prefs"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at_epoch_millis"
        private const val KEY_LAST_UI_STATE_JSON = "last_rendered_ui_state_json"
        private const val KEY_PINNED_STATION_ID = "pinned_station_id"
        private const val KEY_CACHED_LAT = "cached_latitude"
        private const val KEY_CACHED_LON = "cached_longitude"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- 最終更新時刻 ---
    var lastUpdatedAtEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_UPDATED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATED_AT, value).apply()

    // --- 最終描画UI状態（JSON） ---
    var lastRenderedUiStateJson: String?
        get() = prefs.getString(KEY_LAST_UI_STATE_JSON, null)
        set(value) = prefs.edit().putString(KEY_LAST_UI_STATE_JSON, value).apply()

    // --- 固定駅 ---
    var pinnedStationId: String?
        get() = prefs.getString(KEY_PINNED_STATION_ID, null)
        set(value) = prefs.edit().putString(KEY_PINNED_STATION_ID, value).apply()

    // --- キャッシュ位置情報 ---
    fun getCachedLatitude(): Double? {
        return if (prefs.contains(KEY_CACHED_LAT)) {
            java.lang.Double.longBitsToDouble(prefs.getLong(KEY_CACHED_LAT, 0L))
        } else null
    }

    fun getCachedLongitude(): Double? {
        return if (prefs.contains(KEY_CACHED_LON)) {
            java.lang.Double.longBitsToDouble(prefs.getLong(KEY_CACHED_LON, 0L))
        } else null
    }

    fun cacheLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putLong(KEY_CACHED_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_CACHED_LON, java.lang.Double.doubleToRawLongBits(lon))
            .apply()
    }
}

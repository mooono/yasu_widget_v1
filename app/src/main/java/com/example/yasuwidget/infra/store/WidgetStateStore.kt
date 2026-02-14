package com.example.yasuwidget.infra.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.yasuwidget.domain.model.GeoPoint
import kotlinx.coroutines.flow.first
import java.time.Instant

private val Context.widgetDataStore by preferencesDataStore(name = "widget_state")

/**
 * Widget state persistence using DataStore
 * NFR-002: Persist state across app restarts
 */
class WidgetStateStore(
    private val context: Context
) {
    companion object {
        private val KEY_LAST_UPDATED_AT = longPreferencesKey("last_updated_at_millis")
        private val KEY_LAST_RENDERED_UI_STATE = stringPreferencesKey("last_rendered_ui_state_json")
        private val KEY_PINNED_STATION_ID = stringPreferencesKey("pinned_station_id")
        private val KEY_OVERRIDE_STATION_ID = stringPreferencesKey("override_station_id")
        private val KEY_OVERRIDE_EXPIRES_AT = longPreferencesKey("override_expires_at_millis")
        private val KEY_CACHED_LOCATION_LAT = stringPreferencesKey("cached_location_lat")
        private val KEY_CACHED_LOCATION_LON = stringPreferencesKey("cached_location_lon")
    }
    
    suspend fun getLastUpdatedAt(): Instant? {
        val prefs = context.widgetDataStore.data.first()
        val millis = prefs[KEY_LAST_UPDATED_AT] ?: return null
        return Instant.ofEpochMilli(millis)
    }
    
    suspend fun setLastUpdatedAt(instant: Instant) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_LAST_UPDATED_AT] = instant.toEpochMilli()
        }
    }
    
    suspend fun getLastRenderedUiState(): String? {
        val prefs = context.widgetDataStore.data.first()
        return prefs[KEY_LAST_RENDERED_UI_STATE]
    }
    
    suspend fun setLastRenderedUiState(jsonString: String) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_LAST_RENDERED_UI_STATE] = jsonString
        }
    }
    
    suspend fun getPinnedStationId(): String? {
        val prefs = context.widgetDataStore.data.first()
        return prefs[KEY_PINNED_STATION_ID]
    }
    
    suspend fun setPinnedStationId(stationId: String?) {
        context.widgetDataStore.edit { prefs ->
            if (stationId != null) {
                prefs[KEY_PINNED_STATION_ID] = stationId
            } else {
                prefs.remove(KEY_PINNED_STATION_ID)
            }
        }
    }
    
    suspend fun getOverrideStationId(): String? {
        val prefs = context.widgetDataStore.data.first()
        return prefs[KEY_OVERRIDE_STATION_ID]
    }
    
    suspend fun getOverrideExpiresAt(): Instant? {
        val prefs = context.widgetDataStore.data.first()
        val millis = prefs[KEY_OVERRIDE_EXPIRES_AT] ?: return null
        return Instant.ofEpochMilli(millis)
    }
    
    suspend fun setOverride(stationId: String, expiresAt: Instant) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_OVERRIDE_STATION_ID] = stationId
            prefs[KEY_OVERRIDE_EXPIRES_AT] = expiresAt.toEpochMilli()
        }
    }
    
    suspend fun clearOverride() {
        context.widgetDataStore.edit { prefs ->
            prefs.remove(KEY_OVERRIDE_STATION_ID)
            prefs.remove(KEY_OVERRIDE_EXPIRES_AT)
        }
    }
    
    suspend fun getCachedLocation(): GeoPoint? {
        val prefs = context.widgetDataStore.data.first()
        val lat = prefs[KEY_CACHED_LOCATION_LAT]?.toDoubleOrNull() ?: return null
        val lon = prefs[KEY_CACHED_LOCATION_LON]?.toDoubleOrNull() ?: return null
        return GeoPoint(lat, lon)
    }
    
    suspend fun setCachedLocation(location: GeoPoint) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_CACHED_LOCATION_LAT] = location.latitude.toString()
            prefs[KEY_CACHED_LOCATION_LON] = location.longitude.toString()
        }
    }
}

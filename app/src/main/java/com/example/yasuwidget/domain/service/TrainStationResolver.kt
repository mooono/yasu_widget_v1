package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import java.time.Instant

/**
 * Train station selection priority data
 */
data class StationSelectionContext(
    val currentLocation: GeoPoint,
    val pinnedStationId: String?,
    val overrideStationId: String?,
    val overrideExpiresAt: Instant?,
    val currentTime: Instant,
    val availableStationIds: List<String>,
    val stationLocations: Map<String, GeoPoint>
)

/**
 * Resolves target train station based on priority
 * SYS-REQ-030: Priority: 1) Temporary override (within 30min), 2) Pinned, 3) Nearest
 * SYS-REQ-031: Override set when user switches station
 * SYS-REQ-032: Override valid for 30 minutes
 * SYS-REQ-033: After expiry, re-evaluate priority
 */
class TrainStationResolver {
    
    companion object {
        // SYS-REQ-031: Override duration
        const val OVERRIDE_DURATION_MINUTES = 30L
    }
    
    fun resolve(context: StationSelectionContext): String {
        // Priority 1: Temporary override (if valid)
        context.overrideStationId?.let { overrideId ->
            if (isOverrideValid(context.overrideExpiresAt, context.currentTime)) {
                return overrideId
            }
        }
        
        // Priority 2: Pinned station
        context.pinnedStationId?.let { pinnedId ->
            if (context.availableStationIds.contains(pinnedId)) {
                return pinnedId
            }
        }
        
        // Priority 3: Nearest station
        return findNearestStation(
            context.currentLocation,
            context.availableStationIds,
            context.stationLocations
        )
    }
    
    private fun isOverrideValid(expiresAt: Instant?, currentTime: Instant): Boolean {
        return expiresAt != null && currentTime.isBefore(expiresAt)
    }
    
    private fun findNearestStation(
        location: GeoPoint,
        stationIds: List<String>,
        stationLocations: Map<String, GeoPoint>
    ): String {
        return stationIds.minByOrNull { stationId ->
            val stationLocation = stationLocations[stationId]
                ?: return@minByOrNull Double.MAX_VALUE
            location.distanceTo(stationLocation)
        } ?: stationIds.firstOrNull() ?: "Yasu" // Default fallback
    }
}

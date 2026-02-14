package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit tests for TrainStationResolver
 * Verifies AC-008 and SYS-REQ-030/031/032/033
 */
class TrainStationResolverTest {
    
    private lateinit var resolver: TrainStationResolver
    
    @Before
    fun setup() {
        resolver = TrainStationResolver()
    }
    
    @Test
    fun `SYS-REQ-030 - Priority 1 - Valid override takes precedence`() {
        val now = Instant.now()
        val expiresAt = now.plus(20, ChronoUnit.MINUTES)  // Valid for 20 more minutes
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,
            pinnedStationId = "Nagaokakyo",
            overrideStationId = "Kusatsu",
            overrideExpiresAt = expiresAt,
            currentTime = now,
            availableStationIds = listOf("Yasu", "Nagaokakyo", "Kusatsu"),
            stationLocations = mapOf(
                "Yasu" to GeoPoint.YASU_STATION,
                "Nagaokakyo" to GeoPoint(35.0100, 135.7000),
                "Kusatsu" to GeoPoint(35.0200, 135.9000)
            )
        )
        
        // Override should take precedence over pinned and nearest
        assertEquals("Kusatsu", resolver.resolve(context))
    }
    
    @Test
    fun `AC-008 SYS-REQ-033 - Expired override falls back to priority 2`() {
        val now = Instant.now()
        val expiresAt = now.minus(5, ChronoUnit.MINUTES)  // Expired 5 minutes ago
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,
            pinnedStationId = "Nagaokakyo",
            overrideStationId = "Kusatsu",
            overrideExpiresAt = expiresAt,
            currentTime = now,
            availableStationIds = listOf("Yasu", "Nagaokakyo", "Kusatsu"),
            stationLocations = mapOf(
                "Yasu" to GeoPoint.YASU_STATION,
                "Nagaokakyo" to GeoPoint(35.0100, 135.7000),
                "Kusatsu" to GeoPoint(35.0200, 135.9000)
            )
        )
        
        // Should fall back to pinned station
        assertEquals("Nagaokakyo", resolver.resolve(context))
    }
    
    @Test
    fun `SYS-REQ-030 - Priority 2 - Pinned station used when no override`() {
        val now = Instant.now()
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,
            pinnedStationId = "Nagaokakyo",
            overrideStationId = null,
            overrideExpiresAt = null,
            currentTime = now,
            availableStationIds = listOf("Yasu", "Nagaokakyo", "Kusatsu"),
            stationLocations = mapOf(
                "Yasu" to GeoPoint.YASU_STATION,
                "Nagaokakyo" to GeoPoint(35.0100, 135.7000),
                "Kusatsu" to GeoPoint(35.0200, 135.9000)
            )
        )
        
        assertEquals("Nagaokakyo", resolver.resolve(context))
    }
    
    @Test
    fun `SYS-REQ-030 - Priority 3 - Nearest station used when no override or pinned`() {
        val now = Instant.now()
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,  // At Yasu
            pinnedStationId = null,
            overrideStationId = null,
            overrideExpiresAt = null,
            currentTime = now,
            availableStationIds = listOf("Yasu", "Nagaokakyo", "Kusatsu"),
            stationLocations = mapOf(
                "Yasu" to GeoPoint.YASU_STATION,
                "Nagaokakyo" to GeoPoint(35.0100, 135.7000),
                "Kusatsu" to GeoPoint(35.0200, 135.9000)
            )
        )
        
        // Should select Yasu as it's the nearest
        assertEquals("Yasu", resolver.resolve(context))
    }
    
    @Test
    fun `AC-008 - Override duration is 30 minutes`() {
        // This is a constant check
        assertEquals(30L, TrainStationResolver.OVERRIDE_DURATION_MINUTES)
    }
    
    @Test
    fun `Boundary test - Override expires exactly at current time`() {
        val now = Instant.now()
        val expiresAt = now  // Expires exactly now
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,
            pinnedStationId = "Nagaokakyo",
            overrideStationId = "Kusatsu",
            overrideExpiresAt = expiresAt,
            currentTime = now,
            availableStationIds = listOf("Yasu", "Nagaokakyo", "Kusatsu"),
            stationLocations = mapOf(
                "Yasu" to GeoPoint.YASU_STATION,
                "Nagaokakyo" to GeoPoint(35.0100, 135.7000),
                "Kusatsu" to GeoPoint(35.0200, 135.9000)
            )
        )
        
        // At exactly expiry time, override should NOT be valid
        assertEquals("Nagaokakyo", resolver.resolve(context))
    }
    
    @Test
    fun `Pinned station not in available list falls back to nearest`() {
        val now = Instant.now()
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,
            pinnedStationId = "Kyoto",  // Not in available list
            overrideStationId = null,
            overrideExpiresAt = null,
            currentTime = now,
            availableStationIds = listOf("Yasu", "Nagaokakyo"),
            stationLocations = mapOf(
                "Yasu" to GeoPoint.YASU_STATION,
                "Nagaokakyo" to GeoPoint(35.0100, 135.7000)
            )
        )
        
        // Should fall back to nearest station
        assertEquals("Yasu", resolver.resolve(context))
    }
    
    @Test
    fun `Returns default when no stations available`() {
        val now = Instant.now()
        
        val context = StationSelectionContext(
            currentLocation = GeoPoint.YASU_STATION,
            pinnedStationId = null,
            overrideStationId = null,
            overrideExpiresAt = null,
            currentTime = now,
            availableStationIds = emptyList(),
            stationLocations = emptyMap()
        )
        
        // Should return default fallback
        val result = resolver.resolve(context)
        assertEquals("Yasu", result)  // Default fallback
    }
}

package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DisplayModeResolver
 * Verifies AC-001, AC-002, AC-003
 */
class DisplayModeResolverTest {
    
    private lateinit var resolver: DisplayModeResolver
    
    @Before
    fun setup() {
        resolver = DisplayModeResolver()
    }
    
    @Test
    fun `AC-001 - Within 2km of Murata returns BUS_ONLY`() {
        // At Murata location itself
        val atMurata = GeoPoint.MURATA_YASU
        assertEquals(DisplayMode.BUS_ONLY, resolver.resolve(atMurata))
        
        // 1km from Murata (within 2km radius)
        val nearMurata = GeoPoint(
            latitude = GeoPoint.MURATA_YASU.latitude + 0.009,  // ~1km north
            longitude = GeoPoint.MURATA_YASU.longitude
        )
        assertEquals(DisplayMode.BUS_ONLY, resolver.resolve(nearMurata))
        
        // Just under 2km from Murata
        val almostOutOfMurataRadius = GeoPoint(
            latitude = GeoPoint.MURATA_YASU.latitude + 0.017,  // ~1.9km north
            longitude = GeoPoint.MURATA_YASU.longitude
        )
        assertEquals(DisplayMode.BUS_ONLY, resolver.resolve(almostOutOfMurataRadius))
    }
    
    @Test
    fun `AC-002 - Within 1km of Yasu and outside 2km of Murata returns TRAIN_AND_BUS`() {
        // At Yasu station itself
        val atYasu = GeoPoint.YASU_STATION
        assertEquals(DisplayMode.TRAIN_AND_BUS, resolver.resolve(atYasu))
        
        // 500m from Yasu (within 1km radius and far from Murata)
        val nearYasu = GeoPoint(
            latitude = GeoPoint.YASU_STATION.latitude + 0.0045,  // ~500m north
            longitude = GeoPoint.YASU_STATION.longitude
        )
        assertEquals(DisplayMode.TRAIN_AND_BUS, resolver.resolve(nearYasu))
        
        // Just under 1km from Yasu
        val almostOutOfYasuRadius = GeoPoint(
            latitude = GeoPoint.YASU_STATION.latitude + 0.0089,  // ~990m north
            longitude = GeoPoint.YASU_STATION.longitude
        )
        assertEquals(DisplayMode.TRAIN_AND_BUS, resolver.resolve(almostOutOfYasuRadius))
    }
    
    @Test
    fun `AC-003 - Outside both radii returns TRAIN_ONLY`() {
        // Far from both locations (e.g., 5km away)
        val farLocation = GeoPoint(
            latitude = 35.1000,  // Far north
            longitude = 136.0500
        )
        assertEquals(DisplayMode.TRAIN_ONLY, resolver.resolve(farLocation))
        
        // Between Yasu and Murata but outside both radii
        val betweenLocations = GeoPoint(
            latitude = 35.0750,  // Between the two points
            longitude = 136.0334
        )
        val mode = resolver.resolve(betweenLocations)
        // Should be TRAIN_ONLY as it's more than 1km from Yasu and 2km from Murata
        assertEquals(DisplayMode.TRAIN_ONLY, mode)
    }
    
    @Test
    fun `Boundary test - Exactly at radius boundaries`() {
        // This tests the edge case at exactly the boundary distances
        // Note: Due to floating point precision, we test near-boundary cases
        
        // Just over 2km from Murata should not be BUS_ONLY
        val justOverMurataRadius = GeoPoint(
            latitude = GeoPoint.MURATA_YASU.latitude + 0.019,  // ~2.1km
            longitude = GeoPoint.MURATA_YASU.longitude
        )
        val mode = resolver.resolve(justOverMurataRadius)
        assert(mode != DisplayMode.BUS_ONLY) { "Expected not BUS_ONLY but got $mode" }
    }
    
    @Test
    fun `Priority test - Murata radius takes precedence over Yasu radius`() {
        // If a location is within both radii, Murata (BUS_ONLY) should take precedence
        // This shouldn't happen in practice due to the distance between the two locations,
        // but the logic checks Murata first
        
        val withinBoth = GeoPoint.MURATA_YASU  // At Murata
        assertEquals(DisplayMode.BUS_ONLY, resolver.resolve(withinBoth))
    }
}

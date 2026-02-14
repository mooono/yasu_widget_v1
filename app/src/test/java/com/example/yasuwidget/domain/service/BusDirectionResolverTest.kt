package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BusDirectionResolver
 * Verifies AC-004
 */
class BusDirectionResolverTest {
    
    private lateinit var resolver: BusDirectionResolver
    
    @Before
    fun setup() {
        resolver = BusDirectionResolver()
    }
    
    @Test
    fun `AC-004 - Closer to Murata returns TO_YASU`() {
        // At Murata itself - should go to Yasu
        val atMurata = GeoPoint.MURATA_YASU
        assertEquals(BusDirection.TO_YASU, resolver.resolve(atMurata))
        
        // Very close to Murata (100m away)
        val nearMurata = GeoPoint(
            latitude = GeoPoint.MURATA_YASU.latitude + 0.0009,
            longitude = GeoPoint.MURATA_YASU.longitude
        )
        assertEquals(BusDirection.TO_YASU, resolver.resolve(nearMurata))
        
        // Midway but closer to Murata
        val closerToMurata = GeoPoint(
            latitude = 35.0800,  // Closer to Murata (35.0833)
            longitude = 136.0350
        )
        assertEquals(BusDirection.TO_YASU, resolver.resolve(closerToMurata))
    }
    
    @Test
    fun `AC-004 - Closer to Yasu returns TO_MURATA`() {
        // At Yasu station itself - should go to Murata
        val atYasu = GeoPoint.YASU_STATION
        assertEquals(BusDirection.TO_MURATA, resolver.resolve(atYasu))
        
        // Very close to Yasu (100m away)
        val nearYasu = GeoPoint(
            latitude = GeoPoint.YASU_STATION.latitude + 0.0009,
            longitude = GeoPoint.YASU_STATION.longitude
        )
        assertEquals(BusDirection.TO_MURATA, resolver.resolve(nearYasu))
        
        // Midway but closer to Yasu
        val closerToYasu = GeoPoint(
            latitude = 35.0700,  // Closer to Yasu (35.0667)
            longitude = 136.0300
        )
        assertEquals(BusDirection.TO_MURATA, resolver.resolve(closerToYasu))
    }
    
    @Test
    fun `Boundary test - Exactly midpoint between stations`() {
        // Calculate approximate midpoint
        val midpoint = GeoPoint(
            latitude = (GeoPoint.YASU_STATION.latitude + GeoPoint.MURATA_YASU.latitude) / 2,
            longitude = (GeoPoint.YASU_STATION.longitude + GeoPoint.MURATA_YASU.longitude) / 2
        )
        
        // At midpoint, one direction should be chosen (based on distance comparison)
        val direction = resolver.resolve(midpoint)
        assert(direction == BusDirection.TO_YASU || direction == BusDirection.TO_MURATA) {
            "Direction should be either TO_YASU or TO_MURATA"
        }
    }
    
    @Test
    fun `Distance calculation consistency`() {
        // Test that the resolver uses proper distance calculation
        val point1 = GeoPoint(35.0750, 136.0300)  // Arbitrary point
        
        val distanceToMurata = point1.distanceTo(GeoPoint.MURATA_YASU)
        val distanceToYasu = point1.distanceTo(GeoPoint.YASU_STATION)
        
        val expectedDirection = if (distanceToMurata < distanceToYasu) {
            BusDirection.TO_YASU
        } else {
            BusDirection.TO_MURATA
        }
        
        assertEquals(expectedDirection, resolver.resolve(point1))
    }
}

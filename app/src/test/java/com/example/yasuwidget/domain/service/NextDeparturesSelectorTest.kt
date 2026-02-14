package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.Departure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit:Test
import java.time.LocalTime

/**
 * Unit tests for NextDeparturesSelector
 * Verifies DATA-REQ-003 and departure extraction logic
 */
class NextDeparturesSelectorTest {
    
    private lateinit var selector: NextDeparturesSelector
    
    @Before
    fun setup() {
        selector = NextDeparturesSelector()
    }
    
    @Test
    fun `SelectNext returns correct number of departures when enough available`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 10), "京都"),
            Departure(LocalTime.of(7, 25), "京都"),
            Departure(LocalTime.of(7, 40), "京都"),
            Departure(LocalTime.of(8, 10), "京都")
        )
        
        val currentTime = LocalTime.of(7, 0)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(7, 10), result[0].time)
        assertEquals(LocalTime.of(7, 25), result[1].time)
    }
    
    @Test
    fun `SelectNext filters out past departures`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 10), "京都"),
            Departure(LocalTime.of(7, 25), "京都"),
            Departure(LocalTime.of(7, 40), "京都")
        )
        
        val currentTime = LocalTime.of(7, 30)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(7, 40), result[0].time)
    }
    
    @Test
    fun `DATA-REQ-003 - Returns fewer than maxCount when not enough departures`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 40), "京都")
        )
        
        val currentTime = LocalTime.of(7, 0)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        // Should return only 1 departure even though maxCount is 2
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(7, 40), result[0].time)
    }
    
    @Test
    fun `Returns empty list when no future departures`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 10), "京都"),
            Departure(LocalTime.of(7, 25), "京都")
        )
        
        val currentTime = LocalTime.of(8, 0)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `Includes departure at exactly current time`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 30), "京都"),
            Departure(LocalTime.of(7, 45), "京都")
        )
        
        val currentTime = LocalTime.of(7, 30)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        // Should include the departure at exactly 7:30
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(7, 30), result[0].time)
    }
    
    @Test
    fun `Handles unsorted input by sorting`() {
        val departures = listOf(
            Departure(LocalTime.of(8, 10), "京都"),
            Departure(LocalTime.of(7, 25), "京都"),
            Departure(LocalTime.of(7, 40), "京都"),
            Departure(LocalTime.of(7, 10), "京都")
        )
        
        val currentTime = LocalTime.of(7, 0)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(7, 10), result[0].time)
        assertEquals(LocalTime.of(7, 25), result[1].time)
    }
    
    @Test
    fun `Preserves destination information`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 10), "京都"),
            Departure(LocalTime.of(7, 25), "米原")
        )
        
        val currentTime = LocalTime.of(7, 0)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        assertEquals("京都", result[0].destination)
        assertEquals("米原", result[1].destination)
    }
    
    @Test
    fun `Works with null destinations`() {
        val departures = listOf(
            Departure(LocalTime.of(7, 10), null),
            Departure(LocalTime.of(7, 25), null)
        )
        
        val currentTime = LocalTime.of(7, 0)
        val result = selector.selectNext(departures, currentTime, maxCount = 2)
        
        assertEquals(2, result.size)
        assertEquals(null, result[0].destination)
        assertEquals(null, result[1].destination)
    }
}

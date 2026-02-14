package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.ServiceDay
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for ServiceDayResolver
 * Verifies AC-005 and SYS-REQ-003/004
 */
class ServiceDayResolverTest {
    
    private lateinit var resolver: ServiceDayResolver
    
    @Before
    fun setup() {
        resolver = ServiceDayResolver()
    }
    
    @Test
    fun `AC-005 - Saturday returns HOLIDAY`() {
        val saturday = LocalDate.of(2024, 2, 10)  // Saturday
        assertEquals(ServiceDay.HOLIDAY, resolver.resolve(saturday))
    }
    
    @Test
    fun `AC-005 - Sunday returns HOLIDAY`() {
        val sunday = LocalDate.of(2024, 2, 11)  // Sunday
        assertEquals(ServiceDay.HOLIDAY, resolver.resolve(sunday))
    }
    
    @Test
    fun `SYS-REQ-003 - Monday returns WEEKDAY`() {
        val monday = LocalDate.of(2024, 2, 12)  // Monday
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(monday))
    }
    
    @Test
    fun `SYS-REQ-003 - Tuesday returns WEEKDAY`() {
        val tuesday = LocalDate.of(2024, 2, 13)  // Tuesday
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(tuesday))
    }
    
    @Test
    fun `SYS-REQ-003 - Wednesday returns WEEKDAY`() {
        val wednesday = LocalDate.of(2024, 2, 14)  // Wednesday
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(wednesday))
    }
    
    @Test
    fun `SYS-REQ-003 - Thursday returns WEEKDAY`() {
        val thursday = LocalDate.of(2024, 2, 15)  // Thursday
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(thursday))
    }
    
    @Test
    fun `SYS-REQ-003 - Friday returns WEEKDAY`() {
        val friday = LocalDate.of(2024, 2, 16)  // Friday
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(friday))
    }
    
    @Test
    fun `SYS-REQ-004 - National holidays are treated as WEEKDAY`() {
        // Test a known Monday national holiday in Japan
        // e.g., Coming of Age Day (Second Monday of January)
        val nationalHolidayMonday = LocalDate.of(2024, 1, 8)  // Monday (national holiday)
        
        // According to SYS-REQ-004, national holidays are NOT specially handled
        // They should be treated as WEEKDAY (not HOLIDAY)
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(nationalHolidayMonday))
    }
    
    @Test
    fun `Edge case - Year boundary dates`() {
        // New Year's Day (Friday)
        val newYearsDay = LocalDate.of(2024, 1, 1)  // Monday
        assertEquals(ServiceDay.WEEKDAY, resolver.resolve(newYearsDay))
        
        // New Year's Eve (Sunday)
        val newYearsEve = LocalDate.of(2023, 12, 31)  // Sunday
        assertEquals(ServiceDay.HOLIDAY, resolver.resolve(newYearsEve))
    }
    
    @Test
    fun `Consistency across different years`() {
        // Test that same day of week returns same service day across years
        val saturday2023 = LocalDate.of(2023, 3, 11)  // Saturday
        val saturday2024 = LocalDate.of(2024, 3, 9)   // Saturday
        val saturday2025 = LocalDate.of(2025, 3, 8)   // Saturday
        
        assertEquals(ServiceDay.HOLIDAY, resolver.resolve(saturday2023))
        assertEquals(ServiceDay.HOLIDAY, resolver.resolve(saturday2024))
        assertEquals(ServiceDay.HOLIDAY, resolver.resolve(saturday2025))
    }
}

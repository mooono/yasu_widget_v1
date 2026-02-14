package com.example.yasuwidget.infra.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Time provider interface for testability
 * Wraps system clock access
 */
interface TimeProvider {
    fun now(): Instant
    fun currentDate(): LocalDate
    fun currentTime(): LocalTime
    fun zoneId(): ZoneId
}

/**
 * System time provider implementation
 */
class SystemTimeProvider(
    private val zone: ZoneId = ZoneId.systemDefault()
) : TimeProvider {
    
    override fun now(): Instant = Instant.now()
    
    override fun currentDate(): LocalDate = LocalDate.now(zone)
    
    override fun currentTime(): LocalTime = LocalTime.now(zone)
    
    override fun zoneId(): ZoneId = zone
}

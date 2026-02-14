package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.Departure
import java.time.LocalTime

/**
 * Selects next departures from timetable
 * SYS-REQ-001: Train shows next 2 up + next 2 down
 * SYS-REQ-002: Bus shows next 2 in selected direction
 * SYS-REQ-003: If fewer than requested, show what's available
 */
class NextDeparturesSelector {
    
    /**
     * Extract up to [maxCount] next departures after [currentTime]
     */
    fun selectNext(
        departures: List<Departure>,
        currentTime: LocalTime,
        maxCount: Int = 2
    ): List<Departure> {
        return departures
            .filter { it.time.isAfter(currentTime) || it.time == currentTime }
            .sortedBy { it.time }
            .take(maxCount)
    }
}

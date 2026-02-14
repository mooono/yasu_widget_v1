package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.ServiceDay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Resolves service day type from date
 * SYS-REQ-003: Use local day-of-week for weekday/holiday
 * SYS-REQ-004: National holidays are treated as WEEKDAY (no special handling)
 */
class ServiceDayResolver {
    
    fun resolve(date: LocalDate): ServiceDay {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> ServiceDay.HOLIDAY
            else -> ServiceDay.WEEKDAY
        }
    }
}

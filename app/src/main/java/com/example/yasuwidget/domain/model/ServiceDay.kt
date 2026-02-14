package com.example.yasuwidget.domain.model

/**
 * Service day type for timetable selection
 * SYS-REQ-003, SYS-REQ-004
 */
enum class ServiceDay {
    /** Weekdays and holidays (Monday to Friday, including national holidays) */
    WEEKDAY,
    
    /** Weekend (Saturday and Sunday) */
    HOLIDAY
}

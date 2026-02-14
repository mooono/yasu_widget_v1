package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.ServiceDay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 曜日種別判定（SYS-REQ-003/004）
 *
 * - 土日 → HOLIDAY
 * - 平日（祝日含む）→ WEEKDAY
 */
object ServiceDayResolver {

    fun resolve(date: LocalDate): ServiceDay {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> ServiceDay.HOLIDAY
            else -> ServiceDay.WEEKDAY
        }
    }
}

package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.ServiceDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 曜日判定テスト（AC-005）
 *
 * AC-005: 土日はholidayを参照する（SYS-REQ-003）
 * SYS-REQ-004: 祝日はWEEKDAYとして扱う
 */
class ServiceDayResolverTest {

    @Test
    fun `AC-005 土曜日はHOLIDAYになる`() {
        // 2026-02-14 は土曜日
        val saturday = LocalDate.of(2026, 2, 14)
        assertEquals(ServiceDay.HOLIDAY, ServiceDayResolver.resolve(saturday))
    }

    @Test
    fun `AC-005 日曜日はHOLIDAYになる`() {
        val sunday = LocalDate.of(2026, 2, 15)
        assertEquals(ServiceDay.HOLIDAY, ServiceDayResolver.resolve(sunday))
    }

    @Test
    fun `月曜日はWEEKDAYになる`() {
        val monday = LocalDate.of(2026, 2, 16)
        assertEquals(ServiceDay.WEEKDAY, ServiceDayResolver.resolve(monday))
    }

    @Test
    fun `火曜日はWEEKDAYになる`() {
        val tuesday = LocalDate.of(2026, 2, 17)
        assertEquals(ServiceDay.WEEKDAY, ServiceDayResolver.resolve(tuesday))
    }

    @Test
    fun `水曜日はWEEKDAYになる`() {
        val wednesday = LocalDate.of(2026, 2, 18)
        assertEquals(ServiceDay.WEEKDAY, ServiceDayResolver.resolve(wednesday))
    }

    @Test
    fun `木曜日はWEEKDAYになる`() {
        val thursday = LocalDate.of(2026, 2, 19)
        assertEquals(ServiceDay.WEEKDAY, ServiceDayResolver.resolve(thursday))
    }

    @Test
    fun `金曜日はWEEKDAYになる`() {
        val friday = LocalDate.of(2026, 2, 20)
        assertEquals(ServiceDay.WEEKDAY, ServiceDayResolver.resolve(friday))
    }

    @Test
    fun `SYS-REQ-004 祝日(建国記念日)はWEEKDAYとして扱う`() {
        // 2026-02-11は水曜日（建国記念日でも平日扱い）
        val nationalHoliday = LocalDate.of(2026, 2, 11)
        assertEquals(ServiceDay.WEEKDAY, ServiceDayResolver.resolve(nationalHoliday))
    }
}

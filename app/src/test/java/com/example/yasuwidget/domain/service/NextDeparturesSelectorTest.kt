package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.Departure
import com.example.yasuwidget.domain.model.DirectionTimetable
import com.example.yasuwidget.domain.model.ServiceDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * 次便抽出テスト（AC-003 / SYS-REQ-001/002/003）
 *
 * - 現在時刻以降の便を抽出
 * - 要求本数に満たない場合は取得できた範囲で表示
 */
class NextDeparturesSelectorTest {

    private val weekdayDepartures = listOf(
        Departure(LocalTime.of(7, 12), "京都"),
        Departure(LocalTime.of(7, 25), "京都"),
        Departure(LocalTime.of(7, 40), "大阪"),
        Departure(LocalTime.of(7, 55), "京都"),
        Departure(LocalTime.of(8, 10), "京都")
    )

    private val holidayDepartures = listOf(
        Departure(LocalTime.of(8, 0), "京都"),
        Departure(LocalTime.of(9, 0), "京都")
    )

    private val timetable = DirectionTimetable(
        weekday = weekdayDepartures,
        holiday = holidayDepartures
    )

    @Test
    fun `SYS-REQ-001 平日7時15分に上り次2本を取得できる`() {
        val result = NextDeparturesSelector.select(
            timetable, ServiceDay.WEEKDAY, LocalTime.of(7, 15), 2
        )
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(7, 25), result[0].time)
        assertEquals(LocalTime.of(7, 40), result[1].time)
    }

    @Test
    fun `SYS-REQ-003 取得できた範囲で表示（残り1本）`() {
        val result = NextDeparturesSelector.select(
            timetable, ServiceDay.WEEKDAY, LocalTime.of(8, 5), 2
        )
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(8, 10), result[0].time)
    }

    @Test
    fun `最終便後は空リストを返す`() {
        val result = NextDeparturesSelector.select(
            timetable, ServiceDay.WEEKDAY, LocalTime.of(23, 0), 2
        )
        assertEquals(0, result.size)
    }

    @Test
    fun `AC-005 休日はholiday時刻表を参照する`() {
        val result = NextDeparturesSelector.select(
            timetable, ServiceDay.HOLIDAY, LocalTime.of(7, 30), 2
        )
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(8, 0), result[0].time)
        assertEquals(LocalTime.of(9, 0), result[1].time)
    }

    @Test
    fun `空のholiday時刻表では空リストを返す`() {
        val emptyHolidayTimetable = DirectionTimetable(
            weekday = weekdayDepartures,
            holiday = emptyList()
        )
        val result = NextDeparturesSelector.select(
            emptyHolidayTimetable, ServiceDay.HOLIDAY, LocalTime.of(7, 0), 2
        )
        assertEquals(0, result.size)
    }

    @Test
    fun `ちょうど発車時刻と同じ時刻は含まれる`() {
        val result = NextDeparturesSelector.select(
            timetable, ServiceDay.WEEKDAY, LocalTime.of(7, 12), 2
        )
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(7, 12), result[0].time)
        assertEquals(LocalTime.of(7, 25), result[1].time)
    }

    @Test
    fun `count=1の場合は1本のみ取得する`() {
        val result = NextDeparturesSelector.select(
            timetable, ServiceDay.WEEKDAY, LocalTime.of(7, 0), 1
        )
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(7, 12), result[0].time)
    }
}

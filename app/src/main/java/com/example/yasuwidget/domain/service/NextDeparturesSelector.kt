package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.Departure
import com.example.yasuwidget.domain.model.ServiceDay
import com.example.yasuwidget.domain.model.DirectionTimetable
import java.time.LocalTime

/**
 * 次便抽出（SYS-REQ-001/002/003）
 *
 * 現在時刻以降の便を抽出し、指定本数に満たない場合は取得できた範囲で返す
 */
object NextDeparturesSelector {

    /**
     * 指定方向の時刻表から、現在時刻以降の便をcount本まで抽出する
     */
    fun select(
        timetable: DirectionTimetable,
        serviceDay: ServiceDay,
        currentTime: LocalTime,
        count: Int
    ): List<Departure> {
        val departures = when (serviceDay) {
            ServiceDay.WEEKDAY -> timetable.weekday
            ServiceDay.HOLIDAY -> timetable.holiday
        }

        return departures
            .filter { it.time >= currentTime }
            .take(count)
    }
}

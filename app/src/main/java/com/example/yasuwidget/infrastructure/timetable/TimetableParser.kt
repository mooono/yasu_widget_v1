package com.example.yasuwidget.infrastructure.timetable

import com.example.yasuwidget.domain.model.*
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 時刻表JSONのパーサー
 * DATA-REQ-001/002: JSON構造のバリデーション付きパース
 * 必須キー欠落はエラーとして扱う（黙殺しない）
 */
object TimetableParser {

    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * 電車時刻表JSONをパースする（DATA-REQ-001）
     * @throws TimetableParseException 構造不正・必須キー欠落時
     */
    fun parseTrainTimetable(json: String): TrainTimetable {
        try {
            val root = JSONObject(json)
            val stationsObj = root.getJSONObject("stations")

            val stations = mutableMapOf<String, StationTimetable>()
            for (stationId in stationsObj.keys()) {
                val stationObj = stationsObj.getJSONObject(stationId)
                val stationName = stationObj.getString("name")
                val linesObj = stationObj.getJSONObject("lines")

                val lines = mutableMapOf<String, LineTimetable>()
                for (lineId in linesObj.keys()) {
                    val lineObj = linesObj.getJSONObject(lineId)
                    val lineName = lineObj.getString("name")
                    val up = parseDirectionTimetable(lineObj.getJSONObject("up"))
                    val down = parseDirectionTimetable(lineObj.getJSONObject("down"))
                    lines[lineId] = LineTimetable(name = lineName, up = up, down = down)
                }

                stations[stationId] = StationTimetable(name = stationName, lines = lines)
            }

            if (stations.isEmpty()) {
                throw TimetableParseException("電車時刻表に駅が含まれていません")
            }

            return TrainTimetable(stations = stations)
        } catch (e: TimetableParseException) {
            throw e
        } catch (e: Exception) {
            throw TimetableParseException("電車時刻表のパースに失敗: ${e.message}", e)
        }
    }

    /**
     * バス時刻表JSONをパースする（DATA-REQ-002）
     * @throws TimetableParseException 構造不正・必須キー欠落時
     */
    fun parseBusTimetable(json: String): BusTimetable {
        try {
            val root = JSONObject(json)
            val routeName = root.getString("route_name")
            val toYasu = parseDirectionTimetable(root.getJSONObject("to_yasu"))
            val toMurata = parseDirectionTimetable(root.getJSONObject("to_murata"))

            return BusTimetable(
                routeName = routeName,
                toYasu = toYasu,
                toMurata = toMurata
            )
        } catch (e: TimetableParseException) {
            throw e
        } catch (e: Exception) {
            throw TimetableParseException("バス時刻表のパースに失敗: ${e.message}", e)
        }
    }

    private fun parseDirectionTimetable(json: JSONObject): DirectionTimetable {
        val weekday = parseDepartureList(json.getJSONArray("weekday"))
        val holiday = parseDepartureList(json.getJSONArray("holiday"))
        return DirectionTimetable(weekday = weekday, holiday = holiday)
    }

    private fun parseDepartureList(jsonArray: org.json.JSONArray): List<Departure> {
        val departures = mutableListOf<Departure>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val timeStr = obj.getString("time")
            val time = parseTime(timeStr)
            val destination = obj.optString("destination", "")
            departures.add(Departure(time = time, destination = destination))
        }
        return departures
    }

    private fun parseTime(timeStr: String): LocalTime {
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER)
        } catch (e: DateTimeParseException) {
            throw TimetableParseException("不正な時刻形式: '$timeStr'（HH:mm形式が必要です）", e)
        }
    }
}

/**
 * 時刻表パースエラー
 */
class TimetableParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

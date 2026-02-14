package com.example.yasuwidget.infra.timetable

import android.content.Context
import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.Departure
import com.example.yasuwidget.domain.model.ServiceDay
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Timetable repository for loading JSON timetables
 * DATA-REQ-001: Load train_timetable.json
 * DATA-REQ-002: Load bus_timetable.json
 */
class TimetableRepository(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }
    
    /**
     * Load train timetable from assets or external storage
     */
    fun loadTrainTimetable(): Result<TrainTimetableJson> {
        return try {
            val jsonString = context.assets.open("train_timetable.json")
                .bufferedReader()
                .use { it.readText() }
            
            val timetable = json.decodeFromString<TrainTimetableJson>(jsonString)
            Result.success(timetable)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load bus timetable from assets
     */
    fun loadBusTimetable(): Result<BusTimetableJson> {
        return try {
            val jsonString = context.assets.open("bus_timetable.json")
                .bufferedReader()
                .use { it.readText() }
            
            val timetable = json.decodeFromString<BusTimetableJson>(jsonString)
            Result.success(timetable)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract train departures for a specific station and service day
     */
    fun getTrainDepartures(
        timetable: TrainTimetableJson,
        stationId: String,
        serviceDay: ServiceDay
    ): Pair<List<Departure>, List<Departure>>? {
        val station = timetable.stations[stationId] ?: return null
        val line = station.lines.values.firstOrNull() ?: return null
        
        val upList = when (serviceDay) {
            ServiceDay.WEEKDAY -> line.up.weekday
            ServiceDay.HOLIDAY -> line.up.holiday
        }
        
        val downList = when (serviceDay) {
            ServiceDay.WEEKDAY -> line.down.weekday
            ServiceDay.HOLIDAY -> line.down.holiday
        }
        
        return parseDepartures(upList) to parseDepartures(downList)
    }
    
    /**
     * Extract bus departures for a specific direction and service day
     */
    fun getBusDepartures(
        timetable: BusTimetableJson,
        direction: BusDirection,
        serviceDay: ServiceDay
    ): List<Departure> {
        val directionData = when (direction) {
            BusDirection.TO_YASU -> timetable.to_yasu
            BusDirection.TO_MURATA -> timetable.to_murata
        }
        
        val jsonList = when (serviceDay) {
            ServiceDay.WEEKDAY -> directionData.weekday
            ServiceDay.HOLIDAY -> directionData.holiday
        }
        
        return parseDepartures(jsonList)
    }
    
    private fun parseDepartures(jsonList: List<DepartureJson>): List<Departure> {
        return jsonList.mapNotNull { json ->
            try {
                val time = LocalTime.parse(json.time)
                Departure(time, json.destination)
            } catch (e: DateTimeParseException) {
                null // Skip invalid time entries
            }
        }
    }
    
    /**
     * Get list of available station IDs
     */
    fun getAvailableStations(timetable: TrainTimetableJson): List<String> {
        return timetable.stations.keys.toList()
    }
}

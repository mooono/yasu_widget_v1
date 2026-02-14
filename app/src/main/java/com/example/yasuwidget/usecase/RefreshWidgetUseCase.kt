package com.example.yasuwidget.usecase

import android.content.Context
import android.util.Log
import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.Departure
import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.GeoPoint
import com.example.yasuwidget.domain.service.BusDirectionResolver
import com.example.yasuwidget.domain.service.DisplayModeResolver
import com.example.yasuwidget.domain.service.NextDeparturesSelector
import com.example.yasuwidget.domain.service.ServiceDayResolver
import com.example.yasuwidget.domain.service.StationSelectionContext
import com.example.yasuwidget.domain.service.TrainStationResolver
import com.example.yasuwidget.infra.location.LocationRepository
import com.example.yasuwidget.infra.scheduler.UpdateScheduler
import com.example.yasuwidget.infra.store.WidgetStateStore
import com.example.yasuwidget.infra.timetable.TimetableRepository
import com.example.yasuwidget.infra.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter

/**
 * Main use case for refreshing widget display
 * Orchestrates all domain services and infrastructure components
 * NFR-001: Never crash - catch all exceptions
 */
class RefreshWidgetUseCase(
    private val context: Context,
    private val timeProvider: TimeProvider,
    private val locationRepository: LocationRepository,
    private val timetableRepository: TimetableRepository,
    private val stateStore: WidgetStateStore,
    private val updateScheduler: UpdateScheduler,
    private val displayModeResolver: DisplayModeResolver,
    private val busDirectionResolver: BusDirectionResolver,
    private val trainStationResolver: TrainStationResolver,
    private val serviceDayResolver: ServiceDayResolver,
    private val nextDeparturesSelector: NextDeparturesSelector
) {
    companion object {
        private const val TAG = "RefreshWidgetUseCase"
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val json = Json { prettyPrint = false }
    }
    
    suspend fun execute(): WidgetUiState = withContext(Dispatchers.IO) {
        try {
            val now = timeProvider.now()
            val currentDate = timeProvider.currentDate()
            val currentTime = timeProvider.currentTime()
            
            Log.d(TAG, "Starting widget refresh at $now")
            
            // 1. Get current location (with fallback to cached)
            val location = getLocationWithFallback()
            
            // 2. Load timetables
            val trainTimetableResult = timetableRepository.loadTrainTimetable()
            val busTimetableResult = timetableRepository.loadBusTimetable()
            
            if (trainTimetableResult.isFailure || busTimetableResult.isFailure) {
                return@withContext buildErrorState(
                    lastUpdatedAt = now,
                    statusMessage = "データ未登録" // SYS-REQ-043
                )
            }
            
            val trainTimetable = trainTimetableResult.getOrThrow()
            val busTimetable = busTimetableResult.getOrThrow()
            
            // 3. Determine display mode
            val displayMode = displayModeResolver.resolve(location)
            
            // 4. Determine bus direction
            val busDirection = busDirectionResolver.resolve(location)
            
            // 5. Determine service day
            val serviceDay = serviceDayResolver.resolve(currentDate)
            
            // 6. Build UI state based on mode
            val uiState = when (displayMode) {
                DisplayMode.TRAIN_ONLY -> buildTrainOnlyState(
                    location, trainTimetable, currentTime, serviceDay, now
                )
                DisplayMode.TRAIN_AND_BUS -> buildTrainAndBusState(
                    location, trainTimetable, busTimetable, currentTime, 
                    serviceDay, busDirection, now
                )
                DisplayMode.BUS_ONLY -> buildBusOnlyState(
                    busTimetable, busDirection, currentTime, serviceDay, now
                )
            }
            
            // 7. Persist state
            stateStore.setLastUpdatedAt(now)
            stateStore.setLastRenderedUiState(json.encodeToString(uiState))
            
            // 8. Schedule next update
            updateScheduler.scheduleNextUpdate()
            
            Log.d(TAG, "Widget refresh completed successfully")
            uiState
            
        } catch (e: Exception) {
            Log.e(TAG, "Widget refresh failed", e)
            // Return last cached state or error state
            loadCachedStateOrError()
        }
    }
    
    private suspend fun getLocationWithFallback(): GeoPoint {
        return try {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                stateStore.setCachedLocation(location)
                location
            } else {
                // SYS-REQ-042: Use cached location
                stateStore.getCachedLocation() ?: GeoPoint.YASU_STATION
            }
        } catch (e: Exception) {
            Log.w(TAG, "Location fetch failed, using cache", e)
            stateStore.getCachedLocation() ?: GeoPoint.YASU_STATION
        }
    }
    
    private suspend fun buildTrainOnlyState(
        location: GeoPoint,
        trainTimetable: com.example.yasuwidget.infra.timetable.TrainTimetableJson,
        currentTime: java.time.LocalTime,
        serviceDay: com.example.yasuwidget.domain.model.ServiceDay,
        now: java.time.Instant
    ): WidgetUiState {
        val stationId = resolveTrainStation(location, trainTimetable)
        val station = trainTimetable.stations[stationId]
        val stationName = station?.name ?: stationId
        val line = station?.lines?.values?.firstOrNull()
        val lineName = line?.name ?: ""
        
        val (upDepartures, downDepartures) = timetableRepository.getTrainDepartures(
            trainTimetable, stationId, serviceDay
        ) ?: (emptyList<Departure>() to emptyList<Departure>())
        
        val nextUp = nextDeparturesSelector.selectNext(upDepartures, currentTime, 2)
        val nextDown = nextDeparturesSelector.selectNext(downDepartures, currentTime, 2)
        
        return WidgetUiState(
            mode = DisplayMode.TRAIN_ONLY,
            headerTitle = stationName,
            train = TrainSection(
                lineName = lineName,
                upDepartures = nextUp.map { toDepartureDisplay(it) },
                downDepartures = nextDown.map { toDepartureDisplay(it) }
            ),
            bus = null,
            lastUpdatedAtText = timeFormatter.format(
                java.time.LocalDateTime.ofInstant(now, timeProvider.zoneId())
            ),
            statusMessage = null
        )
    }
    
    private suspend fun buildTrainAndBusState(
        location: GeoPoint,
        trainTimetable: com.example.yasuwidget.infra.timetable.TrainTimetableJson,
        busTimetable: com.example.yasuwidget.infra.timetable.BusTimetableJson,
        currentTime: java.time.LocalTime,
        serviceDay: com.example.yasuwidget.domain.model.ServiceDay,
        busDirection: BusDirection,
        now: java.time.Instant
    ): WidgetUiState {
        val stationId = resolveTrainStation(location, trainTimetable)
        val station = trainTimetable.stations[stationId]
        val stationName = station?.name ?: stationId
        val line = station?.lines?.values?.firstOrNull()
        val lineName = line?.name ?: ""
        
        val (upDepartures, downDepartures) = timetableRepository.getTrainDepartures(
            trainTimetable, stationId, serviceDay
        ) ?: (emptyList<Departure>() to emptyList<Departure>())
        
        val nextUp = nextDeparturesSelector.selectNext(upDepartures, currentTime, 2)
        val nextDown = nextDeparturesSelector.selectNext(downDepartures, currentTime, 2)
        
        val busDepartures = timetableRepository.getBusDepartures(
            busTimetable, busDirection, serviceDay
        )
        val nextBus = nextDeparturesSelector.selectNext(busDepartures, currentTime, 2)
        
        val busDirectionText = when (busDirection) {
            BusDirection.TO_YASU -> "→ 野洲駅"
            BusDirection.TO_MURATA -> "→ 村田"
        }
        
        return WidgetUiState(
            mode = DisplayMode.TRAIN_AND_BUS,
            headerTitle = stationName,
            train = TrainSection(
                lineName = lineName,
                upDepartures = nextUp.map { toDepartureDisplay(it) },
                downDepartures = nextDown.map { toDepartureDisplay(it) }
            ),
            bus = BusSection(
                direction = busDirectionText,
                departures = nextBus.map { toDepartureDisplay(it) }
            ),
            lastUpdatedAtText = timeFormatter.format(
                java.time.LocalDateTime.ofInstant(now, timeProvider.zoneId())
            ),
            statusMessage = null
        )
    }
    
    private suspend fun buildBusOnlyState(
        busTimetable: com.example.yasuwidget.infra.timetable.BusTimetableJson,
        busDirection: BusDirection,
        currentTime: java.time.LocalTime,
        serviceDay: com.example.yasuwidget.domain.model.ServiceDay,
        now: java.time.Instant
    ): WidgetUiState {
        val busDepartures = timetableRepository.getBusDepartures(
            busTimetable, busDirection, serviceDay
        )
        val nextBus = nextDeparturesSelector.selectNext(busDepartures, currentTime, 2)
        
        val busDirectionText = when (busDirection) {
            BusDirection.TO_YASU -> "→ 野洲駅"
            BusDirection.TO_MURATA -> "→ 村田"
        }
        
        return WidgetUiState(
            mode = DisplayMode.BUS_ONLY,
            headerTitle = "村田付近",
            train = null,
            bus = BusSection(
                direction = busDirectionText,
                departures = nextBus.map { toDepartureDisplay(it) }
            ),
            lastUpdatedAtText = timeFormatter.format(
                java.time.LocalDateTime.ofInstant(now, timeProvider.zoneId())
            ),
            statusMessage = null
        )
    }
    
    private suspend fun resolveTrainStation(
        location: GeoPoint,
        trainTimetable: com.example.yasuwidget.infra.timetable.TrainTimetableJson
    ): String {
        val availableStations = timetableRepository.getAvailableStations(trainTimetable)
        val stationLocations = mapOf(
            "Yasu" to GeoPoint.YASU_STATION
            // Add more station locations as needed
        )
        
        val context = StationSelectionContext(
            currentLocation = location,
            pinnedStationId = stateStore.getPinnedStationId(),
            overrideStationId = stateStore.getOverrideStationId(),
            overrideExpiresAt = stateStore.getOverrideExpiresAt(),
            currentTime = timeProvider.now(),
            availableStationIds = availableStations,
            stationLocations = stationLocations
        )
        
        return trainStationResolver.resolve(context)
    }
    
    private fun toDepartureDisplay(departure: Departure): DepartureDisplay {
        return DepartureDisplay(
            time = timeFormatter.format(departure.time),
            destination = departure.destination
        )
    }
    
    private suspend fun buildErrorState(
        lastUpdatedAt: java.time.Instant,
        statusMessage: String
    ): WidgetUiState {
        return WidgetUiState(
            mode = DisplayMode.TRAIN_ONLY,
            headerTitle = "エラー",
            train = null,
            bus = null,
            lastUpdatedAtText = timeFormatter.format(
                java.time.LocalDateTime.ofInstant(lastUpdatedAt, timeProvider.zoneId())
            ),
            statusMessage = statusMessage
        )
    }
    
    private suspend fun loadCachedStateOrError(): WidgetUiState {
        val cachedJson = stateStore.getLastRenderedUiState()
        return if (cachedJson != null) {
            try {
                json.decodeFromString<WidgetUiState>(cachedJson)
            } catch (e: Exception) {
                buildErrorState(timeProvider.now(), "エラー")
            }
        } else {
            buildErrorState(timeProvider.now(), "データ未登録")
        }
    }
}

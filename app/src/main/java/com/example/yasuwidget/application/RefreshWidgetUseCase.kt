package com.example.yasuwidget.application

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.*
import com.example.yasuwidget.domain.service.*
import com.example.yasuwidget.infrastructure.location.LocationRepository
import com.example.yasuwidget.infrastructure.store.WidgetStateStore
import com.example.yasuwidget.infrastructure.time.TimeProvider
import com.example.yasuwidget.infrastructure.timetable.TimetableParseException
import com.example.yasuwidget.infrastructure.timetable.TimetableRepository
import java.time.format.DateTimeFormatter

/**
 * Widget更新ユースケース（SYS-REQ-040〜044, NFR-001）
 *
 * 更新フロー:
 * 1. 現在時刻取得
 * 2. 位置取得（失敗ならキャッシュフォールバック）
 * 3. JSON読込・バリデーション（失敗なら「データ未登録」）
 * 4. ドメイン判定（モード/方向/駅/曜日）
 * 5. 次便抽出
 * 6. WidgetUiState構築
 * 7. 永続化
 */
class RefreshWidgetUseCase(
    private val timeProvider: TimeProvider,
    private val locationRepository: LocationRepository,
    private val timetableRepository: TimetableRepository,
    private val stateStore: WidgetStateStore
) {

    companion object {
        private const val TRAIN_COUNT_PER_DIRECTION = 3
        private const val BUS_COUNT = 3
        private val TIME_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    /**
     * Widget更新処理を実行する
     * NFR-001: 例外は捕捉しクラッシュさせない
     *
     * @return 構築された WidgetUiState
     */
    suspend fun execute(): WidgetUiState {
        val now = timeProvider.now()
        val currentDate = now.toLocalDate()
        val currentTime = now.toLocalTime()
        val currentEpochMillis = timeProvider.currentEpochMillis()

        // 1. 位置取得（SYS-REQ-042: 失敗時はキャッシュ）
        var locationFailed = false
        var currentLocation: GeoPoint? = try {
            locationRepository.getCurrentLocation()
        } catch (e: Exception) {
            null
        }

        if (currentLocation != null) {
            stateStore.cacheLocation(currentLocation.latitude, currentLocation.longitude)
        } else {
            // キャッシュからフォールバック
            val cachedLat = stateStore.getCachedLatitude()
            val cachedLon = stateStore.getCachedLongitude()
            if (cachedLat != null && cachedLon != null) {
                currentLocation = GeoPoint(cachedLat, cachedLon)
            }
            locationFailed = true
        }

        // 2. 時刻表読込（SYS-REQ-043: データ欠損時は「データ未登録」）
        val trainTimetable: TrainTimetable?
        val busTimetable: BusTimetable?
        var dataError = false

        try {
            trainTimetable = timetableRepository.loadTrainTimetable()
            busTimetable = timetableRepository.loadBusTimetable()
        } catch (e: TimetableParseException) {
            return buildErrorState(
                currentTime = currentTime,
                statusMessage = "データ未登録",
                locationFailed = locationFailed
            )
        }

        // 3. ドメイン判定
        val displayMode = if (currentLocation != null) {
            DisplayModeResolver.resolve(currentLocation)
        } else {
            DisplayMode.TRAIN_ONLY
        }

        val serviceDay = ServiceDayResolver.resolve(currentDate)

        // 4. 電車セクション構築
        var trainSection: TrainSection? = null
        if (displayMode == DisplayMode.TRAIN_ONLY || displayMode == DisplayMode.TRAIN_AND_BUS) {
            if (trainTimetable == null || trainTimetable.stations.isEmpty()) {
                dataError = true
            } else {
                trainSection = buildTrainSection(
                    trainTimetable, currentLocation, serviceDay, currentTime
                )
            }
        }

        // 5. バスセクション構築（全モードで常に表示）
        var busSection: BusSection? = null
        if (busTimetable == null) {
            dataError = true
        } else {
            busSection = buildBusSection(
                busTimetable, currentLocation, serviceDay, currentTime
            )
        }

        // 6. ステータスメッセージ
        val statusMessage = when {
            dataError -> "データ未登録"
            locationFailed -> "位置取得不可"
            else -> null
        }

        // 7. ヘッダー構築
        val headerTitle = when (displayMode) {
            DisplayMode.BUS_ONLY -> "村田付近"
            DisplayMode.TRAIN_AND_BUS -> trainSection?.stationName ?: "野洲"
            DisplayMode.TRAIN_ONLY -> trainSection?.stationName ?: ""
        }

        val lastUpdatedAtText = "更新 ${currentTime.format(TIME_DISPLAY_FORMATTER)}"

        val uiState = WidgetUiState(
            mode = displayMode,
            headerTitle = headerTitle,
            train = trainSection,
            bus = busSection,
            lastUpdatedAtText = lastUpdatedAtText,
            statusMessage = statusMessage,
            currentTime = currentTime
        )

        // 8. 永続化
        stateStore.lastUpdatedAtEpochMillis = currentEpochMillis

        return uiState
    }

    private fun buildTrainSection(
        timetable: TrainTimetable,
        currentLocation: GeoPoint?,
        serviceDay: ServiceDay,
        currentTime: java.time.LocalTime
    ): TrainSection? {
        // 利用可能な駅一覧（時刻表にある駅のみ）
        val availableStations = LocationConstants.TOKAIDO_STATIONS.filter {
            timetable.stations.containsKey(it.id)
        }
        if (availableStations.isEmpty()) return null

        val station = TrainStationResolver.resolve(
            pinnedStationId = stateStore.pinnedStationId,
            currentLocation = currentLocation,
            availableStations = availableStations
        ) ?: return null

        val stationTimetable = timetable.stations[station.id] ?: return null
        // v1ではTokaido線のみ
        val lineTimetable = stationTimetable.lines.values.firstOrNull() ?: return null

        val upDepartures = NextDeparturesSelector.select(
            lineTimetable.up, serviceDay, currentTime, TRAIN_COUNT_PER_DIRECTION
        )
        val downDepartures = NextDeparturesSelector.select(
            lineTimetable.down, serviceDay, currentTime, TRAIN_COUNT_PER_DIRECTION
        )

        return TrainSection(
            stationName = station.displayName,
            lineName = lineTimetable.name,
            up = upDepartures,
            down = downDepartures
        )
    }

    private fun buildBusSection(
        busTimetable: BusTimetable,
        currentLocation: GeoPoint?,
        serviceDay: ServiceDay,
        currentTime: java.time.LocalTime
    ): BusSection? {
        val direction = if (currentLocation != null) {
            BusDirectionResolver.resolve(currentLocation)
        } else {
            BusDirection.TO_MURATA
        }

        val directionTimetable = when (direction) {
            BusDirection.TO_YASU -> busTimetable.toYasu
            BusDirection.TO_MURATA -> busTimetable.toMurata
        }

        // 野洲駅系と北口系に分割
        val yasuTimetable = DirectionTimetable(
            weekday = directionTimetable.weekday.filter { !it.destination.contains("北口") },
            holiday = directionTimetable.holiday.filter { !it.destination.contains("北口") }
        )
        val kitaguchiTimetable = DirectionTimetable(
            weekday = directionTimetable.weekday.filter { it.destination.contains("北口") },
            holiday = directionTimetable.holiday.filter { it.destination.contains("北口") }
        )

        val busStopName = when (direction) {
            BusDirection.TO_YASU -> "村田製作所"
            BusDirection.TO_MURATA -> "野洲駅"
        }
        val yasuLabel = when (direction) {
            BusDirection.TO_YASU -> "野洲駅行"
            BusDirection.TO_MURATA -> "野洲駅発"
        }
        val kitaguchiLabel = when (direction) {
            BusDirection.TO_YASU -> "北口行"
            BusDirection.TO_MURATA -> "北口発"
        }

        val yasuDepartures = NextDeparturesSelector.select(
            yasuTimetable, serviceDay, currentTime, BUS_COUNT
        )
        val kitaguchiDepartures = NextDeparturesSelector.select(
            kitaguchiTimetable, serviceDay, currentTime, BUS_COUNT
        )

        return BusSection(
            busStopName = busStopName,
            yasuLabel = yasuLabel,
            kitaguchiLabel = kitaguchiLabel,
            yasuDepartures = yasuDepartures,
            kitaguchiDepartures = kitaguchiDepartures
        )
    }

    private fun buildErrorState(
        currentTime: java.time.LocalTime,
        statusMessage: String,
        locationFailed: Boolean
    ): WidgetUiState {
        val message = if (locationFailed) "位置取得不可・$statusMessage" else statusMessage
        return WidgetUiState(
            mode = DisplayMode.TRAIN_ONLY,
            headerTitle = "",
            train = null,
            bus = null,
            lastUpdatedAtText = "更新 ${currentTime.format(TIME_DISPLAY_FORMATTER)}",
            statusMessage = message,
            currentTime = currentTime
        )
    }
}

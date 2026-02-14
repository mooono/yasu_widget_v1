package com.example.yasuwidget.application

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.StationInfo
import com.example.yasuwidget.domain.service.TrainStationResolver
import com.example.yasuwidget.infrastructure.store.WidgetStateStore
import com.example.yasuwidget.infrastructure.time.TimeProvider
import com.example.yasuwidget.infrastructure.timetable.TimetableRepository

/**
 * 駅切替ユースケース（SYS-REQ-031）
 *
 * Widget上の「前/次」ボタンで駅を切り替え、
 * 一時選択として30分間保持する
 */
class SwitchStationOverrideUseCase(
    private val timeProvider: TimeProvider,
    private val stateStore: WidgetStateStore,
    private val timetableRepository: TimetableRepository
) {

    /**
     * 次の駅に切り替える
     * @return 切り替え後の駅（失敗時はnull）
     */
    fun switchToNext(): StationInfo? {
        return switchStation { currentId, stations ->
            TrainStationResolver.nextStation(currentId, stations)
        }
    }

    /**
     * 前の駅に切り替える
     * @return 切り替え後の駅（失敗時はnull）
     */
    fun switchToPrevious(): StationInfo? {
        return switchStation { currentId, stations ->
            TrainStationResolver.previousStation(currentId, stations)
        }
    }

    private fun switchStation(
        selector: (String, List<StationInfo>) -> StationInfo?
    ): StationInfo? {
        val trainTimetable = try {
            timetableRepository.loadTrainTimetable()
        } catch (e: Exception) {
            null
        } ?: return null

        val availableStations = LocationConstants.TOKAIDO_STATIONS.filter {
            trainTimetable.stations.containsKey(it.id)
        }
        if (availableStations.isEmpty()) return null

        // 現在の駅IDを取得
        val currentEpochMillis = timeProvider.currentEpochMillis()
        val currentStationId = if (stateStore.isOverrideActive(currentEpochMillis)) {
            stateStore.overrideStationId
        } else {
            stateStore.pinnedStationId
        } ?: availableStations.first().id

        val newStation = selector(currentStationId, availableStations) ?: return null

        // SYS-REQ-031: 有効期限を現在時刻+30分に設定
        val expiresAt = currentEpochMillis +
                LocationConstants.SWIPE_OVERRIDE_DURATION_MINUTES * 60 * 1000
        stateStore.setOverride(newStation.id, expiresAt)

        return newStation
    }
}

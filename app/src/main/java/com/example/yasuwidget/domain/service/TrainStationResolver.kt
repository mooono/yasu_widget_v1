package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import com.example.yasuwidget.domain.model.StationInfo

/**
 * 電車表示対象駅の解決（SYS-REQ-030/031/032/033）
 *
 * 優先順位:
 * 1. 一時選択（有効期限内）
 * 2. 固定駅（設定画面）
 * 3. 自動最寄り駅（直線距離最短）
 */
object TrainStationResolver {

    /**
     * 表示対象駅を決定する
     *
     * @param overrideStationId 一時選択駅ID（null以外で有効期限内であること）
     * @param pinnedStationId 固定駅ID
     * @param currentLocation 現在地
     * @param availableStations 時刻表に存在する駅一覧
     * @return 選択された駅（見つからない場合はnull）
     */
    fun resolve(
        overrideStationId: String?,
        pinnedStationId: String?,
        currentLocation: GeoPoint?,
        availableStations: List<StationInfo>
    ): StationInfo? {
        if (availableStations.isEmpty()) return null

        // 1. 一時選択（有効期限チェックは呼び出し側で行う）
        if (overrideStationId != null) {
            val station = availableStations.find { it.id == overrideStationId }
            if (station != null) return station
        }

        // 2. 固定駅
        if (pinnedStationId != null) {
            val station = availableStations.find { it.id == pinnedStationId }
            if (station != null) return station
        }

        // 3. 自動最寄り駅
        if (currentLocation != null) {
            return availableStations.minByOrNull {
                GeoUtils.distanceMeters(currentLocation, it.location)
            }
        }

        // 位置情報もなければ最初の駅を返す
        return availableStations.first()
    }

    /**
     * 駅切替操作: 次の駅IDを返す（SYS-REQ-031）
     */
    fun nextStation(
        currentStationId: String,
        availableStations: List<StationInfo>
    ): StationInfo? {
        if (availableStations.isEmpty()) return null
        val currentIndex = availableStations.indexOfFirst { it.id == currentStationId }
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % availableStations.size
        return availableStations[nextIndex]
    }

    /**
     * 駅切替操作: 前の駅IDを返す（SYS-REQ-031）
     */
    fun previousStation(
        currentStationId: String,
        availableStations: List<StationInfo>
    ): StationInfo? {
        if (availableStations.isEmpty()) return null
        val currentIndex = availableStations.indexOfFirst { it.id == currentStationId }
        val prevIndex = if (currentIndex <= 0) availableStations.size - 1 else currentIndex - 1
        return availableStations[prevIndex]
    }
}

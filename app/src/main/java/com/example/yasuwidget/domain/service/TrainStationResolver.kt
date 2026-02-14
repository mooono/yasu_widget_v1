package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import com.example.yasuwidget.domain.model.StationInfo

/**
 * 電車表示対象駅の解決（SYS-REQ-030）
 *
 * 優先順位:
 * 1. 固定駅（設定画面）
 * 2. 自動最寄り駅（直線距離最短）
 */
object TrainStationResolver {

    /**
     * 表示対象駅を決定する
     *
     * @param pinnedStationId 固定駅ID
     * @param currentLocation 現在地
     * @param availableStations 時刻表に存在する駅一覧
     * @return 選択された駅（見つからない場合はnull）
     */
    fun resolve(
        pinnedStationId: String?,
        currentLocation: GeoPoint?,
        availableStations: List<StationInfo>
    ): StationInfo? {
        if (availableStations.isEmpty()) return null

        // 1. 固定駅
        if (pinnedStationId != null) {
            val station = availableStations.find { it.id == pinnedStationId }
            if (station != null) return station
        }

        // 2. 自動最寄り駅
        if (currentLocation != null) {
            return availableStations.minByOrNull {
                GeoUtils.distanceMeters(currentLocation, it.location)
            }
        }

        // 位置情報もなければ最初の駅を返す
        return availableStations.first()
    }
}

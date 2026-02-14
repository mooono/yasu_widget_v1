package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import com.example.yasuwidget.domain.model.StationInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * 電車駅選択テスト
 *
 * SYS-REQ-030: 優先順位（固定駅 > 最寄り駅）
 */
class TrainStationResolverTest {

    private val stations = listOf(
        StationInfo("Kusatsu", "草津", GeoPoint(35.0186, 135.9609)),
        StationInfo("Moriyama", "守山", GeoPoint(35.0524, 136.0055)),
        StationInfo("Yasu", "野洲", GeoPoint(35.0654, 136.0253))
    )

    @Test
    fun `SYS-REQ-030 固定駅が最優先される`() {
        val result = TrainStationResolver.resolve(
            pinnedStationId = "Moriyama",
            currentLocation = GeoPoint(35.0654, 136.0253), // 野洲のそば
            availableStations = stations
        )
        assertEquals("Moriyama", result?.id)
    }

    @Test
    fun `SYS-REQ-030 固定駅がない場合は最寄り駅になる`() {
        // 野洲駅に最も近い位置
        val result = TrainStationResolver.resolve(
            pinnedStationId = null,
            currentLocation = GeoPoint(35.0660, 136.0260),
            availableStations = stations
        )
        assertEquals("Yasu", result?.id)
    }

    @Test
    fun `SYS-REQ-030 草津に最も近い位置では草津が選択される`() {
        val result = TrainStationResolver.resolve(
            pinnedStationId = null,
            currentLocation = GeoPoint(35.0190, 135.9610),
            availableStations = stations
        )
        assertEquals("Kusatsu", result?.id)
    }

    @Test
    fun `存在しない固定駅IDの場合は最寄り駅にフォールバック`() {
        val result = TrainStationResolver.resolve(
            pinnedStationId = "NonExistent",
            currentLocation = GeoPoint(35.0654, 136.0253),
            availableStations = stations
        )
        assertEquals("Yasu", result?.id)
    }

    @Test
    fun `空の駅リストではnullを返す`() {
        val result = TrainStationResolver.resolve(
            pinnedStationId = null,
            currentLocation = GeoPoint(35.0, 136.0),
            availableStations = emptyList()
        )
        assertNull(result)
    }
}

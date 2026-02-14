package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import com.example.yasuwidget.domain.model.StationInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * 電車駅選択テスト（AC-008）
 *
 * AC-008: 駅切替操作で一時選択が30分保持される（SYS-REQ-031/032/033）
 * SYS-REQ-030: 優先順位（一時選択 > 固定駅 > 最寄り駅）
 */
class TrainStationResolverTest {

    private val stations = listOf(
        StationInfo("Kusatsu", "草津", GeoPoint(35.0186, 135.9609)),
        StationInfo("Moriyama", "守山", GeoPoint(35.0524, 136.0055)),
        StationInfo("Yasu", "野洲", GeoPoint(35.0654, 136.0253))
    )

    @Test
    fun `SYS-REQ-030 一時選択が最優先される`() {
        val result = TrainStationResolver.resolve(
            overrideStationId = "Kusatsu",
            pinnedStationId = "Yasu",
            currentLocation = GeoPoint(35.0654, 136.0253), // 野洲のそば
            availableStations = stations
        )
        assertEquals("Kusatsu", result?.id)
    }

    @Test
    fun `SYS-REQ-030 一時選択なしの場合は固定駅が優先される`() {
        val result = TrainStationResolver.resolve(
            overrideStationId = null,
            pinnedStationId = "Moriyama",
            currentLocation = GeoPoint(35.0654, 136.0253), // 野洲のそば
            availableStations = stations
        )
        assertEquals("Moriyama", result?.id)
    }

    @Test
    fun `SYS-REQ-030 一時選択も固定駅もない場合は最寄り駅になる`() {
        // 野洲駅に最も近い位置
        val result = TrainStationResolver.resolve(
            overrideStationId = null,
            pinnedStationId = null,
            currentLocation = GeoPoint(35.0660, 136.0260),
            availableStations = stations
        )
        assertEquals("Yasu", result?.id)
    }

    @Test
    fun `SYS-REQ-030 草津に最も近い位置では草津が選択される`() {
        val result = TrainStationResolver.resolve(
            overrideStationId = null,
            pinnedStationId = null,
            currentLocation = GeoPoint(35.0190, 135.9610),
            availableStations = stations
        )
        assertEquals("Kusatsu", result?.id)
    }

    @Test
    fun `存在しない一時選択IDの場合は固定駅にフォールバック`() {
        val result = TrainStationResolver.resolve(
            overrideStationId = "NonExistent",
            pinnedStationId = "Yasu",
            currentLocation = null,
            availableStations = stations
        )
        assertEquals("Yasu", result?.id)
    }

    @Test
    fun `空の駅リストではnullを返す`() {
        val result = TrainStationResolver.resolve(
            overrideStationId = null,
            pinnedStationId = null,
            currentLocation = GeoPoint(35.0, 136.0),
            availableStations = emptyList()
        )
        assertNull(result)
    }

    @Test
    fun `nextStationで次の駅に切り替わる`() {
        val result = TrainStationResolver.nextStation("Kusatsu", stations)
        assertEquals("Moriyama", result?.id)
    }

    @Test
    fun `nextStationで最後の駅から最初の駅に戻る`() {
        val result = TrainStationResolver.nextStation("Yasu", stations)
        assertEquals("Kusatsu", result?.id)
    }

    @Test
    fun `previousStationで前の駅に切り替わる`() {
        val result = TrainStationResolver.previousStation("Moriyama", stations)
        assertEquals("Kusatsu", result?.id)
    }

    @Test
    fun `previousStationで最初の駅から最後の駅に戻る`() {
        val result = TrainStationResolver.previousStation("Kusatsu", stations)
        assertEquals("Yasu", result?.id)
    }
}

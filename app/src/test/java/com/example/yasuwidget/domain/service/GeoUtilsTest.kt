package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 距離計算ユーティリティのテスト
 */
class GeoUtilsTest {

    @Test
    fun `同一地点間の距離は0`() {
        val point = GeoPoint(35.0, 136.0)
        val distance = GeoUtils.distanceMeters(point, point)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `既知の距離が概ね正しい`() {
        // 東京駅と新大阪駅: 約400km
        val tokyo = GeoPoint(35.6812, 139.7671)
        val osaka = GeoPoint(34.7336, 135.5001)
        val distance = GeoUtils.distanceMeters(tokyo, osaka)
        // 400km前後（380-420km）
        assertTrue("距離: ${distance / 1000}km", distance in 380_000.0..420_000.0)
    }

    @Test
    fun `野洲駅と村田間の距離が約2km`() {
        val yasu = GeoPoint(35.0654, 136.0253)
        val murata = GeoPoint(35.0480, 136.0330)
        val distance = GeoUtils.distanceMeters(yasu, murata)
        // 約2km（1.5-2.5km）
        assertTrue("距離: ${distance}m", distance in 1500.0..2500.0)
    }
}

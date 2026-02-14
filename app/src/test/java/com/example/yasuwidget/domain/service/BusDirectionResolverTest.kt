package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * バス方向判定テスト（AC-004）
 *
 * AC-004: バス方向が距離比較で正しく切り替わる（SYS-REQ-020/021）
 */
class BusDirectionResolverTest {

    @Test
    fun `AC-004 村田に近い場合は野洲駅方面(TO_YASU)になる`() {
        // 村田の座標そのもの
        val atMurata = LocationConstants.MURATA_YASU
        assertEquals(BusDirection.TO_YASU, BusDirectionResolver.resolve(atMurata))
    }

    @Test
    fun `AC-004 野洲駅に近い場合は村田方面(TO_MURATA)になる`() {
        // 野洲駅の座標そのもの
        val atYasu = LocationConstants.YASU_STATION
        assertEquals(BusDirection.TO_MURATA, BusDirectionResolver.resolve(atYasu))
    }

    @Test
    fun `両方から等距離の場合はTO_MURATAになる`() {
        // 中間地点
        val midpoint = GeoPoint(
            (LocationConstants.MURATA_YASU.latitude + LocationConstants.YASU_STATION.latitude) / 2,
            (LocationConstants.MURATA_YASU.longitude + LocationConstants.YASU_STATION.longitude) / 2
        )
        // 厳密に同じ距離の場合は TO_MURATA（else分岐）
        val distToMurata = GeoUtils.distanceMeters(midpoint, LocationConstants.MURATA_YASU)
        val distToYasu = GeoUtils.distanceMeters(midpoint, LocationConstants.YASU_STATION)
        val expected = if (distToMurata < distToYasu) BusDirection.TO_YASU else BusDirection.TO_MURATA
        assertEquals(expected, BusDirectionResolver.resolve(midpoint))
    }

    @Test
    fun `遠方(京都)からは村田方面(TO_MURATA)になる`() {
        // 京都は村田より野洲に遠い（両方遠いがTO_MURATAのはず）
        val kyoto = GeoPoint(34.9858, 135.7588)
        val distToMurata = GeoUtils.distanceMeters(kyoto, LocationConstants.MURATA_YASU)
        val distToYasu = GeoUtils.distanceMeters(kyoto, LocationConstants.YASU_STATION)
        val expected = if (distToMurata < distToYasu) BusDirection.TO_YASU else BusDirection.TO_MURATA
        assertEquals(expected, BusDirectionResolver.resolve(kyoto))
    }
}

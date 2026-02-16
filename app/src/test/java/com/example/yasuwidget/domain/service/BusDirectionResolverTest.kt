package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * バス方向判定テスト（AC-004）
 *
 * AC-004: バス方向が村田半径に基づき正しく切り替わる（SYS-REQ-020/021）
 *   - 村田から MURATA_RADIUS 以内 → TO_YASU（村田発）
 *   - それ以外 → TO_MURATA（野洲駅発）
 */
class BusDirectionResolverTest {

    @Test
    fun `AC-004 村田2km以内では村田発(TO_YASU)になる`() {
        // 村田の座標そのもの
        val atMurata = LocationConstants.MURATA_YASU
        assertEquals(BusDirection.TO_YASU, BusDirectionResolver.resolve(atMurata))
    }

    @Test
    fun `AC-004 村田から1km地点では村田発(TO_YASU)になる`() {
        // 村田の北方約1km
        val nearMurata = GeoPoint(35.0870, 136.0657)
        val distance = GeoUtils.distanceMeters(nearMurata, LocationConstants.MURATA_YASU)
        assert(distance <= LocationConstants.MURATA_RADIUS_METERS) {
            "テスト前提: 距離 $distance m が村田半径以内であること"
        }
        assertEquals(BusDirection.TO_YASU, BusDirectionResolver.resolve(nearMurata))
    }

    @Test
    fun `AC-004 野洲駅では野洲駅発(TO_MURATA)になる`() {
        // 野洲駅の座標そのもの（村田から2km以上離れている）
        val atYasu = LocationConstants.YASU_STATION
        val distToMurata = GeoUtils.distanceMeters(atYasu, LocationConstants.MURATA_YASU)
        assert(distToMurata > LocationConstants.MURATA_RADIUS_METERS) {
            "テスト前提: 野洲駅は村田半径外 (距離=$distToMurata m)"
        }
        assertEquals(BusDirection.TO_MURATA, BusDirectionResolver.resolve(atYasu))
    }

    @Test
    fun `遠方(京都)からは野洲駅発(TO_MURATA)になる`() {
        // 京都は村田半径外
        val kyoto = GeoPoint(34.9858, 135.7588)
        assertEquals(BusDirection.TO_MURATA, BusDirectionResolver.resolve(kyoto))
    }

    @Test
    fun `村田半径境界付近の判定`() {
        val murataCenter = LocationConstants.MURATA_YASU
        // 北方向に約2000m移動（緯度約0.018度）
        val boundary = GeoPoint(murataCenter.latitude + 0.018, murataCenter.longitude)
        val dist = GeoUtils.distanceMeters(boundary, murataCenter)
        val expected = if (dist <= LocationConstants.MURATA_RADIUS_METERS) {
            BusDirection.TO_YASU
        } else {
            BusDirection.TO_MURATA
        }
        assertEquals(expected, BusDirectionResolver.resolve(boundary))
    }
}

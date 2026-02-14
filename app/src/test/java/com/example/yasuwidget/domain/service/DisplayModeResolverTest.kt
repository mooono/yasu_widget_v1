package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 表示モード判定テスト（AC-001/002/003）
 *
 * AC-001: 村田から2km以内で BUS_ONLY になる（SYS-REQ-010）
 * AC-002: 野洲駅から1km以内（村田2km外）で TRAIN_AND_BUS になる（SYS-REQ-011）
 * AC-003: 通常は TRAIN_ONLY で電車が上下2本ずつ表示される（SYS-REQ-012）
 */
class DisplayModeResolverTest {

    @Test
    fun `AC-001 村田2km以内ではBUS_ONLYになる`() {
        // 村田事業所の座標そのもの → 0m
        val atMurata = LocationConstants.MURATA_YASU
        assertEquals(DisplayMode.BUS_ONLY, DisplayModeResolver.resolve(atMurata))
    }

    @Test
    fun `AC-001 村田から1km地点ではBUS_ONLYになる`() {
        // 村田の北方約1km
        val nearMurata = GeoPoint(35.0570, 136.0330)
        val distance = GeoUtils.distanceMeters(nearMurata, LocationConstants.MURATA_YASU)
        assert(distance <= LocationConstants.MURATA_RADIUS_METERS) {
            "テスト前提: 距離 $distance m が村田半径以内であること"
        }
        assertEquals(DisplayMode.BUS_ONLY, DisplayModeResolver.resolve(nearMurata))
    }

    @Test
    fun `AC-002 野洲駅1km以内かつ村田2km外ではTRAIN_AND_BUSになる`() {
        // 野洲駅の座標そのもの
        val atYasu = LocationConstants.YASU_STATION
        val distToMurata = GeoUtils.distanceMeters(atYasu, LocationConstants.MURATA_YASU)
        // 野洲駅は村田から約2km以上離れていることを確認
        assert(distToMurata > LocationConstants.MURATA_RADIUS_METERS) {
            "テスト前提: 野洲駅は村田半径外 (距離=$distToMurata m)"
        }
        assertEquals(DisplayMode.TRAIN_AND_BUS, DisplayModeResolver.resolve(atYasu))
    }

    @Test
    fun `AC-003 どちらの半径にも入らない場合はTRAIN_ONLYになる`() {
        // 京都駅（両方から十分離れた場所）
        val kyoto = GeoPoint(34.9858, 135.7588)
        assertEquals(DisplayMode.TRAIN_ONLY, DisplayModeResolver.resolve(kyoto))
    }

    @Test
    fun `村田半径境界付近の判定`() {
        // 村田からちょうど2000mの位置（境界上）
        // 2000m以内なのでBUS_ONLYのはず
        val murataCenter = LocationConstants.MURATA_YASU
        // 北方向に約2000m移動（緯度約0.018度）
        val boundary = GeoPoint(murataCenter.latitude + 0.018, murataCenter.longitude)
        val dist = GeoUtils.distanceMeters(boundary, murataCenter)
        // 境界付近のためモード確認
        val expected = if (dist <= LocationConstants.MURATA_RADIUS_METERS) {
            DisplayMode.BUS_ONLY
        } else {
            // 野洲駅圏内かどうかで判定
            val distToYasu = GeoUtils.distanceMeters(boundary, LocationConstants.YASU_STATION)
            if (distToYasu <= LocationConstants.YASU_RADIUS_METERS) {
                DisplayMode.TRAIN_AND_BUS
            } else {
                DisplayMode.TRAIN_ONLY
            }
        }
        assertEquals(expected, DisplayModeResolver.resolve(boundary))
    }
}

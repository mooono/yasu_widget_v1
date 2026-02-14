package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.GeoPoint

/**
 * バス方向判定（SYS-REQ-020/021）
 *
 * - 村田に近い → 野洲駅方面 (TO_YASU)
 * - それ以外 → 村田方面 (TO_MURATA)
 */
object BusDirectionResolver {

    fun resolve(currentLocation: GeoPoint): BusDirection {
        val distToMurata = GeoUtils.distanceMeters(currentLocation, LocationConstants.MURATA_YASU)
        val distToYasu = GeoUtils.distanceMeters(currentLocation, LocationConstants.YASU_STATION)

        return if (distToMurata < distToYasu) {
            BusDirection.TO_YASU
        } else {
            BusDirection.TO_MURATA
        }
    }
}

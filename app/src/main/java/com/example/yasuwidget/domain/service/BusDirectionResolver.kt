package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.GeoPoint

/**
 * バス方向判定（SYS-REQ-020/021）
 *
 * - 村田半径以内 → 村田発（TO_YASU: 村田→野洲駅方面）
 * - それ以外 → 野洲駅発（TO_MURATA: 野洲駅→村田方面）
 */
object BusDirectionResolver {

    fun resolve(currentLocation: GeoPoint): BusDirection {
        val distToMurata = GeoUtils.distanceMeters(currentLocation, LocationConstants.MURATA_YASU)

        return if (distToMurata <= LocationConstants.MURATA_RADIUS_METERS) {
            BusDirection.TO_YASU
        } else {
            BusDirection.TO_MURATA
        }
    }
}

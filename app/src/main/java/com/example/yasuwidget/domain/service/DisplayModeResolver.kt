package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.constants.LocationConstants
import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.GeoPoint

/**
 * 表示モード判定（SYS-REQ-010/011/012）
 *
 * 優先順位:
 * 1. 村田2km以内 → BUS_ONLY
 * 2. 野洲1km以内（かつ1に該当しない）→ TRAIN_AND_BUS
 * 3. その他 → TRAIN_ONLY
 */
object DisplayModeResolver {

    fun resolve(currentLocation: GeoPoint): DisplayMode {
        val distToMurata = GeoUtils.distanceMeters(currentLocation, LocationConstants.MURATA_YASU)
        val distToYasu = GeoUtils.distanceMeters(currentLocation, LocationConstants.YASU_STATION)

        return when {
            distToMurata <= LocationConstants.MURATA_RADIUS_METERS -> DisplayMode.BUS_ONLY
            distToYasu <= LocationConstants.YASU_RADIUS_METERS -> DisplayMode.TRAIN_AND_BUS
            else -> DisplayMode.TRAIN_ONLY
        }
    }
}

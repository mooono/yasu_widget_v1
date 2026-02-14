package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.GeoPoint
import kotlin.math.*

/**
 * 2点間の距離計算（Haversine公式）
 * Android非依存の純粋関数
 */
object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * 2つのGeoPoint間の距離（メートル）を計算する
     */
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }
}

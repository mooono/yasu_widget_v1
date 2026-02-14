package com.example.yasuwidget.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic coordinate (latitude, longitude)
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calculate distance to another point in meters using Haversine formula
     */
    fun distanceTo(other: GeoPoint): Double {
        val earthRadiusM = 6371000.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadiusM * c
    }
    
    companion object {
        // Fixed coordinates from requirements
        val YASU_STATION = GeoPoint(35.0667, 136.0278)
        val MURATA_YASU = GeoPoint(35.0833, 136.0389)
        
        // Radius constants (meters)
        const val MURATA_RADIUS = 2000.0
        const val YASU_RADIUS = 1000.0
    }
}

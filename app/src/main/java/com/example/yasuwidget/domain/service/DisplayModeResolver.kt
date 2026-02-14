package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.DisplayMode
import com.example.yasuwidget.domain.model.GeoPoint

/**
 * Resolves display mode based on user location
 * SYS-REQ-010: Within MURATA_RADIUS (2km) -> BUS_ONLY
 * SYS-REQ-011: Within YASU_RADIUS (1km) and NOT within MURATA_RADIUS -> TRAIN_AND_BUS
 * SYS-REQ-012: Otherwise -> TRAIN_ONLY
 */
class DisplayModeResolver {
    
    fun resolve(currentLocation: GeoPoint): DisplayMode {
        val distanceToMurata = currentLocation.distanceTo(GeoPoint.MURATA_YASU)
        val distanceToYasu = currentLocation.distanceTo(GeoPoint.YASU_STATION)
        
        return when {
            // SYS-REQ-010: Within 2km of Murata
            distanceToMurata <= GeoPoint.MURATA_RADIUS -> DisplayMode.BUS_ONLY
            
            // SYS-REQ-011: Within 1km of Yasu Station (and not within Murata radius)
            distanceToYasu <= GeoPoint.YASU_RADIUS -> DisplayMode.TRAIN_AND_BUS
            
            // SYS-REQ-012: Otherwise
            else -> DisplayMode.TRAIN_ONLY
        }
    }
}

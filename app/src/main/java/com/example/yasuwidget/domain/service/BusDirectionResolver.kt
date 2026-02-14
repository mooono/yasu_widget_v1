package com.example.yasuwidget.domain.service

import com.example.yasuwidget.domain.model.BusDirection
import com.example.yasuwidget.domain.model.GeoPoint

/**
 * Resolves bus direction based on user location
 * SYS-REQ-020: Closer to Murata -> TO_YASU (towards station)
 * SYS-REQ-021: Otherwise -> TO_MURATA (towards factory)
 */
class BusDirectionResolver {
    
    fun resolve(currentLocation: GeoPoint): BusDirection {
        val distanceToMurata = currentLocation.distanceTo(GeoPoint.MURATA_YASU)
        val distanceToYasu = currentLocation.distanceTo(GeoPoint.YASU_STATION)
        
        return if (distanceToMurata < distanceToYasu) {
            // SYS-REQ-020: Closer to Murata, go to Yasu
            BusDirection.TO_YASU
        } else {
            // SYS-REQ-021: Closer to Yasu, go to Murata
            BusDirection.TO_MURATA
        }
    }
}

package com.example.yasuwidget.domain.model

/**
 * Bus direction based on user location
 * SYS-REQ-020, SYS-REQ-021
 */
enum class BusDirection {
    /** Direction: Murata → Yasu Station */
    TO_YASU,
    
    /** Direction: Yasu Station → Murata */
    TO_MURATA
}

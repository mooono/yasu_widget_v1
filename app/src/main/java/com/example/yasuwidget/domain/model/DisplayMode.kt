package com.example.yasuwidget.domain.model

/**
 * Widget display mode based on user location
 * SYS-REQ-010, SYS-REQ-011, SYS-REQ-012
 */
enum class DisplayMode {
    /** Show only train information */
    TRAIN_ONLY,
    
    /** Show both train and bus information */
    TRAIN_AND_BUS,
    
    /** Show only bus information */
    BUS_ONLY
}

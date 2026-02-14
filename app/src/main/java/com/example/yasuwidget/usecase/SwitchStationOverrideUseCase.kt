package com.example.yasuwidget.usecase

import com.example.yasuwidget.domain.service.TrainStationResolver
import com.example.yasuwidget.infra.store.WidgetStateStore
import com.example.yasuwidget.infra.time.TimeProvider
import java.time.temporal.ChronoUnit

/**
 * Use case for switching station override
 * SYS-REQ-031: Set temporary override with 30-minute expiry
 */
class SwitchStationOverrideUseCase(
    private val stateStore: WidgetStateStore,
    private val timeProvider: TimeProvider
) {
    suspend fun execute(stationId: String) {
        val now = timeProvider.now()
        val expiresAt = now.plus(
            TrainStationResolver.OVERRIDE_DURATION_MINUTES,
            ChronoUnit.MINUTES
        )
        
        stateStore.setOverride(stationId, expiresAt)
    }
    
    suspend fun clearOverride() {
        stateStore.clearOverride()
    }
}

package com.example.yasuwidget.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.yasuwidget.domain.service.*
import com.example.yasuwidget.infra.location.LocationRepository
import com.example.yasuwidget.infra.scheduler.UpdateScheduler
import com.example.yasuwidget.infra.store.WidgetStateStore
import com.example.yasuwidget.infra.timetable.TimetableRepository
import com.example.yasuwidget.infra.time.SystemTimeProvider
import com.example.yasuwidget.usecase.RefreshWidgetUseCase
import com.example.yasuwidget.usecase.SwitchStationOverrideUseCase

/**
 * Widget action: Manual refresh
 * SYS-REQ-044: Manual update triggers immediate refresh
 * UI-REQ-003: Provide manual update button
 */
class RefreshAction : ActionCallback {
    companion object {
        private const val TAG = "RefreshAction"
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            Log.d(TAG, "Manual refresh triggered")
            
            // Execute refresh use case
            val timeProvider = SystemTimeProvider()
            val locationRepository = LocationRepository(context)
            val timetableRepository = TimetableRepository(context)
            val stateStore = WidgetStateStore(context)
            val updateScheduler = UpdateScheduler(context)
            
            val refreshUseCase = RefreshWidgetUseCase(
                context = context,
                timeProvider = timeProvider,
                locationRepository = locationRepository,
                timetableRepository = timetableRepository,
                stateStore = stateStore,
                updateScheduler = updateScheduler,
                displayModeResolver = DisplayModeResolver(),
                busDirectionResolver = BusDirectionResolver(),
                trainStationResolver = TrainStationResolver(),
                serviceDayResolver = ServiceDayResolver(),
                nextDeparturesSelector = NextDeparturesSelector()
            )
            
            refreshUseCase.execute()
            
            // Update widget display
            TransitWidgetReceiver.updateWidget(context)
            
            Log.d(TAG, "Manual refresh completed")
        } catch (e: Exception) {
            Log.e(TAG, "Manual refresh failed", e)
        }
    }
}

/**
 * Widget action: Switch station
 * UI-REQ-002: Provide station switching controls
 * SYS-REQ-031: Set 30-minute override when switching stations
 */
class SwitchStationAction : ActionCallback {
    companion object {
        private const val TAG = "SwitchStationAction"
        
        // Available stations for switching
        private val AVAILABLE_STATIONS = listOf("Yasu", "Nagaokakyo", "Kusatsu")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            Log.d(TAG, "Station switch triggered")
            
            val stateStore = WidgetStateStore(context)
            val timeProvider = SystemTimeProvider()
            
            // Get current override or pinned station
            val currentOverride = stateStore.getOverrideStationId()
            val pinnedStation = stateStore.getPinnedStationId()
            
            // Determine current station (use override if valid, else pinned, else first station)
            val currentStation = currentOverride ?: pinnedStation ?: AVAILABLE_STATIONS.first()
            
            // Find next station in the list (cycle through)
            val currentIndex = AVAILABLE_STATIONS.indexOf(currentStation)
            val nextIndex = (currentIndex + 1) % AVAILABLE_STATIONS.size
            val nextStation = AVAILABLE_STATIONS[nextIndex]
            
            Log.d(TAG, "Switching from $currentStation to $nextStation")
            
            // Set the override using the use case
            val switchUseCase = SwitchStationOverrideUseCase(stateStore, timeProvider)
            switchUseCase.execute(nextStation)
            
            // Trigger refresh to update display
            val locationRepository = LocationRepository(context)
            val timetableRepository = TimetableRepository(context)
            val updateScheduler = UpdateScheduler(context)
            
            val refreshUseCase = RefreshWidgetUseCase(
                context = context,
                timeProvider = timeProvider,
                locationRepository = locationRepository,
                timetableRepository = timetableRepository,
                stateStore = stateStore,
                updateScheduler = updateScheduler,
                displayModeResolver = DisplayModeResolver(),
                busDirectionResolver = BusDirectionResolver(),
                trainStationResolver = TrainStationResolver(),
                serviceDayResolver = ServiceDayResolver(),
                nextDeparturesSelector = NextDeparturesSelector()
            )
            
            refreshUseCase.execute()
            
            // Update widget display
            TransitWidgetReceiver.updateWidget(context)
            
            Log.d(TAG, "Station switch completed")
        } catch (e: Exception) {
            Log.e(TAG, "Station switch failed", e)
        }
    }
}

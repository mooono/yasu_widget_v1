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

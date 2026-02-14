package com.example.yasuwidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.yasuwidget.domain.service.*
import com.example.yasuwidget.infra.location.LocationRepository
import com.example.yasuwidget.infra.scheduler.UpdateScheduler
import com.example.yasuwidget.infra.store.WidgetStateStore
import com.example.yasuwidget.infra.timetable.TimetableRepository
import com.example.yasuwidget.infra.time.SystemTimeProvider
import com.example.yasuwidget.usecase.RefreshWidgetUseCase
import com.example.yasuwidget.usecase.WidgetUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Main Glance Widget implementation
 * UI-REQ-001: Display based on mode
 */
class TransitWidget : GlanceAppWidget() {
    
    companion object {
        private const val TAG = "TransitWidget"
        private val json = Json { ignoreUnknownKeys = true }
    }
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val uiState = loadOrRefreshWidgetState(context)
        
        provideContent {
            WidgetContent(uiState)
        }
    }
    
    private suspend fun loadOrRefreshWidgetState(context: Context): WidgetUiState {
        return try {
            // Try to load cached state first
            val stateStore = WidgetStateStore(context)
            val cachedJson = stateStore.getLastRenderedUiState()
            
            if (cachedJson != null) {
                json.decodeFromString<WidgetUiState>(cachedJson)
            } else {
                // No cache, trigger refresh
                refreshWidget(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load widget state", e)
            // Return minimal error state
            WidgetUiState(
                mode = com.example.yasuwidget.domain.model.DisplayMode.TRAIN_ONLY,
                headerTitle = "エラー",
                train = null,
                bus = null,
                lastUpdatedAtText = "--:--",
                statusMessage = "読み込み失敗"
            )
        }
    }
    
    private suspend fun refreshWidget(context: Context): WidgetUiState = withContext(Dispatchers.IO) {
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
    }
}

/**
 * Widget receiver
 */
class TransitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TransitWidget()
    
    companion object {
        private const val TAG = "TransitWidgetReceiver"
        
        suspend fun updateWidget(context: Context) {
            try {
                TransitWidget().updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget", e)
            }
        }
    }
}

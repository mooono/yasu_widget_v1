package com.example.yasuwidget.presentation

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.yasuwidget.application.RefreshWidgetUseCase
import com.example.yasuwidget.application.SwitchStationOverrideUseCase
import com.example.yasuwidget.infrastructure.location.LocationRepository
import com.example.yasuwidget.infrastructure.scheduler.UpdateScheduler
import com.example.yasuwidget.infrastructure.store.WidgetStateStore
import com.example.yasuwidget.infrastructure.time.SystemTimeProvider
import com.example.yasuwidget.infrastructure.timetable.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget AppWidgetProvider（エントリーポイント）
 *
 * - SYS-REQ-041: 自己再スケジュール更新
 * - SYS-REQ-044: 手動更新
 * - UI-REQ-002: 駅切替
 * - NFR-001: クラッシュ防止
 */
class TransitWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "TransitWidgetProvider"
        const val ACTION_SCHEDULED_UPDATE = "com.example.yasuwidget.ACTION_SCHEDULED_UPDATE"
        const val ACTION_MANUAL_REFRESH = "com.example.yasuwidget.ACTION_MANUAL_REFRESH"
        const val ACTION_STATION_NEXT = "com.example.yasuwidget.ACTION_STATION_NEXT"
        const val ACTION_STATION_PREV = "com.example.yasuwidget.ACTION_STATION_PREV"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        performUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 最初のWidgetが追加されたとき、スケジュール開始
        UpdateScheduler(context).scheduleNextUpdate()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最後のWidgetが削除されたとき、スケジュール停止
        UpdateScheduler(context).cancelSchedule()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // NFR-001: 例外を捕捉しクラッシュさせない
        try {
            when (intent.action) {
                ACTION_SCHEDULED_UPDATE -> {
                    performUpdate(context)
                    UpdateScheduler(context).scheduleNextUpdate()
                }
                ACTION_MANUAL_REFRESH -> {
                    performUpdate(context)
                    UpdateScheduler(context).scheduleNextUpdate()
                }
                ACTION_STATION_NEXT -> {
                    handleStationSwitch(context, next = true)
                }
                ACTION_STATION_PREV -> {
                    handleStationSwitch(context, next = false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onReceive error", e)
        }
    }

    private fun performUpdate(context: Context) {
        scope.launch {
            try {
                val timeProvider = SystemTimeProvider()
                val locationRepository = LocationRepository(context)
                val timetableRepository = TimetableRepository(context)
                val stateStore = WidgetStateStore(context)

                val useCase = RefreshWidgetUseCase(
                    timeProvider = timeProvider,
                    locationRepository = locationRepository,
                    timetableRepository = timetableRepository,
                    stateStore = stateStore
                )

                val uiState = useCase.execute()
                val views = WidgetRenderer.render(context.packageName, uiState)

                // クリックイベントの設定
                setupClickListeners(context, views)

                // すべてのWidgetインスタンスを更新
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TransitWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (id in widgetIds) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "performUpdate error", e)
            }
        }
    }

    private fun handleStationSwitch(context: Context, next: Boolean) {
        try {
            val timeProvider = SystemTimeProvider()
            val stateStore = WidgetStateStore(context)
            val timetableRepository = TimetableRepository(context)

            val useCase = SwitchStationOverrideUseCase(
                timeProvider = timeProvider,
                stateStore = stateStore,
                timetableRepository = timetableRepository
            )

            if (next) useCase.switchToNext() else useCase.switchToPrevious()

            // 切替後にすぐ更新
            performUpdate(context)
        } catch (e: Exception) {
            Log.e(TAG, "handleStationSwitch error", e)
        }
    }

    private fun setupClickListeners(context: Context, views: android.widget.RemoteViews) {
        // 手動更新ボタン（UI-REQ-003）
        views.setOnClickPendingIntent(
            com.example.yasuwidget.R.id.btn_refresh,
            createPendingIntent(context, ACTION_MANUAL_REFRESH, 2001)
        )

        // 駅切替ボタン：前（UI-REQ-002）
        views.setOnClickPendingIntent(
            com.example.yasuwidget.R.id.btn_station_prev,
            createPendingIntent(context, ACTION_STATION_PREV, 2002)
        )

        // 駅切替ボタン：次（UI-REQ-002）
        views.setOnClickPendingIntent(
            com.example.yasuwidget.R.id.btn_station_next,
            createPendingIntent(context, ACTION_STATION_NEXT, 2003)
        )
    }

    private fun createPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TransitWidgetProvider::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

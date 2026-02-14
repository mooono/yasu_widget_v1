package com.example.yasuwidget.presentation

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.yasuwidget.R
import com.example.yasuwidget.application.RefreshWidgetUseCase
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
 * - NFR-001: クラッシュ防止
 */
class TransitWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "TransitWidgetProvider"
        const val ACTION_SCHEDULED_UPDATE = "com.example.yasuwidget.ACTION_SCHEDULED_UPDATE"
        const val ACTION_MANUAL_REFRESH = "com.example.yasuwidget.ACTION_MANUAL_REFRESH"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 即座に初期レイアウトを設定（Null RemoteViews 防止）
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_transit)
            views.setTextViewText(R.id.text_header_title, "読込中…")
            setupClickListeners(context, views)
            appWidgetManager.updateAppWidget(id, views)
        }
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

    private fun setupClickListeners(context: Context, views: android.widget.RemoteViews) {
        // 手動更新ボタン（UI-REQ-003）
        views.setOnClickPendingIntent(
            com.example.yasuwidget.R.id.btn_refresh,
            createPendingIntent(context, ACTION_MANUAL_REFRESH, 2001)
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

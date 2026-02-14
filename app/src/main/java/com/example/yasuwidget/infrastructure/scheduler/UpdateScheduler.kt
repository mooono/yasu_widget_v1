package com.example.yasuwidget.infrastructure.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.yasuwidget.presentation.TransitWidgetProvider

/**
 * 更新スケジューリング（SYS-REQ-041）
 * AlarmManagerを用いた自己再スケジュール型
 * exact alarmが保証されない前提で設計する
 */
class UpdateScheduler(private val context: Context) {

    companion object {
        private const val REQUEST_CODE = 1001
        private const val UPDATE_INTERVAL_MS = 60_000L // 約1分
    }

    /**
     * 次回更新を約1分後にスケジュールする
     */
    fun scheduleNextUpdate() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = createUpdateIntent()
        val triggerAt = System.currentTimeMillis() + UPDATE_INTERVAL_MS

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    intent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    intent
                )
            }
        } catch (e: SecurityException) {
            // exact alarm権限がない場合はsetで代替
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                intent
            )
        }
    }

    /**
     * スケジュールをキャンセルする
     */
    fun cancelSchedule() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(createUpdateIntent())
    }

    private fun createUpdateIntent(): PendingIntent {
        val intent = Intent(context, TransitWidgetProvider::class.java).apply {
            action = TransitWidgetProvider.ACTION_SCHEDULED_UPDATE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

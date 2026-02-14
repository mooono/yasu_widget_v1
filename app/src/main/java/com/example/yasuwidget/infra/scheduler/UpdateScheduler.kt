package com.example.yasuwidget.infra.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.Instant

/**
 * Update scheduler using AlarmManager
 * SYS-REQ-041: Schedule updates approximately every 1 minute
 */
class UpdateScheduler(
    private val context: Context
) {
    companion object {
        private const val TAG = "UpdateScheduler"
        private const val REQUEST_CODE_PERIODIC = 1001
        const val ACTION_UPDATE = "com.example.yasuwidget.ACTION_UPDATE_WIDGET"
        private const val UPDATE_INTERVAL_MS = 60_000L // 1 minute
    }
    
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    /**
     * Schedule next update
     * Note: Exact alarms are not guaranteed by the system
     */
    fun scheduleNextUpdate() {
        val intent = Intent(context, UpdateBroadcastReceiver::class.java).apply {
            action = ACTION_UPDATE
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PERIODIC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextUpdateTime = Instant.now().plusMillis(UPDATE_INTERVAL_MS)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check if exact alarm permission is available
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        nextUpdateTime.toEpochMilli(),
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for ${nextUpdateTime}")
                } else {
                    // Fallback to inexact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        nextUpdateTime.toEpochMilli(),
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm for ${nextUpdateTime}")
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextUpdateTime.toEpochMilli(),
                    pendingIntent
                )
                Log.d(TAG, "Scheduled alarm for ${nextUpdateTime}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
            // Fallback to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextUpdateTime.toEpochMilli(),
                pendingIntent
            )
        }
    }
    
    /**
     * Cancel scheduled updates
     */
    fun cancelScheduledUpdates() {
        val intent = Intent(context, UpdateBroadcastReceiver::class.java).apply {
            action = ACTION_UPDATE
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PERIODIC,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled scheduled updates")
        }
    }
}

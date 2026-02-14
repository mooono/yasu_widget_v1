package com.example.yasuwidget.infra.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver for scheduled widget updates
 */
class UpdateBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "UpdateBroadcastReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            UpdateScheduler.ACTION_UPDATE -> {
                // Trigger widget update
                // This will be implemented when we create the widget receiver
                Log.d(TAG, "Triggering widget update")
                // TODO: Trigger RefreshWidgetUseCase
            }
        }
    }
}

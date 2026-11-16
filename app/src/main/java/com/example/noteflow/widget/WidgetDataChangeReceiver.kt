package com.example.noteflow.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for in-app data-change broadcasts and triggers a full widget refresh.
 * Any component that mutates persisted data should send [ACTION_DATA_CHANGED]
 * to keep every registered widget in sync.
 */
class WidgetDataChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DATA_CHANGED) {
            WidgetRefreshManager.refreshAll(context)
        }
    }

    companion object {
        const val ACTION_DATA_CHANGED =
            "com.example.noteflow.action.WIDGET_DATA_CHANGED"

        fun notifyDataChanged(context: Context) {
            val intent = Intent(ACTION_DATA_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}

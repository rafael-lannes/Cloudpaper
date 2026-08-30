package com.cloudpaper.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cloudpaper.app.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val preferences = AppPreferences(context)
                    val config = preferences.scheduleConfigFlow.first()
                    if (config.isAutoChangeEnabled) {
                        WorkScheduler.scheduleWallpaperRotation(context, config)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

package com.cloudpaper.app

import android.app.Application
import com.cloudpaper.app.data.preferences.AppPreferences
import com.cloudpaper.app.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CloudpaperApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Verify and restore background wallpaper rotation schedule
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = AppPreferences(this@CloudpaperApplication)
            val config = preferences.scheduleConfigFlow.first()
            if (config.isAutoChangeEnabled) {
                WorkScheduler.scheduleWallpaperRotation(this@CloudpaperApplication, config)
            }
        }
    }
}

package com.cloudpaper.app.worker

import android.content.Context
import androidx.work.*
import com.cloudpaper.app.data.model.ScheduleConfig
import java.util.concurrent.TimeUnit

object WorkScheduler {

    const val WALLPAPER_WORK_NAME = "CloudpaperWallpaperChangeWork"

    /**
     * Schedules periodic wallpaper rotation using Android WorkManager.
     */
    fun scheduleWallpaperRotation(context: Context, config: ScheduleConfig) {
        val workManager = WorkManager.getInstance(context)

        if (!config.isAutoChangeEnabled) {
            cancelWallpaperRotation(context)
            return
        }

        // WorkManager minimum periodic interval is 15 minutes
        val intervalMinutes = maxOf(15L, config.intervalMinutes)

        val constraintsBuilder = Constraints.Builder()
        if (config.requireChargingOnly) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            minOf(5L, intervalMinutes / 3), TimeUnit.MINUTES
        )
            .setConstraints(constraintsBuilder.build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WALLPAPER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Cancels scheduled wallpaper rotation.
     */
    fun cancelWallpaperRotation(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WALLPAPER_WORK_NAME)
    }

    /**
     * Triggers an immediate one-time wallpaper change in background.
     */
    fun triggerImmediateChange(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WallpaperChangeWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

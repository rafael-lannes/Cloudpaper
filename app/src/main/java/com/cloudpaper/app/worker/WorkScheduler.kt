package com.cloudpaper.app.worker

import android.content.Context
import androidx.work.*
import com.cloudpaper.app.data.model.ScheduleConfig
import java.util.concurrent.TimeUnit

object WorkScheduler {

    const val WALLPAPER_WORK_NAME = "CloudpaperWallpaperChangeWork"
    const val DRIVE_SYNC_WORK_NAME = "CloudpaperDriveSyncWork"

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
        if (config.requireWifiOnly) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED)
        }
        if (config.requireChargingOnly) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            // Flex interval of 5 minutes or 1/3 of interval
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
     * Schedules periodic background sync for Google Drive.
     */
    fun schedulePeriodicDriveSync(context: Context, intervalHours: Long = 6L, wifiOnly: Boolean = true) {
        val constraintsBuilder = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)

        val workRequest = PeriodicWorkRequestBuilder<DriveSyncWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraintsBuilder.build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DRIVE_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancels scheduled Google Drive sync.
     */
    fun cancelDriveSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DRIVE_SYNC_WORK_NAME)
    }

    /**
     * Triggers an immediate one-time wallpaper change in background.
     */
    fun triggerImmediateChange(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WallpaperChangeWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Triggers an immediate one-time sync with Google Drive in background.
     */
    fun triggerImmediateSync(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

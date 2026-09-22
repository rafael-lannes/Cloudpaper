package com.cloudpaper.app.worker

import android.content.Context
import androidx.work.*
import com.cloudpaper.app.data.model.AutoChangeMode
import com.cloudpaper.app.data.model.ScheduleConfig
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {

    const val WALLPAPER_WORK_NAME = "CloudpaperWallpaperChangeWork"

    /**
     * Schedules periodic or daily wallpaper rotation using Android WorkManager.
     */
    fun scheduleWallpaperRotation(context: Context, config: ScheduleConfig) {
        val workManager = WorkManager.getInstance(context)

        if (!config.isAutoChangeEnabled) {
            cancelWallpaperRotation(context)
            return
        }

        val constraintsBuilder = Constraints.Builder()
        if (config.requireChargingOnly) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val workRequest: PeriodicWorkRequest = when (config.changeMode) {
            AutoChangeMode.DAILY_SCHEDULE -> {
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, config.scheduledHour)
                    set(Calendar.MINUTE, config.scheduledMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (target.before(now) || target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }

                val initialDelayMillis = maxOf(0L, target.timeInMillis - now.timeInMillis)

                PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
                    24, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES
                )
                    .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                    .setConstraints(constraintsBuilder.build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            }

            AutoChangeMode.INTERVAL -> {
                val intervalMinutes = maxOf(15L, config.intervalMinutes)
                PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
                    intervalMinutes, TimeUnit.MINUTES,
                    minOf(5L, intervalMinutes / 3), TimeUnit.MINUTES
                )
                    .setConstraints(constraintsBuilder.build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
            }
        }

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

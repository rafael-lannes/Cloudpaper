package com.cloudpaper.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cloudpaper.app.data.model.AutoChangeMode
import com.cloudpaper.app.data.repository.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WallpaperChangeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = WallpaperRepository(applicationContext)
            val config = repository.preferences.scheduleConfigFlow.first()

            val success = when (config.changeMode) {
                AutoChangeMode.DAILY_SCHEDULE -> repository.changeScheduledDailyWallpaper()
                AutoChangeMode.INTERVAL -> repository.changeRandomWallpaper()
            }

            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

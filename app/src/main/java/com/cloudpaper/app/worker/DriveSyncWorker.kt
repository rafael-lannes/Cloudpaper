package com.cloudpaper.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cloudpaper.app.data.model.SyncResult
import com.cloudpaper.app.data.repository.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = WallpaperRepository(applicationContext)
            val syncResult = repository.syncGoogleDrive()

            when (syncResult) {
                is SyncResult.Success -> Result.success()
                is SyncResult.Error -> Result.retry()
                else -> Result.success()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

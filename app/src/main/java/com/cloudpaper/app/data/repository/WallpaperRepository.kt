package com.cloudpaper.app.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.core.content.FileProvider
import com.cloudpaper.app.data.drive.DriveAuthHelper
import com.cloudpaper.app.data.drive.GoogleDriveService
import com.cloudpaper.app.data.model.ScheduleConfig
import com.cloudpaper.app.data.model.SyncResult
import com.cloudpaper.app.data.model.WallpaperItem
import com.cloudpaper.app.data.model.WallpaperSource
import com.cloudpaper.app.data.model.WallpaperTarget
import com.cloudpaper.app.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class WallpaperRepository(
    private val context: Context,
    val preferences: AppPreferences = AppPreferences(context),
    val driveAuthHelper: DriveAuthHelper = DriveAuthHelper(context),
    val driveService: GoogleDriveService = GoogleDriveService(context)
) {

    /**
     * Internal offline wallpaper storage folder.
     */
    fun getOfflineWallpapersDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
            File(it, "wallpapers")
        } ?: File(context.filesDir, "wallpapers")

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns the absolute path of the offline wallpapers folder.
     */
    fun getOfflineFolderPath(): String {
        return getOfflineWallpapersDir().absolutePath
    }

    /**
     * Returns a user-friendly path representation.
     */
    fun getReadableOfflineFolderPath(): String {
        val absolutePath = getOfflineFolderPath()
        return if (absolutePath.contains("/Android/data/")) {
            "Armazenamento Principal > Android > data > com.cloudpaper.app > files > Pictures > wallpapers"
        } else {
            absolutePath
        }
    }

    /**
     * Copies the offline folder path to the system clipboard.
     */
    fun copyFolderPathToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Caminho Pasta Cloudpaper", getOfflineFolderPath())
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Attempts to open the offline folder in a file manager app.
     */
    fun openOfflineFolderInFileManager(context: Context): Boolean {
        val dir = getOfflineWallpapersDir()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dir
            )

            val intents = listOf(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )

            for (intent in intents) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    // Try next intent
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: copy to clipboard if cannot launch file explorer directly
        copyFolderPathToClipboard(context)
        return false
    }

    /**
     * Lists all wallpapers stored in the offline local cache.
     */
    suspend fun getOfflineWallpapers(): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val dir = getOfflineWallpapersDir()
        val files = dir.listFiles { file ->
            val ext = file.extension.lowercase()
            file.isFile && (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp")
        } ?: emptyArray()

        files.sortedByDescending { it.lastModified() }.map { file ->
            WallpaperItem(
                id = file.name,
                name = file.nameWithoutExtension.removePrefix("drive_"),
                filePath = file.absolutePath,
                fileUri = file.toUri(),
                sizeBytes = file.length(),
                dateModified = file.lastModified(),
                isCloud = file.name.startsWith("drive_")
            )
        }
    }

    /**
     * Imports an image file into the offline folder (e.g. from local gallery or SAF picker).
     */
    suspend fun importImage(uri: Uri, name: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = name ?: "custom_${System.currentTimeMillis()}.jpg"
            val targetFile = File(getOfflineWallpapersDir(), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.exists() && targetFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Deletes a local offline wallpaper file.
     */
    suspend fun deleteOfflineWallpaper(item: WallpaperItem): Boolean = withContext(Dispatchers.IO) {
        try {
            item.filePath?.let {
                val file = File(it)
                if (file.exists()) file.delete() else false
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Clears all offline wallpapers.
     */
    suspend fun clearOfflineWallpapers(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = getOfflineWallpapersDir()
            dir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Applies a bitmap or stream as the device wallpaper for the selected target screen(s).
     */
    suspend fun applyWallpaper(file: File, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileInputStream(file).use { stream ->
                    wallpaperManager.setStream(stream, null, true, target.flag)
                }
            } else {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                wallpaperManager.setBitmap(bitmap)
            }
            preferences.updateLastChanged(file.nameWithoutExtension.removePrefix("drive_"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Executes automatic or manual wallpaper change according to user preferences.
     * Returns true if wallpaper changed successfully.
     */
    suspend fun changeRandomWallpaper(): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = preferences.scheduleConfigFlow.first()
            val driveFolderId = preferences.driveFolderIdFlow.first()
            val account = driveAuthHelper.getLastSignedInAccount()

            var chosenFile: File? = null

            when (config.source) {
                WallpaperSource.GOOGLE_DRIVE -> {
                    // Try downloading a random wallpaper directly from Google Drive
                    if (account != null && !driveFolderId.isNullOrBlank()) {
                        chosenFile = driveService.downloadRandomWallpaper(
                            account = account,
                            folderId = driveFolderId,
                            targetDir = getOfflineWallpapersDir()
                        )
                    }
                    // Fallback to local offline cache if download fails or offline
                    if (chosenFile == null) {
                        chosenFile = pickRandomLocalWallpaper()
                    }
                }

                WallpaperSource.AUTO_SYNC -> {
                    // 1. If connected and configured, sync Drive folder
                    if (account != null && !driveFolderId.isNullOrBlank()) {
                        try {
                            driveService.syncFolderToLocal(
                                account = account,
                                folderId = driveFolderId,
                                localFolder = getOfflineWallpapersDir()
                            )
                            preferences.updateLastSync()
                        } catch (e: Exception) {
                            // Sync failed (offline or network timeout), continue to pick from local cache
                            e.printStackTrace()
                        }
                    }
                    // 2. Pick random wallpaper from the synced local offline cache
                    chosenFile = pickRandomLocalWallpaper()
                }

                WallpaperSource.LOCAL_OFFLINE -> {
                    // Pick directly from offline folder
                    chosenFile = pickRandomLocalWallpaper()
                }
            }

            if (chosenFile != null && chosenFile.exists()) {
                applyWallpaper(chosenFile, config.target)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun pickRandomLocalWallpaper(): File? = withContext(Dispatchers.IO) {
        val wallpapers = getOfflineWallpapers()
        if (wallpapers.isNotEmpty()) {
            val randomItem = wallpapers.random()
            randomItem.filePath?.let { File(it) }
        } else {
            null
        }
    }

    /**
     * Performs a full synchronization with the Google Drive folder.
     */
    suspend fun syncGoogleDrive(
        onProgress: (current: Int, total: Int, currentFileName: String) -> Unit = { _, _, _ -> }
    ): SyncResult = withContext(Dispatchers.IO) {
        val account = driveAuthHelper.getLastSignedInAccount()
            ?: return@withContext SyncResult.Error("Conta Google não conectada. Conecte sua conta na aba Google Drive.")

        val folderId = preferences.driveFolderIdFlow.first()
        if (folderId.isNullOrBlank()) {
            return@withContext SyncResult.Error("Pasta do Google Drive não configurada.")
        }

        val result = driveService.syncFolderToLocal(
            account = account,
            folderId = folderId,
            localFolder = getOfflineWallpapersDir(),
            onProgress = onProgress
        )

        if (result is SyncResult.Success) {
            preferences.updateLastSync()
        }

        result
    }
}

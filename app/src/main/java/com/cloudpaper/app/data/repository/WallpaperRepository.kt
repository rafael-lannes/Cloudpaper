package com.cloudpaper.app.data.repository

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
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

class WallpaperRepository(
    private val context: Context,
    val preferences: AppPreferences = AppPreferences(context)
) {

    /**
     * Default app internal offline wallpaper storage folder.
     */
    fun getDefaultAppWallpapersDir(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
            File(it, "wallpapers")
        } ?: File(context.filesDir, "wallpapers")

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns the absolute path of the default app folder.
     */
    fun getDefaultFolderPath(): String {
        return getDefaultAppWallpapersDir().absolutePath
    }

    /**
     * Returns a user-friendly path representation for the default app folder.
     */
    fun getReadableDefaultFolderPath(): String {
        val absolutePath = getDefaultFolderPath()
        return if (absolutePath.contains("/Android/data/")) {
            "Armazenamento Principal > Android > data > com.cloudpaper.app > files > Pictures > wallpapers"
        } else {
            absolutePath
        }
    }

    /**
     * Copies the default folder path to the system clipboard.
     */
    fun copyFolderPathToClipboard(context: Context, path: String = getDefaultFolderPath()) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Caminho Pasta Cloudpaper", path)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Persists URI permission for a custom folder selected via SAF OpenDocumentTree.
     */
    fun takePersistableUriPermission(treeUri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lists wallpapers from the default app storage folder.
     */
    suspend fun getWallpapersFromDefaultFolder(): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val dir = getDefaultAppWallpapersDir()
        val files = dir.listFiles { file ->
            val ext = file.extension.lowercase()
            file.isFile && (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp")
        } ?: emptyArray()

        files.sortedByDescending { it.lastModified() }.map { file ->
            WallpaperItem(
                id = file.name,
                name = file.nameWithoutExtension,
                filePath = file.absolutePath,
                fileUri = file.toUri(),
                sizeBytes = file.length(),
                dateModified = file.lastModified()
            )
        }
    }

    /**
     * Lists wallpapers from a custom device folder selected via SAF (Storage Access Framework).
     */
    suspend fun getWallpapersFromCustomFolder(treeUri: Uri): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WallpaperItem>()
        try {
            val documentTree = DocumentFile.fromTreeUri(context, treeUri)
            if (documentTree != null && documentTree.isDirectory) {
                val files = documentTree.listFiles()
                for (doc in files) {
                    val mime = doc.type ?: ""
                    val name = doc.name ?: ""
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val isImage = mime.startsWith("image/") || ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"

                    if (doc.isFile && isImage) {
                        list.add(
                            WallpaperItem(
                                id = doc.uri.toString(),
                                name = name.substringBeforeLast('.'),
                                fileUri = doc.uri,
                                sizeBytes = doc.length(),
                                dateModified = doc.lastModified()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortedByDescending { it.dateModified }
    }

    /**
     * Gets wallpapers based on current active source setting.
     */
    suspend fun getActiveWallpapers(): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val config = preferences.scheduleConfigFlow.first()
        when (config.source) {
            WallpaperSource.DEFAULT_APP_FOLDER -> {
                getWallpapersFromDefaultFolder()
            }
            WallpaperSource.CUSTOM_DEVICE_FOLDER -> {
                val customUriString = config.customFolderUriString
                if (!customUriString.isNullOrBlank()) {
                    val customUri = Uri.parse(customUriString)
                    val customWallpapers = getWallpapersFromCustomFolder(customUri)
                    if (customWallpapers.isNotEmpty()) {
                        customWallpapers
                    } else {
                        // Fallback to default app folder if custom folder is empty or inaccessible
                        getWallpapersFromDefaultFolder()
                    }
                } else {
                    getWallpapersFromDefaultFolder()
                }
            }
        }
    }

    /**
     * Imports an image file into the default app folder.
     */
    suspend fun importImage(uri: Uri, name: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = name ?: "custom_${System.currentTimeMillis()}.jpg"
            val targetFile = File(getDefaultAppWallpapersDir(), fileName)

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
     * Deletes a wallpaper file.
     */
    suspend fun deleteWallpaper(item: WallpaperItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (item.filePath != null) {
                val file = File(item.filePath)
                if (file.exists()) file.delete() else false
            } else if (item.fileUri != null) {
                val doc = DocumentFile.fromSingleUri(context, item.fileUri)
                doc?.delete() ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Applies a wallpaper to the selected screen(s).
     */
    suspend fun applyWallpaper(item: WallpaperItem, target: WallpaperTarget): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val inputStream: InputStream? = when {
                item.fileUri != null -> context.contentResolver.openInputStream(item.fileUri)
                item.filePath != null -> FileInputStream(File(item.filePath))
                else -> null
            }

            if (inputStream != null) {
                inputStream.use { stream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setStream(stream, null, true, target.flag)
                    } else {
                        val bitmap = BitmapFactory.decodeStream(stream)
                        wallpaperManager.setBitmap(bitmap)
                    }
                }
                preferences.updateLastChanged(item.name)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Executes automatic or manual random wallpaper change from the active folder.
     */
    suspend fun changeRandomWallpaper(): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = preferences.scheduleConfigFlow.first()
            val wallpapers = getActiveWallpapers()

            if (wallpapers.isNotEmpty()) {
                val randomItem = wallpapers.random()
                applyWallpaper(randomItem, config.target)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Attempts to open the folder in a file manager app.
     */
    fun openFolderInFileManager(context: Context, customUri: Uri? = null): Boolean {
        try {
            if (customUri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(customUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {}
            }

            val dir = getDefaultAppWallpapersDir()
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
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        copyFolderPathToClipboard(context)
        return false
    }
}

package com.cloudpaper.app.data.repository

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.cloudpaper.app.data.model.*
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
     * Cache directory for framed schedule wallpapers.
     */
    private fun getFramingCacheDir(): File {
        val dir = File(context.filesDir, "framed_cache")
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
     * Obtains the native dimensions (width and height) of a WallpaperItem without decoding full bitmap.
     */
    suspend fun getImageDimensions(item: WallpaperItem): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val openStream: () -> InputStream? = {
                when {
                    item.fileUri != null -> context.contentResolver.openInputStream(item.fileUri)
                    item.filePath != null -> FileInputStream(File(item.filePath))
                    else -> null
                }
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                val metrics = context.resources.displayMetrics
                Pair(metrics.widthPixels, metrics.heightPixels)
            }
        } catch (e: Exception) {
            val metrics = context.resources.displayMetrics
            Pair(metrics.widthPixels, metrics.heightPixels)
        }
    }

    /**
     * Decodes a full Bitmap safely from a WallpaperItem.
     */
    private fun decodeBitmapFromItem(item: WallpaperItem, maxDimension: Int = 4096): Bitmap? {
        return try {
            val openStream: () -> InputStream? = {
                when {
                    item.fileUri != null -> context.contentResolver.openInputStream(item.fileUri)
                    item.filePath != null -> FileInputStream(File(item.filePath))
                    else -> null
                }
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Renders a framed wallpaper bitmap, supporting multi-page parallax scrolling or fixed single screen.
     */
    suspend fun renderFramedWallpaper(
        item: WallpaperItem,
        isParallaxMode: Boolean,
        scale: Float,
        panX: Float,
        panY: Float,
        frameWidth: Float,
        frameHeight: Float
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = decodeBitmapFromItem(item) ?: return@withContext null
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val fWidth = if (frameWidth > 0f) frameWidth else screenWidth.toFloat()
            val fHeight = if (frameHeight > 0f) frameHeight else screenHeight.toFloat()

            val scaleRatioX = screenWidth.toFloat() / fWidth
            val scaleRatioY = screenHeight.toFloat() / fHeight

            val outputBitmap: Bitmap
            val matrix = Matrix()

            if (isParallaxMode) {
                // Parallax / Multi-screen scrolling mode:
                // Height matches screen height, width expands to keep full landscape panorama for parallax
                val baseScale = screenHeight.toFloat() / originalBitmap.height.toFloat()
                val totalScale = baseScale * scale

                val outHeight = screenHeight
                val outWidth = maxOf(screenWidth, (originalBitmap.width.toFloat() * totalScale).toInt())

                val tx = (outWidth - originalBitmap.width * totalScale) / 2f + (panX * scaleRatioX)
                val ty = (screenHeight - originalBitmap.height * totalScale) / 2f + (panY * scaleRatioY)

                outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                matrix.postScale(totalScale, totalScale)
                matrix.postTranslate(tx, ty)
            } else {
                // Fixed single-screen mode
                val baseScale = maxOf(
                    screenWidth.toFloat() / originalBitmap.width.toFloat(),
                    screenHeight.toFloat() / originalBitmap.height.toFloat()
                )
                val totalScale = baseScale * scale

                val tx = (screenWidth - originalBitmap.width * totalScale) / 2f + (panX * scaleRatioX)
                val ty = (screenHeight - originalBitmap.height * totalScale) / 2f + (panY * scaleRatioY)

                outputBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
                matrix.postScale(totalScale, totalScale)
                matrix.postTranslate(tx, ty)
            }

            val canvas = Canvas(outputBitmap)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(originalBitmap, matrix, paint)

            if (!originalBitmap.isRecycled && originalBitmap != outputBitmap) {
                originalBitmap.recycle()
            }

            outputBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Applies framed wallpaper to WallpaperManager, preserving parallax/scrolling dimensions on launchers.
     */
    suspend fun applyFramedWallpaper(
        item: WallpaperItem,
        isParallaxMode: Boolean,
        scale: Float,
        panX: Float,
        panY: Float,
        frameWidth: Float,
        frameHeight: Float,
        target: WallpaperTarget
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val bitmap = renderFramedWallpaper(item, isParallaxMode, scale, panX, panY, frameWidth, frameHeight)

            if (bitmap != null) {
                try {
                    wallpaperManager.suggestDesiredDimensions(bitmap.width, bitmap.height)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, target.flag)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }

                bitmap.recycle()
                preferences.updateLastChanged(item.name, item.filePath, item.fileUri?.toString())
                true
            } else {
                applyWallpaper(item, target)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            applyWallpaper(item, target)
        }
    }

    /**
     * Saves a framed bitmap to internal cache for schedule persistence.
     */
    suspend fun saveFramedWallpaperForSchedule(
        day: DayOfWeekItem,
        item: WallpaperItem,
        isParallaxMode: Boolean,
        scale: Float,
        panX: Float,
        panY: Float,
        frameWidth: Float,
        frameHeight: Float
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderFramedWallpaper(item, isParallaxMode, scale, panX, panY, frameWidth, frameHeight) ?: return@withContext null
            val targetFile = File(getFramingCacheDir(), "schedule_framed_${day.id}.jpg")

            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Applies a standard wallpaper to the selected screen(s).
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
                preferences.updateLastChanged(item.name, item.filePath, item.fileUri?.toString())
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
     * Executes scheduled daily wallpaper change for today or a specific day.
     */
    suspend fun changeScheduledDailyWallpaper(forcedDay: DayOfWeekItem? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDay = forcedDay ?: DayOfWeekItem.currentDay()
            val config = preferences.scheduleConfigFlow.first()
            val dayConfig = config.weeklySchedule[targetDay]

            if (dayConfig != null && dayConfig.isEnabled) {
                // If a customized framed image file was saved, apply it directly
                val customPath = dayConfig.customBitmapPath
                if (!customPath.isNullOrBlank()) {
                    val file = File(customPath)
                    if (file.exists() && file.length() > 0) {
                        val wallpaperManager = WallpaperManager.getInstance(context)
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            try {
                                wallpaperManager.suggestDesiredDimensions(bitmap.width, bitmap.height)
                            } catch (e: Exception) {}
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(bitmap, null, true, dayConfig.target.flag)
                            } else {
                                wallpaperManager.setBitmap(bitmap)
                            }
                            bitmap.recycle()
                            preferences.updateLastChanged(dayConfig.wallpaperName, file.absolutePath, null)
                            return@withContext true
                        }
                    }
                }

                // Fall back to original file
                val item = WallpaperItem(
                    id = dayConfig.wallpaperId,
                    name = dayConfig.wallpaperName,
                    fileUri = dayConfig.fileUriString?.let { Uri.parse(it) },
                    filePath = dayConfig.filePath
                )
                val success = applyWallpaper(item, dayConfig.target)
                if (success) return@withContext true
            }

            // Fallback to random change from active folder
            changeRandomWallpaper()
        } catch (e: Exception) {
            e.printStackTrace()
            changeRandomWallpaper()
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

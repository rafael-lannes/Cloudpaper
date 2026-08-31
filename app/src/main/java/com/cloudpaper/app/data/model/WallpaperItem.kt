package com.cloudpaper.app.data.model

import android.net.Uri

/**
 * Represents a wallpaper item stored locally in a folder on the device.
 */
data class WallpaperItem(
    val id: String,
    val name: String,
    val fileUri: Uri? = null,
    val filePath: String? = null,
    val sizeBytes: Long = 0,
    val dateModified: Long = System.currentTimeMillis()
)

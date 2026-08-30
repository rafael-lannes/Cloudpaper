package com.cloudpaper.app.data.model

import android.net.Uri

/**
 * Represents a wallpaper item either stored locally in cache or referenced from the cloud.
 */
data class WallpaperItem(
    val id: String,
    val name: String,
    val fileUri: Uri? = null,
    val filePath: String? = null,
    val sizeBytes: Long = 0,
    val dateModified: Long = System.currentTimeMillis(),
    val isCloud: Boolean = false,
    val cloudFileId: String? = null,
    val thumbnailUrl: String? = null
)

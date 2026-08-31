package com.cloudpaper.app.data.model

/**
 * Configuration for automatic wallpaper rotation.
 */
data class ScheduleConfig(
    val isAutoChangeEnabled: Boolean = false,
    val intervalMinutes: Long = 60L,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val source: WallpaperSource = WallpaperSource.DEFAULT_APP_FOLDER,
    val requireChargingOnly: Boolean = false,
    val lastChangedTimestamp: Long = 0L,
    val currentWallpaperTitle: String? = null,
    val customFolderUriString: String? = null,
    val customFolderDisplayName: String? = null
)

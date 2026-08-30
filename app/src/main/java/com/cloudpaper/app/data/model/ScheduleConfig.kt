package com.cloudpaper.app.data.model

/**
 * Configuration for automatic wallpaper rotation and synchronization.
 */
data class ScheduleConfig(
    val isAutoChangeEnabled: Boolean = false,
    val intervalMinutes: Long = 60L,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val source: WallpaperSource = WallpaperSource.AUTO_SYNC,
    val requireWifiOnly: Boolean = false,
    val requireChargingOnly: Boolean = false,
    val lastChangedTimestamp: Long = 0L,
    val lastSyncTimestamp: Long = 0L,
    val currentWallpaperTitle: String? = null
)

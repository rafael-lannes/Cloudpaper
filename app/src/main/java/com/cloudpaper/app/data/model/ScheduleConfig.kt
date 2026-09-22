package com.cloudpaper.app.data.model

/**
 * Configuration for automatic wallpaper rotation.
 */
data class ScheduleConfig(
    val isAutoChangeEnabled: Boolean = false,
    val changeMode: AutoChangeMode = AutoChangeMode.INTERVAL,
    val intervalMinutes: Long = 60L,
    val scheduledHour: Int = 8,
    val scheduledMinute: Int = 0,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val source: WallpaperSource = WallpaperSource.DEFAULT_APP_FOLDER,
    val requireChargingOnly: Boolean = false,
    val lastChangedTimestamp: Long = 0L,
    val currentWallpaperTitle: String? = null,
    val currentWallpaperUri: String? = null,
    val currentWallpaperPath: String? = null,
    val customFolderUriString: String? = null,
    val customFolderDisplayName: String? = null,
    val weeklySchedule: Map<DayOfWeekItem, DailyWallpaperConfig> = emptyMap()
)

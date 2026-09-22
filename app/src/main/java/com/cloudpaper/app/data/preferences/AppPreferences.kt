package com.cloudpaper.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cloudpaper.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudpaper_settings")

class AppPreferences(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_CHANGE_ENABLED = booleanPreferencesKey("auto_change_enabled")
        val CHANGE_MODE = stringPreferencesKey("change_mode")
        val INTERVAL_MINUTES = longPreferencesKey("interval_minutes")
        val SCHEDULED_HOUR = intPreferencesKey("scheduled_hour")
        val SCHEDULED_MINUTE = intPreferencesKey("scheduled_minute")
        val WALLPAPER_TARGET = stringPreferencesKey("wallpaper_target")
        val WALLPAPER_SOURCE = stringPreferencesKey("wallpaper_source")
        val REQUIRE_CHARGING_ONLY = booleanPreferencesKey("require_charging_only")
        val LAST_CHANGED_TIMESTAMP = longPreferencesKey("last_changed_timestamp")
        val CURRENT_WALLPAPER_TITLE = stringPreferencesKey("current_wallpaper_title")
        val CURRENT_WALLPAPER_URI = stringPreferencesKey("current_wallpaper_uri")
        val CURRENT_WALLPAPER_PATH = stringPreferencesKey("current_wallpaper_path")

        // Custom Local Folder (SAF Uri & display name)
        val CUSTOM_FOLDER_URI = stringPreferencesKey("custom_folder_uri")
        val CUSTOM_FOLDER_DISPLAY_NAME = stringPreferencesKey("custom_folder_display_name")

        // Weekly schedule stored as JSON array
        val WEEKLY_SCHEDULE_JSON = stringPreferencesKey("weekly_schedule_json")
    }

    val scheduleConfigFlow: Flow<ScheduleConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val weeklyJsonStr = preferences[PreferencesKeys.WEEKLY_SCHEDULE_JSON]
            val weeklySchedule = parseWeeklyScheduleJson(weeklyJsonStr)

            ScheduleConfig(
                isAutoChangeEnabled = preferences[PreferencesKeys.AUTO_CHANGE_ENABLED] ?: false,
                changeMode = preferences[PreferencesKeys.CHANGE_MODE]?.let {
                    try { AutoChangeMode.valueOf(it) } catch (e: Exception) { AutoChangeMode.INTERVAL }
                } ?: AutoChangeMode.INTERVAL,
                intervalMinutes = preferences[PreferencesKeys.INTERVAL_MINUTES] ?: 60L,
                scheduledHour = preferences[PreferencesKeys.SCHEDULED_HOUR] ?: 8,
                scheduledMinute = preferences[PreferencesKeys.SCHEDULED_MINUTE] ?: 0,
                target = preferences[PreferencesKeys.WALLPAPER_TARGET]?.let {
                    try { WallpaperTarget.valueOf(it) } catch (e: Exception) { WallpaperTarget.BOTH }
                } ?: WallpaperTarget.BOTH,
                source = preferences[PreferencesKeys.WALLPAPER_SOURCE]?.let {
                    try { WallpaperSource.valueOf(it) } catch (e: Exception) { WallpaperSource.DEFAULT_APP_FOLDER }
                } ?: WallpaperSource.DEFAULT_APP_FOLDER,
                requireChargingOnly = preferences[PreferencesKeys.REQUIRE_CHARGING_ONLY] ?: false,
                lastChangedTimestamp = preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] ?: 0L,
                currentWallpaperTitle = preferences[PreferencesKeys.CURRENT_WALLPAPER_TITLE],
                currentWallpaperUri = preferences[PreferencesKeys.CURRENT_WALLPAPER_URI],
                currentWallpaperPath = preferences[PreferencesKeys.CURRENT_WALLPAPER_PATH],
                customFolderUriString = preferences[PreferencesKeys.CUSTOM_FOLDER_URI],
                customFolderDisplayName = preferences[PreferencesKeys.CUSTOM_FOLDER_DISPLAY_NAME],
                weeklySchedule = weeklySchedule
            )
        }

    val customFolderUriFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.CUSTOM_FOLDER_URI] }
    val customFolderDisplayNameFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.CUSTOM_FOLDER_DISPLAY_NAME] }

    suspend fun setAutoChangeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CHANGE_ENABLED] = enabled
        }
    }

    suspend fun setChangeMode(mode: AutoChangeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHANGE_MODE] = mode.name
        }
    }

    suspend fun setIntervalMinutes(minutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERVAL_MINUTES] = minutes
        }
    }

    suspend fun setScheduledTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCHEDULED_HOUR] = hour
            preferences[PreferencesKeys.SCHEDULED_MINUTE] = minute
        }
    }

    suspend fun setWallpaperTarget(target: WallpaperTarget) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WALLPAPER_TARGET] = target.name
        }
    }

    suspend fun setWallpaperSource(source: WallpaperSource) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WALLPAPER_SOURCE] = source.name
        }
    }

    suspend fun setRequireChargingOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REQUIRE_CHARGING_ONLY] = enabled
        }
    }

    suspend fun updateLastChanged(title: String, filePath: String? = null, uriString: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] = System.currentTimeMillis()
            preferences[PreferencesKeys.CURRENT_WALLPAPER_TITLE] = title
            if (filePath != null) {
                preferences[PreferencesKeys.CURRENT_WALLPAPER_PATH] = filePath
            } else {
                preferences.remove(PreferencesKeys.CURRENT_WALLPAPER_PATH)
            }
            if (uriString != null) {
                preferences[PreferencesKeys.CURRENT_WALLPAPER_URI] = uriString
            } else {
                preferences.remove(PreferencesKeys.CURRENT_WALLPAPER_URI)
            }
        }
    }

    suspend fun setCustomFolder(uriString: String?, displayName: String? = null) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[PreferencesKeys.CUSTOM_FOLDER_URI] = uriString
                preferences[PreferencesKeys.WALLPAPER_SOURCE] = WallpaperSource.CUSTOM_DEVICE_FOLDER.name
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_FOLDER_URI)
            }

            if (displayName != null) {
                preferences[PreferencesKeys.CUSTOM_FOLDER_DISPLAY_NAME] = displayName
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_FOLDER_DISPLAY_NAME)
            }
        }
    }

    suspend fun setDayWallpaper(config: DailyWallpaperConfig) {
        context.dataStore.edit { preferences ->
            val existing = parseWeeklyScheduleJson(preferences[PreferencesKeys.WEEKLY_SCHEDULE_JSON]).toMutableMap()
            existing[config.day] = config
            preferences[PreferencesKeys.WEEKLY_SCHEDULE_JSON] = encodeWeeklyScheduleJson(existing)
        }
    }

    suspend fun removeDayWallpaper(day: DayOfWeekItem) {
        context.dataStore.edit { preferences ->
            val existing = parseWeeklyScheduleJson(preferences[PreferencesKeys.WEEKLY_SCHEDULE_JSON]).toMutableMap()
            existing.remove(day)
            preferences[PreferencesKeys.WEEKLY_SCHEDULE_JSON] = encodeWeeklyScheduleJson(existing)
        }
    }

    suspend fun setWeeklySchedule(schedule: Map<DayOfWeekItem, DailyWallpaperConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEEKLY_SCHEDULE_JSON] = encodeWeeklyScheduleJson(schedule)
        }
    }

    private fun parseWeeklyScheduleJson(jsonStr: String?): Map<DayOfWeekItem, DailyWallpaperConfig> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<DayOfWeekItem, DailyWallpaperConfig>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val config = DailyWallpaperConfig.fromJson(obj)
                if (config != null) {
                    result[config.day] = config
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun encodeWeeklyScheduleJson(schedule: Map<DayOfWeekItem, DailyWallpaperConfig>): String {
        val array = JSONArray()
        for ((_, config) in schedule) {
            array.put(config.toJson())
        }
        return array.toString()
    }
}

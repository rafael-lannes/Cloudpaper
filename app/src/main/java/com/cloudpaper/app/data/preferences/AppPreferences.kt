package com.cloudpaper.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cloudpaper.app.data.model.ScheduleConfig
import com.cloudpaper.app.data.model.WallpaperSource
import com.cloudpaper.app.data.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudpaper_settings")

class AppPreferences(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_CHANGE_ENABLED = booleanPreferencesKey("auto_change_enabled")
        val INTERVAL_MINUTES = longPreferencesKey("interval_minutes")
        val WALLPAPER_TARGET = stringPreferencesKey("wallpaper_target")
        val WALLPAPER_SOURCE = stringPreferencesKey("wallpaper_source")
        val REQUIRE_CHARGING_ONLY = booleanPreferencesKey("require_charging_only")
        val LAST_CHANGED_TIMESTAMP = longPreferencesKey("last_changed_timestamp")
        val CURRENT_WALLPAPER_TITLE = stringPreferencesKey("current_wallpaper_title")

        // Custom Local Folder (SAF Uri & display name)
        val CUSTOM_FOLDER_URI = stringPreferencesKey("custom_folder_uri")
        val CUSTOM_FOLDER_DISPLAY_NAME = stringPreferencesKey("custom_folder_display_name")
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
            ScheduleConfig(
                isAutoChangeEnabled = preferences[PreferencesKeys.AUTO_CHANGE_ENABLED] ?: false,
                intervalMinutes = preferences[PreferencesKeys.INTERVAL_MINUTES] ?: 60L,
                target = preferences[PreferencesKeys.WALLPAPER_TARGET]?.let {
                    try { WallpaperTarget.valueOf(it) } catch (e: Exception) { WallpaperTarget.BOTH }
                } ?: WallpaperTarget.BOTH,
                source = preferences[PreferencesKeys.WALLPAPER_SOURCE]?.let {
                    try { WallpaperSource.valueOf(it) } catch (e: Exception) { WallpaperSource.DEFAULT_APP_FOLDER }
                } ?: WallpaperSource.DEFAULT_APP_FOLDER,
                requireChargingOnly = preferences[PreferencesKeys.REQUIRE_CHARGING_ONLY] ?: false,
                lastChangedTimestamp = preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] ?: 0L,
                currentWallpaperTitle = preferences[PreferencesKeys.CURRENT_WALLPAPER_TITLE],
                customFolderUriString = preferences[PreferencesKeys.CUSTOM_FOLDER_URI],
                customFolderDisplayName = preferences[PreferencesKeys.CUSTOM_FOLDER_DISPLAY_NAME]
            )
        }

    val customFolderUriFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.CUSTOM_FOLDER_URI] }
    val customFolderDisplayNameFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.CUSTOM_FOLDER_DISPLAY_NAME] }

    suspend fun setAutoChangeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CHANGE_ENABLED] = enabled
        }
    }

    suspend fun setIntervalMinutes(minutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERVAL_MINUTES] = minutes
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

    suspend fun updateLastChanged(title: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] = System.currentTimeMillis()
            preferences[PreferencesKeys.CURRENT_WALLPAPER_TITLE] = title
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
}

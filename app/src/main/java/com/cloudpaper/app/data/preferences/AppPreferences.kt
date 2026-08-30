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
        val REQUIRE_WIFI_ONLY = booleanPreferencesKey("require_wifi_only")
        val REQUIRE_CHARGING_ONLY = booleanPreferencesKey("require_charging_only")
        val LAST_CHANGED_TIMESTAMP = longPreferencesKey("last_changed_timestamp")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val CURRENT_WALLPAPER_TITLE = stringPreferencesKey("current_wallpaper_title")

        // Google Drive Settings
        val DRIVE_FOLDER_ID = stringPreferencesKey("drive_folder_id")
        val DRIVE_FOLDER_NAME = stringPreferencesKey("drive_folder_name")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val GOOGLE_ACCOUNT_DISPLAY_NAME = stringPreferencesKey("google_account_display_name")

        // Custom Local Folder (SAF Uri)
        val CUSTOM_LOCAL_FOLDER_URI = stringPreferencesKey("custom_local_folder_uri")
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
                    try { WallpaperSource.valueOf(it) } catch (e: Exception) { WallpaperSource.AUTO_SYNC }
                } ?: WallpaperSource.AUTO_SYNC,
                requireWifiOnly = preferences[PreferencesKeys.REQUIRE_WIFI_ONLY] ?: false,
                requireChargingOnly = preferences[PreferencesKeys.REQUIRE_CHARGING_ONLY] ?: false,
                lastChangedTimestamp = preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] ?: 0L,
                lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L,
                currentWallpaperTitle = preferences[PreferencesKeys.CURRENT_WALLPAPER_TITLE]
            )
        }

    val driveFolderIdFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.DRIVE_FOLDER_ID] }
    val driveFolderNameFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.DRIVE_FOLDER_NAME] }
    val googleAccountEmailFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GOOGLE_ACCOUNT_EMAIL] }
    val googleAccountDisplayNameFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.GOOGLE_ACCOUNT_DISPLAY_NAME] }
    val customLocalFolderUriFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.CUSTOM_LOCAL_FOLDER_URI] }

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

    suspend fun setRequireWifiOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REQUIRE_WIFI_ONLY] = enabled
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

    suspend fun updateLastSync() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun setDriveFolder(folderId: String, folderName: String = "") {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVE_FOLDER_ID] = folderId
            if (folderName.isNotBlank()) {
                preferences[PreferencesKeys.DRIVE_FOLDER_NAME] = folderName
            }
        }
    }

    suspend fun setGoogleAccount(email: String?, displayName: String?) {
        context.dataStore.edit { preferences ->
            if (email != null) {
                preferences[PreferencesKeys.GOOGLE_ACCOUNT_EMAIL] = email
            } else {
                preferences.remove(PreferencesKeys.GOOGLE_ACCOUNT_EMAIL)
            }

            if (displayName != null) {
                preferences[PreferencesKeys.GOOGLE_ACCOUNT_DISPLAY_NAME] = displayName
            } else {
                preferences.remove(PreferencesKeys.GOOGLE_ACCOUNT_DISPLAY_NAME)
            }
        }
    }

    suspend fun setCustomLocalFolderUri(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[PreferencesKeys.CUSTOM_LOCAL_FOLDER_URI] = uriString
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_LOCAL_FOLDER_URI)
            }
        }
    }
}

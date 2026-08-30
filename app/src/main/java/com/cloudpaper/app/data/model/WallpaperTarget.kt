package com.cloudpaper.app.data.model

import android.app.WallpaperManager
import android.os.Build

/**
 * Defines which screen to apply the wallpaper to.
 */
enum class WallpaperTarget(val title: String, val flag: Int) {
    HOME_SCREEN("Tela Inicial", WallpaperManager.FLAG_SYSTEM),
    LOCK_SCREEN("Tela de Bloqueio", WallpaperManager.FLAG_LOCK),
    BOTH("Ambas as Telas", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK);

    companion object {
        fun fromFlag(flag: Int): WallpaperTarget {
            return entries.find { it.flag == flag } ?: BOTH
        }
    }
}

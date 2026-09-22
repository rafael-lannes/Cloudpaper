package com.cloudpaper.app.data.model

import org.json.JSONObject

/**
 * Configuration for a specific day's scheduled wallpaper.
 */
data class DailyWallpaperConfig(
    val day: DayOfWeekItem,
    val wallpaperId: String,
    val wallpaperName: String,
    val fileUriString: String? = null,
    val filePath: String? = null,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val cropScale: Float = 1.0f,
    val cropOffsetX: Float = 0.0f,
    val cropOffsetY: Float = 0.0f,
    val frameWidth: Float = 0.0f,
    val frameHeight: Float = 0.0f,
    val isParallaxMode: Boolean = true,
    val customBitmapPath: String? = null,
    val isEnabled: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("dayId", day.id)
            put("wallpaperId", wallpaperId)
            put("wallpaperName", wallpaperName)
            put("fileUriString", fileUriString ?: "")
            put("filePath", filePath ?: "")
            put("target", target.name)
            put("cropScale", cropScale.toDouble())
            put("cropOffsetX", cropOffsetX.toDouble())
            put("cropOffsetY", cropOffsetY.toDouble())
            put("frameWidth", frameWidth.toDouble())
            put("frameHeight", frameHeight.toDouble())
            put("isParallaxMode", isParallaxMode)
            put("customBitmapPath", customBitmapPath ?: "")
            put("isEnabled", isEnabled)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DailyWallpaperConfig? {
            return try {
                val dayId = json.optInt("dayId", 1)
                val day = DayOfWeekItem.fromId(dayId)
                val wallpaperId = json.getString("wallpaperId")
                val wallpaperName = json.optString("wallpaperName", "Wallpaper")
                val fileUriString = json.optString("fileUriString").takeIf { it.isNotBlank() }
                val filePath = json.optString("filePath").takeIf { it.isNotBlank() }
                val targetStr = json.optString("target", WallpaperTarget.BOTH.name)
                val target = try { WallpaperTarget.valueOf(targetStr) } catch (e: Exception) { WallpaperTarget.BOTH }
                val cropScale = json.optDouble("cropScale", 1.0).toFloat()
                val cropOffsetX = json.optDouble("cropOffsetX", 0.0).toFloat()
                val cropOffsetY = json.optDouble("cropOffsetY", 0.0).toFloat()
                val frameWidth = json.optDouble("frameWidth", 0.0).toFloat()
                val frameHeight = json.optDouble("frameHeight", 0.0).toFloat()
                val isParallaxMode = json.optBoolean("isParallaxMode", true)
                val customBitmapPath = json.optString("customBitmapPath").takeIf { it.isNotBlank() }
                val isEnabled = json.optBoolean("isEnabled", true)

                DailyWallpaperConfig(
                    day = day,
                    wallpaperId = wallpaperId,
                    wallpaperName = wallpaperName,
                    fileUriString = fileUriString,
                    filePath = filePath,
                    target = target,
                    cropScale = cropScale,
                    cropOffsetX = cropOffsetX,
                    cropOffsetY = cropOffsetY,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight,
                    isParallaxMode = isParallaxMode,
                    customBitmapPath = customBitmapPath,
                    isEnabled = isEnabled
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

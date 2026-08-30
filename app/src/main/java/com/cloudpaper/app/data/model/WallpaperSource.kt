package com.cloudpaper.app.data.model

/**
 * Supported wallpaper sources.
 */
enum class WallpaperSource(val displayName: String, val description: String) {
    LOCAL_OFFLINE(
        displayName = "Pasta Local Offline",
        description = "Usa papéis de parede salvos na pasta offline do aplicativo ou pasta do dispositivo."
    ),
    GOOGLE_DRIVE(
        displayName = "Google Drive (Nuvem)",
        description = "Acessa a pasta do Google Drive para baixar papéis de parede aleatórios."
    ),
    AUTO_SYNC(
        displayName = "Google Drive + Sincronismo Offline",
        description = "Sincroniza automaticamente a pasta do Google Drive com a pasta offline local e troca periodicamente."
    )
}

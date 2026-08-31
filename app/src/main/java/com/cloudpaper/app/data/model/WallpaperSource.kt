package com.cloudpaper.app.data.model

/**
 * Supported local wallpaper sources.
 */
enum class WallpaperSource(val displayName: String, val description: String) {
    DEFAULT_APP_FOLDER(
        displayName = "Pasta Padrão do Aplicativo",
        description = "Usa a pasta interna do aplicativo (Pictures/wallpapers no armazenamento do app)."
    ),
    CUSTOM_DEVICE_FOLDER(
        displayName = "Pasta Personalizada do Dispositivo",
        description = "Usa qualquer pasta do seu celular escolhida por você (Downloads, Imagens, Cartão SD, etc.)."
    )
}

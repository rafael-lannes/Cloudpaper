package com.cloudpaper.app.data.model

/**
 * Mode of automatic wallpaper changing.
 */
enum class AutoChangeMode(val title: String, val description: String) {
    INTERVAL(
        title = "Por Intervalo",
        description = "Troca automaticamente a cada X minutos ou horas"
    ),
    DAILY_SCHEDULE(
        title = "Agendamento Diário",
        description = "Define um wallpaper personalizado para cada dia da semana"
    )
}

package com.cloudpaper.app.data.model

import java.util.Calendar

/**
 * Represents a day of the week for scheduled wallpaper changes.
 */
enum class DayOfWeekItem(
    val id: Int,
    val calendarDay: Int,
    val fullName: String,
    val shortName: String
) {
    MONDAY(1, Calendar.MONDAY, "Segunda-feira", "Seg"),
    TUESDAY(2, Calendar.TUESDAY, "Terça-feira", "Ter"),
    WEDNESDAY(3, Calendar.WEDNESDAY, "Quarta-feira", "Qua"),
    THURSDAY(4, Calendar.THURSDAY, "Quinta-feira", "Qui"),
    FRIDAY(5, Calendar.FRIDAY, "Sexta-feira", "Sex"),
    SATURDAY(6, Calendar.SATURDAY, "Sábado", "Sáb"),
    SUNDAY(7, Calendar.SUNDAY, "Domingo", "Dom");

    companion object {
        fun fromCalendar(calendarDay: Int): DayOfWeekItem {
            return entries.find { it.calendarDay == calendarDay } ?: MONDAY
        }

        fun fromId(id: Int): DayOfWeekItem {
            return entries.find { it.id == id } ?: MONDAY
        }

        fun currentDay(): DayOfWeekItem {
            val calendar = Calendar.getInstance()
            return fromCalendar(calendar.get(Calendar.DAY_OF_WEEK))
        }
    }
}

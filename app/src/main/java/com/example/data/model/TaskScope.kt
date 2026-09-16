package com.example.data.model

enum class TaskScope(val displayName: String) {
    DAILY("Today"),
    WEEKLY("Week"),
    MONTHLY("Month"),
    YEARLY("Year");

    companion object {
        fun fromString(value: String): TaskScope =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: if (value.equals("TODAY", ignoreCase = true)) DAILY else DAILY
    }
}

enum class StartOfWeek(val displayName: String, val dayOfWeek: java.time.DayOfWeek) {
    SATURDAY("Saturday", java.time.DayOfWeek.SATURDAY),
    SUNDAY("Sunday", java.time.DayOfWeek.SUNDAY),
    MONDAY("Monday", java.time.DayOfWeek.MONDAY);

    companion object {
        fun fromString(value: String?): StartOfWeek =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: MONDAY
    }
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(value: String?): ThemeMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

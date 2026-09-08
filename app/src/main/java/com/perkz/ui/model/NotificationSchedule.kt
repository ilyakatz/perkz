package com.perkz.ui.model

enum class NotificationSchedule(val label: String) {
    Off("Off"),
    Daily("Daily"),
    Weekly("Weekly");

    companion object {
        fun fromName(name: String?): NotificationSchedule =
            entries.firstOrNull { it.name == name } ?: Off
    }
}

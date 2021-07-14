package de.lolhens.resticui.ui

import java.time.Duration
import java.time.format.DateTimeFormatter

object Formatters {
    val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun durationDaysHours(duration: Duration) = when {
        duration.toHours() < 24 -> "${duration.toHours()} hours"
        duration.toHours() % 24 <= 0 -> "${duration.toHours() / 24} days"
        else -> "${duration.toHours() / 24} days ${duration.toHours()} hours"
    }
}

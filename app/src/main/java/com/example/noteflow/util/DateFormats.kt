package com.example.noteflow.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Central date formatting. java.time is available because minSdk is 26.
 */
object DateFormats {

    private val fullFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    private val listTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val monthDayTimeFormatter =
        DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    private val dateFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

    fun full(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        format(epochMillis, fullFormatter, zoneId)

    fun listLabel(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateTime = instant(epochMillis, zoneId)
        val date = dateTime.toLocalDate()
        val today = LocalDate.now(zoneId)

        return when {
            date == today -> "今天 ${dateTime.format(listTimeFormatter)}"
            date == today.minusDays(1) -> "昨天 ${dateTime.format(listTimeFormatter)}"
            date.year == today.year -> dateTime.format(monthDayTimeFormatter)
            else -> dateTime.format(dateFormatter)
        }
    }

    fun widgetLabel(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        format(epochMillis, monthDayTimeFormatter, zoneId)

    private fun format(
        epochMillis: Long,
        formatter: DateTimeFormatter,
        zoneId: ZoneId,
    ): String = instant(epochMillis, zoneId).format(formatter)

    private fun instant(epochMillis: Long, zoneId: ZoneId) =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId)
}

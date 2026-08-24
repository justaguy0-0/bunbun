package com.example.bunbun.ui.chat

import com.example.bunbun.data.time.parseServerTimestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val russianLocale = Locale.forLanguageTag("ru-RU")

fun parseLastSeenTimestamp(raw: String?): Long? = raw?.let(::parseServerTimestamp)

fun formatLastSeen(
    timestampMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String? {
    if (timestampMillis == null) return null
    val lastSeen = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val date = lastSeen.toLocalDate()
    val time = DateTimeFormatter.ofPattern("HH:mm", russianLocale).format(lastSeen)
    val dateLabel = when {
        date == today -> "сегодня"
        date == today.minusDays(1) -> "вчера"
        date.year == today.year -> DateTimeFormatter.ofPattern("d MMMM", russianLocale).format(lastSeen)
        else -> DateTimeFormatter.ofPattern("d MMMM yyyy", russianLocale).format(lastSeen)
    }
    return "Был в сети $dateLabel в $time"
}

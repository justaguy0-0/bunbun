package com.example.bunbun.ui.chat

import com.example.bunbun.data.local.CachedMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ChatTimelineItem {
    val key: String

    data class DateHeader(val date: LocalDate, val label: String) : ChatTimelineItem {
        override val key: String = "date:$date"
    }

    data class Message(val value: CachedMessage) : ChatTimelineItem {
        override val key: String = value.stableKey
    }
}

fun buildChatTimeline(
    messages: List<CachedMessage>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale("ru"),
): List<ChatTimelineItem> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val result = mutableListOf<ChatTimelineItem>()
    var previousDate: LocalDate? = null
    messages.sortedWith(compareBy(CachedMessage::createdAtMillis, CachedMessage::localId)).forEach { message ->
        val date = Instant.ofEpochMilli(message.createdAtMillis).atZone(zoneId).toLocalDate()
        if (date != previousDate) {
            result += ChatTimelineItem.DateHeader(date, formatDateHeader(date, today, locale))
            previousDate = date
        }
        result += ChatTimelineItem.Message(message)
    }
    return result
}

private fun formatDateHeader(date: LocalDate, today: LocalDate, locale: Locale): String = when (date) {
    today -> "СЕГОДНЯ"
    today.minusDays(1) -> "ВЧЕРА"
    else -> DateTimeFormatter
        .ofPattern(if (date.year == today.year) "d MMMM" else "d MMMM yyyy", locale)
        .format(date)
        .uppercase(locale)
}

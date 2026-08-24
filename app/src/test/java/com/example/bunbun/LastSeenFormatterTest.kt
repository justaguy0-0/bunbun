package com.example.bunbun

import com.example.bunbun.ui.chat.formatLastSeen
import com.example.bunbun.ui.chat.parseLastSeenTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LastSeenFormatterTest {
    private val zone = ZoneId.of("Europe/Saratov")
    private val now = Instant.parse("2026-08-24T12:00:00Z").toEpochMilli()

    @Test fun parsesExplicitUtcTimestamp() {
        assertEquals(Instant.parse("2026-08-24T07:42:00Z").toEpochMilli(), parseLastSeenTimestamp("2026-08-24T07:42:00Z"))
    }

    @Test fun formatsTodayInDeviceTimezone() {
        assertEquals("Был в сети сегодня в 11:42", format("2026-08-24T07:42:00Z"))
    }

    @Test fun formatsYesterdayInDeviceTimezone() {
        assertEquals("Был в сети вчера в 23:17", format("2026-08-23T19:17:00Z"))
    }

    @Test fun formatsDateInCurrentYear() {
        assertEquals("Был в сети 18 августа в 14:32", format("2026-08-18T10:32:00Z"))
    }

    @Test fun formatsDateInAnotherYear() {
        assertEquals("Был в сети 18 августа 2025 в 14:32", format("2025-08-18T10:32:00Z"))
    }

    @Test fun nullIsHidden() {
        assertNull(parseLastSeenTimestamp(null))
        assertNull(formatLastSeen(null, now, zone))
    }

    private fun format(value: String) = formatLastSeen(
        timestampMillis = requireNotNull(parseLastSeenTimestamp(value)),
        nowMillis = now,
        zoneId = zone,
    )
}

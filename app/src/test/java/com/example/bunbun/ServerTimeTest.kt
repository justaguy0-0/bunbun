package com.example.bunbun

import com.example.bunbun.data.time.parseServerTimestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ServerTimeTest {
    @Test fun parsesExplicitUtc() {
        assertEquals(Instant.parse("2026-08-14T23:30:00Z").toEpochMilli(), parseServerTimestamp("2026-08-14T23:30:00Z"))
    }

    @Test fun legacySqlDatetimeIsUtcForCompatibility() {
        assertEquals(Instant.parse("2026-08-14T23:30:00Z").toEpochMilli(), parseServerTimestamp("2026-08-14 23:30:00"))
    }

    @Test fun invalidTimestampIsRejected() {
        assertEquals(null, parseServerTimestamp("not-a-date"))
    }
}

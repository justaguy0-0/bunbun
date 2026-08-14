package com.example.bunbun.data.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val legacyServerFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** Server timestamps are UTC. ISO-8601 is preferred; legacy SQL DATETIME remains supported. */
fun parseServerTimestamp(raw: String): Long? =
    runCatching { Instant.parse(raw).toEpochMilli() }.getOrElse {
        runCatching { LocalDateTime.parse(raw, legacyServerFormat).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
    }

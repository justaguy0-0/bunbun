package com.example.bunbun.ui.common

import java.net.URI

data class DetectedWebLink(
    val start: Int,
    val end: Int,
    val text: String,
    val normalizedUrl: String,
)

private val webUrlCandidate = Regex(
    pattern = """(?i)(?<![\p{L}\p{N}_@])(?:https?://[^\s<>{}\[\]\"']+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(?::\d{1,5})?(?:[/?#][^\s<>{}\[\]\"']*)?)""",
)

private val schemeImmediatelyBeforeCandidate = Regex("""[A-Za-z][A-Za-z0-9+.-]*:(?://)?$""")
private val alwaysTrailingPunctuation = setOf('.', ',', '!', ';', ':')

fun detectWebLinks(text: String): List<DetectedWebLink> = buildList {
    webUrlCandidate.findAll(text).forEach { match ->
        if (hasCustomSchemePrefix(text, match.range.first)) return@forEach

        val start = match.range.first
        val end = trimCandidateEnd(match.value, start)
        if (end <= start) return@forEach

        val visibleText = text.substring(start, end)
        val normalized = normalizeWebUrl(visibleText) ?: return@forEach
        add(DetectedWebLink(start, end, visibleText, normalized))
    }
}

fun normalizeWebUrl(candidate: String): String? {
    val value = candidate.trim()
    if (value.isEmpty() || value.any(Char::isWhitespace)) return null

    val withScheme = when {
        value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("http://", ignoreCase = true) -> value
        value.contains("://") || value.substringBefore(':').let { prefix ->
            value.contains(':') && '.' !in prefix && looksLikeScheme(prefix)
        } -> return null
        else -> "https://$value"
    }

    return runCatching {
        val uri = URI(withScheme)
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) null
        else withScheme
    }.getOrNull()
}

private fun hasCustomSchemePrefix(text: String, candidateStart: Int): Boolean {
    val prefix = text.substring(maxOf(0, candidateStart - 64), candidateStart)
    return schemeImmediatelyBeforeCandidate.containsMatchIn(prefix)
}

private fun trimCandidateEnd(candidate: String, absoluteStart: Int): Int {
    var length = candidate.length
    while (length > 0) {
        val last = candidate[length - 1]
        val shouldTrim = when {
            last in alwaysTrailingPunctuation -> true
            last == '?' -> true
            last == ')' -> candidate.take(length).count { it == ')' } > candidate.take(length).count { it == '(' }
            else -> false
        }
        if (!shouldTrim) break
        length--
    }
    return absoluteStart + length
}

private fun looksLikeScheme(value: String): Boolean =
    value.isNotEmpty() && value.first().isLetter() && value.all { it.isLetterOrDigit() || it in "+-." }

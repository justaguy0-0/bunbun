package com.example.bunbun

import com.example.bunbun.ui.common.detectWebLinks
import com.example.bunbun.ui.common.normalizeWebUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlTextParserTest {
    @Test
    fun detectsStandaloneHttpsUrl() {
        val links = detectWebLinks("https://example.com")

        assertEquals(1, links.size)
        assertEquals(0, links.single().start)
        assertEquals(19, links.single().end)
        assertEquals("https://example.com", links.single().normalizedUrl)
    }

    @Test
    fun detectsUrlInsideRussianTextWithCorrectRange() {
        val text = "Посмотри https://example.com"
        val link = detectWebLinks(text).single()

        assertEquals("https://example.com", text.substring(link.start, link.end))
        assertEquals(9, link.start)
        assertEquals(text.length, link.end)
    }

    @Test
    fun detectsTwoIndependentLinks() {
        val links = detectWebLinks("https://example.com и https://picnic-bk.ru")

        assertEquals(listOf("https://example.com", "https://picnic-bk.ru"), links.map { it.text })
        assertTrue(links[0].end <= links[1].start)
    }

    @Test
    fun excludesSentencePunctuation() {
        val links = detectWebLinks("Ссылка: https://example.com. Потом: https://picnic-bk.ru!?;")

        assertEquals(listOf("https://example.com", "https://picnic-bk.ru"), links.map { it.text })
    }

    @Test
    fun excludesOuterParenthesesButKeepsBalancedUrlParentheses() {
        val links = detectWebLinks("(https://example.com) https://example.com/a_(b)")

        assertEquals(listOf("https://example.com", "https://example.com/a_(b)"), links.map { it.text })
    }

    @Test
    fun normalizesDomainWithoutSchemeToHttps() {
        val link = detectWebLinks("Зайди на picnic-bk.ru").single()

        assertEquals("picnic-bk.ru", link.text)
        assertEquals("https://picnic-bk.ru", link.normalizedUrl)
        assertEquals("https://picnic-bk.ru", normalizeWebUrl("picnic-bk.ru"))
    }

    @Test
    fun keepsPortAndPathOnDomainWithoutScheme() {
        val link = detectWebLinks("Стенд: picnic-bk.ru:8443/status").single()

        assertEquals("picnic-bk.ru:8443/status", link.text)
        assertEquals("https://picnic-bk.ru:8443/status", link.normalizedUrl)
    }

    @Test
    fun ignoresOrdinaryTextAndNumericLookalikes() {
        assertTrue(detectWebLinks("обычный текст без ссылки test hello abc 12.34 1.2.3 @username").isEmpty())
    }

    @Test
    fun keepsLongPathAndQueryParameters() {
        val url = "https://example.com/some/really/long/path?utm_source=test&something=verylong"
        val link = detectWebLinks("Вот статья: $url.").single()

        assertEquals(url, link.text)
        assertEquals(url, link.normalizedUrl)
    }

    @Test
    fun detectsUrlNextToCyrillicText() {
        val links = detectWebLinks("Посмотри вот это https://picnic-bk.ru/ — прикольная штука")

        assertEquals("https://picnic-bk.ru/", links.single().text)
    }

    @Test
    fun forbiddenSchemesAreNotDetectedOrNormalized() {
        val text = "javascript:alert(1) file:///tmp/a intent://example.com data:text/plain,hello"

        assertTrue(detectWebLinks(text).isEmpty())
        listOf("javascript:alert(1)", "file:///tmp/a", "intent://example.com", "data:text/plain,hello")
            .forEach { assertEquals(null, normalizeWebUrl(it)) }
    }

    @Test
    fun doesNotExtractWebUrlFromInsideCustomScheme() {
        assertTrue(detectWebLinks("javascript:https://example.com file:picnic-bk.ru").isEmpty())
    }

    @Test
    fun preservesQueryButDropsBareTrailingQuestionMark() {
        val links = detectWebLinks("https://example.com/path?q=test? https://picnic-bk.ru?")

        assertEquals("https://example.com/path?q=test", links[0].text)
        assertEquals("https://picnic-bk.ru", links[1].text)
    }
}

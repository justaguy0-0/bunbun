package com.example.bunbun

import com.example.bunbun.ui.settings.versionLabel
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsConstantsTest {
    @Test fun githubReleasesLinkIsAValidCentralizedHttpsUri() {
        val uri = URI.create(AppLinks.GITHUB_RELEASES_URL)
        assertEquals("https", uri.scheme)
        assertEquals("github.com", uri.host)
        assertTrue(uri.path.endsWith("/releases"))
    }

    @Test fun versionLabelUsesGeneratedVersionName() {
        assertEquals("Bunbun ${BuildConfig.VERSION_NAME}", versionLabel(BuildConfig.VERSION_NAME))
    }
}

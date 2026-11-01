package com.tsutsen.platformplayer

import com.tsutsen.platformplayer.core.data.update.GithubReleaseUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseUpdateTest {
    @Test fun newerTagsAreDetected() {
        assertTrue(GithubReleaseUpdate.isNewer("v0.8.0-thor.1", "0.7.3-thor.1"))
        assertTrue(GithubReleaseUpdate.isNewer("v0.7.3-thor.2", "0.7.3-thor.1"))
        assertTrue(GithubReleaseUpdate.isNewer("v1.0.0", "0.9.9-thor.9"))
    }

    @Test fun sameOrOlderTagsAreNotUpdates() {
        assertFalse(GithubReleaseUpdate.isNewer("v0.7.3-thor.1", "0.7.3-thor.1"))
        assertFalse(GithubReleaseUpdate.isNewer("v0.7.2-thor.9", "0.7.3-thor.1"))
        assertFalse(GithubReleaseUpdate.isNewer("not-a-version", "0.7.3-thor.1"))
    }

    @Test fun missingTrailingPartsCountAsZero() {
        assertFalse(GithubReleaseUpdate.isNewer("v0.8", "0.8.0"))
        assertTrue(GithubReleaseUpdate.isNewer("v0.8.1", "0.8"))
    }

    @Test fun parsesReleaseJson() {
        val json = """{"url":"x","html_url":"https://github.com/Tufein/Twinjay/releases/tag/v0.8.0-thor.1",
            "author":{"html_url":"https://github.com/someone"},"tag_name":"v0.8.0-thor.1",
            "assets":[{"browser_download_url":"https://github.com/a/b/releases/download/v0.8.0-thor.1/SHA256SUMS.txt"},
            {"browser_download_url":"https://github.com/a/b/releases/download/v0.8.0-thor.1/TwinJay-v0.8.0-thor.1-arm64.apk"}]}"""
        val release = GithubReleaseUpdate.parse(json)
        assertNotNull(release)
        assertEquals("v0.8.0-thor.1", release!!.tag)
        assertEquals("https://github.com/Tufein/Twinjay/releases/tag/v0.8.0-thor.1", release.pageUrl)
        assertEquals(
            "https://github.com/a/b/releases/download/v0.8.0-thor.1/TwinJay-v0.8.0-thor.1-arm64.apk",
            release.apkUrl,
        )
    }

    @Test fun garbageIsRejected() {
        assertNull(GithubReleaseUpdate.parse("{}"))
    }
}

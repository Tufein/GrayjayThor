package com.tsutsen.platformplayer.core.data.update

import java.net.HttpURLConnection
import java.net.URL

/** A published GitHub release of TwinJay. */
data class ReleaseInfo(
    val tag: String,
    val pageUrl: String,
    val apkUrl: String?,
)

/**
 * Update check against the repository's latest non-prerelease GitHub release.
 * Deliberately independent of the upstream Bluejay update feed, which Thor
 * builds must never contact.
 */
object GithubReleaseUpdate {
    const val LATEST_RELEASE_URL = "https://api.github.com/repos/Tufein/Twinjay/releases/latest"

    /** "v0.8.0-thor.1" / "0.7.3-thor.1" -> [0, 8, 0, 1]; non-numbers are ignored. */
    fun versionParts(version: String): List<Int> =
        Regex("\\d+").findAll(version.removePrefix("v")).mapNotNull { it.value.toIntOrNull() }.toList()

    /** True when [tag] is a strictly newer version than [current]. */
    fun isNewer(tag: String, current: String): Boolean {
        val a = versionParts(tag)
        val b = versionParts(current)
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /**
     * Reads the fields we need from GitHub's release JSON. The release's own
     * `html_url` is the first one in the document; asset URLs end in `.apk`.
     */
    fun parse(json: String): ReleaseInfo? {
        val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: return null
        val page = Regex("\"html_url\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
            ?: "https://github.com/Tufein/Twinjay/releases/tag/$tag"
        val apk = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"").findAll(json)
            .map { it.groupValues[1] }
            .let { all -> all.firstOrNull { it.contains("arm64") } ?: all.firstOrNull() }
        return ReleaseInfo(tag, page, apk)
    }

    /** Blocking; call off the main thread. Returns null when offline or unparseable. */
    fun fetchLatest(): ReleaseInfo? {
        val conn = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "TwinJay")
            if (conn.responseCode != 200) null
            else parse(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }
}

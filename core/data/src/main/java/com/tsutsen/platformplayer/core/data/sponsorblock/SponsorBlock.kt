package com.tsutsen.platformplayer.core.data.sponsorblock

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/** A part of a video that SponsorBlock voters marked as skippable. */
data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String,
)

/**
 * Minimal SponsorBlock client (https://sponsor.ajay.app). Only the YouTube
 * video id's hash prefix is sent, never the id itself.
 */
object SponsorBlock {
    private val CATEGORIES = listOf("sponsor", "selfpromo", "interaction", "intro", "outro")

    /** YouTube video id from a watch / short / youtu.be / embed URL, else null. */
    fun videoId(url: String): String? {
        val id =
            Regex("""(?:[?&]v=|youtu\.be/|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})""")
                .find(url)?.groupValues?.get(1)
        return id
    }

    /** First 4 hex chars of the SHA-256 of [videoId], as the privacy API expects. */
    fun hashPrefix(videoId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(videoId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(4)

    /**
     * Segments for [videoId] out of a hash-prefix response, which lists every
     * video sharing the prefix. Only plain "skip" segments are kept.
     */
    fun parse(json: String, videoId: String): List<SponsorSegment> {
        val block = json.split("\"videoID\"").drop(1).firstOrNull {
            Regex("""^\s*:\s*"${Regex.escape(videoId)}"""").containsMatchIn(it)
        } ?: return emptyList()
        return Regex("""\{[^{}]*"segment"[^{}]*\}""").findAll(block).mapNotNull { m ->
            val obj = m.value
            val times =
                Regex(""""segment"\s*:\s*\[\s*([0-9.]+)\s*,\s*([0-9.]+)\s*]""").find(obj) ?: return@mapNotNull null
            val action = Regex(""""actionType"\s*:\s*"(\w+)"""").find(obj)?.groupValues?.get(1) ?: "skip"
            val category = Regex(""""category"\s*:\s*"(\w+)"""").find(obj)?.groupValues?.get(1) ?: return@mapNotNull null
            if (action != "skip") return@mapNotNull null
            val start = (times.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null) * 1000
            val end = (times.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null) * 1000
            if (end <= start) null else SponsorSegment(start.toLong(), end.toLong(), category)
        }.sortedBy { it.startMs }.toList()
    }

    /** Blocking; call off the main thread. Empty on any failure or when nothing is marked. */
    fun fetch(videoId: String): List<SponsorSegment> {
        val cats = URLEncoder.encode(CATEGORIES.joinToString(",", "[", "]") { "\"$it\"" }, "UTF-8")
        val actions = URLEncoder.encode("[\"skip\"]", "UTF-8")
        val conn =
            URL("https://sponsor.ajay.app/api/skipSegments/${hashPrefix(videoId)}?categories=$cats&actionTypes=$actions")
                .openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.setRequestProperty("User-Agent", "TwinJay")
            if (conn.responseCode != 200) emptyList()
            else parse(conn.inputStream.bufferedReader().use { it.readText() }, videoId)
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Where to seek to when [positionMs] lies inside a segment (with the
     * playhead at least a little past its start so a manual seek back is not
     * fought forever), or null. A skipped segment is not skipped twice.
     */
    fun skipTarget(segments: List<SponsorSegment>, positionMs: Long, alreadySkipped: Set<SponsorSegment>): SponsorSegment? =
        segments.firstOrNull { it !in alreadySkipped && positionMs >= it.startMs && positionMs < it.endMs - 500 }
}

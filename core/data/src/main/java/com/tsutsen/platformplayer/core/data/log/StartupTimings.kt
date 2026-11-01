package com.tsutsen.platformplayer.core.data.log

import android.util.Log

/**
 * Measures how long each step between tapping a video and the first frame
 * takes, so slow starts can be pinned on a step instead of guessed. The last
 * few measurements are logged (tag "StartupTimings") and added to the
 * playback stall log.
 */
object StartupTimings {
    private const val MAX_KEPT = 10
    private val recent = ArrayDeque<String>()
    private var url: String? = null
    private var startMs = 0L
    private val steps = mutableListOf<Pair<String, Long>>()

    @Synchronized
    fun begin(videoUrl: String) {
        url = videoUrl
        startMs = (System.nanoTime() / 1_000_000)
        steps.clear()
    }

    /** Records that [step] finished now (ms since [begin]). Ignored when no start is pending. */
    @Synchronized
    fun mark(step: String) {
        if (url == null) return
        steps += step to ((System.nanoTime() / 1_000_000) - startMs)
    }

    /** Ends the pending measurement with the first rendered state. */
    @Synchronized
    fun finish() {
        val u = url ?: return
        mark("ready")
        val line = format(u, steps)
        Log.i("StartupTimings", line)
        recent.addLast(line)
        while (recent.size > MAX_KEPT) recent.removeFirst()
        url = null
    }

    @Synchronized
    fun recentLines(): List<String> = recent.toList()

    internal fun format(url: String, steps: List<Pair<String, Long>>): String =
        "start $url: " + steps.joinToString(", ") { "${it.first}=${it.second}ms" }
}

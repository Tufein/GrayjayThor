package com.tsutsen.platformplayer.core.data.repository.impl

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostics for "playback froze" reports. When the player has been stuck for
 * a while it saves the player's own state plus this app's recent logcat to
 * Download/twinjay-playback-log-*.txt, so the file can be shared without adb.
 * It only reads this app's own process log, needs no permissions and runs once
 * per stall.
 */
internal class PlaybackStallReporter(
    private val context: Context,
) {
    private var lastPositionMs = -1L
    private var stuckSinceMs = 0L
    private var reported = false

    /** Call regularly (e.g. from the position ticker). */
    fun onTick(player: ExoPlayer, nowMs: Long = System.currentTimeMillis()) {
        val stuckState =
            player.playWhenReady &&
                player.playbackState == androidx.media3.common.Player.STATE_BUFFERING
        val position = player.currentPosition
        if (!stuckState || position != lastPositionMs) {
            lastPositionMs = position
            stuckSinceMs = nowMs
            if (!stuckState) reported = false
            return
        }
        if (!reported && nowMs - stuckSinceMs >= STALL_MS) {
            reported = true
            val snapshot = describe(player, nowMs - stuckSinceMs)
            Thread { save(snapshot) }.start()
        }
    }

    private fun describe(player: ExoPlayer, stuckMs: Long): String =
        buildString {
            appendLine("Twinjay playback stall report")
            appendLine("stuckFor=${stuckMs}ms")
            appendLine("playbackState=${player.playbackState} playWhenReady=${player.playWhenReady}")
            appendLine("isLoading=${player.isLoading} isPlaying=${player.isPlaying}")
            appendLine("position=${player.currentPosition}ms bufferedPosition=${player.bufferedPosition}ms")
            appendLine("totalBufferedDuration=${player.totalBufferedDuration}ms duration=${player.duration}ms")
            appendLine("videoSize=${player.videoSize.width}x${player.videoSize.height}")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
            appendLine("recent video starts:")
            com.tsutsen.platformplayer.core.data.log.StartupTimings.recentLines().forEach { appendLine("  $it") }
            appendLine()
        }

    private fun save(header: String) {
        try {
            val log =
                Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-t", "4000", "--pid=${android.os.Process.myPid()}"))
                    .inputStream.bufferedReader().use { it.readText() }
            val name = "twinjay-playback-log-" +
                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
            val text = header + log
            val where =
                if (Build.VERSION.SDK_INT >= 29) {
                    val values =
                        ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, name)
                            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                    val uri =
                        context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            ?: error("MediaStore insert failed")
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
                    "Download/$name"
                } else {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    File(dir, name).writeText(text)
                    File(dir, name).absolutePath
                }
            Log.i(TAG, "Saved playback stall report to $where")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Playback log saved: $where", Toast.LENGTH_LONG).show()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to save playback stall report", e)
        }
    }

    private companion object {
        const val TAG = "PlaybackStallReporter"
        const val STALL_MS = 10_000L
    }
}

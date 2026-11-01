package com.tsutsen.platformplayer.core.data.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Finds the newest `twinjay-playback-log-*.txt` that the stall reporter saved
 * to Downloads and builds a share sheet for it. Reading files the app wrote
 * itself needs no storage permission.
 */
object PlaybackLogShare {
    private const val PREFIX = "twinjay-playback-log-"

    /** Newest saved playback log, or null if none (or API < 29). */
    fun latestLogUri(context: Context): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
            arrayOf("$PREFIX%"),
            "${MediaStore.Downloads.DATE_ADDED} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return Uri.withAppendedPath(collection, cursor.getLong(0).toString())
            }
        }
        return null
    }

    /** Share-sheet intent for the newest log, or null when there is none. */
    fun shareIntent(context: Context): Intent? {
        val uri = latestLogUri(context) ?: return null
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TwinJay playback log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(send, "Share playback log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

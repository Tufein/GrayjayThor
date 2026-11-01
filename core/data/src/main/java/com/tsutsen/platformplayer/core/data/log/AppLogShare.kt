package com.tsutsen.platformplayer.core.data.log

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves this app's own recent logcat to Download/twinjay-app-log-*.txt and
 * offers it through the share sheet, so problems can be reported without adb.
 * Only the app's own process log is read; no permissions are needed.
 */
object AppLogShare {
    /** Blocking; call off the main thread. Returns the saved file's Uri, or null on failure / API < 29. */
    fun save(context: Context): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            val pm = context.packageManager.getPackageInfo(context.packageName, 0)
            val log =
                Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-t", "20000", "--pid=${android.os.Process.myPid()}"))
                    .inputStream.bufferedReader().use { it.readText() }
            val header =
                "TwinJay app log\nversion=${pm.versionName}\n" +
                    "device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT}\n\n"
            val name =
                "twinjay-app-log-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            val uri =
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write((header + log).toByteArray()) }
            uri
        } catch (_: Throwable) {
            null
        }
    }

    fun shareIntent(uri: Uri): Intent {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TwinJay app log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(send, "Share app log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

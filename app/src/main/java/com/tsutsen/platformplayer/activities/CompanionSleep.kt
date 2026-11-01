package com.tsutsen.platformplayer.activities

import android.os.SystemClock
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/** When the second screen should go dark while a video plays on the main screen. */
internal object CompanionSleepPolicy {
    const val IDLE_MS = 20_000L

    fun shouldSleep(
        enabled: Boolean,
        isPlaying: Boolean,
        idleMs: Long,
        delayMs: Long = IDLE_MS,
    ): Boolean = enabled && isPlaying && idleMs >= delayMs
}

/**
 * Blacks out the second screen (and drops its backlight) after a few seconds
 * without touch while a video is playing. The first touch only wakes it; it
 * also wakes when playback pauses or stops.
 */
@Composable
internal fun CompanionSleepLayer(
    enabled: Boolean,
    isPlaying: Boolean,
    window: Window?,
    content: @Composable () -> Unit,
) {
    var lastTouch by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var asleep by remember { mutableStateOf(false) }

    LaunchedEffect(enabled, isPlaying) {
        lastTouch = SystemClock.elapsedRealtime()
        while (true) {
            asleep = CompanionSleepPolicy.shouldSleep(enabled, isPlaying, SystemClock.elapsedRealtime() - lastTouch)
            delay(1_000)
        }
    }

    DisposableEffect(asleep, window) {
        val w = window
        val saved = w?.attributes?.screenBrightness
        if (asleep && w != null && saved != null) {
            w.attributes = w.attributes.apply { screenBrightness = 0f }
        }
        onDispose {
            if (asleep && w != null && saved != null) {
                w.attributes = w.attributes.apply { screenBrightness = saved }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastTouch = SystemClock.elapsedRealtime()
                        }
                    }
                },
    ) {
        content()
        if (asleep) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                lastTouch = SystemClock.elapsedRealtime()
                                asleep = false
                            }
                        },
            )
        }
    }
}

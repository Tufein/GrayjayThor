package com.futo.platformplayer.thor

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.KeyEvent
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.media3.common.Player
import com.futo.platformplayer.BuildConfig
import com.futo.platformplayer.R
import com.futo.platformplayer.Settings
import com.futo.platformplayer.activities.MainActivity
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.states.StatePlayer
import java.lang.ref.WeakReference

/** The activities share Grayjay's existing player; this class never creates a second decoder. */
object ThorDisplaySession : DisplayManager.DisplayListener {
    private const val EXTRA_VIDEO_DISPLAY = "thor.video_display"
    private var owner = WeakReference<MainActivity>(null)
    private var output = WeakReference<ThorVideoActivity>(null)
    private var displays: DisplayManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var launching = false
    private var outputVisible = false
    private var failedDisplay: Int? = null

    val isOutputActive: Boolean get() = launching || outputVisible

    // onResume runs before the decor view is attached on a cold launch.
    // The activity's WindowManager already knows the launch display at that point.
    @Suppress("DEPRECATION")
    private fun displayId(activity: Activity): Int = activity.windowManager.defaultDisplay.displayId

    fun layout(context: Context): ThorScreenPolicy.Layout? {
        val manager = context.getSystemService(DisplayManager::class.java)
        val screens = manager.displays.filter {
            it.isValid && it.flags and Display.FLAG_PRIVATE == 0 && it.state != Display.STATE_OFF
        }.map {
            val mode = it.mode
            ThorScreenPolicy.Screen(it.displayId, mode.physicalWidth, mode.physicalHeight,
                it.displayId == Display.DEFAULT_DISPLAY)
        }
        return ThorScreenPolicy.select(screens, Settings.instance.thor.swapScreens)
    }

    fun onMainResumed(activity: MainActivity) {
        if (!BuildConfig.IS_THOR_BUILD) return
        owner = WeakReference(activity)
        if (displays == null) {
            displays = activity.applicationContext.getSystemService(DisplayManager::class.java)
            displays?.registerDisplayListener(this, handler)
        }
        refresh()
    }

    private fun refresh() {
        val activity = owner.get() ?: return
        if (!Settings.instance.thor.enabled || layout(activity) == null) {
            closeOutput()
            return
        }
        val selected = layout(activity) ?: return
        val currentDisplay = displayId(activity)
        // Deep links may open Grayjay on a different display. Never cover the controls with video.
        val target = if (selected.video != currentDisplay) selected.video else selected.controls
        val existing = output.get()
        if (existing != null && !existing.isFinishing) {
            if (displayId(existing) == target) {
                if (outputVisible) return
            } else {
                closeOutput()
            }
        }
        if (launching || failedDisplay == target ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val intent = Intent(activity, ThorVideoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_VIDEO_DISPLAY, target)
        launching = true
        try {
            activity.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(target).toBundle())
        } catch (e: RuntimeException) {
            launching = false
            failedDisplay = target
            Logger.w("ThorDisplay", "Cannot open the video display", e)
            Toast.makeText(activity, R.string.thor_output_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    fun onOutputCreated(activity: ThorVideoActivity): Boolean {
        val main = owner.get()
        if (main == null || main.isFinishing || !Settings.instance.thor.enabled) {
            launching = false
            return false
        }
        val outputDisplay = displayId(activity)
        val requestedDisplay = activity.intent.getIntExtra(EXTRA_VIDEO_DISPLAY, outputDisplay)
        if (outputDisplay != requestedDisplay || outputDisplay == displayId(main)) {
            launching = false
            failedDisplay = requestedDisplay
            Toast.makeText(main, R.string.thor_output_unavailable, Toast.LENGTH_LONG).show()
            return false
        }
        output = WeakReference(activity)
        launching = false
        return true
    }

    fun onOutputStarted(activity: ThorVideoActivity) {
        if (output.get() === activity) {
            outputVisible = true
            launching = false
        }
    }

    fun onOutputStopped(activity: ThorVideoActivity) {
        if (output.get() !== activity) return
        outputVisible = false
        owner.get()?.onThorOutputStopped()
    }

    fun onOutputDestroyed(activity: ThorVideoActivity) {
        if (output.get() !== activity) return
        output.clear()
        outputVisible = false
        launching = false
    }

    fun onMainDestroyed(activity: MainActivity) {
        if (owner.get() !== activity || activity.isChangingConfigurations) return
        closeOutput()
        displays?.unregisterDisplayListener(this)
        displays = null
        owner.clear()
        failedDisplay = null
    }

    private fun closeOutput() {
        launching = false
        outputVisible = false
        output.get()?.finish()
    }

    fun bringControlsToFront() {
        val activity = owner.get() ?: return
        val id = displayId(activity)
        activity.startActivity(Intent(activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle())
    }

    fun handleKey(event: KeyEvent): Boolean {
        val activity = owner.get() ?: return false
        if (!isOutputActive || !Settings.instance.thor.gamepad) return false
        if (event.keyCode !in setOf(KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)) return false
        val player = StatePlayer.instance.getPlayerOrCreate(activity).player
        if (player.mediaItemCount == 0) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_START -> when {
                    player.playbackState == Player.STATE_ENDED -> { player.seekToDefaultPosition(); player.play() }
                    player.playWhenReady -> player.pause()
                    else -> player.play()
                }
                KeyEvent.KEYCODE_BUTTON_L1 -> if (player.isCurrentMediaItemSeekable)
                    player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                KeyEvent.KEYCODE_BUTTON_R1 -> if (player.isCurrentMediaItemSeekable) {
                    val position = player.currentPosition + 10_000
                    player.seekTo(if (player.duration > 0) position.coerceAtMost(player.duration) else position)
                }
            }
        }
        return true
    }

    override fun onDisplayAdded(displayId: Int) { failedDisplay = null; handler.post { refresh() } }
    override fun onDisplayRemoved(displayId: Int) { failedDisplay = null; handler.post { refresh() } }
    override fun onDisplayChanged(displayId: Int) { handler.post { refresh() } }
}

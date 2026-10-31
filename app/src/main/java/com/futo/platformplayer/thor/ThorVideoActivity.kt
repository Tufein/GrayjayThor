package com.futo.platformplayer.thor

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.futo.platformplayer.R
import com.futo.platformplayer.states.StatePlayer
import com.futo.platformplayer.video.PlayerManager

/** Video and subtitles only. Navigation and all existing controls stay in MainActivity. */
@UnstableApi
class ThorVideoActivity : Activity() {
    private var playerState: StatePlayer? = null
    private var manager: PlayerManager? = null
    private lateinit var video: PlayerView
    private lateinit var idle: TextView
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ThorDisplaySession.onOutputCreated(this)) {
            finish()
            return
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        video = PlayerView(this).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setKeepContentOnPlayerReset(false)
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setBackgroundColor(Color.BLACK)
            contentDescription = getString(R.string.thor_settings_title)
        }
        root.addView(video, FrameLayout.LayoutParams(-1, -1))
        idle = TextView(this).apply {
            text = getString(R.string.thor_choose_video)
            setTextColor(Color.LTGRAY)
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }
        root.addView(idle, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        WindowInsetsControllerCompat(window, root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val state = StatePlayer.instance
        playerState = state
        manager = state.getPlayerOrCreate(applicationContext)
        manager?.player?.addListener(listener)
        state.onPlayerClosed.subscribe(this) { updateStatus() }
        updateStatus()
    }

    override fun onStart() {
        super.onStart()
        if (!::video.isInitialized) return
        manager?.setExternalView(video)
        ThorDisplaySession.onOutputStarted(this)
    }

    override fun onStop() {
        if (::video.isInitialized) manager?.clearExternalView(video)
        ThorDisplaySession.onOutputStopped(this)
        super.onStop()
    }

    override fun onDestroy() {
        playerState?.onPlayerClosed?.remove(this)
        manager?.player?.removeListener(listener)
        if (::video.isInitialized) manager?.clearExternalView(video)
        ThorDisplaySession.onOutputDestroyed(this)
        super.onDestroy()
    }

    private fun updateStatus() {
        if (!::idle.isInitialized) return
        val player = manager?.player
        // Upstream's legacy isOpen flag is never set by the current detail view.
        // Media3's timeline also covers local files, downloaded videos and live streams.
        val hasMedia = (player?.mediaItemCount ?: 0) > 0
        idle.visibility = if (hasMedia) View.GONE else View.VISIBLE
        if (hasMedia) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        ThorDisplaySession.handleKey(event) || super.dispatchKeyEvent(event)

    @Deprecated("Delegates navigation to the controls display")
    override fun onBackPressed() { ThorDisplaySession.bringControlsToFront() }
}

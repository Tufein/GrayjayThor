package com.futo.platformplayer.video

import android.media.session.PlaybackState
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.futo.platformplayer.R

class PlayerManager {
    private var _currentView: PlayerView? = null;
    private var _attachedView: PlayerView? = null;
    private var _externalView: PlayerView? = null;
    private var _currentStateName: String? = null;
    private var _externalStatus: TextView? = null;
    private val _stateMap = HashMap<String, PlayerState>();
    private var _currentState: PlayerState? = null;
    val currentState: PlayerState get() {
        if(_currentState == null)
            throw java.lang.IllegalStateException("Attempted to access CurrentState while no state is set");
        else
            return _currentState!!;
    };

    val player: ExoPlayer;

    constructor(exoPlayer: ExoPlayer) {
        this.player = exoPlayer;
    }


    fun getPlaybackStateCompat() : Int {
        return when(player.playbackState) {
            ExoPlayer.STATE_READY -> if(player.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED;
            ExoPlayer.STATE_BUFFERING -> PlaybackState.STATE_BUFFERING;
            else -> PlaybackState.STATE_NONE
        }
    }

    @Synchronized
    fun attach(view: PlayerView, stateName: String) {
        if (view != _currentView || stateName != _currentStateName) {
            _currentView = view
            _currentStateName = stateName
            switchState(stateName)
            updateVideoOutput()
        }
    }

    fun detach() {
        removeExternalStatus()
        _attachedView?.player = null
        _attachedView = null
        _currentView = null
        _currentStateName = null
    }

    /** Keep DetailPlayer's listener and state while moving only its video surface. */
    fun setExternalView(view: PlayerView) {
        _externalView = view
        updateVideoOutput()
    }

    fun clearExternalView(view: PlayerView?) {
        if (view == null || _externalView !== view) return
        _externalView = null
        updateVideoOutput()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun updateVideoOutput() {
        val target = if (_currentView != null && _currentStateName == "DetailPlayer")
            _externalView ?: _currentView else _currentView
        removeExternalStatus()
        val local = _currentView
        if (local != null && target != null && target === _externalView) {
            _externalStatus = TextView(local.context).apply {
                setText(R.string.thor_local_controls)
                setTextColor(Color.LTGRAY)
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }
            local.overlayFrameLayout?.addView(_externalStatus, FrameLayout.LayoutParams(-1, -1))
        }
        if (target === _attachedView) return
        PlayerView.switchTargetView(player, _attachedView, target)
        _attachedView = target
    }

    private fun removeExternalStatus() {
        _externalStatus?.let { (it.parent as? ViewGroup)?.removeView(it) }
        _externalStatus = null
    }

    fun getState(name: String): PlayerState {
        if(!_stateMap.containsKey(name))
            _stateMap[name] = PlayerState();
        return _stateMap[name]!!;
    }
    fun modifyState(name: String, cb: (PlayerState) -> Unit) {
        val state = getState(name);
        val previousListener = state.listener;
        cb(state);
        if(_currentState == state) {
            applyState(state);
            val newListener = state.listener;
            if(previousListener !== newListener) {
                if(previousListener != null)
                    player.removeListener(previousListener);
                if(newListener != null)
                    player.addListener(newListener);
            }
        }
    }
    fun switchState(name: String) {
        val newState = getState(name);
        applyState(newState);

        if(_currentState != newState) {

            if(_currentState?.listener != null)
                player.removeListener(_currentState!!.listener!!);
            if(newState.listener != null)
                player.addListener(newState.listener!!);

            _currentState = newState;
        }
    }
    fun applyState(state: PlayerState) {
        player.volume = if(state.muted) 0f else state.volume;
    }

    fun setMuted(muted: Boolean) {
        currentState.muted = muted;
        applyState(currentState);
    }
    fun setVolume(volume: Float) {
        currentState.volume = volume;
        applyState(currentState);
    }
    fun setListener(listener: Player.Listener) {
        if(currentState.listener == listener)
            return;
        if(currentState.listener != null)
            player.removeListener(currentState.listener!!);
        currentState.listener = listener;
        player.addListener(listener);
    }

    fun release(){
        detach();
        _externalView = null;
        player.release();
    }

    class PlayerState {
        var muted: Boolean = false;
        var volume: Float = 1f;

        var listener: Player.Listener? = null;
    }
}

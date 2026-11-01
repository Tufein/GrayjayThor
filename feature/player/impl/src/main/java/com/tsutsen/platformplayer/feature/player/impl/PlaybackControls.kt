package com.tsutsen.platformplayer.feature.player.impl

import com.tsutsen.platformplayer.core.datastore.model.ControllerPreferences
import com.tsutsen.platformplayer.core.model.PlayerControllerActions

internal object PlayerSeekBounds {
    fun clamp(positionMs: Long, durationMs: Long, endPaddingMs: Long = 0): Long {
        val position = positionMs.coerceAtLeast(0)
        return if (durationMs > 0) position.coerceAtMost((durationMs - endPaddingMs.coerceAtLeast(0)).coerceAtLeast(0))
        else position
    }

    fun relative(positionMs: Long, deltaMs: Long, durationMs: Long): Long {
        val position = positionMs.coerceAtLeast(0)
        val target = if (deltaMs > 0 && position > Long.MAX_VALUE - deltaMs) Long.MAX_VALUE
        else position + deltaMs
        return clamp(target, durationMs)
    }
}

internal object ControllerBindings {
    fun actionFor(keyCode: Int, preferences: ControllerPreferences): String? {
        val actions = PlayerControllerActions.ALL
        // Explicit assignments win over other actions' defaults. Ignore removed
        // action IDs; a remapped action must no longer react to its former key.
        return actions.firstOrNull { preferences.mappings[it.id]?.keyCode == keyCode }?.id
            ?: actions.firstOrNull { it.id !in preferences.mappings && it.defaultKeyCode == keyCode }?.id
    }
}

/** Temporary speed modifiers keyed by physical input, independent of delayed UI ticks. */
internal class ControllerSpeedHold {
    private val held = linkedMapOf<Pair<Int, Int>, String>()
    private var baseline: Float? = null

    fun press(deviceId: Int, keyCode: Int, action: String, speed: Float): Float? {
        if (action != PlayerControllerActions.SPEED_UP && action != PlayerControllerActions.SPEED_DOWN) return null
        val key = deviceId to keyCode
        if (key in held) return null
        if (baseline == null) baseline = speed.coerceIn(0.25f, 4f)
        held[key] = action
        return effectiveSpeed()
    }

    fun release(deviceId: Int, keyCode: Int): Float? {
        if (held.remove(deviceId to keyCode) == null) return null
        val result = effectiveSpeed()
        if (held.isEmpty()) baseline = null
        return result
    }

    fun cancel(): Float? {
        val original = baseline
        held.clear()
        baseline = null
        return original
    }

    private fun effectiveSpeed(): Float {
        val up = PlayerControllerActions.SPEED_UP in held.values
        val down = PlayerControllerActions.SPEED_DOWN in held.values
        val multiplier = when { up && !down -> 2f; down && !up -> 0.5f; else -> 1f }
        return ((baseline ?: 1f) * multiplier).coerceIn(0.25f, 4f)
    }
}

package com.tsutsen.platformplayer.feature.player.impl

import com.tsutsen.platformplayer.core.datastore.model.ControllerBinding
import com.tsutsen.platformplayer.core.datastore.model.ControllerPreferences
import com.tsutsen.platformplayer.core.model.PlayerControllerActions as Actions
import org.junit.Assert.*
import org.junit.Test

class PlaybackControlsTest {
    @Test fun unknownDurationAllowsRelativeSeeking() {
        assertEquals(25_000L, PlayerSeekBounds.relative(15_000, 10_000, -9223372036854775807L))
        assertEquals(5_000L, PlayerSeekBounds.relative(15_000, -10_000, 0))
    }
    @Test fun seeksStopAtTheKnownMediaBoundaries() {
        assertEquals(0L, PlayerSeekBounds.relative(5_000, -10_000, 30_000))
        assertEquals(30_000L, PlayerSeekBounds.relative(25_000, 10_000, 30_000))
    }
    @Test fun shortClipsDoNotProduceAnInvalidSeekRange() {
        assertEquals(0L, PlayerSeekBounds.clamp(80, 100, endPaddingMs = 500))
        assertEquals(29_500L, PlayerSeekBounds.clamp(30_000, 30_000, endPaddingMs = 500))
    }
    @Test fun veryLargeForwardSeekCannotWrapNegative() {
        assertEquals(Long.MAX_VALUE, PlayerSeekBounds.relative(Long.MAX_VALUE - 1, 10_000, 0))
    }
    @Test fun negativePositionsAndUnknownDurationsAreSafe() {
        assertEquals(0L, PlayerSeekBounds.clamp(-100, -1))
        assertEquals(10_000L, PlayerSeekBounds.relative(-1, 10_000, -1))
    }
    @Test fun remappingReplacesTheOldDefaultKey() {
        val default = Actions.ALL.first { it.id == Actions.PLAY_PAUSE }.defaultKeyCode
        val preferences = ControllerPreferences(mappings = mapOf(Actions.PLAY_PAUSE to ControllerBinding(900)))
        assertEquals(Actions.PLAY_PAUSE, ControllerBindings.actionFor(900, preferences))
        assertNull(ControllerBindings.actionFor(default, preferences))
    }
    @Test fun explicitBindingWinsOverAnotherActionsDefault() {
        val playKey = Actions.ALL.first { it.id == Actions.PLAY_PAUSE }.defaultKeyCode
        val preferences = ControllerPreferences(mappings = mapOf(Actions.SEEK_FORWARD to ControllerBinding(playKey)))
        assertEquals(Actions.SEEK_FORWARD, ControllerBindings.actionFor(playKey, preferences))
    }
    @Test fun unknownLegacyActionsDoNotStealExistingKeys() {
        val playKey = Actions.ALL.first { it.id == Actions.PLAY_PAUSE }.defaultKeyCode
        val preferences = ControllerPreferences(mappings = mapOf("removed_action" to ControllerBinding(playKey)))
        assertEquals(Actions.PLAY_PAUSE, ControllerBindings.actionFor(playKey, preferences))
    }
    @Test fun clearingBindingRestoresItsDefault() {
        val playKey = Actions.ALL.first { it.id == Actions.PLAY_PAUSE }.defaultKeyCode
        assertEquals(Actions.PLAY_PAUSE, ControllerBindings.actionFor(playKey, ControllerPreferences()))
    }
    @Test fun overlappingSpeedHoldsRestoreTheRemainingModifier() {
        val hold = ControllerSpeedHold()
        assertEquals(3f, hold.press(1, 10, Actions.SPEED_UP, 1.5f))
        assertEquals(1.5f, hold.press(1, 11, Actions.SPEED_DOWN, 3f))
        assertEquals(0.75f, hold.release(1, 10))
        assertEquals(1.5f, hold.release(1, 11))
    }
    @Test fun reverseReleaseOrderAlsoRestoresTheOriginalSpeed() {
        val hold = ControllerSpeedHold()
        hold.press(1, 10, Actions.SPEED_UP, 1f)
        hold.press(1, 11, Actions.SPEED_DOWN, 2f)
        assertEquals(2f, hold.release(1, 11))
        assertEquals(1f, hold.release(1, 10))
    }
    @Test fun twoControllersCanHoldTheSameActionIndependently() {
        val hold = ControllerSpeedHold()
        assertEquals(2f, hold.press(1, 10, Actions.SPEED_UP, 1f))
        assertEquals(2f, hold.press(2, 10, Actions.SPEED_UP, 2f))
        assertEquals(2f, hold.release(1, 10))
        assertEquals(1f, hold.release(2, 10))
    }
    @Test fun duplicatePressAndUnrelatedReleaseDoNotAlterSpeed() {
        val hold = ControllerSpeedHold()
        hold.press(1, 10, Actions.SPEED_UP, 1f)
        assertNull(hold.press(1, 10, Actions.SPEED_UP, 2f))
        assertNull(hold.release(2, 10))
        assertEquals(1f, hold.release(1, 10))
    }
    @Test fun holdsRespectSpeedLimitsAndRestoreUnclampedBaseline() {
        val hold = ControllerSpeedHold()
        assertEquals(4f, hold.press(1, 10, Actions.SPEED_UP, 3f))
        assertEquals(3f, hold.release(1, 10))
        assertEquals(0.25f, hold.press(1, 11, Actions.SPEED_DOWN, 0.25f))
        assertEquals(0.25f, hold.release(1, 11))
    }
    @Test fun cancellationReturnsBaselineOnlyOnce() {
        val hold = ControllerSpeedHold()
        hold.press(1, 10, Actions.SPEED_UP, 1.25f)
        assertEquals(1.25f, hold.cancel())
        assertNull(hold.cancel())
        assertNull(hold.release(1, 10))
    }
}

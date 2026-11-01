package com.tsutsen.platformplayer.activities

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tsutsen.platformplayer.core.model.PlayerControllerActions
import com.tsutsen.platformplayer.core.ui.GamepadKeyBus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamepadInputResetTest {
    @Test fun stopReleasesHeldKeysToThePlayerAndSubscribersAndAcceptsTheNextPress() = onInputBus {
        val playerEdges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        val flowEdges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { playerEdges.add(it); true }
        runBlocking {
            val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                GamepadKeyBus.events.collect { flowEdges.add(it) }
            }
            try {
                assertTrue(GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_DOWN)))
                GamepadKeyBus.releasePressedInputs()
                yield()
                assertEquals(listOf(true, false), playerEdges.map { it.isPress })
                assertEquals(playerEdges, flowEdges)

                GamepadKeyBus.releasePressedInputs()
                assertEquals(2, playerEdges.size)
                assertTrue(GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_DOWN)))
                assertEquals(listOf(true, false, true), playerEdges.map { it.isPress })
            } finally {
                collector.cancelAndJoin()
            }
        }
    }

    @Test fun stopClearsHeldAnalogDirectionsSoTheSameDirectionCanPressAgain() = onInputBus {
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        val pushed = axisMotion(0.8f)
        try {
            assertTrue(GamepadKeyBus.onMotionEvent(pushed))
            GamepadKeyBus.releasePressedInputs()
            assertTrue(GamepadKeyBus.onMotionEvent(pushed))
            assertEquals(listOf(true, false, true), edges.map { it.isPress })
            assertTrue(edges.all { it.keyCode == PlayerControllerActions.KEY_LEFT_STICK_RIGHT })
        } finally {
            pushed.recycle()
        }
    }

    @Test fun heldPlayerActionsReleaseDuringCaptureWithoutChangingCaptureOwnership() = onInputBus {
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        assertTrue(GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_DOWN)))
        GamepadKeyBus.beginCapture()
        GamepadKeyBus.releasePressedInputs()
        assertEquals(listOf(true, false), edges.map { it.isPress })
        assertTrue(GamepadKeyBus.isCapturing)
        GamepadKeyBus.endCapture()
        assertFalse(GamepadKeyBus.isCapturing)
    }

    @Test fun directionReversalDeliversTheReleaseAndNewPressEvenWhenBothAreHandled() = onInputBus {
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        val right = axisMotion(0.8f)
        val left = axisMotion(-0.8f)
        try {
            assertTrue(GamepadKeyBus.onMotionEvent(right))
            assertTrue(GamepadKeyBus.onMotionEvent(left))
            assertEquals(listOf(
                PlayerControllerActions.KEY_LEFT_STICK_RIGHT to true,
                PlayerControllerActions.KEY_LEFT_STICK_RIGHT to false,
                PlayerControllerActions.KEY_LEFT_STICK_LEFT to true,
            ), edges.map { it.keyCode to it.isPress })
        } finally {
            right.recycle()
            left.recycle()
        }
    }

    @Test fun disconnectReleasesOnlyTheRemovedControllersKeys() = onInputBus {
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_DOWN, TEST_DEVICE_ID))
        GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_DOWN, TEST_DEVICE_ID + 1))
        GamepadKeyBus.releasePressedInputs(TEST_DEVICE_ID)
        assertEquals(listOf(TEST_DEVICE_ID to true, (TEST_DEVICE_ID + 1) to true,
            TEST_DEVICE_ID to false), edges.map { it.deviceId to it.isPress })
        assertTrue(GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_UP, TEST_DEVICE_ID + 1)))
        assertEquals((TEST_DEVICE_ID + 1) to false, edges.last().let { it.deviceId to it.isPress })
    }

    @Test fun disconnectClearsOnlyThatControllersAnalogDirections() = onInputBus {
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        val first = axisMotion(0.8f, TEST_DEVICE_ID)
        val second = axisMotion(0.8f, TEST_DEVICE_ID + 1)
        try {
            GamepadKeyBus.onMotionEvent(first)
            GamepadKeyBus.onMotionEvent(second)
            GamepadKeyBus.releasePressedInputs(TEST_DEVICE_ID)
            assertFalse(GamepadKeyBus.onMotionEvent(second))
            assertTrue(GamepadKeyBus.onMotionEvent(first))
            assertEquals(listOf(true, false, true), edges.filter { it.deviceId == TEST_DEVICE_ID }.map { it.isPress })
            assertEquals(listOf(true), edges.filter { it.deviceId == TEST_DEVICE_ID + 1 }.map { it.isPress })
        } finally { first.recycle(); second.recycle() }
    }

    @Test fun removalOfAnUnrelatedDeviceLeavesHeldActionsAlone() = onInputBus {
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_DOWN))
        GamepadKeyBus.releasePressedInputs(TEST_DEVICE_ID + 1)
        assertEquals(1, edges.size)
        assertTrue(GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_UP)))
    }

    private fun axisMotion(x: Float, deviceId: Int = TEST_DEVICE_ID): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply { id = 0 }
        val coordinates = MotionEvent.PointerCoords().apply { this.x = x }
        return MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 1,
            arrayOf(properties), arrayOf(coordinates), 0, 0, 1f, 1f,
            deviceId, 0, InputDevice.SOURCE_JOYSTICK, 0)
    }

    private fun key(action: Int, deviceId: Int = TEST_DEVICE_ID) = KeyEvent(0L, 0L, action, KeyEvent.KEYCODE_BUTTON_START,
        0, 0, deviceId, 0, 0, InputDevice.SOURCE_GAMEPAD)

    private fun onInputBus(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            GamepadKeyBus.releasePressedInputs()
            GamepadKeyBus.endCapture()
            try {
                block()
            } finally {
                GamepadKeyBus.releasePressedInputs()
                GamepadKeyBus.endCapture()
                GamepadKeyBus.setPlayerHandler(null)
            }
        }
    }

    private companion object {
        const val TEST_DEVICE_ID = 47522
    }
}

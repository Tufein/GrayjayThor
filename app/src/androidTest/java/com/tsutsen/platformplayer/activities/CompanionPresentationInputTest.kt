package com.tsutsen.platformplayer.activities

import android.hardware.display.DisplayManager
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tsutsen.platformplayer.core.model.PlayerControllerActions
import com.tsutsen.platformplayer.core.ui.GamepadKeyBus
import com.tsutsen.platformplayer.feature.player.impl.HistoryTracker
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanionPresentationInputTest {
    @Test fun controllerKeyEdgesReachThePlayerFromTheCompanionWindow() = inPresentation { presentation ->
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        try {
            assertTrue(presentation.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START)))
            assertTrue(presentation.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START)))
            assertEquals(listOf(true, false), edges.map { it.isPress })
            assertEquals(listOf(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_START), edges.map { it.keyCode })
        } finally {
            GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START))
        }
    }

    @Test fun unboundControllerKeysKeepTheNormalWindowFallback() = inPresentation { presentation ->
        GamepadKeyBus.setPlayerHandler { false }
        try {
            assertFalse(presentation.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_X)))
            assertFalse(presentation.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_X)))
        } finally {
            GamepadKeyBus.onKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_X))
        }
    }

    @Test fun analogControllerEdgesReachThePlayerFromTheCompanionWindow() = inPresentation { presentation ->
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        val pushed = motion(InputDevice.SOURCE_JOYSTICK, 0.8f)
        val released = motion(InputDevice.SOURCE_JOYSTICK, 0f)
        try {
            assertTrue(presentation.dispatchGenericMotionEvent(pushed))
            assertTrue(presentation.dispatchGenericMotionEvent(released))
            assertEquals(listOf(true, false), edges.map { it.isPress })
            assertTrue(edges.all { it.keyCode == PlayerControllerActions.KEY_LEFT_STICK_RIGHT })
        } finally {
            GamepadKeyBus.onMotionEvent(released)
            pushed.recycle()
            released.recycle()
        }
    }

    @Test fun controllerPointerEmulationDoesNotConvertPixelsIntoSeekCommands() = inPresentation { presentation ->
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        val pointer = motion(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_TOUCHSCREEN, 400f)
        try {
            presentation.dispatchTouchEvent(pointer)
            presentation.dispatchGenericMotionEvent(pointer)
            assertTrue(edges.isEmpty())
        } finally {
            pointer.recycle()
        }
    }

    @Test fun nonPointerControllerMotionCanUseTheTouchDispatchPath() = inPresentation { presentation ->
        val edges = mutableListOf<GamepadKeyBus.GamepadEvent>()
        GamepadKeyBus.setPlayerHandler { edges.add(it); true }
        val pushed = motion(InputDevice.SOURCE_JOYSTICK, 0.8f)
        val released = motion(InputDevice.SOURCE_JOYSTICK, 0f)
        try {
            assertTrue(presentation.dispatchTouchEvent(pushed))
            assertTrue(presentation.dispatchTouchEvent(released))
            assertEquals(listOf(true, false), edges.map { it.isPress })
        } finally {
            GamepadKeyBus.onMotionEvent(released)
            pushed.recycle()
            released.recycle()
        }
    }

    private fun key(action: Int, code: Int) =
        KeyEvent(0L, 0L, action, code, 0, 0, TEST_DEVICE_ID, 0, 0, InputDevice.SOURCE_GAMEPAD)

    private fun motion(source: Int, x: Float): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply { id = 0 }
        val coordinates = MotionEvent.PointerCoords().apply { this.x = x }
        return MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 1,
            arrayOf(properties), arrayOf(coordinates), 0, 0, 1f, 1f,
            TEST_DEVICE_ID, 0, source, 0)
    }

    private fun inPresentation(block: (CompanionPresentation) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, android.R.style.Theme_Material_NoActionBar)
            val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            val presentation = CompanionPresentation(
                context = context,
                display = display,
                playerRepository = unused(),
                libraryRepository = unused(),
                homeRepository = unused(),
                settingsRepository = unused(),
                downloadsRepository = unused(),
                playbackQueueRepository = unused(),
                liveChatRepository = unused(),
                channelRepository = unused(),
                historyTracker = HistoryTracker(unused()),
                subscriptionDao = unused(),
                onChannelClick = {},
                onPlaylistClick = {},
                onWatchStats = {},
            )
            // Dispatch through a real Dialog window without showing Compose or
            // requiring a second physical display, plugin fetches or a database.
            presentation.window!!.decorView
            try {
                block(presentation)
            } finally {
                GamepadKeyBus.endCapture()
                GamepadKeyBus.setPlayerHandler(null)
                presentation.dismiss()
            }
        }
    }

    private inline fun <reified T> unused(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            error("Input dispatch unexpectedly accessed ${T::class.java.simpleName}.${method.name}")
        } as T

    private companion object {
        const val TEST_DEVICE_ID = 47521
    }
}

package com.tsutsen.platformplayer

import android.view.KeyEvent
import com.tsutsen.platformplayer.core.model.PlayerControllerActions
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SuccessorDefaultsTest {
    @Test fun freshInstallUsesFlavorDefaults() {
        val settings = Settings()
        assertEquals(BuildConfig.IS_THOR_BUILD, settings.dualScreen)
        assertEquals(BuildConfig.IS_THOR_BUILD, settings.controller.enabled)
        assertEquals(if (BuildConfig.IS_THOR_BUILD) 10 else 30, settings.controller.seekForwardSeconds)
        assertEquals(!BuildConfig.IS_THOR_BUILD, settings.autoUpdate.isAutoUpdateEnabled())
        if (BuildConfig.IS_THOR_BUILD) {
            assertEquals(KeyEvent.KEYCODE_BUTTON_START, settings.controller.mappings[PlayerControllerActions.PLAY_PAUSE]?.keyCode)
            assertEquals(KeyEvent.KEYCODE_BUTTON_L1, settings.controller.mappings[PlayerControllerActions.SEEK_BACK]?.keyCode)
            assertEquals(KeyEvent.KEYCODE_BUTTON_R1, settings.controller.mappings[PlayerControllerActions.SEEK_FORWARD]?.keyCode)
        }
    }

    @Test fun savedDisabledFeaturesAreNotReenabledWhenLoadingSettings() {
        val settings = Json.decodeFromString<Settings>("""{
            "dualScreen": false,
            "controller": { "enabled": false, "seekForwardSeconds": 45 }
        }""")
        assertFalse(settings.dualScreen)
        assertFalse(settings.controller.enabled)
        assertEquals(45, settings.controller.seekForwardSeconds)
    }

    @Test fun explicitEmptyControllerBindingsRemainUserControlled() {
        val settings = Json.decodeFromString<Settings>("""{"controller":{"mappings":{}}}""")
        assertEquals(emptyMap<String, Any>(), settings.controller.mappings)
    }
}

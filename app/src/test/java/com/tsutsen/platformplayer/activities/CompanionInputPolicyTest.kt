package com.tsutsen.platformplayer.activities

import android.view.InputDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionInputPolicyTest {
    @Test fun normalizedControllerSourcesCanUseTheMotionBus() {
        assertTrue(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_GAMEPAD))
        assertTrue(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_JOYSTICK))
    }

    @Test fun touchAndMouseCoordinatesAreNotTreatedAsStickAxes() {
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_TOUCHSCREEN))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_MOUSE))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_TOUCHPAD))
    }

    @Test fun controllerPointerEmulationRetainsNormalTouchDispatch() {
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(
            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_TOUCHSCREEN,
        ))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(
            InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_MOUSE,
        ))
    }

    @Test fun unrelatedButtonSourcesDoNotBecomeControllerMotion() {
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_KEYBOARD))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_DPAD))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(InputDevice.SOURCE_UNKNOWN))
    }

    @Test fun controllerTaggedTouchpadsDoNotBecomeStickAxes() {
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(
            InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_TOUCHPAD,
        ))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(
            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_TOUCHPAD,
        ))
    }

    @Test fun controllerTaggedRelativePointersDoNotBecomeStickAxes() {
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(
            InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_MOUSE_RELATIVE,
        ))
        assertFalse(CompanionInputPolicy.isNonPointerControllerSource(
            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_TRACKBALL,
        ))
    }
}

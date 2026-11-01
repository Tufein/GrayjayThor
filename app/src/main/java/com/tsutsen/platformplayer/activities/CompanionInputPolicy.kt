package com.tsutsen.platformplayer.activities

import android.view.InputDevice

/** Distinguishes normalized controller axes from touch/mouse pixel coordinates. */
internal object CompanionInputPolicy {
    fun isNonPointerControllerSource(source: Int): Boolean =
        (source and (InputDevice.SOURCE_CLASS_POINTER or
            InputDevice.SOURCE_CLASS_POSITION or InputDevice.SOURCE_CLASS_TRACKBALL)) == 0 &&
            ((source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
}

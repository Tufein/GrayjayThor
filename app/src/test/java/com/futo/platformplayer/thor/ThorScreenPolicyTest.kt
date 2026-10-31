package com.futo.platformplayer.thor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThorScreenPolicyTest {
    private val top = ThorScreenPolicy.Screen(0, 1920, 1080, true)
    private val bottom = ThorScreenPolicy.Screen(7, 1240, 1080, false)

    @Test fun controlsUseSmallerDisplayWithoutAssumingItsId() {
        assertEquals(ThorScreenPolicy.Layout(7, 0), ThorScreenPolicy.select(listOf(top, bottom), false))
    }

    @Test fun swappingExchangesVideoAndControls() {
        assertEquals(ThorScreenPolicy.Layout(0, 7), ThorScreenPolicy.select(listOf(top, bottom), true))
    }

    @Test fun singleScreenUsesNormalGrayjay() {
        assertNull(ThorScreenPolicy.select(listOf(top), false))
        assertNull(ThorScreenPolicy.select(emptyList(), false))
    }

    @Test fun missingPrimaryOrInvalidLowerDisplayFallsBack() {
        assertNull(ThorScreenPolicy.select(listOf(bottom), false))
        assertNull(ThorScreenPolicy.select(listOf(top, bottom.copy(width = 0)), false))
    }

    @Test fun connectedMonitorDoesNotDisplaceTheSmallerThorScreen() {
        val monitor = ThorScreenPolicy.Screen(12, 3840, 2160, false)
        assertEquals(ThorScreenPolicy.Layout(7, 0),
            ThorScreenPolicy.select(listOf(monitor, bottom, top), false))
    }

    @Test fun duplicatePrimaryIsNotTreatedAsSecondScreen() {
        assertNull(ThorScreenPolicy.select(listOf(top, top), false))
    }

    @Test fun equalSizeSecondaryDisplaysHaveStableOrdering() {
        assertEquals(ThorScreenPolicy.Layout(3, 0),
            ThorScreenPolicy.select(listOf(top, bottom, bottom.copy(id = 3)), false))
    }

    @Test fun largeDimensionsDoNotOverflowAreaComparison() {
        val huge = ThorScreenPolicy.Screen(19, 65_536, 65_536, false)
        assertEquals(ThorScreenPolicy.Layout(7, 0),
            ThorScreenPolicy.select(listOf(top, huge, bottom), false))
    }
}

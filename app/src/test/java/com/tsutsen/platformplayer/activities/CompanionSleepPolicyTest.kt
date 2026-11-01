package com.tsutsen.platformplayer.activities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionSleepPolicyTest {
    @Test fun sleepsOnlyWhenEnabledPlayingAndIdle() {
        assertTrue(CompanionSleepPolicy.shouldSleep(true, true, CompanionSleepPolicy.IDLE_MS))
        assertFalse(CompanionSleepPolicy.shouldSleep(false, true, 60_000))
        assertFalse(CompanionSleepPolicy.shouldSleep(true, false, 60_000))
        assertFalse(CompanionSleepPolicy.shouldSleep(true, true, CompanionSleepPolicy.IDLE_MS - 1))
    }
}

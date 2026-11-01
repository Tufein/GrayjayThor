package com.tsutsen.platformplayer.activities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanionDisplayPolicyTest {
    private fun screen(id: Int) = CompanionDisplayPolicy.Screen(id)

    @Test fun aSingleDisplayHasNoCompanion() {
        assertNull(CompanionDisplayPolicy.select(listOf(screen(0)), activityDisplayId = 0))
    }

    @Test fun aSecondDisplayIsSelected() {
        assertEquals(1, CompanionDisplayPolicy.select(listOf(screen(0), screen(1)), activityDisplayId = 0))
    }

    @Test fun mainActivityOnSecondaryDisplayUsesTheDefaultDisplay() {
        assertEquals(0, CompanionDisplayPolicy.select(listOf(screen(0), screen(1)), activityDisplayId = 1))
    }

    @Test fun existingCompanionIsKeptWhenEnumerationOrderChanges() {
        assertEquals(2, CompanionDisplayPolicy.select(
            listOf(screen(0), screen(1), screen(2)), activityDisplayId = 0, currentDisplayId = 2))
    }

    @Test fun aRemovedCompanionIsReplacedByAnotherDisplay() {
        assertEquals(3, CompanionDisplayPolicy.select(
            listOf(screen(0), screen(3)), activityDisplayId = 0, currentDisplayId = 2))
    }

    @Test fun sleepingInvalidAndPrivateDisplaysAreExcluded() {
        val displays = listOf(screen(0), screen(1).copy(awake = false),
            screen(2).copy(valid = false), screen(3).copy(isPrivate = true))
        assertNull(CompanionDisplayPolicy.select(displays, activityDisplayId = 0, currentDisplayId = 1))
    }

    @Test fun aWakingDisplayBecomesAvailableAgain() {
        val asleep = listOf(screen(0), screen(1).copy(awake = false))
        assertNull(CompanionDisplayPolicy.select(asleep, activityDisplayId = 0))
        assertEquals(1, CompanionDisplayPolicy.select(listOf(screen(0), screen(1)), activityDisplayId = 0))
    }

    @Test fun movingTheMainWindowOffItsOldDisplayNeverOverlapsTheCompanion() {
        assertEquals(0, CompanionDisplayPolicy.select(
            listOf(screen(0), screen(1)), activityDisplayId = 1, currentDisplayId = 1))
    }

    @Test fun anInitialChoiceDoesNotDependOnEnumerationOrder() {
        assertEquals(1, CompanionDisplayPolicy.select(
            listOf(screen(3), screen(0), screen(1)), activityDisplayId = 0))
    }
}

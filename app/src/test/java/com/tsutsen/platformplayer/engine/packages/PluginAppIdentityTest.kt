package com.tsutsen.platformplayer.engine.packages

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginAppIdentityTest {
    @Test fun successorReleaseNumberDoesNotOverstatePluginEngineCapabilities() {
        assertEquals(389, PluginAppIdentity.buildVersion(7001, true, 389))
    }

    @Test fun successorDebugFallbackAlsoUsesTheKnownEngineBaseline() {
        assertEquals(389, PluginAppIdentity.buildVersion(1, true, 389))
    }

    @Test fun futureEngineUpgradeIsIndependentOfReleaseNumber() {
        assertEquals(396, PluginAppIdentity.buildVersion(8000, true, 396))
    }

    @Test fun ordinaryBluejayVersionBehaviorIsPreserved() {
        assertEquals(7000, PluginAppIdentity.buildVersion(7000, false, 389))
        assertEquals(Int.MAX_VALUE, PluginAppIdentity.buildVersion(1, false, 389))
    }

    @Test fun successorUsesStablePluginFlavor() {
        assertEquals("stable", PluginAppIdentity.buildFlavor("thor", true))
    }

    @Test fun ordinaryBluejayFlavorsRemainDistinct() {
        assertEquals("stable", PluginAppIdentity.buildFlavor("stable", false))
        assertEquals("unstable", PluginAppIdentity.buildFlavor("unstable", false))
        assertEquals("playstore", PluginAppIdentity.buildFlavor("playstore", false))
    }
}

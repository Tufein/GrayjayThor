package com.futo.platformplayer.thor

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futo.platformplayer.video.PlayerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class ThorPlayerOutputTest {
    @Test fun orphanedVideoActivityFinishesWithoutInitializingTheGlobalPlayer() {
        // Android may restore the video task before the controls task after process death.
        ActivityScenario.launch(ThorVideoActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test fun movingVideoPreservesPlayerPositionVolumeAndDetailListener() = onMain { manager, local, external ->
        val listener = object : Player.Listener {}
        manager.attach(local, "DetailPlayer")
        manager.setListener(listener)
        manager.setVolume(0.35f)
        manager.player.setMediaItem(MediaItem.fromUri("https://example.com/test.mp4"))
        manager.player.seekTo(7_300)
        val position = manager.player.currentPosition

        manager.setExternalView(external)
        assertNull(local.player)
        assertSame(manager.player, external.player)
        assertSame(listener, manager.currentState.listener)
        assertEquals(position, manager.player.currentPosition)
        assertEquals(0.35f, manager.player.volume, 0.001f)

        manager.clearExternalView(external)
        assertSame(manager.player, local.player)
        assertNull(external.player)
        assertSame(listener, manager.currentState.listener)
        assertEquals(position, manager.player.currentPosition)
    }

    @Test fun removingAnOldOutputCannotDetachTheCurrentOutput() = onMain { manager, local, external ->
        val oldOutput = PlayerView(local.context)
        manager.attach(local, "DetailPlayer")
        manager.setExternalView(oldOutput)
        manager.setExternalView(external)
        manager.clearExternalView(oldOutput)
        assertNull(oldOutput.player)
        assertSame(manager.player, external.player)
        assertNull(local.player)
    }

    @Test fun previewPlayersStayOnTheirLocalDisplay() = onMain { manager, local, external ->
        manager.setExternalView(external)
        manager.attach(local, "PreviewPlayer")
        assertSame(manager.player, local.player)
        assertNull(external.player)
        manager.attach(local, "DetailPlayer")
        assertSame(manager.player, external.player)
        assertNull(local.player)
        manager.detach()
        assertNull(external.player)
        assertNull(local.player)
    }

    private fun onMain(test: (PlayerManager, PlayerView, PlayerView) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val manager = PlayerManager(ExoPlayer.Builder(context).build())
            try {
                test(manager, PlayerView(context), PlayerView(context))
            } finally {
                manager.release()
            }
        }
    }
}

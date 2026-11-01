package com.tsutsen.platformplayer.player

import androidx.lifecycle.viewModelScope
import com.tsutsen.platformplayer.core.data.repository.*
import com.tsutsen.platformplayer.core.datastore.model.AppPreferences
import com.tsutsen.platformplayer.core.datastore.model.ControllerBinding
import com.tsutsen.platformplayer.core.datastore.model.ControllerPreferences
import com.tsutsen.platformplayer.core.model.*
import com.tsutsen.platformplayer.core.ui.GamepadKeyBus.GamepadEvent
import com.tsutsen.platformplayer.feature.player.impl.HistoryTracker
import com.tsutsen.platformplayer.feature.player.impl.PlayerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerInteractionTest {
    private val player = mock(PlayerRepository::class.java)
    private val settings = mock(SettingsRepository::class.java)
    private val chat = mock(LiveChatRepository::class.java)
    private val state = MutableStateFlow(PlayerState())
    private val preferences = MutableStateFlow(AppPreferences(controller = ControllerPreferences(enabled = true)))
    private val comments = PendingComments()
    private val extrasApplied = CompletableDeferred<Unit>()
    private lateinit var model: PlayerViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val library = mock(LibraryRepository::class.java)
        val downloads = mock(DownloadsRepository::class.java)
        val casting = mock(CastingRepository::class.java)
        `when`(player.playerState).thenReturn(state)
        `when`(player.loopMode).thenReturn(MutableStateFlow(0))
        `when`(settings.preferences).thenReturn(preferences)
        `when`(library.playlists).thenReturn(MutableStateFlow(emptyList()))
        `when`(library.observeSavedTypes(anyString())).thenReturn(flowOf(emptySet()))
        `when`(library.observePlaylistsContaining(anyString())).thenReturn(flowOf(emptySet()))
        `when`(downloads.downloads).thenReturn(MutableStateFlow(emptyList()))
        `when`(chat.state).thenReturn(MutableStateFlow(null))
        `when`(casting.state).thenReturn(MutableStateFlow(CastState()))
        doAnswer { extrasApplied.complete(Unit); null }.`when`(player).setVideoExtras(anyList(), anyList())
        model = PlayerViewModel(player, comments, settings, mock(HistoryTracker::class.java), library,
            mock(ChannelRepository::class.java), downloads, mock(PlaybackQueueRepository::class.java), casting, chat)
    }

    @After fun cleanup() {
        if (::model.isInitialized) model.viewModelScope.cancel()
        comments.page.complete(emptyList())
        Dispatchers.resetMain()
    }

    @Test fun remoteSeekWorksBeforeDurationIsKnownWithoutALocalPlayer() = runTest {
        state.value = state.value.copy(isCasting = true, currentPositionMs = 5_000,
            durationMs = -9223372036854775807L)
        model.seekBy(10_000)
        runCurrent()
        verify(player).seekTo(15_000)
    }

    @Test fun seekingASubHalfSecondClipCannotThrow() = runTest {
        state.value = state.value.copy(durationMs = 100)
        model.seekToClamped(80)
        runCurrent()
        verify(player).seekTo(0)
    }

    @Test fun remappedActionDoesNotKeepItsOldDefault() = runTest {
        val action = PlayerControllerActions.ALL.first { it.id == PlayerControllerActions.PLAY_PAUSE }
        preferences.value = preferences.value.copy(controller = ControllerPreferences(enabled = true,
            mappings = mapOf(action.id to ControllerBinding(900))))
        assertFalse(model.handleControllerKey(GamepadEvent(action.defaultKeyCode, null, true)))
        assertTrue(model.handleControllerKey(GamepadEvent(900, null, true)))
        runCurrent()
        verify(player, times(1)).resume()
    }

    @Test fun disablingControllerRestoresATemporarySpeed() = runTest {
        val key = PlayerControllerActions.ALL.first { it.id == PlayerControllerActions.SPEED_UP }.defaultKeyCode
        assertTrue(model.handleControllerKey(GamepadEvent(key, null, true)))
        preferences.value = preferences.value.copy(controller = ControllerPreferences(enabled = false))
        runCurrent()
        verify(player).setPlaybackSpeed(2f)
        verify(player).setPlaybackSpeed(1f)
    }

    @Test fun manuallySelectedSpeedIsNotOverwrittenByALateRelease() = runTest {
        val key = PlayerControllerActions.ALL.first { it.id == PlayerControllerActions.SPEED_UP }.defaultKeyCode
        model.handleControllerKey(GamepadEvent(key, null, true))
        model.setPlaybackSpeed(1.5f)
        model.handleControllerKey(GamepadEvent(key, null, false))
        runCurrent()
        verify(player).setPlaybackSpeed(1.5f)
        verify(player, never()).setPlaybackSpeed(1f)
    }

    @Test fun aLateCommentPageCannotModifyTheNextVideo() = runTest {
        state.value = state.value.copy(currentVideo = video("a"))
        model.loadMoreComments("a")
        comments.awaitStarted()
        state.value = state.value.copy(currentVideo = video("b"))
        comments.page.complete(listOf(comment("old")))
        comments.awaitFinished()
        runCurrent()
        verify(player, after(100).never()).setVideoExtras(anyList(), anyList())
    }

    @Test fun returningToTheSameVideoDoesNotReviveACancelledPage() = runTest {
        state.value = state.value.copy(currentVideo = video("a"))
        model.loadMoreComments("a")
        comments.awaitStarted()
        state.value = state.value.copy(currentVideo = video("b"))
        state.value = state.value.copy(currentVideo = video("a"))
        comments.page.complete(listOf(comment("old")))
        comments.awaitFinished()
        runCurrent()
        verify(player, after(100).never()).setVideoExtras(anyList(), anyList())
    }

    @Test fun duplicateScrollRequestsLoadOnePageAndAppendOnce() = runTest {
        val initial = comment("initial")
        val next = comment("next")
        state.value = state.value.copy(currentVideo = video("a"), comments = listOf(initial))
        model.loadMoreComments("a")
        comments.awaitStarted()
        model.loadMoreComments("a")
        assertEquals(1, comments.calls.get())
        comments.page.complete(listOf(next))
        comments.awaitFinished()
        withContext(Dispatchers.IO) { withTimeout(5_000) { extrasApplied.await() } }
        runCurrent()
        verify(player, times(1)).setVideoExtras(listOf(initial, next), emptyList())
    }

    @Test fun volumeAndSpeedChangesDoNotRestartLiveChat() = runTest {
        state.value = state.value.copy(currentVideo = video("live").copy(contentType = ContentType.LIVE))
        state.value = state.value.copy(volume = 0.5f)
        state.value = state.value.copy(playbackSpeed = 1.5f)
        runCurrent()
        verify(chat, times(1)).start("live")
        state.value = state.value.copy(currentVideo = video("normal"))
        state.value = state.value.copy(volume = 0.8f)
        runCurrent()
        // One initial stop and one transition out of the live video.
        verify(chat, times(2)).stop()
    }

    private fun video(url: String) = ContentItem(url, url, url, null, null, ContentType.VIDEO)
    private fun comment(id: String) = CommentItem(id, "author", null, id, 0L, 0, null)

    private class PendingComments : CommentRepository {
        val calls = AtomicInteger()
        val page = CompletableDeferred<List<CommentItem>>()
        private val started = CompletableDeferred<Unit>()
        private val finished = CompletableDeferred<Unit>()
        override suspend fun getComments(contentUrl: String) = emptyList<CommentItem>()
        override suspend fun loadMoreComments(contentUrl: String): List<CommentItem> {
            calls.incrementAndGet()
            started.complete(Unit)
            return try { page.await() } finally { finished.complete(Unit) }
        }
        suspend fun awaitStarted() = withContext(Dispatchers.IO) { withTimeout(5_000) { started.await() } }
        suspend fun awaitFinished() = withContext(Dispatchers.IO) { withTimeout(5_000) { finished.await() } }
    }
}

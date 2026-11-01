package com.tsutsen.platformplayer.player

import android.content.Context
import com.tsutsen.platformplayer.core.data.repository.*
import com.tsutsen.platformplayer.core.data.repository.impl.PlayerRepositoryImpl
import com.tsutsen.platformplayer.core.datastore.model.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStartupTest {
    private lateinit var player: PlayerRepositoryImpl
    private val started = CompletableDeferred<Unit>()
    private val resolved = CompletableDeferred<Unit>()
    private val extras = CopyOnWriteArrayList<String>()
    private val extrasFinished = CompletableDeferred<Unit>()

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        player = PlayerRepositoryImpl(mock(Context::class.java))
        val settings = mock(SettingsRepository::class.java)
        `when`(settings.preferences).thenReturn(MutableStateFlow(AppPreferences()))
        val comments = mock(CommentRepository::class.java)
        val content = object : ContentExtrasRepository {
            override suspend fun getChapters(url: String) = emptyList<com.tsutsen.platformplayer.core.model.VideoChapter>()
            override suspend fun getRecommendations(url: String): List<com.tsutsen.platformplayer.core.model.Card> {
                extras.add(url)
                extrasFinished.complete(Unit)
                return emptyList()
            }
        }
        player.setExtrasRepositories(comments, content, settings)
        player.setUrlResolver(object : VideoUrlResolver {
            override suspend fun resolve(contentUrl: String, resumePositionMs: Long): ResolutionResult {
                if (contentUrl == "video-a") {
                    started.complete(Unit)
                    resolved.await()
                }
                // Exercise the shared startup pipeline without a local decoder.
                return ResolutionResult(null, null, casted = true)
            }
        })
    }

    @After fun cleanup() {
        resolved.complete(Unit)
        Dispatchers.resetMain()
    }

    @Test fun sourceResolutionPrecedesOptionalPluginRequests() = runTest {
        val playback = launch { player.play("video-a", null) }
        withContext(Dispatchers.IO) { withTimeout(5_000) { started.await() } }
        assertTrue("Extras must not compete with the source resolver", extras.isEmpty())
        resolved.complete(Unit)
        playback.join()
        withContext(Dispatchers.IO) { withTimeout(5_000) { extrasFinished.await() } }
        assertEquals(listOf("video-a"), extras.toList())
    }

    @Test fun supersededResolutionDoesNotStartOptionalRequests() = runTest {
        val oldPlayback = launch { player.play("video-a", null) }
        withContext(Dispatchers.IO) { withTimeout(5_000) { started.await() } }
        player.play("video-b", null)
        withContext(Dispatchers.IO) { withTimeout(5_000) { extrasFinished.await() } }
        resolved.complete(Unit)
        oldPlayback.join()
        assertEquals(listOf("video-b"), extras.toList())
    }
}

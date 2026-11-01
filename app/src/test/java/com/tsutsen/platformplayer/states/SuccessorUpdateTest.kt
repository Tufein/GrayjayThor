package com.tsutsen.platformplayer.states

import android.content.Context
import com.tsutsen.platformplayer.BuildConfig
import com.tsutsen.platformplayer.api.http.ManagedHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class SuccessorUpdateTest {
    @Before fun successorVariant() {
        assumeTrue(BuildConfig.IS_THOR_BUILD)
    }

    @Test fun upstreamVersionAndChangelogAreNeverFetched() {
        val client = mock(ManagedHttpClient::class.java)
        val update = StateUpdate()
        assertNull(update.downloadVersionCode(client))
        assertNull(update.downloadChangelog(client, 9999))
        verifyNoInteractions(client)
    }

    @Test fun cachedUpstreamUpdateCannotBeOfferedForInstallation() {
        val context = mock(Context::class.java)
        val update = StateUpdate()
        update.setUiAvailable(9999)
        update.seedUiFromDisk(context)
        assertEquals(UpdateUiState.NONE, update.uiState)
        verifyNoInteractions(context)
    }

    @Test fun upstreamApkUrlCannotBeUsedByTheDownloadService() {
        assertThrows(IllegalStateException::class.java) { StateUpdate.getApkUrl(9999) }
    }
}

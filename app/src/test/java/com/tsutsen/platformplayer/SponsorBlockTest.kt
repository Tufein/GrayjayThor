package com.tsutsen.platformplayer

import com.tsutsen.platformplayer.core.data.sponsorblock.SponsorBlock
import com.tsutsen.platformplayer.core.data.sponsorblock.SponsorSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockTest {
    @Test fun extractsVideoIds() {
        assertEquals("jNQXAC9IVRw", SponsorBlock.videoId("https://www.youtube.com/watch?v=jNQXAC9IVRw"))
        assertEquals("jNQXAC9IVRw", SponsorBlock.videoId("https://youtu.be/jNQXAC9IVRw?t=3"))
        assertEquals("jNQXAC9IVRw", SponsorBlock.videoId("https://www.youtube.com/shorts/jNQXAC9IVRw"))
        assertNull(SponsorBlock.videoId("https://example.com/video/1"))
    }

    @Test fun hashPrefixIsFourHexChars() {
        // sha256("abc") starts with ba7816bf
        assertEquals("ba78", SponsorBlock.hashPrefix("abc"))
    }

    @Test fun parsesOnlyTheRequestedVideoAndOnlySkipSegments() {
        val json = """[{"videoID":"otherVideo1","hash":"x","segments":[{"category":"sponsor","actionType":"skip","segment":[1,2],"UUID":"a"}]},
            {"videoID":"jNQXAC9IVRw","hash":"y","segments":[
              {"category":"intro","actionType":"skip","segment":[0,12.5],"UUID":"b","videoDuration":19},
              {"category":"sponsor","actionType":"mute","segment":[14,15],"UUID":"c"},
              {"category":"outro","actionType":"skip","segment":[100.2,120],"UUID":"d"}]}]"""
        val seg = SponsorBlock.parse(json, "jNQXAC9IVRw")
        assertEquals(
            listOf(SponsorSegment(0, 12_500, "intro"), SponsorSegment(100_200, 120_000, "outro")),
            seg,
        )
        assertTrue(SponsorBlock.parse(json, "nothingHere1").isEmpty())
    }

    @Test fun skipTargetFindsCurrentSegmentOnce() {
        val a = SponsorSegment(10_000, 20_000, "sponsor")
        assertEquals(a, SponsorBlock.skipTarget(listOf(a), 12_000, emptySet()))
        assertNull(SponsorBlock.skipTarget(listOf(a), 12_000, setOf(a)))
        assertNull(SponsorBlock.skipTarget(listOf(a), 9_999, emptySet()))
        assertNull(SponsorBlock.skipTarget(listOf(a), 19_700, emptySet()))
    }
}

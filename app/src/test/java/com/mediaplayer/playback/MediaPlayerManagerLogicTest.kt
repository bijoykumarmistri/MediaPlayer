package com.mediaplayer.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlayerManagerLogicTest {

    @Test
    fun resolveResumePositionMs_resetsNearStartAndNearEnd() {
        val durationMs = 180_000L

        // Within first 3 seconds -> restart from 0
        assertEquals(0L, MediaPlayerManager.resolveResumePositionMs(0L, durationMs))
        assertEquals(0L, MediaPlayerManager.resolveResumePositionMs(2_500L, durationMs))

        // Valid mid-track position -> preserved
        assertEquals(65_000L, MediaPlayerManager.resolveResumePositionMs(65_000L, durationMs))

        // Within last 5 seconds (e.g., 280ms before EOF) or at/beyond duration -> reset to 0
        assertEquals(0L, MediaPlayerManager.resolveResumePositionMs(175_000L, durationMs))
        assertEquals(0L, MediaPlayerManager.resolveResumePositionMs(179_720L, durationMs))
        assertEquals(0L, MediaPlayerManager.resolveResumePositionMs(180_000L, durationMs))

        // Unknown duration keeps > 3s saved offset
        assertEquals(42_000L, MediaPlayerManager.resolveResumePositionMs(42_000L, 0L))
    }

    @Test
    fun resolveRestoredPlaylistIndices_preservesSameMediaTypeQueue() {
        val candidates = listOf(
            1L to true,   // video
            2L to false,  // audio
            3L to true,   // video
            4L to false,  // audio (target)
            5L to false   // audio
        )

        val (indices, currentIndex) = MediaPlayerManager.resolveRestoredPlaylistIndices(
            targetId = 4L,
            targetIsVideo = false,
            candidateIdsAndTypes = candidates
        )

        assertEquals(listOf(1, 3, 4), indices)
        assertEquals(1, currentIndex)

        val (missingIndices, missingIndex) = MediaPlayerManager.resolveRestoredPlaylistIndices(
            targetId = 99L,
            targetIsVideo = false,
            candidateIdsAndTypes = candidates
        )
        assertTrue(missingIndices.isEmpty())
        assertEquals(0, missingIndex)
    }

    @Test
    fun shouldSeekToStartOnPlay_detectsEndedOrNearEofState() {
        assertTrue(MediaPlayerManager.shouldSeekToStartOnPlay(Player.STATE_ENDED, 0L, 180_000L))
        assertTrue(MediaPlayerManager.shouldSeekToStartOnPlay(Player.STATE_READY, 179_500L, 180_000L))
        assertTrue(MediaPlayerManager.shouldSeekToStartOnPlay(Player.STATE_READY, 180_000L, 180_000L))

        assertFalse(MediaPlayerManager.shouldSeekToStartOnPlay(Player.STATE_READY, 45_000L, 180_000L))
        assertFalse(MediaPlayerManager.shouldSeekToStartOnPlay(Player.STATE_IDLE, 0L, 180_000L))
        assertFalse(MediaPlayerManager.shouldSeekToStartOnPlay(Player.STATE_READY, 0L, 0L))
    }

    @Test
    fun normalizeStreamUrl_preservesSupportedSchemesAndPrependsHttpsToBareHosts() {
        assertEquals("http://example.com/live.mp3", MediaPlayerManager.normalizeStreamUrl("  http://example.com/live.mp3  "))
        assertEquals("https://cdn.example.com/video.mp4", MediaPlayerManager.normalizeStreamUrl("https://cdn.example.com/video.mp4"))
        assertEquals("rtsp://192.168.1.10/live", MediaPlayerManager.normalizeStreamUrl("rtsp://192.168.1.10/live"))
        assertEquals("rtmp://live.example.com/app/stream", MediaPlayerManager.normalizeStreamUrl("rtmp://live.example.com/app/stream"))
        assertEquals("mms://stream.example.com/feed", MediaPlayerManager.normalizeStreamUrl("mms://stream.example.com/feed"))
        assertEquals("RTMP://LIVE.EXAMPLE.COM/STREAM", MediaPlayerManager.normalizeStreamUrl("RTMP://LIVE.EXAMPLE.COM/STREAM"))
        assertEquals("https://stream.example.com/radio.aac", MediaPlayerManager.normalizeStreamUrl("stream.example.com/radio.aac"))
        assertEquals("", MediaPlayerManager.normalizeStreamUrl("   "))
    }
}

package com.mediaplayer.video

import com.bkm.player.mpv.DecoderMode
import com.bkm.player.mpv.MediaChapter
import com.bkm.player.mpv.MediaInfo
import com.bkm.player.mpv.NativeMpvEngine
import com.bkm.player.mpv.PlayerUiState
import com.bkm.player.mpv.ResizeMode
import com.bkm.player.mpv.OrientationMode
import com.bkm.player.mpv.PlayerPlaylistItem
import com.bkm.player.mpv.RepeatMode
import com.mediaplayer.ui.video.cleanMovieDisplayTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlayerFeaturesTest {

    @Test
    fun orientationModeCyclesThroughAllModes() {
        assertEquals(OrientationMode.LANDSCAPE, OrientationMode.SENSOR.next())
        assertEquals(OrientationMode.PORTRAIT, OrientationMode.LANDSCAPE.next())
        assertEquals(OrientationMode.SENSOR, OrientationMode.PORTRAIT.next())
    }

    @Test
    fun repeatModeCyclesThroughAllModes() {
        assertEquals(RepeatMode.ONE, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ALL, RepeatMode.ONE.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ALL.next())
    }

    @Test
    fun decoderModeCyclesAndMapsHwdecValues() {
        assertEquals("mediacodec", DecoderMode.HW_PLUS.hwdecValue)
        assertEquals("mediacodec-copy", DecoderMode.HW.hwdecValue)
        assertEquals("no", DecoderMode.SW.hwdecValue)
        assertEquals(DecoderMode.HW, DecoderMode.HW_PLUS.next())
        assertEquals(DecoderMode.SW, DecoderMode.HW.next())
        assertEquals(DecoderMode.HW_PLUS, DecoderMode.SW.next())
    }

    @Test
    fun resizeModeCyclesWrapAround() {
        var current = ResizeMode.FIT
        repeat(ResizeMode.entries.size) {
            current = current.next()
        }
        assertEquals(ResizeMode.FIT, current)
    }

    @Test
    fun playerUiStateDefaultsAndScrubOverlayFormatting() {
        val state = PlayerUiState(
            durationSeconds = 4000.0,
            scrubPreviewSeconds = 3665.0,
            scrubDeltaSeconds = -15.0
        )
        assertFalse(state.isMuted)
        assertFalse(state.isScrubbing)
        assertFalse(state.isGestureScrubbing)
        assertFalse(state.hasPlaybackFailed)
        assertEquals(0, state.videoBrightness)
        assertEquals(0, state.videoContrast)
        assertEquals(0, state.videoSaturation)
        assertEquals(0, state.videoGamma)
        assertTrue(state.chapters.isEmpty())
        assertEquals(-1, state.currentChapterIndex)
        assertEquals("01:01:05 [-00:15]", state.scrubOverlayText)
    }

    @Test
    fun mediaModelsHoldExpectedMetadata() {
        val chapter = MediaChapter(index = 2, title = "Climax", timeSeconds = 1845.5)
        val playlistItem = PlayerPlaylistItem(
            id = 99L,
            title = "Sample Movie.mkv",
            durationMs = 5400_000L,
            resolution = "1920x1080"
        )
        val info = MediaInfo()

        assertEquals(2, chapter.index)
        assertEquals("Climax", chapter.title)
        assertEquals(99L, playlistItem.id)
        assertEquals("1920x1080", playlistItem.resolution)
        assertEquals("", info.demuxerCache)
    }

    @Test
    fun cleanMovieDisplayTitleStripsReleaseTagsAndPreservesYear() {
        val (title, year) = cleanMovieDisplayTitle("Interstellar.2014.1080p.BluRay.x265.HEVC.mkv")
        assertEquals("Interstellar", title)
        assertEquals("2014", year)
    }

    @Test
    fun isLikelyWebPageUrl_detectsWebPagesAndAllowsDirectStreams() {
        assertTrue(NativeMpvEngine.isLikelyWebPageUrl("https://youtu.be/PvG8d5cdB_I"))
        assertTrue(NativeMpvEngine.isLikelyWebPageUrl("https://www.youtube.com/watch?v=PvG8d5cdB_I"))
        assertTrue(NativeMpvEngine.isLikelyWebPageUrl("https://vimeo.com/123456"))

        assertFalse(NativeMpvEngine.isLikelyWebPageUrl("https://example.com/live/stream.m3u8"))
        assertFalse(NativeMpvEngine.isLikelyWebPageUrl("https://cdn.example.com/movie.mp4?token=abc"))
        assertFalse(NativeMpvEngine.isLikelyWebPageUrl("rtsp://192.168.1.10/live"))
        assertFalse(NativeMpvEngine.isLikelyWebPageUrl("  "))
        assertFalse(NativeMpvEngine.isLikelyWebPageUrl(null))
    }

    @Test
    fun buildFriendlyPlaybackError_mapsCommonNetworkAndFormatErrors() {
        val youtubeError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "https://youtu.be/PvG8d5cdB_I",
            detailError = null,
            playerError = "Failed to recognize file format."
        )
        assertTrue(youtubeError.contains("web page link", ignoreCase = true))
        assertTrue(youtubeError.contains(".m3u8", ignoreCase = true))

        val genericNetworkFormatError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "https://example.com/watch/123",
            detailError = "Failed to recognize file format.",
            playerError = null
        )
        assertTrue(genericNetworkFormatError.contains("did not return a recognized media stream", ignoreCase = true))

        val localFormatError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "fd://12",
            detailError = null,
            playerError = "Failed to recognize file format."
        )
        assertEquals("This file format is unsupported or the file is corrupted.", localFormatError)

        val notFoundError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "https://example.com/video.mp4",
            detailError = "HTTP error 404 Not Found",
            playerError = null
        )
        assertTrue(notFoundError.contains("404"))

        val forbiddenError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "https://example.com/video.mp4",
            detailError = "HTTP error 403 Forbidden",
            playerError = null
        )
        assertTrue(forbiddenError.contains("401/403"))

        val timeoutError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "https://example.com/video.mp4",
            detailError = "Connection timed out",
            playerError = null
        )
        assertTrue(timeoutError.contains("timed out", ignoreCase = true))

        val fallbackWebError = NativeMpvEngine.buildFriendlyPlaybackError(
            sourcePath = "https://youtu.be/PvG8d5cdB_I",
            detailError = null,
            playerError = null
        )
        assertTrue(fallbackWebError.contains("web page link", ignoreCase = true))
    }
}

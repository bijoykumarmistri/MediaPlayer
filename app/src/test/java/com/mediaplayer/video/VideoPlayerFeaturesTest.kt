package com.mediaplayer.video

import com.bkm.player.mpv.DecoderMode
import com.bkm.player.mpv.MediaChapter
import com.bkm.player.mpv.MediaInfo
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
}

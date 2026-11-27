package com.mediaplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticLogManagerTest {

    @Test
    fun sanitizeLine_redactsSensitiveQueryParameters() {
        val raw = "Opening https://cdn.example.com/stream.m3u8?token=secret123&quality=1080p&api_key=abc"
        val sanitized = DiagnosticLogManager.sanitizeLine(raw)

        assertFalse(sanitized.contains("secret123"))
        assertFalse(sanitized.contains("=abc"))
        assertTrue(sanitized.contains("token=<REDACTED>"))
        assertTrue(sanitized.contains("api_key=<REDACTED>"))
        assertTrue(sanitized.contains("quality=1080p"))
    }

    @Test
    fun sanitizeLine_masksPersonalStoragePathsPreservingExtension() {
        val raw = "Failed opening /storage/emulated/0/Movies/Private/MyPersonalVideo.mkv with hwdec"
        val sanitized = DiagnosticLogManager.sanitizeLine(raw)

        assertFalse(sanitized.contains("MyPersonalVideo"))
        assertFalse(sanitized.contains("/storage/emulated/0"))
        assertEquals("Failed opening <media_path>.mkv with hwdec", sanitized)
    }

    @Test
    fun sanitizeLine_leavesNonSensitiveLogcatLinesIntact() {
        val raw = "ExoPlayer error: ERROR_CODE_DECODING_FAILED (4003)"
        assertEquals(raw, DiagnosticLogManager.sanitizeLine(raw))
    }

    @Test
    fun mpvDiagnosticLogger_forwardsToSink() {
        val captured = mutableListOf<Pair<String, String>>()
        val previous = com.bkm.player.mpv.MpvDiagnosticLogger.sink
        try {
            com.bkm.player.mpv.MpvDiagnosticLogger.sink = { tag, msg ->
                captured.add(tag to msg)
            }
            com.bkm.player.mpv.MpvDiagnosticLogger.log("MPV", "file-loaded hwdec=mediacodec")
            assertEquals(1, captured.size)
            assertEquals("MPV", captured[0].first)
            assertEquals("file-loaded hwdec=mediacodec", captured[0].second)
        } finally {
            com.bkm.player.mpv.MpvDiagnosticLogger.sink = previous
        }
    }
}

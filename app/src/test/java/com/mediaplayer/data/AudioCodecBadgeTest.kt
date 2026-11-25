package com.mediaplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioCodecBadgeTest {

    @Test
    fun formatAudioCodecBadge_prioritizesFileExtension() {
        assertEquals("Opus", MediaLibraryRepository.formatAudioCodecBadge("audio/ogg", "/storage/emulated/0/Music/song.opus"))
        assertEquals("AAC / M4A", MediaLibraryRepository.formatAudioCodecBadge("audio/mp4", "track.m4a"))
        assertEquals("FLAC Hi-Res", MediaLibraryRepository.formatAudioCodecBadge("audio/*", "master.FLAC"))
        assertEquals("MP3", MediaLibraryRepository.formatAudioCodecBadge("audio/mpeg", "podcast.mp3"))
        assertEquals("WAV PCM", MediaLibraryRepository.formatAudioCodecBadge("audio/x-wav", "sample.wav"))
    }

    @Test
    fun formatAudioCodecBadge_fallsBackToMimeTypeWithoutMisleadingPcm() {
        assertEquals("MP3", MediaLibraryRepository.formatAudioCodecBadge("audio/mpeg"))
        assertEquals("AAC", MediaLibraryRepository.formatAudioCodecBadge("audio/mp4"))
        assertEquals("AAC", MediaLibraryRepository.formatAudioCodecBadge("audio/mp4a-latm"))
        assertEquals("OGG", MediaLibraryRepository.formatAudioCodecBadge("audio/ogg"))
        assertEquals("Audio", MediaLibraryRepository.formatAudioCodecBadge("audio/*"))
        assertEquals("Audio", MediaLibraryRepository.formatAudioCodecBadge(""))
    }
}

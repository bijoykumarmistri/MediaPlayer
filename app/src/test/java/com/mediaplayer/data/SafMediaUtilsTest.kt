package com.mediaplayer.data

import com.bkm.player.mpv.NativeMpvEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafMediaUtilsTest {

    @Test
    fun classifyMediaKind_handlesGenericMimeByExtension() {
        assertEquals(
            MediaLibraryRepository.MediaKind.VIDEO,
            MediaLibraryRepository.classifyMediaKind("application/octet-stream", "Panchayat.S04E06.mkv")
        )
        assertEquals(
            MediaLibraryRepository.MediaKind.VIDEO,
            MediaLibraryRepository.classifyMediaKind("", "clip.TS")
        )
        assertEquals(
            MediaLibraryRepository.MediaKind.AUDIO,
            MediaLibraryRepository.classifyMediaKind("application/octet-stream", "track.flac")
        )
        assertEquals(
            MediaLibraryRepository.MediaKind.AUDIO,
            MediaLibraryRepository.classifyMediaKind("audio/mpeg", "unknown_no_ext")
        )
        assertNull(
            MediaLibraryRepository.classifyMediaKind("text/plain", "notes.txt")
        )
    }

    @Test
    fun extractFolderNameFromTreeDocId_returnsLeafFolderName() {
        assertEquals(
            "Movies and Series",
            MediaLibraryRepository.extractFolderNameFromTreeDocId("primary:Movies and Series")
        )
        assertEquals(
            "Season 4",
            MediaLibraryRepository.extractFolderNameFromTreeDocId("primary:Movies and Series/Season 4/")
        )
        assertEquals(
            "Imported Folder",
            MediaLibraryRepository.extractFolderNameFromTreeDocId("primary:")
        )
        assertEquals(
            "Imported Folder",
            MediaLibraryRepository.extractFolderNameFromTreeDocId(null)
        )
    }

    @Test
    fun stableSafItemId_isDeterministicAndPositiveWithSafBit() {
        val id1 = MediaLibraryRepository.stableSafItemId("content://com.android.externalstorage.documents/tree/primary%3AMovies/document/ep1.mp4")
        val id2 = MediaLibraryRepository.stableSafItemId("content://com.android.externalstorage.documents/tree/primary%3AMovies/document/ep1.mp4")
        val id3 = MediaLibraryRepository.stableSafItemId("content://com.android.externalstorage.documents/tree/primary%3AMovies/document/ep2.mp4")

        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
        assertTrue(id1 >= 0x4000_0000_0000_0000L)
    }

    @Test
    fun extractPrimaryRelativePath_parsesExternalStorageDocumentIdsAndSegments() {
        val fromDocId = NativeMpvEngine.extractPrimaryRelativePath(
            authority = "com.android.externalstorage.documents",
            documentId = "primary:Movies and Series/Panchayat.S04E06.Sanjeevani.720p.AMZN.WEB-DL.DDP5.1.x264-.mkv.mp4"
        )
        assertEquals(
            "Movies and Series/Panchayat.S04E06.Sanjeevani.720p.AMZN.WEB-DL.DDP5.1.x264-.mkv.mp4",
            fromDocId
        )

        val fromSegments = NativeMpvEngine.extractPrimaryRelativePath(
            authority = "com.android.externalstorage.documents",
            documentId = null,
            pathSegments = listOf(
                "tree",
                "primary:Movies and Series",
                "document",
                "primary:Movies and Series/Episode1.mp4"
            )
        )
        assertEquals("Movies and Series/Episode1.mp4", fromSegments)

        assertNull(
            NativeMpvEngine.extractPrimaryRelativePath(
                authority = "com.android.externalstorage.documents",
                documentId = "primary:../secret.mp4"
            )
        )
        assertNull(
            NativeMpvEngine.extractPrimaryRelativePath(
                authority = "com.android.providers.downloads.documents",
                documentId = "primary:Download/video.mp4"
            )
        )
    }
}

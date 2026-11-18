package com.mediaplayer.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultMediaModelTest {

    @Test
    fun testVaultItemProperties() {
        val vaultItem = VaultMediaItem(
            id = "test-uuid-1234",
            title = "SecretVideo.mp4",
            originalUri = "content://media/external/video/media/42",
            vaultFilePath = "/data/user/0/com.mediaplayer/files/.secure_vault/test-uuid-1234.mp4",
            isVideo = true,
            mimeType = "video/mp4",
            durationMs = 120000L,
            sizeBytes = 15000000L,
            dateLocked = 1700000000000L,
            resolution = "1920x1080",
            originalFolderName = "Camera",
            artist = "Unknown Artist",
            album = "Privacy Vault"
        )

        assertEquals("test-uuid-1234", vaultItem.id)
        assertEquals("SecretVideo.mp4", vaultItem.title)
        assertTrue(vaultItem.isVideo)
        assertEquals("video/mp4", vaultItem.mimeType)
        assertEquals(120000L, vaultItem.durationMs)
        assertEquals(15000000L, vaultItem.sizeBytes)
        assertEquals("Privacy Vault", vaultItem.album)
    }
}

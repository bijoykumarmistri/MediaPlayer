package com.mediaplayer.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // Remote is newer
        assertTrue(AppUpdateManager.isNewerVersion("v1.2", "1.1"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.2.0", "1.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.1.1", "1.1"))
        assertTrue(AppUpdateManager.isNewerVersion("v2.0", "1.9.9"))

        // Same version
        assertFalse(AppUpdateManager.isNewerVersion("v1.1", "1.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.1", "1.1"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.1.0", "1.1"))

        // Remote is older
        assertFalse(AppUpdateManager.isNewerVersion("v1.0", "1.1"))
        assertFalse(AppUpdateManager.isNewerVersion("v0.9.9", "1.1"))
    }
}

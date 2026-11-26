package com.mediaplayer.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LazyKeyDeduplicationTest {

    @Test
    fun deduplicateLazyKeys_preservesUniqueKeysAndDisambiguatesDuplicates() {
        val items = listOf(634482607L, 123L, 634482607L, 634482607L, 456L)
        val keys = deduplicateLazyKeys(items) { it }

        assertEquals(items.size, keys.size)
        assertEquals(items.size, keys.toSet().size)
        assertEquals(634482607L, keys[0])
        assertEquals(123L, keys[1])
        assertEquals("634482607_dup_1", keys[2])
        assertEquals("634482607_dup_2", keys[3])
        assertEquals(456L, keys[4])
    }

    @Test
    fun deduplicateLazyKeys_handlesPreExistingDupStringCollision() {
        val items = listOf("a", "a_dup_1", "a")
        val keys = deduplicateLazyKeys(items) { it }

        assertEquals(listOf("a", "a_dup_1", "a_dup_2"), keys)
        assertEquals(3, keys.toSet().size)
    }
}

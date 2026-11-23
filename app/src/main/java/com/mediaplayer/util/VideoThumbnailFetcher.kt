package com.mediaplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private val emptyThumbnailDrawable = Color.TRANSPARENT.toDrawable()

/**
 * High-performance video thumbnail fetcher that leverages Android OS MediaStore
 * system thumbnail caching for instant (<5ms) frame loading, with fallback to
 * low-latency keyframe extraction, paired with an in-memory LruCache and negative cache.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    companion object {
        private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() // KB
        private val cacheSize = (maxMemory / 24).coerceIn(8192, 49152) // 8MB - 48MB in KB

        // In-memory bitmap cache sized by actual byte count to prevent OutOfMemoryError
        val memoryCache = object : LruCache<Uri, Bitmap>(cacheSize) {
            override fun sizeOf(key: Uri, value: Bitmap): Int {
                return (value.byteCount / 1024).coerceAtLeast(1)
            }
        }

        // Bounded negative cache (max 500 items) to prevent unbounded memory growth
        private val noThumbnailLru = LruCache<Uri, Boolean>(500)

        val noThumbnailCache = object : MutableSet<Uri> {
            override val size: Int get() = noThumbnailLru.size()
            override fun clear() = noThumbnailLru.evictAll()
            override fun isEmpty(): Boolean = noThumbnailLru.size() == 0
            override fun iterator(): MutableIterator<Uri> = noThumbnailLru.snapshot().keys.toMutableList().iterator()
            override fun contains(element: Uri): Boolean = noThumbnailLru[element] == true
            override fun containsAll(elements: Collection<Uri>): Boolean = elements.all { contains(it) }
            override fun add(element: Uri): Boolean {
                noThumbnailLru.put(element, true)
                return true
            }
            override fun addAll(elements: Collection<Uri>): Boolean {
                elements.forEach { add(it) }
                return true
            }
            override fun remove(element: Uri): Boolean {
                return noThumbnailLru.remove(element) != null
            }
            override fun removeAll(elements: Collection<Uri>): Boolean = elements.any { remove(it) }
            override fun retainAll(elements: Collection<Uri>): Boolean {
                val toRemove = noThumbnailLru.snapshot().keys - elements.toSet()
                toRemove.forEach { noThumbnailLru.remove(it) }
                return toRemove.isNotEmpty()
            }
        }

        fun clearMemoryCache() {
            memoryCache.evictAll()
            noThumbnailLru.evictAll()
        }

        // Concurrency limiter to prevent CPU starvation and frame skipping during video grid scrolling
        private val extractionSemaphore = kotlinx.coroutines.sync.Semaphore(2)
    }

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        // 0. Negative cache check (instant O(1) exit - prevents Coil fallthrough)
        if (noThumbnailCache.contains(uri)) {
            return@withContext DrawableResult(
                drawable = emptyThumbnailDrawable,
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        // 1. In-memory fast cache check
        val cachedBitmap = memoryCache.get(uri)
        if ((cachedBitmap != null) && (!cachedBitmap.isRecycled)) {
            return@withContext DrawableResult(
                drawable = cachedBitmap.toDrawable(context.resources),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        val targetW = (options.size.width as? Dimension.Pixels)?.px?.coerceIn(120, 1080) ?: 360
        val targetH = (options.size.height as? Dimension.Pixels)?.px?.coerceIn(90, 720) ?: 200

        // Fast-path MediaStore system thumbnail query on API 29+ (<2ms)
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && (uri.authority == MediaStore.AUTHORITY)) {
            try {
                val thumb = context.contentResolver.loadThumbnail(uri, Size(targetW, targetH), null)
                memoryCache.put(uri, thumb)
                return@withContext DrawableResult(
                    drawable = thumb.toDrawable(context.resources),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } catch (_: Throwable) {
                // MediaStore system thumbnail not available, proceed to fallback extractor below
            }
        }

        extractionSemaphore.withPermit {
            // Re-check caches after acquiring permit
            if (noThumbnailCache.contains(uri)) {
                return@withPermit DrawableResult(
                    drawable = emptyThumbnailDrawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                )
            }
            val cachedAfterWait = memoryCache.get(uri)
            if ((cachedAfterWait != null) && (!cachedAfterWait.isRecycled)) {
                return@withPermit DrawableResult(
                    drawable = cachedAfterWait.toDrawable(context.resources),
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                )
            }

            val targetW = (options.size.width as? Dimension.Pixels)?.px?.coerceIn(120, 1080) ?: 360
            val targetH = (options.size.height as? Dimension.Pixels)?.px?.coerceIn(90, 720) ?: 200

            var bitmap: Bitmap? = null

            // 2. High-speed keyframe extraction via MediaMetadataRetriever (hardware-sync frame scaled to target size)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC, targetW, targetH)
                        ?: retriever.getScaledFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetW, targetH)
                        ?: retriever.getFrameAtTime(-1L)
                } else {
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC)
                        ?: retriever.getFrameAtTime(-1L)
                }
            } catch (_: Throwable) {
                // Negative cache handles missing/unreadable videos
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            }

            if (bitmap != null) {
                memoryCache.put(uri, bitmap)
                DrawableResult(
                    drawable = bitmap.toDrawable(context.resources),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } else {
                // Mark as having no thumbnail and return empty transparent drawable to prevent Coil fallback to ContentUriFetcher
                noThumbnailCache.add(uri)
                DrawableResult(
                    drawable = emptyThumbnailDrawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                )
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val uriString = data.toString()
            val isVideo = uriString.contains("video", ignoreCase = true) ||
                data.path?.let { p ->
                    p.endsWith(".mp4", ignoreCase = true) || p.endsWith(".mkv", ignoreCase = true) || p.endsWith(".webm", ignoreCase = true) ||
                    p.endsWith(".avi", ignoreCase = true) || p.endsWith(".mov", ignoreCase = true) || p.endsWith(".ts", ignoreCase = true) ||
                    p.endsWith(".m4v", ignoreCase = true) || p.endsWith(".3gp", ignoreCase = true) || p.endsWith(".flv", ignoreCase = true)
                } == true

            if (!isVideo) return null
            return VideoThumbnailFetcher(context, data, options)
        }
    }
}

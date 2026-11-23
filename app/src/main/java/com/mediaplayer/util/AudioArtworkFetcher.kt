package com.mediaplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
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
import android.graphics.Color
import android.provider.MediaStore
import androidx.core.graphics.drawable.toDrawable

private val emptyArtworkDrawable = Color.TRANSPARENT.toDrawable()

/**
 * High-performance audio album artwork fetcher.
 * 1. O(1) in-memory bitmap cache and negative hit cache (tracks without art skip immediately).
 * 2. Returns empty transparent drawable for tracks without art to prevent Coil from falling through to ContentUriFetcher.
 * 3. Downsampled embedded picture fallback with inSampleSize to eliminate memory overhead.
 */
class AudioArtworkFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    companion object {
        private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() // KB
        private val cacheSize = (maxMemory / 32).coerceIn(4096, 32768) // 4MB - 32MB in KB

        // In-memory bitmap cache sized by actual byte count to prevent OutOfMemoryError
        val memoryCache = object : LruCache<Uri, Bitmap>(cacheSize) {
            override fun sizeOf(key: Uri, value: Bitmap): Int {
                return (value.byteCount / 1024).coerceAtLeast(1)
            }
        }

        // Bounded negative cache (max 500 items) to prevent unbounded memory growth
        private val noArtworkLru = LruCache<Uri, Boolean>(500)

        val noArtworkCache = object : MutableSet<Uri> {
            override val size: Int get() = noArtworkLru.size()
            override fun clear() = noArtworkLru.evictAll()
            override fun isEmpty(): Boolean = noArtworkLru.size() == 0
            override fun iterator(): MutableIterator<Uri> = noArtworkLru.snapshot().keys.toMutableList().iterator()
            override fun contains(element: Uri): Boolean = noArtworkLru[element] == true
            override fun containsAll(elements: Collection<Uri>): Boolean = elements.all { contains(it) }
            override fun add(element: Uri): Boolean {
                noArtworkLru.put(element, true)
                return true
            }
            override fun addAll(elements: Collection<Uri>): Boolean {
                elements.forEach { add(it) }
                return true
            }
            override fun remove(element: Uri): Boolean {
                return noArtworkLru.remove(element) != null
            }
            override fun removeAll(elements: Collection<Uri>): Boolean = elements.any { remove(it) }
            override fun retainAll(elements: Collection<Uri>): Boolean {
                val toRemove = noArtworkLru.snapshot().keys - elements.toSet()
                toRemove.forEach { noArtworkLru.remove(it) }
                return toRemove.isNotEmpty()
            }
        }

        fun clearMemoryCache() {
            memoryCache.evictAll()
            noArtworkLru.evictAll()
        }

        // Concurrency limiter to prevent CPU starvation and frame skipping during list scrolling
        private val extractionSemaphore = kotlinx.coroutines.sync.Semaphore(2)
    }

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        // 1. Check negative cache first (O(1) instant exit - prevents Coil fallthrough)
        if (noArtworkCache.contains(uri)) {
            return@withContext DrawableResult(
                drawable = emptyArtworkDrawable,
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        // 2. Check in-memory bitmap cache
        val cachedBitmap = memoryCache.get(uri)
        if ((cachedBitmap != null) && (!cachedBitmap.isRecycled)) {
            return@withContext DrawableResult(
                drawable = cachedBitmap.toDrawable(context.resources),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        val targetW = (options.size.width as? Dimension.Pixels)?.px?.coerceIn(96, 512) ?: 144
        val targetH = (options.size.height as? Dimension.Pixels)?.px?.coerceIn(96, 512) ?: 144

        // 3. Ultra-fast MediaStore thumbnail query on API 29+ (sub-millisecond check via system MediaProvider)
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
                // MediaStore confirmed this audio track has no album artwork thumbnail.
                // Instantly mark in negative cache and return empty transparent drawable in <1ms!
                noArtworkCache.add(uri)
                return@withContext DrawableResult(
                    drawable = emptyArtworkDrawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                )
            }
        }

        // 4. For non-MediaStore URIs (e.g. SAF folders / OTG drives), fallback to throttled retriever
        extractionSemaphore.withPermit {
            // Re-check caches after acquiring permit
            if (noArtworkCache.contains(uri)) {
                return@withPermit DrawableResult(
                    drawable = emptyArtworkDrawable,
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

            var bitmap: Bitmap? = null

            // 3. Low-latency embedded picture extraction via MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val rawBytes = retriever.embeddedPicture
                if ((rawBytes != null) && rawBytes.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
                    var sampleSize = 1
                    while (opts.outWidth / (sampleSize * 2) >= targetW &&
                        opts.outHeight / (sampleSize * 2) >= targetH) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                }
            } catch (_: Throwable) {
                // Ignore
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
                // Mark as having no artwork and return empty transparent drawable to prevent Coil fallback to ContentUriFetcher
                noArtworkCache.add(uri)
                DrawableResult(
                    drawable = emptyArtworkDrawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                )
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val uriStr = data.toString()
            val isAudio = uriStr.contains("audio", ignoreCase = true) ||
                data.path?.let { p ->
                    p.endsWith(".mp3", ignoreCase = true) || p.endsWith(".flac", ignoreCase = true) || p.endsWith(".m4a", ignoreCase = true) ||
                    p.endsWith(".wav", ignoreCase = true) || p.endsWith(".ogg", ignoreCase = true) || p.endsWith(".opus", ignoreCase = true) ||
                    p.endsWith(".aac", ignoreCase = true) || p.endsWith(".dsd", ignoreCase = true) || p.endsWith(".dff", ignoreCase = true) ||
                    p.endsWith(".dsf", ignoreCase = true)
                } == true ||
                (data.scheme == "content" && runCatching { context.contentResolver.getType(data)?.startsWith("audio") }.getOrNull() == true)

            if (!isAudio) return null
            return AudioArtworkFetcher(context, data, options)
        }
    }
}

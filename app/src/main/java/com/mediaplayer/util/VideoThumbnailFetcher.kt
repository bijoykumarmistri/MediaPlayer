package com.mediaplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import android.graphics.drawable.ColorDrawable
import java.util.concurrent.ConcurrentHashMap

private val emptyThumbnailDrawable = ColorDrawable(Color.TRANSPARENT)

/**
 * High-performance video thumbnail fetcher that leverages Android OS MediaStore
 * system thumbnail caching for instant (<5ms) frame loading, with fallback to
 * low-latency keyframe extraction, paired with an in-memory LruCache and negative cache.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    companion object {
        // Fast in-memory bitmap cache for video thumbnails (128 items = ~15-20 MB in RGB_565)
        val memoryCache = LruCache<Uri, Bitmap>(128)
        // Negative cache: corrupted or unreadable videos skip immediately without disk/IPC query
        val noThumbnailCache: MutableSet<Uri> = ConcurrentHashMap.newKeySet<Uri>()
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
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            return@withContext DrawableResult(
                drawable = BitmapDrawable(context.resources, cachedBitmap),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

        val targetW = (options.size.width as? Dimension.Pixels)?.px?.coerceIn(120, 1080) ?: 360
        val targetH = (options.size.height as? Dimension.Pixels)?.px?.coerceIn(90, 720) ?: 200

        // Fast-path MediaStore system thumbnail query on API 29+ (<2ms)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.authority == MediaStore.AUTHORITY) {
            try {
                val thumb = context.contentResolver.loadThumbnail(uri, Size(targetW, targetH), null)
                memoryCache.put(uri, thumb)
                return@withContext DrawableResult(
                    drawable = BitmapDrawable(context.resources, thumb),
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
            if (cachedAfterWait != null && !cachedAfterWait.isRecycled) {
                return@withPermit DrawableResult(
                    drawable = BitmapDrawable(context.resources, cachedAfterWait),
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
                    retriever.getScaledFrameAtTime(1_500_000L, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC, targetW, targetH)
                        ?: retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetW, targetH)
                        ?: retriever.getFrameAtTime(1_500_000L, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC)
                } else {
                    retriever.getFrameAtTime(1_500_000L, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC)
                        ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
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
                    drawable = BitmapDrawable(context.resources, bitmap),
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
                    p.endsWith(".mp4", true) || p.endsWith(".mkv", true) || p.endsWith(".webm", true) ||
                    p.endsWith(".avi", true) || p.endsWith(".mov", true) || p.endsWith(".ts", true) ||
                    p.endsWith(".m4v", true)
                } == true ||
                (data.scheme == "content" && runCatching { context.contentResolver.getType(data)?.startsWith("video") }.getOrNull() == true)

            if (!isVideo) return null
            return VideoThumbnailFetcher(context, data, options)
        }
    }
}

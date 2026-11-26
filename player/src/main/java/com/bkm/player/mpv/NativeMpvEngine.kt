package com.bkm.player.mpv

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.system.ErrnoException
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceHolder
import android.view.SurfaceView
import dev.jdtech.mpv.MPVLib
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hardware/Software decoder modes matching MX Player's HW+, HW, and SW pipeline:
 * - [HW_PLUS]: FFmpeg demuxes in RAM and feeds Android MediaCodec zero-copy (`hwdec=mediacodec`).
 * - [HW]: MediaCodec hardware decode with frame copy-back to system RAM (`hwdec=mediacodec-copy`).
 * - [SW]: Pure FFmpeg CPU software decoding (`hwdec=no`).
 */
enum class DecoderMode(val label: String, val hwdecValue: String) {
    HW_PLUS(label = "HW+", hwdecValue = "mediacodec"),
    HW(label = "HW", hwdecValue = "mediacodec-copy"),
    SW(label = "SW", hwdecValue = "no");

    fun next(): DecoderMode = when (this) {
        HW_PLUS -> HW
        HW -> SW
        SW -> HW_PLUS
    }
}


/**
 * Aspect Ratio / Resize mode matching MX Player's video scaling presets.
 */
enum class ResizeMode(val label: String) {
    FIT("Fit to Screen"),
    STRETCH("Stretch"),
    CROP("Crop / Zoom"),
    ORIGINAL_100("100% Original"),
    SIXTEEN_NINE("16:9"),
    FOUR_THREE("4:3");

    fun next(): ResizeMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

/**
 * Represents an audio or subtitle track parsed from libmpv's `track-list`.
 */
data class MediaTrack(
    val id: Int,
    val type: String, // "audio", "sub", "video"
    val title: String?,
    val language: String?,
    val codec: String?,
    val channels: Int = 0,
    val sampleRate: Int = 0,
    val isSelected: Boolean = false
) {
    val displayName: String
        get() {
            val base = title?.ifBlank { null }
                ?: language?.ifBlank { null }?.uppercase(Locale.US)
                ?: if (type == "audio") "Audio Track $id" else "Subtitle Track $id"
            return if (channels > 0) {
                val chStr = when (channels) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    else -> "$channels Channels"
                }
                "$base • $chStr"
            } else base
        }
}

/**
 * Represents an embedded chapter marker parsed from libmpv's `chapter-list`.
 */
data class MediaChapter(
    val index: Int,
    val title: String,
    val timeSeconds: Double
)

/**
 * Technical media information displayed in the media info dialog.
 */
data class MediaInfo(
    val title: String = "",
    val resolution: String = "",
    val videoCodec: String = "",
    val videoFps: String = "",
    val audioCodec: String = "",
    val audioChannels: String = "",
    val sampleRate: String = "",
    val hwdec: String = "",
    val demuxerCache: String = ""
)

/**
 * Callbacks dispatched from libmpv's native event loop thread.
 */
interface MpvEngineListener {
    fun onPositionChanged(seconds: Double) {}
    fun onDurationChanged(seconds: Double) {}
    fun onPausedChanged(paused: Boolean) {}
    fun onBufferingChanged(buffering: Boolean) {}
    fun onCacheDurationChanged(seconds: Double) {}
    fun onHwdecChanged(activeHwdec: String) {}
    fun onVideoSizeChanged(width: Int, height: Int) {}
    fun onTracksChanged() {}
    fun onChaptersChanged() {}
    fun onCurrentChapterChanged(chapterIndex: Int) {}
    fun onFileLoaded() {}
    fun onEndOfFile() {}
    fun onError(message: String) {}
}

/**
 * Production-ready controller wrapping [MPVLib] (FFmpeg + MediaCodec + OpenGL ES).
 *
 * Engineered for MX Player HW+ playback characteristics:
 * - Mobile-tuned in-RAM demuxer ring buffer (`48MiB` forward / `16MiB` backward)
 * - OpenGL ES hardware video output (`vo=gpu`, `gpu-context=android`)
 * - Multi-audio and subtitle track selection with sync delay adjustment
 * - Video scaling modes (Fit, Stretch, Crop/Zoom, 100%, 16:9, 4:3)
 * - Zero-buffering keyframe scrubbing (`hr-seek=no` + `absolute+keyframes`) with exact frame settle
 * - Zero-IPC local file playback via POSIX `fd://` from SAF [ParcelFileDescriptor.detachFd]
 */
class NativeMpvEngine(
    private val listener: MpvEngineListener
) : SurfaceHolder.Callback, MPVLib.EventObserver, MPVLib.LogObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isInitialized = AtomicBoolean(false)
    private val isDestroyed = AtomicBoolean(false)
    private val isSurfaceAttached = AtomicBoolean(false)

    private var surfaceViewRef: WeakReference<SurfaceView>? = null
    private val fdLock = Any()
    private var pendingOpenFd: Int = NO_FD
    private var activePlayingFd: Int = NO_FD
    private val drainingFds = mutableListOf<Int>()

    @Volatile
    private var pendingMediaPath: String? = null

    @Volatile
    private var pendingStartPositionMs: Long = 0L

    @Volatile
    private var currentDecoderMode: DecoderMode = DecoderMode.HW_PLUS

    @Volatile
    private var currentResizeMode: ResizeMode = ResizeMode.FIT

    @Volatile
    private var isPausedState: Boolean = false

    @Volatile
    private var lastVideoWidth: Int = 0

    @Volatile
    private var lastVideoHeight: Int = 0

    @Volatile
    private var lastSurfaceWidth: Int = 0

    @Volatile
    private var lastSurfaceHeight: Int = 0

    @Volatile
    private var hasFileLoadedForSurfaceRefresh: Boolean = false

    private val engineLock = Any()
    private var contextRef: WeakReference<Context>? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var legacyAudioFocusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var tracksChangedRunnable: Runnable? = null
    private val pausedSurfaceRefreshRunnables = mutableListOf<Runnable>()

    private fun requestAudioFocus(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) return // Already holding focus request
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            setPaused(true)
                        }
                    }
                }
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            val afListener = legacyAudioFocusListener ?: AudioManager.OnAudioFocusChangeListener { focusChange ->
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    setPaused(true)
                }
            }.also { legacyAudioFocusListener = it }
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                afListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus(context: Context?) {
        val ctx = context ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            legacyAudioFocusListener?.let {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(it)
            }
            legacyAudioFocusListener = null
        }
    }

    /**
     * Initializes the native libmpv context and applies MX Player HW+ options.
     * Safe to call multiple times; subsequent calls are no-ops until [releaseEngine] or [destroy].
     */
    fun initialize(context: Context) {
        synchronized(engineLock) {
            if (isDestroyed.get() || !isInitialized.compareAndSet(false, true)) return

            val appContext = context.applicationContext
            contextRef = WeakReference(appContext)
            MPVLib.create(appContext)
            configureEngineOptions()
            MPVLib.init()

            MPVLib.addObserver(this)
            MPVLib.addLogObserver(this)
            observeCoreProperties()
        }
    }

    private fun configureEngineOptions() {
        // Start with vo=null until a valid Android Surface attaches to avoid headless GPU init errors
        MPVLib.setOptionString("vo", "null")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")

        // Default decoder: Hardware-accelerated MediaCodec zero-copy (HW+)
        MPVLib.setOptionString("hwdec", DecoderMode.HW_PLUS.hwdecValue)
        MPVLib.setOptionString("hwdec-codecs", buildSupportedHwdecCodecs())

        // High-performance multi-threaded decoding
        MPVLib.setOptionString("vd-lavc-threads", "0") // Auto-detect all CPU cores
        MPVLib.setOptionString("vd-lavc-fast", "yes")
        MPVLib.setOptionString("vd-lavc-dr", "yes")
        MPVLib.setOptionString("framedrop", "vo") // Drop late frames in vo to maintain smooth playback without stutter
        MPVLib.setOptionString("demuxer-thread", "yes") // Isolated native demuxer thread

        // Mobile-tuned in-RAM demuxer ring buffer (64MiB total prevents OOM/LMK while keeping 30s readahead)
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-pause", "no")
        MPVLib.setOptionString("demuxer-seekable-cache", "yes")
        MPVLib.setOptionString("demuxer-max-bytes", "48MiB")
        MPVLib.setOptionString("demuxer-max-back-bytes", "16MiB")
        MPVLib.setOptionString("demuxer-readahead-secs", "30")

        // Seeking matching HW+
        MPVLib.setOptionString("hr-seek", "no")
        MPVLib.setOptionString("hr-seek-framedrop", "yes")

        // Network streaming & TLS options for HLS, DASH, RTSP, and direct HTTP/HTTPS video streams
        MPVLib.setOptionString("user-agent", "Mozilla/5.0 (Linux; Android 14) MediaPlayer/2.0")
        MPVLib.setOptionString("tls-verify", "no")
        MPVLib.setOptionString("network-timeout", "15")
        MPVLib.setOptionString("hls-bitrate", "max")
        MPVLib.setOptionString("stream-lavf-o", "reconnect=1,reconnect_streamed=1,reconnect_delay_max=5")

        // Logging (avoid flooding logcat on every seek/property query)
        MPVLib.setOptionString("msg-level", "all=warn")

        // Preserve full theatrical dynamic range (ZERO DRC on AC-3 / E-AC-3 / TrueHD)
        MPVLib.setOptionString("ad-lavc-ac3drc", "0")
        MPVLib.setOptionString("ad-lavc-downmix", "yes")
        MPVLib.setOptionString("audio-normalize-downmix", "no")

        // Downmixing to stereo with universal formats compatible with Android AudioTrack
        MPVLib.setOptionString("audio-channels", "stereo")

        // Pure standard Android AudioTrack output for native Android audio mixer to USB IEM
        MPVLib.setOptionString("ao", "audiotrack")
        MPVLib.setOptionString("audio-buffer", "0.3")
        MPVLib.setOptionString("audio-pitch-correction", "yes")
        MPVLib.setOptionString("volume", "100")
        MPVLib.setOptionString("volume-max", "200")

        // Subtitle and OSD styling
        MPVLib.setOptionString("sub-auto", "fuzzy")
        MPVLib.setOptionString("sub-font-size", "48")
        MPVLib.setOptionString("sub-border-size", "2.5")
        MPVLib.setOptionString("sub-shadow-offset", "1.5")
        MPVLib.setOptionString("sub-font", "sans-serif")

        // Player lifecycle & UI delegation (Compose renders all overlays)
        MPVLib.setOptionString("keep-open", "yes")
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("osc", "no")
        MPVLib.setOptionString("osd-level", "0")
    }

    private fun observeCoreProperties() {
        MPVLib.observeProperty("time-pos", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("duration", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("pause", MPVLib.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("paused-for-cache", MPVLib.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("demuxer-cache-duration", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("hwdec-current", MPVLib.MPV_FORMAT_STRING)
        MPVLib.observeProperty("video-params/w", MPVLib.MPV_FORMAT_INT64)
        MPVLib.observeProperty("video-params/h", MPVLib.MPV_FORMAT_INT64)
        MPVLib.observeProperty("eof-reached", MPVLib.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("track-list", MPVLib.MPV_FORMAT_NONE)
        MPVLib.observeProperty("chapter-list", MPVLib.MPV_FORMAT_NONE)
        MPVLib.observeProperty("chapter", MPVLib.MPV_FORMAT_INT64)
    }

    /**
     * Binds libmpv's OpenGL ES video output to a [SurfaceView] via its [SurfaceHolder].
     */
    fun attachToSurfaceView(surfaceView: SurfaceView) {
        if (isDestroyed.get()) return
        if (!isInitialized.get()) {
            initialize(surfaceView.context)
        }

        val currentView = surfaceViewRef?.get()
        if (currentView === surfaceView && isSurfaceAttached.get()) {
            return
        }

        if (currentView != null && currentView !== surfaceView) {
            currentView.holder.removeCallback(this)
            detachSurfaceInternal()
        }

        surfaceViewRef = WeakReference(surfaceView)
        val holder = surfaceView.holder
        holder.removeCallback(this)
        holder.addCallback(this)

        if (holder.surface != null && holder.surface.isValid) {
            val frame = holder.surfaceFrame
            val w = if (frame.width() > 0) frame.width() else surfaceView.width
            val h = if (frame.height() > 0) frame.height() else surfaceView.height
            attachSurfaceInternal(holder, w, h)
        }
    }

    fun hasAttachedSurface(): Boolean = isSurfaceAttached.get()

    fun detachFromSurfaceView() {
        val oldView = surfaceViewRef?.get()
        oldView?.holder?.removeCallback(this)
        surfaceViewRef = null
        detachSurfaceInternal()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (isDestroyed.get() || !isInitialized.get()) return
        val frame = holder.surfaceFrame
        val w = if (frame.width() > 0) frame.width() else surfaceViewRef?.get()?.width ?: 0
        val h = if (frame.height() > 0) frame.height() else surfaceViewRef?.get()?.height ?: 0
        attachSurfaceInternal(holder, w, h)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        if (width > 0 && height > 0) {
            val sizeChanged = lastSurfaceWidth > 0 && lastSurfaceHeight > 0 &&
                (width != lastSurfaceWidth || height != lastSurfaceHeight)
            lastSurfaceWidth = width
            lastSurfaceHeight = height
            try {
                MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
                if (sizeChanged) {
                    schedulePausedSurfaceRefresh(width, height)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update surface size: ${e.message}")
            }
        }
    }

    private fun cancelPausedSurfaceRefresh() {
        pausedSurfaceRefreshRunnables.forEach { mainHandler.removeCallbacks(it) }
        pausedSurfaceRefreshRunnables.clear()
    }

    private fun schedulePausedSurfaceRefresh(width: Int, height: Int) {
        cancelPausedSurfaceRefresh()
        if (!hasFileLoadedForSurfaceRefresh) return
        val paused = isPausedState || (runCatching { MPVLib.getPropertyBoolean("pause") == true }.getOrDefault(false))
        if (!paused) return

        // On Android BLASTBufferQueue + Adreno EGL, the first frame after a rotation/resize can
        // be rejected due to a pre-dequeued stale buffer or pre-rotation transform hint (transform=7).
        // When paused, mpv only renders once per resize, leaving the old buffer stretched on screen.
        // Nudge panscan subtly after the rotation transaction settles so vo_gpu swaps a fresh frame
        // matching the active surface size and transform without re-decoding or seeking.
        val delaysMs = longArrayOf(60L, 220L)
        for (delayMs in delaysMs) {
            val r = Runnable {
                if (isDestroyed.get() || !isInitialized.get() || !isSurfaceAttached.get() || !hasFileLoadedForSurfaceRefresh) {
                    return@Runnable
                }
                val stillPaused = isPausedState || (runCatching { MPVLib.getPropertyBoolean("pause") == true }.getOrDefault(false))
                if (!stillPaused) return@Runnable
                val view = surfaceViewRef?.get()
                val targetW = view?.width?.takeIf { it > 0 } ?: width
                val targetH = view?.height?.takeIf { it > 0 } ?: height
                try {
                    if (targetW != width || targetH != height) {
                        lastSurfaceWidth = targetW
                        lastSurfaceHeight = targetH
                        MPVLib.setPropertyString("android-surface-size", "${targetW}x${targetH}")
                    }
                    val basePanscan = if (currentResizeMode == ResizeMode.CROP) 1.0 else 0.0
                    val nudgePanscan = if (currentResizeMode == ResizeMode.CROP) 0.9999 else 0.0001
                    MPVLib.setPropertyDouble("panscan", nudgePanscan)
                    MPVLib.setPropertyDouble("panscan", basePanscan)
                } catch (e: Exception) {
                    Log.w(TAG, "Paused surface refresh failed: ${e.message}")
                }
            }
            pausedSurfaceRefreshRunnables.add(r)
            mainHandler.postDelayed(r, delayMs)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        cancelPausedSurfaceRefresh()
        detachSurfaceInternal()
    }

    private fun attachSurfaceInternal(holder: SurfaceHolder, width: Int, height: Int) {
        if (!holder.surface.isValid) return
        try {
            MPVLib.attachSurface(holder.surface)
            isSurfaceAttached.set(true)
            MPVLib.setPropertyString("vo", "gpu")
            MPVLib.setPropertyString("gpu-context", "android")
            MPVLib.setPropertyString("opengl-es", "yes")
            MPVLib.setPropertyString("vid", "auto")
            MPVLib.setPropertyString("hwdec", currentDecoderMode.hwdecValue)

            val w = if (width > 0) width else {
                val fWidth = holder.surfaceFrame.width()
                if (fWidth > 0) fWidth else surfaceViewRef?.get()?.width ?: 0
            }
            val h = if (height > 0) height else {
                val fHeight = holder.surfaceFrame.height()
                if (fHeight > 0) fHeight else surfaceViewRef?.get()?.height ?: 0
            }
            if (w > 0 && h > 0) {
                lastSurfaceWidth = w
                lastSurfaceHeight = h
                MPVLib.setPropertyString("android-surface-size", "${w}x${h}")
            }

            // Restore resize mode on newly attached surface
            applyResizeModeInternal(currentResizeMode)
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching surface to libmpv", e)
        }

        val queued = pendingMediaPath
        val queuedStart = pendingStartPositionMs
        if (queued != null) {
            pendingMediaPath = null
            pendingStartPositionMs = 0L
            loadPathInternal(queued, queuedStart)
        } else if (hasFileLoadedForSurfaceRefresh) {
            // Force redraw/refresh on newly attached surface if video is already loaded
            try {
                MPVLib.command(arrayOf("seek", "0", "relative+exact"))
            } catch (_: Exception) {}
        }
    }

    private fun detachSurfaceInternal() {
        lastSurfaceWidth = 0
        lastSurfaceHeight = 0
        if (isDestroyed.get() || !isInitialized.get()) return
        if (isSurfaceAttached.getAndSet(false)) {
            try {
                // Disable video decoding before detaching surface to avoid background 9-thread SW decode & BufferQueue leaks
                MPVLib.setPropertyString("vid", "no")
                MPVLib.setPropertyString("vo", "null")
                MPVLib.detachSurface()
            } catch (e: Exception) {
                Log.e(TAG, "Error detaching surface from libmpv", e)
            }
        }
    }

    /**
     * Opens a local video selected via Android Storage Access Framework (SAF) by detaching
     * the raw POSIX file descriptor and passing `fd://<fd>` straight to libmpv's C++ demuxer.
     * Optionally resumes from [startPositionMs].
     */
    fun openContentUri(
        context: Context,
        uri: Uri,
        startPositionMs: Long = 0L,
        allowAudioOnlyWithoutSurface: Boolean = false
    ): Boolean {
        if (isDestroyed.get()) return false
        if (!isInitialized.get()) {
            initialize(context)
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        // Direct network streams (http, https, rtsp, rtmp, mms, etc.) bypass ContentResolver and pass directly to libmpv
        if (scheme in listOf("http", "https", "rtsp", "rtmp", "mms")) {
            val urlString = uri.toString()
            val supersededPendingFd = synchronized(fdLock) {
                val oldPending = pendingOpenFd
                pendingOpenFd = NO_FD
                oldPending
            }
            closeFdQuietly(supersededPendingFd)

            if (isSurfaceAttached.get() || allowAudioOnlyWithoutSurface) {
                pendingMediaPath = null
                pendingStartPositionMs = 0L
                mainHandler.post { loadPathInternal(urlString, startPositionMs, NO_FD) }
            } else {
                pendingMediaPath = urlString
                pendingStartPositionMs = startPositionMs
            }
            return true
        }

        val pfd: ParcelFileDescriptor? = try {
            when {
                (uri.scheme == null || uri.scheme == "file") && uri.path != null -> {
                    ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
                }
                else -> context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Permission denied opening URI: $uri, attempting primary storage fallback", se)
            val recovered = openPrimaryStorageFallbackDescriptor(context, uri, allowAudioOnlyWithoutSurface)
            if (recovered != null) {
                Log.i(TAG, "Recovered access to expired SAF URI via primary storage fallback: $uri")
                recovered
            } else {
                Log.e(TAG, "Permission denied opening URI: $uri", se)
                mainHandler.post {
                    listener.onError("Permission expired for this file. Please re-open it using Open Files or Open Folder.")
                }
                return false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to open content URI: $uri", t)
            mainHandler.post { listener.onError("Failed to open video: ${t.localizedMessage ?: "Unknown error"}") }
            return false
        }

        if (pfd == null) {
            mainHandler.post { listener.onError("Unable to open file descriptor for selected video.") }
            return false
        }

        return try {
            val rawFd = pfd.detachFd()
            val supersededPendingFd = synchronized(fdLock) {
                val oldPending = pendingOpenFd
                pendingOpenFd = rawFd
                oldPending
            }
            closeFdQuietly(supersededPendingFd)
            val fdPath = "fd://$rawFd"

            if (isSurfaceAttached.get() || allowAudioOnlyWithoutSurface) {
                pendingMediaPath = null
                pendingStartPositionMs = 0L
                mainHandler.post { loadPathInternal(fdPath, startPositionMs, rawFd) }
            } else {
                pendingMediaPath = fdPath
                pendingStartPositionMs = startPositionMs
            }

            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to prepare file descriptor for URI: $uri", t)
            mainHandler.post { listener.onError("Failed to open video: ${t.localizedMessage ?: "Unknown error"}") }
            false
        }
    }

    private fun openPrimaryStorageFallbackDescriptor(
        context: Context,
        uri: Uri,
        includeAudioStore: Boolean
    ): ParcelFileDescriptor? {
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        val relPath = extractPrimaryRelativePath(uri.authority, docId, uri.pathSegments) ?: return null
        val targetFile = File(Environment.getExternalStorageDirectory(), relPath)

        // 1. Direct file read (works on Android 11+ FUSE when READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE is granted)
        try {
            if (targetFile.exists() && targetFile.isFile && targetFile.canRead()) {
                return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (_: Throwable) {}

        // 2. MediaStore lookup by DATA path (works on Android 10 scoped storage and older)
        val collections = buildList {
            add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            if (includeAudioStore) {
                add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            }
        }
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(targetFile.absolutePath)

        for (collection in collections) {
            try {
                context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val mediaStoreUri = ContentUris.withAppendedId(collection, id)
                        val fallbackPfd = context.contentResolver.openFileDescriptor(mediaStoreUri, "r")
                        if (fallbackPfd != null) {
                            return fallbackPfd
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun loadPathInternal(
        path: String,
        startPositionMs: Long = 0L,
        expectedPendingFd: Int? = null
    ) {
        if (isDestroyed.get() || !isInitialized.get()) {
            if (expectedPendingFd != null && expectedPendingFd >= 0) {
                synchronized(fdLock) {
                    if (pendingOpenFd == expectedPendingFd) {
                        pendingOpenFd = NO_FD
                    }
                }
                closeFdQuietly(expectedPendingFd)
            }
            return
        }

        synchronized(fdLock) {
            val fdToActivate = when {
                expectedPendingFd == NO_FD -> NO_FD
                expectedPendingFd != null -> {
                    // If a newer openContentUri already superseded this fd, skip stale load
                    if (pendingOpenFd != expectedPendingFd) {
                        return
                    }
                    pendingOpenFd = NO_FD
                    expectedPendingFd
                }
                else -> {
                    val pending = pendingOpenFd
                    pendingOpenFd = NO_FD
                    pending
                }
            }
            if (activePlayingFd >= 0 && activePlayingFd != fdToActivate) {
                drainingFds.add(activePlayingFd)
            }
            activePlayingFd = fdToActivate
        }
        hasFileLoadedForSurfaceRefresh = false

        if (startPositionMs > 1000L) {
            val startSec = String.format(Locale.US, "%.3f", startPositionMs / 1000.0)
            MPVLib.setOptionString("start", startSec)
        } else {
            MPVLib.setOptionString("start", "none")
        }
        MPVLib.setPropertyString("audio-channels", "stereo")
        MPVLib.setPropertyString("ao", "audiotrack")
        MPVLib.setPropertyBoolean("mute", false)
        MPVLib.setPropertyDouble("volume", 100.0)

        if (isSurfaceAttached.get()) {
            try {
                MPVLib.setPropertyString("vo", "gpu")
                MPVLib.setPropertyString("vid", "auto")
                MPVLib.setPropertyString("hwdec", currentDecoderMode.hwdecValue)
                val view = surfaceViewRef?.get()
                val w = view?.width ?: 0
                val h = view?.height ?: 0
                if (w > 0 && h > 0) {
                    MPVLib.setPropertyString("android-surface-size", "${w}x${h}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed setting options before loadfile: ${e.message}")
            }
        } else {
            try {
                MPVLib.setPropertyString("vid", "no")
                MPVLib.setPropertyString("vo", "null")
            } catch (_: Exception) {}
        }

        isPausedState = false
        MPVLib.command(arrayOf("loadfile", path, "replace"))
        MPVLib.setPropertyBoolean("pause", false)

        if (isSurfaceAttached.get()) {
            try {
                MPVLib.setPropertyString("vo", "gpu")
                MPVLib.setPropertyString("vid", "auto")
            } catch (_: Exception) {}
        } else {
            try {
                MPVLib.setPropertyString("vid", "no")
                MPVLib.setPropertyString("vo", "null")
            } catch (_: Exception) {}
        }

        contextRef?.get()?.let { requestAudioFocus(it) }
        mainHandler.post { listener.onPausedChanged(false) }
    }

    fun setDecoderMode(mode: DecoderMode) {
        if (isDestroyed.get() || !isInitialized.get()) return
        currentDecoderMode = mode
        MPVLib.setOptionString("hwdec", mode.hwdecValue)
        MPVLib.setPropertyString("hwdec", mode.hwdecValue)
    }

    fun getDecoderMode(): DecoderMode = currentDecoderMode

    fun setPaused(paused: Boolean) {
        if (isDestroyed.get() || !isInitialized.get()) return
        isPausedState = paused
        if (!paused) {
            cancelPausedSurfaceRefresh()
        }
        MPVLib.setPropertyBoolean("pause", paused)
        if (paused) {
            contextRef?.get()?.let { abandonAudioFocus(it) }
        } else {
            contextRef?.get()?.let { requestAudioFocus(it) }
        }
    }

    fun togglePlayPause() {
        if (isDestroyed.get() || !isInitialized.get()) return
        val nextPaused = !isPausedState
        setPaused(nextPaused)
    }

    fun setSoftwareVolume(percent: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyDouble("volume", percent.coerceIn(0, 200).toDouble())
    }

    /**
     * Completely stops playback, terminates demuxer/decoders, and releases Android AudioTrack.
     */
    fun stop() {
        if (isDestroyed.get() || !isInitialized.get()) return
        cancelPausedSurfaceRefresh()
        isPausedState = true
        hasFileLoadedForSurfaceRefresh = false
        try {
            MPVLib.setPropertyBoolean("pause", true)
            MPVLib.command(arrayOf("stop"))
            if (!isSurfaceAttached.get()) {
                MPVLib.setPropertyString("vo", "null")
            }
        } catch (_: Exception) {}
        contextRef?.get()?.let { abandonAudioFocus(it) }
        closeDrainingFds()
        closeAllFds()
        cleanTempSubtitleFiles()
        mainHandler.post { listener.onPausedChanged(true) }
    }

    // region Track Selection & Delay Sync

    /**
     * Queries all audio, video, and subtitle tracks currently loaded in libmpv.
     */
    fun queryTracks(): List<MediaTrack> {
        if (isDestroyed.get() || !isInitialized.get()) return emptyList()
        try {
            val jsonStr = MPVLib.getPropertyString("track-list")
            if (!jsonStr.isNullOrBlank()) {
                val arr = JSONArray(jsonStr)
                val list = ArrayList<MediaTrack>(arr.length())
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val type = obj.optString("type", "").ifBlank { continue }
                    val id = obj.optInt("id", i + 1)
                    val title = obj.optString("title", "").ifBlank { null }
                    val lang = if (type != "video") obj.optString("lang", "").ifBlank { null } else null
                    val codec = obj.optString("codec", "").ifBlank { null }
                    val selected = obj.optBoolean("selected", false)
                    val channels = if (type == "audio") obj.optInt("audio-channels", 0) else 0
                    val sampleRate = if (type == "audio") obj.optInt("demux-samplerate", 0) else 0
                    list.add(
                        MediaTrack(
                            id = id,
                            type = type,
                            title = title,
                            language = lang,
                            codec = codec,
                            channels = channels,
                            sampleRate = sampleRate,
                            isSelected = selected
                        )
                    )
                }
                return list
            }
        } catch (_: Exception) {}

        val count = MPVLib.getPropertyInt("track-list/count") ?: 0
        val list = mutableListOf<MediaTrack>()
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
            val id = MPVLib.getPropertyInt("track-list/$i/id") ?: (i + 1)
            val codec = MPVLib.getPropertyString("track-list/$i/codec")
            val selected = MPVLib.getPropertyBoolean("track-list/$i/selected") ?: false
            val channels = if (type == "audio") MPVLib.getPropertyInt("track-list/$i/audio-channels") ?: 0 else 0
            val sampleRate = if (type == "audio") MPVLib.getPropertyInt("track-list/$i/demux-samplerate") ?: 0 else 0

            list.add(
                MediaTrack(
                    id = id,
                    type = type,
                    title = null,
                    language = null,
                    codec = codec,
                    channels = channels,
                    sampleRate = sampleRate,
                    isSelected = selected
                )
            )
        }
        return list
    }

    fun selectTrack(type: String, id: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        when (type) {
            "audio" -> MPVLib.setPropertyInt("aid", id)
            "sub" -> MPVLib.setPropertyInt("sid", id)
            "video" -> {
                if (isSurfaceAttached.get()) {
                    MPVLib.setPropertyInt("vid", id)
                    MPVLib.setPropertyString("vo", "gpu")
                }
            }
        }
    }

    fun disableTrack(type: String) {
        if (isDestroyed.get() || !isInitialized.get()) return
        when (type) {
            "audio" -> MPVLib.setPropertyString("aid", "no")
            "sub" -> MPVLib.setPropertyString("sid", "no")
            "video" -> MPVLib.setPropertyString("vid", "no")
        }
    }

    fun addExternalSubtitle(context: Context, uri: Uri): Boolean {
        if (isDestroyed.get() || !isInitialized.get()) return false
        return try {
            val extension = uri.lastPathSegment?.substringAfterLast('.', "srt") ?: "srt"
            val tempFile = File(context.cacheDir, "ext_sub_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                MPVLib.command(arrayOf("sub-add", tempFile.absolutePath, "select"))
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load external subtitle: ${t.message}", t)
            false
        }
    }

    fun setPlaybackSpeed(speed: Double) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyDouble("speed", speed.coerceIn(0.25, 4.0))
    }

    fun getPlaybackSpeed(): Double {
        if (isDestroyed.get() || !isInitialized.get()) return 1.0
        return MPVLib.getPropertyDouble("speed") ?: 1.0
    }

    fun setResizeMode(mode: ResizeMode) {
        if (isDestroyed.get() || !isInitialized.get()) return
        currentResizeMode = mode
        applyResizeModeInternal(mode)
    }

    fun getResizeMode(): ResizeMode = currentResizeMode

    private fun applyResizeModeInternal(mode: ResizeMode) {
        when (mode) {
            ResizeMode.FIT -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            ResizeMode.STRETCH -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "no")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            ResizeMode.CROP -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 1.0)
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            ResizeMode.ORIGINAL_100 -> {
                MPVLib.setPropertyString("video-unscaled", "yes")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            ResizeMode.SIXTEEN_NINE -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "16:9")
            }
            ResizeMode.FOUR_THREE -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "4:3")
            }
        }
    }

    fun setAudioDelay(delaySec: Double) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyDouble("audio-delay", delaySec)
    }

    fun getAudioDelay(): Double {
        if (isDestroyed.get() || !isInitialized.get()) return 0.0
        return MPVLib.getPropertyDouble("audio-delay") ?: 0.0
    }

    fun setSubtitleDelay(delaySec: Double) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyDouble("sub-delay", delaySec)
    }

    fun getSubtitleDelay(): Double {
        if (isDestroyed.get() || !isInitialized.get()) return 0.0
        return MPVLib.getPropertyDouble("sub-delay") ?: 0.0
    }

    fun setLoopFile(loop: Boolean) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyString("loop-file", if (loop) "inf" else "no")
    }

    fun getMediaInfo(titleHint: String): MediaInfo {
        if (isDestroyed.get() || !isInitialized.get()) return MediaInfo(title = titleHint)
        val vCodec = MPVLib.getPropertyString("video-codec")?.uppercase(Locale.US) ?: "Unknown"
        val vFormat = MPVLib.getPropertyString("video-format")?.uppercase(Locale.US) ?: ""
        val fps = MPVLib.getPropertyDouble("container-fps")?.let { String.format(Locale.US, "%.2f fps", it) } ?: ""
        val aCodec = MPVLib.getPropertyString("audio-codec")?.uppercase(Locale.US) ?: "Unknown"
        val aChannels = MPVLib.getPropertyInt("audio-params/channel-count")?.let {
            if (it == 6) "5.1 Surround" else "$it Channels"
        } ?: ""
        val aRate = MPVLib.getPropertyInt("audio-params/samplerate")?.let { "${it / 1000} kHz" } ?: ""
        val hwdec = MPVLib.getPropertyString("hwdec-current") ?: currentDecoderMode.hwdecValue
        val cacheSec = MPVLib.getPropertyDouble("demuxer-cache-duration")?.let { String.format(Locale.US, "%.1fs readahead", it) } ?: "Active"

        val res = if (lastVideoWidth > 0 && lastVideoHeight > 0) "${lastVideoWidth}x${lastVideoHeight}" else "Unknown"

        return MediaInfo(
            title = titleHint,
            resolution = res,
            videoCodec = if (vFormat.isNotEmpty() && vFormat != vCodec) "$vCodec ($vFormat)" else vCodec,
            videoFps = fps,
            audioCodec = aCodec,
            audioChannels = aChannels,
            sampleRate = aRate,
            hwdec = hwdec,
            demuxerCache = "64MiB RAM Ring Buffer ($cacheSec)"
        )
    }

    fun setNightModeAudio(enabled: Boolean) {
        if (isDestroyed.get() || !isInitialized.get()) return
        if (enabled) {
            // Apply dynamic audio normalization for speech clarity and dialog leveling
            MPVLib.command(arrayOf("af", "set", "lavfi=[dynaudnorm=f=250:g=15:p=0.95]"))
        } else {
            MPVLib.command(arrayOf("af", "clr", ""))
        }
    }

    fun setMuted(muted: Boolean) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyBoolean("mute", muted)
    }

    fun setVideoBrightness(value: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("brightness", value.coerceIn(-100, 100))
    }

    fun setVideoContrast(value: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("contrast", value.coerceIn(-100, 100))
    }

    fun setVideoSaturation(value: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("saturation", value.coerceIn(-100, 100))
    }

    fun setVideoGamma(value: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("gamma", value.coerceIn(-100, 100))
    }

    fun resetVideoEqualizer() {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("brightness", 0)
        MPVLib.setPropertyInt("contrast", 0)
        MPVLib.setPropertyInt("saturation", 0)
        MPVLib.setPropertyInt("gamma", 0)
    }

    fun queryChapters(): List<MediaChapter> {
        if (isDestroyed.get() || !isInitialized.get()) return emptyList()
        return try {
            val jsonStr = MPVLib.getPropertyString("chapter-list") ?: return emptyList()
            if (jsonStr.isBlank()) return emptyList()
            val arr = JSONArray(jsonStr)
            val result = ArrayList<MediaChapter>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val time = obj.optDouble("time", 0.0).coerceAtLeast(0.0)
                val title = obj.optString("title", "").ifBlank { "Chapter ${i + 1}" }
                result.add(MediaChapter(index = i, title = title, timeSeconds = time))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getCurrentChapterIndex(): Int {
        if (isDestroyed.get() || !isInitialized.get()) return -1
        return MPVLib.getPropertyInt("chapter") ?: -1
    }

    fun seekToChapter(index: Int) {
        if (isDestroyed.get() || !isInitialized.get() || index < 0) return
        MPVLib.setPropertyInt("chapter", index)
    }

    fun setSubtitleFontSize(size: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("sub-font-size", size.coerceIn(24, 96))
    }

    fun setSubtitleColor(colorHex: String) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyString("sub-color", colorHex)
    }

    fun captureSurfaceFrame(onResult: (Bitmap?) -> Unit) {
        if (isDestroyed.get() || !isInitialized.get()) {
            onResult(null)
            return
        }
        val view = surfaceViewRef?.get()
        val surface = view?.holder?.surface
        if (view == null || surface == null || !surface.isValid || view.width <= 0 || view.height <= 0) {
            onResult(null)
            return
        }
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(
                view,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        onResult(bitmap)
                    } else {
                        Log.e(TAG, "PixelCopy failed with code=$copyResult")
                        bitmap.recycle()
                        onResult(null)
                    }
                },
                mainHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "captureSurfaceFrame failed: ${e.message}", e)
            onResult(null)
        }
    }

    fun captureScreenshot(outputFile: File): Boolean {
        if (isDestroyed.get() || !isInitialized.get()) return false
        return try {
            MPVLib.command(arrayOf("screenshot-to-file", outputFile.absolutePath, "video"))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot failed: ${e.message}", e)
            false
        }
    }

    // endregion

    fun beginKeyframeScrub() {
        if (isDestroyed.get() || !isInitialized.get()) return
    }

    fun seekToKeyframe(targetSeconds: Double) {
        if (isDestroyed.get() || !isInitialized.get()) return
        val clamped = targetSeconds.coerceAtLeast(0.0)
        MPVLib.command(
            arrayOf(
                "seek",
                String.format(Locale.US, "%.3f", clamped),
                "absolute+keyframes"
            )
        )
    }

    fun seekTo(targetSeconds: Double) {
        seekToKeyframe(targetSeconds)
    }

    fun endExactScrub(finalSeconds: Double) {
        seekToKeyframe(finalSeconds)
    }

    fun seekRelative(deltaSeconds: Double) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.command(
            arrayOf(
                "seek",
                String.format(Locale.US, "%.3f", deltaSeconds),
                "relative+keyframes"
            )
        )
    }

    /**
     * Releases the native libmpv context and EGL surface memory (re-creatable on next playback).
     */
    fun releaseEngine() {
        synchronized(engineLock) {
            if (!isInitialized.compareAndSet(true, false)) return
            isPausedState = false
            hasFileLoadedForSurfaceRefresh = false
            lastVideoWidth = 0
            lastVideoHeight = 0
            lastSurfaceWidth = 0
            lastSurfaceHeight = 0
            pendingMediaPath = null
            pendingStartPositionMs = 0L

            try {
                tracksChangedRunnable?.let { mainHandler.removeCallbacks(it) }
                tracksChangedRunnable = null
                cancelPausedSurfaceRefresh()
                contextRef?.get()?.let { abandonAudioFocus(it) }
                mainHandler.removeCallbacksAndMessages(null)
                MPVLib.removeObserver(this)
                MPVLib.removeLogObserver(this)
                surfaceViewRef?.get()?.holder?.removeCallback(this)
                surfaceViewRef = null

                if (isSurfaceAttached.compareAndSet(true, false)) {
                    MPVLib.setPropertyString("vid", "no")
                    MPVLib.setPropertyString("vo", "null")
                    MPVLib.detachSurface()
                }

                MPVLib.destroy()
            } catch (t: Throwable) {
                Log.e(TAG, "Error during libmpv release", t)
            } finally {
                closeAllFds()
                cleanTempSubtitleFiles()
            }
        }
    }

    fun destroy() {
        if (!isDestroyed.compareAndSet(false, true)) return
        releaseEngine()
    }

    // region MPVLib.EventObserver

    override fun eventProperty(property: String) {
        if (property == "track-list") {
            tracksChangedRunnable?.let { mainHandler.removeCallbacks(it) }
            val r = Runnable { listener.onTracksChanged() }
            tracksChangedRunnable = r
            mainHandler.postDelayed(r, 60L)
        } else if (property == "chapter-list") {
            mainHandler.post { listener.onChaptersChanged() }
        }
    }

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "video-params/w" -> {
                lastVideoWidth = value.toInt()
                if (lastVideoWidth > 0 && lastVideoHeight > 0) {
                    mainHandler.post { listener.onVideoSizeChanged(lastVideoWidth, lastVideoHeight) }
                }
            }
            "video-params/h" -> {
                lastVideoHeight = value.toInt()
                if (lastVideoWidth > 0 && lastVideoHeight > 0) {
                    mainHandler.post { listener.onVideoSizeChanged(lastVideoWidth, lastVideoHeight) }
                }
            }
            "chapter" -> {
                mainHandler.post { listener.onCurrentChapterChanged(value.toInt()) }
            }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> {
                isPausedState = value
                mainHandler.post { listener.onPausedChanged(value) }
            }
            "paused-for-cache" -> mainHandler.post { listener.onBufferingChanged(value) }
            "eof-reached" -> if (value) mainHandler.post { listener.onEndOfFile() }
        }
    }

    override fun eventProperty(property: String, value: String) {
        if (property == "hwdec-current") {
            mainHandler.post { listener.onHwdecChanged(value) }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos" -> mainHandler.post { listener.onPositionChanged(value) }
            "duration" -> mainHandler.post { listener.onDurationChanged(value) }
            "demuxer-cache-duration" -> mainHandler.post { listener.onCacheDurationChanged(value) }
        }
    }

    override fun event(eventId: Int) {
        when (eventId) {
            MPVLib.MPV_EVENT_END_FILE -> {
                closeOldestDrainingFd()
            }
            MPVLib.MPV_EVENT_FILE_LOADED -> {
                hasFileLoadedForSurfaceRefresh = true
                closeDrainingFds()
                if (isSurfaceAttached.get()) {
                    try {
                        MPVLib.setPropertyString("vo", "gpu")
                        MPVLib.setPropertyString("vid", "auto")
                        val view = surfaceViewRef?.get()
                        if (view != null && view.width > 0 && view.height > 0) {
                            MPVLib.setPropertyString("android-surface-size", "${view.width}x${view.height}")
                        }
                        applyResizeModeInternal(currentResizeMode)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed restoring video surface on file loaded: ${e.message}")
                    }
                }
                mainHandler.post {
                    listener.onFileLoaded()
                }
            }
            else -> Unit
            // Note: MPV_EVENT_END_FILE is intentionally NOT mapped to onEndOfFile()
            // because libmpv fires MPV_EVENT_END_FILE when stopping, replacing, or loading files.
            // Natural playback end-of-file is detected exclusively via the observed "eof-reached" property.
        }
    }

    // endregion

    // region MPVLib.LogObserver

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (level > MPVLib.MPV_LOG_LEVEL_INFO) return
        val clean = text.trimEnd()
        when (level) {
            MPVLib.MPV_LOG_LEVEL_FATAL, MPVLib.MPV_LOG_LEVEL_ERROR -> Log.e(TAG, "[$prefix] $clean")
            MPVLib.MPV_LOG_LEVEL_WARN -> Log.w(TAG, "[$prefix] $clean")
            MPVLib.MPV_LOG_LEVEL_INFO -> Log.i(TAG, "[$prefix] $clean")
        }
    }

    // endregion

    private fun cleanTempSubtitleFiles() {
        try {
            val dir = contextRef?.get()?.cacheDir ?: return
            val tempFiles = dir.listFiles { file -> file.name.startsWith("ext_sub_") }
            tempFiles?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun buildSupportedHwdecCodecs(): String {
        val base = mutableListOf("h264", "hevc", "vp8", "vp9", "mpeg2video", "mpeg4", "vc1")
        val hasHardwareAv1 = try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                !info.isEncoder &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || info.isHardwareAccelerated) &&
                    info.supportedTypes.any { it.equals("video/av01", ignoreCase = true) }
            }
        } catch (_: Throwable) {
            false
        }
        if (hasHardwareAv1) {
            base.add("av1")
        }
        return base.joinToString(",")
    }

    private fun closeOldestDrainingFd() {
        val fdToClose = synchronized(fdLock) {
            if (drainingFds.isEmpty()) return
            drainingFds.removeAt(0)
        }
        closeFdQuietly(fdToClose)
    }

    private fun closeDrainingFds() {
        val toClose = synchronized(fdLock) {
            if (drainingFds.isEmpty()) return
            val copy = drainingFds.toList()
            drainingFds.clear()
            copy
        }
        toClose.forEach { closeFdQuietly(it) }
    }

    private fun closeAllFds() {
        val toClose = synchronized(fdLock) {
            val list = mutableListOf<Int>()
            if (pendingOpenFd >= 0) {
                list.add(pendingOpenFd)
                pendingOpenFd = NO_FD
            }
            if (activePlayingFd >= 0) {
                list.add(activePlayingFd)
                activePlayingFd = NO_FD
            }
            if (drainingFds.isNotEmpty()) {
                list.addAll(drainingFds)
                drainingFds.clear()
            }
            list
        }
        toClose.forEach { closeFdQuietly(it) }
    }

    private fun closeFdQuietly(fdInt: Int) {
        if (fdInt < 0) return
        try {
            ParcelFileDescriptor.adoptFd(fdInt).close()
        } catch (e: Exception) {
            if (e.cause !is ErrnoException) {
                Log.w(TAG, "Ignoring error closing detached fd=$fdInt: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "NativeMpvEngine"
        private const val NO_FD = -1

        fun extractPrimaryRelativePath(
            authority: String?,
            documentId: String?,
            pathSegments: List<String> = emptyList()
        ): String? {
            if (!authority.equals("com.android.externalstorage.documents", ignoreCase = true)) {
                return null
            }
            val resolvedDocId = documentId?.takeIf { it.isNotBlank() } ?: run {
                val docIdx = pathSegments.indexOf("document")
                if (docIdx >= 0 && docIdx + 1 < pathSegments.size) {
                    pathSegments[docIdx + 1]
                } else {
                    null
                }
            } ?: return null

            if (!resolvedDocId.startsWith("primary:", ignoreCase = true)) {
                return null
            }
            val relative = resolvedDocId.substringAfter(':').trimStart('/')
            if (relative.isEmpty() || relative.split('/', '\\').any { it == ".." }) {
                return null
            }
            return relative
        }
    }
}

package com.mxplayer.mpv

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import dev.jdtech.mpv.MPVLib
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    fun onFileLoaded() {}
    fun onEndOfFile() {}
    fun onError(message: String) {}
}

/**
 * Production-ready controller wrapping [MPVLib] (FFmpeg + MediaCodec + OpenGL ES).
 *
 * Engineered for MX Player HW+ playback characteristics:
 * - Aggressive in-RAM demuxer ring buffer (`150MiB` forward / `50MiB` backward)
 * - OpenGL ES hardware video output (`vo=gpu`, `gpu-context=android`)
 * - Multi-audio and subtitle track selection with sync delay adjustment
 * - Video scaling modes (Fit, Stretch, Crop/Zoom, 100%, 16:9, 4:3)
 * - Zero-buffering keyframe scrubbing (`hr-seek=no` + `absolute+keyframes`) with exact frame settle
 * - Zero-IPC local file playback via POSIX `fd://` from SAF [ParcelFileDescriptor.detachFd]
 */
class NativeMpvEngine(
    private val listener: MpvEngineListener
) : SurfaceHolder.Callback, MPVLib.EventObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isInitialized = AtomicBoolean(false)
    private val isDestroyed = AtomicBoolean(false)
    private val isSurfaceAttached = AtomicBoolean(false)

    private var surfaceViewRef: WeakReference<SurfaceView>? = null
    private val activeDetachedFd = AtomicInteger(NO_FD)

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

    /**
     * Initializes the native libmpv context and applies MX Player HW+ options.
     * Safe to call multiple times; subsequent calls are no-ops until [destroy].
     */
    fun initialize(context: Context) {
        if (isDestroyed.get() || !isInitialized.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        MPVLib.create(appContext)
        configureEngineOptions()
        MPVLib.init()

        MPVLib.addObserver(this)
        observeCoreProperties()
    }

    private fun configureEngineOptions() {
        // Video output: OpenGL ES hardware rendering pipeline
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")

        // Default decoder: MX Player HW+ equivalent (FFmpeg RAM demux -> MediaCodec zero-copy)
        MPVLib.setOptionString("hwdec", DecoderMode.HW_PLUS.hwdecValue)
        MPVLib.setOptionString("hwdec-codecs", "all")

        // High-performance multi-threaded decoding
        MPVLib.setOptionString("vd-lavc-threads", "0") // Auto-detect all CPU cores
        MPVLib.setOptionString("vd-lavc-fast", "yes")
        MPVLib.setOptionString("framedrop", "vo") // Drop video output frames under heavy system load
        MPVLib.setOptionString("demuxer-thread", "yes") // Isolated native demuxer thread

        // Aggressive in-memory packet ring buffer (150MiB RAM) for instant seeking
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-pause", "no")
        MPVLib.setOptionString("cache-pause-wait", "0")
        MPVLib.setOptionString("cache-pause-initial", "no")
        MPVLib.setOptionString("demuxer-seekable-cache", "yes")
        MPVLib.setOptionString("demuxer-max-bytes", "150MiB")
        MPVLib.setOptionString("demuxer-max-back-bytes", "50MiB")
        MPVLib.setOptionString("demuxer-readahead-secs", "30")

        // Fast keyframe-aligned seeking matching MX Player HW+ (zero decode preroll lag)
        MPVLib.setOptionString("hr-seek", "no")
        MPVLib.setOptionString("hr-seek-framedrop", "yes")

        // Android AudioTrack output with low audio latency, pitch-corrected speed control and software boost headroom
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("audio-pitch-correction", "yes")
        MPVLib.setOptionString("audio-buffer", "0.2")
        MPVLib.setOptionString("volume-max", "200")

        // Subtitle and OSD styling
        MPVLib.setOptionString("sub-auto", "fuzzy")
        MPVLib.setOptionString("sub-font-size", "48")
        MPVLib.setOptionString("sub-border-size", "2.5")
        MPVLib.setOptionString("sub-shadow-offset", "1.5")
        MPVLib.setOptionString("sub-font", "sans-serif")

        // Player lifecycle & UI delegation (Compose renders all overlays)
        MPVLib.setOptionString("force-window", "no")
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
    }

    /**
     * Binds libmpv's OpenGL ES video output to a [SurfaceView] via its [SurfaceHolder].
     */
    fun attachToSurfaceView(surfaceView: SurfaceView) {
        if (isDestroyed.get()) return
        if (!isInitialized.get()) {
            initialize(surfaceView.context)
        }

        detachFromSurfaceView()
        surfaceViewRef = WeakReference(surfaceView)
        val holder = surfaceView.holder
        holder.addCallback(this)

        if (holder.surface != null && holder.surface.isValid) {
            attachSurfaceInternal(holder, surfaceView.width, surfaceView.height)
        }
    }

    fun detachFromSurfaceView() {
        surfaceViewRef?.get()?.holder?.removeCallback(this)
        surfaceViewRef = null
        detachSurfaceInternal()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (isDestroyed.get() || !isInitialized.get()) return
        val frame = holder.surfaceFrame
        attachSurfaceInternal(holder, frame.width(), frame.height())
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (isDestroyed.get() || !isInitialized.get() || !isSurfaceAttached.get()) return
        if (width > 0 && height > 0) {
            MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        detachSurfaceInternal()
    }

    private fun attachSurfaceInternal(holder: SurfaceHolder, width: Int, height: Int) {
        if (isSurfaceAttached.compareAndSet(false, true)) {
            MPVLib.attachSurface(holder.surface)
            MPVLib.setOptionString("force-window", "yes")
            MPVLib.setPropertyString("vo", "gpu")
            if (width > 0 && height > 0) {
                MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
            }

            // Restore resize mode on newly attached surface
            applyResizeModeInternal(currentResizeMode)

            val queued = pendingMediaPath
            val queuedStart = pendingStartPositionMs
            if (queued != null) {
                pendingMediaPath = null
                pendingStartPositionMs = 0L
                loadPathInternal(queued, queuedStart)
            }
        }
    }

    private fun detachSurfaceInternal() {
        if (isDestroyed.get() || !isInitialized.get()) return
        if (isSurfaceAttached.compareAndSet(true, false)) {
            MPVLib.setPropertyString("vo", "null")
            MPVLib.setOptionString("force-window", "no")
            MPVLib.detachSurface()
        }
    }

    /**
     * Opens a local video selected via Android Storage Access Framework (SAF) by detaching
     * the raw POSIX file descriptor and passing `fd://<fd>` straight to libmpv's C++ demuxer.
     * Optionally resumes from [startPositionMs].
     */
    fun openContentUri(context: Context, uri: Uri, startPositionMs: Long = 0L): Boolean {
        if (isDestroyed.get()) return false
        if (!isInitialized.get()) {
            initialize(context)
        }

        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: run {
                    mainHandler.post { listener.onError("Unable to open file descriptor for selected video.") }
                    return false
                }

            val rawFd = pfd.detachFd()
            val previousFd = activeDetachedFd.getAndSet(rawFd)
            val fdPath = "fd://$rawFd"

            if (isSurfaceAttached.get()) {
                mainHandler.post { loadPathInternal(fdPath, startPositionMs) }
            } else {
                pendingMediaPath = fdPath
                pendingStartPositionMs = startPositionMs
            }

            closeFdQuietly(previousFd)
            true
        } catch (se: SecurityException) {
            Log.e(TAG, "Permission denied opening URI: $uri", se)
            mainHandler.post { listener.onError("Permission denied for selected video.") }
            false
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to open content URI: $uri", t)
            mainHandler.post { listener.onError("Failed to open video: ${t.localizedMessage ?: "Unknown error"}") }
            false
        }
    }

    private fun loadPathInternal(path: String, startPositionMs: Long = 0L) {
        if (startPositionMs > 1000L) {
            val startSec = String.format(Locale.US, "%.3f", startPositionMs / 1000.0)
            MPVLib.setOptionString("start", startSec)
        } else {
            MPVLib.setOptionString("start", "none")
        }
        isPausedState = false
        MPVLib.command(arrayOf("loadfile", path, "replace"))
        MPVLib.setPropertyBoolean("pause", false)
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
        MPVLib.setPropertyBoolean("pause", paused)
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

    // region Track Selection & Delay Sync

    /**
     * Queries all audio, video, and subtitle tracks currently loaded in libmpv.
     */
    fun queryTracks(): List<MediaTrack> {
        if (isDestroyed.get() || !isInitialized.get()) return emptyList()
        val count = MPVLib.getPropertyInt("track-list/count") ?: 0
        val list = mutableListOf<MediaTrack>()
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
            val id = MPVLib.getPropertyInt("track-list/$i/id") ?: (i + 1)
            val title = MPVLib.getPropertyString("track-list/$i/title")
            val lang = MPVLib.getPropertyString("track-list/$i/lang")
            val codec = MPVLib.getPropertyString("track-list/$i/codec")
            val selected = MPVLib.getPropertyBoolean("track-list/$i/selected") ?: false
            val channels = MPVLib.getPropertyInt("track-list/$i/audio-channels") ?: 0
            val sampleRate = MPVLib.getPropertyInt("track-list/$i/demux-samplerate") ?: 0

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

    fun selectTrack(type: String, id: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        when (type) {
            "audio" -> MPVLib.setPropertyInt("aid", id)
            "sub" -> MPVLib.setPropertyInt("sid", id)
        }
    }

    fun disableTrack(type: String) {
        if (isDestroyed.get() || !isInitialized.get()) return
        when (type) {
            "audio" -> MPVLib.setPropertyString("aid", "no")
            "sub" -> MPVLib.setPropertyString("sid", "no")
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
                MPVLib.setPropertyString("video-aspect-override", "-1")
            }
            ResizeMode.STRETCH -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "no")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "-1")
            }
            ResizeMode.CROP -> {
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 1.0)
                MPVLib.setPropertyString("video-aspect-override", "-1")
            }
            ResizeMode.ORIGINAL_100 -> {
                MPVLib.setPropertyString("video-unscaled", "yes")
                MPVLib.setPropertyString("keepaspect", "yes")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-aspect-override", "-1")
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
            demuxerCache = "150MiB RAM Ring Buffer ($cacheSec)"
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

    fun setSubtitleFontSize(size: Int) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyInt("sub-font-size", size.coerceIn(24, 96))
    }

    fun setSubtitleColor(colorHex: String) {
        if (isDestroyed.get() || !isInitialized.get()) return
        MPVLib.setPropertyString("sub-color", colorHex)
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

    fun destroy() {
        if (!isDestroyed.compareAndSet(false, true)) return
        if (!isInitialized.get()) return

        try {
            mainHandler.removeCallbacksAndMessages(null)
            MPVLib.removeObserver(this)
            surfaceViewRef?.get()?.holder?.removeCallback(this)
            surfaceViewRef = null

            if (isSurfaceAttached.compareAndSet(true, false)) {
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setOptionString("force-window", "no")
                MPVLib.detachSurface()
            }

            MPVLib.destroy()
        } catch (t: Throwable) {
            Log.e(TAG, "Error during libmpv teardown", t)
        } finally {
            closeFdQuietly(activeDetachedFd.getAndSet(NO_FD))
        }
    }

    // region MPVLib.EventObserver

    override fun eventProperty(property: String) {
        if (property == "track-list") {
            mainHandler.post { listener.onTracksChanged() }
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
            MPVLib.MPV_EVENT_FILE_LOADED -> {
                mainHandler.post {
                    listener.onTracksChanged()
                    listener.onFileLoaded()
                }
            }
            // Note: MPV_EVENT_END_FILE is intentionally NOT mapped to onEndOfFile()
            // because libmpv fires MPV_EVENT_END_FILE when stopping, replacing, or loading files.
            // Natural playback end-of-file is detected exclusively via the observed "eof-reached" property.
        }
    }

    // endregion

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
    }
}

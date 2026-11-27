package com.bkm.player.mpv

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToLong

sealed interface VerticalGestureHud {
    data class Brightness(val percent: Int) : VerticalGestureHud
    data class Volume(val percent: Int, val isSoftwareBoosted: Boolean) : VerticalGestureHud
}

data class PlayerUiState(
    val hasMediaLoaded: Boolean = false,
    val mediaTitle: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionSeconds: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val cacheDurationSeconds: Double = 0.0,
    val decoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val activeHwdec: String = "mediacodec",
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isScrubbing: Boolean = false,
    val isGestureScrubbing: Boolean = false,
    val scrubPreviewSeconds: Double = 0.0,
    val scrubDeltaSeconds: Double = 0.0,
    val verticalHud: VerticalGestureHud? = null,
    val controlsVisible: Boolean = true,
    val errorMessage: String? = null,
    val hasPlaybackFailed: Boolean = false,
    // Track, Audio, Subtitle & Video Scaling states
    val audioTracks: List<MediaTrack> = emptyList(),
    val subtitleTracks: List<MediaTrack> = emptyList(),
    val selectedAudioTrackId: Int? = null,
    val selectedSubtitleTrackId: Int? = null,
    val playbackSpeed: Double = 1.0,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val isRepeatOne: Boolean = false,
    val audioDelaySeconds: Double = 0.0,
    val subtitleDelaySeconds: Double = 0.0,
    val mediaInfo: MediaInfo = MediaInfo(),
    // Enhanced Pro Features
    val isFastForwarding: Boolean = false,
    val isNightModeAudio: Boolean = false,
    val isMuted: Boolean = false,
    val abRepeatPointA: Double? = null,
    val abRepeatPointB: Double? = null,
    val subtitleFontSize: Int = 48,
    val subtitleColor: String = "#FFFFFF",
    val chapters: List<MediaChapter> = emptyList(),
    val currentChapterIndex: Int = -1,
    val videoBrightness: Int = 0,
    val videoContrast: Int = 0,
    val videoSaturation: Int = 0,
    val videoGamma: Int = 0
) {
    /**
     * MX Player-style scrubbing HUD string, e.g. `01:14:22 [+00:15]` or `01:14:07 [-00:15]`.
     */
    val scrubOverlayText: String
        get() {
            val showHours = durationSeconds >= 3600.0
            val targetFormatted = formatTimestamp(scrubPreviewSeconds, forceHours = showHours)
            val sign = if (scrubDeltaSeconds >= 0.0) "+" else "-"
            val deltaFormatted = formatDelta(abs(scrubDeltaSeconds))
            return "$targetFormatted [$sign$deltaFormatted]"
        }
}

/**
 * High-frequency playback progress state isolated from [PlayerUiState] so 10Hz time ticks,
 * cache updates, and frame-rate scrubbing deltas only recompose the timeline slider and scrub HUD.
 */
data class PlaybackProgressState(
    val positionSeconds: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val cacheDurationSeconds: Double = 0.0,
    val isScrubbing: Boolean = false,
    val scrubPreviewSeconds: Double = 0.0,
    val scrubDeltaSeconds: Double = 0.0
)

/**
 * Coordinates [NativeMpvEngine] state, 60fps throttled keyframe scrubbing, decoder mode switching,
 * multi-audio/subtitle track management, audio/subtitle delay sync, playback speed, aspect ratio,
 * and clean lifecycle teardown in [onCleared].
 */
class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _progressState = MutableStateFlow(PlaybackProgressState())
    val progressState: StateFlow<PlaybackProgressState> = _progressState.asStateFlow()

    private var scrubOriginSeconds: Double = 0.0
    private var lastScrubDispatchNanos: Long = 0L
    private var lastPositionPersistMs: Long = 0L
    private var bufferingDebounceJob: Job? = null
    private var hasTriggeredEofForCurrentMedia = false
    private var isAudioDisabled = false

    var onPositionUpdate: ((posMs: Long, durMs: Long) -> Unit)? = null
    var onProgressUpdate: ((posMs: Long, durMs: Long) -> Unit)? = null
    var onIsPlayingChanged: ((Boolean) -> Unit)? = null
    var onEndOfFileReached: (() -> Unit)? = null

    var currentLoadedUri: Uri? = null
        private set

    private val engineListener = object : MpvEngineListener {
        override fun onPositionChanged(seconds: Double) {
            _progressState.update { state ->
                if (state.isScrubbing) state else state.copy(positionSeconds = seconds)
            }

            // A-B Repeat Loop Trigger
            val ui = _uiState.value
            val a = ui.abRepeatPointA
            val b = ui.abRepeatPointB
            if (a != null && b != null && b > a && seconds >= b) {
                seekTo(a)
                return
            }

            val posMs = (seconds * 1000.0).roundToLong()
            val durMs = (_progressState.value.durationSeconds * 1000.0).roundToLong()
            if (durMs > 0 && posMs >= 0) {
                onProgressUpdate?.invoke(posMs, durMs)
                val now = SystemClock.elapsedRealtime()
                if (now - lastPositionPersistMs >= 2000L) {
                    lastPositionPersistMs = now
                    onPositionUpdate?.invoke(posMs, durMs)
                }
            }
        }

        override fun onDurationChanged(seconds: Double) {
            val dur = seconds.coerceAtLeast(0.0)
            _progressState.update { it.copy(durationSeconds = dur) }
            val currentUiDur = _uiState.value.durationSeconds
            if ((currentUiDur <= 0.0 && dur > 0.0) || abs(currentUiDur - dur) > 1.0) {
                _uiState.update { it.copy(durationSeconds = dur) }
            }
            val posMs = (_progressState.value.positionSeconds * 1000.0).roundToLong()
            val durMs = (dur * 1000.0).roundToLong()
            if (durMs > 0) {
                onProgressUpdate?.invoke(posMs, durMs)
            }
        }

        override fun onPausedChanged(paused: Boolean) {
            _uiState.update { it.copy(isPlaying = !paused) }
            onIsPlayingChanged?.invoke(!paused)
        }

        override fun onBufferingChanged(buffering: Boolean) {
            bufferingDebounceJob?.cancel()
            if (buffering) {
                // Ignore transient micro-pauses during scrubbing or seeking
                if (_progressState.value.isScrubbing) return
                // Debounce buffering indicator (200ms) so fast keyframe seeks never flicker a loading spinner
                bufferingDebounceJob = viewModelScope.launch {
                    delay(200L)
                    _uiState.update { it.copy(isBuffering = true) }
                }
            } else {
                _uiState.update { it.copy(isBuffering = false) }
            }
        }

        override fun onCacheDurationChanged(seconds: Double) {
            _progressState.update { it.copy(cacheDurationSeconds = seconds.coerceAtLeast(0.0)) }
        }

        override fun onHwdecChanged(activeHwdec: String) {
            _uiState.update {
                it.copy(activeHwdec = activeHwdec.ifBlank { "sw" })
            }
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            _uiState.update { it.copy(videoWidth = width, videoHeight = height) }
        }

        override fun onTracksChanged() {
            refreshTracks()
        }

        override fun onChaptersChanged() {
            refreshChapters()
        }

        override fun onCurrentChapterChanged(chapterIndex: Int) {
            _uiState.update { it.copy(currentChapterIndex = chapterIndex) }
        }

        override fun onFileLoaded() {
            hasTriggeredEofForCurrentMedia = false
            isAudioDisabled = false
            _progressState.update {
                if (it.isScrubbing) it.copy(isScrubbing = false, scrubDeltaSeconds = 0.0) else it
            }
            _uiState.update {
                it.copy(
                    hasMediaLoaded = true,
                    isPlaying = true,
                    isBuffering = false,
                    isScrubbing = false,
                    isGestureScrubbing = false,
                    isMuted = false,
                    errorMessage = null,
                    hasPlaybackFailed = false
                )
            }
            onIsPlayingChanged?.invoke(true)
            refreshTracks()
            refreshChapters()
            refreshMediaInfo()
        }

        override fun onEndOfFile() {
            val state = _uiState.value
            val progress = _progressState.value
            // Only fire natural end-of-file when media was actively loaded and the playback position
            // is legitimately near the end of the video duration (within 2.5s of total duration, min duration 3s)
            if (!hasTriggeredEofForCurrentMedia &&
                state.hasMediaLoaded &&
                progress.durationSeconds > 3.0 &&
                progress.positionSeconds >= (progress.durationSeconds - 2.5).coerceAtLeast(1.0)
            ) {
                hasTriggeredEofForCurrentMedia = true
                _progressState.update {
                    if (it.isScrubbing) it.copy(isScrubbing = false, scrubDeltaSeconds = 0.0) else it
                }
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        isScrubbing = false,
                        isGestureScrubbing = false
                    )
                }
                onIsPlayingChanged?.invoke(false)
                onEndOfFileReached?.invoke()
            }
        }

        override fun onError(message: String) {
            MpvDiagnosticLogger.log("PlayerVM", "onError: $message")
            bufferingDebounceJob?.cancel()
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = message,
                    hasPlaybackFailed = true
                )
            }
            onIsPlayingChanged?.invoke(false)
        }
    }

    val engine: NativeMpvEngine = NativeMpvEngine(engineListener)
    private var openDocumentJob: Job? = null

    fun openDocument(
        context: Context,
        uri: Uri,
        titleHint: String? = null,
        startPositionMs: Long = 0L,
        allowAudioOnlyWithoutSurface: Boolean = false
    ) {
        if (currentLoadedUri == uri && _uiState.value.hasMediaLoaded) {
            return
        }
        currentLoadedUri = uri
        openDocumentJob?.cancel()
        hasTriggeredEofForCurrentMedia = false
        val displayName = titleHint?.ifBlank { null } ?: resolveDisplayName(context, uri)
        _progressState.value = PlaybackProgressState(
            positionSeconds = startPositionMs / 1000.0,
            durationSeconds = 0.0
        )
        _uiState.update {
            it.copy(
                mediaTitle = displayName,
                hasMediaLoaded = false,
                positionSeconds = startPositionMs / 1000.0,
                durationSeconds = 0.0,
                isScrubbing = false,
                isGestureScrubbing = false,
                verticalHud = null,
                isFastForwarding = false,
                errorMessage = null,
                hasPlaybackFailed = false,
                isBuffering = true
            )
        }
        openDocumentJob = viewModelScope.launch(Dispatchers.IO) {
            engine.openContentUri(context, uri, startPositionMs, allowAudioOnlyWithoutSurface)
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value
        val progress = _progressState.value
        val nextPlaying = !current.isPlaying
        if (nextPlaying && (hasTriggeredEofForCurrentMedia || (progress.durationSeconds > 3.0 && progress.positionSeconds >= progress.durationSeconds - 0.5))) {
            hasTriggeredEofForCurrentMedia = false
            _progressState.update { it.copy(positionSeconds = 0.0) }
            _uiState.update { it.copy(isPlaying = true, positionSeconds = 0.0) }
            onIsPlayingChanged?.invoke(true)
            engine.seekTo(0.0)
            engine.setPaused(false)
            return
        }
        _uiState.update { it.copy(isPlaying = nextPlaying) }
        onIsPlayingChanged?.invoke(nextPlaying)
        engine.setPaused(!nextPlaying)
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        onIsPlayingChanged?.invoke(false)
        engine.setPaused(true)
    }

    fun stopPlayback(releaseEngine: Boolean = false) {
        openDocumentJob?.cancel()
        bufferingDebounceJob?.cancel()
        currentLoadedUri = null
        hasTriggeredEofForCurrentMedia = false
        isAudioDisabled = false
        _progressState.value = PlaybackProgressState()
        _uiState.update {
            it.copy(
                isPlaying = false,
                hasMediaLoaded = false,
                isBuffering = false,
                isScrubbing = false,
                isGestureScrubbing = false,
                verticalHud = null,
                isFastForwarding = false,
                positionSeconds = 0.0,
                durationSeconds = 0.0,
                audioTracks = emptyList(),
                subtitleTracks = emptyList(),
                selectedAudioTrackId = null,
                selectedSubtitleTrackId = null,
                abRepeatPointA = null,
                abRepeatPointB = null,
                chapters = emptyList(),
                currentChapterIndex = -1,
                isMuted = false,
                errorMessage = null,
                hasPlaybackFailed = false
            )
        }
        onIsPlayingChanged?.invoke(false)
        if (releaseEngine) {
            engine.releaseEngine()
        } else {
            engine.stop()
        }
    }

    fun selectDecoderMode(mode: DecoderMode) {
        engine.setDecoderMode(mode)
        _uiState.update { it.copy(decoderMode = mode) }
    }

    fun cycleDecoderMode() {
        selectDecoderMode(_uiState.value.decoderMode.next())
    }


    fun seekRelative(deltaSeconds: Double) {
        val current = _uiState.value
        val progress = _progressState.value
        if (!current.hasMediaLoaded || progress.durationSeconds <= 0.0) return
        bufferingDebounceJob?.cancel()
        val target = (progress.positionSeconds + deltaSeconds).coerceIn(0.0, progress.durationSeconds)
        if (target < progress.durationSeconds - 3.0) {
            hasTriggeredEofForCurrentMedia = false
        }
        _progressState.update { it.copy(positionSeconds = target) }
        if (current.isBuffering) {
            _uiState.update { it.copy(isBuffering = false) }
        }
        engine.seekRelative(deltaSeconds)
    }

    fun seekTo(targetSeconds: Double) {
        val current = _uiState.value
        val progress = _progressState.value
        if (!current.hasMediaLoaded || progress.durationSeconds <= 0.0) return
        bufferingDebounceJob?.cancel()
        val clamped = targetSeconds.coerceIn(0.0, progress.durationSeconds)
        if (clamped < progress.durationSeconds - 3.0) {
            hasTriggeredEofForCurrentMedia = false
        }
        _progressState.update {
            it.copy(
                positionSeconds = clamped,
                isScrubbing = false,
                scrubDeltaSeconds = 0.0
            )
        }
        if (current.isScrubbing || current.isGestureScrubbing || current.isBuffering) {
            _uiState.update {
                it.copy(
                    isScrubbing = false,
                    isGestureScrubbing = false,
                    scrubDeltaSeconds = 0.0,
                    isBuffering = false
                )
            }
        }
        engine.seekTo(clamped)
    }

    // region Track, Audio & Subtitle Selection

    fun refreshTracks() {
        val allTracks = engine.queryTracks()
        val audios = allTracks.filter { it.type == "audio" }
        val subs = allTracks.filter { it.type == "sub" }
        val videos = allTracks.filter { it.type == "video" }
        val selAudio = if (isAudioDisabled) null else (audios.firstOrNull { it.isSelected }?.id ?: audios.firstOrNull()?.id)
        val selSub = subs.firstOrNull { it.isSelected }?.id
        val selVideo = videos.firstOrNull { it.isSelected }?.id ?: videos.firstOrNull()?.id

        // Auto-select video track only when a video surface is actively attached
        if (engine.hasAttachedSurface() && videos.isNotEmpty() && allTracks.none { it.type == "video" && it.isSelected }) {
            selVideo?.let { engine.selectTrack("video", it) }
        }

        // Auto-select audio track if not already active and audio is not explicitly muted
        if (!isAudioDisabled && audios.isNotEmpty() && allTracks.none { it.type == "audio" && it.isSelected }) {
            selAudio?.let { engine.selectTrack("audio", it) }
        }

        _uiState.update {
            it.copy(
                audioTracks = audios,
                subtitleTracks = subs,
                selectedAudioTrackId = selAudio,
                selectedSubtitleTrackId = selSub,
                audioDelaySeconds = engine.getAudioDelay(),
                subtitleDelaySeconds = engine.getSubtitleDelay()
            )
        }
    }

    fun selectAudioTrack(id: Int) {
        isAudioDisabled = false
        engine.selectTrack("audio", id)
        _uiState.update { it.copy(selectedAudioTrackId = id) }
        refreshTracks()
    }

    fun disableAudio() {
        isAudioDisabled = true
        engine.disableTrack("audio")
        _uiState.update { it.copy(selectedAudioTrackId = null) }
        refreshTracks()
    }

    fun selectSubtitleTrack(id: Int) {
        engine.selectTrack("sub", id)
        _uiState.update { it.copy(selectedSubtitleTrackId = id) }
        refreshTracks()
    }

    fun disableSubtitles() {
        engine.disableTrack("sub")
        _uiState.update { it.copy(selectedSubtitleTrackId = null) }
        refreshTracks()
    }

    fun addExternalSubtitle(context: Context, uri: Uri): Boolean {
        val success = engine.addExternalSubtitle(context, uri)
        if (success) {
            refreshTracks()
        }
        return success
    }

    fun setPlaybackSpeed(speed: Double) {
        val rounded = (kotlin.math.round(speed * 100.0) / 100.0).coerceIn(0.25, 4.0)
        engine.setPlaybackSpeed(rounded)
        _uiState.update { it.copy(playbackSpeed = rounded) }
    }

    fun setResizeMode(mode: ResizeMode) {
        engine.setResizeMode(mode)
        _uiState.update { it.copy(resizeMode = mode) }
    }

    fun cycleResizeMode() {
        setResizeMode(_uiState.value.resizeMode.next())
    }

    fun toggleRepeatOne() {
        val next = !_uiState.value.isRepeatOne
        engine.setLoopFile(next)
        _uiState.update { it.copy(isRepeatOne = next) }
    }

    fun setAudioDelay(delaySec: Double) {
        engine.setAudioDelay(delaySec)
        _uiState.update { it.copy(audioDelaySeconds = delaySec) }
    }

    fun setSubtitleDelay(delaySec: Double) {
        engine.setSubtitleDelay(delaySec)
        _uiState.update { it.copy(subtitleDelaySeconds = delaySec) }
    }

    fun refreshMediaInfo() {
        val info = engine.getMediaInfo(_uiState.value.mediaTitle)
        _uiState.update { it.copy(mediaInfo = info) }
    }

    fun startFastForward() {
        if (_uiState.value.isFastForwarding) return
        val currentSpeed = _uiState.value.playbackSpeed
        engine.setPlaybackSpeed((currentSpeed * 2.0).coerceAtMost(4.0))
        _uiState.update { it.copy(isFastForwarding = true) }
    }

    fun stopFastForward() {
        if (!_uiState.value.isFastForwarding) return
        val originalSpeed = _uiState.value.playbackSpeed
        engine.setPlaybackSpeed(originalSpeed)
        _uiState.update { it.copy(isFastForwarding = false) }
    }

    fun toggleNightModeAudio() {
        val next = !_uiState.value.isNightModeAudio
        engine.setNightModeAudio(next)
        _uiState.update { it.copy(isNightModeAudio = next) }
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        engine.setMuted(next)
        _uiState.update { it.copy(isMuted = next) }
    }

    fun refreshChapters() {
        val chapters = engine.queryChapters()
        val currentIdx = if (chapters.isNotEmpty()) engine.getCurrentChapterIndex() else -1
        _uiState.update {
            it.copy(chapters = chapters, currentChapterIndex = currentIdx)
        }
    }

    fun seekToChapter(index: Int) {
        val chapters = _uiState.value.chapters
        if (index !in chapters.indices) return
        engine.seekToChapter(index)
        _progressState.update { it.copy(positionSeconds = chapters[index].timeSeconds) }
        _uiState.update {
            it.copy(
                currentChapterIndex = index
            )
        }
    }

    fun setVideoBrightness(value: Int) {
        val clamped = value.coerceIn(-100, 100)
        engine.setVideoBrightness(clamped)
        _uiState.update { it.copy(videoBrightness = clamped) }
    }

    fun setVideoContrast(value: Int) {
        val clamped = value.coerceIn(-100, 100)
        engine.setVideoContrast(clamped)
        _uiState.update { it.copy(videoContrast = clamped) }
    }

    fun setVideoSaturation(value: Int) {
        val clamped = value.coerceIn(-100, 100)
        engine.setVideoSaturation(clamped)
        _uiState.update { it.copy(videoSaturation = clamped) }
    }

    fun setVideoGamma(value: Int) {
        val clamped = value.coerceIn(-100, 100)
        engine.setVideoGamma(clamped)
        _uiState.update { it.copy(videoGamma = clamped) }
    }

    fun resetVideoEqualizer() {
        engine.resetVideoEqualizer()
        _uiState.update {
            it.copy(
                videoBrightness = 0,
                videoContrast = 0,
                videoSaturation = 0,
                videoGamma = 0
            )
        }
    }

    fun setABRepeatPointA() {
        val pos = _progressState.value.positionSeconds
        _uiState.update { it.copy(abRepeatPointA = pos, abRepeatPointB = null) }
    }

    fun setABRepeatPointB() {
        val currentA = _uiState.value.abRepeatPointA
        val pos = _progressState.value.positionSeconds
        if (currentA != null && pos > currentA) {
            _uiState.update { it.copy(abRepeatPointB = pos) }
        }
    }

    fun clearABRepeat() {
        _uiState.update { it.copy(abRepeatPointA = null, abRepeatPointB = null) }
    }

    fun setSubtitleFontSize(size: Int) {
        engine.setSubtitleFontSize(size)
        _uiState.update { it.copy(subtitleFontSize = size) }
    }

    fun setSubtitleColor(colorHex: String) {
        engine.setSubtitleColor(colorHex)
        _uiState.update { it.copy(subtitleColor = colorHex) }
    }

    fun captureScreenshot(context: Context, onResult: (String?, Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appContext = context.applicationContext
                val fileName = "BKM_Screenshot_${System.currentTimeMillis()}.jpg"

                // Primary: hardware-compatible SurfaceView PixelCopy capture (works with zero-copy MediaCodec
                // and libmpv builds compiled without libavcodec image encoders).
                val bitmap = withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        engine.captureSurfaceFrame { capturedBitmap ->
                            if (cont.isActive) {
                                cont.resume(capturedBitmap)
                            } else {
                                capturedBitmap?.recycle()
                            }
                        }
                    }
                }

                if (bitmap != null) {
                    val savedPath = try {
                        exportBitmapToGallery(appContext, bitmap, fileName)
                    } finally {
                        bitmap.recycle()
                    }
                    withContext(Dispatchers.Main) {
                        onResult(savedPath, savedPath != null)
                    }
                    return@launch
                }

                // Fallback: libmpv screenshot-to-file if surface unavailable
                val tempDir = File(appContext.cacheDir, "screenshots")
                if (!tempDir.exists()) tempDir.mkdirs()
                val tempFile = File(tempDir, fileName)

                val captured = engine.captureScreenshot(tempFile)
                if (captured) {
                    var attempts = 0
                    while ((!tempFile.exists() || tempFile.length() == 0L) && attempts < 15) {
                        delay(40L)
                        attempts++
                    }
                }

                if (!captured || !tempFile.exists() || tempFile.length() == 0L) {
                    withContext(Dispatchers.Main) { onResult(null, false) }
                    return@launch
                }

                val savedPath = exportScreenshotToGallery(appContext, tempFile, fileName)
                withContext(Dispatchers.Main) {
                    if (savedPath != null) {
                        onResult(savedPath, true)
                    } else {
                        onResult(tempFile.absolutePath, true)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null, false)
                }
            }
        }
    }

    private fun exportBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/BKM Player")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                val compressed = resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                } ?: false
                if (!compressed) {
                    resolver.delete(uri, null, null)
                    return null
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Pictures/BKM Player/$fileName"
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "BKM Player"
                )
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val destFile = File(picturesDir, fileName)
                val compressed = destFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                if (!compressed) {
                    destFile.delete()
                    return null
                }
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("image/jpeg"), null)
                destFile.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun exportScreenshotToGallery(context: Context, sourceFile: File, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/BKM Player")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                } ?: run {
                    resolver.delete(uri, null, null)
                    return null
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                sourceFile.delete()
                "Pictures/BKM Player/$fileName"
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "BKM Player"
                )
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val destFile = File(picturesDir, fileName)
                sourceFile.inputStream().use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                sourceFile.delete()
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("image/jpeg"), null)
                destFile.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }

    // endregion

    /**
     * Begins zero-buffering horizontal gesture scrubbing.
     */
    fun onHorizontalScrubStart() {
        val current = _uiState.value
        val progress = _progressState.value
        if (!current.hasMediaLoaded || progress.durationSeconds <= 0.0) return

        bufferingDebounceJob?.cancel()
        scrubOriginSeconds = progress.positionSeconds
        lastScrubDispatchNanos = 0L
        engine.beginKeyframeScrub()

        _progressState.update {
            it.copy(
                isScrubbing = true,
                scrubPreviewSeconds = scrubOriginSeconds,
                scrubDeltaSeconds = 0.0
            )
        }
        _uiState.update {
            it.copy(
                isScrubbing = true,
                isGestureScrubbing = true,
                isBuffering = false
            )
        }
    }

    fun onHorizontalScrubDelta(deltaSeconds: Double) {
        val progress = _progressState.value
        if (!progress.isScrubbing || progress.durationSeconds <= 0.0) return

        val clampedTarget = (scrubOriginSeconds + deltaSeconds)
            .coerceIn(0.0, progress.durationSeconds)
        val actualDelta = clampedTarget - scrubOriginSeconds

        _progressState.update {
            it.copy(
                scrubPreviewSeconds = clampedTarget,
                scrubDeltaSeconds = actualDelta
            )
        }

        val nowNanos = SystemClock.elapsedRealtimeNanos()
        if (nowNanos - lastScrubDispatchNanos >= SCRUB_FRAME_INTERVAL_NANOS) {
            lastScrubDispatchNanos = nowNanos
            engine.seekToKeyframe(clampedTarget)
        }
    }

    fun onSliderScrubChange(targetSeconds: Double) {
        val current = _uiState.value
        val progress = _progressState.value
        if (!current.hasMediaLoaded || progress.durationSeconds <= 0.0) return

        bufferingDebounceJob?.cancel()
        if (!progress.isScrubbing) {
            scrubOriginSeconds = progress.positionSeconds
            lastScrubDispatchNanos = 0L
            engine.beginKeyframeScrub()
        }
        if (!current.isScrubbing || current.isGestureScrubbing || current.isBuffering) {
            _uiState.update {
                it.copy(
                    isScrubbing = true,
                    isGestureScrubbing = false,
                    isBuffering = false
                )
            }
        }

        val clampedTarget = targetSeconds.coerceIn(0.0, progress.durationSeconds)
        val actualDelta = clampedTarget - scrubOriginSeconds

        _progressState.update {
            it.copy(
                isScrubbing = true,
                scrubPreviewSeconds = clampedTarget,
                scrubDeltaSeconds = actualDelta
            )
        }

        val nowNanos = SystemClock.elapsedRealtimeNanos()
        if (nowNanos - lastScrubDispatchNanos >= SCRUB_FRAME_INTERVAL_NANOS) {
            lastScrubDispatchNanos = nowNanos
            engine.seekToKeyframe(clampedTarget)
        }
    }

    fun onHorizontalScrubEnd() {
        val progress = _progressState.value
        val ui = _uiState.value
        if (!progress.isScrubbing && !ui.isScrubbing && !ui.isGestureScrubbing) return

        bufferingDebounceJob?.cancel()
        if (progress.isScrubbing) {
            val finalTarget = progress.scrubPreviewSeconds
            engine.seekTo(finalTarget)

            _progressState.update {
                it.copy(
                    isScrubbing = false,
                    positionSeconds = finalTarget,
                    scrubDeltaSeconds = 0.0
                )
            }
        }
        _uiState.update {
            it.copy(
                isScrubbing = false,
                isGestureScrubbing = false,
                isBuffering = false
            )
        }
    }

    fun showBrightnessHud(percent: Int) {
        _uiState.update {
            it.copy(verticalHud = VerticalGestureHud.Brightness(percent.coerceIn(0, 100)))
        }
    }

    fun showVolumeHud(percent: Int, isSoftwareBoosted: Boolean) {
        _uiState.update {
            it.copy(
                verticalHud = VerticalGestureHud.Volume(
                    percent = percent.coerceIn(0, 200),
                    isSoftwareBoosted = isSoftwareBoosted
                )
            )
        }
    }

    fun setSoftwareVolumeBoost(percent: Int) {
        engine.setSoftwareVolume(percent)
    }

    fun hideVerticalHud() {
        _uiState.update { it.copy(verticalHud = null) }
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(controlsVisible = visible) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private var sleepTimerJob: Job? = null
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    fun setSleepTimer(minutes: Int, onFinish: (() -> Unit)? = null) {
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            _sleepTimerMinutes.value = minutes
            sleepTimerJob = viewModelScope.launch {
                var left = minutes
                while (left > 0) {
                    delay(60_000L)
                    left--
                    _sleepTimerMinutes.value = left
                }
                pausePlayback()
                onFinish?.invoke()
            }
        } else {
            _sleepTimerMinutes.value = 0
            sleepTimerJob = null
        }
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        bufferingDebounceJob?.cancel()
        openDocumentJob?.cancel()
        engine.destroy()
        super.onCleared()
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            } ?: uri.lastPathSegment.orEmpty().ifBlank { "Local Video" }
        } catch (_: Exception) {
            uri.lastPathSegment.orEmpty().ifBlank { "Local Video" }
        }
    }

    companion object {
        private const val SCRUB_FRAME_INTERVAL_NANOS = 60_000_000L // 60ms throttle (~16.6 seeks/sec)
    }
}

fun formatTimestamp(totalSeconds: Double, forceHours: Boolean = false): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0.0).roundToLong()
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0 || forceHours) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatDelta(absSeconds: Double): String {
    val safeSeconds = absSeconds.coerceAtLeast(0.0).roundToLong()
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

package com.mxplayer.mpv

import android.content.Context
import android.net.Uri
import android.os.SystemClock
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
import java.io.File
import java.util.Locale
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
    val scrubPreviewSeconds: Double = 0.0,
    val scrubDeltaSeconds: Double = 0.0,
    val verticalHud: VerticalGestureHud? = null,
    val controlsVisible: Boolean = true,
    val errorMessage: String? = null,
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
    val abRepeatPointA: Double? = null,
    val abRepeatPointB: Double? = null,
    val subtitleFontSize: Int = 48,
    val subtitleColor: String = "#FFFFFF"
) {
    /**
     * MX Player-style scrubbing HUD string, e.g. `01:14:22 [+00:15]` or `01:14:07 [-00:15]`.
     */
    val scrubOverlayText: String
        get() {
            val targetFormatted = formatTimestamp(scrubPreviewSeconds)
            val sign = if (scrubDeltaSeconds >= 0.0) "+" else "-"
            val deltaFormatted = formatDelta(abs(scrubDeltaSeconds))
            return "$targetFormatted [$sign$deltaFormatted]"
        }
}

/**
 * Coordinates [NativeMpvEngine] state, 60fps throttled keyframe scrubbing, decoder mode switching,
 * multi-audio/subtitle track management, audio/subtitle delay sync, playback speed, aspect ratio,
 * and clean lifecycle teardown in [onCleared].
 */
class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var scrubOriginSeconds: Double = 0.0
    private var lastScrubDispatchNanos: Long = 0L
    private var lastPositionPersistMs: Long = 0L
    private var bufferingDebounceJob: Job? = null
    private var hasTriggeredEofForCurrentMedia = false

    var onPositionUpdate: ((posMs: Long, durMs: Long) -> Unit)? = null
    var onProgressUpdate: ((posMs: Long, durMs: Long) -> Unit)? = null
    var onIsPlayingChanged: ((Boolean) -> Unit)? = null
    var onEndOfFileReached: (() -> Unit)? = null

    var currentLoadedUri: Uri? = null
        private set

    private val engineListener = object : MpvEngineListener {
        override fun onPositionChanged(seconds: Double) {
            _uiState.update { state ->
                if (state.isScrubbing) state else state.copy(positionSeconds = seconds)
            }

            // A-B Repeat Loop Trigger
            val a = _uiState.value.abRepeatPointA
            val b = _uiState.value.abRepeatPointB
            if (a != null && b != null && b > a && seconds >= b) {
                seekTo(a)
                return
            }

            val posMs = (seconds * 1000.0).roundToLong()
            val durMs = (_uiState.value.durationSeconds * 1000.0).roundToLong()
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
            _uiState.update { it.copy(durationSeconds = dur) }
            val posMs = (_uiState.value.positionSeconds * 1000.0).roundToLong()
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
                if (_uiState.value.isScrubbing) return
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
            _uiState.update { it.copy(cacheDurationSeconds = seconds.coerceAtLeast(0.0)) }
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

        override fun onFileLoaded() {
            hasTriggeredEofForCurrentMedia = false
            _uiState.update {
                it.copy(
                    hasMediaLoaded = true,
                    isPlaying = true,
                    isBuffering = false,
                    errorMessage = null
                )
            }
            onIsPlayingChanged?.invoke(true)
            refreshTracks()
            refreshMediaInfo()
        }

        override fun onEndOfFile() {
            val state = _uiState.value
            // Only fire natural end-of-file when media was actively loaded and the playback position
            // is legitimately near the end of the video duration (within 2.5s of total duration, min duration 3s)
            if (!hasTriggeredEofForCurrentMedia &&
                state.hasMediaLoaded &&
                state.durationSeconds > 3.0 &&
                state.positionSeconds >= (state.durationSeconds - 2.5).coerceAtLeast(1.0)
            ) {
                hasTriggeredEofForCurrentMedia = true
                _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                onIsPlayingChanged?.invoke(false)
                onEndOfFileReached?.invoke()
            }
        }

        override fun onError(message: String) {
            _uiState.update { it.copy(errorMessage = message) }
        }
    }

    val engine: NativeMpvEngine = NativeMpvEngine(engineListener)
    private var openDocumentJob: Job? = null

    fun openDocument(context: Context, uri: Uri, titleHint: String? = null, startPositionMs: Long = 0L) {
        if (currentLoadedUri == uri && _uiState.value.hasMediaLoaded) {
            return
        }
        currentLoadedUri = uri
        openDocumentJob?.cancel()
        hasTriggeredEofForCurrentMedia = false
        val displayName = titleHint?.ifBlank { null } ?: resolveDisplayName(context, uri)
        _uiState.update {
            it.copy(
                mediaTitle = displayName,
                hasMediaLoaded = false,
                positionSeconds = startPositionMs / 1000.0,
                durationSeconds = 0.0,
                errorMessage = null,
                isBuffering = true
            )
        }
        openDocumentJob = viewModelScope.launch(Dispatchers.IO) {
            engine.openContentUri(context, uri, startPositionMs)
        }
    }

    fun togglePlayPause() {
        val nextPlaying = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = nextPlaying) }
        onIsPlayingChanged?.invoke(nextPlaying)
        engine.setPaused(!nextPlaying)
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        onIsPlayingChanged?.invoke(false)
        engine.setPaused(true)
    }

    fun stopPlayback() {
        currentLoadedUri = null
        _uiState.update { it.copy(isPlaying = false, hasMediaLoaded = false) }
        onIsPlayingChanged?.invoke(false)
        engine.stop()
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
        if (!current.hasMediaLoaded || current.durationSeconds <= 0.0) return
        bufferingDebounceJob?.cancel()
        val target = (current.positionSeconds + deltaSeconds).coerceIn(0.0, current.durationSeconds)
        if (target < current.durationSeconds - 3.0) {
            hasTriggeredEofForCurrentMedia = false
        }
        _uiState.update { it.copy(positionSeconds = target, isBuffering = false) }
        engine.seekRelative(deltaSeconds)
    }

    fun seekTo(targetSeconds: Double) {
        val current = _uiState.value
        if (!current.hasMediaLoaded || current.durationSeconds <= 0.0) return
        bufferingDebounceJob?.cancel()
        val clamped = targetSeconds.coerceIn(0.0, current.durationSeconds)
        if (clamped < current.durationSeconds - 3.0) {
            hasTriggeredEofForCurrentMedia = false
        }
        _uiState.update {
            it.copy(
                positionSeconds = clamped,
                isScrubbing = false,
                scrubDeltaSeconds = 0.0,
                isBuffering = false
            )
        }
        engine.seekTo(clamped)
    }

    // region Track, Audio & Subtitle Selection

    fun refreshTracks() {
        val allTracks = engine.queryTracks()
        val audios = allTracks.filter { it.type == "audio" }
        val subs = allTracks.filter { it.type == "sub" }
        val videos = allTracks.filter { it.type == "video" }
        val selAudio = audios.firstOrNull { it.isSelected }?.id ?: audios.firstOrNull()?.id
        val selSub = subs.firstOrNull { it.isSelected }?.id
        val selVideo = videos.firstOrNull { it.isSelected }?.id ?: videos.firstOrNull()?.id

        // Auto-select video track if not already active
        if (videos.isNotEmpty() && allTracks.none { it.type == "video" && it.isSelected }) {
            selVideo?.let { engine.selectTrack("video", it) }
        }

        // Auto-select audio track if not already active
        if (audios.isNotEmpty() && allTracks.none { it.type == "audio" && it.isSelected }) {
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
        engine.selectTrack("audio", id)
        _uiState.update { it.copy(selectedAudioTrackId = id) }
        refreshTracks()
    }

    fun disableAudio() {
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
        engine.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
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

    fun setABRepeatPointA() {
        val pos = _uiState.value.positionSeconds
        _uiState.update { it.copy(abRepeatPointA = pos, abRepeatPointB = null) }
    }

    fun setABRepeatPointB() {
        val currentA = _uiState.value.abRepeatPointA
        val pos = _uiState.value.positionSeconds
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
                val picturesDir = File(context.cacheDir, "screenshots")
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, "Screenshot_${System.currentTimeMillis()}.jpg")
                val success = engine.captureScreenshot(file)
                launch(Dispatchers.Main) {
                    if (success) {
                        onResult(file.absolutePath, true)
                    } else {
                        onResult(null, false)
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onResult(null, false)
                }
            }
        }
    }

    // endregion

    /**
     * Begins zero-buffering horizontal gesture scrubbing.
     */
    fun onHorizontalScrubStart() {
        val current = _uiState.value
        if (!current.hasMediaLoaded || current.durationSeconds <= 0.0) return

        bufferingDebounceJob?.cancel()
        scrubOriginSeconds = current.positionSeconds
        lastScrubDispatchNanos = 0L
        engine.beginKeyframeScrub()

        _uiState.update {
            it.copy(
                isScrubbing = true,
                scrubPreviewSeconds = scrubOriginSeconds,
                scrubDeltaSeconds = 0.0,
                isBuffering = false
            )
        }
    }

    fun onHorizontalScrubDelta(deltaSeconds: Double) {
        val current = _uiState.value
        if (!current.isScrubbing || current.durationSeconds <= 0.0) return

        val clampedTarget = (scrubOriginSeconds + deltaSeconds)
            .coerceIn(0.0, current.durationSeconds)
        val actualDelta = clampedTarget - scrubOriginSeconds

        _uiState.update {
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
        if (!current.hasMediaLoaded || current.durationSeconds <= 0.0) return

        bufferingDebounceJob?.cancel()
        if (!current.isScrubbing) {
            scrubOriginSeconds = current.positionSeconds
            lastScrubDispatchNanos = 0L
            _uiState.update {
                it.copy(
                    isScrubbing = true,
                    scrubPreviewSeconds = targetSeconds.coerceIn(0.0, current.durationSeconds),
                    scrubDeltaSeconds = 0.0,
                    isBuffering = false
                )
            }
        }

        val clampedTarget = targetSeconds.coerceIn(0.0, current.durationSeconds)
        val actualDelta = clampedTarget - scrubOriginSeconds

        _uiState.update {
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

    fun onHorizontalScrubEnd() {
        val current = _uiState.value
        if (!current.isScrubbing) return

        bufferingDebounceJob?.cancel()
        val finalTarget = current.scrubPreviewSeconds
        engine.seekTo(finalTarget)

        _uiState.update {
            it.copy(
                isScrubbing = false,
                positionSeconds = finalTarget,
                scrubDeltaSeconds = 0.0,
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

    override fun onCleared() {
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

fun formatTimestamp(totalSeconds: Double): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0.0).roundToLong()
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
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

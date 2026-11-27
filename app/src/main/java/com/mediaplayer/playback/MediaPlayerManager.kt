package com.mediaplayer.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mediaplayer.MainActivity
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.player.UniversalMediaPlaybackService
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MediaPlayerManager(
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    private var _player: ExoPlayer? = null
    val player: ExoPlayer
        get() {
            ensureInitialized()
            return _player!!
        }

    private var _mediaSession: MediaSession? = null
    val mediaSession: MediaSession
        get() {
            ensureInitialized()
            return _mediaSession!!
        }

    val isPlayerInitialized: Boolean
        get() = _player != null

    private var playerListener: Player.Listener? = null
    private var scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressPollJob: Job? = null

    private val _currentMediaItem = MutableStateFlow<MediaItemModel?>(null)
    val currentMediaItem: StateFlow<MediaItemModel?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(value = false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    @Suppress("unused")
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    data class AudioStreamInfo(
        val codec: String = "",
        val sampleRateHz: Int = 0,
        val channelCount: Int = 0,
        val bitrateBps: Int = 0
    )

    private val _audioStreamInfo = MutableStateFlow<AudioStreamInfo?>(null)
    val audioStreamInfo: StateFlow<AudioStreamInfo?> = _audioStreamInfo.asStateFlow()

    @Suppress("unused")
    fun retry() {
        _playbackError.value = null
        player.prepare()
        player.play()
    }

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItemModel>>(emptyList())
    val playlist: StateFlow<List<MediaItemModel>> = _playlist.asStateFlow()

    fun updateMediaItem(updated: MediaItemModel) {
        if (_currentMediaItem.value?.id == updated.id || _currentMediaItem.value?.uri == updated.uri) {
            _currentMediaItem.value = updated
        }
        _playlist.value = _playlist.value.map {
            if (it.id == updated.id || it.uri == updated.uri) updated else it
        }
    }

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    val historyManager = PlaybackHistoryManager(appContext)

    private val _playbackPositions = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val playbackPositions: StateFlow<Map<Long, Long>> = _playbackPositions.asStateFlow()

    private val _playbackTimestamps = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val playbackTimestamps: StateFlow<Map<Long, Long>> = _playbackTimestamps.asStateFlow()

    private val _playedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val playedMediaIds: StateFlow<Set<Long>> = _playedMediaIds.asStateFlow()

    private var videoServiceStartJob: Job? = null

    init {
        // Load playback history asynchronously off the main thread to optimize cold startup
        scope.launch(Dispatchers.IO) {
            val positions = historyManager.getAllPositions()
            val timestamps = historyManager.getAllTimestamps()
            val playedIds = historyManager.getPlayedMediaIds().asSequence().mapNotNull { it.toLongOrNull() }.toSet()
            _playbackPositions.value = positions
            _playbackTimestamps.value = timestamps
            _playedMediaIds.value = playedIds
        }
    }

    private val _decoderMode = MutableStateFlow("FFmpeg Extension (Universal)")
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    @Suppress("unused")
    fun isVideoPlayed(mediaId: Long): Boolean {
        return _playedMediaIds.value.contains(mediaId) || historyManager.isPlayed(mediaId)
    }

    fun getSavedPosition(mediaId: Long): Long {
        return _playbackPositions.value[mediaId] ?: historyManager.getPosition(mediaId)
    }

    fun clearPlaybackHistory(mediaId: Long) {
        _playbackPositions.update { it - mediaId }
        _playbackTimestamps.update { it - mediaId }
        historyManager.clearPosition(mediaId)
    }

    fun clearAllPlaybackHistory() {
        _playbackPositions.value = emptyMap()
        _playbackTimestamps.value = emptyMap()
        historyManager.clearAllPositions()
    }

    fun setDecoderMode(mode: String) {
        _decoderMode.value = mode
    }

    private var forwardingPlayer: UniversalForwardingPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MediaPlayer:VideoPlaybackWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(30 * 60 * 1000L)
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    inner class UniversalForwardingPlayer(player: Player) : ForwardingSimpleBasePlayer(player) {
        fun notifyStateChanged() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                invalidateState()
            } else {
                Handler(Looper.getMainLooper()).post { invalidateState() }
            }
        }

        override fun getState(): State {
            val currentItem = _currentMediaItem.value
            if (currentItem != null && currentItem.isVideo) {
                val isPlaying = _isVideoPlaying.value
                val durationMs = if (_durationMs.value > 0) _durationMs.value else (if (currentItem.durationMs > 0) currentItem.durationMs else C.TIME_UNSET)
                val durationUs = if (durationMs > 0) durationMs * 1000L else C.TIME_UNSET
                val posMs = _currentPositionMs.value
                val media3Item = currentItem.toMedia3Item()

                val videoCommands = Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_PREPARE,
                        Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_FORWARD,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_METADATA,
                        Player.COMMAND_GET_TRACKS
                    )
                    .build()

                val mediaItemData = SimpleBasePlayer.MediaItemData.Builder(currentItem.id)
                    .setMediaItem(media3Item)
                    .setTracks(Tracks.EMPTY)
                    .setDurationUs(durationUs)
                    .build()

                return State.Builder()
                    .setAvailableCommands(videoCommands)
                    .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    .setPlaybackState(Player.STATE_READY)
                    .setPlaylist(listOf(mediaItemData))
                    .setCurrentMediaItemIndex(0)
                    .setContentPositionMs(
                        if (isPlaying) {
                            SimpleBasePlayer.PositionSupplier.getExtrapolating(posMs, 1f)
                        } else {
                            SimpleBasePlayer.PositionSupplier.getConstant(posMs)
                        }
                    )
                    .setPlaylistMetadata(media3Item.mediaMetadata)
                    .setIsLoading(false)
                    .setSeekBackIncrementMs(10000L)
                    .setSeekForwardIncrementMs(10000L)
                    .build()
            }
            return super.getState()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            if (_currentMediaItem.value?.isVideo == true) {
                if (playWhenReady != _isVideoPlaying.value) {
                    _isVideoPlaying.value = playWhenReady
                    _isPlaying.value = playWhenReady
                    if (playWhenReady) {
                        acquireWakeLock()
                    } else {
                        releaseWakeLock()
                    }
                    onToggleVideoPlayPause?.invoke()
                }
                invalidateState()
                return Futures.immediateVoidFuture()
            }
            return super.handleSetPlayWhenReady(playWhenReady)
        }

        override fun handlePrepare(): ListenableFuture<*> {
            if (_currentMediaItem.value?.isVideo == true) {
                return Futures.immediateVoidFuture()
            }
            return super.handlePrepare()
        }

        override fun handleStop(): ListenableFuture<*> {
            if (_currentMediaItem.value?.isVideo == true) {
                stopVideo()
                return Futures.immediateVoidFuture()
            }
            return super.handleStop()
        }

        override fun handleSeek(
            mediaItemIndex: Int,
            positionMs: Long,
            @Player.Command seekCommand: Int
        ): ListenableFuture<*> {
            if (_currentMediaItem.value?.isVideo == true) {
                when (seekCommand) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        playNextVideo()
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        playPreviousVideo()
                    }
                    Player.COMMAND_SEEK_BACK -> {
                        onSeekVideoRelative?.invoke(-10.0)
                    }
                    Player.COMMAND_SEEK_FORWARD -> {
                        onSeekVideoRelative?.invoke(10.0)
                    }
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                        if (positionMs >= 0) {
                            onSeekVideoAbsolute?.invoke(positionMs)
                        }
                    }
                }
                invalidateState()
                return Futures.immediateVoidFuture()
            }
            return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
        }
    }

    fun ensureInitialized() {
        if (_player != null && _mediaSession != null) return
        synchronized(this) {
            if (_player != null && _mediaSession != null) return
            initialize()
        }
    }

    fun initialize() {
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + Job())
        }

        val exo = UniversalMediaPlaybackService.createExoPlayer(appContext)
        _player = exo

        val universalPlayer = UniversalForwardingPlayer(exo)
        forwardingPlayer = universalPlayer

        val sessionActivityIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                if (_currentMediaItem.value?.isVideo == true) {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as? android.view.KeyEvent
                    }
                    if ((keyEvent != null) && (keyEvent.action == android.view.KeyEvent.ACTION_DOWN)) {
                        when (keyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE,
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            android.view.KeyEvent.KEYCODE_HEADSETHOOK
                            -> {
                                playPause()
                                return true
                            }
                            android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                playNextVideo()
                                return true
                            }
                            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                playPreviousVideo()
                                return true
                            }
                        }
                    }
                    return true // Block ExoPlayer from playing music during video playback
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        _mediaSession = MediaSession.Builder(appContext, universalPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        playerListener?.let { player.removeListener(it) }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MediaPlayerManager", "ExoPlayer error: ${error.errorCodeName} (${error.errorCode})", error)
                com.mediaplayer.util.DiagnosticLogManager.recordEvent(
                    "ExoPlayer",
                    "${error.errorCodeName} (${error.errorCode}): ${error.cause?.javaClass?.simpleName ?: error.message}"
                )
                val currentUrl = _currentMediaItem.value?.uri?.toString()
                val msg = when (error.errorCode) {
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Network connection timed out. Check your internet connection or stream URL."
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Server returned an HTTP error (${(error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: "error"}). Stream may be offline or geo-restricted."
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        if (com.bkm.player.mpv.NativeMpvEngine.isLikelyWebPageUrl(currentUrl)) {
                            "This looks like a web page link, not a direct audio stream. Paste a direct media URL (.mp3, .flac, .aac, .m3u8, or rtsp://)."
                        } else {
                            "Stream manifest or container format is invalid or unsupported."
                        }
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                        "Unable to connect to stream server. Please verify the URL."
                    else -> error.localizedMessage ?: "Playback error encountered."
                }
                _playbackError.value = msg
                _isPlaying.value = false
                forwardingPlayer?.notifyStateChanged()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (_currentMediaItem.value?.isVideo == true) return
                _playbackState.value = state
                _durationMs.value = if (player.duration > 0) player.duration else 0L
                if (state == Player.STATE_READY) {
                    _playbackError.value = null
                    if (player.isPlaying) {
                        startProgressPolling()
                    }
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    stopProgressPolling()
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (_currentMediaItem.value?.isVideo == true) return
                _isPlaying.value = playing
                if (playing) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // If currently playing video or no audio item loaded, ignore ExoPlayer transition
                if (_currentMediaItem.value?.isVideo == true || mediaItem == null) {
                    return
                }
                val currentMedia = _playlist.value.getOrNull(player.currentMediaItemIndex)
                _currentMediaItem.value = currentMedia
                _currentIndex.value = player.currentMediaItemIndex
                _durationMs.value = if (player.duration > 0) player.duration else 0L
                _currentPositionMs.value = 0L
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (_currentMediaItem.value?.isVideo == true) return
                _currentPositionMs.value = newPosition.positionMs
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (_currentMediaItem.value?.isVideo == true) return
                var selectedAudioFormat: androidx.media3.common.Format? = null
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                selectedAudioFormat = group.getTrackFormat(i)
                                break
                            }
                        }
                    }
                    if (selectedAudioFormat != null) break
                }
                if (selectedAudioFormat != null) {
                    val mime = selectedAudioFormat.sampleMimeType.orEmpty().lowercase()
                    val codecLabel = when {
                        mime.contains("opus") -> "Opus"
                        mime.contains("flac") -> "FLAC"
                        mime.contains("alac") -> "ALAC"
                        mime.contains("vorbis") -> "Vorbis"
                        mime.contains("mp4a") || mime.contains("aac") -> "AAC"
                        mime.contains("mpeg") || mime.contains("mp3") -> "MP3"
                        mime.contains("raw") || mime.contains("pcm") -> "PCM"
                        mime.contains("ac3") -> "AC3"
                        mime.contains("eac3") -> "E-AC3"
                        mime.contains("dts") -> "DTS"
                        else -> selectedAudioFormat.codecs?.uppercase()?.substringBefore('.') ?: ""
                    }
                    _audioStreamInfo.value = AudioStreamInfo(
                        codec = codecLabel,
                        sampleRateHz = selectedAudioFormat.sampleRate.takeIf { it > 0 } ?: 0,
                        channelCount = selectedAudioFormat.channelCount.takeIf { it > 0 } ?: 0,
                        bitrateBps = selectedAudioFormat.bitrate.takeIf { it > 0 } ?: 0
                    )
                }
            }
        }
        playerListener = listener
        player.addListener(listener)
    }

    private var videoStartPositionOverride: Long? = null

    fun consumeVideoStartPositionOverride(): Long? {
        val pos = videoStartPositionOverride
        videoStartPositionOverride = null
        return pos
    }

    fun playMedia(item: MediaItemModel, playlist: List<MediaItemModel> = listOf(item), startPositionMs: Long? = null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                playMedia(item, playlist, startPositionMs)
            }
            return
        }

        val index = playlist.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        _playlist.value = playlist
        _currentIndex.value = index
        _currentMediaItem.value = item

        // Mark as played in persistent history immediately
        historyManager.markAsPlayed(item.id)
        historyManager.saveLastPlayedMediaId(item.id)
        _playedMediaIds.update { it + item.id }

        if (item.isVideo) {
            _audioStreamInfo.value = null
            // Video playback is handled 100% by C++ Core (libmpv HW+).
            videoStartPositionOverride = startPositionMs
            val initialPos = startPositionMs ?: run {
                val saved = historyManager.getPosition(item.id)
                if (saved > 3000L && (item.durationMs <= 0L || saved < item.durationMs - 5000L)) saved else 0L
            }
            _durationMs.value = if (item.durationMs > 0) item.durationMs else 0L
            currentNotifiedDurationMs = _durationMs.value
            _currentPositionMs.value = initialPos
            stopProgressPolling()
            // Stop ExoPlayer only if already initialized so opening a video never stalls the main thread
            _player?.let { p ->
                p.pause()
                p.stop()
                p.clearMediaItems()
            }
            _isVideoPlaying.value = true
            _isPlaying.value = true
            acquireWakeLock()

            // Defer service start slightly so first video frame renders smoothly without cold ExoPlayer/MediaSession stall
            videoServiceStartJob?.cancel()
            if (com.mediaplayer.util.AppSettingsManager(appContext).backgroundPlayback) {
                videoServiceStartJob = scope.launch {
                    delay(350.milliseconds)
                    if (_currentMediaItem.value?.isVideo == true && _isVideoPlaying.value) {
                        try {
                            val serviceIntent = Intent(appContext, UniversalMediaPlaybackService::class.java)
                            appContext.startService(serviceIntent)
                        } catch (_: Exception) {}
                    }
                }
            }
            forwardingPlayer?.notifyStateChanged()
            return
        }

        // Stop video playback completely to eliminate any audio overlapping
        videoServiceStartJob?.cancel()
        videoServiceStartJob = null
        releaseWakeLock()
        onStopVideo?.invoke() ?: onPauseVideo?.invoke()
        _isVideoPlaying.value = false
        _videoDimensions.value = null
        clearVideoCallbacks()

        // For audio playback: ensure UniversalMediaPlaybackService is running so notification and lockscreen controls are active.
        try {
            val serviceIntent = Intent(appContext, UniversalMediaPlaybackService::class.java)
            appContext.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isNetworkStream = item.uri.scheme?.lowercase() in listOf("http", "https", "rtsp")
        _playbackError.value = null
        com.mediaplayer.util.DiagnosticLogManager.recordEvent(
            "ExoPlayer",
            "playMedia audio scheme=${item.uri.scheme ?: "none"} mime=${item.mimeType.ifBlank { "-" }} listSize=${playlist.size}"
        )

        // Determine starting position: resume from saved position if valid.
        // For network streams without explicit start position, default to C.TIME_UNSET so live streams start at the live edge!
        val resumePos = startPositionMs ?: run {
            if (isNetworkStream) {
                C.TIME_UNSET
            } else {
                val saved = historyManager.getPosition(item.id)
                if (saved > 3000L && (item.durationMs <= 0 || saved < item.durationMs - 5000L)) {
                    saved
                } else {
                    0L
                }
            }
        }

        val mediaItems = playlist.map { it.toMedia3Item() }
        player.setMediaItems(mediaItems, index, resumePos)
        player.prepare()
        player.play()
        forwardingPlayer?.notifyStateChanged()
    }

    fun restoreLastPlayedMedia(allMedia: List<MediaItemModel>) {
        if (_currentMediaItem.value != null || allMedia.isEmpty()) return
        val lastId = historyManager.getLastPlayedMediaId() ?: return
        val item = allMedia.find { it.id == lastId } ?: return
        val savedPos = historyManager.getPosition(item.id)

        _currentMediaItem.value = item
        _playlist.value = listOf(item)
        _currentIndex.value = 0
        _currentPositionMs.value = savedPos
        _durationMs.value = if (item.durationMs > 0) item.durationMs else 0L

        if (item.isVideo) {
            _player?.pause()
            _player?.clearMediaItems()
            return
        }

        // Defer audio engine setup until the user explicitly hits play, avoiding cold-start decoder stalls
        if (_player != null) {
            try {
                val media3Item = item.toMedia3Item()
                _player?.setMediaItem(media3Item, savedPos)
                _player?.prepare()
                _player?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var onToggleVideoPlayPause: (() -> Unit)? = null
    var onPauseVideo: (() -> Unit)? = null
    var onStopVideo: (() -> Unit)? = null
    var onSeekVideoRelative: ((deltaSeconds: Double) -> Unit)? = null
    var onSeekVideoAbsolute: ((positionMs: Long) -> Unit)? = null

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private val _videoDimensions = MutableStateFlow<Pair<Int, Int>?>(null)
    val videoDimensions: StateFlow<Pair<Int, Int>?> = _videoDimensions.asStateFlow()

    private var currentNotifiedDurationMs = 0L

    fun setVideoPlaying(playing: Boolean) {
        val stateChanged = _isVideoPlaying.value != playing ||
            (_currentMediaItem.value?.isVideo == true && _isPlaying.value != playing)
        _isVideoPlaying.value = playing
        if (_currentMediaItem.value?.isVideo == true) {
            _isPlaying.value = playing
            if (playing) {
                acquireWakeLock()
            } else {
                releaseWakeLock()
            }
        }
        if (stateChanged) {
            forwardingPlayer?.notifyStateChanged()
        }
    }

    fun updateVideoPosition(positionMs: Long, durationMs: Long) {
        if (_currentMediaItem.value?.isVideo == true) {
            _currentPositionMs.value = positionMs
            if (durationMs > 0 && _durationMs.value != durationMs) {
                _durationMs.value = durationMs
                if (currentNotifiedDurationMs <= 0L) {
                    currentNotifiedDurationMs = durationMs
                    forwardingPlayer?.notifyStateChanged()
                }
            }
        }
    }

    fun setVideoDimensions(width: Int, height: Int) {
        _videoDimensions.value = Pair(width, height)
    }

    fun clearVideoCallbacks() {
        onToggleVideoPlayPause = null
        onPauseVideo = null
        onStopVideo = null
        onSeekVideoRelative = null
        onSeekVideoAbsolute = null
    }

    fun stopVideo() {
        if (_currentMediaItem.value?.isVideo == true) {
            videoServiceStartJob?.cancel()
            videoServiceStartJob = null
            releaseWakeLock()
            onStopVideo?.invoke() ?: onPauseVideo?.invoke()
            clearVideoCallbacks()
            _currentMediaItem.value = null
            _isPlaying.value = false
            _isVideoPlaying.value = false
            _videoDimensions.value = null
            currentNotifiedDurationMs = 0L
            _isPlayerExpanded.value = false
            forwardingPlayer?.notifyStateChanged()
        }
    }

    fun playPause() {
        if (_currentMediaItem.value?.isVideo == true) {
            val nextPlaying = !_isVideoPlaying.value
            _isVideoPlaying.value = nextPlaying
            _isPlaying.value = nextPlaying
            if (nextPlaying) {
                acquireWakeLock()
            } else {
                releaseWakeLock()
            }
            onToggleVideoPlayPause?.invoke()
            forwardingPlayer?.notifyStateChanged()
            return
        }
        val p = player
        if (p.playWhenReady && p.playbackState != Player.STATE_ENDED) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_currentMediaItem.value?.isVideo == true) {
            if (!_isVideoPlaying.value) {
                _isVideoPlaying.value = true
                _isPlaying.value = true
                acquireWakeLock()
                onToggleVideoPlayPause?.invoke()
                forwardingPlayer?.notifyStateChanged()
            }
            return
        }
        val p = player
        if (_isVideoPlaying.value) {
            releaseWakeLock()
            onStopVideo?.invoke() ?: onPauseVideo?.invoke()
            _isVideoPlaying.value = false
            _videoDimensions.value = null
            clearVideoCallbacks()
        }
        try {
            val serviceIntent = Intent(appContext, UniversalMediaPlaybackService::class.java)
            appContext.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _playbackError.value = null
        if (p.mediaItemCount == 0 && _currentMediaItem.value != null) {
            val item = _currentMediaItem.value!!
            val pos = _currentPositionMs.value
            val media3Item = item.toMedia3Item()
            p.setMediaItem(media3Item, pos)
        }
        if (p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) {
            p.prepare()
        }
        p.play()
        _isPlaying.value = true
        forwardingPlayer?.notifyStateChanged()
    }

    fun pause() {
        if (_currentMediaItem.value?.isVideo == true) {
            _isVideoPlaying.value = false
            _isPlaying.value = false
            releaseWakeLock()
            onPauseVideo?.invoke()
            forwardingPlayer?.notifyStateChanged()
            return
        }
        val p = _player ?: return
        stopProgressPolling()
        val pos = p.currentPosition
        val dur = p.duration
        _currentMediaItem.value?.id?.let { mediaId ->
            _playbackPositions.update { it + (mediaId to pos) }
            historyManager.savePosition(mediaId, pos, dur)
        }
        p.pause()
        _isPlaying.value = false
        forwardingPlayer?.notifyStateChanged()
    }

    fun stop() {
        stopProgressPolling()
        _player?.let { p ->
            p.stop()
            p.clearMediaItems()
        }
        _playbackError.value = null
        _currentMediaItem.value = null
        _audioStreamInfo.value = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        clearVideoCallbacks()
        forwardingPlayer?.notifyStateChanged()
    }

    @Suppress("UsePropertyAccessSyntax")
    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = safePos
        if (_currentMediaItem.value?.isVideo == true) {
            onSeekVideoAbsolute?.invoke(safePos)
            forwardingPlayer?.notifyStateChanged()
            return
        }
        player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
        player.seekTo(safePos)
    }

    @Suppress("unused", "UsePropertyAccessSyntax")
    fun seekToFast(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = safePos
        player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
        player.seekTo(safePos)
    }

    @Suppress("unused", "UsePropertyAccessSyntax")
    fun seekToExact(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = safePos
        player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
        player.seekTo(safePos)
    }

    @Suppress("unused")
    fun setVolumeBoost(level: Float) {
        // level: 0.0f .. 2.0f
        if (level <= 1.0f) {
            player.volume = level.coerceIn(0f, 1f)
        } else {
            // Soft-knee tanh saturation:
            // Smoothly compresses loudness spikes while boosting quiet dialog without digital clipping
            val boost = (level - 1.0f).toDouble()
            val gain = 1.0f + kotlin.math.tanh(boost).toFloat()
            player.volume = gain
        }
    }

    fun seekForward(deltaMs: Long = 10000L) {
        if (_currentMediaItem.value?.isVideo == true) {
            onSeekVideoRelative?.invoke(deltaMs / 1000.0)
            return
        }
        val target = (player.currentPosition + deltaMs).coerceAtMost(player.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        if (_currentMediaItem.value?.isVideo == true) {
            onSeekVideoRelative?.invoke(-(deltaMs / 1000.0))
            return
        }
        val target = (player.currentPosition - deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun skipNext() {
        if (_currentMediaItem.value?.isVideo == true) {
            playNextVideo()
            return
        }
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (_currentMediaItem.value?.isVideo == true) {
            playPreviousVideo()
            return
        }
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun playNextVideo() {
        val list = _playlist.value
        val nextIdx = _currentIndex.value + 1
        if (nextIdx in list.indices) {
            val nextItem = list[nextIdx]
            playMedia(nextItem, list)
        }
    }

    fun playPreviousVideo() {
        val list = _playlist.value
        val prevIdx = _currentIndex.value - 1
        if (prevIdx in list.indices) {
            val prevItem = list[prevIdx]
            playMedia(prevItem, list)
        }
    }

    fun playVideoAtIndex(index: Int) {
        val list = _playlist.value
        if (index in list.indices) {
            val item = list[index]
            playMedia(item, list)
        }
    }

    @Suppress("unused")
    fun hasNextVideo(): Boolean = _currentIndex.value < _playlist.value.size - 1

    @Suppress("unused")
    fun hasPreviousVideo(): Boolean = _currentIndex.value > 0

    fun saveVideoPlaybackPosition(mediaId: Long, positionMs: Long, durationMs: Long) {
        if (mediaId <= 0 || positionMs < 0) return
        val now = System.currentTimeMillis()
        _playbackPositions.update { it + (mediaId to positionMs) }
        _playbackTimestamps.update { it + (mediaId to now) }
        historyManager.savePosition(mediaId, positionMs, durationMs)
    }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = next
        player.repeatMode = next
    }

    fun toggleShuffle() {
        val next = !_shuffleMode.value
        _shuffleMode.value = next
        player.shuffleModeEnabled = next
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
        if (!expanded) {
            val item = _currentMediaItem.value
            if (item != null && !item.isVideo) {
                _player?.let { p ->
                    val pos = p.currentPosition
                    val dur = if (p.duration > 0) p.duration else 0L
                    _playbackPositions.update { it + (item.id to pos) }
                    historyManager.savePosition(item.id, pos, dur)
                }
            }
        }
    }

    fun playUrl(url: String, title: String = "Streaming Media", isVideo: Boolean = false) {
        val cleanUrl = url.trim().let {
            if (!it.startsWith("http://", ignoreCase = true) &&
                !it.startsWith("https://", ignoreCase = true) &&
                !it.startsWith("rtsp://", ignoreCase = true)
            ) {
                "https://$it"
            } else {
                it
            }
        }

        historyManager.saveRecentStream(cleanUrl, title, isVideo)
        val uri = cleanUrl.toUri()

        // Strip query parameters and fragments to identify path extension accurately
        val path = try {
            uri.path ?: cleanUrl.substringBefore('?').substringBefore('#')
        } catch (_: Exception) {
            cleanUrl.substringBefore('?').substringBefore('#')
        }

        val resolvedMimeType = when {
            path.contains(".m3u8", ignoreCase = true) -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
            path.contains(".mpd", ignoreCase = true) -> androidx.media3.common.MimeTypes.APPLICATION_MPD
            path.contains(".ism", ignoreCase = true) -> androidx.media3.common.MimeTypes.APPLICATION_SS
            cleanUrl.startsWith("rtsp://", ignoreCase = true) -> androidx.media3.common.MimeTypes.APPLICATION_RTSP
            path.endsWith(".mp4", ignoreCase = true) || path.endsWith(".m4v", ignoreCase = true) -> "video/mp4"
            path.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            path.endsWith(".webm", ignoreCase = true) -> if (isVideo) "video/webm" else "audio/webm"
            path.endsWith(".ts", ignoreCase = true) -> "video/mp2t"
            path.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            path.endsWith(".flac", ignoreCase = true) -> "audio/flac"
            path.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            path.endsWith(".aac", ignoreCase = true) -> "audio/aac"
            path.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            path.endsWith(".ogg", ignoreCase = true) || path.endsWith(".opus", ignoreCase = true) -> "audio/ogg"
            else -> ""
        }

        val actualIsVideo = isVideo ||
            resolvedMimeType.startsWith("video/") ||
            resolvedMimeType == androidx.media3.common.MimeTypes.APPLICATION_M3U8 ||
            resolvedMimeType == androidx.media3.common.MimeTypes.APPLICATION_MPD ||
            path.endsWith(".mp4", ignoreCase = true) ||
            path.endsWith(".mkv", ignoreCase = true) ||
            path.endsWith(".ts", ignoreCase = true) ||
            path.endsWith(".mov", ignoreCase = true) ||
            path.endsWith(".m4v", ignoreCase = true)

        val item = MediaItemModel(
            id = System.currentTimeMillis(),
            uri = uri,
            title = title,
            artist = if (actualIsVideo) "Network Video Stream" else "Network Audio Stream",
            album = "Network Stream",
            isVideo = actualIsVideo,
            mimeType = resolvedMimeType
        )
        playMedia(item)
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        var pollCount = 0
        progressPollJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    val dur = if (player.duration > 0) player.duration else 0L
                    _currentPositionMs.value = pos
                    if (dur > 0 && _durationMs.value != dur) {
                        _durationMs.value = dur
                    }
                    _currentMediaItem.value?.id?.let { mediaId ->
                        // Throttle library UI state updates to every 5 seconds (50 x 100ms)
                        // to avoid recomposing the entire media library 10 times a second
                        if (++pollCount % 50 == 0) {
                            _playbackPositions.update { it + (mediaId to pos) }
                            historyManager.savePosition(mediaId, pos, dur)
                        }
                    }
                }
                delay(100.milliseconds)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = null
    }

    fun release() {
        videoServiceStartJob?.cancel()
        videoServiceStartJob = null
        releaseWakeLock()
        stopProgressPolling()
        clearVideoCallbacks()
        playerListener?.let {
            _player?.removeListener(it)
            playerListener = null
        }
        _mediaSession?.release()
        _mediaSession = null
        _player?.release()
        _player = null
        scope.cancel()
    }
}

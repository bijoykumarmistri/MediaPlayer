package com.mediaplayer.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.mediaplayer.MainActivity
import com.mediaplayer.audio.UsbBitPerfectAudioController
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.player.UniversalMediaPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MediaPlayerManager(
    private val context: Context,
    val usbAudioController: UsbBitPerfectAudioController
) {
    lateinit var player: ExoPlayer
        private set

    lateinit var mediaSession: MediaSession
        private set

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressPollJob: Job? = null

    private val _currentMediaItem = MutableStateFlow<MediaItemModel?>(null)
    val currentMediaItem: StateFlow<MediaItemModel?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun clearPlaybackError() {
        _playbackError.value = null
    }

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

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    val historyManager = PlaybackHistoryManager(context)

    private val _playbackPositions = MutableStateFlow<Map<Long, Long>>(historyManager.getAllPositions())
    val playbackPositions: StateFlow<Map<Long, Long>> = _playbackPositions.asStateFlow()

    private val _playedMediaIds = MutableStateFlow<Set<Long>>(
        historyManager.getPlayedMediaIds().mapNotNull { it.toLongOrNull() }.toSet()
    )
    val playedMediaIds: StateFlow<Set<Long>> = _playedMediaIds.asStateFlow()

    private val _decoderMode = MutableStateFlow("FFmpeg Extension (Universal)")
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    fun isVideoPlayed(mediaId: Long): Boolean {
        return _playedMediaIds.value.contains(mediaId) || historyManager.isPlayed(mediaId)
    }

    fun getSavedPosition(mediaId: Long): Long {
        return _playbackPositions.value[mediaId] ?: historyManager.getPosition(mediaId)
    }

    fun setDecoderMode(mode: String) {
        _decoderMode.value = mode
    }

    fun initialize() {
        player = UniversalMediaPlaybackService.createExoPlayer(context, usbAudioController)

        val sessionActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(context, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MediaPlayerManager", "ExoPlayer error: ${error.errorCodeName} (${error.errorCode})", error)
                val msg = when (error.errorCode) {
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Network connection timed out. Check your internet connection or stream URL."
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Server returned an HTTP error (${(error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: "error"}). Stream may be offline or geo-restricted."
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Stream manifest or container format is invalid or unsupported."
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                        "Unable to connect to stream server. Please verify the URL."
                    else -> error.localizedMessage ?: "Playback error encountered."
                }
                _playbackError.value = msg
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (_currentMediaItem.value?.isVideo == true) return
                _playbackState.value = state
                _durationMs.value = if (player.duration > 0) player.duration else 0L
                if (state == Player.STATE_READY) {
                    _playbackError.value = null
                    startProgressPolling()
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

                currentMedia?.let {
                    usbAudioController.updateMediaAudioFormat(
                        sampleRate = it.sampleRate,
                        bitDepth = it.bitDepth,
                        channels = it.channels,
                        codec = extractCodecName(it.mimeType)
                    )
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                inspectAudioTracks(tracks)
            }
        })
    }

    private fun inspectAudioTracks(tracks: Tracks) {
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val format: Format = group.getTrackFormat(i)
                        val sampleRate = if (format.sampleRate > 0) format.sampleRate else (_currentMediaItem.value?.sampleRate ?: 44100)
                        val channels = if (format.channelCount > 0) format.channelCount else (_currentMediaItem.value?.channels ?: 2)
                        val bitDepth = resolveBitDepth(format.pcmEncoding, _currentMediaItem.value?.bitDepth ?: 16)
                        val codec = extractCodecName(format.sampleMimeType ?: _currentMediaItem.value?.mimeType ?: "audio/pcm")

                        usbAudioController.updateMediaAudioFormat(
                            sampleRate = sampleRate,
                            bitDepth = bitDepth,
                            channels = channels,
                            codec = codec
                        )
                        return
                    }
                }
            }
        }
    }

    private fun resolveBitDepth(pcmEncoding: Int, fallback: Int): Int {
        return when (pcmEncoding) {
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            C.ENCODING_PCM_16BIT -> 16
            else -> fallback
        }
    }

    private fun extractCodecName(mimeType: String): String {
        return when {
            mimeType.contains("flac", ignoreCase = true) -> "FLAC"
            mimeType.contains("alac", ignoreCase = true) -> "ALAC"
            mimeType.contains("wav", ignoreCase = true) -> "WAV"
            mimeType.contains("aac", ignoreCase = true) -> "AAC"
            mimeType.contains("mp4", ignoreCase = true) -> "AAC/PCM"
            mimeType.contains("mp3", ignoreCase = true) -> "MP3"
            mimeType.contains("opus", ignoreCase = true) -> "OPUS"
            mimeType.contains("vorbis", ignoreCase = true) -> "VORBIS"
            mimeType.contains("eac3", ignoreCase = true) -> "E-AC3"
            mimeType.contains("ac3", ignoreCase = true) -> "AC3"
            mimeType.contains("dts", ignoreCase = true) -> "DTS"
            else -> "PCM"
        }
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
            // Video playback is handled 100% by C++ Core (libmpv HW+).
            videoStartPositionOverride = startPositionMs
            // Pause ExoPlayer and clear media items so it does NOT allocate hardware video decoders or audio sinks.
            player.pause()
            player.clearMediaItems()
            _isPlaying.value = false
            // Safely stop background audio playback service so it doesn't linger while watching video
            try {
                val serviceIntent = Intent(context, UniversalMediaPlaybackService::class.java)
                context.stopService(serviceIntent)
            } catch (_: Exception) {}
            return
        }

        // For audio playback: ensure UniversalMediaPlaybackService is running so notification and lockscreen controls are active.
        try {
            val serviceIntent = Intent(context, UniversalMediaPlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isNetworkStream = item.uri.scheme?.lowercase() in listOf("http", "https", "rtsp")
        _playbackError.value = null

        // Determine starting position: resume from saved position if valid.
        // For network streams without explicit start position, default to C.TIME_UNSET so live streams start at the live edge!
        val resumePos = startPositionMs ?: run {
            if (isNetworkStream) {
                androidx.media3.common.C.TIME_UNSET
            } else {
                val saved = historyManager.getPosition(item.id)
                if (saved > 3000L && (item.durationMs <= 0 || saved < item.durationMs - 5000L)) {
                    saved
                } else {
                    0L
                }
            }
        }

        // Configure bit-perfect USB routing BEFORE player starts preparing the audio track
        usbAudioController.updateMediaAudioFormat(
            sampleRate = item.sampleRate,
            bitDepth = item.bitDepth,
            channels = item.channels,
            codec = extractCodecName(item.mimeType)
        )

        // Ensure unity volume gain (1.0f) to preserve bit-perfect fidelity without software attenuation
        player.volume = 1.0f

        val mediaItems = playlist.map { it.toMedia3Item() }
        player.setMediaItems(mediaItems, index, resumePos)
        player.prepare()
        player.play()
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
            player.pause()
            player.clearMediaItems()
            return
        }

        try {
            val media3Item = item.toMedia3Item()
            player.setMediaItem(media3Item, savedPos)
            player.prepare()
            player.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var onToggleVideoPlayPause: (() -> Unit)? = null
    var onPauseVideo: (() -> Unit)? = null

    fun stopVideo() {
        if (_currentMediaItem.value?.isVideo == true) {
            onPauseVideo?.invoke()
            _currentMediaItem.value = null
            _isPlaying.value = false
            _isPlayerExpanded.value = false
        }
    }

    fun playPause() {
        if (_currentMediaItem.value?.isVideo == true) {
            onToggleVideoPlayPause?.invoke()
            return
        }
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) {
            pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            player.play()
            _isPlaying.value = true
        }
    }

    fun pause() {
        if (_currentMediaItem.value?.isVideo == true) {
            onPauseVideo?.invoke()
            _isPlaying.value = false
            return
        }
        val pos = player.currentPosition
        val dur = player.duration
        _currentMediaItem.value?.id?.let { mediaId ->
            _playbackPositions.update { it + (mediaId to pos) }
            historyManager.savePosition(mediaId, pos, dur)
        }
        player.pause()
        _isPlaying.value = false
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _currentMediaItem.value = null
        _isPlaying.value = false
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = safePos
        player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
        player.seekTo(safePos)
    }

    fun seekToFast(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = safePos
        player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
        player.seekTo(safePos)
    }

    fun seekToExact(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = safePos
        player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
        player.seekTo(safePos)
    }

    fun setVolumeBoost(level: Float) {
        // level: 0.0f .. 2.0f
        if (level <= 1.0f) {
            player.volume = 1.0f
        } else {
            // Soft-knee tanh saturation:
            // Smoothly compresses loudness spikes while boosting quiet dialog without digital clipping
            val boost = (level - 1.0f).toDouble()
            val gain = 1.0f + kotlin.math.tanh(boost).toFloat()
            player.volume = gain
        }
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val target = (player.currentPosition + deltaMs).coerceAtMost(player.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val target = (player.currentPosition - deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
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

    fun hasNextVideo(): Boolean = _currentIndex.value < _playlist.value.size - 1

    fun hasPreviousVideo(): Boolean = _currentIndex.value > 0

    fun saveVideoPlaybackPosition(mediaId: Long, positionMs: Long, durationMs: Long) {
        if (mediaId <= 0 || positionMs < 0) return
        _playbackPositions.update { it + (mediaId to positionMs) }
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
            val mediaId = _currentMediaItem.value?.id
            if (mediaId != null) {
                val pos = player.currentPosition
                val dur = if (player.duration > 0) player.duration else 0L
                _playbackPositions.update { it + (mediaId to pos) }
                historyManager.savePosition(mediaId, pos, dur)
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
        val uri = android.net.Uri.parse(cleanUrl)

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
                delay(100)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = null
    }

    fun release() {
        stopProgressPolling()
        mediaSession.release()
        player.release()
    }
}

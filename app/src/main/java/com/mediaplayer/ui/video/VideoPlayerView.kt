package com.mediaplayer.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.util.AppSettingsManager
import com.mxplayer.mpv.PlayerViewModel
import com.mxplayer.mpv.VideoPlayerScreen as MpvVideoPlayerScreen

/**
 * High-performance video player interface powered 100% by the C++ Core (libmpv HW+) native engine.
 * ExoPlayer video playback has been completely wiped out.
 */
@Composable
fun VideoPlayerView(
    playerManager: MediaPlayerManager,
    telemetry: UsbAudioStatus,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenDacSettings: () -> Unit,
    isInPipMode: Boolean = false,
    settingsManager: AppSettingsManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings = remember(context, settingsManager) { settingsManager ?: AppSettingsManager(context) }
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val videoViewModel: PlayerViewModel = viewModel()
    val uiState by videoViewModel.uiState.collectAsState()

    // Sync libmpv playback status and video dimensions to MediaPlayerManager for PiP
    LaunchedEffect(uiState.isPlaying) {
        playerManager.setVideoPlaying(uiState.isPlaying)
    }

    LaunchedEffect(uiState.videoWidth, uiState.videoHeight) {
        if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
            playerManager.setVideoDimensions(uiState.videoWidth, uiState.videoHeight)
        }
    }

    // Ensure ExoPlayer is paused and cleared so video decoders and audio sinks are 100% dedicated to C++ Core
    LaunchedEffect(Unit) {
        playerManager.player.pause()
    }

    // Connect playerManager pause/play/seek controls to libmpv engine, and ensure video pauses on exit
    DisposableEffect(videoViewModel) {
        playerManager.onToggleVideoPlayPause = { videoViewModel.togglePlayPause() }
        playerManager.onPauseVideo = { videoViewModel.pausePlayback() }
        playerManager.onSeekVideoRelative = { deltaSec -> videoViewModel.seekRelative(deltaSec) }
        onDispose {
            videoViewModel.pausePlayback()
            playerManager.onToggleVideoPlayPause = null
            playerManager.onPauseVideo = null
            playerManager.onSeekVideoRelative = null
            playerManager.setVideoPlaying(false)
        }
    }

    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex < playlist.size - 1

    val initialPositionMs = playerManager.consumeVideoStartPositionOverride() ?: (currentItem?.id?.let { mediaId ->
        if (currentSettings.autoResume) {
            val saved = playerManager.getSavedPosition(mediaId)
            val dur = currentItem?.durationMs ?: 0L
            if (saved > 3000L && (dur <= 0L || saved < dur - 5000L)) {
                saved
            } else {
                0L
            }
        } else {
            0L
        }
    } ?: 0L)

    MpvVideoPlayerScreen(
        modifier = modifier,
        initialUri = currentItem?.uri,
        initialTitle = currentItem?.title,
        initialPositionMs = initialPositionMs,
        hasPrevious = hasPrevious,
        hasNext = hasNext,
        onPrevious = { playerManager.playPreviousVideo() },
        onNext = { playerManager.playNextVideo() },
        onPositionUpdate = { posMs, durMs ->
            currentItem?.id?.let { mediaId ->
                playerManager.saveVideoPlaybackPosition(mediaId, posMs, durMs)
            }
        },
        onBack = onBack,
        onEnterPip = onEnterPip,
        isInPipMode = isInPipMode,
        viewModel = videoViewModel,
        doubleTapSeekSeconds = currentSettings.doubleTapSeekSeconds,
        swipeGesturesEnabled = currentSettings.swipeGesturesEnabled,
        fastScrubEnabled = currentSettings.fastScrubEnabled,
        hold2xSpeedEnabled = currentSettings.hold2xSpeedEnabled,
        defaultDecoderMode = currentSettings.defaultDecoderMode,
        defaultResizeMode = currentSettings.defaultResizeMode,
        subtitleSize = currentSettings.subtitleSize,
        subtitleColorHex = currentSettings.subtitleColorHex
    )
}

fun formatTimeMs(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}

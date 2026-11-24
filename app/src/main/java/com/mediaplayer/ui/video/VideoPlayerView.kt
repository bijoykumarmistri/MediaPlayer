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
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.util.AppSettingsManager
import com.bkm.player.mpv.PlayerViewModel
import com.bkm.player.mpv.VideoPlayerScreen as MpvVideoPlayerScreen

/**
 * High-performance video player interface powered 100% by the C++ Core (libmpv HW+) native engine.
 * ExoPlayer video playback has been completely wiped out.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerView(
    playerManager: MediaPlayerManager,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    settingsManager: AppSettingsManager? = null,
    videoViewModel: PlayerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val currentSettings = remember(context, settingsManager) { settingsManager ?: AppSettingsManager(context) }
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val uiState by videoViewModel.uiState.collectAsState()

    // Sync libmpv playback status and video dimensions to MediaPlayerManager for PiP
    LaunchedEffect(uiState.isPlaying) {
        playerManager.setVideoPlaying(uiState.isPlaying)
    }

    LaunchedEffect(uiState.videoWidth, uiState.videoHeight) {
        if ((uiState.videoWidth > 0) && (uiState.videoHeight > 0)) {
            playerManager.setVideoDimensions(uiState.videoWidth, uiState.videoHeight)
        }
    }

    // Ensure ExoPlayer is stopped for video
    LaunchedEffect(Unit) {
        if (playerManager.isPlayerInitialized) {
            playerManager.player.pause()
            playerManager.player.stop()
            playerManager.player.clearMediaItems()
        }
    }

    // Connect playerManager pause/play/seek controls to libmpv engine, enabling background audio playback
    DisposableEffect(videoViewModel) {
        playerManager.onToggleVideoPlayPause = { videoViewModel.togglePlayPause() }
        playerManager.onPauseVideo = { videoViewModel.pausePlayback() }
        playerManager.onStopVideo = {
            videoViewModel.stopPlayback()
            playerManager.clearVideoCallbacks()
            playerManager.setVideoPlaying(false)
        }
        playerManager.onSeekVideoRelative = { deltaSec -> videoViewModel.seekRelative(deltaSec) }
        playerManager.onSeekVideoAbsolute = { posMs -> videoViewModel.seekTo(posMs / 1000.0) }
        onDispose {
            // Allow video audio to continue playing in background when collapsed to floating mini player
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
            playerManager.updateVideoPosition(posMs, durMs)
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
        backgroundPlaybackEnabled = currentSettings.backgroundPlayback,
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

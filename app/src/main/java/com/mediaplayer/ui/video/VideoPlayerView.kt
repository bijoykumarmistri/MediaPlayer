package com.mediaplayer.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.playback.MediaPlayerManager
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
    modifier: Modifier = Modifier
) {
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val videoViewModel: PlayerViewModel = viewModel()

    // Ensure ExoPlayer is paused and cleared so video decoders and audio sinks are 100% dedicated to C++ Core
    LaunchedEffect(Unit) {
        playerManager.player.pause()
    }

    // Connect playerManager pause/play controls to libmpv engine, and ensure video pauses on exit
    DisposableEffect(videoViewModel) {
        playerManager.onToggleVideoPlayPause = { videoViewModel.togglePlayPause() }
        playerManager.onPauseVideo = { videoViewModel.pausePlayback() }
        onDispose {
            videoViewModel.pausePlayback()
            playerManager.onToggleVideoPlayPause = null
            playerManager.onPauseVideo = null
        }
    }

    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex < playlist.size - 1

    val initialPositionMs = playerManager.consumeVideoStartPositionOverride() ?: (currentItem?.id?.let { mediaId ->
        val saved = playerManager.getSavedPosition(mediaId)
        val dur = currentItem?.durationMs ?: 0L
        if (saved > 3000L && (dur <= 0L || saved < dur - 5000L)) {
            saved
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
        viewModel = videoViewModel
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

package com.mediaplayer.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.util.AppSettingsManager
import com.bkm.player.mpv.PlayerPlaylistItem
import com.bkm.player.mpv.PlayerViewModel
import com.bkm.player.mpv.VideoPlayerScreen as MpvVideoPlayerScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    val currentSettings = remember(context, settingsManager) { settingsManager ?: AppSettingsManager(context.applicationContext) }
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()

    // Sync only discrete playback status and video dimensions to MediaPlayerManager (ignores 10Hz time ticks)
    LaunchedEffect(videoViewModel) {
        videoViewModel.uiState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { playing ->
                playerManager.setVideoPlaying(playing)
            }
    }

    LaunchedEffect(videoViewModel) {
        videoViewModel.uiState
            .map { it.videoWidth to it.videoHeight }
            .distinctUntilChanged()
            .collect { (w, h) ->
                if (w > 0 && h > 0) {
                    playerManager.setVideoDimensions(w, h)
                }
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

    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex < playlist.size - 1
    val playerPlaylistItems = remember(playlist) {
        playlist.map { item ->
            PlayerPlaylistItem(
                id = item.id,
                title = item.title,
                durationMs = item.durationMs,
                resolution = item.resolution
            )
        }
    }

    val initialPositionMs = remember(currentItem?.id, currentItem?.uri) {
        playerManager.consumeVideoStartPositionOverride() ?: (currentItem?.id?.let { mediaId ->
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
    }

    MpvVideoPlayerScreen(
        modifier = modifier,
        initialUri = currentItem?.uri,
        initialTitle = currentItem?.title,
        initialPositionMs = initialPositionMs,
        hasPrevious = hasPrevious,
        hasNext = hasNext,
        onPrevious = { playerManager.playPreviousVideo() },
        onNext = { playerManager.playNextVideo() },
        playlistItems = playerPlaylistItems,
        currentPlaylistIndex = currentIndex,
        onSelectPlaylistItem = { idx -> playerManager.playVideoAtIndex(idx) },
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
        autoRotateByDefault = currentSettings.autoRotateVideo,
        autoPlayNext = currentSettings.autoPlayNextVideo,
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

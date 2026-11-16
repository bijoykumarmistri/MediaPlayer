package com.mediaplayer.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.playback.MediaPlayerManager

// Delegates to VideoPlayerScreen
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
    VideoPlayerScreen(
        playerManager = playerManager,
        telemetry = telemetry,
        onBack = onBack,
        onEnterPip = onEnterPip,
        onOpenDacSettings = onOpenDacSettings,
        isInPipMode = isInPipMode,
        modifier = modifier
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

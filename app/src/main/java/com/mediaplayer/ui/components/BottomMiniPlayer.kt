package com.mediaplayer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.video.formatTimeMs

@Composable
fun BottomMiniPlayer(
    playerManager: MediaPlayerManager,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isPlayingState by playerManager.isPlaying.collectAsState()
    val isVideoPlayingState by playerManager.isVideoPlaying.collectAsState()

    if (currentItem == null) return
    val item = currentItem!!
    val isVideo = item.isVideo
    val isPlaying = if (isVideo) isVideoPlayingState else isPlayingState

    val accentColor = if (isVideo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    val appContext = remember(context) { context.applicationContext }
    val imageRequest = remember(item.artworkUri, item.id) {
        item.artworkUri?.let { uri ->
            ImageRequest.Builder(appContext)
                .data(uri)
                .size(128, 128)
                .crossfade(false)
                .build()
        }
    }

    Surface(
        onClick = onExpand,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    accentColor.copy(alpha = 0.2f)
                )
            )
        ),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiniPlayerProgressBar(
                playerManager = playerManager,
                accentColor = accentColor
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art / Video Thumbnail with animated ripple
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = if (isVideo) "Video Thumbnail" else "Artwork",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.matchParentSize(),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Rounded.Movie else Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Playing indicator overlay
                    this@Row.AnimatedVisibility(
                        visible = isPlaying,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }

                    // Video badge indicator
                    if (isVideo) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "VIDEO",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle Column
                Column(modifier = Modifier.weight(1f).clipToBounds()) {
                    Text(
                        text = item.title.ifEmpty { if (isVideo) "Video" else "Unknown Track" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    MiniPlayerSubtitle(
                        playerManager = playerManager,
                        isVideo = isVideo,
                        artist = item.artist
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 34.dp) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        IconButton(
                            onClick = {
                                if (isVideo) playerManager.playPreviousVideo() else playerManager.skipPrevious()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Play / Pause
                        IconButton(
                            onClick = { playerManager.playPause() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        ) {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(140)) + scaleIn(initialScale = 0.7f, animationSpec = tween(140)))
                                        .togetherWith(fadeOut(animationSpec = tween(140)) + scaleOut(targetScale = 0.7f, animationSpec = tween(140)))
                                },
                                label = "miniPlayerPlayPauseAnimation"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = if (isVideo) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Next
                        IconButton(
                            onClick = {
                                if (isVideo) playerManager.playNextVideo() else playerManager.skipNext()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Dismiss / Stop Mini Player
                        IconButton(
                            onClick = {
                                if (isVideo) playerManager.stopVideo() else playerManager.stop()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    playerManager: MediaPlayerManager,
    accentColor: Color
) {
    val currentPos by playerManager.currentPositionMs.collectAsState()
    val totalDur by playerManager.durationMs.collectAsState()
    val progressFraction = if (totalDur > 0) (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f) else 0f

    LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp),
        color = accentColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun MiniPlayerSubtitle(
    playerManager: MediaPlayerManager,
    isVideo: Boolean,
    artist: String
) {
    if (isVideo) {
        val currentPos by playerManager.currentPositionMs.collectAsState()
        val totalDur by playerManager.durationMs.collectAsState()
        val subtitleText = if (totalDur > 0) "${formatTimeMs(currentPos)} / ${formatTimeMs(totalDur)}" else "Video Playback"
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    } else {
        Text(
            text = artist.ifEmpty { "BKM Player" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

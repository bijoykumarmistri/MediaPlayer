package com.mediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold

@Composable
fun BottomMiniPlayer(
    playerManager: MediaPlayerManager,
    telemetry: UsbAudioStatus,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    if (currentItem == null) return

    val accentColor = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan

    Surface(
        onClick = onExpand,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF2131622),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = 0.45f),
                    Color(0x18FFFFFF),
                    accentColor.copy(alpha = 0.45f)
                )
            )
        ),
        shadowElevation = 12.dp,
        tonalElevation = 6.dp
    ) {
        Column {
            // Live Micro Seek/Progress Line (Evaluated in Draw Phase to avoid recomposition)
            LinearProgressIndicator(
                progress = {
                    val dur = playerManager.durationMs.value
                    val pos = playerManager.currentPositionMs.value
                    if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = accentColor,
                trackColor = Color(0x18FFFFFF)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork / Video Preview
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF1E212D))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val item = currentItem
                    if (item?.artworkUri != null) {
                        val imageRequest = remember(item.artworkUri, item.id) {
                            val builder = ImageRequest.Builder(context)
                                .data(item.artworkUri)
                                .size(160, 160)
                                .crossfade(true)
                            if (item.isVideo) {
                                builder.diskCacheKey("vthumb_${item.id}")
                                    .memoryCacheKey("vthumb_${item.id}")
                            }
                            builder.build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (currentItem?.isVideo == true) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AudiophileCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info with scrolling marquee title
                Column(modifier = Modifier.weight(1f)) {
                    val rawTitle = currentItem?.title ?: ""
                    val displayTitle = remember(rawTitle, currentItem?.isVideo) {
                        if (currentItem?.isVideo == true) {
                            val (title, year) = com.mediaplayer.ui.cleanMovieDisplayTitle(rawTitle)
                            if (!year.isNullOrBlank()) "$title ($year)" else title
                        } else {
                            rawTitle
                        }
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live Bit-Perfect sample rate badge
                        if (telemetry.isMixerBypassActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FFD54F))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "BIT-PERFECT ${currentItem?.bitDepth}b/${UsbAudioStatus.formatSampleRate(currentItem?.sampleRate ?: 44100)}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AudiophileGold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (telemetry.isUsbDacConnected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2200E5FF))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "USB DAC",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AudiophileCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = currentItem?.artist ?: "Media Player",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9EAE),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause
                IconButton(
                    onClick = { playerManager.playPause() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF0C0D12),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Skip Next
                IconButton(
                    onClick = { playerManager.skipNext() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Skip Next",
                        tint = Color(0xFFD0D5E0),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

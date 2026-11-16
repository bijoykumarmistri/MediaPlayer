package com.mediaplayer.ui.music

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.components.UsbTelemetryPill
import com.mediaplayer.ui.components.rememberDominantColors
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurface
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.ui.video.formatTimeMs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicNowPlayingScreen(
    playerManager: MediaPlayerManager,
    telemetry: UsbAudioStatus,
    onCollapse: () -> Unit,
    onOpenDacSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val shuffleMode by playerManager.shuffleMode.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()

    var showSignalPathDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Dynamic color extraction with Palette
    val dominantColors = rememberDominantColors(currentItem?.artworkUri)

    // Subtle scale animation on play/pause for album artwork card (Deferred to Draw phase)
    val artworkScale = animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "albumArtScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        dominantColors.dominant.copy(alpha = 0.55f),
                        dominantColors.darkBackground,
                        Color.Black
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFFC0C0D4)
                )

                IconButton(onClick = onOpenDacSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Usb,
                        contentDescription = "USB DAC Settings",
                        tint = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Artwork Card (ExtraLarge Rounded Square with Scale Animation in Draw phase)
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = artworkScale.value
                        scaleY = artworkScale.value
                    }
                    .shadow(elevation = 28.dp, shape = ShapeDefaults.ExtraLarge)
                    .border(1.dp, Color(0x33FFFFFF), ShapeDefaults.ExtraLarge),
                shape = ShapeDefaults.ExtraLarge,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14151C))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentItem?.artworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentItem?.artworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = currentItem?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = dominantColors.accent.copy(alpha = 0.6f),
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Track Title & Artist
            Text(
                text = currentItem?.title ?: "No track playing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${currentItem?.artist ?: "Unknown Artist"} • ${currentItem?.album ?: "Unknown Album"}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0C4),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Audiophile Signal Path & Bit-Perfect Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x221E202B))
                    .border(1.dp, if (telemetry.isMixerBypassActive) AudiophileGold.copy(alpha = 0.7f) else OledBorder, RoundedCornerShape(20.dp))
                    .clickable { showSignalPathDialog = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Route,
                    contentDescription = null,
                    tint = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (telemetry.isMixerBypassActive) {
                        "BIT-PERFECT DIRECT SIGNAL • ${currentItem?.bitDepth ?: 16}b/${UsbAudioStatus.formatSampleRate(currentItem?.sampleRate ?: 44100)}"
                    } else {
                        "AUDIO SIGNAL PATH • ${currentItem?.bitDepth ?: 16}b/${UsbAudioStatus.formatSampleRate(currentItem?.sampleRate ?: 44100)}"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scrubber (Isolated sub-composable to confine timeline updates)
            MusicScrubber(
                playerManager = playerManager,
                accentColor = dominantColors.accent
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Mode
                IconButton(onClick = { playerManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleMode) dominantColors.accent else Color(0xFF757585)
                    )
                }

                // Skip Previous
                IconButton(
                    onClick = { playerManager.skipPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause FAB
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(dominantColors.accent)
                        .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { playerManager.playPause() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Skip Next
                IconButton(
                    onClick = { playerManager.skipNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { playerManager.toggleRepeat() }) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) dominantColors.accent else Color(0xFF757585)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions: Queue sheet button & DAC lab shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenDacSettings) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = AudiophileCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DAC & Audio Lab", color = AudiophileCyan, fontWeight = FontWeight.SemiBold)
                }

                TextButton(onClick = { showQueueSheet = true }) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Up Next (${playlist.size})", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Audiophile Signal Path Inspection Dialog
        if (showSignalPathDialog) {
            AlertDialog(
                onDismissRequest = { showSignalPathDialog = false },
                containerColor = OledSurfaceVariant,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Route, contentDescription = null, tint = AudiophileGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Signal Path", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Complete audiophile pipeline from media stream to hardware DAC:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0C0)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        SignalNode(
                            step = "1. Source Media Stream",
                            detail = "${currentItem?.codecBadge.orEmpty().ifEmpty { currentItem?.mimeType.orEmpty() }} • ${currentItem?.bitDepth}b / ${UsbAudioStatus.formatSampleRate(currentItem?.sampleRate ?: 44100)}",
                            icon = Icons.Rounded.MusicNote,
                            accentColor = AudiophileCyan
                        )
                        SignalDivider()

                        SignalNode(
                            step = "2. Decoding Engine",
                            detail = "Media3 FFmpeg Native Audio Decoder (Lossless Float PCM)",
                            icon = Icons.Rounded.Memory,
                            accentColor = AudiophileCyan
                        )
                        SignalDivider()

                        SignalNode(
                            step = "3. Android Audio Framework",
                            detail = if (telemetry.isMixerBypassActive) {
                                "MIXER_BEHAVIOR_BIT_PERFECT (System Mixer & SRC Bypassed)"
                            } else {
                                "Standard Android AudioTrack Mixer (48kHz Resampler Active)"
                            },
                            icon = Icons.Rounded.GraphicEq,
                            accentColor = if (telemetry.isMixerBypassActive) AudiophileGold else Color.Gray
                        )
                        SignalDivider()

                        SignalNode(
                            step = "4. Output Hardware Interface",
                            detail = if (telemetry.isUsbDacConnected) {
                                "${telemetry.dacName} (USB-C Direct Output)"
                            } else {
                                "Built-in Device Speaker / Headphone Jack"
                            },
                            icon = if (telemetry.isUsbDacConnected) Icons.Rounded.Usb else Icons.Rounded.Speaker,
                            accentColor = if (telemetry.isMixerBypassActive) AudiophileGreen else AudiophileCyan
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSignalPathDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        // Up Next Queue Bottom Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = OledSurfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next Queue (${playlist.size} tracks)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showQueueSheet = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        itemsIndexed(playlist) { index, track ->
                            val isCurrent = index == currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) Color(0x2200E5FF) else Color.Transparent)
                                    .clickable {
                                        playerManager.playMedia(track, playlist)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (isCurrent) AudiophileCyan else Color(0xFF7E7E90),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) AudiophileCyan else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${formatTimeMs(track.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E8E9E),
                                        maxLines = 1
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = "Playing",
                                        tint = AudiophileCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalNode(
    step: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18000000))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(step, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0C0))
        }
    }
}

@Composable
private fun SignalDivider() {
    Box(
        modifier = Modifier
            .padding(start = 28.dp)
            .width(2.dp)
            .height(14.dp)
            .background(Color(0x44FFFFFF))
    )
}

@Composable
private fun MusicScrubber(
    playerManager: MediaPlayerManager,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val positionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = currentFraction,
            onValueChange = { frac ->
                isDragging = true
                dragFraction = frac
            },
            onValueChangeFinished = {
                if (durationMs > 0) {
                    playerManager.seekTo((dragFraction * durationMs).toLong())
                }
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0x33FFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayedPos = if (isDragging) (dragFraction * durationMs).toLong() else positionMs
            Text(
                text = formatTimeMs(displayedPos),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA0A0B0)
            )
            Text(
                text = formatTimeMs(durationMs),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA0A0B0)
            )
        }
    }
}

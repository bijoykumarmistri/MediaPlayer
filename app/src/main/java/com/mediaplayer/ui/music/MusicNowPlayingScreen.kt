package com.mediaplayer.ui.music

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.components.rememberDominantColors
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.video.formatTimeMs
import com.mediaplayer.util.AppSettingsManager
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicNowPlayingScreen(
    playerManager: MediaPlayerManager,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    isPitchBlack: Boolean = false,
) {
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val shuffleMode by playerManager.shuffleMode.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()

    var showQueueSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Dynamic color extraction with Palette
    val (dominantColor, accentColor, darkBgColor) = rememberDominantColors(currentItem?.artworkUri)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isPitchBlack) {
                    Brush.verticalGradient(listOf(Color.Black, Color.Black))
                } else {
                    Brush.verticalGradient(
                        listOf(
                            dominantColor.copy(alpha = 0.6f),
                            darkBgColor,
                            Color.Black
                        )
                    )
                }
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar: Collapse action & Screen Title
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
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (playlist.isNotEmpty()) {
                        Text(
                            text = "Track ${currentIndex + 1} of ${playlist.size}",
                            fontSize = 11.sp,
                            color = Color(0xFF9E9EAE)
                        )
                    }
                }

                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cool Dynamic Music Thumbnail (Vinyl Record & Modern Cover Art with Smooth Animations)
            CoolMusicThumbnail(
                currentItem = currentItem,
                isPlaying = isPlaying,
                accentColor = accentColor,
                dominantColor = dominantColor,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Metadata: Title, Artist & Album
            val titleText = currentItem?.title?.ifEmpty { "Unknown Track" } ?: "No Track Selected"
            val artistText = currentItem?.artist?.ifEmpty { "Unknown Artist" } ?: "Select a song to play"
            val albumText = currentItem?.album?.ifEmpty { "" }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .basicMarquee()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (!albumText.isNullOrEmpty()) "$artistText • $albumText" else artistText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0B0C4),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hi-Fi & Audio Format Badges
            NowPlayingFormatBadges(
                playerManager = playerManager,
                currentItem = currentItem,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Scrubber
            MusicScrubber(
                playerManager = playerManager,
                accentColor = accentColor
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
                        tint = if (shuffleMode) accentColor else Color(0xFF757585)
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
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor)
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
                            modifier = Modifier.size(40.dp)
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
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) accentColor else Color(0xFF757585)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Actions: Queue sheet button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Up Next (${playlist.size} tracks)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Up Next Queue Bottom Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                        itemsIndexed(
                            items = playlist,
                            key = { index, track -> "${index}_${track.id}" },
                            contentType = { _, _ -> "queue_track" }
                        ) { index, track ->
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
                                    text = (index + 1).toString(),
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
private fun MusicScrubber(
    playerManager: MediaPlayerManager,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settingsManager = remember { AppSettingsManager(context) }
    var showRemainingTime by rememberSaveable { mutableStateOf(settingsManager.musicTimeViewRemaining) }

    val positionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val displayedPos = if (isDragging) (dragFraction * durationMs).toLong() else positionMs
    val remainingMs = (durationMs - displayedPos).coerceAtLeast(0L)

    Column(modifier = modifier.fillMaxWidth()) {
        // Timeline Seekbar
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

        // Time labels row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        showRemainingTime = !showRemainingTime
                        settingsManager.musicTimeViewRemaining = showRemainingTime
                    }
            ) {
                Text(
                    text = formatTimeMs(displayedPos),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (isDragging) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "Scrub: ${formatTimeMs(displayedPos)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (showRemainingTime) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                border = if (showRemainingTime) BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        showRemainingTime = !showRemainingTime
                        settingsManager.musicTimeViewRemaining = showRemainingTime
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val durationDisplay = if (showRemainingTime) {
                        "-${formatTimeMs(remainingMs)}"
                    } else {
                        formatTimeMs(durationMs)
                    }
                    Text(
                        text = durationDisplay,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (showRemainingTime) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (showRemainingTime) accentColor else Color(0xFFA0A0B0)
                    )
                }
            }
        }
    }
}

/**
 * Animated 4-bar equalizer indicator that dances to live playback.
 */
@Composable
private fun EqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eqAnim")
    val s1 by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.89f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s1"
    )
    val s2 by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 0.33f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s2"
    )
    val s3 by infiniteTransition.animateFloat(
        initialValue = 0.33f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(430, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s3"
    )
    val s4 by infiniteTransition.animateFloat(
        initialValue = 0.67f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(610, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s4"
    )

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        EqualizerBarItem(isPlaying = isPlaying, color = color, scaleProvider = { s1 })
        EqualizerBarItem(isPlaying = isPlaying, color = color, scaleProvider = { s2 })
        EqualizerBarItem(isPlaying = isPlaying, color = color, scaleProvider = { s3 })
        EqualizerBarItem(isPlaying = isPlaying, color = color, scaleProvider = { s4 })
    }
}

@Composable
private fun EqualizerBarItem(
    isPlaying: Boolean,
    color: Color,
    scaleProvider: () -> Float
) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(18.dp)
            .graphicsLayer {
                scaleY = if (isPlaying) scaleProvider() else 0.22f
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(RoundedCornerShape(1.5.dp))
            .background(color)
    )
}

/**
 * Cool dynamic music thumbnail offering:
 * 1. Realistic spinning vinyl turntable disc with micro-grooves and reflective sheen.
 * 2. Animated breathing glow halo.
 * 3. Modern full cover art card mode (tap anywhere to toggle between Vinyl & Cover Art).
 * 4. Animated live equalizer badge in the corner.
 */
@Composable
fun CoolMusicThumbnail(
    currentItem: MediaItemModel?,
    isPlaying: Boolean,
    accentColor: Color,
    dominantColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artUri = currentItem?.artworkUri
    val artworkRequest = remember(artUri, currentItem?.id) {
        artUri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .size(512, 512)
                .crossfade(true)
                .build()
        }
    }
    var isVinylMode by rememberSaveable { mutableStateOf(true) }
    var currentRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = android.os.SystemClock.uptimeMillis()
            while (isActive) {
                withFrameMillis { frameTime ->
                    val delta = (frameTime - lastTime).coerceIn(0L, 100L)
                    lastTime = frameTime
                    currentRotation = (currentRotation + (delta * 0.035f)) % 360f
                }
            }
        }
    }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "artworkScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = artworkScale
                scaleY = artworkScale
            }
            .clickable { isVinylMode = !isVinylMode },
        contentAlignment = Alignment.Center
    ) {
        // Ambient Pulsing Glow Halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = if (isPlaying) 0.38f else 0.16f),
                            dominantColor.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        if (isVinylMode) {
            VinylDiscArtwork(
                currentItem = currentItem,
                artworkRequest = artworkRequest,
                isPlaying = isPlaying,
                currentRotationProvider = { currentRotation },
                accentColor = accentColor,
                dominantColor = dominantColor
            )
        } else {
            CoverArtCard(
                currentItem = currentItem,
                artworkRequest = artworkRequest,
                isPlaying = isPlaying,
                accentColor = accentColor,
                dominantColor = dominantColor
            )
        }

        ThumbnailModeBadge(
            isVinylMode = isVinylMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        PlaybackStatusBadge(
            isPlaying = isPlaying,
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        )
    }
}

@Composable
private fun VinylDiscArtwork(
    currentItem: MediaItemModel?,
    artworkRequest: ImageRequest?,
    isPlaying: Boolean,
    currentRotationProvider: () -> Float,
    accentColor: Color,
    dominantColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = if (isPlaying) 28.dp else 12.dp,
                shape = CircleShape,
                ambientColor = accentColor.copy(alpha = 0.4f),
                spotColor = accentColor.copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            .background(Color(0xFF0C0D12))
            .graphicsLayer {
                rotationZ = currentRotationProvider()
            },
        contentAlignment = Alignment.Center
    ) {
        VinylGroovesCanvas()

        VinylCenterLabel(
            currentItem = currentItem,
            artworkRequest = artworkRequest,
            accentColor = accentColor,
            dominantColor = dominantColor
        )
    }
}

@Composable
private fun VinylGroovesCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        for (i in 1..10) {
            val r = maxRadius * (0.50f + i * 0.046f)
            if (r < maxRadius - 6f) {
                drawCircle(
                    color = Color.White.copy(alpha = if (i % 2 == 0) 0.055f else 0.025f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.3f)
                )
            }
        }

        drawArc(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                center = center,
                radius = maxRadius
            ),
            startAngle = 40f,
            sweepAngle = 45f,
            useCenter = true
        )
        drawArc(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                center = center,
                radius = maxRadius
            ),
            startAngle = 220f,
            sweepAngle = 45f,
            useCenter = true
        )
    }
}

@Composable
private fun VinylCenterLabel(
    currentItem: MediaItemModel?,
    artworkRequest: ImageRequest?,
    accentColor: Color,
    dominantColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize(0.48f)
            .clip(CircleShape)
            .border(2.5.dp, accentColor.copy(alpha = 0.7f), CircleShape)
            .background(Color(0xFF161824)),
        contentAlignment = Alignment.Center
    ) {
        if (artworkRequest != null) {
            AsyncImage(
                model = artworkRequest,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = 0.85f),
                                dominantColor,
                                Color(0xFF0F1018)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    val labelText = currentItem?.title?.take(3)?.uppercase() ?: "MP"
                    Text(
                        text = labelText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, Color(0xFFD4AF37), CircleShape)
        )
    }
}

@Composable
private fun CoverArtCard(
    currentItem: MediaItemModel?,
    artworkRequest: ImageRequest?,
    isPlaying: Boolean,
    accentColor: Color,
    dominantColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = if (isPlaying) 26.dp else 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = accentColor.copy(alpha = 0.4f),
                spotColor = accentColor.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF131520)),
        contentAlignment = Alignment.Center
    ) {
        if (artworkRequest != null) {
            AsyncImage(
                model = artworkRequest,
                contentDescription = "Album Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CoverArtFallbackPlaceholder(
                title = currentItem?.title ?: "Music Player",
                accentColor = accentColor,
                dominantColor = dominantColor
            )
        }
    }
}

@Composable
private fun CoverArtFallbackPlaceholder(
    title: String,
    accentColor: Color,
    dominantColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        accentColor.copy(alpha = 0.35f),
                        dominantColor.copy(alpha = 0.5f),
                        Color(0xFF0E101A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(2.dp, accentColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ThumbnailModeBadge(
    isVinylMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x99000000),
        border = BorderStroke(0.8.dp, Color(0x33FFFFFF)),
        modifier = modifier
    ) {
        Text(
            text = if (isVinylMode) "VINYL • TAP TO FLIP" else "COVER ART • TAP TO FLIP",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xDDFFFFFF),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PlaybackStatusBadge(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD90F111A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EqualizerBars(isPlaying = isPlaying, color = accentColor)
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isPlaying) "PLAYING" else "PAUSED",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun NowPlayingFormatBadges(
    playerManager: MediaPlayerManager,
    currentItem: MediaItemModel?,
    accentColor: Color
) {
    val streamInfo by playerManager.audioStreamInfo.collectAsState()

    val effectiveSampleRate = streamInfo?.sampleRateHz?.takeIf { it > 0 }
        ?: currentItem?.sampleRate?.takeIf { it > 0 }
        ?: 44100
    val effectiveChannels = streamInfo?.channelCount?.takeIf { it > 0 }
        ?: currentItem?.channels?.takeIf { it > 0 }
        ?: 2
    val effectiveCodec = streamInfo?.codec?.takeIf { it.isNotBlank() }
        ?: currentItem?.codecBadge?.takeIf { it.isNotBlank() && it != "PCM Audio" }
        ?: currentItem?.mimeType?.substringAfterLast("/")?.removePrefix("x-")?.uppercase()
        ?: "AUDIO"
    val isHiRes = (currentItem?.bitDepth ?: 16) >= 24 ||
        effectiveSampleRate >= 48000 ||
        effectiveCodec.contains("FLAC", ignoreCase = true) ||
        effectiveCodec.contains("ALAC", ignoreCase = true) ||
        effectiveCodec.contains("DSD", ignoreCase = true) ||
        effectiveCodec.contains("Lossless", ignoreCase = true)

    val primaryBadge = if (isHiRes && !effectiveCodec.contains("Opus", ignoreCase = true)) {
        if (effectiveCodec.contains("Hi-Res", ignoreCase = true) || effectiveCodec.contains("Lossless", ignoreCase = true)) {
            effectiveCodec.uppercase()
        } else {
            "HI-RES • ${effectiveCodec.uppercase()}"
        }
    } else {
        effectiveCodec.uppercase()
    }

    val channelLabel = when (effectiveChannels) {
        1 -> "MONO"
        2 -> "STEREO"
        else -> "${effectiveChannels}CH"
    }
    val rateLabel = com.mediaplayer.util.AudioFormatUtils.formatSampleRate(effectiveSampleRate).uppercase()
    val bitrateBps = streamInfo?.bitrateBps ?: 0
    val formatText = if (bitrateBps >= 1000) {
        "$channelLabel • $rateLabel • ${bitrateBps / 1000} KBPS"
    } else {
        "$channelLabel • $rateLabel"
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = primaryBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x22FFFFFF),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Text(
                text = formatText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC0C0D0),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}


package com.mediaplayer.ui.video

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.ui.components.EmptyStateView
import com.mediaplayer.ui.components.PermissionOnboardingCard
import com.mediaplayer.ui.components.VideoFolderItem
import com.mediaplayer.ui.components.formatFileSize
import com.mediaplayer.ui.components.formatFolderDuration
import com.mediaplayer.ui.components.shareMediaFiles
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurfaceVariant

fun cleanMovieDisplayTitle(rawTitle: String): Pair<String, String?> {
    var clean = rawTitle.replace(Regex("\\.(mp4|mkv|avi|mov|webm|flv|m4v|wmv)$", RegexOption.IGNORE_CASE), "")
    val yearRegex = Regex("[ ._(\\[](19\\d\\d|20\\d\\d)[ ._)\\]]?")
    val match = yearRegex.find(clean)
    val year = match?.groupValues?.getOrNull(1)

    if (match != null) {
        val titlePart = clean.substring(0, match.range.first)
            .replace(".", " ")
            .replace("_", " ")
            .trim()
        if (titlePart.isNotBlank()) {
            return Pair(titlePart, year)
        }
    }

    clean = clean.replace(".", " ").replace("_", " ").trim()
    return Pair(clean, null)
}

@Composable
fun InfoTextRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color(0xFFA0A0B0), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FolderListItem(
    folder: VideoFolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firstVideo = remember(folder.videos) { folder.videos.firstOrNull() }
    val imageRequest = remember(firstVideo?.artworkUri, firstVideo?.id) {
        if (firstVideo?.artworkUri != null) {
            ImageRequest.Builder(context)
                .data(firstVideo.artworkUri)
                .size(320, 200)
                .diskCacheKey("vthumb_${firstVideo.id}")
                .memoryCacheKey("vthumb_${firstVideo.id}")
                .crossfade(true)
                .build()
        } else null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF131822),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Thumbnail Box with Scrim and Badges
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B2330)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x22000000), Color(0x88000000))
                                )
                            )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = AudiophileCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Duration badge at bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatFolderDuration(folder.totalDurationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // New items indicator badge
                if (folder.newCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${folder.newCount} NEW",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right Column: Title, Path, Videos Count Pill
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = AudiophileCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = folder.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = folder.path,
                    color = Color(0xFF7E8E9E),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E2838), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${folder.videos.size} Videos",
                        color = AudiophileCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun FolderGridItem(
    folder: VideoFolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firstVideo = remember(folder.videos) { folder.videos.firstOrNull() }
    val imageRequest = remember(firstVideo?.artworkUri, firstVideo?.id) {
        if (firstVideo?.artworkUri != null) {
            ImageRequest.Builder(context)
                .data(firstVideo.artworkUri)
                .size(360, 220)
                .diskCacheKey("vthumb_${firstVideo.id}")
                .memoryCacheKey("vthumb_${firstVideo.id}")
                .crossfade(true)
                .build()
        } else null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131822),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 16:9 Thumbnail Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A222E)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x33000000), Color(0x88000000))
                                )
                            )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = AudiophileCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Top-Left Folder Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = AudiophileCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${folder.videos.size}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top-Right New Count Badge
                if (folder.newCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${folder.newCount} NEW",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Bottom-Right Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color(0xD9000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatFolderDuration(folder.totalDurationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = folder.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = folder.path,
                color = Color(0xFF7E8E9E),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoCardItem(
    item: MediaItemModel,
    savedPositionMs: Long,
    isPlayed: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onShowOptions: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onLongClick: () -> Unit = onDeleteClick
) {
    val context = LocalContext.current

    val imageRequest = remember(item.artworkUri, item.id) {
        ImageRequest.Builder(context)
            .data(item.artworkUri)
            .size(480, 270)
            .diskCacheKey("vthumb_${item.id}")
            .memoryCacheKey("vthumb_${item.id}")
            .crossfade(true)
            .build()
    }

    val isRecentlyAdded = remember(item.dateAdded) {
        (item.dateAdded > 0) && ((System.currentTimeMillis() / 1000 - item.dateAdded) < (14 * 24 * 3600))
    }
    val showNewBadge = !isPlayed && savedPositionMs == 0L && (item.isNew || isRecentlyAdded)

    val (cleanTitle, year) = remember(item.title) { cleanMovieDisplayTitle(item.title) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) AudiophileCyan else if (isPlaying) AudiophileCyan else OledBorder,
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1B2A38) else OledSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF161722)),
                contentAlignment = Alignment.Center
            ) {
                if (item.artworkUri != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color(0xFF6E6E80), modifier = Modifier.size(36.dp))
                }

                // Selection Checkbox Badge
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AudiophileCyan else Color(0x99000000))
                            .border(1.5.dp, if (isSelected) AudiophileCyan else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Play Icon Overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) AudiophileCyan else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Resolution Badge
                val resBadge = when {
                    item.resolution.contains("4K", ignoreCase = true) || item.resolution.contains("3840") -> "4K UHD"
                    item.resolution.contains("1080", ignoreCase = true) -> "1080p FHD"
                    item.resolution.isNotEmpty() -> item.resolution
                    else -> "HD"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC00E5FF))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(text = resBadge, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // NEW Badge (only shown on genuinely unplayed & recent videos)
                if (showNewBadge && !isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00E676))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "NEW", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatTimeMs(item.durationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // In-progress playback indicator on thumbnail
                if (savedPositionMs > 0 && item.durationMs > 0) {
                    val progress = (savedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = AudiophileCyan,
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cleanTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (year != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "($year)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AudiophileGold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (savedPositionMs > 0) {
                            "Resume ${formatTimeMs(savedPositionMs)} • ${formatFileSize(item.sizeBytes)}"
                        } else {
                            "${formatFileSize(item.sizeBytes)} • ${formatTimeMs(item.durationMs)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (savedPositionMs > 0) AudiophileCyan.copy(alpha = 0.9f) else Color(0xFF8E8E9E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3-dots Context Menu Action
                IconButton(
                    onClick = onShowOptions,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFFA0A0B0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListItem(
    item: MediaItemModel,
    savedPositionMs: Long,
    isPlayed: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onShowOptions: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onLongClick: () -> Unit = onDeleteClick
) {
    val context = LocalContext.current

    val imageRequest = remember(item.artworkUri, item.id) {
        ImageRequest.Builder(context)
            .data(item.artworkUri)
            .size(320, 180)
            .diskCacheKey("vthumb_${item.id}")
            .memoryCacheKey("vthumb_${item.id}")
            .crossfade(true)
            .build()
    }

    val isRecentlyAdded = remember(item.dateAdded) {
        (item.dateAdded > 0) && ((System.currentTimeMillis() / 1000 - item.dateAdded) < (14 * 24 * 3600))
    }
    val showNewBadge = !isPlayed && savedPositionMs == 0L && (item.isNew || isRecentlyAdded)

    val (cleanTitle, year) = remember(item.title) { cleanMovieDisplayTitle(item.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1E2D3D) else if (isPlaying) Color(0x1A00E5FF) else OledSurfaceVariant)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) AudiophileCyan else if (isPlaying) AudiophileCyan else OledBorder,
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AudiophileCyan else Color(0x33FFFFFF))
                    .border(1.5.dp, if (isSelected) AudiophileCyan else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Thumbnail Box
        Box(
            modifier = Modifier
                .size(100.dp, 60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1B1C28))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.artworkUri != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color(0xFF6E6E80), modifier = Modifier.size(28.dp))
            }

            // Duration Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = formatTimeMs(item.durationMs),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // NEW Badge (only on unplayed recent videos)
            if (showNewBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00E676))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = "NEW", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }

            // In-progress playback indicator on thumbnail
            if (savedPositionMs > 0 && item.durationMs > 0) {
                val progress = (savedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = AudiophileCyan,
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Metadata
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) AudiophileCyan else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (year != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "($year)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AudiophileGold,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val resBadge = when {
                    item.resolution.contains("4K", ignoreCase = true) || item.resolution.contains("3840") -> "4K UHD"
                    item.resolution.contains("1080", ignoreCase = true) -> "1080p FHD"
                    item.resolution.isNotEmpty() -> item.resolution
                    else -> "HD"
                }
                Text(
                    text = resBadge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AudiophileCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x2200E5FF))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (savedPositionMs > 0) {
                        "Resume ${formatTimeMs(savedPositionMs)} • ${formatFileSize(item.sizeBytes)}"
                    } else {
                        "${formatFileSize(item.sizeBytes)} • ${formatTimeMs(item.durationMs)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (savedPositionMs > 0) AudiophileCyan.copy(alpha = 0.9f) else Color(0xFF8E8E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onShowOptions,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFFA0A0B0),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun VideoOptionsMenuDialog(
    video: MediaItemModel,
    savedPositionMs: Long,
    onPlay: () -> Unit,
    onPlayFromBeginning: () -> Unit,
    onShowDetails: () -> Unit,
    onShare: () -> Unit,
    onStartSelection: () -> Unit = {},
    onDelete: () -> Unit,
    onMoveToVault: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161822),
        title = {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (savedPositionMs > 0) {
                    TextButton(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = AudiophileCyan)
                            Spacer(Modifier.width(12.dp))
                            Text("Resume (${formatTimeMs(savedPositionMs)})", color = Color.White)
                        }
                    }
                }
                TextButton(
                    onClick = onPlayFromBeginning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Replay10, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Play from Beginning", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onShowDetails,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = AudiophileGold)
                        Spacer(Modifier.width(12.dp))
                        Text("Video Details", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Share Video", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onStartSelection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = AudiophileCyan)
                        Spacer(Modifier.width(12.dp))
                        Text("Select File", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onMoveToVault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AudiophileGold)
                        Spacer(Modifier.width(12.dp))
                        Text("Hide in Privacy Vault", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                        Spacer(Modifier.width(12.dp))
                        Text("Delete from Storage", color = Color(0xFFFF5252))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9093A0))
            }
        }
    )
}

@Composable
fun VideosTabScreen(
    videos: List<MediaItemModel>,
    isGridView: Boolean,
    openedFolder: VideoFolderItem?,
    onOpenFolder: (VideoFolderItem?) -> Unit,
    sortOrder: Int,
    playbackPositions: Map<Long, Long>,
    playedMediaIds: Set<Long> = emptySet(),
    currentPlayingId: Long?,
    hasPermission: Boolean,
    onToggleView: () -> Unit,
    onPlayVideo: (MediaItemModel, List<MediaItemModel>, Long?) -> Unit,
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit,
    onDeleteVideo: (MediaItemModel) -> Unit = {},
    onMoveToVault: (MediaItemModel) -> Unit = {},
    searchQuery: String = "",
    selectedMediaItems: Set<MediaItemModel> = emptySet(),
    onToggleSelectItem: (MediaItemModel) -> Unit = {},
    onStartSelection: (MediaItemModel) -> Unit = {}
) {
    var selectedVideoForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedVideoForOptions by remember { mutableStateOf<MediaItemModel?>(null) }

    // Intercept back gesture when a folder is opened
    BackHandler(enabled = openedFolder != null) {
        onOpenFolder(null)
    }

    // Group videos by folder name & path
    val folderItems = remember(videos, searchQuery, playbackPositions, playedMediaIds) {
        val grouped = videos.groupBy { video ->
            val name = video.folderName.ifEmpty { "Videos" }
            val path = video.folderPath.ifEmpty { "/storage/emulated/0" }
            name to path
        }
        grouped.map { (key, vList) ->
            val (name, path) = key
            val totalDuration = vList.sumOf { it.durationMs }
            val newCount = vList.count { v ->
                val pos = playbackPositions[v.id] ?: 0L
                val isPlayed = playedMediaIds.contains(v.id) || pos > 0
                val isRecent = (v.dateAdded > 0) && ((System.currentTimeMillis() / 1000 - v.dateAdded) < (14 * 24 * 3600))
                !isPlayed && pos == 0L && (v.isNew || isRecent)
            }
            VideoFolderItem(
                name = name,
                path = path,
                videos = vList,
                totalDurationMs = totalDuration,
                newCount = newCount
            )
        }.filter { folder ->
            searchQuery.isBlank() ||
                folder.name.contains(searchQuery, ignoreCase = true) ||
                folder.path.contains(searchQuery, ignoreCase = true) ||
                folder.videos.any { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    AnimatedContent<VideoFolderItem?>(
        targetState = openedFolder,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(250))).togetherWith(
                    slideOutHorizontally(tween(250)) { -it / 3 } + fadeOut(tween(250))
                )
            } else {
                (slideInHorizontally(tween(250)) { -it / 3 } + fadeIn(tween(250))).togetherWith(
                    slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(250))
                )
            }
        },
        label = "FolderNavigationTransition"
    ) { currentFolder: VideoFolderItem? ->
        if (currentFolder != null) {
            val sortedFolderVideos = remember(currentFolder.videos, sortOrder) {
                when (sortOrder) {
                    1 -> currentFolder.videos.sortedBy { it.title.lowercase() }
                    2 -> currentFolder.videos.sortedByDescending { it.sizeBytes }
                    3 -> currentFolder.videos.sortedByDescending { it.durationMs }
                    else -> currentFolder.videos
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (sortedFolderVideos.isEmpty()) {
                    EmptyStateView(
                        title = "No videos in this folder",
                        subtitle = "Pick files from storage or choose another folder.",
                        onAction = onPickFiles
                    )
                } else if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = sortedFolderVideos, key = { video: MediaItemModel -> video.id }, contentType = { "video_grid_card" }) { video: MediaItemModel ->
                            val pos = playbackPositions[video.id] ?: 0L
                            val isPlayed = playedMediaIds.contains(video.id) || pos > 0
                            val isSelected = selectedMediaItems.contains(video)
                            val isSelectionMode = selectedMediaItems.isNotEmpty()
                            VideoCardItem(
                                item = video,
                                savedPositionMs = pos,
                                isPlayed = isPlayed,
                                isPlaying = currentPlayingId == video.id,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        onToggleSelectItem(video)
                                    } else {
                                        onPlayVideo(video, sortedFolderVideos, null)
                                    }
                                },
                                onShowOptions = { selectedVideoForOptions = video },
                                onDeleteClick = { onDeleteVideo(video) },
                                onLongClick = { onStartSelection(video) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = sortedFolderVideos, key = { video: MediaItemModel -> video.id }, contentType = { "video_list_card" }) { video: MediaItemModel ->
                            val pos = playbackPositions[video.id] ?: 0L
                            val isPlayed = playedMediaIds.contains(video.id) || pos > 0
                            val isSelected = selectedMediaItems.contains(video)
                            val isSelectionMode = selectedMediaItems.isNotEmpty()
                            VideoListItem(
                                item = video,
                                savedPositionMs = pos,
                                isPlayed = isPlayed,
                                isPlaying = currentPlayingId == video.id,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        onToggleSelectItem(video)
                                    } else {
                                        onPlayVideo(video, sortedFolderVideos, null)
                                    }
                                },
                                onShowOptions = { selectedVideoForOptions = video },
                                onDeleteClick = { onDeleteVideo(video) },
                                onLongClick = { onStartSelection(video) }
                            )
                        }
                    }
                }
            }
        } else {
            // Main Videos View: Dedicated Folders View
            Column(modifier = Modifier.fillMaxSize()) {
                if (!hasPermission) {
                    PermissionOnboardingCard(
                        onRequestPermission = onRequestPermission,
                        onPickFiles = onPickFiles
                    )
                }

                // Clean Folders Header with View Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = AudiophileCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Folders (${folderItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Grid / List View Toggle Button
                    IconButton(
                        onClick = onToggleView,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle Grid/List",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (folderItems.isEmpty()) {
                    EmptyStateView(
                        title = "No video folders found",
                        subtitle = "Pick files from storage or connect a USB OTG drive to start playing.",
                        onAction = onPickFiles
                    )
                } else if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = folderItems, key = { folder: VideoFolderItem -> folder.path + "/" + folder.name }, contentType = { "folder_grid_card" }) { folder: VideoFolderItem ->
                            FolderGridItem(
                                folder = folder,
                                onClick = { onOpenFolder(folder) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = folderItems, key = { folder: VideoFolderItem -> folder.path + "/" + folder.name }, contentType = { "folder_list_card" }) { folder: VideoFolderItem ->
                            FolderListItem(
                                folder = folder,
                                onClick = { onOpenFolder(folder) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Video Details Dialog
    if (selectedVideoForDetails != null) {
        val video = selectedVideoForDetails!!
        AlertDialog(
            onDismissRequest = { selectedVideoForDetails = null },
            containerColor = OledSurfaceVariant,
            title = { Text("Video Details", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    InfoTextRow("Title", video.title)
                    InfoTextRow("Folder", video.folderName.ifEmpty { "Videos" })
                    InfoTextRow("Resolution", video.resolution.ifEmpty { "HD (1080p)" })
                    InfoTextRow("Duration", formatTimeMs(video.durationMs))
                    InfoTextRow("File Size", formatFileSize(video.sizeBytes))
                    InfoTextRow("Audio", if (video.sampleRate >= 48000) "Hi-Res Audio (${video.sampleRate / 1000} kHz)" else "Standard Audio (44.1 kHz)")
                    InfoTextRow("Location", video.uri.toString())
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedVideoForDetails = null }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    // Hoisted Video Options Dialog (Single instance across entire screen)
    selectedVideoForOptions?.let { video ->
        val context = LocalContext.current
        val pos = playbackPositions[video.id] ?: 0L
        val currentPlaylist = openedFolder?.videos?.let { folderVids ->
            when (sortOrder) {
                1 -> folderVids.sortedBy { it.title.lowercase() }
                2 -> folderVids.sortedByDescending { it.sizeBytes }
                3 -> folderVids.sortedByDescending { it.durationMs }
                else -> folderVids
            }
        } ?: videos
        VideoOptionsMenuDialog(
            video = video,
            savedPositionMs = pos,
            onPlay = {
                selectedVideoForOptions = null
                onPlayVideo(video, currentPlaylist, null)
            },
            onPlayFromBeginning = {
                selectedVideoForOptions = null
                onPlayVideo(video, currentPlaylist, 0L)
            },
            onShowDetails = {
                selectedVideoForOptions = null
                selectedVideoForDetails = video
            },
            onShare = {
                selectedVideoForOptions = null
                shareMediaFiles(context, listOf(video))
            },
            onStartSelection = {
                selectedVideoForOptions = null
                onStartSelection(video)
            },
            onDelete = {
                selectedVideoForOptions = null
                onDeleteVideo(video)
            },
            onMoveToVault = {
                selectedVideoForOptions = null
                onMoveToVault(video)
            },
            onDismiss = { selectedVideoForOptions = null }
        )
    }
}

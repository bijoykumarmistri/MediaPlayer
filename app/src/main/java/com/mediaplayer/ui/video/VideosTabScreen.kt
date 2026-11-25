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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import android.content.Context
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
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FolderListItem(
    folder: VideoFolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
) {
    val firstVideo = remember(folder.videos) { folder.videos.firstOrNull() }
    val imageRequest = remember(firstVideo?.artworkUri, firstVideo?.id) {
        if (firstVideo?.artworkUri != null) {
            ImageRequest.Builder(context)
                .data(firstVideo.artworkUri)
                .size(320, 200)
                .diskCacheKey("vthumb_${firstVideo.id}")
                .memoryCacheKey("vthumb_${firstVideo.id}")
                .crossfade(false)
                .build()
        } else null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
                        tint = MaterialTheme.colorScheme.primary,
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
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${folder.newCount} NEW",
                            color = MaterialTheme.colorScheme.onPrimary,
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = folder.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = folder.path,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${folder.videos.size} ${if (folder.videos.size == 1) "Video" else "Videos"}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val firstVideo = remember(folder.videos) { folder.videos.firstOrNull() }
    val imageRequest = remember(firstVideo?.artworkUri, firstVideo?.id) {
        if (firstVideo?.artworkUri != null) {
            ImageRequest.Builder(context)
                .data(firstVideo.artworkUri)
                .size(360, 220)
                .diskCacheKey("vthumb_${firstVideo.id}")
                .memoryCacheKey("vthumb_${firstVideo.id}")
                .crossfade(false)
                .build()
        } else null
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 16:9 Thumbnail Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
                        tint = MaterialTheme.colorScheme.primary,
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = folder.videos.size.toString(),
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
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${folder.newCount} NEW",
                            color = MaterialTheme.colorScheme.onPrimary,
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
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = folder.path,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onLongClick: () -> Unit = onDeleteClick,
    context: Context = LocalContext.current,
    recentThresholdEpochSec: Long = remember { (System.currentTimeMillis() / 1000L) - (14L * 24L * 3600L) }
) {
    val imageRequest = remember(item.artworkUri, item.id) {
        ImageRequest.Builder(context)
            .data(item.artworkUri)
            .size(480, 270)
            .diskCacheKey("vthumb_${item.id}")
            .memoryCacheKey("vthumb_${item.id}")
            .crossfade(false)
            .build()
    }

    val isRecentlyAdded = item.dateAdded > 0 && item.dateAdded >= recentThresholdEpochSec
    val showNewBadge = !isPlayed && savedPositionMs == 0L && (item.isNew || isRecentlyAdded)
    val (cleanTitle, year) = remember(item.title) { cleanMovieDisplayTitle(item.title) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            VideoCardThumbnail(
                item = item,
                imageRequest = imageRequest,
                savedPositionMs = savedPositionMs,
                showNewBadge = showNewBadge,
                isPlaying = isPlaying,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode
            )
            VideoCardBottomBar(
                cleanTitle = cleanTitle,
                year = year,
                durationMs = item.durationMs,
                sizeBytes = item.sizeBytes,
                savedPositionMs = savedPositionMs,
                onShowOptions = onShowOptions
            )
        }
    }
}

@Composable
private fun VideoCardThumbnail(
    item: MediaItemModel,
    imageRequest: ImageRequest,
    savedPositionMs: Long,
    showNewBadge: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean
) {
    val resBadge = remember(item.resolution) {
        when {
            item.resolution.contains("4K", ignoreCase = true) || item.resolution.contains("3840") -> "4K UHD"
            item.resolution.contains("1080", ignoreCase = true) -> "1080p FHD"
            item.resolution.isNotEmpty() -> item.resolution
            else -> "HD"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }

        if (isSelectionMode) {
            VideoSelectionCheckbox(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }

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
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(text = resBadge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        if (showNewBadge && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "NEW", color = MaterialTheme.colorScheme.onPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }

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

        if (savedPositionMs > 0 && item.durationMs > 0) {
            val progress = (savedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun VideoCardBottomBar(
    cleanTitle: String,
    year: String?,
    durationMs: Long,
    sizeBytes: Long,
    savedPositionMs: Long,
    onShowOptions: () -> Unit
) {
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (savedPositionMs > 0) {
                    "Resume ${formatTimeMs(savedPositionMs)} • ${formatFileSize(sizeBytes)}"
                } else {
                    "${formatFileSize(sizeBytes)} • ${formatTimeMs(durationMs)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (savedPositionMs > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onShowOptions,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VideoSelectionCheckbox(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0x99000000))
            .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
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
    onLongClick: () -> Unit = onDeleteClick,
    context: Context = LocalContext.current,
    recentThresholdEpochSec: Long = remember { System.currentTimeMillis() / 1000L - 14L * 24L * 3600L }
) {
    val imageRequest = remember(item.artworkUri, item.id) {
        ImageRequest.Builder(context)
            .data(item.artworkUri)
            .size(320, 180)
            .diskCacheKey("vthumb_${item.id}")
            .memoryCacheKey("vthumb_${item.id}")
            .crossfade(false)
            .build()
    }

    val isRecentlyAdded = item.dateAdded > 0 && item.dateAdded >= recentThresholdEpochSec
    val showNewBadge = !isPlayed && savedPositionMs == 0L && (item.isNew || isRecentlyAdded)
    val (cleanTitle, year) = remember(item.title) { cleanMovieDisplayTitle(item.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else -> MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected || isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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
            VideoSelectionCheckbox(isSelected = isSelected)
            Spacer(modifier = Modifier.width(8.dp))
        }

        VideoListThumbnail(
            item = item,
            imageRequest = imageRequest,
            savedPositionMs = savedPositionMs,
            showNewBadge = showNewBadge
        )

        Spacer(modifier = Modifier.width(12.dp))

        VideoListMetadataColumn(
            cleanTitle = cleanTitle,
            year = year,
            resolution = item.resolution,
            durationMs = item.durationMs,
            sizeBytes = item.sizeBytes,
            savedPositionMs = savedPositionMs,
            isPlaying = isPlaying,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onShowOptions,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VideoListThumbnail(
    item: MediaItemModel,
    imageRequest: ImageRequest,
    savedPositionMs: Long,
    showNewBadge: Boolean
) {
    Box(
        modifier = Modifier
            .size(100.dp, 60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
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
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }

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

        if (showNewBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(text = "NEW", color = MaterialTheme.colorScheme.onPrimary, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
        }

        if (savedPositionMs > 0 && item.durationMs > 0) {
            val progress = (savedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun VideoListMetadataColumn(
    cleanTitle: String,
    year: String?,
    resolution: String,
    durationMs: Long,
    sizeBytes: Long,
    savedPositionMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val resBadge = remember(resolution) {
        when {
            resolution.contains("4K", ignoreCase = true) || resolution.contains("3840") -> "4K UHD"
            resolution.contains("1080", ignoreCase = true) -> "1080p FHD"
            resolution.isNotEmpty() -> resolution
            else -> "HD"
        }
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = cleanTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = resBadge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (savedPositionMs > 0) {
                    "Resume ${formatTimeMs(savedPositionMs)} • ${formatFileSize(sizeBytes)}"
                } else {
                    "${formatFileSize(sizeBytes)} • ${formatTimeMs(durationMs)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (savedPositionMs > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    onRename: () -> Unit = {},
    onShare: () -> Unit,
    onStartSelection: () -> Unit = {},
    onDelete: () -> Unit,
    onMoveToVault: () -> Unit = {},
    onRemoveFromContinueWatching: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Resume (${formatTimeMs(savedPositionMs)})", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                TextButton(
                    onClick = onPlayFromBeginning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Replay10, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(12.dp))
                        Text("Play from Beginning", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (savedPositionMs > 0 && onRemoveFromContinueWatching != null) {
                    TextButton(
                        onClick = onRemoveFromContinueWatching,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Text("Remove from Continue Watching", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                TextButton(
                    onClick = onShowDetails,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text("Video Details", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Rename File", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(12.dp))
                        Text("Share Video", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(
                    onClick = onStartSelection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Select File", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(
                    onClick = onMoveToVault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text("Hide in Privacy Vault", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("Delete from Storage", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun <T> StateFlow<T>.currentSnapshot(): T = value

@Composable
private fun <T> StateFlow<T>.collectAsStateWhenVisible(isObscured: Boolean): State<T> {
    val state = remember(this) { mutableStateOf(currentSnapshot()) }
    LaunchedEffect(this, isObscured) {
        if (!isObscured) {
            collect { state.value = it }
        }
    }
    return state
}

@Composable
fun VideosTabScreen(
    videos: List<MediaItemModel>,
    isGridView: Boolean,
    openedFolder: VideoFolderItem?,
    onOpenFolder: (VideoFolderItem?) -> Unit,
    sortOrder: Int,
    playbackPositionsFlow: StateFlow<Map<Long, Long>>,
    playbackTimestampsFlow: StateFlow<Map<Long, Long>>,
    playedMediaIdsFlow: StateFlow<Set<Long>>,
    isPlayerObscured: Boolean = false,
    currentPlayingId: Long?,
    hasPermission: Boolean,
    onToggleView: () -> Unit,
    onPlayVideo: (MediaItemModel, List<MediaItemModel>, Long?) -> Unit,
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit,
    onDeleteVideo: (MediaItemModel) -> Unit = {},
    onRenameVideo: (MediaItemModel) -> Unit = {},
    onMoveToVault: (MediaItemModel) -> Unit = {},
    searchQuery: String = "",
    selectedMediaItems: Set<MediaItemModel> = emptySet(),
    onToggleSelectItem: (MediaItemModel) -> Unit = {},
    onStartSelection: (MediaItemModel) -> Unit = {},
    onRemoveFromContinueWatching: (MediaItemModel) -> Unit = {},
    onClearAllContinueWatching: () -> Unit = {}
) {
    val context = LocalContext.current
    val recentThresholdEpochSec = remember { (System.currentTimeMillis() / 1000L) - (14L * 24L * 3600L) }

    // Freeze library history observation while full-screen/PiP video player covers the tab
    val playbackPositions by playbackPositionsFlow.collectAsStateWhenVisible(isPlayerObscured)
    val playbackTimestamps by playbackTimestampsFlow.collectAsStateWhenVisible(isPlayerObscured)
    val playedMediaIds by playedMediaIdsFlow.collectAsStateWhenVisible(isPlayerObscured)

    var selectedVideoForDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedVideoForOptions by remember { mutableStateOf<MediaItemModel?>(null) }

    // Intercept back gesture when a folder is opened
    BackHandler(enabled = openedFolder != null) {
        onOpenFolder(null)
    }

    val continueWatchingVideos = remember(videos, playbackPositions, playbackTimestamps, searchQuery) {
        if (searchQuery.isNotBlank()) emptyList()
        else videos.asSequence().mapNotNull { video ->
            val pos = playbackPositions[video.id] ?: 0L
            val isInProgress = pos > 3000L && (video.durationMs <= 0L || pos < (video.durationMs * 0.92).toLong())
            if (isInProgress) {
                Triple(video, pos, playbackTimestamps[video.id] ?: 0L)
            } else {
                null
            }
        }.sortedByDescending { it.third }.take(10).toList()
    }

    // Group videos by folder name & path
    val folderItems = remember(videos, searchQuery, playbackPositions, playedMediaIds, recentThresholdEpochSec) {
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
                val isRecent = v.dateAdded > 0 && v.dateAdded >= recentThresholdEpochSec
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

    AnimatedContent(
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
    ) { currentFolder ->
        if (currentFolder != null) {
            VideosOpenedFolderContent(
                folder = currentFolder,
                isGridView = isGridView,
                sortOrder = sortOrder,
                playbackPositions = playbackPositions,
                playedMediaIds = playedMediaIds,
                currentPlayingId = currentPlayingId,
                selectedMediaItems = selectedMediaItems,
                recentThresholdEpochSec = recentThresholdEpochSec,
                onPlayVideo = onPlayVideo,
                onPickFiles = onPickFiles,
                onShowOptions = { selectedVideoForOptions = it },
                onDeleteVideo = onDeleteVideo,
                onToggleSelectItem = onToggleSelectItem,
                onStartSelection = onStartSelection,
                context = context
            )
        } else {
            VideosHomeContent(
                hasPermission = hasPermission,
                continueWatchingVideos = continueWatchingVideos,
                folderItems = folderItems,
                isGridView = isGridView,
                currentPlayingId = currentPlayingId,
                onRequestPermission = onRequestPermission,
                onPickFiles = onPickFiles,
                onToggleView = onToggleView,
                onOpenFolder = onOpenFolder,
                onPlayVideo = onPlayVideo,
                onLongClickContinueVideo = { selectedVideoForOptions = it },
                onRemoveFromContinueWatching = onRemoveFromContinueWatching,
                onClearAllContinueWatching = onClearAllContinueWatching,
                context = context
            )
        }
    }

    // Video Details Dialog
    if (selectedVideoForDetails != null) {
        val video = selectedVideoForDetails!!
        AlertDialog(
            onDismissRequest = { selectedVideoForDetails = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Video Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
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
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Hoisted Video Options Dialog (Single instance across entire screen)
    selectedVideoForOptions?.let { video ->
        val pos = playbackPositions[video.id] ?: 0L
        val currentPlaylist = openedFolder?.videos?.let { folderVids ->
            sortVideosList(folderVids, sortOrder)
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
            onRename = {
                selectedVideoForOptions = null
                onRenameVideo(video)
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
            onRemoveFromContinueWatching = if (pos > 0) {
                {
                    selectedVideoForOptions = null
                    onRemoveFromContinueWatching(video)
                }
            } else null,
            onDismiss = { selectedVideoForOptions = null }
        )
    }
}

@Composable
private fun VideosOpenedFolderContent(
    folder: VideoFolderItem,
    isGridView: Boolean,
    sortOrder: Int,
    playbackPositions: Map<Long, Long>,
    playedMediaIds: Set<Long>,
    currentPlayingId: Long?,
    selectedMediaItems: Set<MediaItemModel>,
    recentThresholdEpochSec: Long,
    onPlayVideo: (MediaItemModel, List<MediaItemModel>, Long?) -> Unit,
    onPickFiles: () -> Unit,
    onShowOptions: (MediaItemModel) -> Unit,
    onDeleteVideo: (MediaItemModel) -> Unit,
    onToggleSelectItem: (MediaItemModel) -> Unit,
    onStartSelection: (MediaItemModel) -> Unit,
    context: Context
) {
    val sortedFolderVideos = remember(folder.videos, sortOrder) {
        sortVideosList(folder.videos, sortOrder)
    }
    val isSelectionMode = selectedMediaItems.isNotEmpty()

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
                items(items = sortedFolderVideos, key = { it.id }, contentType = { "video_grid_card" }) { video ->
                    val pos = playbackPositions[video.id] ?: 0L
                    val isPlayed = playedMediaIds.contains(video.id) || pos > 0
                    val isSelected = selectedMediaItems.contains(video)
                    VideoCardItem(
                        item = video,
                        savedPositionMs = pos,
                        isPlayed = isPlayed,
                        isPlaying = currentPlayingId == video.id,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) onToggleSelectItem(video)
                            else onPlayVideo(video, sortedFolderVideos, null)
                        },
                        onShowOptions = { onShowOptions(video) },
                        onDeleteClick = { onDeleteVideo(video) },
                        onLongClick = { onStartSelection(video) },
                        context = context,
                        recentThresholdEpochSec = recentThresholdEpochSec
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = sortedFolderVideos, key = { it.id }, contentType = { "video_list_card" }) { video ->
                    val pos = playbackPositions[video.id] ?: 0L
                    val isPlayed = playedMediaIds.contains(video.id) || pos > 0
                    val isSelected = selectedMediaItems.contains(video)
                    VideoListItem(
                        item = video,
                        savedPositionMs = pos,
                        isPlayed = isPlayed,
                        isPlaying = currentPlayingId == video.id,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) onToggleSelectItem(video)
                            else onPlayVideo(video, sortedFolderVideos, null)
                        },
                        onShowOptions = { onShowOptions(video) },
                        onDeleteClick = { onDeleteVideo(video) },
                        onLongClick = { onStartSelection(video) },
                        context = context,
                        recentThresholdEpochSec = recentThresholdEpochSec
                    )
                }
            }
        }
    }
}

@Composable
private fun VideosHomeContent(
    hasPermission: Boolean,
    continueWatchingVideos: List<Triple<MediaItemModel, Long, Long>>,
    folderItems: List<VideoFolderItem>,
    isGridView: Boolean,
    currentPlayingId: Long?,
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit,
    onToggleView: () -> Unit,
    onOpenFolder: (VideoFolderItem) -> Unit,
    onPlayVideo: (MediaItemModel, List<MediaItemModel>, Long?) -> Unit,
    onLongClickContinueVideo: (MediaItemModel) -> Unit,
    onRemoveFromContinueWatching: (MediaItemModel) -> Unit,
    onClearAllContinueWatching: () -> Unit,
    context: Context
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasPermission) {
            PermissionOnboardingCard(
                onRequestPermission = onRequestPermission,
                onPickFiles = onPickFiles
            )
        }

        if (continueWatchingVideos.isNotEmpty()) {
            ContinueWatchingSection(
                items = continueWatchingVideos,
                currentPlayingId = currentPlayingId,
                onPlayVideo = { video ->
                    val folderName = video.folderName.ifEmpty { "Videos" }
                    val folderPlaylist = folderItems.firstOrNull { it.name == folderName }?.videos ?: listOf(video)
                    onPlayVideo(video, folderPlaylist, null)
                },
                onLongClickVideo = onLongClickContinueVideo,
                onRemoveVideo = onRemoveFromContinueWatching,
                onClearAll = onClearAllContinueWatching
            )
        }

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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Folders (${folderItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onToggleView,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = "Toggle Grid/List",
                    tint = MaterialTheme.colorScheme.onSurface,
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
                items(items = folderItems, key = { it.path + "/" + it.name }, contentType = { "folder_grid_card" }) { folder ->
                    FolderGridItem(
                        folder = folder,
                        onClick = { onOpenFolder(folder) },
                        context = context
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = folderItems, key = { it.path + "/" + it.name }, contentType = { "folder_list_card" }) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = { onOpenFolder(folder) },
                        context = context
                    )
                }
            }
        }
    }
}

private fun sortVideosList(videos: List<MediaItemModel>, sortOrder: Int): List<MediaItemModel> {
    return when (sortOrder) {
        1 -> videos.sortedBy { it.title.lowercase() }
        2 -> videos.sortedByDescending { it.sizeBytes }
        3 -> videos.sortedByDescending { it.durationMs }
        else -> videos
    }
}

@Composable
private fun ContinueWatchingSection(
    items: List<Triple<MediaItemModel, Long, Long>>,
    currentPlayingId: Long?,
    onPlayVideo: (MediaItemModel) -> Unit,
    onLongClickVideo: (MediaItemModel) -> Unit,
    onRemoveVideo: (MediaItemModel) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(
                onClick = { showClearAllConfirmDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (showClearAllConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirmDialog = false },
                title = { Text("Clear Continue Watching?") },
                text = { Text("Are you sure you want to remove all items from your continue watching history?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearAllConfirmDialog = false
                            onClearAll()
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirmDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = items,
                key = { (video, _, _) -> "cw_${video.id}" },
                contentType = { "continue_watching_card" }
            ) { (video, posMs, _) ->
                val isPlaying = currentPlayingId == video.id
                val (cleanTitle, _) = remember(video.title) { cleanMovieDisplayTitle(video.title) }
                val imageRequest = remember(video.artworkUri, video.id) {
                    ImageRequest.Builder(context)
                        .data(video.artworkUri)
                        .size(360, 200)
                        .diskCacheKey("vthumb_${video.id}")
                        .memoryCacheKey("vthumb_${video.id}")
                        .crossfade(false)
                        .build()
                }

                Card(
                    modifier = Modifier
                        .width(168.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .combinedClickable(
                            onClick = { onPlayVideo(video) },
                            onLongClick = { onLongClickVideo(video) }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (video.artworkUri != null) {
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = cleanTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onRemoveVideo(video) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove from Continue Watching",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (video.durationMs > posMs) {
                                val remainingMs = (video.durationMs - posMs).coerceAtLeast(0L)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(5.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${formatTimeMs(remainingMs)} left",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (video.durationMs > 0) {
                                val progress = (posMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .align(Alignment.BottomCenter),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                            Text(
                                text = cleanTitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Resume ${formatTimeMs(posMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

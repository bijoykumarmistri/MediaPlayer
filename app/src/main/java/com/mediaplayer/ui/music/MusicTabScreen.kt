package com.mediaplayer.ui.music

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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mediaplayer.ui.components.MusicFolderItem
import com.mediaplayer.ui.components.PermissionOnboardingCard
import com.mediaplayer.ui.components.deduplicateLazyKeys
import com.mediaplayer.ui.components.formatFileSize
import com.mediaplayer.ui.components.formatFolderDuration
import com.mediaplayer.ui.components.shareMediaFiles
import com.mediaplayer.ui.video.InfoTextRow
import com.mediaplayer.ui.video.formatTimeMs
import com.mediaplayer.util.AudioFormatUtils

private val FallbackProceduralPalettes = listOf(
    listOf(Color(0xFF1E2640), Color(0xFF0D1B2A)),
    listOf(Color(0xFF2E1C38), Color(0xFF1A0F24)),
    listOf(Color(0xFF1B3B36), Color(0xFF0F2320)),
    listOf(Color(0xFF382D1C), Color(0xFF221A0F)),
    listOf(Color(0xFF1E3240), Color(0xFF101B24))
)

@Composable
fun MusicFolderListItem(
    folder: MusicFolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val firstTrack = remember(folder.tracks) { folder.tracks.firstOrNull() }
    val artUri = firstTrack?.artworkUri
    val imageRequest = remember(artUri) {
        if (artUri != null) {
            ImageRequest.Builder(context)
                .data(artUri)
                .size(320, 200)
                .diskCacheKey("athumb_$artUri")
                .memoryCacheKey("athumb_$artUri")
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

            // Right Column: Title, Path, Tracks Count Pill
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
                        text = "${folder.tracks.size} ${if (folder.tracks.size == 1) "Track" else "Tracks"}",
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
fun MusicFolderGridItem(
    folder: MusicFolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val firstTrack = remember(folder.tracks) { folder.tracks.firstOrNull() }
    val artUri = firstTrack?.artworkUri
    val imageRequest = remember(artUri) {
        if (artUri != null) {
            ImageRequest.Builder(context)
                .data(artUri)
                .size(360, 220)
                .diskCacheKey("athumb_$artUri")
                .memoryCacheKey("athumb_$artUri")
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
            // 16:10 Thumbnail Banner
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
                            text = "${folder.tracks.size}",
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
fun MusicCardItem(
    item: MediaItemModel,
    isPlaying: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onShowOptions: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onLongClick: () -> Unit = onDeleteClick,
    context: Context = LocalContext.current
) {
    val artUri = item.artworkUri
    val imageRequest = remember(artUri) {
        if (artUri == null) null
        else {
            ImageRequest.Builder(context)
                .data(artUri)
                .size(480, 480)
                .diskCacheKey("athumb_$artUri")
                .memoryCacheKey("athumb_$artUri")
                .crossfade(false)
                .build()
        }
    }

    val isHiRes = remember(item.bitDepth, item.sampleRate, item.codecBadge, item.mimeType) {
        isAudioItemHiRes(item)
    }

    val badgeText = remember(item.codecBadge, item.sampleRate, item.bitDepth, item.mimeType) {
        resolveAudioBadgeText(item)
    }

    val fallbackColors = remember(item.id) {
        FallbackProceduralPalettes[(item.id % FallbackProceduralPalettes.size).toInt().coerceAtLeast(0)]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected || isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            MusicCardArtworkBox(
                item = item,
                imageRequest = imageRequest,
                fallbackColors = fallbackColors,
                isPlaying = isPlaying,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                isHiRes = isHiRes,
                badgeText = badgeText
            )
            MusicCardFooterRow(
                item = item,
                isPlaying = isPlaying,
                onShowOptions = onShowOptions
            )
        }
    }
}

@Composable
private fun MusicCardArtworkBox(
    item: MediaItemModel,
    imageRequest: ImageRequest?,
    fallbackColors: List<Color>,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isHiRes: Boolean,
    badgeText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Brush.linearGradient(fallbackColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )

        if (imageRequest != null && item.artworkUri != null && !com.mediaplayer.util.AudioArtworkFetcher.noArtworkCache.contains(item.artworkUri)) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
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

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
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
                .background(if (isHiRes) Color(0xCCFFD54F) else Color(0xCC00E5FF))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
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
    }
}

@Composable
private fun MusicCardFooterRow(
    item: MediaItemModel,
    isPlaying: Boolean,
    onShowOptions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.artist} • ${item.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun MusicListItem(
    item: MediaItemModel,
    isPlaying: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onShowOptions: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onLongClick: () -> Unit = onDeleteClick,
    context: Context = LocalContext.current
) {
    val artUri = item.artworkUri
    val imageRequest = remember(artUri) {
        if (artUri == null) null
        else {
            ImageRequest.Builder(context)
                .data(artUri)
                .size(240, 240)
                .diskCacheKey("athumb_$artUri")
                .memoryCacheKey("athumb_$artUri")
                .crossfade(false)
                .build()
        }
    }

    val isHiRes = remember(item.bitDepth, item.sampleRate, item.codecBadge, item.mimeType) {
        isAudioItemHiRes(item)
    }

    val badgeText = remember(item.codecBadge, item.sampleRate, item.bitDepth, item.mimeType) {
        resolveAudioBadgeText(item)
    }

    val fallbackColors = remember(item.id) {
        FallbackProceduralPalettes[(item.id % FallbackProceduralPalettes.size).toInt().coerceAtLeast(0)]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isPlaying -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
            MusicSelectionIndicator(isSelected = isSelected)
            Spacer(modifier = Modifier.width(8.dp))
        }

        MusicListThumbnailBox(
            item = item,
            imageRequest = imageRequest,
            fallbackColors = fallbackColors,
            isPlaying = isPlaying
        )

        Spacer(modifier = Modifier.width(12.dp))

        MusicListMetadataColumn(
            item = item,
            isPlaying = isPlaying,
            isHiRes = isHiRes,
            badgeText = badgeText,
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
private fun MusicSelectionIndicator(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
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
private fun MusicListThumbnailBox(
    item: MediaItemModel,
    imageRequest: ImageRequest?,
    fallbackColors: List<Color>,
    isPlaying: Boolean
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(fallbackColors))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )

        if (imageRequest != null && item.artworkUri != null && !com.mediaplayer.util.AudioArtworkFetcher.noArtworkCache.contains(item.artworkUri)) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = formatTimeMs(item.durationMs),
                color = Color.White,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MusicListMetadataColumn(
    item: MediaItemModel,
    isPlaying: Boolean,
    isHiRes: Boolean,
    badgeText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = badgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHiRes) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isHiRes) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${item.artist} • ${formatFileSize(item.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun isAudioItemHiRes(item: MediaItemModel): Boolean {
    return item.bitDepth >= 24 || item.sampleRate >= 48000 ||
        item.codecBadge.contains("Hi-Res", true) ||
        item.codecBadge.contains("Lossless", true) ||
        item.mimeType.contains("flac", true) ||
        item.mimeType.contains("alac", true) ||
        item.mimeType.contains("dsd", true)
}

private fun resolveAudioBadgeText(item: MediaItemModel): String {
    return item.codecBadge.ifEmpty {
        if (item.sampleRate >= 48000 || item.bitDepth >= 24) {
            "${item.bitDepth}b/${AudioFormatUtils.formatSampleRate(item.sampleRate)}"
        } else if (item.mimeType.isNotEmpty() && !item.mimeType.endsWith("/*")) {
            item.mimeType.substringAfterLast("/").removePrefix("x-").uppercase()
        } else {
            "AUDIO"
        }
    }
}

@Composable
fun TrackOptionsMenuDialog(
    item: MediaItemModel,
    onPlay: () -> Unit,
    onShowDetails: () -> Unit,
    onRename: () -> Unit = {},
    onShare: () -> Unit,
    onStartSelection: () -> Unit = {},
    onDelete: () -> Unit,
    onMoveToVault: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Play Now", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(
                    onClick = onShowDetails,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text("Track Details", color = MaterialTheme.colorScheme.onSurface)
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
                        Text("Share Track", color = MaterialTheme.colorScheme.onSurface)
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

@Composable
fun MusicCategoryFilterPills(
    filterTabs: List<String>,
    selectedTab: String,
    onFilterTabSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = filterTabs,
            key = { it },
            contentType = { "music_filter_pill" }
        ) { tab: String ->
            val isSelected = tab == selectedTab
            val pillBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            val pillBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            val pillContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            val pillIconColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBg)
                    .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                    .clickable { onFilterTabSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (tab) {
                        "Albums" -> Icons.Rounded.Album
                        "Artists" -> Icons.Rounded.Person
                        "Folders" -> Icons.Rounded.Folder
                        "Hi-Res / Lossless" -> Icons.Rounded.GraphicEq
                        else -> Icons.AutoMirrored.Rounded.QueueMusic
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = pillIconColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab,
                        color = pillContentColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MusicTrackDetailsDialog(
    item: MediaItemModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Track Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoTextRow("Title", item.title)
                InfoTextRow("Artist", item.artist.ifEmpty { "Unknown Artist" })
                InfoTextRow("Album", item.album.ifEmpty { "Unknown Album" })
                InfoTextRow("Folder", item.folderName.ifEmpty { "Music" })
                InfoTextRow("Format", item.codecBadge.ifEmpty { item.mimeType.substringAfterLast("/").uppercase() })
                InfoTextRow("Sample Rate", AudioFormatUtils.formatSampleRate(item.sampleRate))
                InfoTextRow("Bit Depth", "${item.bitDepth}-bit")
                InfoTextRow("Duration", formatTimeMs(item.durationMs))
                InfoTextRow("File Size", formatFileSize(item.sizeBytes))
                InfoTextRow("Location", item.uri.toString())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun MusicTabScreen(
    audioList: List<MediaItemModel>,
    isGridView: Boolean,
    openedFolder: MusicFolderItem?,
    onOpenFolder: (MusicFolderItem?) -> Unit,
    sortOrder: Int,
    currentPlayingId: Long?,
    hasPermission: Boolean,
    onToggleView: () -> Unit,
    filterTab: String,
    onFilterTabSelect: (String) -> Unit,
    onPlayAudio: (MediaItemModel, List<MediaItemModel>) -> Unit,
    onShuffleAll: () -> Unit,
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit,
    onDeleteAudio: (MediaItemModel) -> Unit = {},
    onRenameAudio: (MediaItemModel) -> Unit = {},
    onMoveToVault: (MediaItemModel) -> Unit = {},
    searchQuery: String = "",
    selectedMediaItems: Set<MediaItemModel> = emptySet(),
    onToggleSelectItem: (MediaItemModel) -> Unit = {},
    onStartSelection: (MediaItemModel) -> Unit = {}
) {
    val context = LocalContext.current
    val recentThresholdEpochSec = remember { System.currentTimeMillis() / 1000L - 14L * 24L * 3600L }
    var selectedTrackDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedTrackForOptions by remember { mutableStateOf<MediaItemModel?>(null) }

    BackHandler(enabled = openedFolder != null) {
        onOpenFolder(null)
    }

    val filterTabs = remember { listOf("All Tracks", "Folders", "Albums", "Artists", "Hi-Res / Lossless") }

    val folderItems = remember(audioList, searchQuery, filterTab, recentThresholdEpochSec) {
        if (filterTab != "Folders") emptyList()
        else audioList.groupBy { audio ->
            val name = audio.folderName.ifEmpty { "Music" }
            val path = audio.folderPath.ifEmpty { "/storage/emulated/0/Music" }
            name to path
        }.map { (key, aList) ->
            val (name, path) = key
            MusicFolderItem(
                name = name,
                path = path,
                tracks = aList,
                totalDurationMs = aList.sumOf { it.durationMs },
                newCount = aList.count { a -> a.dateAdded > 0 && a.dateAdded >= recentThresholdEpochSec }
            )
        }.filter { folder ->
            searchQuery.isBlank() ||
                folder.name.contains(searchQuery, ignoreCase = true) ||
                folder.path.contains(searchQuery, ignoreCase = true) ||
                folder.tracks.any { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
        }
    }

    val albumItems = remember(audioList, searchQuery, filterTab) {
        if (filterTab != "Albums") emptyList()
        else audioList.groupBy { it.album.ifEmpty { "Unknown Album" } }
            .map { (albumName, aList) ->
                val firstTrack = aList.firstOrNull()
                MusicFolderItem(
                    name = albumName,
                    path = firstTrack?.artist.orEmpty().ifEmpty { "${aList.size} Tracks" },
                    tracks = aList,
                    totalDurationMs = aList.sumOf { it.durationMs }
                )
            }.filter { album ->
                searchQuery.isBlank() ||
                    album.name.contains(searchQuery, ignoreCase = true) ||
                    album.path.contains(searchQuery, ignoreCase = true) ||
                    album.tracks.any { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
            }
    }

    val artistItems = remember(audioList, searchQuery, filterTab) {
        if (filterTab != "Artists") emptyList()
        else audioList.groupBy { it.artist.ifEmpty { "Unknown Artist" } }
            .map { (artistName, aList) ->
                MusicFolderItem(
                    name = artistName,
                    path = "${aList.size} Tracks",
                    tracks = aList,
                    totalDurationMs = aList.sumOf { it.durationMs }
                )
            }.filter { artist ->
                searchQuery.isBlank() ||
                    artist.name.contains(searchQuery, ignoreCase = true) ||
                    artist.tracks.any { it.title.contains(searchQuery, ignoreCase = true) || it.album.contains(searchQuery, ignoreCase = true) }
            }
    }

    AnimatedContent<MusicFolderItem?>(
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
        label = "MusicFolderNavigationTransition"
    ) { currentFolder: MusicFolderItem? ->
        if (currentFolder != null) {
            MusicOpenedFolderContent(
                folder = currentFolder,
                isGridView = isGridView,
                sortOrder = sortOrder,
                currentPlayingId = currentPlayingId,
                selectedMediaItems = selectedMediaItems,
                onPlayAudio = onPlayAudio,
                onPickFiles = onPickFiles,
                onShowOptions = { selectedTrackForOptions = it },
                onDeleteAudio = onDeleteAudio,
                onToggleSelectItem = onToggleSelectItem,
                onStartSelection = onStartSelection,
                context = context
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!hasPermission) {
                    PermissionOnboardingCard(
                        onRequestPermission = onRequestPermission,
                        onPickFiles = onPickFiles
                    )
                }

                MusicCategoryFilterPills(
                    filterTabs = filterTabs,
                    selectedTab = filterTab,
                    onFilterTabSelect = onFilterTabSelect
                )

                when (filterTab) {
                    "Folders" -> MusicCollectionSection(
                        title = "Folders (${folderItems.size})",
                        icon = Icons.Rounded.Folder,
                        items = folderItems,
                        keySelector = { it.path + "/" + it.name },
                        gridContentType = "folder_grid_card",
                        listContentType = "folder_list_card",
                        emptyTitle = "No music folders found",
                        emptySubtitle = "Pick files from storage or connect a USB OTG drive to start playing.",
                        isGridView = isGridView,
                        onToggleView = onToggleView,
                        onOpenItem = onOpenFolder,
                        onPickFiles = onPickFiles,
                        context = context
                    )

                    "Albums" -> MusicCollectionSection(
                        title = "Albums (${albumItems.size})",
                        icon = Icons.Rounded.Album,
                        items = albumItems,
                        keySelector = { it.name },
                        gridContentType = "album_grid_card",
                        listContentType = "album_list_card",
                        emptyTitle = "No albums found",
                        emptySubtitle = "Pick files from storage or add audio with album metadata.",
                        isGridView = isGridView,
                        onToggleView = onToggleView,
                        onOpenItem = onOpenFolder,
                        onPickFiles = onPickFiles,
                        context = context
                    )

                    "Artists" -> MusicCollectionSection(
                        title = "Artists (${artistItems.size})",
                        icon = Icons.Rounded.Person,
                        items = artistItems,
                        keySelector = { it.name },
                        gridContentType = "artist_grid_card",
                        listContentType = "artist_list_card",
                        emptyTitle = "No artists found",
                        emptySubtitle = "Pick files from storage or add audio with artist metadata.",
                        isGridView = isGridView,
                        onToggleView = onToggleView,
                        onOpenItem = onOpenFolder,
                        onPickFiles = onPickFiles,
                        context = context
                    )

                    else -> MusicTracksSection(
                        audioList = audioList,
                        filterTab = filterTab,
                        sortOrder = sortOrder,
                        isGridView = isGridView,
                        currentPlayingId = currentPlayingId,
                        selectedMediaItems = selectedMediaItems,
                        onToggleView = onToggleView,
                        onPlayAudio = onPlayAudio,
                        onShuffleAll = onShuffleAll,
                        onPickFiles = onPickFiles,
                        onShowOptions = { selectedTrackForOptions = it },
                        onDeleteAudio = onDeleteAudio,
                        onToggleSelectItem = onToggleSelectItem,
                        onStartSelection = onStartSelection,
                        context = context
                    )
                }
            }
        }
    }

    selectedTrackForOptions?.let { track ->
        val currentPlaylist = openedFolder?.tracks ?: audioList
        TrackOptionsMenuDialog(
            item = track,
            onPlay = {
                selectedTrackForOptions = null
                onPlayAudio(track, currentPlaylist)
            },
            onShowDetails = {
                selectedTrackForOptions = null
                selectedTrackDetails = track
            },
            onRename = {
                val target = selectedTrackForOptions
                selectedTrackForOptions = null
                if (target != null) onRenameAudio(target)
            },
            onShare = {
                selectedTrackForOptions = null
                shareMediaFiles(context, listOf(track))
            },
            onStartSelection = {
                selectedTrackForOptions = null
                onStartSelection(track)
            },
            onDelete = {
                selectedTrackForOptions = null
                onDeleteAudio(track)
            },
            onMoveToVault = {
                selectedTrackForOptions = null
                onMoveToVault(track)
            },
            onDismiss = { selectedTrackForOptions = null }
        )
    }

    selectedTrackDetails?.let { item ->
        MusicTrackDetailsDialog(
            item = item,
            onDismiss = { selectedTrackDetails = null }
        )
    }
}

@Composable
private fun MusicOpenedFolderContent(
    folder: MusicFolderItem,
    isGridView: Boolean,
    sortOrder: Int,
    currentPlayingId: Long?,
    selectedMediaItems: Set<MediaItemModel>,
    onPlayAudio: (MediaItemModel, List<MediaItemModel>) -> Unit,
    onPickFiles: () -> Unit,
    onShowOptions: (MediaItemModel) -> Unit,
    onDeleteAudio: (MediaItemModel) -> Unit,
    onToggleSelectItem: (MediaItemModel) -> Unit,
    onStartSelection: (MediaItemModel) -> Unit,
    context: Context
) {
    val sortedFolderTracks = remember(folder.tracks, sortOrder) {
        sortAudioTracks(folder.tracks, sortOrder)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (sortedFolderTracks.isEmpty()) {
            EmptyStateView(
                title = "No tracks in this folder",
                subtitle = "Pick files from storage or choose another folder.",
                onAction = onPickFiles
            )
        } else {
            MusicTrackGridOrList(
                tracks = sortedFolderTracks,
                isGridView = isGridView,
                currentPlayingId = currentPlayingId,
                selectedMediaItems = selectedMediaItems,
                onPlayAudio = { track -> onPlayAudio(track, sortedFolderTracks) },
                onShowOptions = onShowOptions,
                onDeleteAudio = onDeleteAudio,
                onToggleSelectItem = onToggleSelectItem,
                onStartSelection = onStartSelection,
                context = context
            )
        }
    }
}

@Composable
private fun MusicCollectionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<MusicFolderItem>,
    keySelector: (MusicFolderItem) -> String,
    gridContentType: String,
    listContentType: String,
    emptyTitle: String,
    emptySubtitle: String,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onOpenItem: (MusicFolderItem) -> Unit,
    onPickFiles: () -> Unit,
    context: Context
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
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

    val sectionKeys = remember(items, keySelector) {
        deduplicateLazyKeys(items, keySelector)
    }
    if (items.isEmpty()) {
        EmptyStateView(
            title = emptyTitle,
            subtitle = emptySubtitle,
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
            itemsIndexed(
                items = items,
                key = { index, _ -> sectionKeys[index] },
                contentType = { _, _ -> gridContentType }
            ) { _, folder ->
                MusicFolderGridItem(
                    folder = folder,
                    context = context,
                    onClick = { onOpenItem(folder) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = items,
                key = { index, _ -> sectionKeys[index] },
                contentType = { _, _ -> listContentType }
            ) { _, folder ->
                MusicFolderListItem(
                    folder = folder,
                    context = context,
                    onClick = { onOpenItem(folder) }
                )
            }
        }
    }
}

@Composable
private fun MusicTracksSection(
    audioList: List<MediaItemModel>,
    filterTab: String,
    sortOrder: Int,
    isGridView: Boolean,
    currentPlayingId: Long?,
    selectedMediaItems: Set<MediaItemModel>,
    onToggleView: () -> Unit,
    onPlayAudio: (MediaItemModel, List<MediaItemModel>) -> Unit,
    onShuffleAll: () -> Unit,
    onPickFiles: () -> Unit,
    onShowOptions: (MediaItemModel) -> Unit,
    onDeleteAudio: (MediaItemModel) -> Unit,
    onToggleSelectItem: (MediaItemModel) -> Unit,
    onStartSelection: (MediaItemModel) -> Unit,
    context: Context
) {
    val tracksToDisplay = remember(audioList, filterTab, sortOrder) {
        val baseList = if (filterTab == "Hi-Res / Lossless") {
            audioList.filter {
                it.bitDepth >= 24 || it.sampleRate >= 48000 ||
                    it.codecBadge.contains("Hi-Res", true) ||
                    it.codecBadge.contains("Lossless", true) ||
                    it.mimeType.contains("flac", true) ||
                    it.mimeType.contains("alac", true) ||
                    it.mimeType.contains("wav", true) ||
                    it.mimeType.contains("dsd", true)
            }
        } else {
            audioList
        }
        sortAudioTracks(baseList, sortOrder)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (tracksToDisplay.isNotEmpty()) {
                            onPlayAudio(tracksToDisplay.first(), tracksToDisplay)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play All",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play All",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .clickable(onClick = onShuffleAll)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Shuffle",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
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

    if (tracksToDisplay.isEmpty()) {
        EmptyStateView(
            title = "No tracks found",
            subtitle = "Grant permissions or pick audio files from custom folders/OTG drives.",
            onAction = onPickFiles
        )
    } else {
        MusicTrackGridOrList(
            tracks = tracksToDisplay,
            isGridView = isGridView,
            currentPlayingId = currentPlayingId,
            selectedMediaItems = selectedMediaItems,
            onPlayAudio = { track -> onPlayAudio(track, tracksToDisplay) },
            onShowOptions = onShowOptions,
            onDeleteAudio = onDeleteAudio,
            onToggleSelectItem = onToggleSelectItem,
            onStartSelection = onStartSelection,
            context = context
        )
    }
}

@Composable
private fun MusicTrackGridOrList(
    tracks: List<MediaItemModel>,
    isGridView: Boolean,
    currentPlayingId: Long?,
    selectedMediaItems: Set<MediaItemModel>,
    onPlayAudio: (MediaItemModel) -> Unit,
    onShowOptions: (MediaItemModel) -> Unit,
    onDeleteAudio: (MediaItemModel) -> Unit,
    onToggleSelectItem: (MediaItemModel) -> Unit,
    onStartSelection: (MediaItemModel) -> Unit,
    context: Context
) {
    val isSelectionMode = selectedMediaItems.isNotEmpty()
    val trackKeys = remember(tracks) {
        deduplicateLazyKeys(tracks) { it.id }
    }
    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = tracks,
                key = { index, _ -> trackKeys[index] },
                contentType = { _, _ -> "music_grid_card" }
            ) { _, track ->
                val isSelected = selectedMediaItems.contains(track)
                MusicCardItem(
                    item = track,
                    isPlaying = currentPlayingId == track.id,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    context = context,
                    onClick = {
                        if (isSelectionMode) onToggleSelectItem(track) else onPlayAudio(track)
                    },
                    onShowOptions = { onShowOptions(track) },
                    onDeleteClick = { onDeleteAudio(track) },
                    onLongClick = { onStartSelection(track) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = tracks,
                key = { index, _ -> trackKeys[index] },
                contentType = { _, _ -> "music_list_card" }
            ) { _, track ->
                val isSelected = selectedMediaItems.contains(track)
                MusicListItem(
                    item = track,
                    isPlaying = currentPlayingId == track.id,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    context = context,
                    onClick = {
                        if (isSelectionMode) onToggleSelectItem(track) else onPlayAudio(track)
                    },
                    onShowOptions = { onShowOptions(track) },
                    onDeleteClick = { onDeleteAudio(track) },
                    onLongClick = { onStartSelection(track) }
                )
            }
        }
    }
}

private fun sortAudioTracks(tracks: List<MediaItemModel>, sortOrder: Int): List<MediaItemModel> {
    return when (sortOrder) {
        1 -> tracks.sortedBy { it.title.lowercase() }
        2 -> tracks.sortedBy { it.artist.lowercase() }
        3 -> tracks.sortedByDescending { it.sizeBytes }
        4 -> tracks.sortedByDescending { it.durationMs }
        else -> tracks
    }
}

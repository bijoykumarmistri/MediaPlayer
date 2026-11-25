package com.mediaplayer.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.mediaplayer.distribution.DistributionPlatform
import com.mediaplayer.playback.StreamHistoryItem
import com.mediaplayer.util.AppUpdateManager
import com.mediaplayer.util.AudioArtworkFetcher
import com.mediaplayer.util.DiagnosticLogManager
import com.mediaplayer.util.VideoThumbnailFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MediaHubPage(
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onPlayStream: (url: String, title: String, isVideo: Boolean) -> Unit,
    recentStreams: List<StreamHistoryItem>,
    onDeleteRecentStream: (String) -> Unit,
    onClearAllRecentStreams: () -> Unit,
    onOpenPrivacyVault: () -> Unit = {},
    onCheckAppUpdate: (() -> Unit)? = null,
    videoTotalBytes: Long = 0L,
    audioTotalBytes: Long = 0L,
    vaultCount: Int = 0,
    initialTab: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) } // 0: Sources, 1: Stream, 2: About

    var streamUrlInput by remember { mutableStateOf("") }
    var streamTitleInput by remember { mutableStateOf("") }
    var isVideoStream by remember { mutableStateOf(true) }
    var userPinnedStreamType by remember { mutableStateOf(false) }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var clipboardBannerDismissed by remember { mutableStateOf(false) }

    val currentVersion = remember { AppUpdateManager.getCurrentVersionName(context) }

    var cacheBytes by remember { mutableLongStateOf(0L) }
    var isClearingCache by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            cacheBytes = withContext(Dispatchers.IO) {
                calculateDirSize(File(context.cacheDir, "coil_cache"))
            }
        }
    }

    @Suppress("HttpUrlsUsage")
    LaunchedEffect(selectedTab) {
        if (selectedTab != 1) return@LaunchedEffect
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val itemText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (!itemText.isNullOrBlank() &&
                (itemText.startsWith("http://", ignoreCase = true) ||
                    itemText.startsWith("https://", ignoreCase = true) ||
                    itemText.startsWith("rtsp://", ignoreCase = true) ||
                    itemText.startsWith("rtmp://", ignoreCase = true))
            ) {
                detectedClipboardUrl = itemText
            }
        } catch (_: Exception) {
        }
    }

    fun inferStreamIsVideo(url: String): Boolean {
        val clean = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            clean.endsWith(".mp3") || clean.endsWith(".flac") || clean.endsWith(".aac") ||
                clean.endsWith(".ogg") || clean.endsWith(".opus") || clean.endsWith(".wav") ||
                clean.endsWith(".m4a") -> false
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 3-Tab Segmented Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HubTabItem(
                selected = selectedTab == 0,
                title = "Sources",
                icon = Icons.Rounded.Folder,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            HubTabItem(
                selected = selectedTab == 1,
                title = if (recentStreams.isNotEmpty()) "Stream (${recentStreams.size})" else "Stream",
                icon = Icons.Rounded.Link,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1.1f)
            )
            HubTabItem(
                selected = selectedTab == 2,
                title = "About",
                icon = Icons.Rounded.Info,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(0.9f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "MediaHubTabContent"
        ) { tab ->
            when (tab) {
                0 -> {
                    SourcesAndStorageTab(
                        videoTotalBytes = videoTotalBytes,
                        audioTotalBytes = audioTotalBytes,
                        vaultCount = vaultCount,
                        cacheBytes = cacheBytes,
                        isClearingCache = isClearingCache,
                        onPickFiles = onPickFiles,
                        onPickFolder = onPickFolder,
                        onOpenPrivacyVault = onOpenPrivacyVault,
                        onClearCache = {
                            if (!isClearingCache) {
                                isClearingCache = true
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        AudioArtworkFetcher.clearMemoryCache()
                                        VideoThumbnailFetcher.clearMemoryCache()
                                        try {
                                            val loader = Coil.imageLoader(context)
                                            loader.memoryCache?.clear()
                                            loader.diskCache?.clear()
                                        } catch (_: Exception) {
                                        }
                                        File(context.cacheDir, "coil_cache").deleteRecursively()
                                    }
                                    cacheBytes = 0L
                                    isClearingCache = false
                                    Toast.makeText(context, "Thumbnail & artwork cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                1 -> {
                    StreamHubTab(
                        streamUrlInput = streamUrlInput,
                        onUrlChange = { newUrl ->
                            streamUrlInput = newUrl
                            if (!userPinnedStreamType && newUrl.isNotBlank()) {
                                isVideoStream = inferStreamIsVideo(newUrl)
                            }
                        },
                        streamTitleInput = streamTitleInput,
                        onTitleChange = { streamTitleInput = it },
                        isVideoStream = isVideoStream,
                        onStreamTypeChange = { isVid ->
                            userPinnedStreamType = true
                            isVideoStream = isVid
                        },
                        detectedClipboardUrl = if (clipboardBannerDismissed) null else detectedClipboardUrl,
                        onDismissClipboardBanner = { clipboardBannerDismissed = true },
                        onPasteClipboard = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val paste = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!paste.isNullOrBlank()) {
                                    streamUrlInput = paste
                                    if (!userPinnedStreamType) {
                                        isVideoStream = inferStreamIsVideo(paste)
                                    }
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                            }
                        },
                        onPlayCurrent = {
                            val trimmed = streamUrlInput.trim()
                            if (trimmed.isNotEmpty()) {
                                onPlayStream(
                                    trimmed,
                                    streamTitleInput.trim().ifBlank { "Direct Network Stream" },
                                    isVideoStream
                                )
                            }
                        },
                        recentStreams = recentStreams,
                        onPlayRecent = { item ->
                            onPlayStream(item.url, item.title, item.isVideo)
                        },
                        onEditRecent = { item ->
                            streamUrlInput = item.url
                            streamTitleInput = item.title
                            isVideoStream = item.isVideo
                            userPinnedStreamType = true
                        },
                        onDeleteRecentStream = onDeleteRecentStream,
                        onClearAllRecentStreams = onClearAllRecentStreams
                    )
                }
                else -> {
                    AboutAppSection(
                        currentVersion = currentVersion,
                        onCheckAppUpdate = onCheckAppUpdate
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SourcesAndStorageTab(
    videoTotalBytes: Long,
    audioTotalBytes: Long,
    vaultCount: Int,
    cacheBytes: Long,
    isClearingCache: Boolean,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onOpenPrivacyVault: () -> Unit,
    onClearCache: () -> Unit,
) {
    val totalMediaBytes = (videoTotalBytes + audioTotalBytes).coerceAtLeast(0L)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // 1. External & Secure Sources (Exclusive to Media Hub)
        HubSection(title = "EXTERNAL & SECURE SOURCES") {
            HubActionRow(
                title = "Open Media Files",
                subtitle = "Play video or audio directly from storage & USB OTG",
                icon = Icons.Rounded.FileOpen,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                onClick = onPickFiles
            )
            HubRowDivider()
            HubActionRow(
                title = "Scan Folder / SD Card",
                subtitle = "Index a custom directory or external card into library",
                icon = Icons.Rounded.FolderOpen,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onPickFolder
            )
            HubRowDivider()
            HubActionRow(
                title = "Privacy Vault",
                subtitle = if (vaultCount > 0) {
                    "$vaultCount encrypted ${if (vaultCount == 1) "item" else "items"} • PIN & Biometric lock"
                } else {
                    "AES-256 hidden media safe • PIN & Biometric lock"
                },
                icon = Icons.Rounded.Lock,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                trailingBadge = if (vaultCount > 0) vaultCount.toString() else null,
                onClick = onOpenPrivacyVault
            )
        }

        // 2. Storage & Cache Management (Exclusive to Media Hub)
        HubSection(title = "STORAGE & CACHE") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Indexed Media Footprint",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalMediaBytes > 0L) formatFileSize(totalMediaBytes) else "0.0 MB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Proportional Video vs Audio storage bar
                    val videoRatio = if (totalMediaBytes > 0L) {
                        (videoTotalBytes.toFloat() / totalMediaBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        if (videoRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(videoRatio.coerceAtLeast(0.02f))
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (1f - videoRatio > 0.01f && totalMediaBytes > 0L) {
                            Box(
                                modifier = Modifier
                                    .weight((1f - videoRatio).coerceAtLeast(0.02f))
                                    .height(6.dp)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Videos: ${if (videoTotalBytes > 0L) formatFileSize(videoTotalBytes) else "0.0 MB"}  •  Music: ${if (audioTotalBytes > 0L) formatFileSize(audioTotalBytes) else "0.0 MB"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            HubRowDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isClearingCache && cacheBytes > 0L) { onClearCache() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thumbnail & Artwork Cache",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (cacheBytes > 0L) {
                            "${formatFileSize(cacheBytes)} cached on disk"
                        } else {
                            "Cache is clean"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (cacheBytes > 0L) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isClearingCache) { onClearCache() }
                ) {
                    Text(
                        text = if (isClearingCache) "Clearing..." else "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cacheBytes > 0L) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamHubTab(
    streamUrlInput: String,
    onUrlChange: (String) -> Unit,
    streamTitleInput: String,
    onTitleChange: (String) -> Unit,
    isVideoStream: Boolean,
    onStreamTypeChange: (Boolean) -> Unit,
    detectedClipboardUrl: String?,
    onDismissClipboardBanner: () -> Unit,
    onPasteClipboard: () -> Unit,
    onPlayCurrent: () -> Unit,
    recentStreams: List<StreamHistoryItem>,
    onPlayRecent: (StreamHistoryItem) -> Unit,
    onEditRecent: (StreamHistoryItem) -> Unit,
    onDeleteRecentStream: (String) -> Unit,
    onClearAllRecentStreams: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Smart Clipboard Banner
        AnimatedVisibility(visible = (detectedClipboardUrl != null) && streamUrlInput.isBlank()) {
            detectedClipboardUrl?.let { clipUrl ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUrlChange(clipUrl) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link detected in clipboard",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = clipUrl,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { onUrlChange(clipUrl) }) {
                            Text("Paste", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = onDismissClipboardBanner,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Stream Composer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Network Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "HLS • DASH • RTSP • MP4 • FLAC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = streamUrlInput,
                    onValueChange = onUrlChange,
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/stream.m3u8") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (streamUrlInput.isNotEmpty()) {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(onClick = onPasteClipboard) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )

                OutlinedTextField(
                    value = streamTitleInput,
                    onValueChange = onTitleChange,
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("Custom Stream Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = isVideoStream,
                        onClick = { onStreamTypeChange(true) },
                        label = { Text("Video Stream", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !isVideoStream,
                        onClick = { onStreamTypeChange(false) },
                        label = { Text("Audio / Radio", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = onPlayCurrent,
                    enabled = streamUrlInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideoStream) "Play Video Stream" else "Play Audio Stream",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Recent Streams History
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (recentStreams.isNotEmpty()) "RECENT STREAMS (${recentStreams.size})" else "RECENT STREAMS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                if (recentStreams.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAllRecentStreams,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (recentStreams.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "No recent streams yet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Played URLs will be saved here for 1-tap replay or editing.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                ) {
                    recentStreams.forEachIndexed { index, item ->
                        if (index > 0) {
                            HubRowDivider()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayRecent(item) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isVideo) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = if (item.isVideo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.url,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.timestamp > 0L) {
                                    val relTime = remember(item.timestamp) {
                                        DateUtils.getRelativeTimeSpanString(
                                            item.timestamp,
                                            System.currentTimeMillis(),
                                            DateUtils.MINUTE_IN_MILLIS,
                                            DateUtils.FORMAT_ABBREV_RELATIVE
                                        ).toString()
                                    }
                                    Text(
                                        text = "${if (item.isVideo) "Video" else "Audio"} • $relTime",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onEditRecent(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit in URL field",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteRecentStream(item.url) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compact Sample Streams Strip
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SAMPLE TEST STREAMS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Big Buck Bunny (HLS)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", true),
                    Triple("Oceans (MP4)", "https://vjs.zencdn.net/v/oceans.mp4", true),
                    Triple("Akamai Live (HLS)", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", true),
                    Triple("Web Radio Test", "https://stream.zeno.fm/f3wvbbqmdg8uv", false)
                ).forEach { (title, url, isVid) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                onUrlChange(url)
                                onTitleChange(title)
                                onStreamTypeChange(isVid)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isVid) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = if (isVid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutAppSection(
    currentVersion: String,
    onCheckAppUpdate: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isExportingLogs by remember { mutableStateOf(false) }
    var hasCrashLog by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        hasCrashLog = withContext(Dispatchers.IO) {
            DiagnosticLogManager.hasSavedCrashReport(context)
        }
    }

    val appIcon = remember(context) {
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(context.applicationInfo)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
            val bitmap = createBitmap(width, height)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Clean App Hero & Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = "BKM Player",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "BKM Player",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "BKM Player",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = DistributionPlatform.DEVELOPER_SUBTITLE,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("100% Free", "Ad-Free", "No Subscriptions").forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            if (onCheckAppUpdate != null) {
                                onCheckAppUpdate()
                                isCheckingUpdate = false
                            } else {
                                scope.launch {
                                    val result = DistributionPlatform.checkForUpdates(context, isManual = true)
                                    isCheckingUpdate = false
                                    result.onSuccess { info ->
                                        val msg = if (info?.isUpdateAvailable == true) {
                                            "New version ${info.tagName} available!"
                                        } else {
                                            "You are on the latest version v$currentVersion"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            "Update check failed: ${err.localizedMessage ?: "Network error"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking Updates...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DistributionPlatform.openPrimaryAction(context)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Launch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            DistributionPlatform.primaryActionLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "BKM Player")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Check out BKM Player: ${DistributionPlatform.shareUrl}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share BKM Player"))
                            } catch (_: Exception) {
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share App", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Distribution-specific Support & Developer Links Cards
        DistributionPlatform.SupportAndDeveloperCards()

        // 3. Diagnostics & Debug Logs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Diagnostics & Debug Logs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasCrashLog) {
                                "Crash report captured • Tap below to share"
                            } else {
                                "Device specs, codecs & recent logs (no personal media)"
                            },
                            fontSize = 12.sp,
                            color = if (hasCrashLog) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (!isExportingLogs) {
                            isExportingLogs = true
                            scope.launch {
                                val result = DiagnosticLogManager.exportDiagnosticReport(context)
                                isExportingLogs = false
                                result.onSuccess { file ->
                                    try {
                                        DiagnosticLogManager.shareDiagnosticFile(context, file)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Unable to open share sheet: ${e.localizedMessage ?: "error"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }.onFailure { err ->
                                    Toast.makeText(
                                        context,
                                        "Failed to collect logs: ${err.localizedMessage ?: "error"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isExportingLogs) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Collecting Logs...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Debug Logs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HubSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun HubRowDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun HubActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    trailingBadge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingBadge != null) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = trailingBadge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HubTabItem(
    selected: Boolean,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun calculateDirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    var total = 0L
    dir.walkTopDown().forEach { file ->
        if (file.isFile) {
            total += file.length()
        }
    }
    return total
}

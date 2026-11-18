package com.mediaplayer.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.playback.StreamHistoryItem
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.util.AppUpdateManager

@Composable
fun MediaHubPage(
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onPlayStream: (url: String, title: String, isVideo: Boolean) -> Unit,
    recentStreams: List<StreamHistoryItem>,
    onDeleteRecentStream: (String) -> Unit,
    onClearAllRecentStreams: () -> Unit,
    onOpenAboutDialog: () -> Unit = {},
    onOpenPrivacyVault: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local, 1: Stream, 2: History

    var streamUrlInput by remember { mutableStateOf("") }
    var streamTitleInput by remember { mutableStateOf("") }
    var isVideoStream by remember { mutableStateOf(true) }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }

    // Auto-inspect system clipboard for streaming links
    LaunchedEffect(Unit) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Modern Segmented Tab Bar for Media Hub
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141722))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HubTabItem(
                selected = selectedTab == 0,
                title = "Local Storage",
                icon = Icons.Rounded.Folder,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            HubTabItem(
                selected = selectedTab == 1,
                title = "Network Stream",
                icon = Icons.Rounded.Link,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            if (recentStreams.isNotEmpty()) {
                HubTabItem(
                    selected = selectedTab == 2,
                    title = "History (${recentStreams.size})",
                    icon = Icons.Rounded.History,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1.1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Switcher (Zero transition latency)
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            label = "MediaHubTabContent"
        ) { tab ->
            when (tab) {
                0 -> {
                    // LOCAL STORAGE & USB-OTG
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 1. Pick Files Tile
                        HubSourceCard(
                            title = "Browse Media Files",
                            subtitle = "Open video, lossless FLAC, ALAC, WAV, DSD, or USB-OTG files",
                            icon = Icons.Rounded.FileOpen,
                            iconTint = AudiophileCyan,
                            iconBg = Color(0x2600E5FF),
                            onClick = onPickFiles
                        )

                        // 2. Pick Folder / SD Card Tile
                        HubSourceCard(
                            title = "Scan Folder / SD Card",
                            subtitle = "Index custom directory trees, micro-SD cards, or external drives",
                            icon = Icons.Rounded.FolderOpen,
                            iconTint = AudiophileGold,
                            iconBg = Color(0x26FFD54F),
                            onClick = onPickFolder
                        )

                        // 3. Privacy Folder / Secure Vault Tile
                        HubSourceCard(
                            title = "Privacy Vault",
                            subtitle = "Password-protected folder for sensitive video and audio files",
                            icon = Icons.Rounded.Lock,
                            iconTint = AudiophileGold,
                            iconBg = Color(0x26FFD54F),
                            onClick = onOpenPrivacyVault
                        )

                        // 4. App Updates & GitHub Tile
                        val currentVersion = remember { AppUpdateManager.getCurrentVersionName(context) }
                        HubSourceCard(
                            title = "App Updates & Releases",
                            subtitle = "Installed: v$currentVersion • Check for latest version on GitHub",
                            icon = Icons.Rounded.RocketLaunch,
                            iconTint = AudiophileCyan,
                            iconBg = Color(0x2600E5FF),
                            onClick = onOpenAboutDialog
                        )

                        // Supported Decoders Info Pill
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10121A)),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AudiophileCyan)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "UNIVERSAL MEDIA DECODING ENGINE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AudiophileCyan,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Equipped with Jellyfin libffmpeg 6.0 native core and Snapdragon MediaCodec hardware accelerators. Supports direct playback of MKV, MP4, FLAC, ALAC, DSD, TrueHD, AC3, AV1, and HEVC without transcoding.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E9EAE),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // DIRECT NETWORK STREAM
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Smart Clipboard Detection Banner
                        AnimatedVisibility(
                            visible = detectedClipboardUrl != null && streamUrlInput.isBlank()
                        ) {
                            detectedClipboardUrl?.let { clipUrl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            streamUrlInput = clipUrl
                                            detectedClipboardUrl = null
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x1FFF6D00)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0x66FF6D00))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentPaste,
                                                contentDescription = null,
                                                tint = Color(0xFFFF9100),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Stream URL detected in clipboard",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFD180)
                                                )
                                                Text(
                                                    text = clipUrl,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFB0B0C0),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Tap to Paste",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AudiophileGold,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Stream URL Field
                        OutlinedTextField(
                            value = streamUrlInput,
                            onValueChange = { streamUrlInput = it },
                            label = { Text("Stream URL (HLS / DASH / MP4 / FLAC / RTSP)") },
                            placeholder = { Text("https://example.com/live/master.m3u8", color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = AudiophileGold
                                )
                            },
                            trailingIcon = {
                                if (streamUrlInput.isNotEmpty()) {
                                    IconButton(onClick = { streamUrlInput = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.Gray
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val paste = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                            if (!paste.isNullOrBlank()) {
                                                streamUrlInput = paste
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = AudiophileCyan
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AudiophileGold,
                                unfocusedBorderColor = OledBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color(0x05FFFFFF)
                            )
                        )

                        // Stream Title Field (Optional)
                        OutlinedTextField(
                            value = streamTitleInput,
                            onValueChange = { streamTitleInput = it },
                            label = { Text("Stream Title (Optional)") },
                            placeholder = { Text("Custom Stream Name", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AudiophileCyan,
                                unfocusedBorderColor = OledBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color(0x05FFFFFF)
                            )
                        )

                        // Stream Mode Selector (Video vs Audio)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = isVideoStream,
                                onClick = { isVideoStream = true },
                                label = { Text("Video Stream (Visual Surface)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0x3300E5FF),
                                    selectedLabelColor = AudiophileCyan,
                                    selectedLeadingIconColor = AudiophileCyan
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = !isVideoStream,
                                onClick = { isVideoStream = false },
                                label = { Text("Audio / Web Radio") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0x33FFD54F),
                                    selectedLabelColor = AudiophileGold,
                                    selectedLeadingIconColor = AudiophileGold
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }


                        // Play Button
                        Button(
                            onClick = {
                                if (streamUrlInput.isNotBlank()) {
                                    onPlayStream(
                                        streamUrlInput.trim(),
                                        streamTitleInput.ifBlank { "Direct Network Stream" },
                                        isVideoStream
                                    )
                                }
                            },
                            enabled = streamUrlInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AudiophileGold,
                                disabledContainerColor = Color(0x33888888)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Play Live Stream Now",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                2 -> {
                    // STREAM HISTORY
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (recentStreams.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent network streams recorded.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888899)
                                )
                            }
                        } else {
                            recentStreams.forEach { streamItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onPlayStream(streamItem.url, streamItem.title, streamItem.isVideo)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (streamItem.isVideo) Color(0x2600E5FF) else Color(0x26FFD54F)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (streamItem.isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (streamItem.isVideo) AudiophileCyan else AudiophileGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = streamItem.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = streamItem.url,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF888899),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteRecentStream(streamItem.url) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = Color(0xFF888899),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onClearAllRecentStreams,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = "Clear Stream History",
                                    color = Color(0xFFE57373),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun HubTabItem(
    selected: Boolean,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x2E00E5FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AudiophileCyan else Color(0xFF888899),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color(0xFF888899),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HubSourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9EAE),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


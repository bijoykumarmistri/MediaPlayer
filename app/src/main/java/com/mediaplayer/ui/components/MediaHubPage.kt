package com.mediaplayer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mediaplayer.R
import com.mediaplayer.playback.StreamHistoryItem
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.util.AppUpdateManager
import kotlinx.coroutines.launch

private const val HUB_DEVELOPER_NAME = "BKM"
private const val HUB_DEVELOPER_UPI_ID = "bijoymistri367@okaxis"

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
    initialTab: Int = 0,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: Storage, 1: Stream, 2: History, 3: About

    var streamUrlInput by remember { mutableStateOf("") }
    var streamTitleInput by remember { mutableStateOf("") }
    var isVideoStream by remember { mutableStateOf(value = true) }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    val currentVersion = remember { AppUpdateManager.getCurrentVersionName(context) }

    // Auto-inspect system clipboard for streaming links
    @Suppress("HttpUrlsUsage")
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

    // Helper functions for UPI payments
    fun copyUpiId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Developer UPI ID", HUB_DEVELOPER_UPI_ID)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "UPI ID copied: $HUB_DEVELOPER_UPI_ID", Toast.LENGTH_SHORT).show()
    }

    fun openUpiPayment(amount: String? = null) {
        try {
            val amountParam = if (amount.isNullOrBlank()) "" else "&am=$amount"
            val uri = "upi://pay?pa=$HUB_DEVELOPER_UPI_ID&pn=$HUB_DEVELOPER_NAME&tn=Support%20Media%20Player$amountParam&cu=INR".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyUpiId()
            Toast.makeText(context, "No UPI app detected. UPI ID copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Modern Segmented Navigation Bar for Media Hub
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF12141F))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HubTabItem(
                selected = selectedTab == 0,
                title = "Storage",
                icon = Icons.Rounded.Folder,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            HubTabItem(
                selected = selectedTab == 1,
                title = "Stream",
                icon = Icons.Rounded.Link,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            HubTabItem(
                selected = selectedTab == 2,
                title = if (recentStreams.isNotEmpty()) "History (${recentStreams.size})" else "History",
                icon = Icons.Rounded.History,
                onClick = { selectedTab = 2 },
                modifier = Modifier.weight(1.05f)
            )
            HubTabItem(
                selected = selectedTab == 3,
                title = "About",
                icon = Icons.Rounded.Info,
                badgeColor = AudiophileGold,
                onClick = { selectedTab = 3 },
                modifier = Modifier.weight(0.95f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Switcher
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            label = "MediaHubTabContent"
        ) { tab ->
            when (tab) {
                0 -> {
                    // TAB 0: LOCAL STORAGE (Clean & Focused)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 1. Browse Files Tile
                        HubSourceCard(
                            title = "Browse Media Files",
                            subtitle = "Directly open audio or video from storage or USB",
                            icon = Icons.Rounded.FileOpen,
                            iconTint = AudiophileCyan,
                            iconBg = Color(0x2600E5FF),
                            tags = listOf("MP4", "MKV", "FLAC", "MP3", "WAV"),
                            onClick = onPickFiles
                        )

                        // 2. Pick Folder / SD Card Tile
                        HubSourceCard(
                            title = "Scan Folder / SD Card",
                            subtitle = "Index specific folder or external storage into library",
                            icon = Icons.Rounded.FolderOpen,
                            iconTint = AudiophileGold,
                            iconBg = Color(0x26FFD54F),
                            tags = listOf("Directory Scan", "SD Card", "Auto-Index"),
                            onClick = onPickFolder
                        )

                        // 3. Privacy Folder / Secure Vault Tile
                        HubSourceCard(
                            title = "Privacy Vault",
                            subtitle = "Encrypted local media safe • PIN & Biometric lock",
                            icon = Icons.Rounded.Lock,
                            iconTint = AudiophileGreen,
                            iconBg = Color(0x2600E676),
                            tags = listOf("AES-256", "Hidden Storage", "Zero Cloud"),
                            onClick = onOpenPrivacyVault
                        )
                    }
                }
                1 -> {
                    // TAB 1: DIRECT NETWORK STREAM
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Supported Protocol Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("HLS (.m3u8)", "DASH (.mpd)", "Direct MP4", "FLAC Audio", "RTSP / RTMP").forEach { proto ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x1A00E5FF))
                                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = proto,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AudiophileCyan
                                    )
                                }
                            }
                        }

                        // Smart Clipboard Detection Banner
                        AnimatedVisibility(
                            visible = (detectedClipboardUrl != null) && streamUrlInput.isBlank()
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
                                    IconButton(
                                        onClick = {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val paste = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                                if (!paste.isNullOrBlank()) {
                                                    streamUrlInput = paste
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    ) {
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
                                label = { Text("Video Stream") },
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
                                label = { Text("Audio / Radio") },
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

                        // Quick Test Streams Presets
                        Text(
                            text = "QUICK TEST PRESETS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA0A5B5),
                            letterSpacing = 1.sp
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x14FFFFFF))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable {
                                            streamUrlInput = url
                                            streamTitleInput = title
                                            isVideoStream = isVid
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isVid) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = if (isVid) AudiophileCyan else AudiophileGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

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
                    // TAB 2: STREAM HISTORY
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (recentStreams.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10121C)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x1A00E5FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = AudiophileCyan,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Stream History Yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Direct network streams you play will appear here for 1-tap instant replay.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888899),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = { selectedTab = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Open Stream Player", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT STREAMS (${recentStreams.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA0A5B5),
                                    letterSpacing = 1.sp
                                )
                                TextButton(onClick = onClearAllRecentStreams) {
                                    Text(
                                        text = "Clear All",
                                        color = Color(0xFFE57373),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            recentStreams.forEach { (streamUrl, streamTitle, isItemVideo) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onPlayStream(streamUrl, streamTitle, isItemVideo)
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
                                                        if (isItemVideo) Color(0x2600E5FF) else Color(0x26FFD54F)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isItemVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (isItemVideo) AudiophileCyan else AudiophileGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = streamTitle,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = streamUrl,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF888899),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onPlayStream(streamUrl, streamTitle, isItemVideo) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = AudiophileCyan,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteRecentStream(streamUrl) },
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
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: ABOUT APP & DEVELOPER
                    AboutAppSection(
                        currentVersion = currentVersion,
                        onCheckAppUpdate = onCheckAppUpdate,
                        onCopyUpi = { copyUpiId() },
                        onPayUpi = { amount -> openUpiPayment(amount) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * Clean, modern About section for Media Player.
 */
@Composable
private fun AboutAppSection(
    currentVersion: String,
    onCheckAppUpdate: (() -> Unit)?,
    onCopyUpi: () -> Unit,
    onPayUpi: (amount: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isUpiCopied by remember { mutableStateOf(false) }

    // Crash-proof, official launcher icon extraction
    val appIcon = remember(context) {
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(context.applicationInfo)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. APP BRAND HEADER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F111C)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x3300E5FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo matching official launcher icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141724)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = "Media Player",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Media Player",
                            tint = AudiophileCyan,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Media Player",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version $currentVersion",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0C0),
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A clean, private, and modern audio and video player for Android.",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9EAE),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. DEVELOPER PROFILE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111422)),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0x33FFD54F))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Monogram "BKM"
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AudiophileCyan, AudiophileGold)
                                    )
                                )
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0C0D14)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = HUB_DEVELOPER_NAME,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = AudiophileCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = HUB_DEVELOPER_NAME,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2200E5FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "DEVELOPER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AudiophileCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lead Developer",
                                fontSize = 11.sp,
                                color = Color(0xFFB0B0C0)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🇮🇳 India",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x1AFFFFFF))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Developed by BKM using Kotlin, Jetpack Compose, and AndroidX Media3.",
                    fontSize = 12.sp,
                    color = Color(0xFFC0C0D0),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Check out Media Player")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out Media Player by $HUB_DEVELOPER_NAME: ${AppUpdateManager.GITHUB_RELEASES_WEB_URL}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media Player"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Media Player", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. SUPPORT & DONATION CARD (UPI)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16130D)),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0x55FFD54F))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFD54F))
                            .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = AudiophileGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Support Development ☕",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AudiophileGold
                        )
                        Text(
                            text = "100% Free & Ad-Free",
                            fontSize = 11.sp,
                            color = Color(0xFFC0A870)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Media Player is completely free and ad-free. If you find this app helpful, consider supporting future development with a donation via UPI!",
                    fontSize = 12.sp,
                    color = Color(0xFFD4C8B0),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive UPI ID Pill (Tap to Copy)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x44000000))
                        .border(1.dp, Color(0x44FFD54F), RoundedCornerShape(14.dp))
                        .clickable {
                            onCopyUpi()
                            isUpiCopied = true
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DEVELOPER UPI ID (TAP TO COPY)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9E8E6A),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = HUB_DEVELOPER_UPI_ID,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isUpiCopied) Color(0x3300E676) else Color(0x22FFD54F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUpiCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                contentDescription = "Copy UPI ID",
                                tint = if (isUpiCopied) AudiophileGreen else AudiophileGold,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Accepted via Google Pay, PhonePe, Paytm, BHIM & any UPI app",
                    fontSize = 10.sp,
                    color = Color(0xFF9E8E6A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Donation Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("50" to "₹50 ☕", "100" to "₹100 ☕", "250" to "₹250 🍕", "500" to "₹500 🚀").forEach { (amount, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFD54F))
                                .border(1.dp, Color(0x44FFD54F), RoundedCornerShape(10.dp))
                                .clickable { onPayUpi(amount) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AudiophileGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Donate Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPayUpi(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AudiophileGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay via UPI App", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onCopyUpi()
                            isUpiCopied = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AudiophileGold),
                        border = BorderStroke(1.dp, AudiophileGold.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            tint = AudiophileGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Copy ID", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. SOFTWARE UPDATES & RELEASES
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131E)),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0x3300E5FF))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2200E5FF))
                            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = AudiophileCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Software Updates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "v$currentVersion • GitHub Releases",
                            fontSize = 11.sp,
                            color = Color(0xFFA0A5B5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isCheckingUpdate) {
                                isCheckingUpdate = true
                                if (onCheckAppUpdate != null) {
                                    onCheckAppUpdate()
                                    isCheckingUpdate = false
                                } else {
                                    scope.launch {
                                        val result = AppUpdateManager.checkForUpdates(context)
                                        isCheckingUpdate = false
                                        result.onSuccess { info ->
                                            if (info.isUpdateAvailable) {
                                                Toast.makeText(context, "New version ${info.tagName} available!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "You are on the latest version v$currentVersion", Toast.LENGTH_SHORT).show()
                                            }
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Update check failed: ${err.localizedMessage ?: "Network error"}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AudiophileCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Updates", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            AppUpdateManager.openUrl(context, AppUpdateManager.GITHUB_RELEASES_WEB_URL)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AudiophileCyan),
                        border = BorderStroke(1.dp, AudiophileCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Launch,
                            contentDescription = null,
                            tint = AudiophileCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Releases", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer & Copyright
        Text(
            text = "Crafted with ❤️ by $HUB_DEVELOPER_NAME • Open Source",
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
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
    badgeColor: Color? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x2E00E5FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AudiophileCyan else (badgeColor ?: Color(0xFF888899)),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color(0xFF888899),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    tags: List<String>? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            if (!tags.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x14FFFFFF))
                                .border(0.8.dp, Color(0x1FFFFFFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB0B0C0)
                            )
                        }
                    }
                }
            }
        }
    }
}

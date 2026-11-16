package com.mediaplayer.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.media.MediaMetadataRetriever
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.request.videoFrameOption
import com.mediaplayer.audio.UsbAudioTelemetry
import com.mediaplayer.audio.UsbBitPerfectAudioController
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.data.MediaRepository
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.components.UsbDacSettingsDialog
import com.mediaplayer.ui.components.UsbTelemetryPill
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurface
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.ui.video.formatTimeMs
import kotlinx.coroutines.launch

@Composable
fun MediaLibraryScreen(
    repository: MediaRepository,
    playerManager: MediaPlayerManager,
    usbController: UsbBitPerfectAudioController,
    onPlayItem: (MediaItemModel, List<MediaItemModel>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val telemetry by usbController.telemetry.collectAsState()
    val currentPlayingItem by playerManager.currentMediaItem.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var audioList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var videoList by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showDacDialog by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        val videoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] == true
        val storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val partialGranted = if (Build.VERSION.SDK_INT >= 34) {
            permissions["android.permission.READ_MEDIA_VISUAL_USER_SELECTED"] == true
        } else false

        hasPermission = audioGranted || videoGranted || storageGranted || partialGranted

        scope.launch {
            audioList = repository.getAudioList()
            videoList = repository.getVideoList()
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val item = repository.importSafSingleFile(uri)
                if (item != null) {
                    if (item.isVideo) {
                        videoList = listOf(item) + videoList
                    } else {
                        audioList = listOf(item) + audioList
                    }
                    onPlayItem(item, if (item.isVideo) videoList else audioList)
                }
            }
        }
    }

    val openFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            scope.launch {
                val (audios, videos) = repository.scanSafFolder(treeUri)
                if (audios.isNotEmpty()) audioList = audios + audioList
                if (videos.isNotEmpty()) videoList = videos + videoList
            }
        }
    }

    fun loadMedia() {
        scope.launch {
            audioList = repository.getAudioList()
            videoList = repository.getVideoList()
        }
    }

    LaunchedEffect(Unit) {
        loadMedia()
    }

    val filteredAudio = remember(audioList, searchQuery) {
        if (searchQuery.isBlank()) audioList
        else audioList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredVideo = remember(videoList, searchQuery) {
        if (searchQuery.isBlank()) videoList
        else videoList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledSurface)
            .statusBarsPadding()
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Media Player",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Hi-Res Video & Audio Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9EAE)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // USB Telemetry Pill
                UsbTelemetryPill(
                    telemetry = telemetry,
                    onClick = { showDacDialog = true },
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(onClick = { openFileLauncher.launch(arrayOf("audio/*", "video/*")) }) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = "Open Media File",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { openFolderLauncher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = "Scan Folder",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { loadMedia() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Library",
                        tint = Color.White
                    )
                }
            }
        }

        // Search Bar
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search titles, artists, albums...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AudiophileCyan,
                    unfocusedBorderColor = OledBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Granular Permissions Banner
        if (!hasPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1F00E5FF)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = AudiophileCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Grant Storage Permission",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Enable access to local video & high-res music on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC0C0D0)
                        )
                    }
                    Button(
                        onClick = {
                            val perms = if (Build.VERSION.SDK_INT >= 34) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                                )
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.READ_MEDIA_VIDEO
                                )
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(perms)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = OledSurface,
            contentColor = AudiophileCyan,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = AudiophileCyan,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Videos (${filteredVideo.size})", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Music (${filteredAudio.size})", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = {
                    selectedTabIndex = 2
                    showDacDialog = true
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("USB DAC", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> {
                    // Videos Tab
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredVideo, key = { it.id }) { video ->
                            VideoGridItem(
                                item = video,
                                isPlaying = currentPlayingItem?.id == video.id,
                                onClick = { onPlayItem(video, filteredVideo) }
                            )
                        }
                    }
                }
                1 -> {
                    // Music Tab
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAudio, key = { it.id }) { audio ->
                            MusicListItem(
                                item = audio,
                                isPlaying = currentPlayingItem?.id == audio.id,
                                onClick = { onPlayItem(audio, filteredAudio) }
                            )
                        }
                    }
                }
                2 -> {
                    // USB DAC hardware status info view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Usb,
                                contentDescription = null,
                                tint = AudiophileCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = telemetry.dacName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (telemetry.isBitPerfectActive) "Bit-Perfect USB-C Audio Active" else "Standard Audio Engine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (telemetry.isBitPerfectActive) AudiophileGold else Color(0xFF9E9EAE)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showDacDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Configure Bit-Perfect DAC", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDacDialog) {
        UsbDacSettingsDialog(
            telemetry = telemetry,
            controller = usbController,
            onDismiss = { showDacDialog = false }
        )
    }
}

@Composable
private fun VideoGridItem(
    item: MediaItemModel,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isPlaying) AudiophileCyan else OledBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF1B1C24)),
                contentAlignment = Alignment.Center
            ) {
                if (item.artworkUri != null) {
                    val context = LocalContext.current
                    val imageRequest = remember(item.artworkUri, item.id) {
                        ImageRequest.Builder(context)
                            .data(item.artworkUri)
                            .size(360, 200)
                            .diskCacheKey("vthumb_${item.id}")
                            .memoryCacheKey("vthumb_${item.id}")
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color(0xFF6E6E80),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play icon overlay
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

                // Duration badge
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

                // Resolution badge if present
                if (item.resolution.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC00E5FF))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.resolution.contains("3840") || item.resolution.contains("4096")) "4K UHD" else "HD",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.folderName.ifEmpty { "Videos" }} • ${formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MusicListItem(
    item: MediaItemModel,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isPlaying) Color(0x1A00E5FF) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E202B)),
            contentAlignment = Alignment.Center
        ) {
            if (item.artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = if (isPlaying) AudiophileCyan else Color(0xFF707080),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlaying) AudiophileCyan else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // High-res audio / codec badge
                val badgeText = item.codecBadge.ifEmpty {
                    if (item.sampleRate >= 48000 || item.bitDepth >= 24) {
                        "${item.bitDepth}b/${UsbAudioTelemetry.formatSampleRate(item.sampleRate)}"
                    } else "PCM"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (badgeText.contains("Hi-Res") || badgeText.contains("24") || badgeText.contains("Direct")) Color(0x33FFD54F) else Color(0x2200E5FF))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badgeText.contains("Hi-Res") || badgeText.contains("24") || badgeText.contains("Direct")) AudiophileGold else AudiophileCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${item.artist} • ${item.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatTimeMs(item.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF707080),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Direct Stream"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

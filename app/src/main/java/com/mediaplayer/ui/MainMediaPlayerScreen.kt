package com.mediaplayer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsVoice
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.media.MediaMetadataRetriever
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.request.videoFrameOption
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.audio.UsbBitPerfectAudioController
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.data.MediaLibraryRepository
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.playback.StreamHistoryItem
import com.mediaplayer.ui.components.AboutDeveloperDialog
import com.mediaplayer.ui.components.AppUpdateDialog
import com.mediaplayer.ui.components.BottomMiniPlayer
import com.mediaplayer.ui.components.DeveloperSupportCard
import com.mediaplayer.ui.components.MediaHubPage
import com.mediaplayer.ui.components.UsbDacSettingsDialog
import com.mediaplayer.ui.components.UsbTelemetryPill
import com.mediaplayer.util.AppReleaseInfo
import com.mediaplayer.util.AppUpdateManager
import com.mediaplayer.vault.PrivacyVaultScreen
import com.mediaplayer.vault.VaultMediaItem
import com.mediaplayer.vault.VaultPinDialog
import com.mediaplayer.vault.VaultRepository
import com.mediaplayer.vault.VaultSecurityManager
import com.mediaplayer.ui.music.MusicNowPlayingScreen
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurface
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.ui.video.VideoPlayerView
import com.mediaplayer.ui.video.formatTimeMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class VideoFolderItem(
    val name: String,
    val path: String,
    val videos: List<MediaItemModel>,
    val totalDurationMs: Long,
    val newCount: Int = 0
)

fun formatFolderDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun shareMediaFiles(ctx: Context, items: List<MediaItemModel>) {
    if (items.isEmpty()) return
    val uris = ArrayList<Uri>(items.map { it.uri })
    val mimeType = when {
        items.all { it.isVideo } -> "video/*"
        items.all { !it.isVideo } -> "audio/*"
        else -> "*/*"
    }
    val shareIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = items.first().mimeType.ifEmpty { mimeType }
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    val chooserTitle = if (items.size == 1) "Share Media File" else "Share ${items.size} Files"
    val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        ctx.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(ctx, "No app available to share files", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMediaPlayerScreen(
    repository: MediaLibraryRepository,
    vaultRepository: VaultRepository,
    vaultSecurityManager: VaultSecurityManager,
    playerManager: MediaPlayerManager,
    usbController: UsbBitPerfectAudioController,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val telemetry by usbController.status.collectAsState()
    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isExpanded by playerManager.isPlayerExpanded.collectAsState()
    val playbackPositions by playerManager.playbackPositions.collectAsState()
    val playedMediaIds by playerManager.playedMediaIds.collectAsState()
    val decoderMode by playerManager.decoderMode.collectAsState()

    var selectedDestination by remember {
        mutableIntStateOf(playerManager.historyManager.getLastTab().coerceIn(0, 3))
    }
    var recentStreamsState by remember {
        mutableStateOf(playerManager.historyManager.getRecentStreams())
    }
    var audioList by remember { mutableStateOf(repository.getCachedAudiosFast()) }
    var videoList by remember { mutableStateOf(repository.getCachedVideosFast()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var showDacDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var appReleaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }

    // Privacy Vault states
    var showVaultScreen by remember { mutableStateOf(false) }
    var showVaultPinDialog by remember { mutableStateOf(false) }
    var isMovingToVault by remember { mutableStateOf(false) }

    // Automatic silent check for GitHub releases on app startup
    LaunchedEffect(Unit) {
        val updateResult = AppUpdateManager.checkForUpdates(context)
        updateResult.onSuccess { info ->
            if (info.isUpdateAvailable) {
                appReleaseInfo = info
                showAutoUpdateDialog = true
            }
        }
    }

    // Video view mode: Grid vs List (default to List view)
    var isVideoGridView by remember { mutableStateOf(false) }
    var openedVideoFolder by remember { mutableStateOf<VideoFolderItem?>(null) }
    var videoSortOrder by remember { mutableIntStateOf(0) }
    var showVideoSortMenu by remember { mutableStateOf(false) }
    var selectedVideoFolder by remember { mutableStateOf("All Folders") }

    // Music filters: All Tracks, Hi-Res / Lossless, Artists, Albums, Folders
    var musicFilterTab by remember { mutableStateOf("All Tracks") }

    // Multi-Selection and Deletion state
    var selectedMediaItems by remember { mutableStateOf<Set<MediaItemModel>>(emptySet()) }
    var itemsToDelete by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var pendingConsentItems by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        itemsToDelete = emptyList()
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val items = pendingConsentItems
            if (items.isNotEmpty()) {
                scope.launch {
                    repository.removeCachedItems(items)
                    val deletedIds = items.map { it.id }.toSet()
                    val deletedUris = items.map { it.uri }.toSet()
                    val hasVideos = items.any { it.isVideo }
                    val hasAudios = items.any { !it.isVideo }
                    if (hasVideos) {
                        videoList = videoList.filter { it.id !in deletedIds && it.uri !in deletedUris }
                        if (openedVideoFolder != null) {
                            val remaining = videoList.filter {
                                (it.folderName.ifEmpty { "Videos" } == openedVideoFolder!!.name)
                            }
                            if (remaining.isEmpty()) {
                                openedVideoFolder = null
                            }
                        }
                    }
                    if (hasAudios) {
                        audioList = audioList.filter { it.id !in deletedIds && it.uri !in deletedUris }
                    }
                    if (playerManager.currentMediaItem.value?.id in deletedIds) {
                        playerManager.stop()
                        playerManager.setPlayerExpanded(false)
                    }
                    selectedMediaItems = selectedMediaItems.filter { it.id !in deletedIds && it.uri !in deletedUris }.toSet()
                    val msg = if (items.size == 1) "${items.first().title} deleted from storage"
                              else "${items.size} files deleted from storage"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingConsentItems = emptyList()
    }

    val onDeleteMedia: (MediaItemModel) -> Unit = { item ->
        itemsToDelete = listOf(item)
    }

    fun performDelete(items: List<MediaItemModel>) {
        itemsToDelete = emptyList()
        if (items.isEmpty()) return
        scope.launch {
            val directlyDeleted = repository.deleteMediaFiles(items) { intentSender, needingConsent ->
                pendingConsentItems = needingConsent
                deleteConsentLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            if (directlyDeleted.isNotEmpty()) {
                val deletedIds = directlyDeleted.map { it.id }.toSet()
                val deletedUris = directlyDeleted.map { it.uri }.toSet()
                if (directlyDeleted.any { it.isVideo }) {
                    videoList = videoList.filter { it.id !in deletedIds && it.uri !in deletedUris }
                    if (openedVideoFolder != null) {
                        val remaining = videoList.filter {
                            (it.folderName.ifEmpty { "Videos" } == openedVideoFolder!!.name)
                        }
                        if (remaining.isEmpty()) {
                            openedVideoFolder = null
                        }
                    }
                }
                if (directlyDeleted.any { !it.isVideo }) {
                    audioList = audioList.filter { it.id !in deletedIds && it.uri !in deletedUris }
                }
                if (playerManager.currentMediaItem.value?.id in deletedIds) {
                    playerManager.stop()
                    playerManager.setPlayerExpanded(false)
                }
                selectedMediaItems = selectedMediaItems.filter { it.id !in deletedIds && it.uri !in deletedUris }.toSet()
                val msg = if (directlyDeleted.size == 1) "${directlyDeleted.first().title} deleted from storage"
                          else "${directlyDeleted.size} files deleted from storage"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun moveToPrivacyVault(items: List<MediaItemModel>) {
        if (items.isEmpty()) return
        scope.launch {
            isMovingToVault = true
            val moved = vaultRepository.moveToVault(items)
            if (moved.isNotEmpty()) {
                repository.removeCachedItems(items)
                val movedIds = items.map { it.id }.toSet()
                val movedUris = items.map { it.uri }.toSet()
                if (items.any { it.isVideo }) {
                    videoList = videoList.filter { it.id !in movedIds && it.uri !in movedUris }
                    if (openedVideoFolder != null) {
                        val remaining = videoList.filter {
                            (it.folderName.ifEmpty { "Videos" } == openedVideoFolder!!.name)
                        }
                        if (remaining.isEmpty()) {
                            openedVideoFolder = null
                        }
                    }
                }
                if (items.any { !it.isVideo }) {
                    audioList = audioList.filter { it.id !in movedIds && it.uri !in movedUris }
                }
                if (playerManager.currentMediaItem.value?.id in movedIds) {
                    playerManager.stop()
                    playerManager.setPlayerExpanded(false)
                }
                selectedMediaItems = emptySet()
                val msg = if (moved.size == 1) "${moved.first().title} hidden from Gallery & Files"
                          else "${moved.size} files hidden from Gallery & Files"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not hide files. Please verify All Files Access permission.", Toast.LENGTH_LONG).show()
            }
            isMovingToVault = false
        }
    }

    fun openPrivacyVault() {
        if (vaultSecurityManager.isUnlocked()) {
            showVaultScreen = true
        } else {
            showVaultPinDialog = true
        }
    }

    // Predictive back progress state
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    // Back handling for Privacy Vault screen
    BackHandler(enabled = showVaultScreen && !isExpanded) {
        showVaultScreen = false
        scope.launch {
            videoList = repository.getVideoList()
            audioList = repository.getAudioList()
        }
    }

    // Back handling for multi-selection mode
    BackHandler(enabled = selectedMediaItems.isNotEmpty()) {
        selectedMediaItems = emptySet()
    }

    // Back handling for opened video folder
    BackHandler(enabled = selectedMediaItems.isEmpty() && openedVideoFolder != null && selectedDestination == 0) {
        openedVideoFolder = null
    }

    // Permission state
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED)
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

    // SAF launchers
    val openFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val newAudios = mutableListOf<MediaItemModel>()
                val newVideos = mutableListOf<MediaItemModel>()
                for (uri in uris) {
                    val item = repository.importSafSingleFile(uri)
                    if (item != null) {
                        if (item.isVideo) newVideos.add(item) else newAudios.add(item)
                    }
                }
                if (newAudios.isNotEmpty()) {
                    audioList = newAudios + audioList
                    repository.saveCachedAudios(audioList)
                }
                if (newVideos.isNotEmpty()) {
                    videoList = newVideos + videoList
                    repository.saveCachedVideos(videoList)
                }
                val firstItem = newVideos.firstOrNull() ?: newAudios.firstOrNull()
                if (firstItem != null) {
                    playerManager.playMedia(firstItem, if (firstItem.isVideo) videoList else audioList)
                    playerManager.setPlayerExpanded(true)
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
                if (audios.isNotEmpty()) {
                    audioList = audios + audioList
                    repository.saveCachedAudios(audioList)
                }
                if (videos.isNotEmpty()) {
                    videoList = videos + videoList
                    repository.saveCachedVideos(videoList)
                }
            }
        }
    }

    fun reloadMedia() {
        scope.launch(Dispatchers.IO) {
            val newAudios = repository.getAudioList()
            val newVideos = repository.getVideoList()
            var changed = false
            if (newAudios.size != audioList.size || (newAudios.isNotEmpty() && audioList.isNotEmpty() && newAudios.first().id != audioList.first().id)) {
                audioList = newAudios
                repository.saveCachedAudios(newAudios)
                changed = true
            }
            if (newVideos.size != videoList.size || (newVideos.isNotEmpty() && videoList.isNotEmpty() && newVideos.first().id != videoList.first().id)) {
                videoList = newVideos
                repository.saveCachedVideos(newVideos)
                changed = true
            }
            if (changed || playerManager.currentMediaItem.value == null) {
                playerManager.restoreLastPlayedMedia(newVideos + newAudios)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (audioList.isEmpty() || videoList.isEmpty()) {
            val (cachedAudios, cachedVideos) = withContext(Dispatchers.IO) {
                Pair(repository.getCachedAudios(), repository.getCachedVideos())
            }
            if (audioList.isEmpty() && cachedAudios.isNotEmpty()) audioList = cachedAudios
            if (videoList.isEmpty() && cachedVideos.isNotEmpty()) videoList = cachedVideos
        }
        // Immediately restore last played media in paused state from persistent cache without re-querying
        if (playerManager.currentMediaItem.value == null) {
            playerManager.restoreLastPlayedMedia(videoList + audioList)
        }
        // Background scan to sync any changes in filesystem
        reloadMedia()
    }

    // Back navigation for expanded player (Standard back button / gesture fallback)
    BackHandler(enabled = isExpanded && !isInPipMode) {
        if (currentItem?.isVideo == true) {
            playerManager.stopVideo()
        }
        playerManager.setPlayerExpanded(false)
    }

    // Predictive Back Gesture Animation Handler
    PredictiveBackHandler(enabled = isExpanded && !isInPipMode) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }
            if (currentItem?.isVideo == true) {
                playerManager.stopVideo()
            }
            playerManager.setPlayerExpanded(false)
        } catch (_: CancellationException) {
            predictiveBackProgress = 0f
        } finally {
            predictiveBackProgress = 0f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (predictiveBackProgress > 0f) 1f - (predictiveBackProgress * 0.14f) else 1f,
        label = "predictiveBackScale"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (predictiveBackProgress > 0f) (predictiveBackProgress * 28).dp else 0.dp,
        label = "predictiveBackCorners"
    )

    // Filtered Video List
    val videoFolders = remember(videoList) {
        listOf("All Folders") + videoList.map { it.folderName.ifEmpty { "Videos" } }.distinct()
    }

    val filteredVideo = remember(videoList, searchQuery, selectedVideoFolder) {
        videoList.filter { video ->
            val matchesFolder = selectedVideoFolder == "All Folders" || (video.folderName.ifEmpty { "Videos" } == selectedVideoFolder)
            val matchesQuery = searchQuery.isBlank() || video.title.contains(searchQuery, ignoreCase = true) || video.artist.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }

    val filteredAudio = remember(audioList, searchQuery) {
        if (searchQuery.isBlank()) audioList
        else audioList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = OledSurface
    ) {
        if (isInPipMode && currentItem?.isVideo == true) {
            VideoPlayerView(
                playerManager = playerManager,
                telemetry = telemetry,
                onBack = {},
                onEnterPip = {},
                onOpenDacSettings = {},
                isInPipMode = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val configuration = LocalConfiguration.current
            val isWideScreen = configuration.screenWidthDp >= 640

            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Navigation Rail on Tablets / Landscape
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = OledSurface,
                            contentColor = AudiophileCyan,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            NavigationRailItem(
                                selected = selectedDestination == 0,
                                onClick = {
                                    selectedDestination = 0
                                    playerManager.historyManager.saveLastTab(0)
                                },
                                icon = { Icon(Icons.Rounded.Movie, contentDescription = "Videos") },
                                label = { Text("Videos") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = AudiophileCyan,
                                    indicatorColor = AudiophileCyan,
                                    unselectedIconColor = Color(0xFF9E9EAE),
                                    unselectedTextColor = Color(0xFF9E9EAE)
                                )
                            )
                            NavigationRailItem(
                                selected = selectedDestination == 1,
                                onClick = {
                                    selectedDestination = 1
                                    playerManager.historyManager.saveLastTab(1)
                                },
                                icon = { Icon(Icons.Rounded.MusicNote, contentDescription = "Music") },
                                label = { Text("Music") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = AudiophileCyan,
                                    indicatorColor = AudiophileCyan,
                                    unselectedIconColor = Color(0xFF9E9EAE),
                                    unselectedTextColor = Color(0xFF9E9EAE)
                                )
                            )
                            NavigationRailItem(
                                selected = selectedDestination == 2,
                                onClick = {
                                    selectedDestination = 2
                                    playerManager.historyManager.saveLastTab(2)
                                },
                                icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = "DAC Lab") },
                                label = { Text("DAC Lab") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                                    indicatorColor = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                                    unselectedIconColor = Color(0xFF9E9EAE),
                                    unselectedTextColor = Color(0xFF9E9EAE)
                                )
                            )
                            NavigationRailItem(
                                selected = selectedDestination == 3,
                                onClick = {
                                    selectedDestination = 3
                                    playerManager.historyManager.saveLastTab(3)
                                },
                                icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = "Media Hub") },
                                label = { Text("Media Hub") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = AudiophileCyan,
                                    indicatorColor = AudiophileCyan,
                                    unselectedIconColor = Color(0xFF9E9EAE),
                                    unselectedTextColor = Color(0xFF9E9EAE)
                                )
                            )
                        }
                    }

                    // Main Content Scaffold
                    Scaffold(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        containerColor = OledSurface,
                        topBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF11141C), Color(0xFF090A0E))
                                        )
                                    )
                                    .statusBarsPadding()
                            ) {
                                if (selectedMediaItems.isNotEmpty()) {
                                    // Contextual Action Bar for Multi-Selection
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable { selectedMediaItems = emptySet() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Cancel Selection",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${selectedMediaItems.size} Selected",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                val totalSize = selectedMediaItems.sumOf { it.sizeBytes }
                                                Text(
                                                    text = formatFileSize(totalSize),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFA0A8B8),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Select All / Deselect All
                                            val currentPool = if (selectedDestination == 0) {
                                                openedVideoFolder?.videos ?: filteredVideo
                                            } else {
                                                filteredAudio
                                            }
                                            val isAllSelected = currentPool.isNotEmpty() && selectedMediaItems.containsAll(currentPool)
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable {
                                                        if (isAllSelected) {
                                                            selectedMediaItems = emptySet()
                                                        } else {
                                                            selectedMediaItems = currentPool.toSet()
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isAllSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                                    contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                                                    tint = AudiophileCyan,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Share Button
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable {
                                                        shareMediaFiles(context, selectedMediaItems.toList())
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Share,
                                                    contentDescription = "Share Selected Files",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Lock in Privacy Vault Button (Gold)
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x33FFD54F))
                                                    .border(1.dp, Color(0x66FFD54F), CircleShape)
                                                    .clickable {
                                                        moveToPrivacyVault(selectedMediaItems.toList())
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lock,
                                                    contentDescription = "Hide in Privacy Vault",
                                                    tint = AudiophileGold,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Delete Button (Red)
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x33FF453A))
                                                    .border(1.dp, Color(0x66FF453A), CircleShape)
                                                    .clickable {
                                                        itemsToDelete = selectedMediaItems.toList()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "Delete Selected Files",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (selectedDestination == 0) {
                                    if (openedVideoFolder != null) {
                                        val folder = openedVideoFolder!!
                                        // Folder Header Bar when inside a folder
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Luxury frosted circular back button
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable { openedVideoFolder = null },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = folder.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF1B2230))
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "${folder.videos.size} Videos",
                                                            color = AudiophileCyan,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = formatFolderDuration(folder.totalDurationMs),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF7E8E9E),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            // Sort Action in 38.dp glass circle
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(if (videoSortOrder != 0) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                                                        .clickable { showVideoSortMenu = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Tune,
                                                        contentDescription = "Sort",
                                                        tint = if (videoSortOrder != 0) AudiophileCyan else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showVideoSortMenu,
                                                    onDismissRequest = { showVideoSortMenu = false },
                                                    modifier = Modifier.background(OledSurfaceVariant)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Date Added", color = Color.White) },
                                                        onClick = { videoSortOrder = 0; showVideoSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Name (A-Z)", color = Color.White) },
                                                        onClick = { videoSortOrder = 1; showVideoSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by File Size", color = Color.White) },
                                                        onClick = { videoSortOrder = 2; showVideoSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Duration", color = Color.White) },
                                                        onClick = { videoSortOrder = 3; showVideoSortMenu = false }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Layout Toggle (Grid / List) in 38.dp glass circle
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable { isVideoGridView = !isVideoGridView },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isVideoGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                                    contentDescription = "Toggle Grid/List",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Main Videos Header (Folder list)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Media Player",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Text(
                                                    text = "${videoFolders.size} Folders • ${videoList.size} Videos",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF7E8E9E),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Search button in glass circle
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSearchActive) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                                                        .clickable { isSearchActive = !isSearchActive },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Search,
                                                        contentDescription = "Search",
                                                        tint = if (isSearchActive) AudiophileCyan else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // View Toggle
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x1AFFFFFF))
                                                        .clickable { isVideoGridView = !isVideoGridView },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isVideoGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                                        contentDescription = "Toggle View",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // Settings / DAC dialog
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x1AFFFFFF))
                                                        .clickable { showDacDialog = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Settings,
                                                        contentDescription = "Settings",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (selectedDestination == 1) {
                                    // Top App Bar for Music
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Music",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${filteredAudio.size} Tracks" + if (telemetry.isMixerBypassActive) " • Bit-Perfect Direct" else " • 144Hz Hi-Fi",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (telemetry.isMixerBypassActive) AudiophileGold else Color(0xFF9E9EAE)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSearchActive) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                                                    .clickable { isSearchActive = !isSearchActive },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Search,
                                                    contentDescription = "Search",
                                                    tint = if (isSearchActive) AudiophileCyan else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable { reloadMedia() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Refresh,
                                                    contentDescription = "Refresh",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (selectedDestination == 2) {
                                    // Top App Bar for DAC Lab
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "DAC Lab",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (telemetry.isMixerBypassActive) AudiophileGold
                                                            else if (telemetry.isUsbDacConnected) AudiophileCyan
                                                            else Color(0xFF6B7280)
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (telemetry.isMixerBypassActive) "BIT-PERFECT DIRECT • ${telemetry.dacName}"
                                                    else if (telemetry.isUsbDacConnected) "DIRECT USB • ${telemetry.dacName}"
                                                    else "INTERNAL AUDIO DRIVER",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (telemetry.isMixerBypassActive) AudiophileGold
                                                    else if (telemetry.isUsbDacConnected) AudiophileCyan
                                                    else Color(0xFF8E8E9E),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x1AFFFFFF))
                                                .clickable { usbController.scanAudioDevices() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = "Rescan DAC Hardware",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // Top App Bar for Media Hub
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Media Hub",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(AudiophileCyan)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "STORAGE, USB-OTG & NETWORK STREAMS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AudiophileCyan,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x1AFFFFFF))
                                                .clickable { showAboutDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Info,
                                                contentDescription = "About & Updates",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Razor-thin subtle OLED border under header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color(0x1AFFFFFF))
                                )

                                // Search Bar Expansion
                                AnimatedVisibility(
                                    visible = isSearchActive,
                                    enter = expandVertically(
                                        animationSpec = tween(140, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(100)),
                                    exit = shrinkVertically(
                                        animationSpec = tween(100, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(80))
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search videos, folders, music, artists...") },
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
                            }
                        },
                        bottomBar = {
                            Column {
                                // Persistent Mini-Player Bar (Audio Only)
                                AnimatedVisibility(
                                    visible = currentItem != null && !isExpanded && currentItem?.isVideo != true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(100)),
                                    exit = slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(120, easing = FastOutLinearInEasing)
                                    ) + fadeOut(animationSpec = tween(80))
                                ) {
                                    BottomMiniPlayer(
                                        playerManager = playerManager,
                                        telemetry = telemetry,
                                        onExpand = { playerManager.setPlayerExpanded(true) }
                                    )
                                }

                                // Unified Floating Glassmorphic Navigation Island on Phones
                                if (!isWideScreen) {
                                    val activeAccentColor = if (selectedDestination == 2 && telemetry.isMixerBypassActive) {
                                        AudiophileGold
                                    } else {
                                        AudiophileCyan
                                    }
                                    val indicatorFraction by animateFloatAsState(
                                        targetValue = selectedDestination.toFloat(),
                                        animationSpec = tween(
                                            durationMillis = 120,
                                            easing = FastOutSlowInEasing
                                        ),
                                        label = "dockIndicatorFraction"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                                            .navigationBarsPadding(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(28.dp),
                                            color = Color(0xF20F121C),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        activeAccentColor.copy(alpha = 0.40f),
                                                        Color(0x22FFFFFF),
                                                        activeAccentColor.copy(alpha = 0.40f)
                                                    )
                                                )
                                            ),
                                            shadowElevation = 14.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                // Hardware-accelerated sliding capsule indicator (Pure GPU Draw-phase translation, 0 recompositions)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.25f)
                                                        .fillMaxHeight()
                                                        .graphicsLayer {
                                                            translationX = indicatorFraction * size.width
                                                        }
                                                        .padding(horizontal = 3.dp, vertical = 2.dp)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    activeAccentColor.copy(alpha = 0.24f),
                                                                    activeAccentColor.copy(alpha = 0.08f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            1.dp,
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    activeAccentColor.copy(alpha = 0.60f),
                                                                    activeAccentColor.copy(alpha = 0.15f)
                                                                )
                                                            ),
                                                            RoundedCornerShape(20.dp)
                                                        )
                                                )

                                                // Fixed stationary tabs: perfectly centered, no layout shifts
                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // 0: Videos (Home)
                                                    FloatingDockItem(
                                                        selected = selectedDestination == 0,
                                                        onClick = {
                                                            selectedMediaItems = emptySet()
                                                            if (selectedDestination == 0 && openedVideoFolder != null) {
                                                                openedVideoFolder = null
                                                            } else {
                                                                selectedDestination = 0
                                                                playerManager.historyManager.saveLastTab(0)
                                                            }
                                                        },
                                                        icon = Icons.Rounded.Movie,
                                                        label = "Videos",
                                                        activeColor = AudiophileCyan,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    // 1: Music
                                                    FloatingDockItem(
                                                        selected = selectedDestination == 1,
                                                        onClick = {
                                                            selectedMediaItems = emptySet()
                                                            selectedDestination = 1
                                                            playerManager.historyManager.saveLastTab(1)
                                                        },
                                                        icon = Icons.Rounded.MusicNote,
                                                        label = "Music",
                                                        activeColor = AudiophileCyan,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    // 2: DAC Lab
                                                    FloatingDockItem(
                                                        selected = selectedDestination == 2,
                                                        onClick = {
                                                            selectedMediaItems = emptySet()
                                                            selectedDestination = 2
                                                            playerManager.historyManager.saveLastTab(2)
                                                        },
                                                        icon = Icons.Rounded.GraphicEq,
                                                        label = "DAC Lab",
                                                        activeColor = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    // 3: Media Hub
                                                    FloatingDockItem(
                                                        selected = selectedDestination == 3,
                                                        onClick = {
                                                            selectedMediaItems = emptySet()
                                                            selectedDestination = 3
                                                            playerManager.historyManager.saveLastTab(3)
                                                        },
                                                        icon = Icons.Rounded.FolderOpen,
                                                        label = "Media Hub",
                                                        activeColor = AudiophileCyan,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedDestination) {
                                0 -> {
                                        // VIDEOS SCREEN
                                        VideosTabScreen(
                                            videos = filteredVideo,
                                            isGridView = isVideoGridView,
                                            openedFolder = openedVideoFolder,
                                            onOpenFolder = {
                                                selectedMediaItems = emptySet()
                                                openedVideoFolder = it
                                            },
                                            sortOrder = videoSortOrder,
                                            playbackPositions = playbackPositions,
                                            playedMediaIds = playedMediaIds,
                                            currentPlayingId = currentItem?.id,
                                            hasPermission = hasPermission,
                                            onToggleView = { isVideoGridView = !isVideoGridView },
                                            onPlayVideo = { video, playlist, startPos ->
                                                playerManager.playMedia(video, playlist, startPos)
                                                playerManager.setPlayerExpanded(true)
                                            },
                                            onRequestPermission = {
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
                                            onPickFiles = { openFilesLauncher.launch(arrayOf("video/*", "audio/*")) },
                                            onDeleteVideo = onDeleteMedia,
                                            onMoveToVault = { video -> moveToPrivacyVault(listOf(video)) },
                                            searchQuery = searchQuery,
                                            selectedMediaItems = selectedMediaItems,
                                            onToggleSelectItem = { item ->
                                                selectedMediaItems = if (item in selectedMediaItems) selectedMediaItems - item else selectedMediaItems + item
                                            },
                                            onStartSelection = { item ->
                                                selectedMediaItems = setOf(item)
                                            }
                                        )
                                    }
                                    1 -> {
                                        // MUSIC SCREEN
                                        MusicTabScreen(
                                            audioList = filteredAudio,
                                            filterTab = musicFilterTab,
                                            currentPlayingId = currentItem?.id,
                                            hasPermission = hasPermission,
                                            onFilterTabSelect = { musicFilterTab = it },
                                            onPlayAudio = { audio, playlist ->
                                                playerManager.playMedia(audio, playlist)
                                                playerManager.setPlayerExpanded(true)
                                            },
                                            onShuffleAll = {
                                                if (filteredAudio.isNotEmpty()) {
                                                    val shuffled = filteredAudio.shuffled()
                                                    playerManager.playMedia(shuffled.first(), shuffled)
                                                    playerManager.setPlayerExpanded(true)
                                                }
                                            },
                                            onRequestPermission = {
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
                                            onPickFiles = { openFilesLauncher.launch(arrayOf("audio/*", "video/*")) },
                                            onDeleteAudio = onDeleteMedia,
                                            onMoveToVault = { audio -> moveToPrivacyVault(listOf(audio)) },
                                            selectedMediaItems = selectedMediaItems,
                                            onToggleSelectItem = { item: MediaItemModel ->
                                                selectedMediaItems = if (item in selectedMediaItems) selectedMediaItems - item else selectedMediaItems + item
                                            },
                                            onStartSelection = { item: MediaItemModel ->
                                                selectedMediaItems = setOf(item)
                                            }
                                        )
                                    }
                                    2 -> {
                                        // AUDIO & DAC LAB
                                        AudioDacLabScreen(
                                            telemetry = telemetry,
                                            decoderMode = decoderMode,
                                            onSetBitPerfect = { usbController.setExclusiveBitPerfect(it) },
                                            onSetDecoderMode = { playerManager.setDecoderMode(it) },
                                            onRequestUsbPermission = {
                                                if (context is android.app.Activity) {
                                                    usbController.requestUsbPermission(context)
                                                }
                                            }
                                        )
                                    }
                                    3 -> {
                                        // MEDIA HUB PAGE
                                        MediaHubPage(
                                            onPickFiles = { openFilesLauncher.launch(arrayOf("video/*", "audio/*")) },
                                            onPickFolder = { openFolderLauncher.launch(null) },
                                            onPlayStream = { url, title, isVideo ->
                                                playerManager.playUrl(url = url, title = title, isVideo = isVideo)
                                                playerManager.setPlayerExpanded(true)
                                            },
                                            recentStreams = recentStreamsState,
                                            onDeleteRecentStream = { url ->
                                                playerManager.historyManager.removeRecentStream(url)
                                                recentStreamsState = playerManager.historyManager.getRecentStreams()
                                            },
                                            onClearAllRecentStreams = {
                                                playerManager.historyManager.clearRecentStreams()
                                                recentStreamsState = emptyList()
                                            },
                                            onOpenAboutDialog = { showAboutDialog = true },
                                            onOpenPrivacyVault = { openPrivacyVault() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                // Privacy Vault Full Screen
                if (showVaultScreen) {
                    PrivacyVaultScreen(
                        vaultRepository = vaultRepository,
                        securityManager = vaultSecurityManager,
                        onPlayMedia = { item ->
                            if (item.isVideo) {
                                playerManager.playMedia(item, listOf(item))
                            } else {
                                playerManager.playMedia(item, listOf(item))
                            }
                            playerManager.setPlayerExpanded(true)
                        },
                        onLockVault = {
                            showVaultScreen = false
                            scope.launch {
                                videoList = repository.getVideoList()
                                audioList = repository.getAudioList()
                            }
                        },
                        onBack = {
                            showVaultScreen = false
                            scope.launch {
                                videoList = repository.getVideoList()
                                audioList = repository.getAudioList()
                            }
                        }
                    )
                }

                // Full-Screen Player with Predictive Back Morphing (Hardware accelerated via graphicsLayer)
                AnimatedVisibility(
                    visible = isExpanded && currentItem != null,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing)),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                translationY = predictiveBackProgress * 260.dp.toPx()
                                if (animatedCornerRadius > 0.dp) {
                                    clip = true
                                    shape = RoundedCornerShape(animatedCornerRadius)
                                }
                            }
                            .background(Color.Black)
                    ) {
                        if (currentItem?.isVideo == true) {
                            VideoPlayerView(
                                playerManager = playerManager,
                                telemetry = telemetry,
                                onBack = {
                                    playerManager.stopVideo()
                                    playerManager.setPlayerExpanded(false)
                                },
                                onEnterPip = onEnterPip,
                                onOpenDacSettings = { showDacDialog = true },
                                isInPipMode = isInPipMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            MusicNowPlayingScreen(
                                playerManager = playerManager,
                                telemetry = telemetry,
                                onCollapse = { playerManager.setPlayerExpanded(false) },
                                onOpenDacSettings = { showDacDialog = true }
                            )
                        }
                    }
                }

                // Bit-Perfect Control Dialog
                if (showDacDialog) {
                    UsbDacSettingsDialog(
                        telemetry = telemetry,
                        controller = usbController,
                        onDismiss = { showDacDialog = false }
                    )
                }


                // About Developer & Support Dialog
                if (showAboutDialog) {
                    AboutDeveloperDialog(
                        onDismiss = { showAboutDialog = false }
                    )
                }

                // Automatic Update Dialog
                if (showAutoUpdateDialog && appReleaseInfo != null) {
                    AppUpdateDialog(
                        releaseInfo = appReleaseInfo!!,
                        onDismiss = { showAutoUpdateDialog = false }
                    )
                }

                // Privacy Vault PIN Dialog
                if (showVaultPinDialog) {
                    VaultPinDialog(
                        securityManager = vaultSecurityManager,
                        onSuccess = {
                            showVaultPinDialog = false
                            showVaultScreen = true
                        },
                        onDismiss = { showVaultPinDialog = false }
                    )
                }

                // Moving to Privacy Vault Progress Dialog
                if (isMovingToVault) {
                    AlertDialog(
                        onDismissRequest = { /* prevent dismissal while moving */ },
                        containerColor = Color(0xFF141722),
                        shape = RoundedCornerShape(24.dp),
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0x22FFD54F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AudiophileGold,
                                    modifier = Modifier.size(34.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        },
                        title = {
                            Text(
                                text = "Securing Media...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Moving to Privacy Vault & hiding from device gallery...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD0D0DC),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        },
                        confirmButton = {}
                    )
                }

                // Delete Media Confirmation Dialog
                if (itemsToDelete.isNotEmpty()) {
                    DeleteMediaConfirmDialog(
                        items = itemsToDelete,
                        onConfirm = {
                            val targets = itemsToDelete
                            itemsToDelete = emptyList()
                            performDelete(targets)
                        },
                        onDismiss = { itemsToDelete = emptyList() }
                    )
                }
            }
        }
    }
}

// ----------------- VIDEOS TAB (FOLDER-BASED MEDIA PLAYER FORMAT) -----------------

@Composable
private fun FolderListItem(
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
private fun FolderGridItem(
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
private fun VideosTabScreen(
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
                val isRecent = if (v.dateAdded > 0) (System.currentTimeMillis() / 1000 - v.dateAdded) < (14 * 24 * 3600) else false
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
            if (searchQuery.isBlank()) true
            else folder.name.contains(searchQuery, ignoreCase = true) ||
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
            val sortedFolderVideos = remember(currentFolder.videos, sortOrder) {
                when (sortOrder) {
                    1 -> currentFolder.videos.sortedBy { it.title.lowercase() }
                    2 -> currentFolder.videos.sortedByDescending { it.sizeBytes }
                    3 -> currentFolder.videos.sortedByDescending { it.durationMs }
                    else -> currentFolder.videos
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Folder Navigation Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        onClick = { onOpenFolder(null) },
                        shape = CircleShape,
                        color = Color(0xFF1B2330),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to folders",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentFolder.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${sortedFolderVideos.size} Videos • ${formatFolderDuration(currentFolder.totalDurationMs)}",
                            color = Color(0xFFA0A0B0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleView,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle View",
                            tint = AudiophileCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

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
                        items(sortedFolderVideos, key = { it.id }, contentType = { "video_grid_card" }) { video ->
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
                        items(sortedFolderVideos, key = { it.id }, contentType = { "video_list_card" }) { video ->
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
                        items(folderItems, key = { it.path + "/" + it.name }, contentType = { "folder_grid_card" }) { folder ->
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
                        items(folderItems, key = { it.path + "/" + it.name }, contentType = { "folder_list_card" }) { folder ->
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

@Composable
private fun VideoOptionsMenuDialog(
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
private fun InfoTextRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color(0xFFA0A0B0), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCardItem(
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
        if (item.dateAdded > 0) {
            (System.currentTimeMillis() / 1000 - item.dateAdded) < (14 * 24 * 3600)
        } else {
            false
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoListItem(
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
        if (item.dateAdded > 0) {
            (System.currentTimeMillis() / 1000 - item.dateAdded) < (14 * 24 * 3600)
        } else {
            false
        }
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

// ----------------- MUSIC TAB -----------------
private val FallbackProceduralPalettes = listOf(
    listOf(Color(0xFF1E2640), Color(0xFF0D1B2A)),
    listOf(Color(0xFF2E1C38), Color(0xFF1A0F24)),
    listOf(Color(0xFF1B3B36), Color(0xFF0F2320)),
    listOf(Color(0xFF382D1C), Color(0xFF221A0F)),
    listOf(Color(0xFF1E3240), Color(0xFF101B24))
)

@Composable
private fun TrackOptionsMenuDialog(
    item: MediaItemModel,
    onPlay: () -> Unit,
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
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = AudiophileCyan)
                        Spacer(Modifier.width(12.dp))
                        Text("Play Now", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onShowDetails,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = AudiophileCyan)
                        Spacer(Modifier.width(12.dp))
                        Text("Track Details", color = Color.White)
                    }
                }
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Share Track", color = Color.White)
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
private fun MusicCategoryFilterPills(
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
        items(filterTabs) { tab ->
            val isSelected = tab == selectedTab
            val pillBg = if (isSelected) AudiophileCyan else Color(0x1F222533)
            val pillBorder = if (isSelected) AudiophileCyan else Color(0x22FFFFFF)
            val pillContentColor = if (isSelected) Color.Black else Color(0xFFE0E0EC)
            val pillIconColor = if (isSelected) Color.Black else Color(0xFFC0C0D0)

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
private fun MusicQuickActionsBar(
    sortOrder: String,
    onSortOrderChange: (String) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play All Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(AudiophileCyan, Color(0xFF00B0FF))))
                    .clickable { onPlayAll() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play All",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Shuffle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x1F222533))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                    .clickable { onShuffleAll() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Shuffle",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Sort Dropdown
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showSortMenu = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Sort",
                        tint = AudiophileCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sortOrder,
                        color = Color(0xFFA0A0B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier = Modifier.background(Color(0xFF1E202B))
            ) {
                listOf("Default", "Title", "Artist", "Duration", "Recently Added").forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order, color = if (sortOrder == order) AudiophileCyan else Color.White) },
                        onClick = {
                            onSortOrderChange(order)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicTrackLazyList(
    tracks: List<MediaItemModel>,
    currentPlayingId: Long?,
    selectedMediaItems: Set<MediaItemModel> = emptySet(),
    onPlayTrack: (MediaItemModel) -> Unit,
    onShowOptions: (MediaItemModel) -> Unit,
    onDeleteTrack: (MediaItemModel) -> Unit,
    onToggleSelect: (MediaItemModel) -> Unit = {},
    onStartSelection: (MediaItemModel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSelectionMode = selectedMediaItems.isNotEmpty()
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(tracks, key = { it.id }, contentType = { "music_track" }) { audio ->
            val isPlaying = currentPlayingId == audio.id
            val isSelected = selectedMediaItems.contains(audio)
            MusicTrackRow(
                item = audio,
                isPlaying = isPlaying,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect(audio)
                    } else {
                        onPlayTrack(audio)
                    }
                },
                onShowOptions = { onShowOptions(audio) },
                onDeleteClick = { onDeleteTrack(audio) },
                onLongClick = { onStartSelection(audio) }
            )
        }
    }
}

@Composable
private fun MusicTrackDetailsDialog(
    item: MediaItemModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161822),
        title = {
            Text(
                text = "Track Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabRow("Title", item.title)
                LabRow("Artist", item.artist)
                LabRow("Album", item.album)
                LabRow("Format", item.mimeType.substringAfterLast("/"))
                LabRow("Sample Rate", UsbAudioStatus.formatSampleRate(item.sampleRate))
                LabRow("Bit Depth", "${item.bitDepth}-bit")
                LabRow("Duration", formatTimeMs(item.durationMs))
                LabRow("File Size", formatFileSize(item.sizeBytes))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AudiophileCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MusicTabScreen(
    audioList: List<MediaItemModel>,
    filterTab: String,
    currentPlayingId: Long?,
    hasPermission: Boolean,
    onFilterTabSelect: (String) -> Unit,
    onPlayAudio: (MediaItemModel, List<MediaItemModel>) -> Unit,
    onShuffleAll: () -> Unit,
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit,
    onDeleteAudio: (MediaItemModel) -> Unit = {},
    onMoveToVault: (MediaItemModel) -> Unit = {},
    selectedMediaItems: Set<MediaItemModel> = emptySet(),
    onToggleSelectItem: (MediaItemModel) -> Unit = {},
    onStartSelection: (MediaItemModel) -> Unit = {}
) {
    var sortOrder by remember { mutableStateOf("Default") }
    var selectedTrackDetails by remember { mutableStateOf<MediaItemModel?>(null) }
    var selectedTrackForOptions by remember { mutableStateOf<MediaItemModel?>(null) }

    val filterTabs = remember { listOf("All Tracks", "Hi-Res / Lossless", "Artists", "Albums", "Folders") }

    val displayedList = remember(audioList, filterTab, sortOrder) {
        val filtered = when (filterTab) {
            "Hi-Res / Lossless" -> audioList.filter {
                it.bitDepth >= 24 || it.sampleRate >= 48000 ||
                        it.codecBadge.contains("Hi-Res", true) ||
                        it.codecBadge.contains("Lossless", true) ||
                        it.mimeType.contains("flac", true) ||
                        it.mimeType.contains("alac", true) ||
                        it.mimeType.contains("wav", true) ||
                        it.mimeType.contains("dsd", true)
            }
            "Artists" -> audioList.sortedBy { it.artist.lowercase() }
            "Albums" -> audioList.sortedBy { it.album.lowercase() }
            "Folders" -> audioList.sortedBy { it.folderName.lowercase() }
            else -> audioList
        }
        when (sortOrder) {
            "Title" -> filtered.sortedBy { it.title.lowercase() }
            "Artist" -> filtered.sortedBy { it.artist.lowercase() }
            "Duration" -> filtered.sortedByDescending { it.durationMs }
            "Recently Added" -> filtered.sortedByDescending { it.dateAdded }
            else -> filtered
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            MusicQuickActionsBar(
                sortOrder = sortOrder,
                onSortOrderChange = { sortOrder = it },
                onPlayAll = {
                    if (displayedList.isNotEmpty()) {
                        onPlayAudio(displayedList.first(), displayedList)
                    }
                },
                onShuffleAll = onShuffleAll
            )

            if (displayedList.isEmpty()) {
                EmptyStateView(
                    title = "No music tracks found",
                    subtitle = "Grant permissions or pick audio files from custom folders/OTG drives.",
                    onAction = onPickFiles
                )
            } else {
                MusicTrackLazyList(
                    tracks = displayedList,
                    currentPlayingId = currentPlayingId,
                    selectedMediaItems = selectedMediaItems,
                    onPlayTrack = { track -> onPlayAudio(track, displayedList) },
                    onShowOptions = { track -> selectedTrackForOptions = track },
                    onDeleteTrack = onDeleteAudio,
                    onToggleSelect = onToggleSelectItem,
                    onStartSelection = onStartSelection
                )
            }
        }

        // Track Options Dialog (Hoisted singleton dialog, avoids allocating hundreds of DropdownMenus in LazyColumn)
        selectedTrackForOptions?.let { track ->
            val context = LocalContext.current
            TrackOptionsMenuDialog(
                item = track,
                onPlay = {
                    selectedTrackForOptions = null
                    onPlayAudio(track, displayedList)
                },
                onShowDetails = {
                    selectedTrackForOptions = null
                    selectedTrackDetails = track
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

        // Track Details Dialog
        selectedTrackDetails?.let { item ->
            MusicTrackDetailsDialog(
                item = item,
                onDismiss = { selectedTrackDetails = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicTrackRow(
    item: MediaItemModel,
    isPlaying: Boolean,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onShowOptions: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onLongClick: () -> Unit = onDeleteClick
) {
    val context = LocalContext.current
    val imageRequest = remember(item.id, item.artworkUri) {
        if (item.artworkUri == null) null
        else {
            ImageRequest.Builder(context)
                .data(item.artworkUri)
                .size(144)
                .crossfade(false)
                .build()
        }
    }

    val isHiRes = remember(item.bitDepth, item.sampleRate, item.codecBadge) {
        item.bitDepth >= 24 || item.sampleRate >= 48000 ||
                item.codecBadge.contains("Hi-Res", true) ||
                item.codecBadge.contains("Lossless", true) ||
                item.mimeType.contains("flac", true) ||
                item.mimeType.contains("alac", true) ||
                item.mimeType.contains("dsd", true)
    }

    val badgeText = remember(item.codecBadge, item.sampleRate, item.bitDepth) {
        item.codecBadge.ifEmpty {
            if (item.sampleRate >= 48000 || item.bitDepth >= 24) {
                "${item.bitDepth}b/${UsbAudioStatus.formatSampleRate(item.sampleRate)}"
            } else "PCM"
        }
    }

    // Procedural color palette for tracks without artwork
    val fallbackColors = remember(item.id) {
        FallbackProceduralPalettes[(item.id % FallbackProceduralPalettes.size).toInt().coerceAtLeast(0)]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                if (isSelected) Color(0x3300E5FF)
                else if (isPlaying) Color(0x1F00E5FF)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
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
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Album artwork / Procedural Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(fallbackColors))
                .border(1.dp, if (isPlaying) AudiophileCyan.copy(alpha = 0.5f) else Color(0x1AFFFFFF), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Centered fallback glyph: ALWAYS rendered underneath with zero lag
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = if (isPlaying) AudiophileCyan else Color(0xFF8A99AD),
                modifier = Modifier.size(22.dp)
            )

            // AsyncImage: renders on top only when artwork is present
            if (imageRequest != null && item.artworkUri != null && !com.mediaplayer.util.AudioArtworkFetcher.noArtworkCache.contains(item.artworkUri)) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Playing",
                        tint = AudiophileCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPlaying) AudiophileCyan else Color.White,
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
                    color = if (isHiRes) AudiophileGold else AudiophileCyan,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isHiRes) Color(0x33FFD54F) else Color(0x2200E5FF))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )

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

        // 3-dots more action button
        IconButton(
            onClick = onShowOptions,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Track Options",
                tint = Color(0xFF808595),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------- AUDIO & DAC LAB -----------------
@Composable
private fun AudioDacLabScreen(
    telemetry: UsbAudioStatus,
    decoderMode: String,
    onSetBitPerfect: (Boolean) -> Unit,
    onSetDecoderMode: (String) -> Unit,
    onRequestUsbPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Unified Audiophile Studio Master Console & Signal Deck
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (telemetry.isMixerBypassActive) Color(0xFF14120B) else Color(0xFF0F1118)
            ),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (telemetry.isMixerBypassActive) AudiophileGold.copy(alpha = 0.7f) else Color(0x2AFFFFFF)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header Row: GraphicEq Icon + Title + Status Pill + Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (telemetry.isMixerBypassActive) Color(0x33FFD54F) else Color(0x2200E5FF)
                                )
                                .border(
                                    1.dp,
                                    if (telemetry.isMixerBypassActive) AudiophileGold.copy(alpha = 0.6f) else Color(0x3300E5FF),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bit-Perfect Audio Bypass",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (telemetry.isMixerBypassActive) AudiophileGold
                                            else if (telemetry.isUsbDacConnected) AudiophileCyan
                                            else Color(0xFF888899)
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (telemetry.isMixerBypassActive) "BIT-PERFECT HAL LOCKED"
                                    else if (telemetry.isUsbDacConnected) "USB DAC DIRECT READY"
                                    else "SYSTEM MIXER STANDBY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp,
                                    color = if (telemetry.isMixerBypassActive) AudiophileGold
                                    else if (telemetry.isUsbDacConnected) AudiophileCyan
                                    else Color(0xFF8E8E9E)
                                )
                            }
                        }
                    }

                    Switch(
                        checked = telemetry.isExclusiveBitPerfectEnabled,
                        onCheckedChange = onSetBitPerfect,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AudiophileGold,
                            checkedTrackColor = Color(0x66FFD54F),
                            uncheckedThumbColor = Color(0xFF888899),
                            uncheckedTrackColor = Color(0x33888888)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Integrated Elegant Signal Path Flow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SignalNode(
                            label = "Source",
                            value = telemetry.sourceCodec.ifEmpty { "PCM" },
                            active = true
                        )
                        SignalArrow(active = true)
                        SignalNode(
                            label = "Decoder",
                            value = if (decoderMode.contains("FFmpeg")) "FFmpeg 6" else "MediaCodec",
                            active = true
                        )
                        SignalArrow(active = true)
                        SignalNode(
                            label = "Bypass",
                            value = if (telemetry.isMixerBypassActive) "DIRECT" else "Flinger",
                            active = telemetry.isMixerBypassActive,
                            highlightGold = true
                        )
                        SignalArrow(active = telemetry.isMixerBypassActive)
                        SignalNode(
                            label = "Output",
                            value = if (telemetry.isUsbDacConnected) "USB DAC" else "Internal",
                            active = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (telemetry.isMixerBypassActive)
                        "Direct hardware HAL bypass active. AudioFlinger 48kHz resampler and DSP limiters are completely bypassed."
                    else
                        "Directly bypasses the Android 48kHz mixer and DSP limiters on Android 14+ (API 34+) when a USB DAC is connected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9EAE),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Hardware USB DAC Telemetry HUD (2x2 Grid)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "HARDWARE TELEMETRY & ROUTE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AudiophileCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Telemetry Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryCard(
                        title = "DAC Device",
                        value = telemetry.dacName,
                        highlight = telemetry.isUsbDacConnected,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryCard(
                        title = "Connection",
                        value = if (telemetry.isUsbDacConnected) "USB-C Direct OTG" else "Internal Bus",
                        highlight = telemetry.isUsbDacConnected,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryCard(
                        title = "Mixer Pipeline",
                        value = telemetry.mixerBehavior,
                        highlight = telemetry.isMixerBypassActive,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryCard(
                        title = "Active Stream",
                        value = telemetry.sourceFormatText,
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (telemetry.supportedSampleRates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Supported Hardware Sample Rates:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E9E))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(telemetry.supportedSampleRates) { rate ->
                            Text(
                                text = UsbAudioStatus.formatSampleRate(rate),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AudiophileGold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x22FFD54F))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (telemetry.isUsbDacConnected && !telemetry.hasUsbPermission) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRequestUsbPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Direct USB Hardware Access", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Audio Decoder Preference Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "AUDIO / VIDEO DECODER ARCHITECTURE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AudiophileGold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Decoder Option 1: FFmpeg Native
                val isFfmpeg = decoderMode.contains("FFmpeg")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isFfmpeg) Color(0x1F00E5FF) else Color(0x0AFFFFFF))
                        .border(1.dp, if (isFfmpeg) AudiophileCyan else Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onSetDecoderMode("FFmpeg Extension (Universal)") }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isFfmpeg,
                            onClick = { onSetDecoderMode("FFmpeg Extension (Universal)") },
                            colors = RadioButtonDefaults.colors(selectedColor = AudiophileCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("FFmpeg Native Core (Bit-Accurate)", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Jellyfin libffmpeg 6.0: FLAC, ALAC, DTS, TrueHD, AC3, DSD bit-accurate decoding.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9EAE))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Decoder Option 2: MediaCodec Hardware
                val isMediaCodec = decoderMode.contains("MediaCodec")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isMediaCodec) Color(0x1F00E5FF) else Color(0x0AFFFFFF))
                        .border(1.dp, if (isMediaCodec) AudiophileCyan else Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                        .clickable { onSetDecoderMode("MediaCodec Hardware") }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isMediaCodec,
                            onClick = { onSetDecoderMode("MediaCodec Hardware") },
                            colors = RadioButtonDefaults.colors(selectedColor = AudiophileCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Android MediaCodec (Hardware DSP)", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Snapdragon silicon hardware decoders for ultra-low power playback.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9EAE))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SignalNode(label: String, value: String, active: Boolean, highlightGold: Boolean = false) {
    val activeColor = if (highlightGold) AudiophileGold else AudiophileCyan
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) activeColor.copy(alpha = 0.15f) else Color(0x11FFFFFF))
                .border(1.dp, if (active) activeColor else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (active) activeColor else Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 8.sp, color = Color(0xFF7E8A9A))
    }
}

@Composable
private fun SignalArrow(active: Boolean) {
    Text(
        text = "➔",
        fontSize = 11.sp,
        color = if (active) AudiophileCyan else Color(0xFF445566),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, if (highlight) AudiophileCyan.copy(alpha = 0.3f) else Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7E8A9A),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (highlight) Color.White else Color(0xFFAAAAAA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LabRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E9E))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PermissionOnboardingCard(
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1F00E5FF)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = AudiophileCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Media Storage Access", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Grant permission to browse device video and audiophile music, or pick single media files.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0C4))
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onPickFiles,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Pick Media File", color = AudiophileCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    subtitle: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x2200E5FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = AudiophileCyan, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E9E), modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Media Files", color = Color.Black, fontWeight = FontWeight.Bold)
        }
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

@Composable
private fun FloatingDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    activeColor: Color = AudiophileCyan
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val itemColor = if (selected) activeColor else Color(0xFF9093A0)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = itemColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                letterSpacing = (-0.1).sp
            )
        }
    }
}

@Composable
private fun DeleteMediaConfirmDialog(
    items: List<MediaItemModel>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (items.isEmpty()) return
    val isSingle = items.size == 1
    val singleItem = items.first()
    val totalBytes = items.sumOf { it.sizeBytes }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141722),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0x22FF453A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isSingle) "Delete from Device Storage?" else "Delete ${items.size} Files?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSingle) {
                        "Are you sure you want to permanently delete \"${singleItem.title}\"?"
                    } else {
                        "Are you sure you want to permanently delete these ${items.size} files from device storage?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD0D0DC),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2230),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSingle) {
                                if (singleItem.isVideo) "VIDEO FILE" else "AUDIO FILE"
                            } else {
                                val vCount = items.count { it.isVideo }
                                val aCount = items.count { !it.isVideo }
                                when {
                                    vCount > 0 && aCount > 0 -> "$vCount VIDEOS • $aCount AUDIOS"
                                    vCount > 0 -> "$vCount VIDEO FILES"
                                    else -> "$aCount AUDIO FILES"
                                }
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AudiophileCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatFileSize(totalBytes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA0AAB8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (!isSingle) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.take(4).forEach { item ->
                            Text(
                                text = "• ${item.title}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFFA0A8B8),
                                fontSize = 12.sp
                            )
                        }
                        if (items.size > 4) {
                            Text(
                                text = "... and ${items.size - 4} more files",
                                color = AudiophileCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "This will physically remove the file${if (isSingle) "" else "s"} from your device storage. This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888898),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
            ) {
                Text(
                    text = if (isSingle) "Delete Permanently" else "Delete ${items.size} Files",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = Color(0xFFA0A0B0))
            }
        }
    )
}


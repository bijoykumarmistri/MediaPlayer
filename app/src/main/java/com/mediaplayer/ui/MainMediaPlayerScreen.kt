package com.mediaplayer.ui

import android.Manifest
import android.app.Activity
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
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.data.MediaLibraryRepository
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.components.AppUpdateDialog
import com.mediaplayer.ui.components.BottomMiniPlayer
import com.mediaplayer.ui.components.DeleteMediaConfirmDialog
import com.mediaplayer.ui.components.FloatingDockItem
import com.mediaplayer.ui.components.MediaHubPage
import com.mediaplayer.ui.components.MusicFolderItem
import com.mediaplayer.ui.components.VideoCustomizationDialog
import com.mediaplayer.ui.components.VideoFolderItem
import com.mediaplayer.ui.components.formatFileSize
import com.mediaplayer.ui.components.formatFolderDuration
import com.mediaplayer.ui.components.shareMediaFiles
import com.mediaplayer.ui.music.MusicNowPlayingScreen
import com.mediaplayer.ui.music.MusicTabScreen
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurface
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.ui.video.VideoPlayerView
import com.mediaplayer.ui.video.VideosTabScreen
import com.mediaplayer.util.AppReleaseInfo
import com.mediaplayer.util.AppSettingsManager
import com.mediaplayer.util.AppUpdateManager
import com.mediaplayer.util.HighRefreshRateManager
import com.mediaplayer.vault.PrivacyVaultScreen
import com.mediaplayer.vault.VaultPinDialog
import com.mediaplayer.vault.VaultRepository
import com.mediaplayer.vault.VaultSecurityManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mxplayer.mpv.PlayerViewModel
import kotlinx.coroutines.withContext

@Composable
fun MainMediaPlayerScreen(
    repository: MediaLibraryRepository,
    vaultRepository: VaultRepository,
    vaultSecurityManager: VaultSecurityManager,
    playerManager: MediaPlayerManager,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val inPip = isInPipMode || (activity?.isInPictureInPictureMode == true)
    val scope = rememberCoroutineScope()

    val videoViewModel: PlayerViewModel = viewModel()

    // Keep playerManager and videoViewModel synchronized across all screens & bottom mini player
    DisposableEffect(videoViewModel) {
        videoViewModel.onIsPlayingChanged = { playing ->
            playerManager.setVideoPlaying(playing)
        }
        videoViewModel.onProgressUpdate = { posMs, durMs ->
            playerManager.updateVideoPosition(posMs, durMs)
        }
        playerManager.onToggleVideoPlayPause = { videoViewModel.togglePlayPause() }
        playerManager.onPauseVideo = { videoViewModel.pausePlayback() }
        playerManager.onStopVideo = {
            videoViewModel.stopPlayback()
            playerManager.clearVideoCallbacks()
            playerManager.setVideoPlaying(false)
        }
        playerManager.onSeekVideoRelative = { deltaSec -> videoViewModel.seekRelative(deltaSec) }
        onDispose {
            playerManager.clearVideoCallbacks()
        }
    }

    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isExpanded by playerManager.isPlayerExpanded.collectAsState()
    val playbackPositions by playerManager.playbackPositions.collectAsState()
    val playedMediaIds by playerManager.playedMediaIds.collectAsState()

    var selectedDestination by remember {
        mutableIntStateOf(playerManager.historyManager.getLastTab().coerceIn(0, 2))
    }
    var recentStreamsState by remember {
        mutableStateOf<List<com.mediaplayer.playback.StreamHistoryItem>>(emptyList())
    }
    LaunchedEffect(selectedDestination) {
        if (selectedDestination == 2) {
            recentStreamsState = withContext(Dispatchers.IO) {
                playerManager.historyManager.getRecentStreams()
            }
        }
    }
    var audioList by remember { mutableStateOf(repository.getCachedAudiosFast()) }
    var videoList by remember { mutableStateOf(repository.getCachedVideosFast()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val settingsManager = remember { AppSettingsManager(context) }
    var showVideoSettingsDialog by remember { mutableStateOf(false) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    val highRefreshRateManager = remember(activity) { activity?.let { HighRefreshRateManager(it) } }

    var appReleaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }

    // Privacy Vault states
    var showVaultScreen by remember { mutableStateOf(false) }
    var showVaultPinDialog by remember { mutableStateOf(false) }
    var isMovingToVault by remember { mutableStateOf(false) }

    // Automatic silent check for GitHub releases on app startup (deferred to prioritize cold-start render)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
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

    // Music view mode: Grid vs List (default to List view)
    var isMusicGridView by remember { mutableStateOf(false) }
    var openedMusicFolder by remember { mutableStateOf<MusicFolderItem?>(null) }
    var musicSortOrder by remember { mutableIntStateOf(0) }
    var showMusicSortMenu by remember { mutableStateOf(false) }

    // Music filters: Folders, All Tracks, Albums, Artists, Hi-Res / Lossless
    var musicFilterTab by remember { mutableStateOf("Folders") }

    // Multi-Selection and Deletion state
    var selectedMediaItems by remember { mutableStateOf<Set<MediaItemModel>>(emptySet()) }
    var itemsToDelete by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }
    var pendingConsentItems by remember { mutableStateOf<List<MediaItemModel>>(emptyList()) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        itemsToDelete = emptyList()
        if (result.resultCode == Activity.RESULT_OK) {
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
        val audioGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        val videoGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true
        val storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val partialGranted = Build.VERSION.SDK_INT >= 34 &&
            permissions["android.permission.READ_MEDIA_VISUAL_USER_SELECTED"] == true

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
            if (newAudios.size != audioList.size || (newAudios.isNotEmpty() && newAudios.first().id != audioList.first().id)) {
                audioList = newAudios
                repository.saveCachedAudios(newAudios)
                changed = true
            }
            if (newVideos.size != videoList.size || (newVideos.isNotEmpty() && newVideos.first().id != videoList.first().id)) {
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
        // Immediately restore last played media in paused state from persistent cache without re-querying
        if (playerManager.currentMediaItem.value == null) {
            playerManager.restoreLastPlayedMedia(videoList + audioList)
        }
        // Defer background media filesystem scan until after cold-start rendering is complete
        kotlinx.coroutines.delay(2500)
        reloadMedia()
    }

    // Predictive Back Gesture Animation Handler
    PredictiveBackHandler(enabled = isExpanded && !inPip) { progressFlow ->
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
    val activeVideoList = remember(videoList, settingsManager.hideShortVideos, settingsVersion) {
        if (settingsManager.hideShortVideos) {
            val minDurationMs = settingsManager.minVideoDurationSec * 1000L
            videoList.filter { it.durationMs >= minDurationMs }
        } else {
            videoList
        }
    }

    val videoFolders = remember(activeVideoList, selectedDestination) {
        if (selectedDestination == 0) {
            listOf("All Folders") + activeVideoList.map { it.folderName.ifEmpty { "Videos" } }.distinct()
        } else emptyList()
    }

    val musicFolders = remember(audioList, selectedDestination) {
        if (selectedDestination == 1) {
            listOf("All Folders") + audioList.map { it.folderName.ifEmpty { "Music" } }.distinct()
        } else emptyList()
    }

    val filteredVideo = remember(activeVideoList, searchQuery, selectedVideoFolder, selectedDestination) {
        if (selectedDestination != 0) emptyList()
        else activeVideoList.filter { video ->
            val matchesFolder = selectedVideoFolder == "All Folders" || (video.folderName.ifEmpty { "Videos" } == selectedVideoFolder)
            val matchesQuery = searchQuery.isBlank() || video.title.contains(searchQuery, ignoreCase = true) || video.artist.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }

    val filteredAudio = remember(audioList, searchQuery, selectedDestination) {
        if (selectedDestination != 1) emptyList()
        else if (searchQuery.isBlank()) audioList
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
        val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
        val density = androidx.compose.ui.platform.LocalDensity.current
        val isWideScreen = with(density) { windowInfo.containerSize.width.toDp() } >= 640.dp

        Box(modifier = Modifier.fillMaxSize()) {
            if (!inPip) {
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
                                    selectedMediaItems = emptySet()
                                    if (selectedDestination == 0 && openedVideoFolder != null) {
                                        openedVideoFolder = null
                                    } else {
                                        selectedDestination = 0
                                        playerManager.historyManager.saveLastTab(0)
                                    }
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
                                    selectedMediaItems = emptySet()
                                    if (selectedDestination == 1 && openedMusicFolder != null) {
                                        openedMusicFolder = null
                                    } else {
                                        selectedDestination = 1
                                        playerManager.historyManager.saveLastTab(1)
                                    }
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
                                                        selectedMediaItems = if (isAllSelected) {
                                                            emptySet()
                                                        } else {
                                                            currentPool.toSet()
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
                                                    text = "Videos",
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

                                                // Settings / Customization dialog
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x1AFFFFFF))
                                                        .clickable { showVideoSettingsDialog = true },
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
                                    if (openedMusicFolder != null) {
                                        val folder = openedMusicFolder!!
                                        // Music Folder Navigation Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Back Button in 38.dp glass circle
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable { openedMusicFolder = null },
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
                                                            text = "${folder.tracks.size} Tracks",
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
                                                        .background(if (musicSortOrder != 0) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                                                        .clickable { showMusicSortMenu = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Tune,
                                                        contentDescription = "Sort",
                                                        tint = if (musicSortOrder != 0) AudiophileCyan else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showMusicSortMenu,
                                                    onDismissRequest = { showMusicSortMenu = false },
                                                    modifier = Modifier.background(OledSurfaceVariant)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Date Added", color = Color.White) },
                                                        onClick = { musicSortOrder = 0; showMusicSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Title (A-Z)", color = Color.White) },
                                                        onClick = { musicSortOrder = 1; showMusicSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Artist", color = Color.White) },
                                                        onClick = { musicSortOrder = 2; showMusicSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by File Size", color = Color.White) },
                                                        onClick = { musicSortOrder = 3; showMusicSortMenu = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sort by Duration", color = Color.White) },
                                                        onClick = { musicSortOrder = 4; showMusicSortMenu = false }
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
                                                    .clickable { isMusicGridView = !isMusicGridView },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isMusicGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                                    contentDescription = "Toggle Grid/List",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Main Music Header (Folder / Library list)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Music",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Text(
                                                    text = "${musicFolders.size} Folders • ${audioList.size} Tracks",
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
                                                        .clickable { isMusicGridView = !isMusicGridView },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isMusicGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                                        contentDescription = "Toggle View",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                // Sort Action in 38.dp glass circle
                                                Box {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(if (musicSortOrder != 0) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                                                            .clickable { showMusicSortMenu = true },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Tune,
                                                            contentDescription = "Sort",
                                                            tint = if (musicSortOrder != 0) AudiophileCyan else Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = showMusicSortMenu,
                                                        onDismissRequest = { showMusicSortMenu = false },
                                                        modifier = Modifier.background(OledSurfaceVariant)
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Sort by Date Added", color = Color.White) },
                                                            onClick = { musicSortOrder = 0; showMusicSortMenu = false }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Sort by Title (A-Z)", color = Color.White) },
                                                            onClick = { musicSortOrder = 1; showMusicSortMenu = false }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Sort by Artist", color = Color.White) },
                                                            onClick = { musicSortOrder = 2; showMusicSortMenu = false }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Sort by File Size", color = Color.White) },
                                                            onClick = { musicSortOrder = 3; showMusicSortMenu = false }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Sort by Duration", color = Color.White) },
                                                            onClick = { musicSortOrder = 4; showMusicSortMenu = false }
                                                        )
                                                    }
                                                }

                                                // Refresh button
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
                                    }
                                } else {
                                    // Top App Bar for Media Hub
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Media Hub",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Storage & Streams",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF9E9EAE)
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
                                        onValueChange = { query: String -> searchQuery = query },
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
                                // Persistent Mini-Player Bar (Audio & Video)
                                AnimatedVisibility(
                                    visible = currentItem != null && !isExpanded,
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
                                        onExpand = { playerManager.setPlayerExpanded(true) }
                                    )
                                }

                                // Unified Floating Glassmorphic Navigation Island on Phones
                                if (!isWideScreen) {
                                    val activeAccentColor = AudiophileCyan
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
                                                        .fillMaxWidth(1f / 3f)
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
                                                            if (selectedDestination == 1 && openedMusicFolder != null) {
                                                                openedMusicFolder = null
                                                            } else {
                                                                selectedDestination = 1
                                                                playerManager.historyManager.saveLastTab(1)
                                                            }
                                                        },
                                                        icon = Icons.Rounded.MusicNote,
                                                        label = "Music",
                                                        activeColor = AudiophileCyan,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    // 2: Media Hub
                                                    FloatingDockItem(
                                                        selected = selectedDestination == 2,
                                                        onClick = {
                                                            selectedMediaItems = emptySet()
                                                            selectedDestination = 2
                                                            playerManager.historyManager.saveLastTab(2)
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
                    ) { innerPadding: PaddingValues ->
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
                                            isGridView = isMusicGridView,
                                            openedFolder = openedMusicFolder,
                                            onOpenFolder = {
                                                selectedMediaItems = emptySet()
                                                openedMusicFolder = it
                                            },
                                            sortOrder = musicSortOrder,
                                            currentPlayingId = currentItem?.id,
                                            hasPermission = hasPermission,
                                            onToggleView = { isMusicGridView = !isMusicGridView },
                                            filterTab = musicFilterTab,
                                            onFilterTabSelect = {
                                                musicFilterTab = it
                                                openedMusicFolder = null
                                            },
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
                                            searchQuery = searchQuery,
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
                                            onOpenPrivacyVault = { openPrivacyVault() },
                                            onCheckAppUpdate = {
                                                scope.launch {
                                                    val result = AppUpdateManager.checkForUpdates(context)
                                                    result.onSuccess { info ->
                                                        appReleaseInfo = info
                                                        showAutoUpdateDialog = true
                                                    }.onFailure { err ->
                                                        Toast.makeText(
                                                            context,
                                                            "Check failed: ${err.localizedMessage ?: "Network error"}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
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
            }

            // Full-Screen Player with Predictive Back Morphing (Hardware accelerated via graphicsLayer)
            AnimatedVisibility(
                visible = (isExpanded || inPip) && currentItem != null,
                enter = if (inPip) EnterTransition.None else (slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing))),
                exit = if (inPip) ExitTransition.None else (slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing))),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (!inPip) {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                translationY = predictiveBackProgress * 260.dp.toPx()
                                if (animatedCornerRadius > 0.dp) {
                                    clip = true
                                    shape = RoundedCornerShape(animatedCornerRadius)
                                }
                            }
                        }
                        .background(Color.Black)
                ) {
                    if (currentItem?.isVideo == true) {
                        VideoPlayerView(
                            playerManager = playerManager,
                            videoViewModel = videoViewModel,
                            onBack = {
                                playerManager.setPlayerExpanded(false)
                            },
                            onEnterPip = onEnterPip,
                            isInPipMode = inPip,
                            settingsManager = settingsManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!inPip) {
                        MusicNowPlayingScreen(
                            playerManager = playerManager,
                            onCollapse = { playerManager.setPlayerExpanded(false) }
                        )
                    }
                }
            }

            if (!inPip) {
                // Video & Playback Customization Dialog
                if (showVideoSettingsDialog) {
                    VideoCustomizationDialog(
                        settingsManager = settingsManager,
                        onDismiss = { showVideoSettingsDialog = false },
                        onSettingsChanged = {
                            settingsVersion++
                            if (settingsManager.highRefreshRateEnabled) {
                                highRefreshRateManager?.applyOptimalRefreshRate()
                            }
                        }
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

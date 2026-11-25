package com.mediaplayer.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
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
import com.mediaplayer.distribution.DistributionPlatform
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.components.AppUpdateDialog
import com.mediaplayer.ui.components.BottomMiniPlayer
import com.mediaplayer.ui.components.DeleteMediaConfirmDialog
import com.mediaplayer.ui.components.FloatingDockItem
import com.mediaplayer.ui.components.MediaHubPage
import com.mediaplayer.ui.components.RenameFileDialog
import com.mediaplayer.ui.components.MusicFolderItem
import com.mediaplayer.ui.components.VideoCustomizationDialog
import com.mediaplayer.ui.components.VideoFolderItem
import com.mediaplayer.ui.components.formatFileSize
import com.mediaplayer.ui.components.formatFolderDuration
import com.mediaplayer.ui.components.shareMediaFiles
import com.mediaplayer.ui.music.MusicNowPlayingScreen
import com.mediaplayer.ui.music.MusicTabScreen
import com.mediaplayer.ui.theme.MediaPlayerTheme
import com.mediaplayer.ui.video.VideoPlayerView
import com.mediaplayer.ui.video.VideosTabScreen
import com.mediaplayer.util.AppReleaseInfo
import com.mediaplayer.util.AppSettingsManager
import com.mediaplayer.util.AppUpdateManager
import com.mediaplayer.vault.PrivacyVaultScreen
import com.mediaplayer.vault.VaultPinDialog
import com.mediaplayer.vault.VaultRepository
import com.mediaplayer.vault.VaultSecurityManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bkm.player.mpv.PlayerViewModel
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainMediaPlayerScreen(
    repository: MediaLibraryRepository,
    vaultRepository: VaultRepository,
    vaultSecurityManager: VaultSecurityManager,
    playerManager: MediaPlayerManager,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier,
    onRefreshRateSettingChanged: () -> Unit = {}
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
            videoViewModel.stopPlayback(releaseEngine = true)
            playerManager.clearVideoCallbacks()
            playerManager.setVideoPlaying(playing = false)
        }
        playerManager.onSeekVideoRelative = { deltaSec -> videoViewModel.seekRelative(deltaSec) }
        playerManager.onSeekVideoAbsolute = { posMs -> videoViewModel.seekTo(posMs / 1000.0) }
        onDispose {
            playerManager.clearVideoCallbacks()
        }
    }

    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isExpanded by playerManager.isPlayerExpanded.collectAsState()
    val isVideoPlaying by playerManager.isVideoPlaying.collectAsState()
    val isPlayerObscured = (isExpanded || inPip) && (currentItem != null)

    val settingsManager = remember { AppSettingsManager(context) }

    // Keep background video loading & EOF auto-advance working even when the video player is collapsed to the mini-player
    LaunchedEffect(currentItem?.id, isExpanded, inPip, isVideoPlaying) {
        val item = currentItem
        if (item != null && item.isVideo && !isExpanded && !inPip && isVideoPlaying) {
            if (videoViewModel.currentLoadedUri != item.uri) {
                val resumePos = playerManager.consumeVideoStartPositionOverride() ?: (
                    if (settingsManager.autoResume) {
                        val saved = playerManager.getSavedPosition(item.id)
                        val dur = item.durationMs
                        if (saved > 3000L && (dur <= 0L || saved < dur - 5000L)) saved else 0L
                    } else 0L
                )
                videoViewModel.onPositionUpdate = { posMs, durMs ->
                    playerManager.updateVideoPosition(posMs, durMs)
                    playerManager.saveVideoPlaybackPosition(item.id, posMs, durMs)
                }
                if (videoViewModel.onEndOfFileReached == null) {
                    videoViewModel.onEndOfFileReached = {
                        if (settingsManager.autoPlayNextVideo) {
                            playerManager.playNextVideo()
                        }
                    }
                }
                videoViewModel.openDocument(
                    context = context,
                    uri = item.uri,
                    titleHint = item.title,
                    startPositionMs = resumePos,
                    allowAudioOnlyWithoutSurface = true
                )
            }
        }
    }

    var selectedDestination by remember {
        mutableIntStateOf(playerManager.historyManager.getLastTab().coerceIn(0, 2))
    }
    var recentStreamsState by remember {
        mutableStateOf<List<com.mediaplayer.playback.StreamHistoryItem>>(emptyList())
    }
    var audioList by remember { mutableStateOf(repository.getCachedAudiosFast()) }
    var videoList by remember { mutableStateOf(repository.getCachedVideosFast()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val pitchBlackEnabled by AppSettingsManager.pitchBlackThemeFlow(context).collectAsState()
    var showVideoSettingsDialog by remember { mutableStateOf(false) }
    var settingsVersion by remember { mutableIntStateOf(0) }

    var appReleaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }

    // Privacy Vault states
    var showVaultScreen by remember { mutableStateOf(false) }
    var showVaultPinDialog by remember { mutableStateOf(false) }
    var isMovingToVault by remember { mutableStateOf(false) }
    var vaultItemCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedDestination, showVaultScreen) {
        if (selectedDestination == 2 && !showVaultScreen) {
            recentStreamsState = withContext(Dispatchers.IO) {
                playerManager.historyManager.getRecentStreams()
            }
            vaultItemCount = withContext(Dispatchers.IO) {
                vaultRepository.getVaultItems().size
            }
        }
    }

    // Automatic silent check for app updates on startup (deferred to prioritize cold-start render)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000.milliseconds)
        val updateResult = DistributionPlatform.checkForUpdates(context, isManual = false)
        updateResult.onSuccess { info ->
            if (info != null && info.isUpdateAvailable) {
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
    var itemToRename by remember { mutableStateOf<MediaItemModel?>(null) }
    var pendingConsentRename by remember { mutableStateOf<Pair<MediaItemModel, String>?>(null) }

    fun applyRenameSuccess(oldItem: MediaItemModel, newItem: MediaItemModel) {
        if (oldItem.isVideo) {
            videoList = videoList.map { if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it }
            if (openedVideoFolder != null) {
                val updatedFolderVideos = openedVideoFolder!!.videos.map {
                    if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it
                }
                openedVideoFolder = openedVideoFolder!!.copy(videos = updatedFolderVideos)
            }
        } else {
            audioList = audioList.map { if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it }
            if (openedMusicFolder != null) {
                val updatedFolderTracks = openedMusicFolder!!.tracks.map {
                    if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it
                }
                openedMusicFolder = openedMusicFolder!!.copy(tracks = updatedFolderTracks)
            }
        }
        playerManager.updateMediaItem(newItem)
        selectedMediaItems = selectedMediaItems
            .asSequence()
            .map { if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it }
            .toSet()
        Toast.makeText(context, "Renamed to ${newItem.title}", Toast.LENGTH_SHORT).show()
    }

    val renameConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pending = pendingConsentRename
            if (pending != null) {
                val (item, newDisplayName) = pending
                scope.launch {
                    val updated = repository.completeRenameAfterConsent(item, newDisplayName)
                    if (updated != null) {
                        applyRenameSuccess(item, updated)
                    } else {
                        Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        pendingConsentRename = null
    }

    fun performRename(item: MediaItemModel, newName: String) {
        scope.launch {
            val updated = repository.renameMediaFile(item, newName) { intentSender, displayNameToRename ->
                pendingConsentRename = Pair(item, displayNameToRename)
                renameConsentLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            if (updated != null) {
                applyRenameSuccess(item, updated)
            }
        }
    }

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
            val audiosChanged = newAudios != audioList
            val videosChanged = newVideos != videoList
            val changed = audiosChanged || videosChanged
            if (changed) {
                withContext(Dispatchers.Main) {
                    if (audiosChanged) audioList = newAudios
                    if (videosChanged) videoList = newVideos
                }
                if (audiosChanged) repository.saveCachedAudios(newAudios)
                if (videosChanged) repository.saveCachedVideos(newVideos)
            }
            if (changed || playerManager.currentMediaItem.value == null) {
                playerManager.restoreLastPlayedMedia(newVideos + newAudios)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Preload memory cache asynchronously off main thread if cold start
        if (videoList.isEmpty() || audioList.isEmpty()) {
            withContext(Dispatchers.IO) {
                repository.preloadCache()
            }
            videoList = repository.getCachedVideosFast()
            audioList = repository.getCachedAudiosFast()
        }
        // Immediately restore last played media in paused state from persistent cache without re-querying
        if (playerManager.currentMediaItem.value == null) {
            playerManager.restoreLastPlayedMedia(videoList + audioList)
        }
        // Defer background media filesystem scan until after cold-start rendering settles;
        // ExoPlayer/MediaSession initialize lazily when audio or stream playback begins.
        kotlinx.coroutines.delay(2200.milliseconds)
        reloadMedia()
    }

    // Predictive Back Gesture Animation Handler
    PredictiveBackHandler(enabled = isExpanded && !inPip) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }
            if (currentItem?.isVideo == true && !settingsManager.backgroundPlayback) {
                playerManager.stopVideo()
            } else {
                playerManager.setPlayerExpanded(false)
            }
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

    val videoFolderCount = remember(activeVideoList) {
        activeVideoList.asSequence().map {
            it.folderName.ifEmpty { "Videos" } to it.folderPath.ifEmpty { "/storage/emulated/0" }
        }.distinct().count()
    }

    val musicFolderCount = remember(audioList) {
        audioList.asSequence().map {
            it.folderName.ifEmpty { "Music" } to it.folderPath.ifEmpty { "/storage/emulated/0/Music" }
        }.distinct().count()
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

    fun requestMediaPermissions() {
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
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
        val density = androidx.compose.ui.platform.LocalDensity.current
        val isWideScreen = with(density) { windowInfo.containerSize.width.toDp() } >= 640.dp

        Box(modifier = Modifier.fillMaxSize()) {
            if (!inPip) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideScreen) {
                        MainNavigationRail(
                            selectedDestination = selectedDestination,
                            onSelectVideos = {
                                selectedMediaItems = emptySet()
                                if (selectedDestination == 0 && openedVideoFolder != null) {
                                    openedVideoFolder = null
                                } else {
                                    selectedDestination = 0
                                    playerManager.historyManager.saveLastTab(0)
                                }
                            },
                            onSelectMusic = {
                                selectedMediaItems = emptySet()
                                if (selectedDestination == 1 && openedMusicFolder != null) {
                                    openedMusicFolder = null
                                } else {
                                    selectedDestination = 1
                                    playerManager.historyManager.saveLastTab(1)
                                }
                            },
                            onSelectMediaHub = {
                                selectedMediaItems = emptySet()
                                selectedDestination = 2
                                playerManager.historyManager.saveLastTab(2)
                            }
                        )
                    }

                    Scaffold(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            MainTopBarContainer(
                                selectedDestination = selectedDestination,
                                selectedMediaItems = selectedMediaItems,
                                currentSelectionPool = if (selectedDestination == 0) {
                                    openedVideoFolder?.videos ?: filteredVideo
                                } else {
                                    filteredAudio
                                },
                                openedVideoFolder = openedVideoFolder,
                                videoFolderCount = videoFolderCount,
                                activeVideoCount = activeVideoList.size,
                                videoSortOrder = videoSortOrder,
                                showVideoSortMenu = showVideoSortMenu,
                                isVideoGridView = isVideoGridView,
                                openedMusicFolder = openedMusicFolder,
                                musicFolderCount = musicFolderCount,
                                audioCount = audioList.size,
                                musicSortOrder = musicSortOrder,
                                showMusicSortMenu = showMusicSortMenu,
                                isMusicGridView = isMusicGridView,
                                isSearchActive = isSearchActive,
                                searchQuery = searchQuery,
                                onClearSelection = { selectedMediaItems = emptySet() },
                                onSelectAll = { pool -> selectedMediaItems = pool.toSet() },
                                onRenameSelected = { itemToRename = selectedMediaItems.first() },
                                onShareSelected = { shareMediaFiles(context, selectedMediaItems.toList()) },
                                onLockVaultSelected = { moveToPrivacyVault(selectedMediaItems.toList()) },
                                onDeleteSelected = { itemsToDelete = selectedMediaItems.toList() },
                                onCloseVideoFolder = { openedVideoFolder = null },
                                onToggleVideoSortMenu = { showVideoSortMenu = it },
                                onSelectVideoSortOrder = { videoSortOrder = it; showVideoSortMenu = false },
                                onToggleVideoGridView = { isVideoGridView = !isVideoGridView },
                                onToggleSearchActive = { isSearchActive = !isSearchActive },
                                onOpenVideoSettings = { showVideoSettingsDialog = true },
                                onCloseMusicFolder = { openedMusicFolder = null },
                                onToggleMusicSortMenu = { showMusicSortMenu = it },
                                onSelectMusicSortOrder = { musicSortOrder = it; showMusicSortMenu = false },
                                onToggleMusicGridView = { isMusicGridView = !isMusicGridView },
                                onReloadMedia = { reloadMedia() },
                                onSearchQueryChange = { searchQuery = it }
                            )
                        },
                        bottomBar = {
                            MainBottomBarContainer(
                                isMiniPlayerVisible = currentItem != null && !isExpanded,
                                isWideScreen = isWideScreen,
                                selectedDestination = selectedDestination,
                                playerManager = playerManager,
                                onExpandPlayer = { playerManager.setPlayerExpanded(true) },
                                onSelectVideos = {
                                    selectedMediaItems = emptySet()
                                    if (selectedDestination == 0 && openedVideoFolder != null) {
                                        openedVideoFolder = null
                                    } else {
                                        selectedDestination = 0
                                        playerManager.historyManager.saveLastTab(0)
                                    }
                                },
                                onSelectMusic = {
                                    selectedMediaItems = emptySet()
                                    if (selectedDestination == 1 && openedMusicFolder != null) {
                                        openedMusicFolder = null
                                    } else {
                                        selectedDestination = 1
                                        playerManager.historyManager.saveLastTab(1)
                                    }
                                },
                                onSelectMediaHub = {
                                    selectedMediaItems = emptySet()
                                    selectedDestination = 2
                                    playerManager.historyManager.saveLastTab(2)
                                }
                            )
                        }
                    ) { innerPadding: PaddingValues ->
                        MainTabContentHost(
                            innerPadding = innerPadding,
                            selectedDestination = selectedDestination,
                            filteredVideo = filteredVideo,
                            activeVideoList = activeVideoList,
                            isVideoGridView = isVideoGridView,
                            openedVideoFolder = openedVideoFolder,
                            videoSortOrder = videoSortOrder,
                            filteredAudio = filteredAudio,
                            audioList = audioList,
                            isMusicGridView = isMusicGridView,
                            openedMusicFolder = openedMusicFolder,
                            musicSortOrder = musicSortOrder,
                            musicFilterTab = musicFilterTab,
                            playerManager = playerManager,
                            isPlayerObscured = isPlayerObscured,
                            currentPlayingId = currentItem?.id,
                            hasPermission = hasPermission,
                            searchQuery = searchQuery,
                            selectedMediaItems = selectedMediaItems,
                            recentStreamsState = recentStreamsState,
                            vaultItemCount = vaultItemCount,
                            onOpenVideoFolder = {
                                selectedMediaItems = emptySet()
                                openedVideoFolder = it
                            },
                            onToggleVideoView = { isVideoGridView = !isVideoGridView },
                            onOpenMusicFolder = {
                                selectedMediaItems = emptySet()
                                openedMusicFolder = it
                            },
                            onToggleMusicView = { isMusicGridView = !isMusicGridView },
                            onMusicFilterTabSelect = {
                                musicFilterTab = it
                                openedMusicFolder = null
                            },
                            onPlayMedia = { media, playlist, startPos ->
                                playerManager.playMedia(media, playlist, startPos)
                                playerManager.setPlayerExpanded(true)
                            },
                            onShuffleAllMusic = {
                                if (filteredAudio.isNotEmpty()) {
                                    val shuffled = filteredAudio.shuffled()
                                    playerManager.playMedia(shuffled.first(), shuffled)
                                    playerManager.setPlayerExpanded(true)
                                }
                            },
                            onRequestPermission = ::requestMediaPermissions,
                            onPickMediaFiles = { mimeTypes -> openFilesLauncher.launch(mimeTypes) },
                            onPickFolder = { openFolderLauncher.launch(null) },
                            onDeleteMedia = onDeleteMedia,
                            onRenameMedia = { itemToRename = it },
                            onMoveToVault = { item -> moveToPrivacyVault(listOf(item)) },
                            onToggleSelectItem = { item ->
                                selectedMediaItems = if (item in selectedMediaItems) selectedMediaItems - item else selectedMediaItems + item
                            },
                            onStartSelection = { item -> selectedMediaItems = setOf(item) },
                            onRemoveFromContinueWatching = { video -> playerManager.clearPlaybackHistory(video.id) },
                            onClearAllContinueWatching = { playerManager.clearAllPlaybackHistory() },
                            onPlayStream = { url, title, isVideo ->
                                playerManager.playUrl(url = url, title = title, isVideo = isVideo)
                                recentStreamsState = playerManager.historyManager.getRecentStreams()
                                playerManager.setPlayerExpanded(true)
                            },
                            onDeleteRecentStream = { url ->
                                playerManager.historyManager.removeRecentStream(url)
                                recentStreamsState = playerManager.historyManager.getRecentStreams()
                            },
                            onClearAllRecentStreams = {
                                playerManager.historyManager.clearRecentStreams()
                                recentStreamsState = emptyList()
                            },
                            onOpenPrivacyVault = ::openPrivacyVault,
                            onCheckAppUpdate = {
                                scope.launch {
                                    val result = DistributionPlatform.checkForUpdates(context, isManual = true)
                                    result.onSuccess { info ->
                                        if (info != null) {
                                            appReleaseInfo = info
                                            showAutoUpdateDialog = true
                                        }
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

                if (showVaultScreen) {
                    PrivacyVaultScreen(
                        vaultRepository = vaultRepository,
                        securityManager = vaultSecurityManager,
                        onPlayMedia = { item ->
                            playerManager.playMedia(item, listOf(item))
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

            MainExpandedPlayerOverlay(
                visible = (isExpanded || inPip) && (currentItem != null),
                isVideo = currentItem?.isVideo == true,
                inPip = inPip,
                animatedScale = animatedScale,
                animatedCornerRadius = animatedCornerRadius,
                predictiveBackProgress = predictiveBackProgress,
                pitchBlackEnabled = pitchBlackEnabled,
                playerManager = playerManager,
                videoViewModel = videoViewModel,
                settingsManager = settingsManager,
                onBackVideo = {
                    if (!settingsManager.backgroundPlayback) {
                        playerManager.stopVideo()
                    } else {
                        playerManager.setPlayerExpanded(false)
                    }
                },
                onEnterPip = onEnterPip,
                onCollapseMusic = { playerManager.setPlayerExpanded(false) }
            )

            if (!inPip) {
                MainModalDialogsHost(
                    showVideoSettingsDialog = showVideoSettingsDialog,
                    settingsManager = settingsManager,
                    onDismissVideoSettings = { showVideoSettingsDialog = false },
                    onSettingsChanged = {
                        settingsVersion++
                        onRefreshRateSettingChanged()
                    },
                    showAutoUpdateDialog = showAutoUpdateDialog,
                    appReleaseInfo = appReleaseInfo,
                    onDismissAutoUpdate = { showAutoUpdateDialog = false },
                    showVaultPinDialog = showVaultPinDialog,
                    vaultSecurityManager = vaultSecurityManager,
                    onVaultUnlockSuccess = {
                        showVaultPinDialog = false
                        showVaultScreen = true
                    },
                    onDismissVaultPin = { showVaultPinDialog = false },
                    isMovingToVault = isMovingToVault,
                    itemsToDelete = itemsToDelete,
                    onConfirmDelete = {
                        val targets = itemsToDelete
                        itemsToDelete = emptyList()
                        performDelete(targets)
                    },
                    onDismissDelete = { itemsToDelete = emptyList() },
                    itemToRename = itemToRename,
                    onConfirmRename = { newName ->
                        val target = itemToRename
                        itemToRename = null
                        if (target != null) {
                            performRename(target, newName)
                        }
                    },
                    onDismissRename = { itemToRename = null }
                )
            }
        }
    }
}

@Composable
private fun MainNavigationRail(
    selectedDestination: Int,
    onSelectVideos: () -> Unit,
    onSelectMusic: () -> Unit,
    onSelectMediaHub: () -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        NavigationRailItem(
            selected = selectedDestination == 0,
            onClick = onSelectVideos,
            icon = { Icon(Icons.Rounded.Movie, contentDescription = "Videos") },
            label = { Text("Videos") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationRailItem(
            selected = selectedDestination == 1,
            onClick = onSelectMusic,
            icon = { Icon(Icons.Rounded.MusicNote, contentDescription = "Music") },
            label = { Text("Music") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationRailItem(
            selected = selectedDestination == 2,
            onClick = onSelectMediaHub,
            icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = "Media Hub") },
            label = { Text("Media Hub") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun MainTopBarContainer(
    selectedDestination: Int,
    selectedMediaItems: Set<MediaItemModel>,
    currentSelectionPool: List<MediaItemModel>,
    openedVideoFolder: VideoFolderItem?,
    videoFolderCount: Int,
    activeVideoCount: Int,
    videoSortOrder: Int,
    showVideoSortMenu: Boolean,
    isVideoGridView: Boolean,
    openedMusicFolder: MusicFolderItem?,
    musicFolderCount: Int,
    audioCount: Int,
    musicSortOrder: Int,
    showMusicSortMenu: Boolean,
    isMusicGridView: Boolean,
    isSearchActive: Boolean,
    searchQuery: String,
    onClearSelection: () -> Unit,
    onSelectAll: (List<MediaItemModel>) -> Unit,
    onRenameSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onLockVaultSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCloseVideoFolder: () -> Unit,
    onToggleVideoSortMenu: (Boolean) -> Unit,
    onSelectVideoSortOrder: (Int) -> Unit,
    onToggleVideoGridView: () -> Unit,
    onToggleSearchActive: () -> Unit,
    onOpenVideoSettings: () -> Unit,
    onCloseMusicFolder: () -> Unit,
    onToggleMusicSortMenu: (Boolean) -> Unit,
    onSelectMusicSortOrder: (Int) -> Unit,
    onToggleMusicGridView: () -> Unit,
    onReloadMedia: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
    ) {
        if (selectedMediaItems.isNotEmpty()) {
            MainSelectionTopBar(
                selectedMediaItems = selectedMediaItems,
                currentPool = currentSelectionPool,
                onClearSelection = onClearSelection,
                onSelectAll = { onSelectAll(currentSelectionPool) },
                onRename = onRenameSelected,
                onShare = onShareSelected,
                onLockVault = onLockVaultSelected,
                onDelete = onDeleteSelected
            )
        } else {
            when (selectedDestination) {
                0 -> if (openedVideoFolder != null) {
                    MainVideosFolderTopBar(
                        folder = openedVideoFolder,
                        videoSortOrder = videoSortOrder,
                        showVideoSortMenu = showVideoSortMenu,
                        isVideoGridView = isVideoGridView,
                        onBack = onCloseVideoFolder,
                        onToggleSortMenu = onToggleVideoSortMenu,
                        onSelectSortOrder = onSelectVideoSortOrder,
                        onToggleGridView = onToggleVideoGridView
                    )
                } else {
                    MainVideosHomeTopBar(
                        videoFolderCount = videoFolderCount,
                        activeVideoCount = activeVideoCount,
                        isSearchActive = isSearchActive,
                        isVideoGridView = isVideoGridView,
                        onToggleSearch = onToggleSearchActive,
                        onToggleGridView = onToggleVideoGridView,
                        onOpenSettings = onOpenVideoSettings
                    )
                }

                1 -> if (openedMusicFolder != null) {
                    MainMusicFolderTopBar(
                        folder = openedMusicFolder,
                        musicSortOrder = musicSortOrder,
                        showMusicSortMenu = showMusicSortMenu,
                        isMusicGridView = isMusicGridView,
                        onBack = onCloseMusicFolder,
                        onToggleSortMenu = onToggleMusicSortMenu,
                        onSelectSortOrder = onSelectMusicSortOrder,
                        onToggleGridView = onToggleMusicGridView
                    )
                } else {
                    MainMusicHomeTopBar(
                        musicFolderCount = musicFolderCount,
                        audioCount = audioCount,
                        isSearchActive = isSearchActive,
                        isMusicGridView = isMusicGridView,
                        musicSortOrder = musicSortOrder,
                        showMusicSortMenu = showMusicSortMenu,
                        onToggleSearch = onToggleSearchActive,
                        onToggleGridView = onToggleMusicGridView,
                        onToggleSortMenu = onToggleMusicSortMenu,
                        onSelectSortOrder = onSelectMusicSortOrder,
                        onReloadMedia = onReloadMedia
                    )
                }

                else -> MainMediaHubTopBar()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        MainSearchBar(
            visible = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        )
    }
}

@Composable
private fun MainVideosFolderTopBar(
    folder: VideoFolderItem,
    videoSortOrder: Int,
    showVideoSortMenu: Boolean,
    isVideoGridView: Boolean,
    onBack: () -> Unit,
    onToggleSortMenu: (Boolean) -> Unit,
    onSelectSortOrder: (Int) -> Unit,
    onToggleGridView: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${folder.videos.size} ${if (folder.videos.size == 1) "Video" else "Videos"}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatFolderDuration(folder.totalDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (videoSortOrder != 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onToggleSortMenu(true) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Sort",
                    tint = if (videoSortOrder != 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showVideoSortMenu,
                onDismissRequest = { onToggleSortMenu(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                DropdownMenuItem(
                    text = { Text("Sort by Date Added", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelectSortOrder(0) }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Name (A-Z)", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelectSortOrder(1) }
                )
                DropdownMenuItem(
                    text = { Text("Sort by File Size", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelectSortOrder(2) }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Duration", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelectSortOrder(3) }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onToggleGridView),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideoGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                contentDescription = "Toggle Grid/List",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MainVideosHomeTopBar(
    videoFolderCount: Int,
    activeVideoCount: Int,
    isSearchActive: Boolean,
    isVideoGridView: Boolean,
    onToggleSearch: () -> Unit,
    onToggleGridView: () -> Unit,
    onOpenSettings: () -> Unit
) {
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
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$videoFolderCount ${if (videoFolderCount == 1) "Folder" else "Folders"} • $activeVideoCount Videos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
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
                    .background(if (isSearchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onToggleSearch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = if (isSearchActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onToggleGridView),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideoGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MainMusicFolderTopBar(
    folder: MusicFolderItem,
    musicSortOrder: Int,
    showMusicSortMenu: Boolean,
    isMusicGridView: Boolean,
    onBack: () -> Unit,
    onToggleSortMenu: (Boolean) -> Unit,
    onSelectSortOrder: (Int) -> Unit,
    onToggleGridView: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${folder.tracks.size} ${if (folder.tracks.size == 1) "Track" else "Tracks"}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatFolderDuration(folder.totalDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        MusicSortMenuButton(
            musicSortOrder = musicSortOrder,
            showMusicSortMenu = showMusicSortMenu,
            onToggleSortMenu = onToggleSortMenu,
            onSelectSortOrder = onSelectSortOrder
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onToggleGridView),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMusicGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                contentDescription = "Toggle Grid/List",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MainMusicHomeTopBar(
    musicFolderCount: Int,
    audioCount: Int,
    isSearchActive: Boolean,
    isMusicGridView: Boolean,
    musicSortOrder: Int,
    showMusicSortMenu: Boolean,
    onToggleSearch: () -> Unit,
    onToggleGridView: () -> Unit,
    onToggleSortMenu: (Boolean) -> Unit,
    onSelectSortOrder: (Int) -> Unit,
    onReloadMedia: () -> Unit
) {
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
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$musicFolderCount ${if (musicFolderCount == 1) "Folder" else "Folders"} • $audioCount Tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
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
                    .background(if (isSearchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onToggleSearch),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = if (isSearchActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onToggleGridView),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMusicGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            MusicSortMenuButton(
                musicSortOrder = musicSortOrder,
                showMusicSortMenu = showMusicSortMenu,
                onToggleSortMenu = onToggleSortMenu,
                onSelectSortOrder = onSelectSortOrder
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onReloadMedia),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicSortMenuButton(
    musicSortOrder: Int,
    showMusicSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    onSelectSortOrder: (Int) -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (musicSortOrder != 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onToggleSortMenu(true) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Sort",
                tint = if (musicSortOrder != 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = showMusicSortMenu,
            onDismissRequest = { onToggleSortMenu(false) },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            DropdownMenuItem(
                text = { Text("Sort by Date Added", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { onSelectSortOrder(0) }
            )
            DropdownMenuItem(
                text = { Text("Sort by Title (A-Z)", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { onSelectSortOrder(1) }
            )
            DropdownMenuItem(
                text = { Text("Sort by Artist", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { onSelectSortOrder(2) }
            )
            DropdownMenuItem(
                text = { Text("Sort by File Size", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { onSelectSortOrder(3) }
            )
            DropdownMenuItem(
                text = { Text("Sort by Duration", color = MaterialTheme.colorScheme.onSurface) },
                onClick = { onSelectSortOrder(4) }
            )
        }
    }
}

@Composable
private fun MainMediaHubTopBar() {
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sources, Streams & Storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MainSearchBar(
    visible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(100)),
        exit = shrinkVertically(
            animationSpec = tween(100, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(80))
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search videos, folders, music, artists...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun MainBottomBarContainer(
    isMiniPlayerVisible: Boolean,
    isWideScreen: Boolean,
    selectedDestination: Int,
    playerManager: MediaPlayerManager,
    onExpandPlayer: () -> Unit,
    onSelectVideos: () -> Unit,
    onSelectMusic: () -> Unit,
    onSelectMediaHub: () -> Unit
) {
    Column {
        AnimatedVisibility(
            visible = isMiniPlayerVisible,
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
                onExpand = onExpandPlayer
            )
        }

        if (!isWideScreen) {
            MainFloatingBottomDock(
                selectedDestination = selectedDestination,
                onSelectVideos = onSelectVideos,
                onSelectMusic = onSelectMusic,
                onSelectMediaHub = onSelectMediaHub
            )
        }
    }
}

@Composable
private fun MainTabContentHost(
    innerPadding: PaddingValues,
    selectedDestination: Int,
    filteredVideo: List<MediaItemModel>,
    activeVideoList: List<MediaItemModel>,
    isVideoGridView: Boolean,
    openedVideoFolder: VideoFolderItem?,
    videoSortOrder: Int,
    filteredAudio: List<MediaItemModel>,
    audioList: List<MediaItemModel>,
    isMusicGridView: Boolean,
    openedMusicFolder: MusicFolderItem?,
    musicSortOrder: Int,
    musicFilterTab: String,
    playerManager: MediaPlayerManager,
    isPlayerObscured: Boolean,
    currentPlayingId: Long?,
    hasPermission: Boolean,
    searchQuery: String,
    selectedMediaItems: Set<MediaItemModel>,
    recentStreamsState: List<com.mediaplayer.playback.StreamHistoryItem>,
    vaultItemCount: Int,
    onOpenVideoFolder: (VideoFolderItem?) -> Unit,
    onToggleVideoView: () -> Unit,
    onOpenMusicFolder: (MusicFolderItem?) -> Unit,
    onToggleMusicView: () -> Unit,
    onMusicFilterTabSelect: (String) -> Unit,
    onPlayMedia: (MediaItemModel, List<MediaItemModel>, Long?) -> Unit,
    onShuffleAllMusic: () -> Unit,
    onRequestPermission: () -> Unit,
    onPickMediaFiles: (Array<String>) -> Unit,
    onPickFolder: () -> Unit,
    onDeleteMedia: (MediaItemModel) -> Unit,
    onRenameMedia: (MediaItemModel) -> Unit,
    onMoveToVault: (MediaItemModel) -> Unit,
    onToggleSelectItem: (MediaItemModel) -> Unit,
    onStartSelection: (MediaItemModel) -> Unit,
    onRemoveFromContinueWatching: (MediaItemModel) -> Unit,
    onClearAllContinueWatching: () -> Unit,
    onPlayStream: (String, String, Boolean) -> Unit,
    onDeleteRecentStream: (String) -> Unit,
    onClearAllRecentStreams: () -> Unit,
    onOpenPrivacyVault: () -> Unit,
    onCheckAppUpdate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        when (selectedDestination) {
            0 -> {
                VideosTabScreen(
                    videos = filteredVideo,
                    isGridView = isVideoGridView,
                    openedFolder = openedVideoFolder,
                    onOpenFolder = onOpenVideoFolder,
                    sortOrder = videoSortOrder,
                    playbackPositionsFlow = playerManager.playbackPositions,
                    playbackTimestampsFlow = playerManager.playbackTimestamps,
                    playedMediaIdsFlow = playerManager.playedMediaIds,
                    isPlayerObscured = isPlayerObscured,
                    currentPlayingId = currentPlayingId,
                    hasPermission = hasPermission,
                    onToggleView = onToggleVideoView,
                    onPlayVideo = onPlayMedia,
                    onRequestPermission = onRequestPermission,
                    onPickFiles = { onPickMediaFiles(arrayOf("video/*", "audio/*")) },
                    onDeleteVideo = onDeleteMedia,
                    onRenameVideo = onRenameMedia,
                    onMoveToVault = onMoveToVault,
                    searchQuery = searchQuery,
                    selectedMediaItems = selectedMediaItems,
                    onToggleSelectItem = onToggleSelectItem,
                    onStartSelection = onStartSelection,
                    onRemoveFromContinueWatching = onRemoveFromContinueWatching,
                    onClearAllContinueWatching = onClearAllContinueWatching
                )
            }

            1 -> {
                MusicTabScreen(
                    audioList = filteredAudio,
                    isGridView = isMusicGridView,
                    openedFolder = openedMusicFolder,
                    onOpenFolder = onOpenMusicFolder,
                    sortOrder = musicSortOrder,
                    currentPlayingId = currentPlayingId,
                    hasPermission = hasPermission,
                    onToggleView = onToggleMusicView,
                    filterTab = musicFilterTab,
                    onFilterTabSelect = onMusicFilterTabSelect,
                    onPlayAudio = { audio, playlist -> onPlayMedia(audio, playlist, null) },
                    onShuffleAll = onShuffleAllMusic,
                    onRequestPermission = onRequestPermission,
                    onPickFiles = { onPickMediaFiles(arrayOf("audio/*", "video/*")) },
                    onDeleteAudio = onDeleteMedia,
                    onRenameAudio = onRenameMedia,
                    onMoveToVault = onMoveToVault,
                    searchQuery = searchQuery,
                    selectedMediaItems = selectedMediaItems,
                    onToggleSelectItem = onToggleSelectItem,
                    onStartSelection = onStartSelection
                )
            }

            else -> {
                val videoTotalBytes = remember(activeVideoList) {
                    activeVideoList.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                }
                val audioTotalBytes = remember(audioList) {
                    audioList.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                }
                MediaHubPage(
                    onPickFiles = { onPickMediaFiles(arrayOf("video/*", "audio/*")) },
                    onPickFolder = onPickFolder,
                    onPlayStream = onPlayStream,
                    recentStreams = recentStreamsState,
                    onDeleteRecentStream = onDeleteRecentStream,
                    onClearAllRecentStreams = onClearAllRecentStreams,
                    onOpenPrivacyVault = onOpenPrivacyVault,
                    onCheckAppUpdate = onCheckAppUpdate,
                    videoTotalBytes = videoTotalBytes,
                    audioTotalBytes = audioTotalBytes,
                    vaultCount = vaultItemCount
                )
            }
        }
    }
}

@Composable
private fun MainExpandedPlayerOverlay(
    visible: Boolean,
    isVideo: Boolean,
    inPip: Boolean,
    animatedScale: Float,
    animatedCornerRadius: androidx.compose.ui.unit.Dp,
    predictiveBackProgress: Float,
    pitchBlackEnabled: Boolean,
    playerManager: MediaPlayerManager,
    videoViewModel: PlayerViewModel,
    settingsManager: AppSettingsManager,
    onBackVideo: () -> Unit,
    onEnterPip: () -> Unit,
    onCollapseMusic: () -> Unit
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = visible,
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
            if (isVideo) {
                val videoColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicDarkColorScheme(context)
                } else {
                    darkColorScheme()
                }
                MaterialTheme(colorScheme = videoColorScheme) {
                    VideoPlayerView(
                        playerManager = playerManager,
                        videoViewModel = videoViewModel,
                        onBack = onBackVideo,
                        onEnterPip = onEnterPip,
                        isInPipMode = inPip,
                        settingsManager = settingsManager,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (!inPip) {
                MediaPlayerTheme(
                    darkTheme = true,
                    oledMode = pitchBlackEnabled
                ) {
                    MusicNowPlayingScreen(
                        playerManager = playerManager,
                        onCollapse = onCollapseMusic,
                        isPitchBlack = pitchBlackEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun MainModalDialogsHost(
    showVideoSettingsDialog: Boolean,
    settingsManager: AppSettingsManager,
    onDismissVideoSettings: () -> Unit,
    onSettingsChanged: () -> Unit,
    showAutoUpdateDialog: Boolean,
    appReleaseInfo: AppReleaseInfo?,
    onDismissAutoUpdate: () -> Unit,
    showVaultPinDialog: Boolean,
    vaultSecurityManager: VaultSecurityManager,
    onVaultUnlockSuccess: () -> Unit,
    onDismissVaultPin: () -> Unit,
    isMovingToVault: Boolean,
    itemsToDelete: List<MediaItemModel>,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    itemToRename: MediaItemModel?,
    onConfirmRename: (String) -> Unit,
    onDismissRename: () -> Unit
) {
    if (showVideoSettingsDialog) {
        VideoCustomizationDialog(
            settingsManager = settingsManager,
            onDismiss = onDismissVideoSettings,
            onSettingsChanged = onSettingsChanged
        )
    }

    if (showAutoUpdateDialog && appReleaseInfo != null) {
        AppUpdateDialog(
            releaseInfo = appReleaseInfo,
            onDismiss = onDismissAutoUpdate
        )
    }

    if (showVaultPinDialog) {
        VaultPinDialog(
            securityManager = vaultSecurityManager,
            onSuccess = onVaultUnlockSuccess,
            onDismiss = onDismissVaultPin
        )
    }

    if (isMovingToVault) {
        AlertDialog(
            onDismissRequest = { /* prevent dismissal while moving */ },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
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
                    color = MaterialTheme.colorScheme.onSurface
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (itemsToDelete.isNotEmpty()) {
        DeleteMediaConfirmDialog(
            items = itemsToDelete,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }

    itemToRename?.let { item ->
        RenameFileDialog(
            item = item,
            onDismiss = onDismissRename,
            onConfirm = onConfirmRename
        )
    }
}

@Composable
private fun MainSelectionTopBar(
    selectedMediaItems: Set<MediaItemModel>,
    currentPool: List<MediaItemModel>,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onLockVault: () -> Unit,
    onDelete: () -> Unit
) {
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onClearSelection),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel Selection",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${selectedMediaItems.size} Selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val totalSize = selectedMediaItems.sumOf { it.sizeBytes }
                Text(
                    text = formatFileSize(totalSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isAllSelected = currentPool.isNotEmpty() && selectedMediaItems.containsAll(currentPool)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable {
                        if (isAllSelected) onClearSelection() else onSelectAll()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAllSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                    contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (selectedMediaItems.size == 1) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onRename),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Rename File",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share Selected Files",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onLockVault),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Hide in Privacy Vault",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete Selected Files",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MainFloatingBottomDock(
    selectedDestination: Int,
    onSelectVideos: () -> Unit,
    onSelectMusic: () -> Unit,
    onSelectMediaHub: () -> Unit
) {
    val activeAccentColor = MaterialTheme.colorScheme.primary
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        activeAccentColor.copy(alpha = 0.40f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
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

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingDockItem(
                        selected = selectedDestination == 0,
                        onClick = onSelectVideos,
                        icon = Icons.Rounded.Movie,
                        label = "Videos",
                        activeColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    FloatingDockItem(
                        selected = selectedDestination == 1,
                        onClick = onSelectMusic,
                        icon = Icons.Rounded.MusicNote,
                        label = "Music",
                        activeColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    FloatingDockItem(
                        selected = selectedDestination == 2,
                        onClick = onSelectMediaHub,
                        icon = Icons.Rounded.FolderOpen,
                        label = "Media Hub",
                        activeColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

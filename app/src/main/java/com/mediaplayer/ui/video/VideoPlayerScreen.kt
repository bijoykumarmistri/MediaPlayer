package com.mediaplayer.ui.video

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import com.mediaplayer.ui.cleanMovieDisplayTitle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mediaplayer.audio.UsbAudioStatus
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.components.UsbTelemetryPill
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledSurfaceVariant
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerManager: MediaPlayerManager,
    telemetry: UsbAudioStatus,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenDacSettings: () -> Unit,
    isInPipMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val currentItem by playerManager.currentMediaItem.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val playbackState by playerManager.playbackState.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val decoderMode by playerManager.decoderMode.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }

    // MX Player Touch Lock (Child Lock)
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }

    // Auto-hide unlock prompt after 3.5s
    LaunchedEffect(showUnlockPrompt) {
        if (showUnlockPrompt) {
            delay(3500)
            showUnlockPrompt = false
        }
    }

    // Auto-Rotation support when video is playing (0 = Sensor Auto-Rotate, 1 = Landscape Lock, 2 = Portrait Lock)
    var orientationMode by remember { mutableIntStateOf(0) }

    DisposableEffect(orientationMode, isInPipMode) {
        if (!isInPipMode) {
            val target = when (orientationMode) {
                1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            activity?.requestedOrientation = target
        }

        onDispose {
            if (!isInPipMode) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }

    // Truly Immersive Fullscreen Mode: Hides status bar (network speed, battery logo, clock) and gesture navigation bar
    DisposableEffect(isInPipMode) {
        val window = activity?.window
        if (window != null && !isInPipMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        onDispose {
            val window = activity?.window
            if (window != null && !isInPipMode) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Automatically re-hide system bars when playback controls hide
    LaunchedEffect(areControlsVisible, isInPipMode) {
        if (!areControlsVisible && !isInPipMode) {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Aspect Ratio options: Fit, Fill, Zoom, Stretch (MX Player Aspect Ratio switcher)
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit to Screen",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop / Zoom",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "100% Original"
    )

    // Gesture feedback states: Brightness, Volume, Seek Scrubbing
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f
        )
    }
    var showBrightnessFeedback by remember { mutableStateOf(false) }

    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember {
        mutableFloatStateOf(
            (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        )
    }
    var showVolumeFeedback by remember { mutableStateOf(false) }

    // MX Player Horizontal Swipe Scrubbing
    var isHorizontalScrubbing by remember { mutableStateOf(false) }
    var scrubStartPos by remember { mutableLongStateOf(0L) }
    var scrubTargetPos by remember { mutableLongStateOf(0L) }
    var scrubDeltaMs by remember { mutableLongStateOf(0L) }

    // Double-tap visual feedback animations
    val leftRippleAlpha = remember { Animatable(0f) }
    val rightRippleAlpha = remember { Animatable(0f) }

    // Time display toggle: Total duration vs Remaining time (Classic MX Player feature)
    var showRemainingTime by remember { mutableStateOf(false) }

    // Dialogs: Speed, Audio Tracks, Subtitle Tracks, Video Info, Sleep Timer, More Menu
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showMediaInfoDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var currentSpeed by remember { mutableFloatStateOf(playerManager.player.playbackParameters.speed) }

    // Sleep Timer Job
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }
    var sleepTimerRemainingMinutes by remember { mutableIntStateOf(0) }

    // External Subtitle Picker
    val subtitleFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = when {
                uri.toString().endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                uri.toString().endsWith(".ass", ignoreCase = true) || uri.toString().endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                uri.toString().endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLanguage("External Subtitle")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            val curPos = playerManager.player.currentPosition
            val curItem = playerManager.player.currentMediaItem
            if (curItem != null) {
                val updatedItem = curItem.buildUpon()
                    .setSubtitleConfigurations(listOf(subConfig))
                    .build()
                playerManager.player.setMediaItem(updatedItem, curPos)
                playerManager.player.prepare()
                playerManager.player.play()
            }
        }
    }

    // Auto-hide controls after inactivity (only when playing and no dialog or menu is open)
    LaunchedEffect(
        areControlsVisible,
        isPlaying,
        showSpeedDialog,
        showAudioTrackDialog,
        showSubtitleDialog,
        showMediaInfoDialog,
        showSleepTimerDialog,
        showMoreMenu
    ) {
        if (areControlsVisible && isPlaying &&
            !showSpeedDialog && !showAudioTrackDialog && !showSubtitleDialog &&
            !showMediaInfoDialog && !showSleepTimerDialog && !showMoreMenu
        ) {
            delay(5000)
            areControlsVisible = false
        }
    }

    // Interactive Pinch-to-Zoom & Pan states (smooth zoom from 1.0x to 5.0x)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showZoomFeedback by remember { mutableStateOf(false) }
    var isPinchingActive by remember { mutableStateOf(false) }

    // Auto-hide Zoom Level HUD Pill after 1.5s
    LaunchedEffect(showZoomFeedback, zoomScale) {
        if (showZoomFeedback) {
            delay(1500)
            showZoomFeedback = false
        }
    }

    // Reset zoom when switching into Picture-in-Picture mode
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            zoomScale = 1.0f
            panOffset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Media3 Video Surface with Interactive Pinch-to-Zoom and Pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.player
                        useController = false
                        controllerAutoShow = false
                        resizeMode = resizeModes[resizeModeIndex].first
                        isClickable = false
                        isFocusable = false
                        setOnTouchListener { _, _ -> false }
                    }
                },
                update = { view ->
                    view.player = playerManager.player
                    view.resizeMode = if (isInPipMode) AspectRatioFrameLayout.RESIZE_MODE_FIT else resizeModes[resizeModeIndex].first
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isInPipMode) {
            // Under-the-hood MX Player Gesture Engine (Pinch-to-Zoom, Pan, Taps, Double-Taps, Vertical Brightness/Volume, Horizontal Scrubbing)
            Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked, durationMs) {
                    coroutineScope {
                        // 1. Pinch-to-Zoom & Pan Multi-Touch Engine
                        launch {
                            if (!isScreenLocked) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var lastDistance = 0f
                                    var lastCentroid = Offset.Zero
                                    var isPinching = false

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val activePointers = event.changes.filter { it.pressed }

                                        if (activePointers.size >= 2) {
                                            isPinchingActive = true
                                            val p1 = activePointers[0]
                                            val p2 = activePointers[1]
                                            val currentDistance = (p1.position - p2.position).getDistance()
                                            val currentCentroid = (p1.position + p2.position) / 2f

                                            if (!isPinching) {
                                                isPinching = true
                                                lastDistance = currentDistance
                                                lastCentroid = currentCentroid
                                            } else if (lastDistance > 0f) {
                                                val zoomFactor = currentDistance / lastDistance
                                                val panDelta = currentCentroid - lastCentroid

                                                val newScale = (zoomScale * zoomFactor).coerceIn(1.0f, 5.0f)
                                                zoomScale = newScale

                                                val maxPanX = (size.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                                val maxPanY = (size.height * (newScale - 1f) / 2f).coerceAtLeast(0f)

                                                panOffset = if (newScale <= 1.01f) {
                                                    Offset.Zero
                                                } else {
                                                    Offset(
                                                        x = (panOffset.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                                                        y = (panOffset.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                                    )
                                                }

                                                lastDistance = currentDistance
                                                lastCentroid = currentCentroid
                                                showZoomFeedback = true
                                            }

                                            // Consume multi-touch pointers so single-finger drags/taps don't trigger
                                            p1.consume()
                                            p2.consume()
                                        } else {
                                            if (isPinching) {
                                                isPinching = false
                                                lastDistance = 0f
                                                isPinchingActive = false
                                            }
                                            if (activePointers.isEmpty()) {
                                                isPinchingActive = false
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Taps Engine (Tap to toggle controls, Double-tap to reset zoom or seek/pause)
                        launch {
                            detectTapGestures(
                                onTap = {
                                    if (isScreenLocked) {
                                        Toast.makeText(context, "Tap the lock icon in the top-left to unlock", Toast.LENGTH_SHORT).show()
                                    } else {
                                        areControlsVisible = !areControlsVisible
                                    }
                                },
                                onDoubleTap = { offset ->
                                    if (!isScreenLocked) {
                                        if (zoomScale > 1.05f) {
                                            // Reset zoom back to 100% on double-tap
                                            scope.launch {
                                                val startScale = zoomScale
                                                val startPan = panOffset
                                                animate(
                                                    initialValue = 0f,
                                                    targetValue = 1f,
                                                    animationSpec = tween(220)
                                                ) { fraction, _ ->
                                                    zoomScale = startScale + (1.0f - startScale) * fraction
                                                    panOffset = Offset(
                                                        startPan.x * (1f - fraction),
                                                        startPan.y * (1f - fraction)
                                                    )
                                                }
                                                zoomScale = 1.0f
                                                panOffset = Offset.Zero
                                            }
                                            showZoomFeedback = true
                                        } else {
                                            val width = size.width
                                            when {
                                                offset.x < width * 0.35f -> {
                                                    playerManager.seekBackward(10000L)
                                                    scope.launch {
                                                        leftRippleAlpha.snapTo(1f)
                                                        leftRippleAlpha.animateTo(0f, tween(500))
                                                    }
                                                }
                                                offset.x > width * 0.65f -> {
                                                    playerManager.seekForward(10000L)
                                                    scope.launch {
                                                        rightRippleAlpha.snapTo(1f)
                                                        rightRippleAlpha.animateTo(0f, tween(500))
                                                    }
                                                }
                                                else -> {
                                                    playerManager.playPause()
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Single-Finger Drag Engine (Volume, Brightness, Scrubbing)
                        launch {
                            if (!isScreenLocked) {
                                var startX = 0f
                                var startY = 0f
                                var dragDirection = 0 // 0 = uncommitted, 1 = horizontal, 2 = vertical
                                var initialPos = 0L

                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (isPinchingActive) return@detectDragGestures
                                        startX = offset.x
                                        startY = offset.y
                                        dragDirection = 0
                                        initialPos = playerManager.player.currentPosition
                                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        volumeLevel = (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
                                    },
                                    onDragEnd = {
                                        if (dragDirection == 1 && isHorizontalScrubbing) {
                                            playerManager.seekTo(scrubTargetPos)
                                        }
                                        isHorizontalScrubbing = false
                                        showBrightnessFeedback = false
                                        showVolumeFeedback = false
                                        dragDirection = 0
                                    },
                                    onDragCancel = {
                                        isHorizontalScrubbing = false
                                        showBrightnessFeedback = false
                                        showVolumeFeedback = false
                                        dragDirection = 0
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (isPinchingActive) {
                                            isHorizontalScrubbing = false
                                            showBrightnessFeedback = false
                                            showVolumeFeedback = false
                                            dragDirection = 0
                                            return@detectDragGestures
                                        }
                                        change.consume()
                                        val totalDx = change.position.x - startX
                                        val totalDy = change.position.y - startY

                                        // Commit to direction once slop is crossed
                                        if (dragDirection == 0) {
                                            val isLiveStream = playerManager.player.isCurrentMediaItemLive ||
                                                (currentItem?.uri?.scheme?.lowercase() in listOf("http", "https", "rtsp") && durationMs <= 0L)
                                            if (abs(totalDx) > 30f && abs(totalDx) > abs(totalDy) * 1.3f && !isLiveStream && durationMs > 0L) {
                                                dragDirection = 1 // Horizontal Seek Scrubbing
                                                isHorizontalScrubbing = true
                                                scrubStartPos = initialPos
                                            } else if (abs(totalDy) > 30f) {
                                                dragDirection = 2 // Vertical Brightness / Volume
                                            }
                                        }

                                        if (dragDirection == 1) {
                                            // MX Player Horizontal Scrubbing
                                            val scrubFactor = if (durationMs > 3600000L) 180000L else 90000L
                                            val deltaFraction = (change.position.x - startX) / size.width.toFloat()
                                            val deltaMs = (deltaFraction * scrubFactor).toLong()
                                            scrubDeltaMs = deltaMs
                                            scrubTargetPos = (scrubStartPos + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
                                        } else if (dragDirection == 2) {
                                            val isLeftSide = startX < size.width / 2f
                                            if (isLeftSide && activity != null) {
                                                // Left half: Screen Brightness
                                                showBrightnessFeedback = true
                                                val delta = -dragAmount.y / size.height.toFloat()
                                                brightnessLevel = (brightnessLevel + delta).coerceIn(0.01f, 1.0f)
                                                val lp = activity.window.attributes
                                                lp.screenBrightness = brightnessLevel
                                                activity.window.attributes = lp
                                            } else {
                                                // Right half: Media Volume (proportional to screen height, matching brightness rate)
                                                showVolumeFeedback = true
                                                if (!telemetry.isMixerBypassActive) {
                                                    val delta = -dragAmount.y / size.height.toFloat()
                                                    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                                    val targetVolume = (volumeLevel * maxVolume).roundToInt().coerceIn(0, maxVolume)
                                                    if (targetVolume != currentVolume) {
                                                        currentVolume = targetVolume
                                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
        )

        // Double-Tap Left Seek Visual Ripple (Pure Draw phase alpha, 0 recompositions)
        Box(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .align(Alignment.CenterStart)
                .graphicsLayer { alpha = leftRippleAlpha.value }
                .background(Color(0x3300E5FF)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Replay10,
                    contentDescription = "-10s",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Double-Tap Right Seek Visual Ripple (Pure Draw phase alpha, 0 recompositions)
        Box(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .align(Alignment.CenterEnd)
                .graphicsLayer { alpha = rightRippleAlpha.value }
                .background(Color(0x3300E5FF)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Forward10,
                    contentDescription = "+10s",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // MX Player Gesture OSD: Horizontal Seek Scrubbing Card
        if (isHorizontalScrubbing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC12131A)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AudiophileCyan),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (scrubDeltaMs >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = if (scrubDeltaMs >= 0) AudiophileCyan else AudiophileGold,
                            modifier = Modifier.size(36.dp)
                        )
                        val sign = if (scrubDeltaMs >= 0) "+" else "-"
                        val formattedDelta = formatTimeMs(abs(scrubDeltaMs))
                        Text(
                            text = "[ $sign$formattedDelta ]",
                            color = if (scrubDeltaMs >= 0) AudiophileCyan else AudiophileGold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatTimeMs(scrubTargetPos)} / ${formatTimeMs(durationMs)}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Gesture Feedback HUD Card: Brightness / Volume / Bit-Perfect Output
        AnimatedVisibility(
            visible = showBrightnessFeedback || showVolumeFeedback,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC12131A)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showBrightnessFeedback) {
                        Icon(
                            imageVector = Icons.Rounded.BrightnessHigh,
                            contentDescription = null,
                            tint = AudiophileCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Brightness ${(brightnessLevel * 100).roundToInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    } else if (showVolumeFeedback) {
                        if (telemetry.isMixerBypassActive) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = AudiophileGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bit-Perfect Direct Output",
                                color = AudiophileGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Android software volume DSP bypassed.\nUse your DAC hardware volume dial.",
                                color = Color(0xFFC0C0D0),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            val pct = (volumeLevel * 100).roundToInt()
                            val volumeIcon = when {
                                pct == 0 -> Icons.AutoMirrored.Rounded.VolumeOff
                                pct < 50 -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            }
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = null,
                                tint = AudiophileCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Volume $pct%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Gesture Feedback HUD Card: Zoom In / Zoom Out Level
        AnimatedVisibility(
            visible = showZoomFeedback,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD12131A)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AudiophileCyan)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = null,
                        tint = AudiophileCyan,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zoom: ${(zoomScale * 100).roundToInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (zoomScale > 1.05f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Double-tap to reset to 100%",
                            color = Color(0xFFA0A8B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // MX Player Touch Lock Floating Unlock Button
        if (isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    onClick = {
                        isScreenLocked = false
                        showUnlockPrompt = false
                        areControlsVisible = true
                        Toast.makeText(context, "Screen Touches Unlocked", Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    color = Color(0xEE1A1C24),
                    border = androidx.compose.foundation.BorderStroke(2.dp, AudiophileCyan),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Unlock Screen",
                            tint = AudiophileCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Buffering Indicator
        if (playbackState == Player.STATE_BUFFERING) {
            CircularProgressIndicator(
                color = AudiophileGold,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
            )
        }

        // Playback Error Overlay
        val playbackError by playerManager.playbackError.collectAsState()
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD906080F)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
                    border = BorderStroke(1.dp, Color(0x33FF3B30)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0x22FF3B30), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Playback Failed",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playbackError ?: "Unable to connect to stream server.",
                            color = Color(0xFFA0AAB8),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Go Back", color = Color.White, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { playerManager.retry() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AudiophileGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Translucent HUD Overlays (Top and Bottom)
        AnimatedVisibility(
            visible = areControlsVisible && !isScreenLocked,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xD9000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xE6000000)
                            )
                        )
                    )
            ) {
                // Safe Insets calculation for Display Cutout (Camera Punch-Hole) and Status Bars
                val configuration = LocalConfiguration.current
                val layoutDirection = LocalLayoutDirection.current
                val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()

                val safeTopPadding = maxOf(
                    cutoutPadding.calculateTopPadding(),
                    if (isPortrait) 48.dp else 16.dp
                )
                val safeLeftPadding = maxOf(cutoutPadding.calculateLeftPadding(layoutDirection), 12.dp)
                val safeRightPadding = maxOf(cutoutPadding.calculateRightPadding(layoutDirection), 12.dp)

                // Top Bar HUD (Back, Title, HW/SW Decoder, Controls)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = safeLeftPadding,
                            end = safeRightPadding,
                            top = safeTopPadding + 4.dp,
                            bottom = 6.dp
                        )
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button (Luxury frosted glass circle)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    val (cleanTitle, releaseYear) = remember(currentItem?.title) {
                        cleanMovieDisplayTitle(currentItem?.title.orEmpty().ifEmpty { "Video Player" })
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = cleanTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentItem?.resolution.orEmpty().ifEmpty { "1080p FHD" },
                                color = AudiophileCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2200E5FF))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                            if (releaseYear != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($releaseYear)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AudiophileGold
                                )
                            }
                        }
                    }

                    // MX Player Signature HW / SW Decoder Switcher Pill
                    val isFfmpeg = decoderMode.contains("FFmpeg", ignoreCase = true) || decoderMode.contains("SW", ignoreCase = true)
                    Surface(
                        onClick = {
                            val nextMode = if (isFfmpeg) "Android MediaCodec (Hardware)" else "FFmpeg Native (Universal SW)"
                            playerManager.setDecoderMode(nextMode)
                            val label = if (isFfmpeg) "HW (Hardware MediaCodec)" else "SW (FFmpeg Native Extension)"
                            Toast.makeText(context, "Decoder: $label", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFfmpeg) AudiophileGold else AudiophileCyan),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (isFfmpeg) "SW" else "HW",
                            color = if (isFfmpeg) AudiophileGold else AudiophileCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // In landscape mode, show direct action buttons on the spacious top bar
                    if (!isPortrait) {
                        // Audio Track Selector
                        IconButton(onClick = { showAudioTrackDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = "Audio Tracks",
                                tint = Color.White
                            )
                        }

                        // Subtitle Selector
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = "Subtitles",
                                tint = Color.White
                            )
                        }

                        // Screen Orientation Toggle
                        IconButton(
                            onClick = {
                                orientationMode = (orientationMode + 1) % 3
                                val message = when (orientationMode) {
                                    1 -> "Landscape Orientation Locked"
                                    2 -> "Portrait Orientation Locked"
                                    else -> "Auto-Rotation (Sensor) Active"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = when (orientationMode) {
                                    1 -> Icons.Rounded.StayCurrentLandscape
                                    2 -> Icons.Rounded.StayCurrentPortrait
                                    else -> Icons.Rounded.ScreenRotation
                                },
                                contentDescription = "Orientation Mode",
                                tint = if (orientationMode == 0) AudiophileCyan else AudiophileGold
                            )
                        }

                        // Aspect Ratio Button
                        IconButton(
                            onClick = {
                                resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                Toast.makeText(context, "Aspect: ${resizeModes[resizeModeIndex].second}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White
                            )
                        }

                        // Quick Reset Zoom Pill Button (Visible when video is zoomed in)
                        if (zoomScale > 1.05f) {
                            Surface(
                                onClick = {
                                    scope.launch {
                                        val startScale = zoomScale
                                        val startPan = panOffset
                                        animate(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = tween(200)
                                        ) { fraction, _ ->
                                            zoomScale = startScale + (1.0f - startScale) * fraction
                                            panOffset = Offset(
                                                startPan.x * (1f - fraction),
                                                startPan.y * (1f - fraction)
                                            )
                                        }
                                        zoomScale = 1.0f
                                        panOffset = Offset.Zero
                                    }
                                    showZoomFeedback = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x3300E5FF),
                                border = BorderStroke(1.dp, AudiophileCyan.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomOutMap,
                                        contentDescription = "Reset Zoom",
                                        tint = AudiophileCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${(zoomScale * 100).roundToInt()}%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Picture-in-Picture Button
                    IconButton(onClick = onEnterPip) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPicture,
                            contentDescription = "Picture-in-Picture",
                            tint = Color.White
                        )
                    }

                    // More Options 3-Dots Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(OledSurfaceVariant)
                        ) {
                            if (isPortrait) {
                                DropdownMenuItem(
                                    text = { Text("Audio Tracks", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = AudiophileCyan) },
                                    onClick = {
                                        showMoreMenu = false
                                        showAudioTrackDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Subtitles", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = AudiophileGold) },
                                    onClick = {
                                        showMoreMenu = false
                                        showSubtitleDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aspect: ${resizeModes[resizeModeIndex].second}", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                        Toast.makeText(context, "Aspect: ${resizeModes[resizeModeIndex].second}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val label = when (orientationMode) {
                                            1 -> "Lock: Landscape"
                                            2 -> "Lock: Portrait"
                                            else -> "Lock: Auto Sensor"
                                        }
                                        Text(label, color = Color.White)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = AudiophileCyan) },
                                    onClick = {
                                        orientationMode = (orientationMode + 1) % 3
                                        val message = when (orientationMode) {
                                            1 -> "Landscape Orientation Locked"
                                            2 -> "Portrait Orientation Locked"
                                            else -> "Auto-Rotation (Sensor) Active"
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Playback Speed (${currentSpeed}x)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = AudiophileCyan) },
                                onClick = {
                                    showMoreMenu = false
                                    showSpeedDialog = true
                                }
                            )
                            if (zoomScale > 1.05f) {
                                DropdownMenuItem(
                                    text = { Text("Reset Zoom (${(zoomScale * 100).roundToInt()}%)", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Rounded.ZoomOutMap, contentDescription = null, tint = AudiophileCyan) },
                                    onClick = {
                                        showMoreMenu = false
                                        scope.launch {
                                            val startScale = zoomScale
                                            val startPan = panOffset
                                            animate(0f, 1f, animationSpec = tween(200)) { fraction, _ ->
                                                zoomScale = startScale + (1.0f - startScale) * fraction
                                                panOffset = Offset(startPan.x * (1f - fraction), startPan.y * (1f - fraction))
                                            }
                                            zoomScale = 1.0f
                                            panOffset = Offset.Zero
                                        }
                                        showZoomFeedback = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    val timerLabel = if (sleepTimerRemainingMinutes > 0) "Sleep Timer (${sleepTimerRemainingMinutes}m)" else "Sleep Timer"
                                    Text(timerLabel, color = Color.White)
                                },
                                leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = AudiophileGold) },
                                onClick = {
                                    showMoreMenu = false
                                    showSleepTimerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Video & Audio Details", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = AudiophileGreen) },
                                onClick = {
                                    showMoreMenu = false
                                    showMediaInfoDialog = true
                                }
                            )
                        }
                    }
                }

                // Center Transport Row: Seek -10s, Play/Pause FAB, Seek +10s
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    IconButton(
                        onClick = { playerManager.seekBackward(10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Seek -10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause FAB
                    IconButton(
                        onClick = { playerManager.playPause() },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerManager.seekForward(10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Seek +10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Bar HUD (Time, Scrubber, Transport Row with Lock, Skip, Speed, Loop, Bit-Perfect)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val positionMs by playerManager.currentPositionMs.collectAsState()
                    val isLiveStream = playerManager.player.isCurrentMediaItemLive ||
                        (currentItem?.uri?.scheme?.lowercase() in listOf("http", "https", "rtsp") && durationMs <= 0L)

                    if (isLiveStream) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFFF3B30), CircleShape)
                                )
                                Text(
                                    text = "LIVE STREAM",
                                    color = Color(0xFFFF3B30),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "REAL-TIME BROADCAST",
                                color = Color(0xFFA0AAB8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Time and Duration (Click right time to toggle Remaining Time vs Total Time)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimeMs(positionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
                            Text(
                                text = if (showRemainingTime) "-${formatTimeMs(remainingMs)}" else formatTimeMs(durationMs),
                                color = if (showRemainingTime) AudiophileCyan else Color(0xFFA0A0B0),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        showRemainingTime = !showRemainingTime
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        // Progress Slider
                        Slider(
                            value = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { frac ->
                                playerManager.seekTo((frac * durationMs).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                                activeTrackColor = if (telemetry.isMixerBypassActive) AudiophileGold else AudiophileCyan,
                                inactiveTrackColor = Color(0x55FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // MX Player Transport & Utility Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Touch Lock Button (Locks screen touches)
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                areControlsVisible = false
                                Toast.makeText(context, "Screen Touches Locked. Tap to unlock.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = "Lock Screen",
                                tint = Color.White
                            )
                        }

                        // 2. Previous Video
                        IconButton(onClick = { playerManager.skipPrevious() }) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous Video",
                                tint = Color.White
                            )
                        }

                        // 3. Playback Speed Selector (Icon-only)
                        IconButton(onClick = { showSpeedDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "Playback Speed",
                                tint = if (currentSpeed != 1.0f) AudiophileCyan else Color.White
                            )
                        }

                        // 4. Repeat / Loop Mode
                        IconButton(onClick = { playerManager.toggleRepeat() }) {
                            Icon(
                                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = "Repeat Mode",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) AudiophileCyan else Color.White
                            )
                        }

                        // 5. Next Video
                        IconButton(onClick = { playerManager.skipNext() }) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next Video",
                                tint = Color.White
                            )
                        }

                        // 6. Quick Bit-Perfect USB Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bit-Perfect",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isMixerBypassActive) AudiophileGold else Color(0xFF9E9EAE)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = telemetry.isExclusiveBitPerfectEnabled,
                                onCheckedChange = { playerManager.usbAudioController.setExclusiveBitPerfect(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AudiophileGold,
                                    checkedTrackColor = Color(0x66FFD54F),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0x33888888)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Speed Selector Dialog
        if (showSpeedDialog) {
            val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f)
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = OledSurfaceVariant,
                title = { Text("Playback Speed", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        speedOptions.forEach { sp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentSpeed = sp
                                        playerManager.player.setPlaybackSpeed(sp)
                                        Toast.makeText(context, "Playback Speed: ${if (sp == 1.0f) "1.0x (Normal)" else "${sp}x"}", Toast.LENGTH_SHORT).show()
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentSpeed == sp,
                                    onClick = {
                                        currentSpeed = sp
                                        playerManager.player.setPlaybackSpeed(sp)
                                        Toast.makeText(context, "Playback Speed: ${if (sp == 1.0f) "1.0x (Normal)" else "${sp}x"}", Toast.LENGTH_SHORT).show()
                                        showSpeedDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AudiophileCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (sp == 1.0f) "1.0x (Normal)" else "${sp}x",
                                    color = if (currentSpeed == sp) AudiophileCyan else Color.White,
                                    fontWeight = if (currentSpeed == sp) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        // Audio Track Selector Dialog (Multi-Audio MKV / MP4)
        if (showAudioTrackDialog) {
            val tracks = playerManager.player.currentTracks
            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }

            AlertDialog(
                onDismissRequest = { showAudioTrackDialog = false },
                containerColor = OledSurfaceVariant,
                title = { Text("Audio Track Selector", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (audioGroups.isEmpty()) {
                            Text("No alternate audio tracks available.", color = Color(0xFFA0A0B0))
                        } else {
                            audioGroups.forEachIndexed { groupIndex, group ->
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    val isSelected = group.isTrackSelected(i)
                                    val trackName = format.label ?: format.language ?: "Audio Track ${groupIndex + 1}"
                                    val channels = if (format.channelCount == 6) "5.1 Surround" else "${format.channelCount} Channels"
                                    val sampleRateStr = if (format.sampleRate > 0) "${format.sampleRate / 1000} kHz" else "48 kHz"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                playerManager.player.trackSelectionParameters = playerManager.player.trackSelectionParameters
                                                    .buildUpon()
                                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                                    .build()
                                                showAudioTrackDialog = false
                                                Toast.makeText(context, "Selected: $trackName ($channels)", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                playerManager.player.trackSelectionParameters = playerManager.player.trackSelectionParameters
                                                    .buildUpon()
                                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                                    .build()
                                                showAudioTrackDialog = false
                                                Toast.makeText(context, "Selected: $trackName ($channels)", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = AudiophileGold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "$trackName • $channels • $sampleRateStr",
                                                color = if (isSelected) AudiophileGold else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            format.sampleMimeType?.let { mime ->
                                                Text(
                                                    text = mime.substringAfterLast('/'),
                                                    color = Color(0xFF8E8E9E),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioTrackDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        // Subtitle Selector Dialog (Internal tracks + External subtitle loader)
        if (showSubtitleDialog) {
            val tracks = playerManager.player.currentTracks
            val subGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = OledSurfaceVariant,
                title = { Text("Subtitle Selector", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        // Option: Subtitles Off
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.player.trackSelectionParameters = playerManager.player.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                        .build()
                                    showSubtitleDialog = false
                                    Toast.makeText(context, "Subtitles Disabled", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Disable Subtitles (Off)",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        // Internal tracks
                        subGroups.forEachIndexed { groupIndex, group ->
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val isSelected = group.isTrackSelected(i)
                                val subName = format.label ?: format.language ?: "Subtitle Track ${groupIndex + 1}"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            playerManager.player.trackSelectionParameters = playerManager.player.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                                .build()
                                            showSubtitleDialog = false
                                            Toast.makeText(context, "Subtitles: $subName", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            playerManager.player.trackSelectionParameters = playerManager.player.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                                .build()
                                            showSubtitleDialog = false
                                            Toast.makeText(context, "Subtitles: $subName", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = AudiophileCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$subName (${format.sampleMimeType?.substringAfterLast('/') ?: "SRT/ASS"})",
                                        color = if (isSelected) AudiophileCyan else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        // Load external subtitle button (.srt, .ass, .vtt)
                        TextButton(
                            onClick = {
                                subtitleFileLauncher.launch(arrayOf("*/*"))
                                showSubtitleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = AudiophileCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open External Subtitle (.srt / .ass / .vtt)", color = AudiophileCyan)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            val timerOptions = listOf(0 to "Off (Cancel Timer)", 15 to "15 Minutes", 30 to "30 Minutes", 45 to "45 Minutes", 60 to "60 Minutes", 90 to "90 Minutes")
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                containerColor = OledSurfaceVariant,
                title = { Text("Sleep Timer", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column {
                        timerOptions.forEach { (mins, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        sleepTimerJob?.cancel()
                                        if (mins == 0) {
                                            sleepTimerRemainingMinutes = 0
                                            Toast.makeText(context, "Sleep Timer Cancelled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            sleepTimerRemainingMinutes = mins
                                            sleepTimerJob = scope.launch {
                                                var remaining = mins
                                                while (remaining > 0) {
                                                    delay(60000L)
                                                    remaining--
                                                    sleepTimerRemainingMinutes = remaining
                                                }
                                                playerManager.player.pause()
                                                Toast.makeText(context, "Sleep timer finished. Playback paused.", Toast.LENGTH_LONG).show()
                                            }
                                            Toast.makeText(context, "Sleep Timer set for $mins minutes", Toast.LENGTH_SHORT).show()
                                        }
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sleepTimerRemainingMinutes == mins,
                                    onClick = {
                                        sleepTimerJob?.cancel()
                                        if (mins == 0) {
                                            sleepTimerRemainingMinutes = 0
                                            Toast.makeText(context, "Sleep Timer Cancelled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            sleepTimerRemainingMinutes = mins
                                            sleepTimerJob = scope.launch {
                                                var remaining = mins
                                                while (remaining > 0) {
                                                    delay(60000L)
                                                    remaining--
                                                    sleepTimerRemainingMinutes = remaining
                                                }
                                                playerManager.player.pause()
                                                Toast.makeText(context, "Sleep timer finished. Playback paused.", Toast.LENGTH_LONG).show()
                                            }
                                            Toast.makeText(context, "Sleep Timer set for $mins minutes", Toast.LENGTH_SHORT).show()
                                        }
                                        showSleepTimerDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AudiophileGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    color = if (sleepTimerRemainingMinutes == mins) AudiophileGold else Color.White,
                                    fontWeight = if (sleepTimerRemainingMinutes == mins) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        // Video & Audio Details Dialog (MX Player Media Information)
        if (showMediaInfoDialog) {
            AlertDialog(
                onDismissRequest = { showMediaInfoDialog = false },
                containerColor = OledSurfaceVariant,
                title = { Text("Media Information", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        InfoRow("Title", currentItem?.title ?: "Unknown")
                        InfoRow("Folder", currentItem?.folderName.orEmpty().ifEmpty { "Videos" })
                        InfoRow("Resolution", currentItem?.resolution.orEmpty().ifEmpty { "1080p FHD" })
                        InfoRow("Duration", formatTimeMs(durationMs))
                        InfoRow("Decoder", decoderMode)
                        InfoRow("Audio Output", if (telemetry.isMixerBypassActive) "Bit-Perfect USB Direct (${telemetry.liveFormatTelemetry})" else "System Audio Engine (48 kHz)")
                        InfoRow("File Size", if ((currentItem?.sizeBytes ?: 0) > 0) "${(currentItem?.sizeBytes ?: 0) / 1024 / 1024} MB" else "Local Storage")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMediaInfoDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = Color(0xFFA0A0B0), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

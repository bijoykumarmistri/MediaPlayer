package com.mxplayer.mpv

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.res.Configuration
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt



/**
 * Screen orientation mode for video playback:
 * - [SENSOR]: Sensor auto-rotation
 * - [LANDSCAPE]: Force landscape orientation
 * - [PORTRAIT]: Force portrait orientation
 */
enum class OrientationMode(val label: String) {
    SENSOR("Auto Rotate"),
    LANDSCAPE("Landscape Lock"),
    PORTRAIT("Portrait Lock");

    fun next(): OrientationMode = when (this) {
        SENSOR -> LANDSCAPE
        LANDSCAPE -> PORTRAIT
        PORTRAIT -> SENSOR
    }
}

/**
 * Categories within the video player options sheets.
 */
enum class StudioDrawerTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AUDIO("Audio", Icons.Rounded.Audiotrack),
    SUBTITLES("Subtitles", Icons.Rounded.Subtitles),
    VIDEO("Decoder & Display", Icons.Rounded.Memory),
    PLAYBACK("Player Settings", Icons.Rounded.Tune),
    SPECS("Media Details", Icons.Rounded.Info)
}

/**
 * Production-ready C++ core (libmpv HW+) video player screen:
 * - Multi-audio track switching (Dolby Digital, 5.1 Surround, Stereo, AAC, EAC3, TrueHD)
 * - Subtitle track switching (internal tracks + external .srt/.ass/.vtt file loader)
 * - Audio & Subtitle delay synchronization (-5.0s .. +5.0s)
 * - Screen auto-rotation & orientation locking (Sensor, Landscape, Portrait)
 * - Touch Lock (Child Lock) preventing accidental touches/gestures
 * - Video scaling presets (Fit to Screen, Stretch, Crop/Zoom, 100%, 16:9, 4:3)
 * - Interactive multi-touch pinch-to-zoom (1.0x .. 5.0x) & pan
 * - Pitch-corrected playback speed control (0.25x .. 2.0x)
 * - Time display toggle (Current / Total vs Current / Remaining)
 * - Repeat mode toggle (Repeat Off vs Repeat One)
 * - Comprehensive technical Media Information dialog
 * - Sleep Timer (15m .. 60m countdown)
 * - Zero-buffering 60fps keyframe scrubbing with live HUD overlay
 * - Vertical brightness (left 35%) & volume + SW boost (right 35%) gestures
 * - Double-tap ±10s seeking with animated ripple feedback
 * - Truly immersive edge-to-edge fullscreen with display cutout mode
 */
@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    initialUri: Uri? = null,
    initialTitle: String? = null,
    initialPositionMs: Long = 0L,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onPositionUpdate: ((posMs: Long, durMs: Long) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    isInPipMode: Boolean = false,
    viewModel: PlayerViewModel = viewModel(),
    doubleTapSeekSeconds: Int = 10,
    swipeGesturesEnabled: Boolean = true,
    fastScrubEnabled: Boolean = true,
    hold2xSpeedEnabled: Boolean = true,
    defaultDecoderMode: String = "HW+",
    defaultResizeMode: String = "FIT",
    subtitleSize: Int = 48,
    subtitleColorHex: String = "#FFFFFF"
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Apply user customization settings on startup
    LaunchedEffect(defaultDecoderMode) {
        val mode = when (defaultDecoderMode) {
            "SW" -> DecoderMode.SW
            "HW" -> DecoderMode.HW
            else -> DecoderMode.HW_PLUS
        }
        viewModel.selectDecoderMode(mode)
    }

    LaunchedEffect(defaultResizeMode) {
        val mode = when (defaultResizeMode) {
            "CROP" -> ResizeMode.CROP
            "16:9" -> ResizeMode.SIXTEEN_NINE
            "4:3" -> ResizeMode.FOUR_THREE
            "STRETCH" -> ResizeMode.STRETCH
            else -> ResizeMode.FIT
        }
        viewModel.setResizeMode(mode)
    }

    LaunchedEffect(subtitleSize) {
        viewModel.setSubtitleFontSize(subtitleSize)
    }

    LaunchedEffect(subtitleColorHex) {
        viewModel.setSubtitleColor(subtitleColorHex)
    }

    // Unified Cinema Studio Drawer state
    var showStudioDrawer by remember { mutableStateOf(false) }
    var activeStudioTab by remember { mutableStateOf(StudioDrawerTab.AUDIO) }

    val onQuickCycleSpeed: () -> Unit = {
        val current = uiState.playbackSpeed
        val next = when {
            abs(current - 1.0) < 0.05 -> 1.25
            abs(current - 1.25) < 0.05 -> 1.5
            abs(current - 1.5) < 0.05 -> 2.0
            abs(current - 2.0) < 0.05 -> 0.75
            else -> 1.0
        }
        viewModel.setPlaybackSpeed(next)
        Toast.makeText(context, "Playback Speed: ${next}x", Toast.LENGTH_SHORT).show()
    }

    // Screen Touch Lock
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }

    // Screen Orientation Mode
    var orientationMode by remember { mutableStateOf(OrientationMode.SENSOR) }

    // Intercept back gestures and hardware back buttons
    BackHandler(enabled = onBack != null && !isInPipMode) {
        if (showStudioDrawer) {
            showStudioDrawer = false
        } else if (isScreenLocked) {
            showUnlockPrompt = true
        } else {
            onBack?.invoke()
        }
    }

    // Time Display Toggle: Elapsed/Total vs Elapsed/Remaining
    var showRemainingTime by remember { mutableStateOf(false) }

    // Aspect Ratio HUD Toast overlay
    var showAspectHud by remember { mutableStateOf(false) }
    var aspectHudText by remember { mutableStateOf("") }

    // Sleep Timer Job & Remaining time
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }
    var sleepTimerRemainingMinutes by remember { mutableIntStateOf(0) }

    // Interactive Pinch-to-Zoom & Pan states (1.0x .. 5.0x)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showZoomHud by remember { mutableStateOf(false) }

    // Double-tap visual feedback animations
    val leftRippleAlpha = remember { Animatable(0f) }
    val rightRippleAlpha = remember { Animatable(0f) }

    // SAF External Video File Picker
    val openVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openDocument(context, uri)
        }
    }

    // SAF External Subtitle File Picker (.srt, .ass, .ssa, .vtt)
    val openSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val success = viewModel.addExternalSubtitle(context, uri)
            Toast.makeText(
                context,
                if (success) "External subtitle loaded" else "Failed to load subtitle file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Connect position update callback to persistent history
    LaunchedEffect(onPositionUpdate) {
        viewModel.onPositionUpdate = onPositionUpdate
    }

    // Auto-advance to next video on EOF when playlist has next item
    LaunchedEffect(hasNext, onNext) {
        viewModel.onEndOfFileReached = {
            if (hasNext) {
                onNext?.invoke()
            }
        }
    }

    // Handle initialUri playback on startup with instant resume support
    LaunchedEffect(initialUri, initialTitle) {
        if (initialUri != null) {
            viewModel.openDocument(context, initialUri, initialTitle, initialPositionMs)
        }
    }

    // Truly Immersive Fullscreen Mode (hide system bars & notch cutout layout)
    DisposableEffect(isInPipMode) {
        val window = activity?.window
        if (window != null && !isInPipMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

    val lifecycleOwner = LocalLifecycleOwner.current

    // Pause video playback when app is paused or stopped (backgrounded) unless in PiP mode
    DisposableEffect(lifecycleOwner, isInPipMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                val isPip = isInPipMode || (activity?.isInPictureInPictureMode == true)
                if (!isPip) {
                    viewModel.pausePlayback()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Restore portrait orientation and pause video engine on leaving player screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.pausePlayback()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    // Apply Orientation Mode smoothly to Activity (Auto-rotate by default using hardware sensor)
    LaunchedEffect(orientationMode, isInPipMode) {
        if (!isInPipMode) {
            val target = when (orientationMode) {
                OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                OrientationMode.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            activity?.requestedOrientation = target
        }
    }

    // Auto-hide controls after 7 seconds of inactivity when playing and drawer is not open
    LaunchedEffect(
        uiState.controlsVisible,
        uiState.isPlaying,
        isScreenLocked,
        showStudioDrawer
    ) {
        if (uiState.controlsVisible && uiState.isPlaying && !isScreenLocked && !showStudioDrawer) {
            delay(7000L)
            viewModel.setControlsVisible(false)
        }
    }

    // Auto-hide unlock prompt after 3.5 seconds
    LaunchedEffect(showUnlockPrompt) {
        if (showUnlockPrompt) {
            delay(3500L)
            showUnlockPrompt = false
        }
    }

    // Auto-hide Zoom HUD after 1.5 seconds
    LaunchedEffect(showZoomHud, zoomScale) {
        if (showZoomHud) {
            delay(1500L)
            showZoomHud = false
        }
    }

    // Auto-hide Aspect Ratio HUD after 1.5 seconds
    LaunchedEffect(showAspectHud, aspectHudText) {
        if (showAspectHud) {
            delay(1500L)
            showAspectHud = false
        }
    }

    // Reset zoom when entering PiP
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            zoomScale = 1.0f
            panOffset = Offset.Zero
        }
    }

    // Re-hide system bars when controls hide
    LaunchedEffect(uiState.controlsVisible, isInPipMode) {
        if (!uiState.controlsVisible && !isInPipMode) {
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. OpenGL ES Video Surface with Interactive Pinch-to-Zoom and Pan
        val zoomModifier = if (zoomScale > 1.01f || panOffset != Offset.Zero) {
            Modifier
                .clipToBounds()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(zoomModifier)
        ) {
            MpvSurfaceCanvas(
                engine = viewModel.engine,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isInPipMode) {
            // 2. Unified Gesture Engine (Single-Tap, Double-Tap ±10s, Hold 2x Speed, Multi-Touch Pinch-to-Zoom, 60fps Scrubbing, Volume & Brightness)
            var lastTapTime by remember { mutableLongStateOf(0L) }
            var lastTapX by remember { mutableFloatStateOf(0f) }
            var holdSpeedJob by remember { mutableStateOf<Job?>(null) }
            var isHoldingForSpeed by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isScreenLocked, uiState.hasMediaLoaded, uiState.durationSeconds) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startY = down.position.y
                            var isDrag = false
                            var dragDirection = 0 // 1 = seek, 2 = brightness/volume
                            var seekStartX = 0f
                            var dragStartY = 0f
                            var startBrightness = 0.5f
                            var startVolumeFraction = 0.5f
                            var lastPinchDist = 0f
                            val maxSystemVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

                            // Launch Press & Hold for 2.0x Quick Speed Boost
                            if (!isScreenLocked && uiState.isPlaying && hold2xSpeedEnabled) {
                                holdSpeedJob?.cancel()
                                holdSpeedJob = scope.launch {
                                    delay(450L)
                                    isHoldingForSpeed = true
                                    viewModel.startFastForward()
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.filter { it.pressed }

                                if (active.size >= 2) {
                                    holdSpeedJob?.cancel()
                                    if (isHoldingForSpeed) {
                                        isHoldingForSpeed = false
                                        viewModel.stopFastForward()
                                    }

                                    // Pinch-to-zoom multi-touch with mathematically bounded pan viewport
                                    val p1 = active[0]
                                    val p2 = active[1]
                                    val dx = p2.position.x - p1.position.x
                                    val dy = p2.position.y - p1.position.y
                                    val dist = sqrt(dx * dx + dy * dy)

                                    if (lastPinchDist > 0f) {
                                        val factor = dist / lastPinchDist
                                        zoomScale = (zoomScale * factor).coerceIn(1.0f, 5.0f)
                                        showZoomHud = true

                                        val centerChange = (p1.positionChange() + p2.positionChange()) / 2f
                                        if (zoomScale > 1.01f) {
                                            val maxPanX = (size.width * (zoomScale - 1f)) / 2f
                                            val maxPanY = (size.height * (zoomScale - 1f)) / 2f
                                            val newPan = panOffset + centerChange
                                            panOffset = Offset(
                                                x = newPan.x.coerceIn(-maxPanX, maxPanX),
                                                y = newPan.y.coerceIn(-maxPanY, maxPanY)
                                            )
                                        }
                                    }
                                    lastPinchDist = dist
                                    p1.consume()
                                    p2.consume()
                                    isDrag = true
                                } else if (active.size == 1) {
                                    val change = active[0]
                                    val totalDx = change.position.x - startX
                                    val totalDy = change.position.y - startY

                                    if (!isDrag && (abs(totalDx) > 24f || abs(totalDy) > 24f)) {
                                        isDrag = true
                                        holdSpeedJob?.cancel()
                                        if (isHoldingForSpeed) {
                                            isHoldingForSpeed = false
                                            viewModel.stopFastForward()
                                        }

                                        if (abs(totalDx) > abs(totalDy) * 1.2f && uiState.hasMediaLoaded) {
                                            if (fastScrubEnabled) {
                                                dragDirection = 1
                                                seekStartX = change.position.x
                                                viewModel.onHorizontalScrubStart()
                                            }
                                        } else if (swipeGesturesEnabled) {
                                            dragDirection = 2
                                            dragStartY = change.position.y
                                            startBrightness = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                                            startVolumeFraction = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxSystemVol.toFloat()
                                        }
                                    }

                                    if (isDrag) {
                                        change.consume()
                                        if (dragDirection == 1) {
                                            val currentDx = change.position.x - seekStartX
                                            val normalizedX = (currentDx / size.width.toFloat()).coerceIn(-1.0f, 1.0f)
                                            val duration = uiState.durationSeconds
                                            // Adaptive maximum span based on video length
                                            val maxSpan = when {
                                                duration <= 60.0 -> duration.coerceAtLeast(10.0)
                                                duration <= 600.0 -> 60.0 + (duration - 60.0) * 0.35
                                                else -> (249.0 + (duration - 600.0) * 0.12).coerceAtMost(1200.0)
                                            }
                                            // Non-linear response curve: fine precision for small swipes, rapid traverse for wide swipes
                                            val sign = if (normalizedX >= 0f) 1.0 else -1.0
                                            val magnitude = abs(normalizedX).toDouble()
                                            val deltaSeconds = sign * maxSpan * magnitude.pow(1.25)
                                            viewModel.onHorizontalScrubDelta(deltaSeconds)
                                        } else if (dragDirection == 2) {
                                            val isLeftSide = startX < size.width * 0.45f
                                            val currentDy = change.position.y - dragStartY
                                            val deltaFraction = -currentDy / (size.height.toFloat() * 0.75f)

                                            if (isLeftSide && activity != null) {
                                                val targetB = (startBrightness + deltaFraction).coerceIn(0.01f, 1.0f)
                                                val lp = activity.window.attributes
                                                lp.screenBrightness = targetB
                                                activity.window.attributes = lp
                                                viewModel.showBrightnessHud((targetB * 100f).roundToInt())
                                            } else {
                                                val targetVol = (startVolumeFraction + deltaFraction).coerceIn(0f, 2.0f)
                                                if (targetVol <= 1.0f) {
                                                    val streamVol = (targetVol * maxSystemVol).roundToInt().coerceIn(0, maxSystemVol)
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                                                    viewModel.setSoftwareVolumeBoost(100)
                                                    viewModel.showVolumeHud((targetVol * 100f).roundToInt(), false)
                                                } else {
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxSystemVol, 0)
                                                    val boost = (targetVol * 100f).roundToInt()
                                                    viewModel.setSoftwareVolumeBoost(boost)
                                                    viewModel.showVolumeHud(boost, true)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Pointer released (UP)
                                    holdSpeedJob?.cancel()
                                    if (isHoldingForSpeed) {
                                        isHoldingForSpeed = false
                                        viewModel.stopFastForward()
                                    } else if (!isDrag) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTime < 350L && abs(startX - lastTapX) < 140f) {
                                            // DOUBLE TAP!
                                            lastTapTime = 0L
                                            if (zoomScale > 1.05f) {
                                                zoomScale = 1.0f
                                                panOffset = Offset.Zero
                                                showZoomHud = true
                                            } else {
                                                val third = size.width / 3f
                                                val seekDelta = doubleTapSeekSeconds.toDouble()
                                                when {
                                                    startX < third -> {
                                                        viewModel.seekRelative(-seekDelta)
                                                        scope.launch {
                                                            leftRippleAlpha.snapTo(0.85f)
                                                            leftRippleAlpha.animateTo(0f, tween(500))
                                                        }
                                                    }
                                                    startX > third * 2f -> {
                                                        viewModel.seekRelative(seekDelta)
                                                        scope.launch {
                                                            rightRippleAlpha.snapTo(0.85f)
                                                            rightRippleAlpha.animateTo(0f, tween(500))
                                                        }
                                                    }
                                                    else -> {
                                                        viewModel.togglePlayPause()
                                                    }
                                                }
                                            }
                                        } else {
                                            lastTapTime = now
                                            lastTapX = startX
                                            if (isScreenLocked) {
                                                showUnlockPrompt = true
                                            } else {
                                                viewModel.toggleControlsVisibility()
                                            }
                                        }
                                    } else {
                                        if (dragDirection == 1) {
                                            viewModel.onHorizontalScrubEnd()
                                        } else if (dragDirection == 2) {
                                            viewModel.hideVerticalHud()
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
            )

            // 3. Double-Tap Seek Ripples (Left & Right)
            if (leftRippleAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = leftRippleAlpha.value * 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "-${doubleTapSeekSeconds}s",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (rightRippleAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 48.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = rightRippleAlpha.value * 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "+${doubleTapSeekSeconds}s",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Zoom HUD Pill (e.g. Zoom 175%)
            if (showZoomHud) {
                Surface(
                    color = Color(0xD910141E),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x3300E5FF)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomIn,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Zoom ${(zoomScale * 100f).roundToInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 5. Zero-Buffering Horizontal Scrubbing HUD (`01:14:22 [+00:15]`)
            if (uiState.isScrubbing) {
                Surface(
                    color = Color(0xF00A0E18),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0x5500E5FF)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = formatTimestamp(uiState.scrubPreviewSeconds),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val sign = if (uiState.scrubDeltaSeconds >= 0) "+" else "-"
                        Text(
                            text = "[$sign${formatTimestamp(abs(uiState.scrubDeltaSeconds))}]",
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 6. Vertical Gesture HUD (Brightness / Volume + SW Boost)
            uiState.verticalHud?.let { hud ->
                Surface(
                    color = Color(0xD910141E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, when (hud) {
                        is VerticalGestureHud.Volume -> if (hud.isSoftwareBoosted) Color(0x66FF5252) else Color(0x3300E5FF)
                        is VerticalGestureHud.Brightness -> Color(0x33FFD54F)
                    }),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        when (hud) {
                            is VerticalGestureHud.Brightness -> {
                                Icon(
                                    imageVector = Icons.Rounded.Brightness6,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Brightness",
                                            color = Color(0xFFA0A0B0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${hud.percent}%",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = (hud.percent / 100f).coerceIn(0f, 1f))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFFFFB300), Color(0xFFFFD54F))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                            is VerticalGestureHud.Volume -> {
                                Icon(
                                    imageVector = if (hud.isSoftwareBoosted) {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeDown
                                    },
                                    contentDescription = null,
                                    tint = if (hud.isSoftwareBoosted) Color(0xFFFF5252) else Color(0xFF00E5FF),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (hud.isSoftwareBoosted) "SW Boost" else "Volume",
                                            color = if (hud.isSoftwareBoosted) Color(0xFFFF8A80) else Color(0xFFA0A0B0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${hud.percent}%",
                                            color = if (hud.isSoftwareBoosted) Color(0xFFFF5252) else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FFFFFF))
                                    ) {
                                        val fraction = (hud.percent / 200f).coerceIn(0f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = fraction)
                                                .background(
                                                    if (hud.isSoftwareBoosted) {
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF00E5FF), Color(0xFFFF5252))
                                                        )
                                                    } else {
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF00B0FF), Color(0xFF00E5FF))
                                                        )
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Buffering Progress Indicator
            if ((uiState.isBuffering || !uiState.hasMediaLoaded) && !uiState.isScrubbing && uiState.errorMessage == null) {
                CircularProgressIndicator(
                    color = Color(0xFF00E676),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }

            // 8. Fast Forward 2.0x HUD Pill (animated cyber cyan glass)
            if (uiState.isFastForwarding) {
                Surface(
                    color = Color(0xEE0A0E1A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            )
                        )
                        .padding(top = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2.0x Fast Forward",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 9. Aspect Ratio Toast Pill
            if (showAspectHud) {
                Surface(
                    color = Color(0xEE0A0E1A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aspectHudText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // 10. Child Lock / Screen Lock Floating Button (Safely padded away from selfie camera cutout & edges)
            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    FilledTonalButton(
                        onClick = {
                            isScreenLocked = false
                            Toast.makeText(context, "Screen Unlocked", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Unlock Screen",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (showUnlockPrompt) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = "Screen is Locked. Tap lock button to unlock.",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 11. Full Controls Overlay (Symmetrical adaptive layout with Cinema Studio button)
            FullPlayerControlsOverlay(
                isVisible = uiState.controlsVisible && !uiState.isScrubbing && !isScreenLocked,
                uiState = uiState,
                orientationMode = orientationMode,
                showRemainingTime = showRemainingTime,
                onToggleTimeDisplay = { showRemainingTime = !showRemainingTime },
                onCycleOrientation = {
                    orientationMode = orientationMode.next()
                    Toast.makeText(context, orientationMode.label, Toast.LENGTH_SHORT).show()
                },
                onLockScreen = {
                    isScreenLocked = true
                    viewModel.setControlsVisible(false)
                    Toast.makeText(context, "Screen Locked", Toast.LENGTH_SHORT).show()
                },
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onPrevious = onPrevious,
                onNext = onNext,
                onBack = onBack,
                onEnterPip = onEnterPip,
                onOpenStudioTab = { tab ->
                    viewModel.refreshTracks()
                    viewModel.refreshMediaInfo()
                    activeStudioTab = tab
                    showStudioDrawer = true
                },
                onQuickCycleSpeed = onQuickCycleSpeed,
                onCycleResizeMode = {
                    viewModel.cycleResizeMode()
                    val label = viewModel.uiState.value.resizeMode.label
                    aspectHudText = "Aspect Ratio: $label"
                    showAspectHud = true
                },
                onToggleRepeatOne = viewModel::toggleRepeatOne,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekRelative = viewModel::seekRelative,
                seekIntervalSeconds = doubleTapSeekSeconds.toDouble(),
                onSliderScrubChange = viewModel::onSliderScrubChange,
                onSliderScrubEnd = viewModel::onHorizontalScrubEnd
            )

            // 10. Empty State Prompt (only shown if launched standalone with no media URI)
            if (initialUri == null && !uiState.hasMediaLoaded) {
                EmptyPlayerPrompt(
                    errorMessage = uiState.errorMessage,
                    onOpenFileClick = { openVideoLauncher.launch(arrayOf("video/*")) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Playback Error Dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            containerColor = Color(0xFF1E1E26),
            title = {
                Text("Playback Error", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(uiState.errorMessage ?: "Unknown playback error", color = Color(0xFFA0A0B0))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissError()
                        onBack?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ==========================================
    // UNIFIED CINEMA STUDIO DRAWER
    // ==========================================
    CinemaStudioDrawer(
        isOpen = showStudioDrawer,
        currentTab = activeStudioTab,
        onTabSelected = { activeStudioTab = it },
        uiState = uiState,
        onSelectAudioTrack = { id -> viewModel.selectAudioTrack(id) },
        onDisableAudio = { viewModel.disableAudio() },
        onAdjustAudioDelay = { delta -> viewModel.setAudioDelay(delta) },
        onToggleNightModeAudio = {
            viewModel.toggleNightModeAudio()
            Toast.makeText(
                context,
                if (viewModel.uiState.value.isNightModeAudio) "Speech Clarifier Enabled" else "Speech Clarifier Disabled",
                Toast.LENGTH_SHORT
            ).show()
        },
        onSelectSubtitleTrack = { id -> viewModel.selectSubtitleTrack(id) },
        onDisableSubtitles = { viewModel.disableSubtitles() },
        onLoadExternalSubtitle = {
            openSubtitleLauncher.launch(arrayOf("*/*"))
            showStudioDrawer = false
        },
        onAdjustSubtitleDelay = { delta -> viewModel.setSubtitleDelay(delta) },
        onSelectResizeMode = { mode ->
            viewModel.setResizeMode(mode)
            aspectHudText = "Aspect Ratio: ${mode.label}"
            showAspectHud = true
        },
        onSelectDecoderMode = { mode -> viewModel.selectDecoderMode(mode) },
        onCaptureScreenshot = {
            viewModel.captureScreenshot(context) { _, success ->
                Toast.makeText(
                    context,
                    if (success) "Video frame captured to cache" else "Failed to capture frame",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onSelectPlaybackSpeed = { speed ->
            viewModel.setPlaybackSpeed(speed)
            Toast.makeText(context, "Playback Speed: ${speed}x", Toast.LENGTH_SHORT).show()
        },
        currentSleepTimerMinutes = sleepTimerRemainingMinutes,
        onSetSleepTimerMinutes = { minutes ->
            sleepTimerJob?.cancel()
            if (minutes > 0) {
                sleepTimerRemainingMinutes = minutes
                sleepTimerJob = scope.launch {
                    var left = minutes
                    while (left > 0) {
                        delay(60_000L)
                        left--
                        sleepTimerRemainingMinutes = left
                    }
                    viewModel.pausePlayback()
                    Toast.makeText(context, "Sleep Timer expired. Playback paused.", Toast.LENGTH_LONG).show()
                }
                Toast.makeText(context, "Sleep timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
            } else {
                sleepTimerRemainingMinutes = 0
                sleepTimerJob = null
                Toast.makeText(context, "Sleep timer cancelled", Toast.LENGTH_SHORT).show()
            }
        },
        onSetPointA = { viewModel.setABRepeatPointA() },
        onSetPointB = { viewModel.setABRepeatPointB() },
        onClearABRepeat = { viewModel.clearABRepeat() },
        onOpenFileClick = {
            showStudioDrawer = false
            openVideoLauncher.launch(arrayOf("video/*"))
        },
        onDismiss = { showStudioDrawer = false }
    )
}

@Composable
private fun MpvSurfaceCanvas(
    engine: NativeMpvEngine,
    modifier: Modifier = Modifier
) {
    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    DisposableEffect(engine) {
        onDispose {
            engine.detachFromSurfaceView()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                keepScreenOn = true
                surfaceViewRef = this
                engine.attachToSurfaceView(this)
            }
        },
        update = { view ->
            if (surfaceViewRef !== view) {
                surfaceViewRef = view
                engine.attachToSurfaceView(view)
            }
        },
        onRelease = {
            engine.detachFromSurfaceView()
        }
    )
}

@Composable
private fun FullPlayerControlsOverlay(
    isVisible: Boolean,
    uiState: PlayerUiState,
    orientationMode: OrientationMode,
    showRemainingTime: Boolean,
    onToggleTimeDisplay: () -> Unit,
    onCycleOrientation: () -> Unit,
    onLockScreen: () -> Unit,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    onOpenStudioTab: (StudioDrawerTab) -> Unit,
    onQuickCycleSpeed: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onToggleRepeatOne: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekRelative: (Double) -> Unit,
    seekIntervalSeconds: Double = 10.0,
    onSliderScrubChange: (Double) -> Unit,
    onSliderScrubEnd: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        // TOP CONTROLS BAR (Smoothly slides in from top)
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(animationSpec = tween(220)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(220)) { -it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF0020408),
                                Color(0xCC04060E),
                                Color(0x66000000),
                                Color.Transparent
                            )
                        )
                    )
                    // SAFE INSETS & CUTOUT PROTECTION:
                    // Padded away from punch-hole and selfie camera notches on all edges (top in portrait, sides in landscape)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onBack != null) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = Color(0x33FFFFFF),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Media Title & Technical Metadata Badges
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = uiState.mediaTitle.ifBlank { "libmpv HW+ Player" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { onOpenStudioTab(StudioDrawerTab.VIDEO) },
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0x2600E5FF),
                                border = BorderStroke(0.5.dp, Color(0x8000E5FF))
                            ) {
                                Text(
                                    text = uiState.decoderMode.label,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
                                Text(
                                    text = " • ${uiState.videoWidth}x${uiState.videoHeight}",
                                    color = Color(0xCCFFFFFF),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (uiState.isNightModeAudio) {
                                Text(
                                    text = " • Night Audio",
                                    color = Color(0xFF80D8FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (uiState.abRepeatPointA != null && uiState.abRepeatPointB != null) {
                                Text(
                                    text = " • A-B Loop",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // In Landscape: show active center status pill if A-B loop or Sleep Timer active
                    if (isLandscape && (uiState.abRepeatPointA != null || uiState.isNightModeAudio)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2200E5FF),
                            border = BorderStroke(1.dp, Color(0x4400E5FF)),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (uiState.abRepeatPointA != null && uiState.abRepeatPointB != null) {
                                        "🔁 A-B [${formatDelta(uiState.abRepeatPointA)} ⇄ ${formatDelta(uiState.abRepeatPointB)}]"
                                    } else if (uiState.abRepeatPointA != null) {
                                        "🔁 A- [${formatDelta(uiState.abRepeatPointA)}]"
                                    } else {
                                        "🌙 Night Audio"
                                    },
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // RIGHT ACTION BUTTONS CLUSTER:
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Quick Audio Button
                        IconButton(
                            onClick = { onOpenStudioTab(StudioDrawerTab.AUDIO) },
                            modifier = Modifier.size(if (isLandscape) 36.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = "Audio Tracks",
                                modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp),
                                tint = if (uiState.audioTracks.size > 1 || uiState.isNightModeAudio) Color(0xFF00E5FF) else Color.White
                            )
                        }

                        // Quick Subtitle Button
                        IconButton(
                            onClick = { onOpenStudioTab(StudioDrawerTab.SUBTITLES) },
                            modifier = Modifier.size(if (isLandscape) 36.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = "Subtitles",
                                modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp),
                                tint = if (uiState.selectedSubtitleTrackId != null) Color(0xFF00E5FF) else Color.White
                            )
                        }

                        // Screen Auto-Rotation Toggle
                        IconButton(
                            onClick = onCycleOrientation,
                            modifier = Modifier.size(if (isLandscape) 36.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = when (orientationMode) {
                                    OrientationMode.LANDSCAPE -> Icons.Rounded.StayCurrentLandscape
                                    OrientationMode.PORTRAIT -> Icons.Rounded.StayCurrentPortrait
                                    OrientationMode.SENSOR -> Icons.Rounded.ScreenRotation
                                },
                                contentDescription = orientationMode.label,
                                modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp),
                                tint = Color.White
                            )
                        }

                        // Picture-in-Picture
                        if (onEnterPip != null) {
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier.size(if (isLandscape) 36.dp else 38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = "Enter PiP",
                                    modifier = Modifier.size(if (isLandscape) 20.dp else 22.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // More Options (Three Dots Menu)
                        IconButton(
                            onClick = { onOpenStudioTab(StudioDrawerTab.PLAYBACK) },
                            modifier = Modifier.size(if (isLandscape) 36.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(if (isLandscape) 22.dp else 24.dp)
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM PLAYBACK CONTROLS BAR (Smoothly slides in from bottom)
        if (uiState.hasMediaLoaded) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(animationSpec = tween(220)) { it } + fadeOut(tween(220)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x80000000), Color(0xDD04060E), Color(0xF5020408))
                            )
                        )
                        // SAFE DRAWING & DISPLAY CUTOUT ON BOTTOM:
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Seekbar & Time labels row (Symmetrical with fixed min-width for time labels)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current Position (Elapsed)
                        Text(
                            text = formatTimestamp(uiState.positionSeconds),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 54.dp)
                        )

                        // 60fps Keyframe Scrubbing Slider with Cyan Glow
                        val sliderValue = if (uiState.isScrubbing) {
                            uiState.scrubPreviewSeconds
                        } else {
                            uiState.positionSeconds
                        }.coerceIn(0.0, uiState.durationSeconds.coerceAtLeast(1.0)).toFloat()

                        Slider(
                            value = sliderValue,
                            onValueChange = { targetSec ->
                                onSliderScrubChange(targetSec.toDouble())
                            },
                            onValueChangeFinished = onSliderScrubEnd,
                            valueRange = 0f..uiState.durationSeconds.coerceAtLeast(1.0).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        // Total Duration or Remaining Time (Tappable to toggle)
                        val durationDisplay = if (showRemainingTime) {
                            val remaining = (uiState.durationSeconds - uiState.positionSeconds).coerceAtLeast(0.0)
                            "-${formatTimestamp(remaining)}"
                        } else {
                            formatTimestamp(uiState.durationSeconds)
                        }

                        Text(
                            text = durationDisplay,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(min = 54.dp)
                                .clickable { onToggleTimeDisplay() }
                        )
                    }

                    // Playback Control Buttons (Symmetrical layout in both landscape and portrait)
                    if (isLandscape) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Left Wing: Speed Pill + Lock Screen
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onQuickCycleSpeed,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.playbackSpeed}x",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = onLockScreen,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Lock Screen",
                                        tint = Color(0xFFFF5252).copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Center Hero Cluster: Prev, -10s, Hero Play/Pause, +10s, Next (Anchored at exact center)
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = { onPrevious?.invoke() },
                                    enabled = hasPrevious,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous video",
                                        tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.32f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSeekRelative(-seekIntervalSeconds) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay10,
                                        contentDescription = "Rewind ${seekIntervalSeconds.toInt()} seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Surface(
                                    onClick = onTogglePlayPause,
                                    shape = CircleShape,
                                    color = Color(0xD90D1322),
                                    border = BorderStroke(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                        )
                                    ),
                                    modifier = Modifier.size(62.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) {
                                                Icons.Rounded.Pause
                                            } else {
                                                Icons.Rounded.PlayArrow
                                            },
                                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onSeekRelative(seekIntervalSeconds) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forward10,
                                        contentDescription = "Forward ${seekIntervalSeconds.toInt()} seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onNext?.invoke() },
                                    enabled = hasNext,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next video",
                                        tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.32f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Right Wing: Repeat Mode + Resize Mode
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onToggleRepeatOne,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isRepeatOne) {
                                            Icons.Rounded.RepeatOne
                                        } else {
                                            Icons.Rounded.Repeat
                                        },
                                        contentDescription = "Repeat Mode",
                                        tint = if (uiState.isRepeatOne) Color(0xFF00E5FF) else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    onClick = onCycleResizeMode,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = when (uiState.resizeMode) {
                                                ResizeMode.FIT -> "FIT"
                                                ResizeMode.STRETCH -> "STRETCH"
                                                ResizeMode.CROP -> "CROP"
                                                ResizeMode.ORIGINAL_100 -> "100%"
                                                ResizeMode.SIXTEEN_NINE -> "16:9"
                                                ResizeMode.FOUR_THREE -> "4:3"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // PORTRAIT: Ergonomic symmetrical 2-tier layout
                        // Tier 1: Hero Playback Cluster (Centered)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onPrevious?.invoke() },
                                enabled = hasPrevious,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous video",
                                    tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.32f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = { onSeekRelative(-seekIntervalSeconds) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = "Rewind ${seekIntervalSeconds.toInt()} seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // HERO PLAY/PAUSE BUTTON
                            Surface(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                color = Color(0xD90D1322),
                                border = BorderStroke(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                    )
                                ),
                                modifier = Modifier.size(62.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPlaying) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = { onSeekRelative(seekIntervalSeconds) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = "Forward ${seekIntervalSeconds.toInt()} seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = { onNext?.invoke() },
                                enabled = hasNext,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next video",
                                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.32f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Tier 2: Symmetrical Action Bar (Left: Speed + Lock, Right: Repeat + Resize)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Left Wing: Speed Pill + Lock Screen
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onQuickCycleSpeed,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.playbackSpeed}x",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = onLockScreen,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Lock Screen",
                                        tint = Color(0xFFFF5252).copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Right Wing: Repeat Mode + Resize Mode
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onToggleRepeatOne,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isRepeatOne) {
                                            Icons.Rounded.RepeatOne
                                        } else {
                                            Icons.Rounded.Repeat
                                        },
                                        contentDescription = "Repeat Mode",
                                        tint = if (uiState.isRepeatOne) Color(0xFF00E5FF) else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(
                                    onClick = onCycleResizeMode,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22FFFFFF),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = when (uiState.resizeMode) {
                                                ResizeMode.FIT -> "FIT"
                                                ResizeMode.STRETCH -> "STRETCH"
                                                ResizeMode.CROP -> "CROP"
                                                ResizeMode.ORIGINAL_100 -> "100%"
                                                ResizeMode.SIXTEEN_NINE -> "16:9"
                                                ResizeMode.FOUR_THREE -> "4:3"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UNIFIED CINEMA STUDIO DRAWER ARCHITECTURE
// ==========================================

@Composable
private fun CinemaStudioDrawer(
    isOpen: Boolean,
    currentTab: StudioDrawerTab,
    onTabSelected: (StudioDrawerTab) -> Unit,
    uiState: PlayerUiState,
    onSelectAudioTrack: (Int) -> Unit,
    onDisableAudio: () -> Unit,
    onAdjustAudioDelay: (Double) -> Unit,
    onToggleNightModeAudio: () -> Unit,
    onSelectSubtitleTrack: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onLoadExternalSubtitle: () -> Unit,
    onAdjustSubtitleDelay: (Double) -> Unit,
    onSelectResizeMode: (ResizeMode) -> Unit,
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onSelectPlaybackSpeed: (Double) -> Unit,
    currentSleepTimerMinutes: Int,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearABRepeat: () -> Unit,
    onOpenFileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var backStackTarget by remember { mutableStateOf<StudioDrawerTab?>(null) }

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            backStackTarget = null
        }
    }

    if (isOpen) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Backdrop Scrim (subtle translucent touch-out dismiss scrim allowing video to show through)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x30000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            backStackTarget = null
                            onDismiss()
                        }
                    )
            )

            // Animated Drawer Container (Glassmorphism / Frosted Translucent Design)
            // Landscape: slides in from right edge (420dp width, full height) with glass styling
            // Portrait: slides up from bottom (full width, max 600dp) with rounded glass corners
            AnimatedVisibility(
                visible = isOpen,
                enter = if (isLandscape) {
                    slideInHorizontally(tween(240)) { it } + fadeIn(tween(180))
                } else {
                    slideInVertically(tween(240)) { it } + fadeIn(tween(180))
                },
                exit = if (isLandscape) {
                    slideOutHorizontally(tween(220)) { it } + fadeOut(tween(160))
                } else {
                    slideOutVertically(tween(220)) { it } + fadeOut(tween(160))
                },
                modifier = Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
            ) {
                Surface(
                    shape = if (isLandscape) {
                        RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    } else {
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    },
                    color = Color(0x94080D18), // High-transparency glass background (~58% opacity)
                    border = BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x5500E5FF), Color(0x2200E5FF), Color(0x3D00E5FF))
                        )
                    ),
                    modifier = if (isLandscape) {
                        Modifier
                            .width(420.dp)
                            .fillMaxHeight()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Vertical + WindowInsetsSides.End
                                )
                            )
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                                )
                            )
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Portrait Drag Handle
                        if (!isLandscape) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 4.dp)
                                        .background(Color(0x55FFFFFF), CircleShape)
                                )
                            }
                        }

                        // Modal Header (Dedicated per modal sheet, clean Material You styling)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            if (backStackTarget != null) {
                                IconButton(
                                    onClick = {
                                        val target = backStackTarget ?: StudioDrawerTab.PLAYBACK
                                        backStackTarget = null
                                        onTabSelected(target)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to Player Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0x2A00E5FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = when (currentTab) {
                                                StudioDrawerTab.AUDIO -> Icons.Rounded.Audiotrack
                                                StudioDrawerTab.SUBTITLES -> Icons.Rounded.Subtitles
                                                StudioDrawerTab.VIDEO -> Icons.Rounded.Memory
                                                StudioDrawerTab.PLAYBACK -> Icons.Rounded.Tune
                                                StudioDrawerTab.SPECS -> Icons.Rounded.Info
                                            },
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (currentTab) {
                                            StudioDrawerTab.AUDIO -> "Audio Tracks"
                                            StudioDrawerTab.SUBTITLES -> "Subtitles"
                                            StudioDrawerTab.VIDEO -> "Hardware Decoder & Display"
                                            StudioDrawerTab.PLAYBACK -> "Player Settings"
                                            StudioDrawerTab.SPECS -> "Media Details"
                                        },
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentTab == StudioDrawerTab.VIDEO) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0x2200E5FF),
                                            border = BorderStroke(0.5.dp, Color(0x6600E5FF)),
                                            modifier = Modifier.height(18.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            ) {
                                                Text(
                                                    text = uiState.decoderMode.label,
                                                    color = Color(0xFF00E5FF),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = when (currentTab) {
                                        StudioDrawerTab.AUDIO -> if (uiState.audioTracks.size > 1) "${uiState.audioTracks.size} audio streams • Speech clarifier & sync" else "Speech clarifier & audio delay sync"
                                        StudioDrawerTab.SUBTITLES -> if (uiState.selectedSubtitleTrackId != null) "Active subtitle stream • Delay sync & custom files" else "External subtitle loader & delay sync"
                                        StudioDrawerTab.VIDEO -> "Hardware acceleration mode & aspect ratio"
                                        StudioDrawerTab.PLAYBACK -> "Speed, aspect ratio, sleep timer & tools"
                                        StudioDrawerTab.SPECS -> "${uiState.videoWidth}x${uiState.videoHeight} • ${uiState.mediaInfo.videoCodec.ifBlank { "libmpv stream" }}"
                                    },
                                    color = Color(0xFFA0A0B0),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    backStackTarget = null
                                    onDismiss()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0x18FFFFFF),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Scrollable Tab Body
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (currentTab) {
                                StudioDrawerTab.AUDIO -> {
                                    AudioTabContent(
                                        uiState = uiState,
                                        onSelectTrack = onSelectAudioTrack,
                                        onDisableAudio = onDisableAudio,
                                        onAdjustDelay = onAdjustAudioDelay,
                                        onToggleNightModeAudio = onToggleNightModeAudio
                                    )
                                }
                                StudioDrawerTab.SUBTITLES -> {
                                    SubtitlesTabContent(
                                        uiState = uiState,
                                        onSelectTrack = onSelectSubtitleTrack,
                                        onDisableSubtitles = onDisableSubtitles,
                                        onLoadExternalSubtitle = onLoadExternalSubtitle,
                                        onAdjustDelay = onAdjustSubtitleDelay
                                    )
                                }
                                StudioDrawerTab.VIDEO -> {
                                    VideoTabContent(
                                        uiState = uiState,
                                        onSelectResizeMode = onSelectResizeMode,
                                        onSelectDecoderMode = onSelectDecoderMode,
                                        onCaptureScreenshot = onCaptureScreenshot
                                    )
                                }
                                StudioDrawerTab.PLAYBACK -> {
                                    PlaybackTabContent(
                                        uiState = uiState,
                                        onSelectPlaybackSpeed = onSelectPlaybackSpeed,
                                        onSelectResizeMode = onSelectResizeMode,
                                        currentSleepTimerMinutes = currentSleepTimerMinutes,
                                        onSetSleepTimerMinutes = onSetSleepTimerMinutes,
                                        onSetPointA = onSetPointA,
                                        onSetPointB = onSetPointB,
                                        onClearABRepeat = onClearABRepeat,
                                        onNavigateToTab = { target ->
                                            backStackTarget = StudioDrawerTab.PLAYBACK
                                            onTabSelected(target)
                                        },
                                        onCaptureScreenshot = onCaptureScreenshot,
                                        onOpenFileClick = onOpenFileClick
                                    )
                                }
                                StudioDrawerTab.SPECS -> {
                                    SpecsTabContent(info = uiState.mediaInfo)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTabContent(
    uiState: PlayerUiState,
    onSelectTrack: (Int) -> Unit,
    onDisableAudio: () -> Unit,
    onAdjustDelay: (Double) -> Unit,
    onToggleNightModeAudio: () -> Unit
) {
    // 1. Dialogue Clarifier / Night Mode Card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x1EFFFFFF),
        border = BorderStroke(1.dp, if (uiState.isNightModeAudio) Color(0x6600E676) else Color(0x18FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (uiState.isNightModeAudio) Color(0x3300E676) else Color(0x22FFFFFF),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Hearing,
                        contentDescription = null,
                        tint = if (uiState.isNightModeAudio) Color(0xFF00E676) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Speech Clarifier",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Softens loud sounds and boosts quiet speech for comfortable listening",
                    color = Color(0xFFA0A0B0),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Switch(
                checked = uiState.isNightModeAudio,
                onCheckedChange = { onToggleNightModeAudio() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00E676),
                    checkedTrackColor = Color(0x6600E676),
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0x33FFFFFF)
                )
            )
        }
    }

    // 2. Audio Delay Sync
    SyncAdjuster(
        label = "Audio Delay Sync",
        delaySec = uiState.audioDelaySeconds,
        onAdjust = onAdjustDelay
    )

    // 3. Audio Streams List
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "AUDIO STREAMS",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        // Mute / Off option
        val isMuted = (uiState.selectedAudioTrackId == null)
        Surface(
            onClick = onDisableAudio,
            shape = RoundedCornerShape(12.dp),
            color = if (isMuted) Color(0x33FF5252) else Color(0x18FFFFFF),
            border = BorderStroke(1.dp, if (isMuted) Color(0xFFFF5252) else Color(0x18FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                RadioButton(
                    selected = isMuted,
                    onClick = onDisableAudio,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5252))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Mute Audio (Off)",
                        color = if (isMuted) Color(0xFFFF5252) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Disables audio stream output",
                        color = Color(0xFFA0A0B0),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (uiState.audioTracks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x18FFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Default Audio Stream (No alternate tracks detected)",
                    color = Color(0xFFA0A0B0),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            uiState.audioTracks.forEach { track ->
                val isSelected = (track.id == uiState.selectedAudioTrackId) || track.isSelected
                Surface(
                    onClick = { onSelectTrack(track.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTrack(track.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.displayName,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            val details = listOfNotNull(
                                track.codec?.uppercase(java.util.Locale.US),
                                if (track.channels > 0) "${track.channels} ch" else null,
                                if (track.sampleRate > 0) "${track.sampleRate} Hz" else null
                            ).joinToString(" • ")
                            if (details.isNotBlank()) {
                                Text(
                                    text = details,
                                    color = Color(0xFFA0A0B0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitlesTabContent(
    uiState: PlayerUiState,
    onSelectTrack: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onLoadExternalSubtitle: () -> Unit,
    onAdjustDelay: (Double) -> Unit
) {
    // 1. External Subtitle Loader Card
    Surface(
        onClick = onLoadExternalSubtitle,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x2200E5FF),
        border = BorderStroke(1.dp, Color(0x6600E5FF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x3300E5FF),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "+ Load External Subtitle File",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supports .srt, .ass, .vtt, .sub files from device storage",
                    color = Color(0xFFA0A0B0),
                    fontSize = 11.sp
                )
            }
        }
    }

    // 2. Subtitle Delay Sync
    SyncAdjuster(
        label = "Subtitle Delay Sync",
        delaySec = uiState.subtitleDelaySeconds,
        onAdjust = onAdjustDelay
    )

    // 3. Subtitles List
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SUBTITLE TRACKS",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        // Disable Subtitles
        val isOff = (uiState.selectedSubtitleTrackId == null)
        Surface(
            onClick = onDisableSubtitles,
            shape = RoundedCornerShape(12.dp),
            color = if (isOff) Color(0x3300E5FF) else Color(0x18FFFFFF),
            border = BorderStroke(1.dp, if (isOff) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                RadioButton(
                    selected = isOff,
                    onClick = onDisableSubtitles,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Subtitles Off",
                        color = if (isOff) Color(0xFF00E5FF) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Hide on-screen captions",
                        color = Color(0xFFA0A0B0),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (uiState.subtitleTracks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x18FFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No embedded subtitles found. Tap '+ Load External Subtitle File' above to add captions.",
                    color = Color(0xFFA0A0B0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            uiState.subtitleTracks.forEach { track ->
                val isSelected = (track.id == uiState.selectedSubtitleTrackId)
                Surface(
                    onClick = { onSelectTrack(track.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTrack(track.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.displayName,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (!track.codec.isNullOrBlank()) {
                                Text(
                                    text = track.codec.uppercase(java.util.Locale.US),
                                    color = Color(0xFFA0A0B0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTabContent(
    uiState: PlayerUiState,
    onSelectResizeMode: (ResizeMode) -> Unit,
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onCaptureScreenshot: () -> Unit
) {
    // 1. Hardware Decoder Selection
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "HARDWARE ACCELERATION",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        val decoderOptions = listOf(
            Triple(
                DecoderMode.HW_PLUS,
                "HW+ Direct Surface",
                "Zero-copy direct surface rendering • Highest fps & best battery"
            ),
            Triple(
                DecoderMode.HW,
                "HW MediaCodec",
                "Standard Android hardware decoding pipeline"
            ),
            Triple(
                DecoderMode.SW,
                "SW Software",
                "CPU software decoding • Universal compatibility for all codecs"
            )
        )

        decoderOptions.forEach { (mode, label, desc) ->
            val isSelected = (mode == uiState.decoderMode)
            Surface(
                onClick = { onSelectDecoderMode(mode) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectDecoderMode(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (mode == DecoderMode.HW_PLUS) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0x3300E676),
                                    border = BorderStroke(0.5.dp, Color(0x6600E676))
                                ) {
                                    Text(
                                        text = "RECOMMENDED",
                                        color = Color(0xFF00E676),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = desc,
                            color = Color(0xFFA0A0B0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }

    // 2. Aspect Ratio Presets
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SCREEN ASPECT RATIO",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        val aspectOptions = listOf(
            Triple(ResizeMode.FIT, "Fit Screen", "Standard proportions"),
            Triple(ResizeMode.CROP, "Crop / Fill", "Zoom to fill screen"),
            Triple(ResizeMode.STRETCH, "Stretch", "Stretch full screen"),
            Triple(ResizeMode.SIXTEEN_NINE, "16:9", "Widescreen video"),
            Triple(ResizeMode.FOUR_THREE, "4:3", "Classic TV"),
            Triple(ResizeMode.ORIGINAL_100, "100% 1:1", "Original pixel size")
        )

        for (i in aspectOptions.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in i..minOf(i + 1, aspectOptions.size - 1)) {
                    val (mode, label, desc) = aspectOptions[j]
                    val isSelected = (mode == uiState.resizeMode)
                    Surface(
                        onClick = { onSelectResizeMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = desc,
                                    color = Color(0xFFA0A0B0),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Frame Capture Action
    Surface(
        onClick = onCaptureScreenshot,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x1EFFFFFF),
        border = BorderStroke(1.dp, Color(0x33FFB300)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x33FFB300),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capture Video Screenshot",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saves current frame at original video resolution to pictures",
                    color = Color(0xFFA0A0B0),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PlaybackTabContent(
    uiState: PlayerUiState,
    onSelectPlaybackSpeed: (Double) -> Unit,
    onSelectResizeMode: (ResizeMode) -> Unit,
    currentSleepTimerMinutes: Int,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearABRepeat: () -> Unit,
    onNavigateToTab: (StudioDrawerTab) -> Unit,
    onCaptureScreenshot: () -> Unit,
    onOpenFileClick: () -> Unit
) {
    // 1. Playback Speed Selector (User-friendly horizontal chips with active highlight)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PLAYBACK SPEED",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "${uiState.playbackSpeed}x",
                color = if (uiState.playbackSpeed != 1.0) Color(0xFF00E676) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        val speedPresets = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speedPresets.forEach { speed ->
                val isSelected = (uiState.playbackSpeed == speed)
                Surface(
                    onClick = { onSelectPlaybackSpeed(speed) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (speed == 1.0) "1.0x Normal" else "${speed}x",
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // 2. Aspect Ratio Quick Selector
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ASPECT RATIO",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = uiState.resizeMode.label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        val aspectPresets = listOf(
            ResizeMode.FIT to "Fit",
            ResizeMode.CROP to "Crop",
            ResizeMode.STRETCH to "Stretch",
            ResizeMode.SIXTEEN_NINE to "16:9",
            ResizeMode.FOUR_THREE to "4:3",
            ResizeMode.ORIGINAL_100 to "100%"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspectPresets.forEach { (mode, label) ->
                val isSelected = (uiState.resizeMode == mode)
                Surface(
                    onClick = { onSelectResizeMode(mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // 3. Sleep Timer
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SLEEP TIMER",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            if (currentSleepTimerMinutes > 0) {
                Text(
                    text = "Pausing in $currentSleepTimerMinutes min",
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val timerPresets = listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timerPresets.forEach { (minutes, label) ->
                val isSelected = (currentSleepTimerMinutes == minutes)
                Surface(
                    onClick = { onSetSleepTimerMinutes(minutes) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0x3300E5FF) else Color(0x18FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x18FFFFFF)),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // 4. A-B Repeat Looper
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x1EFFFFFF),
        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "A-B REPEAT LOOPER",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Point A: ${uiState.abRepeatPointA?.let { formatTimestamp(it) } ?: "--:--"}",
                        color = if (uiState.abRepeatPointA != null) Color(0xFF00E676) else Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Point B: ${uiState.abRepeatPointB?.let { formatTimestamp(it) } ?: "--:--"}",
                        color = if (uiState.abRepeatPointB != null) Color(0xFF00E676) else Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onSetPointA,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x3300E5FF))
                    ) {
                        Text("Set A", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onSetPointB,
                        enabled = (uiState.abRepeatPointA != null && uiState.positionSeconds > (uiState.abRepeatPointA ?: 0.0)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x3300E5FF))
                    ) {
                        Text("Set B", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.abRepeatPointA != null || uiState.abRepeatPointB != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClearABRepeat,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Clear Loop Points", color = Color(0xFFFF5252), fontSize = 11.sp)
                }
            }
        }
    }

    // 5. Player Options Quick Links (Material You Menu Items)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "PLAYER OPTIONS & TOOLS",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        // Hardware Decoder Link
        SettingsNavTile(
            icon = Icons.Rounded.Memory,
            title = "Hardware Decoder & Display",
            subtitle = "Active: ${uiState.decoderMode.label} • ${uiState.resizeMode.label}",
            badge = uiState.decoderMode.label,
            onClick = { onNavigateToTab(StudioDrawerTab.VIDEO) }
        )

        // Audio Tracks Link
        SettingsNavTile(
            icon = Icons.Rounded.Audiotrack,
            title = "Audio Tracks & Sync",
            subtitle = if (uiState.audioTracks.size > 1) "${uiState.audioTracks.size} streams available" else "Audio streams & delay sync",
            badge = if (uiState.isNightModeAudio) "Speech Clarifier" else null,
            onClick = { onNavigateToTab(StudioDrawerTab.AUDIO) }
        )

        // Subtitles Link
        SettingsNavTile(
            icon = Icons.Rounded.Subtitles,
            title = "Subtitles & Captions",
            subtitle = if (uiState.selectedSubtitleTrackId != null) "Subtitles active" else "Captions & external subtitle files",
            badge = if (uiState.selectedSubtitleTrackId != null) "ON" else null,
            onClick = { onNavigateToTab(StudioDrawerTab.SUBTITLES) }
        )

        // Screenshot Action
        SettingsNavTile(
            icon = Icons.Rounded.CameraAlt,
            title = "Capture Video Screenshot",
            subtitle = "Save current frame at native resolution",
            onClick = onCaptureScreenshot
        )

        // Media Specs Link
        SettingsNavTile(
            icon = Icons.Rounded.Info,
            title = "Media Details & Technical Specs",
            subtitle = "${uiState.videoWidth}x${uiState.videoHeight} • ${uiState.mediaInfo.videoCodec.ifBlank { "Stream Info" }}",
            onClick = { onNavigateToTab(StudioDrawerTab.SPECS) }
        )

        // Open File Action
        SettingsNavTile(
            icon = Icons.Rounded.FolderOpen,
            title = "Open Another Video File",
            subtitle = "Browse device storage for media files",
            onClick = onOpenFileClick
        )
    }
}

@Composable
private fun SettingsNavTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x18FFFFFF),
        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x2200E5FF),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFA0A0B0),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x2A00E5FF),
                    border = BorderStroke(0.5.dp, Color(0x8000E5FF)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SpecsTabContent(info: MediaInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "MEDIA DETAILS & SPECS",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0x1EFFFFFF),
            border = BorderStroke(1.dp, Color(0x18FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                InfoRow(label = "Title", value = info.title)
                InfoRow(label = "Resolution", value = info.resolution)
                InfoRow(label = "Video Codec", value = info.videoCodec)
                if (info.videoFps.isNotBlank()) {
                    InfoRow(label = "Framerate", value = info.videoFps)
                }
                InfoRow(label = "Audio Codec", value = info.audioCodec)
                if (info.audioChannels.isNotBlank()) {
                    InfoRow(label = "Channels", value = info.audioChannels)
                }
                if (info.sampleRate.isNotBlank()) {
                    InfoRow(label = "Sample Rate", value = info.sampleRate)
                }
                InfoRow(label = "Hardware Decoder", value = info.hwdec)
                InfoRow(label = "Buffer Pipeline", value = info.demuxerCache)
            }
        }
    }
}

@Composable
private fun SyncAdjuster(
    label: String,
    delaySec: Double,
    onAdjust: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0x1EFFFFFF),
        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        delaySec > 0.001 -> "+${String.format(java.util.Locale.US, "%.2f", delaySec)}s"
                        delaySec < -0.001 -> "${String.format(java.util.Locale.US, "%.2f", delaySec)}s"
                        else -> "Synced (0.0s)"
                    },
                    color = if (delaySec != 0.0) Color(0xFF00E5FF) else Color(0xFFA0A0B0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onAdjust(delaySec - 0.5) },
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x26FFFFFF))
                ) {
                    Text("-0.5s", fontSize = 11.sp, color = Color.White)
                }
                FilledTonalButton(
                    onClick = { onAdjust(delaySec - 0.1) },
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x26FFFFFF))
                ) {
                    Text("-0.1s", fontSize = 11.sp, color = Color.White)
                }
                FilledTonalButton(
                    onClick = { onAdjust(0.0) },
                    enabled = delaySec != 0.0,
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0x3300E5FF),
                        contentColor = Color(0xFF00E5FF)
                    )
                ) {
                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(
                    onClick = { onAdjust(delaySec + 0.1) },
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x26FFFFFF))
                ) {
                    Text("+0.1s", fontSize = 11.sp, color = Color.White)
                }
                FilledTonalButton(
                    onClick = { onAdjust(delaySec + 0.5) },
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x26FFFFFF))
                ) {
                    Text("+0.5s", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFFA0A0B0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value.ifBlank { "N/A" },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
        )
    }
}

@Composable
private fun EmptyPlayerPrompt(
    errorMessage: String?,
    onOpenFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E1E26),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "libmpv HW+ Native Player",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Zero-buffering keyframe scrubbing • 150MiB RAM ring buffer",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenFileClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("Select Local Video", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}



private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

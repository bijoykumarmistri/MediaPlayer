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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.res.Configuration
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * Screen orientation mode matching MX Player's rotation toggle:
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
 * Production-ready C++ core (libmpv HW+) video player screen replicating MX Player:
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

    // Dialog & Menu visibility states
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var showMediaInfoDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showABRepeatDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Screen Touch Lock
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }

    // Screen Orientation Mode
    var orientationMode by remember { mutableStateOf(OrientationMode.SENSOR) }

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

    // Auto-hide controls after 5 seconds of inactivity when playing and no dialog is open
    LaunchedEffect(
        uiState.controlsVisible,
        uiState.isPlaying,
        isScreenLocked,
        showAudioDialog,
        showSubtitleDialog,
        showSpeedDialog,
        showResizeDialog,
        showMediaInfoDialog,
        showSleepTimerDialog,
        showABRepeatDialog,
        showMoreMenu
    ) {
        if (uiState.controlsVisible && uiState.isPlaying && !isScreenLocked &&
            !showAudioDialog && !showSubtitleDialog && !showSpeedDialog &&
            !showResizeDialog && !showMediaInfoDialog && !showSleepTimerDialog &&
            !showABRepeatDialog && !showMoreMenu
        ) {
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

            // 11. Full Controls Overlay (Adaptive Portrait & Landscape Layouts + Camera Cutout safe margin)
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
                onOpenFileClick = { openVideoLauncher.launch(arrayOf("video/*")) },
                onOpenAudioDialog = {
                    viewModel.refreshTracks()
                    showAudioDialog = true
                },
                onOpenSubtitleDialog = {
                    viewModel.refreshTracks()
                    showSubtitleDialog = true
                },
                onOpenSpeedDialog = { showSpeedDialog = true },
                onOpenResizeDialog = { showResizeDialog = true },
                onCycleResizeMode = {
                    viewModel.cycleResizeMode()
                    val label = viewModel.uiState.value.resizeMode.label
                    aspectHudText = "Aspect Ratio: $label"
                    showAspectHud = true
                },
                onOpenMediaInfoDialog = {
                    viewModel.refreshMediaInfo()
                    showMediaInfoDialog = true
                },
                onOpenSleepTimerDialog = { showSleepTimerDialog = true },
                onOpenABRepeatDialog = { showABRepeatDialog = true },
                onToggleNightModeAudio = {
                    viewModel.toggleNightModeAudio()
                    Toast.makeText(
                        context,
                        if (viewModel.uiState.value.isNightModeAudio) "Audio Night Mode Enabled (Speech Clarifier)" else "Audio Night Mode Disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onCaptureScreenshot = {
                    viewModel.captureScreenshot(context) { path, success ->
                        Toast.makeText(
                            context,
                            if (success) "Video frame captured to cache" else "Failed to capture frame",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onToggleRepeatOne = viewModel::toggleRepeatOne,
                onSelectDecoderMode = viewModel::selectDecoderMode,
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
    // DIALOGS & OVERLAYS
    // ==========================================

    // Audio Track Selector Dialog (Multi-Audio Streams + Audio Delay Sync)
    if (showAudioDialog) {
        AudioTrackSelectorDialog(
            audioTracks = uiState.audioTracks,
            selectedTrackId = uiState.selectedAudioTrackId,
            currentDelaySec = uiState.audioDelaySeconds,
            onSelectTrack = { id ->
                viewModel.selectAudioTrack(id)
                showAudioDialog = false
            },
            onDisableAudio = {
                viewModel.disableAudio()
                showAudioDialog = false
            },
            onAdjustDelay = { newDelay -> viewModel.setAudioDelay(newDelay) },
            onDismiss = { showAudioDialog = false }
        )
    }

    // Subtitle Selector Dialog (Internal tracks + External subtitle loader + Subtitle Delay Sync)
    if (showSubtitleDialog) {
        SubtitleSelectorDialog(
            subtitleTracks = uiState.subtitleTracks,
            selectedTrackId = uiState.selectedSubtitleTrackId,
            currentDelaySec = uiState.subtitleDelaySeconds,
            onSelectTrack = { id ->
                viewModel.selectSubtitleTrack(id)
                showSubtitleDialog = false
            },
            onDisableSubtitles = {
                viewModel.disableSubtitles()
                showSubtitleDialog = false
            },
            onLoadExternalSubtitle = {
                openSubtitleLauncher.launch(arrayOf("*/*"))
                showSubtitleDialog = false
            },
            onAdjustDelay = { newDelay -> viewModel.setSubtitleDelay(newDelay) },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    // Playback Speed Dialog (0.25x .. 2.0x)
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { speed ->
                viewModel.setPlaybackSpeed(speed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    // Aspect Ratio / Resize Mode Dialog
    if (showResizeDialog) {
        ResizeModeDialog(
            currentMode = uiState.resizeMode,
            onSelectMode = { mode ->
                viewModel.setResizeMode(mode)
                showResizeDialog = false
            },
            onDismiss = { showResizeDialog = false }
        )
    }

    // Media Technical Info Dialog
    if (showMediaInfoDialog) {
        MediaInfoDialog(
            info = uiState.mediaInfo,
            onDismiss = { showMediaInfoDialog = false }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMinutes = sleepTimerRemainingMinutes,
            onSetTimerMinutes = { minutes ->
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
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    // A-B Repeat Loop Dialog
    if (showABRepeatDialog) {
        ABRepeatDialog(
            currentPositionSec = uiState.positionSeconds,
            pointA = uiState.abRepeatPointA,
            pointB = uiState.abRepeatPointB,
            onSetPointA = { viewModel.setABRepeatPointA() },
            onSetPointB = { viewModel.setABRepeatPointB() },
            onClear = { viewModel.clearABRepeat() },
            onDismiss = { showABRepeatDialog = false }
        )
    }
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
    onOpenFileClick: () -> Unit,
    onOpenAudioDialog: () -> Unit,
    onOpenSubtitleDialog: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenResizeDialog: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onOpenMediaInfoDialog: () -> Unit,
    onOpenSleepTimerDialog: () -> Unit,
    onOpenABRepeatDialog: () -> Unit,
    onToggleNightModeAudio: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onToggleRepeatOne: () -> Unit,
    onSelectDecoderMode: (DecoderMode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekRelative: (Double) -> Unit,
    seekIntervalSeconds: Double = 10.0,
    onSliderScrubChange: (Double) -> Unit,
    onSliderScrubEnd: () -> Unit
) {
    var decoderMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
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
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
                    // In Landscape: Full cinema buttons row (Decoder mode pill, Audio, Subtitle, Aspect, PiP, More)
                    // In Portrait: Clean, spacious 3-button cluster (Tracks, PiP, More) so nothing collides with selfie camera!
                    if (isLandscape) {
                        // Decoder Mode Dropdown Chip
                        Box {
                            Surface(
                                onClick = { decoderMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x2AFFFFFF),
                                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Memory,
                                        contentDescription = "Decoder mode",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = uiState.decoderMode.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = decoderMenuExpanded,
                                onDismissRequest = { decoderMenuExpanded = false }
                            ) {
                                DecoderMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${mode.label} (${mode.hwdecValue})",
                                                fontWeight = if (mode == uiState.decoderMode) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            decoderMenuExpanded = false
                                            onSelectDecoderMode(mode)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Audio Track Quick Button
                        IconButton(
                            onClick = onOpenAudioDialog,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = "Audio Tracks",
                                modifier = Modifier.size(20.dp),
                                tint = if (uiState.audioTracks.size > 1) Color(0xFF00E5FF) else Color.White
                            )
                        }

                        // Subtitle Track Quick Button
                        IconButton(
                            onClick = onOpenSubtitleDialog,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = "Subtitles",
                                modifier = Modifier.size(20.dp),
                                tint = if (uiState.selectedSubtitleTrackId != null) Color(0xFF00E5FF) else Color.White
                            )
                        }

                        // Aspect Ratio Quick Cycle
                        IconButton(
                            onClick = onCycleResizeMode,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }

                        // Screen Auto-Rotation Mode Toggle
                        IconButton(
                            onClick = onCycleOrientation,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = when (orientationMode) {
                                    OrientationMode.LANDSCAPE -> Icons.Rounded.StayCurrentLandscape
                                    OrientationMode.PORTRAIT -> Icons.Rounded.StayCurrentPortrait
                                    OrientationMode.SENSOR -> Icons.Rounded.ScreenRotation
                                },
                                contentDescription = orientationMode.label,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    } else {
                        // PORTRAIT: Dedicated Audio & Subtitle track toggle
                        IconButton(
                            onClick = onOpenAudioDialog,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Audiotrack,
                                contentDescription = "Audio & Tracks",
                                modifier = Modifier.size(22.dp),
                                tint = if (uiState.audioTracks.size > 1) Color(0xFF00E5FF) else Color.White
                            )
                        }

                        IconButton(
                            onClick = onOpenSubtitleDialog,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = "Subtitles",
                                modifier = Modifier.size(22.dp),
                                tint = if (uiState.selectedSubtitleTrackId != null) Color(0xFF00E5FF) else Color.White
                            )
                        }
                    }

                    // Picture-in-Picture
                    if (onEnterPip != null) {
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = "Enter PiP",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // More Options Menu (3 Dots)
                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More Options",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                text = { Text("Aspect Ratio (${uiState.resizeMode.label})") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenResizeDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                text = { Text("Playback Speed (${uiState.playbackSpeed}x)") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenSpeedDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Hearing,
                                        contentDescription = null,
                                        tint = if (uiState.isNightModeAudio) Color(0xFF00E676) else Color(0xFFA0A0B0)
                                    )
                                },
                                text = {
                                    Text(
                                        if (uiState.isNightModeAudio) "Audio Night Mode: ON" else "Audio Night Mode: OFF",
                                        fontWeight = if (uiState.isNightModeAudio) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onToggleNightModeAudio()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Repeat,
                                        contentDescription = null,
                                        tint = if (uiState.abRepeatPointA != null) Color(0xFF00E5FF) else Color(0xFFA0A0B0)
                                    )
                                },
                                text = {
                                    Text(
                                        if (uiState.abRepeatPointA != null) "A-B Repeat (Active)" else "A-B Repeat Looper",
                                        fontWeight = if (uiState.abRepeatPointA != null) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenABRepeatDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color(0xFFFFB300)) },
                                text = { Text("Capture Video Frame") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onCaptureScreenshot()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color(0xFFFFD54F)) },
                                text = { Text("Sleep Timer") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenSleepTimerDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFFF5252)) },
                                text = { Text("Lock Screen (Child Lock)") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onLockScreen()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                text = { Text("Media Technical Information") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenMediaInfoDialog()
                                }
                            )
                            HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                                text = { Text("Open Local Video File") },
                                onClick = {
                                    moreMenuExpanded = false
                                    onOpenFileClick()
                                }
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
                    // Seekbar & Time labels row
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
                            fontWeight = FontWeight.SemiBold
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
                            modifier = Modifier.clickable { onToggleTimeDisplay() }
                        )
                    }

                    // Playback Control Buttons
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        // Left Pill: Playback Speed
                        Surface(
                            onClick = onOpenSpeedDialog,
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

                        // Center Hero Cluster: Prev, -10s, Hero Play/Pause, +10s, Next
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Previous Video
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

                            // Rewind
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

                            // HERO PLAY/PAUSE BUTTON (62dp glowing cyan capsule)
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

                            // Forward
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

                            // Next Video
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

                        // Right Controls: Resize Mode, Lock Screen, Repeat Mode
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                            Spacer(modifier = Modifier.width(2.dp))

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
                        }
                    }
                }
            }
        }

        // In Portrait mode: show a floating "Rotate to Landscape" pill above bottom controls when playing widescreen video!
        if (!isLandscape && uiState.videoWidth > uiState.videoHeight && isVisible) {
            Surface(
                onClick = onCycleOrientation,
                shape = RoundedCornerShape(20.dp),
                color = Color(0xD90D1322),
                border = BorderStroke(1.dp, Color(0x5500E5FF)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                    .padding(bottom = 120.dp, end = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ScreenRotation,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rotate",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// DIALOG IMPLEMENTATIONS
// ==========================================

@Composable
private fun AudioTrackSelectorDialog(
    audioTracks: List<MediaTrack>,
    selectedTrackId: Int?,
    currentDelaySec: Double,
    onSelectTrack: (Int) -> Unit,
    onDisableAudio: () -> Unit,
    onAdjustDelay: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text(
                text = "Audio Track & Sync",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Mute / Disable Audio option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisableAudio() }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = (selectedTrackId == null),
                        onClick = onDisableAudio,
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mute / Disable Audio (Off)",
                        color = if (selectedTrackId == null) Color(0xFF00E676) else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                if (audioTracks.isEmpty()) {
                    Text(
                        text = "Default Audio Track (No alternate streams)",
                        color = Color(0xFFA0A0B0)
                    )
                } else {
                    audioTracks.forEach { track ->
                        val isSelected = (track.id == selectedTrackId) || track.isSelected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTrack(track.id) }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectTrack(track.id) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = track.displayName,
                                    color = if (isSelected) Color(0xFF00E676) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                track.codec?.let { codec ->
                                    val rate = if (track.sampleRate > 0) " • ${track.sampleRate / 1000} kHz" else ""
                                    Text(
                                        text = "${codec.uppercase(Locale.US)}$rate",
                                        color = Color(0xFF8E8E9E),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 12.dp))

                // Audio Delay Sync Adjuster (-5.0s .. +5.0s in 0.1s steps)
                Text(
                    text = "Audio Delay Sync",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = { onAdjustDelay((currentDelaySec - 0.1).coerceAtLeast(-5.0)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("-0.1s")
                    }

                    Text(
                        text = String.format(Locale.US, "%.1fs", currentDelaySec),
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )

                    FilledTonalButton(
                        onClick = { onAdjustDelay((currentDelaySec + 0.1).coerceAtMost(5.0)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("+0.1s")
                    }

                    TextButton(onClick = { onAdjustDelay(0.0) }) {
                        Text("Reset", color = Color(0xFFFF5252))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
private fun SubtitleSelectorDialog(
    subtitleTracks: List<MediaTrack>,
    selectedTrackId: Int?,
    currentDelaySec: Double,
    onSelectTrack: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onLoadExternalSubtitle: () -> Unit,
    onAdjustDelay: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text(
                text = "Subtitle Selector & Sync",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Disable subtitles option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisableSubtitles() }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = (selectedTrackId == null),
                        onClick = onDisableSubtitles,
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Disable Subtitles (Off)",
                        color = if (selectedTrackId == null) Color(0xFF00E676) else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                // Internal subtitles
                if (subtitleTracks.isNotEmpty()) {
                    Text(
                        text = "Embedded Subtitle Tracks",
                        color = Color(0xFFA0A0B0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    subtitleTracks.forEach { track ->
                        val isSelected = (track.id == selectedTrackId) || track.isSelected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTrack(track.id) }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectTrack(track.id) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = track.displayName,
                                    color = if (isSelected) Color(0xFF00E676) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                track.codec?.let { codec ->
                                    Text(
                                        text = codec.uppercase(Locale.US),
                                        color = Color(0xFF8E8E9E),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))
                }

                // Load External Subtitle File (.srt, .ass, .vtt)
                OutlinedButton(
                    onClick = onLoadExternalSubtitle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load External Subtitle (.srt / .ass / .vtt)")
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 12.dp))

                // Subtitle Delay Sync Adjuster (-5.0s .. +5.0s)
                Text(
                    text = "Subtitle Delay Sync",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = { onAdjustDelay((currentDelaySec - 0.1).coerceAtLeast(-5.0)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("-0.1s")
                    }

                    Text(
                        text = String.format(Locale.US, "%.1fs", currentDelaySec),
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )

                    FilledTonalButton(
                        onClick = { onAdjustDelay((currentDelaySec + 0.1).coerceAtMost(5.0)) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("+0.1s")
                    }

                    TextButton(onClick = { onAdjustDelay(0.0) }) {
                        Text("Reset", color = Color(0xFFFF5252))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Double,
    onSelectSpeed: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val presets = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume click to prevent dismissing */ }
                    )
                    .then(
                        if (isLandscape) {
                            Modifier
                                .fillMaxHeight()
                                .width(280.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                        }
                    ),
                shape = if (isLandscape) {
                    RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                } else {
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                },
                color = Color(0xF5161820),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isLandscape) 16.dp else 24.dp)
                ) {
                    // Portrait drag handle
                    if (!isLandscape) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x44FFFFFF))
                            )
                        }
                    }

                    // Header Row: Title & Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = if (isLandscape) 14.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Playback speed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x22FFFFFF))

                    // YouTube-style Speed Items List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        presets.forEach { speed ->
                            val isSelected = abs(currentSpeed - speed) < 0.02
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSpeed(speed)
                                        onDismiss()
                                    }
                                    .background(
                                        if (isSelected) Color(0x1F00E5FF) else Color.Transparent
                                    )
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left checkmark indicator
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = if (speed == 1.0) "Normal" else "${speed}x",
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
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
private fun ResizeModeDialog(
    currentMode: ResizeMode,
    onSelectMode: (ResizeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text("Video Aspect Ratio", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ResizeMode.entries.forEach { mode ->
                    val isSelected = (mode == currentMode)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.label,
                            color = if (isSelected) Color(0xFF00E676) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
private fun MediaInfoDialog(
    info: MediaInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text("Media Information", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
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
private fun SleepTimerDialog(
    remainingMinutes: Int,
    onSetTimerMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text("Sleep Timer", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (remainingMinutes > 0) {
                    Text(
                        text = "Active: Pausing in $remainingMinutes minute(s)",
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                presets.forEach { minutes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimerMinutes(minutes) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "$minutes minutes",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                if (remainingMinutes > 0) {
                    HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = { onSetTimerMinutes(0) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn Off Timer", color = Color(0xFFFF5252))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
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

@Composable
private fun ABRepeatDialog(
    currentPositionSec: Double,
    pointA: Double?,
    pointB: Double?,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text("A-B Repeat Looper", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current playback: ${formatTimestamp(currentPositionSec)}",
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Point A row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Point A (Loop Start)", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text(
                            text = if (pointA != null) formatTimestamp(pointA) else "Not set",
                            color = if (pointA != null) Color(0xFF00E676) else Color(0xFFA0A0B0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    FilledTonalButton(
                        onClick = onSetPointA,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x3300E5FF))
                    ) {
                        Text("Set A Here", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 10.dp))

                // Point B row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Point B (Loop End)", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text(
                            text = if (pointB != null) formatTimestamp(pointB) else "Not set",
                            color = if (pointB != null) Color(0xFF00E676) else Color(0xFFA0A0B0),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    FilledTonalButton(
                        onClick = onSetPointB,
                        enabled = pointA != null && currentPositionSec > pointA,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x3300E5FF))
                    ) {
                        Text("Set B Here", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }

                if (pointA != null && pointB != null && pointB > pointA) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x2200E5FF),
                        border = BorderStroke(1.dp, Color(0x4400E5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Looping [${formatTimestamp(pointA)} ⇄ ${formatTimestamp(pointB)}]",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (pointA != null || pointB != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Loop Points", color = Color(0xFFFF5252))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

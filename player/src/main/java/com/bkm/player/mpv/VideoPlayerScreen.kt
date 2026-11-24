package com.bkm.player.mpv

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ============================================================================
// MATERIAL YOU 3 DESIGN SYSTEM & DYNAMIC COLOR PALETTE
// ============================================================================
private val MxBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

private val MxRed = Color(0xFFFF3B30)              // Volume boost (>100%) & mute alert
private val MxWhite = Color(0xFFFFFFFF)            // Sharp white icons and text for video overlay
private val MxSubtext: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

private val MxDialogBg: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)

private val MxHudBg = Color(0xDC000000)            // Video player gesture HUD overlay background

private val MxPillBg: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.50f)

private val MxMenuBg: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)

/**
 * Screen orientation mode for video playback:
 * - [SENSOR]: Sensor auto-rotation
 * - [LANDSCAPE]: Force landscape orientation
 * - [PORTRAIT]: Force portrait orientation
 */
enum class OrientationMode(val label: String) {
    SENSOR("Auto Rotation"),
    LANDSCAPE("Landscape Lock"),
    PORTRAIT("Portrait Lock");

    fun next(): OrientationMode = when (this) {
        SENSOR -> LANDSCAPE
        LANDSCAPE -> PORTRAIT
        PORTRAIT -> SENSOR
    }
}

/**
 * Repeat modes matching MX Player:
 * - [OFF]: Repeat Off
 * - [ONE]: Repeat Single Video
 * - [ALL]: Repeat All Playlist
 */
enum class RepeatMode(val label: String) {
    OFF("Repeat Off"),
    ONE("Repeat One"),
    ALL("Repeat All");

    fun next(): RepeatMode = when (this) {
        OFF -> ONE
        ONE -> ALL
        ALL -> OFF
    }
}

/**
 * MX Player Video Player Screen:
 * Authentic, pixel-perfect replication of MX Player:
 * - Iconic top bar: Back, Title, HW/HW+/SW Decoder Selector button, Audio track, Subtitle, Screen Rotation, PiP, 3-dots Menu.
 * - Iconic 3-dot overflow menu: Cascading categorized hierarchy:
 *   - Play (Speed, Repeat, A-B Repeat, Sleep Timer)
 *   - Audio (Audio Track, Audio Delay, Speech Clarifier, Mute)
 *   - Subtitle (Subtitle Track, Open Subtitle File, Synchronization, Text Size & Style)
 *   - Display (Aspect Ratio, Screen Orientation, Video Zoom, Screen Lock)
 *   - Tools (Properties, Screenshot, Open Video)
 * - Iconic bottom bar:
 *   - Full-width seekbar with MX Player Blue active progress, buffer cache indicator, and clean time labels.
 *   - Transport row: Child Lock (left), Skip Prev, Rewind 10s, Hero Play/Pause, Forward 10s, Skip Next (center), Speed, Repeat, Aspect Ratio (right).
 * - Exact MX Player Gestures & HUDs:
 *   - Horizontal swipe: Large time HUD `00:14:22 [+00:15]` with mini progress bar.
 *   - Vertical left: Brightness HUD with sun icon & level bar.
 *   - Vertical right: Volume HUD with speaker icon, level bar, and SW Volume Boost (up to 200%).
 *   - Double-tap left/right ±10s seek ripple feedback.
 *   - Multi-touch pinch-to-zoom (1.0x .. 5.0x) with Zoom % HUD.
 *   - Screen Child Lock: Blocks touches and displays top-left floating lock button.
 * - Native MX Player Dialogs for Decoder, Audio, Subtitles, Appearance, Speed, Aspect ratio, Sleep timer, A-B repeat, and Properties.
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
    backgroundPlaybackEnabled: Boolean = true,
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

    // Dialog visibility states (MX Player Dialogs)
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showAudioTracksDialog by remember { mutableStateOf(false) }
    var showAudioDelayDialog by remember { mutableStateOf(false) }
    var showSubtitleTracksDialog by remember { mutableStateOf(false) }
    var showSubtitleDelayDialog by remember { mutableStateOf(false) }
    var showSubtitleAppearanceDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAbRepeatDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    // Screen Touch Lock (Child Lock)
    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }
    var unlockTapCount by remember { mutableIntStateOf(0) }

    // Screen Orientation Mode
    var orientationMode by remember { mutableStateOf(OrientationMode.SENSOR) }

    // Repeat Mode (Off, One, All)
    var repeatMode by remember { mutableStateOf(if (uiState.isRepeatOne) RepeatMode.ONE else RepeatMode.OFF) }

    // Back handling
    BackHandler(enabled = onBack != null && !isInPipMode) {
        when {
            showDecoderDialog -> showDecoderDialog = false
            showAudioTracksDialog -> showAudioTracksDialog = false
            showAudioDelayDialog -> showAudioDelayDialog = false
            showSubtitleTracksDialog -> showSubtitleTracksDialog = false
            showSubtitleDelayDialog -> showSubtitleDelayDialog = false
            showSubtitleAppearanceDialog -> showSubtitleAppearanceDialog = false
            showSpeedDialog -> showSpeedDialog = false
            showAspectRatioDialog -> showAspectRatioDialog = false
            showSleepTimerDialog -> showSleepTimerDialog = false
            showAbRepeatDialog -> showAbRepeatDialog = false
            showPropertiesDialog -> showPropertiesDialog = false
            isScreenLocked -> showUnlockPrompt = true
            else -> onBack?.invoke()
        }
    }

    // Time Display Toggle: Elapsed/Total vs Elapsed/Remaining
    var showRemainingTime by remember { mutableStateOf(false) }

    // Aspect Ratio HUD Toast overlay
    var showAspectHud by remember { mutableStateOf(false) }
    var aspectHudText by remember { mutableStateOf("") }

    // Sleep Timer Remaining time from ViewModel
    val sleepTimerRemainingMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()

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

    // Handle playlist EOF based on RepeatMode
    LaunchedEffect(repeatMode, hasNext, onNext) {
        viewModel.onEndOfFileReached = {
            when (repeatMode) {
                RepeatMode.ONE -> {
                    viewModel.seekTo(0.0)
                    if (!viewModel.uiState.value.isPlaying) {
                        viewModel.togglePlayPause()
                    }
                }
                RepeatMode.ALL -> {
                    if (hasNext) {
                        onNext?.invoke()
                    } else {
                        onNext?.invoke()
                    }
                }
                RepeatMode.OFF -> {
                    if (hasNext) {
                        onNext?.invoke()
                    }
                }
            }
        }
    }

    // Handle initialUri playback on startup
    LaunchedEffect(initialUri, initialTitle) {
        if (initialUri != null) {
            viewModel.openDocument(context, initialUri, initialTitle, initialPositionMs)
        }
    }

    // Immersive Fullscreen Mode
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
            if (window != null && activity?.isInPictureInPictureMode != true) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, isInPipMode, backgroundPlaybackEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                val isPip = isInPipMode || (activity?.isInPictureInPictureMode == true)
                if (!isPip && !backgroundPlaybackEnabled) {
                    viewModel.pausePlayback()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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

    val hasAnyDialogOpen = showDecoderDialog || showAudioTracksDialog || showAudioDelayDialog ||
            showSubtitleTracksDialog || showSubtitleDelayDialog || showSubtitleAppearanceDialog ||
            showSpeedDialog || showAspectRatioDialog || showSleepTimerDialog || showAbRepeatDialog ||
            showPropertiesDialog

    // Auto-hide controls after 6 seconds of inactivity when playing (suppressed during scrubbing)
    LaunchedEffect(
        uiState.controlsVisible,
        uiState.isPlaying,
        uiState.isScrubbing,
        isScreenLocked,
        hasAnyDialogOpen
    ) {
        if (uiState.controlsVisible && uiState.isPlaying && !uiState.isScrubbing && !isScreenLocked && !hasAnyDialogOpen) {
            delay(6000L)
            viewModel.setControlsVisible(false)
        }
    }

    // Auto-hide unlock prompt after 3.5 seconds and reset tap counter
    LaunchedEffect(showUnlockPrompt) {
        if (showUnlockPrompt) {
            delay(3500L)
            showUnlockPrompt = false
            unlockTapCount = 0
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

    val onCycleResizeMode: () -> Unit = {
        viewModel.cycleResizeMode()
        val label = when (viewModel.uiState.value.resizeMode) {
            ResizeMode.FIT -> "Fit to screen"
            ResizeMode.STRETCH -> "Stretch"
            ResizeMode.CROP -> "Crop"
            ResizeMode.ORIGINAL_100 -> "100%"
            ResizeMode.SIXTEEN_NINE -> "16:9"
            ResizeMode.FOUR_THREE -> "4:3"
        }
        aspectHudText = label
        showAspectHud = true
    }

    val onCycleRepeatMode: () -> Unit = {
        val next = repeatMode.next()
        repeatMode = next
        when (next) {
            RepeatMode.ONE -> viewModel.engine.setLoopFile(true)
            else -> viewModel.engine.setLoopFile(false)
        }
        Toast.makeText(context, next.label, Toast.LENGTH_SHORT).show()
    }

    val onCycleOrientation: () -> Unit = {
        orientationMode = orientationMode.next()
        Toast.makeText(context, orientationMode.label, Toast.LENGTH_SHORT).show()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val onToggleFullscreen: () -> Unit = {
        if (isLandscape) {
            orientationMode = OrientationMode.PORTRAIT
            Toast.makeText(context, "Portrait Mode", Toast.LENGTH_SHORT).show()
        } else {
            orientationMode = OrientationMode.LANDSCAPE
            Toast.makeText(context, "Landscape Fullscreen", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. OpenGL ES Video Surface Canvas
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
            // 2. Gesture Engine (Tap, Double-Tap ±10s, Hold 2x, Pinch Zoom, Scrubbing, Brightness, Volume)
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

                                    if (!isScreenLocked) {
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
                                    }
                                    active[0].consume()
                                    active[1].consume()
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

                                        if (!isScreenLocked) {
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
                                        } else {
                                            showUnlockPrompt = true
                                        }
                                    }

                                    if (isDrag) {
                                        change.consume()
                                        if (!isScreenLocked) {
                                            if (dragDirection == 1) {
                                                val currentDx = change.position.x - seekStartX
                                                val normalizedX = (currentDx / size.width.toFloat()).coerceIn(-1.0f, 1.0f)
                                                val duration = uiState.durationSeconds
                                                val maxSpan = when {
                                                    duration <= 60.0 -> duration.coerceAtLeast(10.0)
                                                    duration <= 600.0 -> 60.0 + (duration - 60.0) * 0.35
                                                    else -> (249.0 + (duration - 600.0) * 0.12).coerceAtMost(1200.0)
                                                }
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
                                    }
                                } else {
                                    holdSpeedJob?.cancel()
                                    if (isHoldingForSpeed) {
                                        isHoldingForSpeed = false
                                        viewModel.stopFastForward()
                                    } else if (!isDrag) {
                                        val now = System.currentTimeMillis()
                                        if (!isScreenLocked && now - lastTapTime < 350L && abs(startX - lastTapX) < 140f) {
                                            // Double tap (disabled when screen is locked)
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
                                                unlockTapCount = (unlockTapCount + 1).coerceAtMost(3)
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

            // 3. Double-Tap Seek Ripples (MX Player circle feedback)
            if (leftRippleAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = leftRippleAlpha.value * 0.75f),
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = null,
                                tint = MxWhite,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "-${doubleTapSeekSeconds}s",
                                color = MxWhite,
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
                        color = Color.Black.copy(alpha = rightRippleAlpha.value * 0.75f),
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = null,
                                tint = MxWhite,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "+${doubleTapSeekSeconds}s",
                                color = MxWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. MX Player Seek HUD (Large time + [delta] + progress indicator)
            if (uiState.isScrubbing) {
                Surface(
                    color = MxHudBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        val showHours = uiState.durationSeconds >= 3600.0
                        Text(
                            text = formatTimestamp(uiState.scrubPreviewSeconds, forceHours = showHours),
                            color = MxWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sign = if (uiState.scrubDeltaSeconds >= 0) "+" else "-"
                        Text(
                            text = "[$sign${formatDelta(abs(uiState.scrubDeltaSeconds))}]",
                            color = MxBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val scrubFraction = (uiState.scrubPreviewSeconds / uiState.durationSeconds.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x40FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = scrubFraction)
                                    .background(MxBlue)
                            )
                        }
                    }
                }
            }

            // 5. MX Player Vertical Gesture HUD (Brightness / Volume + SW Boost)
            uiState.verticalHud?.let { hud ->
                Surface(
                    color = MxHudBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        when (hud) {
                            is VerticalGestureHud.Brightness -> {
                                Icon(
                                    imageVector = Icons.Rounded.Brightness6,
                                    contentDescription = null,
                                    tint = MxWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Brightness",
                                            color = MxSubtext,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${hud.percent}%",
                                            color = MxWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = (hud.percent / 100f).coerceIn(0f, 1f))
                                                .background(MxWhite)
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
                                    tint = if (hud.isSoftwareBoosted) MxRed else MxWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (hud.isSoftwareBoosted) "Volume (Boost)" else "Volume",
                                            color = if (hud.isSoftwareBoosted) MxRed else MxSubtext,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = (hud.percent * 15 / 100).coerceAtLeast(0).toString(),
                                            color = if (hud.isSoftwareBoosted) MxRed else MxWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x33FFFFFF))
                                    ) {
                                        val fraction = (hud.percent / 200f).coerceIn(0f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = fraction)
                                                .background(if (hud.isSoftwareBoosted) MxRed else MxBlue)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Buffering Indicator
            if ((uiState.isBuffering || !uiState.hasMediaLoaded) && !uiState.isScrubbing && uiState.errorMessage == null) {
                CircularProgressIndicator(
                    color = MxBlue,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    strokeWidth = 3.5.dp
                )
            }

            // 7. Fast Forward 2.0x HUD Pill
            if (uiState.isFastForwarding) {
                Surface(
                    color = MxHudBg,
                    shape = RoundedCornerShape(20.dp),
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = MxBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2.0x Speed",
                            color = MxWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 8. Aspect Ratio Toast Pill
            if (showAspectHud) {
                Surface(
                    color = MxHudBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(
                        text = aspectHudText,
                        color = MxWhite,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            // 9. Zoom HUD Toast Pill
            if (showZoomHud) {
                Surface(
                    color = MxHudBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${(zoomScale * 100f).roundToInt()}%",
                        color = MxWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            // 10. (Child Lock moved to top of Z-stack below controls overlay)

            // 11. BKM Player Top & Bottom Controls Overlays
            BkmPlayerControlsOverlay(
                isVisible = uiState.controlsVisible && !uiState.isScrubbing && !isScreenLocked,
                uiState = uiState,
                orientationMode = orientationMode,
                repeatMode = repeatMode,
                sleepTimerMinutes = sleepTimerRemainingMinutes,
                showRemainingTime = showRemainingTime,
                onToggleTimeDisplay = { showRemainingTime = !showRemainingTime },
                onCycleOrientation = onCycleOrientation,
                onToggleFullscreen = onToggleFullscreen,
                onLockScreen = {
                    isScreenLocked = true
                    showUnlockPrompt = true
                    viewModel.setControlsVisible(false)
                    Toast.makeText(context, "Screen locked", Toast.LENGTH_SHORT).show()
                },
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onPrevious = onPrevious,
                onNext = onNext,
                onBack = onBack,
                onEnterPip = onEnterPip,
                onOpenDecoderDialog = { showDecoderDialog = true },
                onOpenAudioDialog = {
                    viewModel.refreshTracks()
                    showAudioTracksDialog = true
                },
                onOpenAudioDelayDialog = { showAudioDelayDialog = true },
                onToggleNightMode = {
                    viewModel.toggleNightModeAudio()
                    Toast.makeText(
                        context,
                        if (viewModel.uiState.value.isNightModeAudio) "Speech Clarifier On" else "Speech Clarifier Off",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onOpenSubtitleDialog = {
                    viewModel.refreshTracks()
                    showSubtitleTracksDialog = true
                },
                onOpenSubtitleDelayDialog = { showSubtitleDelayDialog = true },
                onOpenSubtitleAppearanceDialog = { showSubtitleAppearanceDialog = true },
                onOpenExternalSubtitle = {
                    openSubtitleLauncher.launch(arrayOf("*/*"))
                },
                onOpenSpeedDialog = { showSpeedDialog = true },
                onCycleResizeMode = onCycleResizeMode,
                onOpenSleepTimerDialog = { showSleepTimerDialog = true },
                onOpenAbRepeatDialog = { showAbRepeatDialog = true },
                onOpenPropertiesDialog = {
                    viewModel.refreshMediaInfo()
                    showPropertiesDialog = true
                },
                onCaptureScreenshot = {
                    viewModel.captureScreenshot(context) { _, success ->
                        Toast.makeText(
                            context,
                            if (success) "Screenshot saved" else "Failed to save screenshot",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onOpenFile = {
                    openVideoLauncher.launch(arrayOf("video/*"))
                },
                onResetZoom = {
                    zoomScale = 1.0f
                    panOffset = Offset.Zero
                    Toast.makeText(context, "Zoom reset to 100%", Toast.LENGTH_SHORT).show()
                },
                onToggleRepeat = onCycleRepeatMode,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekRelative = viewModel::seekRelative,
                seekIntervalSeconds = doubleTapSeekSeconds.toDouble(),
                onSliderScrubChange = viewModel::onSliderScrubChange,
                onSliderScrubEnd = viewModel::onHorizontalScrubEnd
            )

            // 12. Empty State Prompt
            if (initialUri == null && !uiState.hasMediaLoaded) {
                MxEmptyPlayerPrompt(
                    errorMessage = uiState.errorMessage,
                    onOpenFileClick = { openVideoLauncher.launch(arrayOf("video/*")) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 13. MX Player Child Lock Floating Button & Notification (Top of Z-stack)
            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            )
                        )
                        .padding(start = 18.dp, top = 16.dp, end = 18.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    // Floating circular unlock button with blue halo on tap
                    Surface(
                        onClick = {
                            isScreenLocked = false
                            showUnlockPrompt = false
                            unlockTapCount = 0
                            viewModel.setControlsVisible(true)
                            Toast.makeText(context, "Screen unlocked", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = Color(0xCC111111),
                        border = BorderStroke(1.5.dp, if (showUnlockPrompt) MxBlue else Color(0x88FFFFFF)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Unlock Screen",
                                tint = if (showUnlockPrompt) MxBlue else MxWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Prompt message pill at top center
                    AnimatedVisibility(
                        visible = showUnlockPrompt,
                        enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it },
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Surface(
                            color = Color(0xEE1A1A1A),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MxBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Screen is locked. Tap lock icon to unlock.",
                                    color = MxWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================================================
    // MX PLAYER DIALOGS & OVERFLOW MENUS
    // ========================================================================

    // 1. Decoder Dialog (HW+ / HW / SW)
    if (showDecoderDialog) {
        MxDecoderDialog(
            currentMode = uiState.decoderMode,
            onSelectMode = { mode ->
                viewModel.selectDecoderMode(mode)
                showDecoderDialog = false
                Toast.makeText(context, "Decoder: ${mode.label}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDecoderDialog = false }
        )
    }

    // 2. Audio Tracks Dialog
    if (showAudioTracksDialog) {
        MxAudioTracksDialog(
            uiState = uiState,
            onSelectTrack = { id ->
                viewModel.selectAudioTrack(id)
                showAudioTracksDialog = false
            },
            onDisableAudio = {
                viewModel.disableAudio()
                showAudioTracksDialog = false
            },
            onOpenDelay = {
                showAudioTracksDialog = false
                showAudioDelayDialog = true
            },
            onDismiss = { showAudioTracksDialog = false }
        )
    }

    // 3. Audio Delay Sync Dialog
    if (showAudioDelayDialog) {
        MxAudioDelayDialog(
            currentDelay = uiState.audioDelaySeconds,
            onAdjust = { delta -> viewModel.setAudioDelay(delta) },
            onDismiss = { showAudioDelayDialog = false }
        )
    }

    // 4. Subtitle Tracks Dialog
    if (showSubtitleTracksDialog) {
        MxSubtitleTracksDialog(
            uiState = uiState,
            onSelectTrack = { id ->
                viewModel.selectSubtitleTrack(id)
                showSubtitleTracksDialog = false
            },
            onDisableSubtitles = {
                viewModel.disableSubtitles()
                showSubtitleTracksDialog = false
            },
            onLoadExternalSubtitle = {
                showSubtitleTracksDialog = false
                openSubtitleLauncher.launch(arrayOf("*/*"))
            },
            onOpenDelay = {
                showSubtitleTracksDialog = false
                showSubtitleDelayDialog = true
            },
            onOpenAppearance = {
                showSubtitleTracksDialog = false
                showSubtitleAppearanceDialog = true
            },
            onDismiss = { showSubtitleTracksDialog = false }
        )
    }

    // 5. Subtitle Delay Sync Dialog
    if (showSubtitleDelayDialog) {
        MxSubtitleDelayDialog(
            currentDelay = uiState.subtitleDelaySeconds,
            onAdjust = { delta -> viewModel.setSubtitleDelay(delta) },
            onDismiss = { showSubtitleDelayDialog = false }
        )
    }

    // 6. Subtitle Appearance (Font size & Color) Dialog
    if (showSubtitleAppearanceDialog) {
        MxSubtitleAppearanceDialog(
            currentSize = uiState.subtitleFontSize,
            currentColor = uiState.subtitleColor,
            onSelectSize = { size -> viewModel.setSubtitleFontSize(size) },
            onSelectColor = { color -> viewModel.setSubtitleColor(color) },
            onDismiss = { showSubtitleAppearanceDialog = false }
        )
    }

    // 7. Playback Speed Dialog
    if (showSpeedDialog) {
        MxSpeedDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = { speed ->
                viewModel.setPlaybackSpeed(speed)
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    // 8. Aspect Ratio Dialog
    if (showAspectRatioDialog) {
        MxAspectRatioDialog(
            currentMode = uiState.resizeMode,
            onSelectMode = { mode ->
                viewModel.setResizeMode(mode)
                showAspectRatioDialog = false
                aspectHudText = mode.label
                showAspectHud = true
            },
            onDismiss = { showAspectRatioDialog = false }
        )
    }

    // 9. Sleep Timer Dialog
    if (showSleepTimerDialog) {
        MxSleepTimerDialog(
            currentMinutes = sleepTimerRemainingMinutes,
            onSetMinutes = { minutes ->
                if (minutes > 0) {
                    viewModel.setSleepTimer(minutes) {
                        Toast.makeText(context, "Sleep Timer finished. Video paused.", Toast.LENGTH_LONG).show()
                    }
                    Toast.makeText(context, "Sleep timer set for $minutes min", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setSleepTimer(0)
                    Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                }
                showSleepTimerDialog = false
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    // 10. A-B Repeat Looper Dialog
    if (showAbRepeatDialog) {
        MxAbRepeatDialog(
            uiState = uiState,
            onSetPointA = { viewModel.setABRepeatPointA() },
            onSetPointB = { viewModel.setABRepeatPointB() },
            onClearRepeat = { viewModel.clearABRepeat() },
            onDismiss = { showAbRepeatDialog = false }
        )
    }

    // 11. Media Properties Dialog
    if (showPropertiesDialog) {
        MxPropertiesDialog(
            info = uiState.mediaInfo,
            videoWidth = uiState.videoWidth,
            videoHeight = uiState.videoHeight,
            decoderMode = uiState.decoderMode.label,
            filePath = viewModel.currentLoadedUri?.toString() ?: "",
            onDismiss = { showPropertiesDialog = false }
        )
    }

    // Playback Error Dialog
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            containerColor = MxDialogBg,
            title = {
                Text("Playback Error", color = MxWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(uiState.errorMessage ?: "Unknown playback error", color = MxSubtext)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissError()
                        onBack?.invoke()
                    }
                ) {
                    Text("OK", color = MxBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ============================================================================
// TOP & BOTTOM PLAYER CONTROLS (AUTHENTIC MX PLAYER STYLE)
// ============================================================================

@Composable
private fun BkmPlayerControlsOverlay(
    isVisible: Boolean,
    uiState: PlayerUiState,
    orientationMode: OrientationMode,
    repeatMode: RepeatMode,
    sleepTimerMinutes: Int,
    showRemainingTime: Boolean,
    onToggleTimeDisplay: () -> Unit,
    onCycleOrientation: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onLockScreen: () -> Unit,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onEnterPip: (() -> Unit)? = null,
    onOpenDecoderDialog: () -> Unit,
    onOpenAudioDialog: () -> Unit,
    onOpenAudioDelayDialog: () -> Unit,
    onToggleNightMode: () -> Unit,
    onOpenSubtitleDialog: () -> Unit,
    onOpenSubtitleDelayDialog: () -> Unit,
    onOpenSubtitleAppearanceDialog: () -> Unit,
    onOpenExternalSubtitle: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onOpenSleepTimerDialog: () -> Unit,
    onOpenAbRepeatDialog: () -> Unit,
    onOpenPropertiesDialog: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onOpenFile: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleRepeat: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekRelative: (Double) -> Unit,
    seekIntervalSeconds: Double = 10.0,
    onSliderScrubChange: (Double) -> Unit,
    onSliderScrubEnd: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showMoreDropdown by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(tween(180)) { -it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(180)) { -it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xD9000000),
                                Color(0x99000000),
                                Color(0x40000000),
                                Color.Transparent
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                        )
                    )
                    .padding(
                        start = if (isLandscape) 14.dp else 8.dp,
                        end = if (isLandscape) 14.dp else 8.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back button
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MxWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Video Title (Spacious, prominent, legible)
                    Text(
                        text = uiState.mediaTitle.ifBlank { "MX Player" },
                        color = MxWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    // HW/HW+/SW Decoder Button (The classic MX Player decoder badge)
                    Surface(
                        onClick = onOpenDecoderDialog,
                        shape = RoundedCornerShape(4.dp),
                        color = MxPillBg,
                        border = BorderStroke(1.dp, Color(0x80FFFFFF)),
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = uiState.decoderMode.label,
                                color = MxWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Audio Track Selector
                    IconButton(
                        onClick = onOpenAudioDialog,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Audiotrack,
                            contentDescription = "Audio Tracks",
                            tint = if (uiState.audioTracks.size > 1 || uiState.isNightModeAudio) MxBlue else MxWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Subtitle Selector
                    IconButton(
                        onClick = onOpenSubtitleDialog,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Subtitles,
                            contentDescription = "Subtitles",
                            tint = if (uiState.selectedSubtitleTrackId != null) MxBlue else MxWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // In landscape mode or wide screens, keep the screen rotation & PiP icons in top bar
                    if (isLandscape || configuration.screenWidthDp >= 440) {
                        IconButton(
                            onClick = onCycleOrientation,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = when (orientationMode) {
                                    OrientationMode.LANDSCAPE -> Icons.Rounded.StayCurrentLandscape
                                    OrientationMode.PORTRAIT -> Icons.Rounded.StayCurrentPortrait
                                    OrientationMode.SENSOR -> Icons.Rounded.ScreenRotation
                                },
                                contentDescription = "Orientation",
                                tint = MxWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (onEnterPip != null) {
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = "Enter PiP",
                                    tint = MxWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 3-DOTS OVERFLOW MENU ANCHOR (Real MX Player Dropdown Menu)
                    Box {
                        IconButton(
                            onClick = { showMoreDropdown = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More Options",
                                tint = MxWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Streamlined 3-Dot Overflow Menu (Settings not available on the player screen)
                        BkmPlayerOverflowMenu(
                            expanded = showMoreDropdown,
                            onDismissRequest = { showMoreDropdown = false },
                            uiState = uiState,
                            sleepTimerMinutes = sleepTimerMinutes,
                            onOpenSleepTimerDialog = {
                                showMoreDropdown = false
                                onOpenSleepTimerDialog()
                            },
                            onOpenAbRepeatDialog = {
                                showMoreDropdown = false
                                onOpenAbRepeatDialog()
                            },
                            onOpenAudioDelayDialog = {
                                showMoreDropdown = false
                                onOpenAudioDelayDialog()
                            },
                            onToggleNightMode = onToggleNightMode,
                            onOpenSubtitleDelayDialog = {
                                showMoreDropdown = false
                                onOpenSubtitleDelayDialog()
                            },
                            onOpenSubtitleAppearanceDialog = {
                                showMoreDropdown = false
                                onOpenSubtitleAppearanceDialog()
                            },
                            onOpenExternalSubtitle = {
                                showMoreDropdown = false
                                onOpenExternalSubtitle()
                            },
                            onResetZoom = {
                                showMoreDropdown = false
                                onResetZoom()
                            },
                            onOpenPropertiesDialog = {
                                showMoreDropdown = false
                                onOpenPropertiesDialog()
                            },
                            onCaptureScreenshot = {
                                showMoreDropdown = false
                                onCaptureScreenshot()
                            },
                            onOpenFile = {
                                showMoreDropdown = false
                                onOpenFile()
                            }
                        )
                    }
                }
            }
        }

        // BOTTOM BAR
        if (uiState.hasMediaLoaded) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(tween(180)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x40000000),
                                    Color(0x99000000),
                                    Color(0xD9000000)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                            )
                        )
                        .padding(
                            start = if (isLandscape) 18.dp else 14.dp,
                            end = if (isLandscape) 18.dp else 14.dp,
                            top = 4.dp,
                            bottom = if (isLandscape) 6.dp else 6.dp
                        )
                ) {
                    val showHours = uiState.durationSeconds >= 3600.0

                    // Seekbar & Time row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current Time
                        Text(
                            text = formatTimestamp(uiState.positionSeconds, forceHours = showHours),
                            color = MxWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.width(if (showHours) 56.dp else 42.dp)
                        )

                        // MX Player Seekbar with buffer cache preview
                        val sliderValue = if (uiState.isScrubbing) {
                            uiState.scrubPreviewSeconds
                        } else {
                            uiState.positionSeconds
                        }.coerceIn(0.0, uiState.durationSeconds.coerceAtLeast(1.0)).toFloat()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.durationSeconds > 0 && uiState.cacheDurationSeconds > 0) {
                                val bufferFraction = (uiState.cacheDurationSeconds / uiState.durationSeconds).coerceIn(0.0, 1.0).toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(bufferFraction)
                                        .height(3.dp)
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color(0x55FFFFFF))
                                )
                            }

                            Slider(
                                value = sliderValue,
                                onValueChange = { targetSec ->
                                    onSliderScrubChange(targetSec.toDouble())
                                },
                                onValueChangeFinished = onSliderScrubEnd,
                                valueRange = 0f..uiState.durationSeconds.coerceAtLeast(1.0).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MxBlue,
                                    activeTrackColor = MxBlue,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Duration or Remaining Time
                        val durationDisplay = if (showRemainingTime) {
                            val remaining = (uiState.durationSeconds - uiState.positionSeconds).coerceAtLeast(0.0)
                            "-${formatTimestamp(remaining, forceHours = showHours)}"
                        } else {
                            formatTimestamp(uiState.durationSeconds, forceHours = showHours)
                        }

                        Text(
                            text = durationDisplay,
                            color = MxWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .width(if (showHours) 56.dp else 42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleTimeDisplay() }
                        )
                    }

                    if (!isLandscape) {
                        // ================================================================
                        // PORTRAIT CONTROLS: 2 distinct rows (Hero Transport + Quick Tools)
                        // Eliminates any horizontal overlap & maximizes thumb ergonomics!
                        // ================================================================

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 1: Centered Hero Transport Controls (Prev, Rewind 10, Play/Pause, Forward 10, Next)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = { onPrevious?.invoke() },
                                enabled = hasPrevious,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = if (hasPrevious) MxWhite else Color(0x44FFFFFF),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = { onSeekRelative(-seekIntervalSeconds) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = MxWhite,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Hero Play/Pause Button
                            Surface(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                color = MxBlue.copy(alpha = 0.25f),
                                border = BorderStroke(1.5.dp, MxBlue),
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(62.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                        tint = MxWhite,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            IconButton(
                                onClick = { onSeekRelative(seekIntervalSeconds) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = MxWhite,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = { onNext?.invoke() },
                                enabled = hasNext,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = if (hasNext) MxWhite else Color(0x44FFFFFF),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: Portrait Quick Actions (Lock, Speed, Repeat, Aspect Ratio, Fullscreen Toggle)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. Lock Screen
                            IconButton(
                                onClick = onLockScreen,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = "Lock Screen",
                                    tint = MxWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. Playback Speed Pill
                            Surface(
                                onClick = onOpenSpeedDialog,
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.playbackSpeed != 1.0) MxBlue.copy(alpha = 0.3f) else MxPillBg,
                                border = BorderStroke(1.dp, if (uiState.playbackSpeed != 1.0) MxBlue else Color(0x66FFFFFF)),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = formatSpeed(uiState.playbackSpeed),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.playbackSpeed != 1.0) MxBlue else MxWhite
                                    )
                                }
                            }

                            // 3. Repeat Mode
                            IconButton(
                                onClick = onToggleRepeat,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = when (repeatMode) {
                                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                        RepeatMode.ALL -> Icons.Rounded.Repeat
                                        RepeatMode.OFF -> Icons.Rounded.Repeat
                                    },
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != RepeatMode.OFF) MxBlue else Color(0x77FFFFFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 4. Aspect Ratio Pill
                            Surface(
                                onClick = onCycleResizeMode,
                                shape = RoundedCornerShape(12.dp),
                                color = MxPillBg,
                                border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                                modifier = Modifier.height(28.dp)
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
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MxWhite
                                    )
                                }
                            }

                            // 5. Fullscreen Toggle Button (Enter Landscape Fullscreen)
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = "Enter Fullscreen",
                                    tint = MxWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        // ================================================================
                        // LANDSCAPE CONTROLS: True visual symmetry & balanced wings
                        // Center Hero Controls are flanked by proportional left/right clusters.
                        // ================================================================
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Left Wing: Child Lock + Repeat Mode
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onLockScreen,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LockOpen,
                                        contentDescription = "Lock Screen",
                                        tint = MxWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onToggleRepeat,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = when (repeatMode) {
                                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                            RepeatMode.ALL -> Icons.Rounded.Repeat
                                            RepeatMode.OFF -> Icons.Rounded.Repeat
                                        },
                                        contentDescription = "Repeat",
                                        tint = if (repeatMode != RepeatMode.OFF) MxBlue else Color(0x77FFFFFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Center Anchor: Perfectly centered Hero Playback Controls
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { onPrevious?.invoke() },
                                    enabled = hasPrevious,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = if (hasPrevious) MxWhite else Color(0x44FFFFFF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSeekRelative(-seekIntervalSeconds) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay10,
                                        contentDescription = "Rewind 10s",
                                        tint = MxWhite,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                // Hero Play/Pause Button
                                Surface(
                                    onClick = onTogglePlayPause,
                                    shape = CircleShape,
                                    color = MxBlue.copy(alpha = 0.25f),
                                    border = BorderStroke(1.5.dp, MxBlue),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(58.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                            tint = MxWhite,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onSeekRelative(seekIntervalSeconds) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forward10,
                                        contentDescription = "Forward 10s",
                                        tint = MxWhite,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onNext?.invoke() },
                                    enabled = hasNext,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        tint = if (hasNext) MxWhite else Color(0x44FFFFFF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Right Wing: Quick Tools (Playback Speed Pill, Aspect Ratio Pill, Fullscreen Toggle)
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = onOpenSpeedDialog,
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (uiState.playbackSpeed != 1.0) MxBlue.copy(alpha = 0.3f) else MxPillBg,
                                    border = BorderStroke(1.dp, if (uiState.playbackSpeed != 1.0) MxBlue else Color(0x66FFFFFF)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = formatSpeed(uiState.playbackSpeed),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.playbackSpeed != 1.0) MxBlue else MxWhite
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onCycleResizeMode,
                                    shape = RoundedCornerShape(12.dp),
                                    color = MxPillBg,
                                    border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                                    modifier = Modifier.height(28.dp)
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
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MxWhite
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = MxWhite,
                                        modifier = Modifier.size(24.dp)
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

// ============================================================================
// STREAMLINED 3-DOT OVERFLOW MENU (ADVANCED SETTINGS NOT ON SCREEN)
// ============================================================================

@Composable
private fun BkmPlayerOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    uiState: PlayerUiState,
    sleepTimerMinutes: Int,
    onOpenSleepTimerDialog: () -> Unit,
    onOpenAbRepeatDialog: () -> Unit,
    onOpenAudioDelayDialog: () -> Unit,
    onToggleNightMode: () -> Unit,
    onOpenSubtitleDelayDialog: () -> Unit,
    onOpenSubtitleAppearanceDialog: () -> Unit,
    onOpenExternalSubtitle: () -> Unit,
    onResetZoom: () -> Unit,
    onOpenPropertiesDialog: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onOpenFile: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = (-8).dp, y = 6.dp),
        modifier = Modifier
            .background(MxMenuBg)
            .border(BorderStroke(1.dp, Color(0x28FFFFFF)), RoundedCornerShape(16.dp))
            .width(270.dp)
            .padding(vertical = 4.dp)
    ) {
        // Menu Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MxBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Advanced Controls",
                color = MxWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MxBlue.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = "${uiState.videoHeight}p",
                        color = MxBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 12.dp))

        // 1. Audio Sync Delay
        MxMenuActionItem(
            icon = Icons.Rounded.Timer,
            title = "Audio Delay",
            trailing = "${String.format(Locale.US, "%.1f", uiState.audioDelaySeconds)}s",
            onClick = onOpenAudioDelayDialog
        )

        // 2. Speech Clarifier (Night Mode Voice Boost)
        MxMenuToggleItem(
            icon = Icons.Rounded.Hearing,
            title = "Speech Clarifier",
            subtitle = "Night mode voice boost",
            checked = uiState.isNightModeAudio,
            onCheckedChange = { onToggleNightMode() }
        )

        HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.padding(horizontal = 12.dp))

        // 3. Subtitle Sync Delay
        MxMenuActionItem(
            icon = Icons.Rounded.Timer,
            title = "Subtitle Delay",
            trailing = "${String.format(Locale.US, "%.1f", uiState.subtitleDelaySeconds)}s",
            onClick = onOpenSubtitleDelayDialog
        )

        // 4. Subtitle Style & Size
        MxMenuActionItem(
            icon = Icons.Rounded.Subtitles,
            title = "Subtitle Style & Size",
            trailing = "${uiState.subtitleFontSize}sp",
            onClick = onOpenSubtitleAppearanceDialog
        )

        // 5. Open External Subtitle File
        MxMenuActionItem(
            icon = Icons.Rounded.FolderOpen,
            title = "Load Subtitle File...",
            trailing = null,
            onClick = onOpenExternalSubtitle
        )

        HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.padding(horizontal = 12.dp))

        // 6. Sleep Timer
        MxMenuActionItem(
            icon = Icons.Rounded.Timer,
            title = "Sleep Timer",
            trailing = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m left" else "Off",
            onClick = onOpenSleepTimerDialog
        )

        // 7. A-B Repeat Loop
        MxMenuActionItem(
            icon = Icons.Rounded.RepeatOne,
            title = "A-B Repeat",
            trailing = if (uiState.abRepeatPointA != null && uiState.abRepeatPointB != null) "Active" else null,
            onClick = onOpenAbRepeatDialog
        )

        // 8. Reset Video Zoom (if zoomed)
        MxMenuActionItem(
            icon = Icons.Rounded.AspectRatio,
            title = "Reset Video Zoom",
            trailing = "100%",
            onClick = onResetZoom
        )

        HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.padding(horizontal = 12.dp))

        // 9. Frame Capture / Screenshot
        MxMenuActionItem(
            icon = Icons.Rounded.CameraAlt,
            title = "Capture Frame",
            trailing = null,
            onClick = onCaptureScreenshot
        )

        // 10. Media Properties & Codecs
        MxMenuActionItem(
            icon = Icons.Rounded.Info,
            title = "Media Properties",
            trailing = null,
            onClick = onOpenPropertiesDialog
        )

        // 11. Open Another Video
        MxMenuActionItem(
            icon = Icons.Rounded.FolderOpen,
            title = "Open Video File...",
            trailing = null,
            onClick = onOpenFile
        )
    }
}

@Composable
private fun MxMenuActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailing: String?,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x1AFFFFFF),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MxWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = MxWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MxBlue.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = trailing,
                            color = MxBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun MxMenuToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (checked) MxBlue.copy(alpha = 0.22f) else Color(0x1AFFFFFF),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (checked) MxBlue else MxWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MxWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        color = if (checked) MxBlue else MxSubtext,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MxWhite,
                        checkedTrackColor = MxBlue,
                        uncheckedThumbColor = MxSubtext,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        },
        onClick = { onCheckedChange(!checked) },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    )
}

// ============================================================================
// MX PLAYER MODAL DIALOGS (CLEAN POPUPS)
// ============================================================================

@Composable
private fun MxDecoderDialog(
    currentMode: DecoderMode,
    onSelectMode: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Decoder", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    Triple(DecoderMode.HW_PLUS, "HW+ decoder", "Hardware decoding with MX custom renderer"),
                    Triple(DecoderMode.HW, "HW decoder", "Standard Android hardware decoding"),
                    Triple(DecoderMode.SW, "SW decoder", "CPU software decoding (universal)")
                )

                options.forEach { (mode, title, subtitle) ->
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
                            colors = RadioButtonDefaults.colors(selectedColor = MxBlue)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                color = if (isSelected) MxBlue else MxWhite,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = subtitle,
                                color = MxSubtext,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MxBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun MxAudioTracksDialog(
    uiState: PlayerUiState,
    onSelectTrack: (Int) -> Unit,
    onDisableAudio: () -> Unit,
    onOpenDelay: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Audio Tracks", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mute / Off option
                val isMuted = (uiState.selectedAudioTrackId == null)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisableAudio() }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = isMuted,
                        onClick = onDisableAudio,
                        colors = RadioButtonDefaults.colors(selectedColor = MxRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mute audio (Off)",
                        color = if (isMuted) MxRed else MxWhite,
                        fontSize = 13.sp,
                        fontWeight = if (isMuted) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Available tracks
                if (uiState.audioTracks.isEmpty()) {
                    Text(
                        text = "Default audio track",
                        color = MxSubtext,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                } else {
                    uiState.audioTracks.forEach { track ->
                        val isSelected = (track.id == uiState.selectedAudioTrackId)
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
                                colors = RadioButtonDefaults.colors(selectedColor = MxBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = track.displayName,
                                    color = if (isSelected) MxBlue else MxWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                val details = listOfNotNull(
                                    track.codec?.uppercase(Locale.US),
                                    if (track.channels > 0) "${track.channels} ch" else null,
                                    if (track.sampleRate > 0) "${track.sampleRate / 1000} kHz" else null
                                ).joinToString(" • ")
                                if (details.isNotBlank()) {
                                    Text(
                                        text = details,
                                        color = MxSubtext,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))

                TextButton(
                    onClick = onOpenDelay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Adjust Audio Delay (Sync)...",
                        color = MxBlue,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxAudioDelayDialog(
    currentDelay: Double,
    onAdjust: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Audio Delay", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            MxSyncDelayAdjuster(
                label = "Audio Synchronization",
                delaySec = currentDelay,
                onAdjust = onAdjust
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxSubtitleTracksDialog(
    uiState: PlayerUiState,
    onSelectTrack: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onLoadExternalSubtitle: () -> Unit,
    onOpenDelay: () -> Unit,
    onOpenAppearance: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Subtitles", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subtitles Off
                val isOff = (uiState.selectedSubtitleTrackId == null)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisableSubtitles() }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = isOff,
                        onClick = onDisableSubtitles,
                        colors = RadioButtonDefaults.colors(selectedColor = MxBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Subtitles Off",
                        color = if (isOff) MxBlue else MxWhite,
                        fontSize = 13.sp,
                        fontWeight = if (isOff) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Subtitle tracks
                if (uiState.subtitleTracks.isEmpty()) {
                    Text(
                        text = "No embedded subtitles found",
                        color = MxSubtext,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                } else {
                    uiState.subtitleTracks.forEach { track ->
                        val isSelected = (track.id == uiState.selectedSubtitleTrackId)
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
                                colors = RadioButtonDefaults.colors(selectedColor = MxBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = track.displayName,
                                    color = if (isSelected) MxBlue else MxWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (!track.codec.isNullOrBlank()) {
                                    Text(
                                        text = track.codec.uppercase(Locale.US),
                                        color = MxSubtext,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))

                // Actions: Open File, Delay, Appearance
                OutlinedButton(
                    onClick = onLoadExternalSubtitle,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MxBlue)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = MxBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open external subtitle...", color = MxBlue, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onOpenDelay,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delay...", color = MxBlue, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onOpenAppearance,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Style & Size...", color = MxBlue, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxSubtitleDelayDialog(
    currentDelay: Double,
    onAdjust: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Subtitle Delay", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            MxSyncDelayAdjuster(
                label = "Subtitle Synchronization",
                delaySec = currentDelay,
                onAdjust = onAdjust
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxSubtitleAppearanceDialog(
    currentSize: Int,
    currentColor: String,
    onSelectSize: (Int) -> Unit,
    onSelectColor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Subtitle Appearance", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Font Size Section
                Text(
                    text = "TEXT SIZE",
                    color = MxBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                val sizes = listOf(
                    36 to "Small",
                    48 to "Normal",
                    60 to "Large",
                    72 to "Extra Large"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sizes.forEach { (sz, label) ->
                        val isSelected = (currentSize == sz)
                        FilledTonalButton(
                            onClick = { onSelectSize(sz) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected) MxBlue else Color(0x22FFFFFF)
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSelected) MxWhite else Color(0xCCFFFFFF),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0x22FFFFFF))

                // Font Color Section
                Text(
                    text = "TEXT COLOR",
                    color = MxBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                val colors = listOf(
                    "#FFFFFF" to "White",
                    "#FFEE58" to "Yellow",
                    "#00E5FF" to "Cyan",
                    "#69F0AE" to "Green"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colors.forEach { (hex, name) ->
                        val isSelected = currentColor.equals(hex, ignoreCase = true)
                        FilledTonalButton(
                            onClick = { onSelectColor(hex) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected) MxBlue else Color(0x22FFFFFF)
                            )
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                color = if (isSelected) MxWhite else Color(0xCCFFFFFF),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun formatSpeed(speed: Double): String {
    val rounded = (kotlin.math.round(speed * 100.0) / 100.0)
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toInt()}.0x"
    } else if ((rounded * 10.0) % 1.0 == 0.0) {
        String.format(Locale.US, "%.1fx", rounded)
    } else {
        String.format(Locale.US, "%.2fx", rounded)
    }
}

@Composable
private fun MxSpeedDialog(
    currentSpeed: Double,
    onSelectSpeed: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = remember { listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Speed",
                    color = MxWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (abs(currentSpeed - 1.0) < 0.01) Color(0x22FFFFFF) else MxBlue.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, if (abs(currentSpeed - 1.0) < 0.01) Color(0x33FFFFFF) else MxBlue)
                ) {
                    Text(
                        text = formatSpeed(currentSpeed),
                        color = if (abs(currentSpeed - 1.0) < 0.01) MxWhite else MxBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stepper adjustments for fine tuning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val next = (kotlin.math.round((currentSpeed - 0.05) * 100.0) / 100.0).coerceAtLeast(0.25)
                            onSelectSpeed(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x22FFFFFF))
                    ) {
                        Text("-0.05x", fontSize = 12.sp, color = MxWhite, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = { onSelectSpeed(1.0) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (abs(currentSpeed - 1.0) < 0.01) MxBlue else MxBlue.copy(alpha = 0.22f)
                        )
                    ) {
                        Text(
                            "1.0x Normal",
                            fontSize = 12.sp,
                            color = if (abs(currentSpeed - 1.0) < 0.01) MxWhite else MxBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val next = (kotlin.math.round((currentSpeed + 0.05) * 100.0) / 100.0).coerceAtMost(3.0)
                            onSelectSpeed(next)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x22FFFFFF))
                    ) {
                        Text("+0.05x", fontSize = 12.sp, color = MxWhite, fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(color = Color(0x18FFFFFF))

                // Presets grid in 4 columns (2 rows of 4)
                val chunkedPresets = presets.chunked(4)
                chunkedPresets.forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { speed ->
                            val isSelected = abs(currentSpeed - speed) < 0.02
                            Surface(
                                onClick = { onSelectSpeed(speed) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MxBlue else Color(0x1AFFFFFF),
                                border = BorderStroke(1.dp, if (isSelected) MxBlue else Color(0x28FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (speed == 1.0) "1.0x" else "${speed}x",
                                        color = if (isSelected) MxWhite else Color(0xDDFFFFFF),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MxBlue)
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxAspectRatioDialog(
    currentMode: ResizeMode,
    onSelectMode: (ResizeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Aspect Ratio", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val options = listOf(
                    ResizeMode.FIT to "Fit to screen",
                    ResizeMode.STRETCH to "Stretch",
                    ResizeMode.CROP to "Crop",
                    ResizeMode.ORIGINAL_100 to "100% (Original)",
                    ResizeMode.SIXTEEN_NINE to "16:9",
                    ResizeMode.FOUR_THREE to "4:3"
                )

                options.forEach { (mode, label) ->
                    val isSelected = (mode == currentMode)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = MxBlue)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            color = if (isSelected) MxBlue else MxWhite,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MxBlue)
            }
        }
    )
}

@Composable
private fun MxSleepTimerDialog(
    currentMinutes: Int,
    onSetMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Sleep Timer", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val presets = listOf(
                    0 to "Turn off",
                    10 to "10 minutes",
                    20 to "20 minutes",
                    30 to "30 minutes",
                    45 to "45 minutes",
                    60 to "60 minutes",
                    90 to "90 minutes"
                )
                presets.forEach { (minutes, label) ->
                    val isSelected = (currentMinutes == minutes)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetMinutes(minutes) }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSetMinutes(minutes) },
                            colors = RadioButtonDefaults.colors(selectedColor = MxBlue)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            color = if (isSelected) MxBlue else MxWhite,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MxBlue)
            }
        }
    )
}

@Composable
private fun MxAbRepeatDialog(
    uiState: PlayerUiState,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearRepeat: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("A-B Repeat", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Repeat a section of the video between Point A and Point B continuously.",
                    color = MxSubtext,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Point A: ${uiState.abRepeatPointA?.let { formatTimestamp(it) } ?: "Not set"}",
                            color = if (uiState.abRepeatPointA != null) MxBlue else MxWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Point B: ${uiState.abRepeatPointB?.let { formatTimestamp(it) } ?: "Not set"}",
                            color = if (uiState.abRepeatPointB != null) MxBlue else MxWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val pointA = uiState.abRepeatPointA
                    val isPointBEnabled = pointA != null && uiState.positionSeconds > pointA
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onSetPointA,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MxBlue.copy(alpha = 0.22f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Set A", color = MxBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = onSetPointB,
                            enabled = isPointBEnabled,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MxBlue.copy(alpha = 0.22f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Set B", color = MxBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.abRepeatPointA != null || uiState.abRepeatPointB != null) {
                    OutlinedButton(
                        onClick = onClearRepeat,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MxRed)
                    ) {
                        Text("Clear Loop", color = MxRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxPropertiesDialog(
    info: MediaInfo,
    videoWidth: Int,
    videoHeight: Int,
    decoderMode: String,
    filePath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MxDialogBg,
        title = {
            Text("Properties", color = MxWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MxPropertyRow(label = "Title", value = info.title)
                if (filePath.isNotBlank()) {
                    MxPropertyRow(label = "Path", value = filePath)
                }
                MxPropertyRow(
                    label = "Resolution",
                    value = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x$videoHeight" else info.resolution
                )
                MxPropertyRow(label = "Video Codec", value = info.videoCodec)
                if (info.videoFps.isNotBlank()) {
                    MxPropertyRow(label = "Framerate", value = info.videoFps)
                }
                MxPropertyRow(label = "Audio Codec", value = info.audioCodec)
                if (info.audioChannels.isNotBlank()) {
                    MxPropertyRow(label = "Channels", value = info.audioChannels)
                }
                if (info.sampleRate.isNotBlank()) {
                    MxPropertyRow(label = "Sample Rate", value = info.sampleRate)
                }
                MxPropertyRow(label = "Decoder", value = decoderMode)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = MxBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MxSyncDelayAdjuster(
    label: String,
    delaySec: Double,
    onAdjust: (Double) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = MxWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = when {
                    delaySec > 0.001 -> "+${String.format(Locale.US, "%.2f", delaySec)}s (${(delaySec * 1000).roundToInt()}ms)"
                    delaySec < -0.001 -> "${String.format(Locale.US, "%.2f", delaySec)}s (${(delaySec * 1000).roundToInt()}ms)"
                    else -> "0.0s (0ms)"
                },
                color = if (delaySec != 0.0) MxBlue else MxSubtext,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalButton(
                onClick = { onAdjust(delaySec - 0.5) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x22FFFFFF))
            ) {
                Text("-0.5s", fontSize = 11.sp, color = MxWhite)
            }
            FilledTonalButton(
                onClick = { onAdjust(delaySec - 0.1) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x22FFFFFF))
            ) {
                Text("-0.1s", fontSize = 11.sp, color = MxWhite)
            }
            FilledTonalButton(
                onClick = { onAdjust(0.0) },
                enabled = delaySec != 0.0,
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MxBlue.copy(alpha = 0.22f))
            ) {
                Text("Reset", fontSize = 11.sp, color = MxBlue, fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                onClick = { onAdjust(delaySec + 0.1) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x22FFFFFF))
            ) {
                Text("+0.1s", fontSize = 11.sp, color = MxWhite)
            }
            FilledTonalButton(
                onClick = { onAdjust(delaySec + 0.5) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x22FFFFFF))
            ) {
                Text("+0.5s", fontSize = 11.sp, color = MxWhite)
            }
        }
    }
}

@Composable
private fun MxPropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MxSubtext, fontSize = 12.sp)
        Text(
            text = value.ifBlank { "N/A" },
            color = MxWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
        )
    }
}

@Composable
private fun MxEmptyPlayerPrompt(
    errorMessage: String?,
    onOpenFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MxDialogBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = MxBlue,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "MX Player",
                style = MaterialTheme.typography.titleMedium,
                color = MxWhite,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hardware accelerated video playback",
                style = MaterialTheme.typography.bodySmall,
                color = MxSubtext
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
                colors = ButtonDefaults.buttonColors(containerColor = MxBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Select Video File", color = MxWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MpvSurfaceCanvas(
    engine: NativeMpvEngine,
    modifier: Modifier = Modifier
) {
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
                engine.attachToSurfaceView(this)
            }
        },
        update = { view ->
            engine.attachToSurfaceView(view)
        },
        onRelease = { view ->
            view.holder.removeCallback(engine)
            engine.detachFromSurfaceView()
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

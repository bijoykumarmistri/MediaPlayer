package com.mediaplayer

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mediaplayer.data.MediaLibraryRepository
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.MainMediaPlayerScreen
import com.mediaplayer.ui.theme.MediaPlayerTheme
import com.mediaplayer.util.AppSettingsManager
import com.mediaplayer.util.HighRefreshRateManager
import com.mediaplayer.vault.VaultRepository
import com.mediaplayer.vault.VaultSecurityManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_PIP_CONTROL = "com.bkm.player.ACTION_PIP_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "control_type"
        private const val CONTROL_TYPE_PLAY_PAUSE = 1
        private const val CONTROL_TYPE_REPLAY = 2
        private const val CONTROL_TYPE_FORWARD = 3
    }

    private lateinit var playerManager: MediaPlayerManager
    private lateinit var repository: MediaLibraryRepository
    private lateinit var highRefreshRateManager: HighRefreshRateManager

    private val isInPipModeState = mutableStateOf(value = false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Notification permission response handled */ }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_CONTROL) {
                when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                    CONTROL_TYPE_PLAY_PAUSE -> playerManager.playPause()
                    CONTROL_TYPE_REPLAY -> playerManager.seekBackward(10000L)
                    CONTROL_TYPE_FORWARD -> playerManager.seekForward(10000L)
                }
                if (isInPictureInPictureMode) {
                    try {
                        setPictureInPictureParams(buildPipParams())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        highRefreshRateManager = HighRefreshRateManager(this)
        highRefreshRateManager.start()

        window?.decorView?.post {
            requestNotificationPermission()
        }

        val app = application as MediaPlayerApplication
        playerManager = app.playerManager
        repository = app.repository
        val vaultRepository = app.vaultRepository
        val vaultSecurityManager = app.vaultSecurityManager

        val filter = IntentFilter(ACTION_PIP_CONTROL)
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        lifecycleScope.launch {
            var wasAutoEnterEligible = false
            combine(
                playerManager.currentMediaItem,
                playerManager.isPlaying,
                playerManager.isVideoPlaying,
                playerManager.videoDimensions,
                playerManager.isPlayerExpanded,
            ) { item, isPlaying, isVideoPlaying, dims, isExpanded ->
                val isVideo = item?.isVideo == true
                val autoEnterEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    isVideo && isVideoPlaying && isExpanded
                Triple(autoEnterEligible, isInPictureInPictureMode, Triple(isPlaying, isVideoPlaying, dims))
            }.collect { (autoEnterEligible, inPip, _) ->
                if (inPip || autoEnterEligible || wasAutoEnterEligible) {
                    wasAutoEnterEligible = autoEnterEligible
                    try {
                        setPictureInPictureParams(buildPipParams())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        setContent {
            val pitchBlackEnabled by AppSettingsManager.pitchBlackThemeFlow(this).collectAsState()
            MediaPlayerTheme(oledMode = pitchBlackEnabled) {
                MainContent(
                    repository = repository,
                    vaultRepository = vaultRepository,
                    vaultSecurityManager = vaultSecurityManager,
                    playerManager = playerManager,
                    isInPipMode = isInPipModeState.value,
                    onEnterPip = ::enterPip,
                    onRefreshRateSettingChanged = { highRefreshRateManager.applyOptimalRefreshRate() },
                )
            }
        }

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if ((action == Intent.ACTION_VIEW) && (intent.data != null)) {
            val uri = intent.data ?: return
            lifecycleScope.launch {
                val item = repository.importSafSingleFile(uri)
                if (item != null) {
                    playerManager.playMedia(item)
                    playerManager.setPlayerExpanded(expanded = true)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            try {
                setPictureInPictureParams(buildPipParams())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Returned from PiP back to app - ensure player remains expanded and video keeps playing
            playerManager.setPlayerExpanded(true)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerManager.currentMediaItem.value?.isVideo == true &&
            playerManager.isPlayerExpanded.value &&
            playerManager.isVideoPlaying.value
        ) {
            enterPip()
        }
    }

    private fun enterPip() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        val currentItem = playerManager.currentMediaItem.value
        if (currentItem?.isVideo == true && playerManager.isPlayerExpanded.value) {
            try {
                isInPipModeState.value = true
                val params = buildPipParams()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                isInPipModeState.value = false
                e.printStackTrace()
            }
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val rational = try {
            val isVideo = playerManager.currentMediaItem.value?.isVideo == true
            val dims = playerManager.videoDimensions.value
            val (width, height) = if (isVideo && dims != null && dims.first > 0 && dims.second > 0) {
                dims
            } else if (isVideo) {
                val res = playerManager.currentMediaItem.value?.resolution.orEmpty()
                val parts = res.split("x")
                if (parts.size == 2) {
                    val w = parts[0].trim().toIntOrNull() ?: 0
                    val h = parts[1].trim().toIntOrNull() ?: 0
                    if (w > 0 && h > 0) Pair(w, h) else Pair(0, 0)
                } else {
                    Pair(0, 0)
                }
            } else {
                Pair(0, 0)
            }

            if (width > 0 && height > 0) {
                // Android framework mandates PiP aspect ratio between 0.418410 and 2.390000
                val aspect = (width.toFloat() / height.toFloat()).coerceIn(0.42f, 2.38f)
                val denom = 1000
                val num = (aspect * denom).roundToInt()
                Rational(num, denom)
            } else {
                Rational(16, 9)
            }
        } catch (_: Exception) {
            Rational(16, 9)
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)

        val isVideo = playerManager.currentMediaItem.value?.isVideo == true
        val isVideoPlaying = playerManager.isVideoPlaying.value
        val isPlaying = if (isVideo) isVideoPlaying else playerManager.isPlaying.value

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isVideoPlaying && isVideo && playerManager.isPlayerExpanded.value)
            builder.setSeamlessResizeEnabled(true)
        }

        val actions = mutableListOf<RemoteAction>()

        // 1. Replay 10s
        val replayIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_REPLAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val replayIcon = Icon.createWithResource(this, android.R.drawable.ic_media_rew)
        actions.add(RemoteAction(replayIcon, "Rewind 10s", "Rewind 10s", replayIntent))

        // 2. Play / Pause
        val playPauseIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = Icon.createWithResource(
            this,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        actions.add(RemoteAction(playPauseIcon, if (isPlaying) "Pause" else "Play", if (isPlaying) "Pause" else "Play", playPauseIntent))

        // 3. Forward 10s
        val forwardIntent = PendingIntent.getBroadcast(
            this,
            3,
            Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_FORWARD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardIcon = Icon.createWithResource(this, android.R.drawable.ic_media_ff)
        actions.add(RemoteAction(forwardIcon, "Forward 10s", "Forward 10s", forwardIntent))

        builder.setActions(actions)
        return builder.build()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        highRefreshRateManager.applyOptimalRefreshRate()
    }

    override fun onPause() {
        super.onPause()
        (application as? MediaPlayerApplication)?.vaultSecurityManager?.lock()
    }

    override fun onStop() {
        super.onStop()
        val backgroundPlayback = com.mediaplayer.util.AppSettingsManager(this).backgroundPlayback
        // Only pause video playback if background playback is disabled and not in PiP mode
        if (!backgroundPlayback && !isInPictureInPictureMode && playerManager.currentMediaItem.value?.isVideo == true) {
            playerManager.pause()
        }
    }

    override fun onDestroy() {
        highRefreshRateManager.stop()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
        val backgroundPlayback = com.mediaplayer.util.AppSettingsManager(this).backgroundPlayback
        if (!backgroundPlayback && isFinishing && playerManager.currentMediaItem.value?.isVideo == true) {
            playerManager.pause()
        }
        super.onDestroy()
    }
}

@Composable
fun MainContent(
    repository: MediaLibraryRepository,
    vaultRepository: VaultRepository,
    vaultSecurityManager: VaultSecurityManager,
    playerManager: MediaPlayerManager,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    onRefreshRateSettingChanged: () -> Unit = {}
) {
    MainMediaPlayerScreen(
        repository = repository,
        vaultRepository = vaultRepository,
        vaultSecurityManager = vaultSecurityManager,
        playerManager = playerManager,
        isInPipMode = isInPipMode,
        onEnterPip = onEnterPip,
        onRefreshRateSettingChanged = onRefreshRateSettingChanged
    )
}

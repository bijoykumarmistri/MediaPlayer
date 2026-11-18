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
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mediaplayer.audio.UsbBitPerfectAudioController
import com.mediaplayer.data.MediaRepository
import com.mediaplayer.playback.MediaPlayerManager
import com.mediaplayer.ui.MainMediaPlayerScreen
import com.mediaplayer.ui.theme.MediaPlayerTheme
import com.mediaplayer.util.HighRefreshRateManager
import com.mediaplayer.vault.VaultRepository
import com.mediaplayer.vault.VaultSecurityManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_PIP_CONTROL = "com.mediaplayer.ACTION_PIP_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "control_type"
        private const val CONTROL_TYPE_PLAY_PAUSE = 1
        private const val CONTROL_TYPE_REPLAY = 2
        private const val CONTROL_TYPE_FORWARD = 3
    }

    private lateinit var playerManager: MediaPlayerManager
    private lateinit var usbController: UsbBitPerfectAudioController
    private lateinit var repository: MediaRepository
    private lateinit var highRefreshRateManager: HighRefreshRateManager

    private val isInPipModeState = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        highRefreshRateManager = HighRefreshRateManager(this)
        highRefreshRateManager.start()

        requestNotificationPermission()

        val app = application as MediaPlayerApplication
        playerManager = app.playerManager
        usbController = app.usbAudioController
        repository = app.repository
        val vaultRepository = app.vaultRepository
        val vaultSecurityManager = app.vaultSecurityManager

        val filter = IntentFilter(ACTION_PIP_CONTROL)
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        lifecycleScope.launch {
            playerManager.isPlaying.collect {
                if (isInPictureInPictureMode) {
                    try {
                        setPictureInPictureParams(buildPipParams())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        setContent {
            MediaPlayerTheme(oledMode = true) {
                MainContent(
                    repository = repository,
                    vaultRepository = vaultRepository,
                    vaultSecurityManager = vaultSecurityManager,
                    playerManager = playerManager,
                    usbController = usbController,
                    isInPipMode = isInPipModeState.value,
                    onEnterPip = { enterPip() }
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
        if (action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data ?: return
            lifecycleScope.launch {
                val item = repository.importSafSingleFile(uri)
                if (item != null) {
                    playerManager.playMedia(item)
                    playerManager.setPlayerExpanded(true)
                }
            }
        } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            usbController.scanAudioDevices()
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
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerManager.isPlaying.value && playerManager.currentMediaItem.value?.isVideo == true) {
            enterPip()
        }
    }

    private fun enterPip() {
        val currentItem = playerManager.currentMediaItem.value
        if (currentItem?.isVideo == true) {
            try {
                val params = buildPipParams()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val videoSize = playerManager.player.videoSize
        val rational = if (videoSize.width > 0 && videoSize.height > 0) {
            val aspect = (videoSize.width.toFloat() / videoSize.height.toFloat()).coerceIn(0.42f, 2.38f)
            val denom = 1000
            val num = (aspect * denom).roundToInt()
            Rational(num, denom)
        } else {
            Rational(16, 9)
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(rational)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(playerManager.isPlaying.value && playerManager.currentMediaItem.value?.isVideo == true)
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
        val isPlaying = playerManager.isPlaying.value
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

    override fun onDestroy() {
        super.onDestroy()
        highRefreshRateManager.stop()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
        if (isFinishing && playerManager.currentMediaItem.value?.isVideo == true) {
            playerManager.pause()
        }
    }
}

@Composable
fun MainContent(
    repository: MediaRepository,
    vaultRepository: VaultRepository,
    vaultSecurityManager: VaultSecurityManager,
    playerManager: MediaPlayerManager,
    usbController: UsbBitPerfectAudioController,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit
) {
    MainMediaPlayerScreen(
        repository = repository,
        vaultRepository = vaultRepository,
        vaultSecurityManager = vaultSecurityManager,
        playerManager = playerManager,
        usbController = usbController,
        isInPipMode = isInPipMode,
        onEnterPip = onEnterPip
    )
}

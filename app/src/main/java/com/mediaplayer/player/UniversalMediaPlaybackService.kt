package com.mediaplayer.player

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mediaplayer.MediaPlayerApplication
import com.mediaplayer.R
import com.mediaplayer.audio.UsbBitPerfectAudioController

@OptIn(UnstableApi::class)
class UniversalMediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("media_playback_channel")
                .setChannelName(R.string.media_playback_channel_name)
                .build()
            notificationProvider.setSmallIcon(R.drawable.ic_stat_media_play)
            setMediaNotificationProvider(notificationProvider)

            val app = application as? MediaPlayerApplication
            if (app != null) {
                mediaSession = app.playerManager.mediaSession
                mediaSession?.let { addSession(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? MediaPlayerApplication
        val currentItem = app?.playerManager?.currentMediaItem?.value
        if (currentItem?.isVideo == true) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (mediaSession == null) {
            if (app != null) {
                mediaSession = app.playerManager.mediaSession
                mediaSession?.let { addSession(it) }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null) {
            val app = application as? MediaPlayerApplication
            if (app != null) {
                mediaSession = app.playerManager.mediaSession
                mediaSession?.let { addSession(it) }
            }
        }
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val app = application as? MediaPlayerApplication
        val currentItem = app?.playerManager?.currentMediaItem?.value
        val isVideo = currentItem?.isVideo == true
        val player = mediaSession?.player

        if (isVideo) {
            // Video playback stops when the app is removed from the minimization/recents window
            player?.pause()
            player?.stop()
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            stopSelf()
        } else {
            // Audio/music playback continues running seamlessly in background
            if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        @OptIn(UnstableApi::class)
        fun createExoPlayer(
            context: Context,
            usbAudioController: UsbBitPerfectAudioController
        ): ExoPlayer {
            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink {
                    return DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(true) // Enables 32-bit float output (preventing 24-bit and 32-bit PCM truncation to 16-bit)
                        .setEnableAudioTrackPlaybackParams(true) // Native OS AudioTrack timing for smooth zero-lag speed adjustments
                        .build()
                }
            }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
             .setEnableDecoderFallback(true)
             .setAllowedVideoJoiningTimeMs(5000L) // Standard video joining window preventing frame drops

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            // Optimized HTTP data source with cross-protocol redirect support, standard browser UA, and robust timeouts
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MediaPlayer/2.0")
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)
                .setDefaultRequestProperties(
                    mapOf(
                        "Icy-MetaData" to "1",
                        "Accept" to "*/*"
                    )
                )

            val upstreamDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

            val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)

            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(upstreamDataSourceFactory, extractorsFactory)
                .setLoadErrorHandlingPolicy(androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(4))

            // Smooth high-throughput buffer configuration:
            // 500ms seek start threshold for fast seeks, with 15s to 50s headroom for stutter-free playback
            val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 50_000,
                    /* bufferForPlaybackMs = */ 500,
                    /* bufferForPlaybackAfterRebufferMs = */ 1_000
                )
                .setBackBuffer(/* backBufferDurationMs = */ 30_000, /* retainBackBufferFromKeyframe = */ true)
                .setPrioritizeTimeOverSizeThresholds(false)
                .build()

            val player = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                .build()

            // Wire audio device disconnect safety pause
            usbAudioController.onPlaybackPauseRequested = {
                if (player.isPlaying) {
                    player.pause()
                }
            }

            // CRITICAL: Route ExoPlayer AudioTrack directly to USB DAC for bit-perfect output
            usbAudioController.onActiveUsbAudioDeviceChanged = { device ->
                if (device != null && usbAudioController.status.value.isExclusiveBitPerfectEnabled) {
                    player.setPreferredAudioDevice(device)
                    android.util.Log.d("UsbBitPerfect", "ExoPlayer routed to USB device: ${device.productName} (id: ${device.id})")
                } else {
                    player.setPreferredAudioDevice(null)
                    android.util.Log.d("UsbBitPerfect", "ExoPlayer routing restored to default audio device")
                }
            }

            // Apply immediately if already connected
            val currentDevice = usbAudioController.currentUsbAudioDevice
            if (currentDevice != null && usbAudioController.status.value.isExclusiveBitPerfectEnabled) {
                player.setPreferredAudioDevice(currentDevice)
            }

            // Listen to tracks to lock USB DAC mixer attributes
            player.addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    usbAudioController.enableBitPerfectBypass(
                                        enabled = usbAudioController.status.value.isExclusiveBitPerfectEnabled,
                                        trackFormat = format
                                    )
                                    return
                                }
                            }
                        }
                    }
                }
            })

            return player
        }
    }
}

package com.mediaplayer.player

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mediaplayer.MediaPlayerApplication
import com.mediaplayer.R

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
        if (intent?.action == ACTION_STOP) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            stopSelf()
            return START_NOT_STICKY
        }
        val app = application as? MediaPlayerApplication
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
        val backgroundPlayback = app?.let { com.mediaplayer.util.AppSettingsManager(it).backgroundPlayback } ?: true

        if (isVideo && !backgroundPlayback) {
            // Video playback stops when the app is removed from the recents window if background playback disabled
            player?.pause()
            player?.stop()
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            stopSelf()
        } else {
            // Audio/music playback and background video continue running seamlessly in background
            if ((player == null) || (!player.playWhenReady) || (player.playbackState == Player.STATE_ENDED) || (player.mediaItemCount == 0)) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.let {
            try {
                removeSession(it)
            } catch (_: Exception) {}
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.mediaplayer.action.STOP_SERVICE"

        @OptIn(UnstableApi::class)
        fun createExoPlayer(context: Context): ExoPlayer {
            val appContext = context.applicationContext
            val renderersFactory = DefaultRenderersFactory(appContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)
                .setAllowedVideoJoiningTimeMs(5000L)

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

            val upstreamDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(appContext, httpDataSourceFactory)

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

            return ExoPlayer.Builder(appContext, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                .build()
        }
    }
}

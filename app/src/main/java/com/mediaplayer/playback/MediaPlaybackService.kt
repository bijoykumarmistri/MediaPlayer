package com.mediaplayer.playback

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mediaplayer.MediaPlayerApplication

class MediaPlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return try {
            MediaPlayerApplication.instance.playerManager.mediaSession
        } catch (e: Exception) {
            null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val app = application as? MediaPlayerApplication
        val currentItem = app?.playerManager?.currentMediaItem?.value
        val isVideo = currentItem?.isVideo == true
        val player = MediaPlayerApplication.instance.playerManager.player

        if (isVideo) {
            player.pause()
            player.stop()
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            stopSelf()
        } else {
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

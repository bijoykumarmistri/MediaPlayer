package com.mediaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.mediaplayer.audio.UsbBitPerfectAudioController
import com.mediaplayer.data.MediaRepository
import com.mediaplayer.playback.MediaPlayerManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaPlayerApplication : Application(), ImageLoaderFactory {

    lateinit var playerManager: MediaPlayerManager
        private set

    lateinit var usbAudioController: UsbBitPerfectAudioController
        private set

    lateinit var repository: MediaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        usbAudioController = UsbBitPerfectAudioController(this)
        repository = MediaRepository(this)
        playerManager = MediaPlayerManager(this, usbAudioController)
        playerManager.initialize()

        // Asynchronously warm up the media file cache off the main thread for zero startup lag
        CoroutineScope(Dispatchers.IO).launch {
            repository.preloadCache()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(com.mediaplayer.util.AudioArtworkFetcher.Factory(this@MediaPlayerApplication))
                add(com.mediaplayer.util.VideoThumbnailFetcher.Factory(this@MediaPlayerApplication))
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: MediaPlayerApplication
            private set
    }
}

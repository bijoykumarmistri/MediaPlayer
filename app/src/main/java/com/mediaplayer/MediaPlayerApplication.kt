package com.mediaplayer

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.mediaplayer.data.MediaLibraryRepository
import com.mediaplayer.playback.MediaPlayerManager

import com.mediaplayer.vault.VaultRepository
import com.mediaplayer.vault.VaultSecurityManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaPlayerApplication : Application(), ImageLoaderFactory {

    lateinit var playerManager: MediaPlayerManager
        private set

    lateinit var repository: MediaLibraryRepository
        private set

    lateinit var vaultRepository: VaultRepository
        private set

    lateinit var vaultSecurityManager: VaultSecurityManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.mediaplayer.util.DiagnosticLogManager.installCrashHandler(this)
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
        repository = MediaLibraryRepository(this)
        vaultRepository = VaultRepository(this)
        vaultSecurityManager = VaultSecurityManager(this)
        playerManager = MediaPlayerManager(this)
        // Note: playerManager initializes ExoPlayer and MediaSession lazily on-demand
        // to ensure zero main-thread blocking during cold application startup.

        // Asynchronously warm up the media file cache, preferences, Coil ImageLoader, and FFmpeg native decoder off the main thread
        CoroutineScope(Dispatchers.IO).launch {
            com.mediaplayer.util.AppSettingsManager.pitchBlackThemeFlow(this@MediaPlayerApplication)
            playerManager.historyManager.getLastTab()
            coil.Coil.imageLoader(this@MediaPlayerApplication)
            repository.preloadCache()
            warmAudioDecoderLibraries()
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun warmAudioDecoderLibraries() {
        runCatching {
            androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable()
        }
        runCatching {
            androidx.media3.exoplayer.audio.AudioCapabilities.getCapabilities(
                this,
                androidx.media3.common.AudioAttributes.DEFAULT,
                null
            )
        }
        runCatching {
            androidx.media3.exoplayer.DefaultRenderersFactory(this)
                .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)
            androidx.media3.extractor.DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
            androidx.media3.exoplayer.DefaultLoadControl.Builder().build()
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
                    .strongReferencesEnabled(enable = true)
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.mediaplayer.util.AudioArtworkFetcher.clearMemoryCache()
        com.mediaplayer.util.VideoThumbnailFetcher.clearMemoryCache()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.mediaplayer.util.AudioArtworkFetcher.clearMemoryCache()
        com.mediaplayer.util.VideoThumbnailFetcher.clearMemoryCache()
    }

    companion object {
        lateinit var instance: MediaPlayerApplication
            private set
    }
}

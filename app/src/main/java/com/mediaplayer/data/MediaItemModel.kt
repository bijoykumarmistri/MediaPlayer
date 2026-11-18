package com.mediaplayer.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class MediaItemModel(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val artworkUri: Uri? = null,
    val isVideo: Boolean = false,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val channels: Int = 2,
    val resolution: String = "",
    val folderName: String = "",
    val folderPath: String = "",
    val codecBadge: String = "",
    val dateAdded: Long = 0L,
    val isNew: Boolean = false
) {
    fun toMedia3Item(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setIsPlayable(true)

        // Only pass artworkUri to MediaMetadata if it represents a valid image file or web URL,
        // preventing MediaSession from attempting to decode raw audio/video files as bitmaps
        artworkUri?.let { artUri ->
            val artStr = artUri.toString().lowercase()
            val isDirectImage = artStr.contains(".jpg") || artStr.contains(".jpeg") ||
                    artStr.contains(".png") || artStr.contains(".webp") ||
                    artStr.startsWith("android.resource://")
            if (isDirectImage) {
                metadataBuilder.setArtworkUri(artUri)
            }
        }

        val metadata = metadataBuilder.build()

        val cleanMime = when {
            uri.scheme == "file" -> null // Auto-sniff container format (MKV, MP4, AVI, WebM, TS) directly from file headers
            mimeType.isBlank() || mimeType == "video/*" || mimeType == "audio/*" -> null
            mimeType.startsWith("video/") || mimeType.startsWith("audio/") || mimeType.startsWith("application/") -> mimeType
            else -> null
        }

        val builder = MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMimeType(cleanMime)
            .setMediaMetadata(metadata)

        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https" || scheme == "rtsp") {
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.2f)
                    .setMinPlaybackSpeed(0.9f)
                    .build()
            )
        }

        return builder.build()
    }
}

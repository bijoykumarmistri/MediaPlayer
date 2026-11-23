package com.mediaplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MediaLibraryRepository(private val context: Context) {

    private val tag = "MediaLibraryRepository"

    private val videosCacheFile = File(context.filesDir, "cached_videos.json")
    private val audioCacheFile = File(context.filesDir, "cached_audio.json")

    @Volatile
    private var inMemoryVideos: List<MediaItemModel>? = null

    @Volatile
    private var inMemoryAudios: List<MediaItemModel>? = null

    fun preloadCache() {
        if (inMemoryVideos == null) {
            inMemoryVideos = readCacheFromFile(videosCacheFile)
        }
        if (inMemoryAudios == null) {
            inMemoryAudios = readCacheFromFile(audioCacheFile)
        }
    }

    fun getCachedVideosFast(): List<MediaItemModel> {
        return inMemoryVideos ?: getCachedVideos()
    }

    fun getCachedAudiosFast(): List<MediaItemModel> {
        return inMemoryAudios ?: getCachedAudios()
    }

    fun getCachedVideos(): List<MediaItemModel> {
        return inMemoryVideos ?: readCacheFromFile(videosCacheFile).also { inMemoryVideos = it }
    }

    fun getCachedAudios(): List<MediaItemModel> {
        return inMemoryAudios ?: readCacheFromFile(audioCacheFile).also { inMemoryAudios = it }
    }

    suspend fun saveCachedVideos(list: List<MediaItemModel>) = withContext(Dispatchers.IO) {
        inMemoryVideos = list
        saveCacheToFile(videosCacheFile, list)
    }

    suspend fun saveCachedAudios(list: List<MediaItemModel>) = withContext(Dispatchers.IO) {
        inMemoryAudios = list
        saveCacheToFile(audioCacheFile, list)
    }

    @Suppress("unused")
    suspend fun removeCachedItem(item: MediaItemModel) = withContext(Dispatchers.IO) {
        removeCachedItems(listOf(item))
    }

    suspend fun removeCachedItems(items: List<MediaItemModel>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val videoIds = items.asSequence().filter { it.isVideo }.map { it.id }.toSet()
        val videoUris = items.asSequence().filter { it.isVideo }.map { it.uri }.toSet()
        if (videoIds.isNotEmpty() || videoUris.isNotEmpty()) {
            val list = getCachedVideos().filter { (it.id !in videoIds) && (it.uri !in videoUris) }
            inMemoryVideos = list
            saveCacheToFile(videosCacheFile, list)
        }

        val audioIds = items.asSequence().filter { !it.isVideo }.map { it.id }.toSet()
        val audioUris = items.asSequence().filter { !it.isVideo }.map { it.uri }.toSet()
        if (audioIds.isNotEmpty() || audioUris.isNotEmpty()) {
            val list = getCachedAudios().filter { (it.id !in audioIds) && (it.uri !in audioUris) }
            inMemoryAudios = list
            saveCacheToFile(audioCacheFile, list)
        }
    }

    @Suppress("unused")
    suspend fun deleteMediaFile(
        item: MediaItemModel,
        onRequiresConsent: (android.content.IntentSender) -> Unit,
    ): Boolean {
        val deleted = deleteMediaFiles(listOf(item)) { sender, _ ->
            onRequiresConsent(sender)
        }
        return deleted.isNotEmpty()
    }

    suspend fun deleteMediaFiles(
        items: List<MediaItemModel>,
        onRequiresConsent: (android.content.IntentSender, List<MediaItemModel>) -> Unit,
    ): List<MediaItemModel> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()
        val contentResolver = context.contentResolver
        val successfullyDeleted = mutableListOf<MediaItemModel>()
        val remainingForConsent = mutableListOf<MediaItemModel>()

        for (item in items) {
            val uri = item.uri
            var isDeleted = false

            // 1. If it's a SAF document tree / file picker URI
            if (DocumentsContract.isDocumentUri(context, uri)) {
                try {
                    val deleted = DocumentsContract.deleteDocument(contentResolver, uri)
                    if (deleted) {
                        successfullyDeleted.add(item)
                        isDeleted = true
                    }
                } catch (e: Exception) {
                    Log.w(tag, "DocumentsContract.deleteDocument failed: ${e.message}")
                }
            }

            // 2. Direct ContentResolver delete (works if app has ownership or direct rights)
            if (!isDeleted) {
                try {
                    val rows = contentResolver.delete(uri, null, null)
                    if (rows > 0) {
                        successfullyDeleted.add(item)
                        isDeleted = true
                    }
                } catch (_: SecurityException) {
                    remainingForConsent.add(item)
                    isDeleted = true // Mark to be requested via batch consent below
                } catch (e: Exception) {
                    Log.w(tag, "Direct delete failed: ${e.message}")
                }
            }

            // 3. File system fallback if path is accessible and not already deferred to consent
            if (!isDeleted && !remainingForConsent.contains(item)) {
                if ((uri.scheme == "file") || (item.folderPath.isNotEmpty() && item.title.isNotEmpty())) {
                    try {
                        val path = if (uri.scheme == "file") uri.path else "${item.folderPath}/${item.title}"
                        if (path != null) {
                            val file = File(path)
                            if (file.exists() && file.delete()) {
                                successfullyDeleted.add(item)
                                isDeleted = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "File.delete fallback failed: ${e.message}")
                    }
                }
                if (!isDeleted) {
                    // Try to request consent via MediaStore if direct delete didn't succeed
                    remainingForConsent.add(item)
                }
            }
        }

        // Clean cache for immediately deleted files
        if (successfullyDeleted.isNotEmpty()) {
            removeCachedItems(successfullyDeleted)
        }

        // Handle batch consent for Android 11+ (API 30+) or RecoverableSecurityException for Q
        if (remainingForConsent.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val uris = remainingForConsent.map { it.uri }
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                    withContext(Dispatchers.Main) {
                        onRequiresConsent(pendingIntent.intentSender, remainingForConsent)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to create batch delete request", e)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (Q), prompt for items that triggered RecoverableSecurityException
                val first = remainingForConsent.first()
                try {
                    contentResolver.delete(first.uri, null, null)
                } catch (secEx: android.app.RecoverableSecurityException) {
                    withContext(Dispatchers.Main) {
                        onRequiresConsent(secEx.userAction.actionIntent.intentSender, listOf(first))
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Android Q delete consent failed", e)
                }
            }
        }

        successfullyDeleted
    }

    private fun readCacheFromFile(file: File): List<MediaItemModel> {
        if (!file.exists() || (file.length() == 0L)) return emptyList()
        return try {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = ArrayList<MediaItemModel>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val parsedUri = Uri.parse(obj.getString("uri"))
                val isVideo = obj.optBoolean("isVideo", false)
                val rawArtUri = obj.optString("artworkUri", "").takeIf { it.isNotEmpty() }?.toUri()
                // Sanitize audio artwork: if an audio file was saved with its own audio playback URI as artwork,
                // or if it points to a raw audio track URI rather than albumart/image, clear it so Coil never stalls.
                val sanitizedArtUri = if (!isVideo && rawArtUri != null && (rawArtUri == parsedUri || rawArtUri.toString().contains("/audio/media/"))) {
                    null
                } else {
                    rawArtUri
                }
                list.add(
                    MediaItemModel(
                        id = obj.getLong("id"),
                        uri = parsedUri,
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", "Unknown Album"),
                        durationMs = obj.optLong("durationMs", 0L),
                        artworkUri = sanitizedArtUri,
                        isVideo = isVideo,
                        mimeType = obj.optString("mimeType", ""),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        sampleRate = obj.optInt("sampleRate", 44100),
                        bitDepth = obj.optInt("bitDepth", 16),
                        channels = obj.optInt("channels", 2),
                        resolution = obj.optString("resolution", ""),
                        folderName = obj.optString("folderName", ""),
                        folderPath = obj.optString("folderPath", ""),
                        codecBadge = obj.optString("codecBadge", ""),
                        dateAdded = obj.optLong("dateAdded", 0L),
                        isNew = obj.optBoolean("isNew", false),
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(tag, "Failed to read cache from ${file.name}", e)
            emptyList()
        }
    }

    private fun saveCacheToFile(file: File, list: List<MediaItemModel>) {
        try {
            val jsonArray = JSONArray()
            for (i in list.indices) {
                val item = list[i]
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("uri", item.uri.toString())
                obj.put("title", item.title)
                obj.put("artist", item.artist)
                obj.put("album", item.album)
                obj.put("durationMs", item.durationMs)
                item.artworkUri?.let { obj.put("artworkUri", it.toString()) }
                obj.put("isVideo", item.isVideo)
                obj.put("mimeType", item.mimeType)
                obj.put("sizeBytes", item.sizeBytes)
                obj.put("sampleRate", item.sampleRate)
                obj.put("bitDepth", item.bitDepth)
                obj.put("channels", item.channels)
                obj.put("resolution", item.resolution)
                obj.put("folderName", item.folderName)
                obj.put("folderPath", item.folderPath)
                obj.put("codecBadge", item.codecBadge)
                obj.put("dateAdded", item.dateAdded)
                obj.put("isNew", item.isNew)
                jsonArray.put(obj)
            }

            val tempFile = File(context.filesDir, "${file.name}.tmp")
            tempFile.writeText(jsonArray.toString())
            if (tempFile.exists()) {
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save cache to ${file.name}", e)
        }
    }

    suspend fun getAudioList(): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<MediaItemModel>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "audio/*"
                    val dateAdded = cursor.getLong(dateCol)
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val parentDir = if (!rawPath.isNullOrEmpty()) File(rawPath).parent ?: "" else ""
                    val folderName = if (!rawPath.isNullOrEmpty()) File(rawPath).parentFile?.name ?: "Music" else "Music"

                    // Point directly to MediaStore's indexed album art URI if album exists, or null.
                    // Never point artworkUri to the raw audio track contentUri (which triggers expensive ID3 extraction on 100+ files).
                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId)
                    } else null
                    val codecInfo = formatAudioCodecBadge(mime)

                    audioList.add(
                        MediaItemModel(
                            id = id,
                            uri = contentUri,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            artworkUri = artworkUri,
                            isVideo = false,
                            mimeType = mime,
                            sizeBytes = size,
                            folderName = folderName,
                            folderPath = parentDir,
                            codecBadge = codecInfo,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying MediaStore Audio", e)
        }

        if (audioList.isNotEmpty()) {
            saveCacheToFile(audioCacheFile, audioList)
            audioList
        } else {
            getCachedAudios()
        }
    }

    suspend fun getVideoList(): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<MediaItemModel>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val title = cursor.getString(titleCol) ?: "Unknown Video"
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "video/*"
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val bucketName = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Videos" else "Videos"
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val dateAdded = cursor.getLong(dateCol)

                    val parentDir = if (!rawPath.isNullOrEmpty()) {
                        val f = File(rawPath)
                        f.parent ?: "/storage/emulated/0"
                    } else {
                        "/storage/emulated/0"
                    }

                    val folderName = if (!rawPath.isNullOrEmpty()) {
                        val f = File(rawPath)
                        f.parentFile?.name ?: bucketName
                    } else {
                        bucketName
                    }

                    val resolution = formatVideoResolution(width, height)

                    videoList.add(
                        MediaItemModel(
                            id = id,
                            uri = contentUri,
                            title = title,
                            durationMs = duration,
                            isVideo = true,
                            mimeType = mime,
                            sizeBytes = size,
                            resolution = resolution,
                            folderName = folderName,
                            folderPath = parentDir,
                            artworkUri = contentUri,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying MediaStore Video", e)
        }

        if (videoList.isNotEmpty()) {
            saveCacheToFile(videosCacheFile, videoList)
            videoList
        } else {
            getCachedVideos()
        }
    }

    suspend fun importSafSingleFile(uri: Uri): MediaItemModel? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val isVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment
                ?: "Imported Media"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: if (isVideo) "video/*" else "audio/*"

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val resolution = if (isVideo) formatVideoResolution(width, height) else ""

            retriever.release()

            MediaItemModel(
                id = System.currentTimeMillis(),
                uri = uri,
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                isVideo = isVideo,
                mimeType = mime,
                resolution = resolution,
                artworkUri = uri,
                codecBadge = if (!isVideo) formatAudioCodecBadge(mime) else ""
            )
        } catch (e: Exception) {
            Log.e(tag, "Error importing SAF document", e)
            null
        }
    }

    suspend fun scanSafFolder(treeUri: Uri): Pair<List<MediaItemModel>, List<MediaItemModel>> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<MediaItemModel>()
        val videoList = mutableListOf<MediaItemModel>()

        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: "Media"
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)

                    if (mime.startsWith("audio/")) {
                        audioList.add(
                            MediaItemModel(
                                id = childDocId.hashCode().toLong(),
                                uri = docUri,
                                title = name.substringBeforeLast("."),
                                artist = "Folder Import",
                                album = "Local Storage",
                                isVideo = false,
                                mimeType = mime,
                                sizeBytes = size,
                                codecBadge = formatAudioCodecBadge(mime)
                            )
                        )
                    } else if (mime.startsWith("video/")) {
                        videoList.add(
                            MediaItemModel(
                                id = childDocId.hashCode().toLong(),
                                uri = docUri,
                                title = name.substringBeforeLast("."),
                                isVideo = true,
                                mimeType = mime,
                                sizeBytes = size,
                                folderName = "Imported Folder",
                                artworkUri = docUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error scanning SAF tree", e)
        }

        Pair(audioList, videoList)
    }

    private fun formatAudioCodecBadge(mimeType: String): String {
        return when {
            mimeType.contains("flac", ignoreCase = true) -> "FLAC Hi-Res"
            mimeType.contains("alac", ignoreCase = true) -> "ALAC Lossless"
            mimeType.contains("wav", ignoreCase = true) -> "WAV PCM"
            mimeType.contains("dsd", ignoreCase = true) -> "DSD Direct"
            mimeType.contains("aac", ignoreCase = true) -> "AAC"
            mimeType.contains("opus", ignoreCase = true) -> "Opus"
            mimeType.contains("mp3", ignoreCase = true) -> "MP3"
            else -> "PCM Audio"
        }
    }

    private fun formatVideoResolution(width: Int, height: Int): String {
        return when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 2560 || height >= 1440 -> "2K QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            width > 0 && height > 0 -> "${width}x$height"
            else -> "HD Video"
        }
    }
}

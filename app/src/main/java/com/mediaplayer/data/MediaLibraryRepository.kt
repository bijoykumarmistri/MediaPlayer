package com.mediaplayer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MediaLibraryRepository(context: Context) {

    private val tag = "MediaLibraryRepository"
    private val context: Context = context.applicationContext

    private val videosCacheFile = File(this.context.filesDir, "cached_videos.json")
    private val audioCacheFile = File(this.context.filesDir, "cached_audio.json")

    @Volatile
    private var inMemoryVideos: List<MediaItemModel>? = null

    @Volatile
    private var inMemoryAudios: List<MediaItemModel>? = null

    fun preloadCache() {
        if (inMemoryVideos == null) {
            val vids = readCacheFromFile(videosCacheFile)
            if (inMemoryVideos == null) inMemoryVideos = vids
        }
        if (inMemoryAudios == null) {
            val auds = readCacheFromFile(audioCacheFile)
            if (inMemoryAudios == null) inMemoryAudios = auds
        }
    }

    fun getCachedVideosFast(): List<MediaItemModel> {
        return inMemoryVideos ?: emptyList()
    }

    fun getCachedAudiosFast(): List<MediaItemModel> {
        return inMemoryAudios ?: emptyList()
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

    suspend fun updateCachedItem(oldItem: MediaItemModel, newItem: MediaItemModel) = withContext(Dispatchers.IO) {
        if (oldItem.isVideo) {
            val list = getCachedVideos().map { if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it }
            inMemoryVideos = list
            saveCacheToFile(videosCacheFile, list)
        } else {
            val list = getCachedAudios().map { if (it.id == oldItem.id || it.uri == oldItem.uri) newItem else it }
            inMemoryAudios = list
            saveCacheToFile(audioCacheFile, list)
        }
    }

    suspend fun renameMediaFile(
        item: MediaItemModel,
        desiredName: String,
        onRequiresConsent: (android.content.IntentSender, String) -> Unit,
    ): MediaItemModel? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val uri = item.uri
        val cleanInput = desiredName.trim()
        if (cleanInput.isEmpty()) return@withContext null

        // Determine extension
        val currentExt = when {
            item.title.contains(".") -> item.title.substringAfterLast(".")
            item.folderPath.isNotEmpty() -> {
                val f = File(item.folderPath, item.title)
                if (f.exists() && f.extension.isNotEmpty()) f.extension
                else MimeTypeMap.getSingleton().getExtensionFromMimeType(item.mimeType) ?: ""
            }
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(item.mimeType) ?: ""
        }

        val newDisplayName = if (currentExt.isNotEmpty() && !cleanInput.endsWith(".$currentExt", ignoreCase = true)) {
            "$cleanInput.$currentExt"
        } else {
            cleanInput
        }
        val newTitle = if (newDisplayName.contains(".")) newDisplayName.substringBeforeLast(".") else newDisplayName

        // 1. SAF document tree URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            try {
                val renamedUri = DocumentsContract.renameDocument(contentResolver, uri, newDisplayName)
                if (renamedUri != null) {
                    val updatedItem = item.copy(uri = renamedUri, title = newTitle)
                    updateCachedItem(item, updatedItem)
                    return@withContext updatedItem
                }
            } catch (e: Exception) {
                Log.w(tag, "DocumentsContract.renameDocument failed: ${e.message}")
            }
        }

        // 2. Direct MediaStore update (Android Q+ Scoped Storage & pre-Q)
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
                put(MediaStore.MediaColumns.TITLE, newTitle)
            }
            val rows = contentResolver.update(uri, values, null, null)
            if (rows > 0) {
                val updatedItem = item.copy(title = newTitle)
                updateCachedItem(item, updatedItem)
                return@withContext updatedItem
            }
        } catch (secEx: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createWriteRequest(contentResolver, listOf(uri))
                    withContext(Dispatchers.Main) {
                        onRequiresConsent(pendingIntent.intentSender, newDisplayName)
                    }
                    return@withContext null
                } catch (e: Exception) {
                    Log.e(tag, "Failed to create write request on Android R+", e)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && secEx is android.app.RecoverableSecurityException) {
                withContext(Dispatchers.Main) {
                    onRequiresConsent(secEx.userAction.actionIntent.intentSender, newDisplayName)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            Log.w(tag, "contentResolver.update failed: ${e.message}")
        }

        // 3. Direct File rename fallback (e.g., file:// URI or direct access path)
        try {
            val oldFile = if (uri.scheme == "file") {
                uri.path?.let { File(it) }
            } else if (item.folderPath.isNotEmpty()) {
                val direct = File(item.folderPath, item.title)
                if (direct.exists()) direct else {
                    File(item.folderPath).listFiles()?.firstOrNull { it.nameWithoutExtension == item.title }
                }
            } else null

            if (oldFile != null && oldFile.exists()) {
                val newFile = File(oldFile.parentFile, newDisplayName)
                if (oldFile.renameTo(newFile)) {
                    MediaScannerConnection.scanFile(context, arrayOf(newFile.absolutePath), null, null)
                    val updatedItem = item.copy(
                        uri = if (uri.scheme == "file") Uri.fromFile(newFile) else uri,
                        title = newTitle
                    )
                    updateCachedItem(item, updatedItem)
                    return@withContext updatedItem
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "File rename fallback failed: ${e.message}")
        }

        null
    }

    suspend fun completeRenameAfterConsent(
        item: MediaItemModel,
        newDisplayName: String
    ): MediaItemModel? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val newTitle = if (newDisplayName.contains(".")) newDisplayName.substringBeforeLast(".") else newDisplayName
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
                put(MediaStore.MediaColumns.TITLE, newTitle)
            }
            val rows = contentResolver.update(item.uri, values, null, null)
            if (rows > 0) {
                val updatedItem = item.copy(title = newTitle)
                updateCachedItem(item, updatedItem)
                return@withContext updatedItem
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to complete rename after consent", e)
        }
        null
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
                val parsedMime = obj.optString("mimeType", "")
                val rawBadge = obj.optString("codecBadge", "")
                val parsedTitle = obj.getString("title")
                val resolvedBadge = if (!isVideo && (rawBadge.isBlank() || rawBadge == "PCM Audio")) {
                    formatAudioCodecBadge(parsedMime, parsedTitle)
                } else {
                    rawBadge
                }
                list.add(
                    MediaItemModel(
                        id = obj.getLong("id"),
                        uri = parsedUri,
                        title = parsedTitle,
                        artist = sanitizeMetadata(obj.optString("artist", "Unknown Artist"), "Unknown Artist"),
                        album = sanitizeMetadata(obj.optString("album", "Unknown Album"), "Unknown Album"),
                        durationMs = obj.optLong("durationMs", 0L),
                        artworkUri = sanitizedArtUri,
                        isVideo = isVideo,
                        mimeType = parsedMime,
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        sampleRate = obj.optInt("sampleRate", 44100),
                        bitDepth = obj.optInt("bitDepth", 16),
                        channels = obj.optInt("channels", 2),
                        resolution = obj.optString("resolution", ""),
                        folderName = obj.optString("folderName", ""),
                        folderPath = obj.optString("folderPath", ""),
                        codecBadge = resolvedBadge,
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
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Track"
                    val artist = sanitizeMetadata(cursor.getString(artistCol), "Unknown Artist")
                    val album = sanitizeMetadata(cursor.getString(albumCol), "Unknown Album")
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
                    val codecInfo = formatAudioCodecBadge(mime, rawPath ?: title)

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
        val retriever = MediaMetadataRetriever()
        try {
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
                codecBadge = if (!isVideo) formatAudioCodecBadge(mime, uri.lastPathSegment.orEmpty()) else ""
            )
        } catch (e: Exception) {
            Log.e(tag, "Error importing SAF document", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
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
                                codecBadge = formatAudioCodecBadge(mime, name)
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

    private fun sanitizeMetadata(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        val trimmed = raw.trim()
        return if (trimmed.equals("<unknown>", ignoreCase = true) || trimmed.equals("unknown", ignoreCase = true)) {
            fallback
        } else {
            trimmed
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

    companion object {
        internal fun formatAudioCodecBadge(mimeType: String, filePathOrName: String = ""): String {
            val ext = filePathOrName.substringAfterLast('.', "").lowercase()
            when (ext) {
                "opus" -> return "Opus"
                "flac" -> return "FLAC Hi-Res"
                "alac" -> return "ALAC Lossless"
                "wav" -> return "WAV PCM"
                "dsd", "dsf", "dff" -> return "DSD Direct"
                "mp3" -> return "MP3"
                "aac" -> return "AAC"
                "m4a", "mp4" -> return "AAC / M4A"
                "ogg", "oga" -> return "OGG Vorbis"
                "wma" -> return "WMA"
                "mka" -> return "MKA"
                "ac3" -> return "AC3"
                "eac3" -> return "E-AC3"
                "dts" -> return "DTS"
            }
            val lowerMime = mimeType.lowercase()
            return when {
                lowerMime.contains("flac") -> "FLAC Hi-Res"
                lowerMime.contains("alac") -> "ALAC Lossless"
                lowerMime.contains("wav") || lowerMime.contains("wave") || lowerMime.contains("lpcm") -> "WAV PCM"
                lowerMime.contains("dsd") -> "DSD Direct"
                lowerMime.contains("opus") -> "Opus"
                lowerMime.contains("mpeg") || lowerMime.contains("mp3") -> "MP3"
                lowerMime.contains("aac") || lowerMime.contains("mp4a") || lowerMime == "audio/mp4" -> "AAC"
                lowerMime.contains("ogg") || lowerMime.contains("vorbis") -> "OGG"
                lowerMime.contains("webm") -> "WebM Audio"
                lowerMime.contains("eac3") || lowerMime.contains("ec-3") -> "E-AC3"
                lowerMime.contains("ac3") -> "AC3"
                lowerMime.contains("dts") -> "DTS"
                lowerMime.contains("amr") -> "AMR"
                lowerMime.startsWith("audio/") && !lowerMime.endsWith("/*") -> {
                    lowerMime.substringAfter("audio/").removePrefix("x-").uppercase()
                }
                else -> "Audio"
            }
        }
    }
}

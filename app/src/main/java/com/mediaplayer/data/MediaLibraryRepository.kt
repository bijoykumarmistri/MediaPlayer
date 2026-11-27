package com.mediaplayer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

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

    fun takePersistableReadPermission(uri: Uri): Boolean {
        if (!uri.scheme.equals("content", ignoreCase = true)) return false
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        } catch (e: SecurityException) {
            Log.w(tag, "URI does not support persistable read permission: $uri (${e.message})")
            false
        } catch (e: Throwable) {
            Log.w(tag, "Failed to persist read permission for $uri", e)
            false
        }
    }

    fun hasUriReadAccess(
        uri: Uri,
        persistedPermissions: List<UriPermission>? = null
    ): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "content") return true
        val authority = uri.authority?.lowercase(Locale.ROOT)
        if (authority == MediaStore.AUTHORITY) return true

        val permissions = persistedPermissions ?: runCatching {
            context.contentResolver.persistedUriPermissions
        }.getOrElse { emptyList() }

        val uriTreeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        val hasPersisted = permissions.any { perm ->
            if (!perm.isReadPermission) return@any false
            val permUri = perm.uri
            if (permUri == uri) return@any true
            if (permUri.authority.equals(uri.authority, ignoreCase = true) &&
                !uriTreeId.isNullOrEmpty() &&
                DocumentsContract.isTreeUri(permUri)
            ) {
                val permTreeId = runCatching { DocumentsContract.getTreeDocumentId(permUri) }.getOrNull()
                permTreeId == uriTreeId
            } else {
                false
            }
        }
        if (hasPersisted) return true

        return runCatching {
            context.checkUriPermission(
                uri,
                Process.myPid(),
                Process.myUid(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    private fun readCacheFromFile(file: File): List<MediaItemModel> {
        if (!file.exists() || (file.length() == 0L)) return emptyList()
        return try {
            val persistedPermissions = runCatching {
                context.contentResolver.persistedUriPermissions
            }.getOrElse { emptyList() }
            var modifiedCache = false
            val seenIds = HashSet<Long>()
            val seenUris = HashSet<String>()
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = ArrayList<MediaItemModel>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val parsedId = obj.getLong("id")
                val rawUri = obj.getString("uri")
                val parsedUri = Uri.parse(rawUri)
                if (!hasUriReadAccess(parsedUri, persistedPermissions)) {
                    modifiedCache = true
                    continue
                }
                if (!seenUris.add(rawUri) || !seenIds.add(parsedId)) {
                    modifiedCache = true
                    continue
                }
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
                        id = parsedId,
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
            if (modifiedCache) {
                Log.i(tag, "Cleaned expired/duplicate entries from ${file.name}; remaining=${list.size}")
                saveCacheToFile(file, list)
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
        val mediaStorePaths = HashSet<String>()
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
                    if (!rawPath.isNullOrBlank()) {
                        mediaStorePaths.add(rawPath.lowercase(Locale.ROOT))
                    }
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
            com.mediaplayer.util.DiagnosticLogManager.recordEvent("MediaLibrary", "MediaStore audio error: ${e.message}")
        }

        val persistedSafAudios = collectPersistedSafItems(wantVideo = false)
        val cachedAccessibleSafAudios = getCachedAudios().filter { isExternalSafItemAccessible(it) }
        val merged = mergeUniqueItems(
            newItems = persistedSafAudios + cachedAccessibleSafAudios,
            existingItems = audioList,
            existingAbsolutePaths = mediaStorePaths
        )

        if (merged.isNotEmpty()) {
            inMemoryAudios = merged
            saveCacheToFile(audioCacheFile, merged)
            merged
        } else {
            getCachedAudios()
        }
    }

    suspend fun getVideoList(): List<MediaItemModel> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<MediaItemModel>()
        val mediaStorePaths = HashSet<String>()
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
                    if (!rawPath.isNullOrBlank()) {
                        mediaStorePaths.add(rawPath.lowercase(Locale.ROOT))
                    }
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
            com.mediaplayer.util.DiagnosticLogManager.recordEvent("MediaLibrary", "MediaStore video error: ${e.message}")
        }

        val persistedSafVideos = collectPersistedSafItems(wantVideo = true)
        val cachedAccessibleSafVideos = getCachedVideos().filter { isExternalSafItemAccessible(it) }
        val merged = mergeUniqueItems(
            newItems = persistedSafVideos + cachedAccessibleSafVideos,
            existingItems = videoList,
            existingAbsolutePaths = mediaStorePaths
        )

        if (merged.isNotEmpty()) {
            inMemoryVideos = merged
            saveCacheToFile(videosCacheFile, merged)
            merged
        } else {
            getCachedVideos()
        }
    }

    fun mergeUniqueItems(
        newItems: List<MediaItemModel>,
        existingItems: List<MediaItemModel>,
        existingAbsolutePaths: Set<String> = emptySet()
    ): List<MediaItemModel> {
        if (newItems.isEmpty()) return existingItems
        val seenUris = HashSet<String>(existingItems.size + newItems.size)
        val seenIds = HashSet<Long>(existingItems.size + newItems.size)
        val seenTitleSize = HashSet<String>(existingItems.size + newItems.size)
        val seenPaths = HashSet<String>(existingAbsolutePaths)

        for (existing in existingItems) {
            seenUris.add(existing.uri.toString())
            seenIds.add(existing.id)
            if (existing.sizeBytes > 0L) {
                seenTitleSize.add("${existing.title.lowercase(Locale.ROOT)}|${existing.sizeBytes}")
            }
            extractPrimaryAbsolutePath(existing.uri)?.let { seenPaths.add(it.lowercase(Locale.ROOT)) }
        }

        val uniqueAdditions = mutableListOf<MediaItemModel>()
        for (candidate in newItems) {
            val uriKey = candidate.uri.toString()
            val pathKey = extractPrimaryAbsolutePath(candidate.uri)?.lowercase(Locale.ROOT)
            val titleSizeKey = if (candidate.sizeBytes > 0L) {
                "${candidate.title.lowercase(Locale.ROOT)}|${candidate.sizeBytes}"
            } else {
                null
            }
            val alreadyExists = uriKey in seenUris ||
                candidate.id in seenIds ||
                (pathKey != null && pathKey in seenPaths) ||
                (titleSizeKey != null && titleSizeKey in seenTitleSize)
            if (!alreadyExists) {
                uniqueAdditions.add(candidate)
                seenUris.add(uriKey)
                seenIds.add(candidate.id)
                if (pathKey != null) seenPaths.add(pathKey)
                if (titleSizeKey != null) seenTitleSize.add(titleSizeKey)
            }
        }
        return if (uniqueAdditions.isEmpty()) existingItems else uniqueAdditions + existingItems
    }

    private fun isExternalSafItemAccessible(item: MediaItemModel): Boolean {
        val uri = item.uri
        if (!uri.scheme.equals("content", ignoreCase = true)) return false
        val authority = uri.authority?.lowercase(Locale.ROOT)
        if (authority == MediaStore.AUTHORITY) return false
        return hasUriReadAccess(uri)
    }

    private suspend fun collectPersistedSafItems(wantVideo: Boolean): List<MediaItemModel> {
        val permissions = runCatching {
            context.contentResolver.persistedUriPermissions
        }.getOrElse { return emptyList() }

        val result = mutableListOf<MediaItemModel>()
        for (perm in permissions) {
            if (!perm.isReadPermission) continue
            if (DocumentsContract.isTreeUri(perm.uri)) {
                val (audios, videos) = scanSafFolder(perm.uri)
                result.addAll(if (wantVideo) videos else audios)
            }
        }
        return result
    }

    private fun extractPrimaryAbsolutePath(uri: Uri): String? {
        if (!uri.authority.equals("com.android.externalstorage.documents", ignoreCase = true)) return null
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: run {
                val segments = uri.pathSegments
                val docIndex = segments.indexOf("document")
                if (docIndex >= 0 && docIndex + 1 < segments.size) segments[docIndex + 1] else null
            } ?: return null
        if (!docId.startsWith("primary:", ignoreCase = true)) return null
        val relative = docId.substringAfter(':').trimStart('/')
        if (relative.isEmpty() || relative.split('/', '\\').any { it == ".." }) return null
        return File(Environment.getExternalStorageDirectory(), relative).absolutePath
    }

    suspend fun importSafSingleFile(
        uri: Uri,
        fallbackToVideoIfUnknown: Boolean = false
    ): MediaItemModel? = withContext(Dispatchers.IO) {
        var queriedDisplayName: String? = null
        var queriedSize = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        queriedDisplayName = cursor.getString(nameIdx)
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        queriedSize = cursor.getLong(sizeIdx)
                    }
                }
            }
        }
        val resolverMime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()

        var retrieverHasVideo: Boolean? = null
        var retrieverTitle: String? = null
        var retrieverArtist: String? = null
        var retrieverAlbum: String? = null
        var retrieverDuration = 0L
        var retrieverMime: String? = null
        var retrieverWidth = 0
        var retrieverHeight = 0

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retrieverHasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
            retrieverTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            retrieverArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            retrieverAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            retrieverDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retrieverMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            retrieverWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            retrieverHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.w(tag, "MediaMetadataRetriever failed for $uri, using fallback metadata: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }

        val fallbackName = queriedDisplayName
            ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            ?: "Imported Media"
        val resolvedMime = retrieverMime?.takeIf { it.isNotBlank() } ?: resolverMime
        val mediaKind = classifyMediaKind(resolvedMime, fallbackName)
        val isVideo = when {
            retrieverHasVideo == true -> true
            mediaKind == MediaKind.VIDEO -> true
            mediaKind == MediaKind.AUDIO -> false
            retrieverHasVideo == false -> false
            fallbackToVideoIfUnknown -> true
            else -> return@withContext null
        }

        val cleanTitle = retrieverTitle?.takeIf { it.isNotBlank() }
            ?: cleanSafTitle(fallbackName)
        val artist = sanitizeMetadata(retrieverArtist, "Unknown Artist")
        val album = sanitizeMetadata(retrieverAlbum, "Unknown Album")
        val effectiveMime = resolvedMime.ifBlank { if (isVideo) "video/*" else "audio/*" }
        val resolution = if (isVideo) formatVideoResolution(retrieverWidth, retrieverHeight) else ""
        val primaryFolderPath = extractPrimaryAbsolutePath(uri)?.let { File(it).parent }.orEmpty()
        val folderName = primaryFolderPath.takeIf { it.isNotEmpty() }?.let { File(it).name } ?: "Imported Files"

        MediaItemModel(
            id = stableSafItemId(uri.toString()),
            uri = uri,
            title = cleanTitle,
            artist = artist,
            album = album,
            durationMs = retrieverDuration,
            isVideo = isVideo,
            mimeType = effectiveMime,
            sizeBytes = queriedSize,
            resolution = resolution,
            folderName = folderName,
            folderPath = primaryFolderPath,
            artworkUri = if (isVideo) uri else null,
            codecBadge = if (!isVideo) formatAudioCodecBadge(effectiveMime, fallbackName) else ""
        )
    }

    suspend fun scanSafFolder(treeUri: Uri): Pair<List<MediaItemModel>, List<MediaItemModel>> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<MediaItemModel>()
        val videoList = mutableListOf<MediaItemModel>()

        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootFolderName = extractFolderNameFromTreeDocId(rootDocId)
            val visitedDocIds = HashSet<String>()
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            fun scanDirectory(docId: String, currentFolderName: String, depth: Int) {
                if (!visitedDocIds.add(docId)) return
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    if (idCol < 0 || nameCol < 0 || mimeCol < 0) return@use

                    while (cursor.moveToNext()) {
                        val childDocId = cursor.getString(idCol) ?: continue
                        val name = cursor.getString(nameCol) ?: "Media"
                        val mime = cursor.getString(mimeCol) ?: ""
                        val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (depth < 4) {
                                scanDirectory(childDocId, name.ifBlank { currentFolderName }, depth + 1)
                            }
                            continue
                        }

                        val kind = classifyMediaKind(mime, name) ?: continue
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val resolvedMime = mime.ifBlank {
                            if (kind == MediaKind.VIDEO) "video/*" else "audio/*"
                        }
                        val folderPath = extractPrimaryAbsolutePath(docUri)?.let { File(it).parent }
                            ?: treeUri.toString()

                        when (kind) {
                            MediaKind.AUDIO -> {
                                audioList.add(
                                    MediaItemModel(
                                        id = stableSafItemId(docUri.toString()),
                                        uri = docUri,
                                        title = cleanSafTitle(name),
                                        artist = currentFolderName,
                                        album = "Local Storage",
                                        isVideo = false,
                                        mimeType = resolvedMime,
                                        sizeBytes = size,
                                        folderName = currentFolderName,
                                        folderPath = folderPath,
                                        artworkUri = null,
                                        codecBadge = formatAudioCodecBadge(resolvedMime, name)
                                    )
                                )
                            }
                            MediaKind.VIDEO -> {
                                videoList.add(
                                    MediaItemModel(
                                        id = stableSafItemId(docUri.toString()),
                                        uri = docUri,
                                        title = cleanSafTitle(name),
                                        isVideo = true,
                                        mimeType = resolvedMime,
                                        sizeBytes = size,
                                        folderName = currentFolderName,
                                        folderPath = folderPath,
                                        artworkUri = docUri
                                    )
                                )
                            }
                        }
                    }
                }
            }

            scanDirectory(rootDocId, rootFolderName, 0)
        } catch (e: Exception) {
            Log.e(tag, "Error scanning SAF tree: $treeUri", e)
            com.mediaplayer.util.DiagnosticLogManager.recordEvent("MediaLibrary", "SAF scan error (${treeUri.authority}): ${e.message}")
        }

        Pair(audioList, videoList)
    }

    private fun cleanSafTitle(name: String): String {
        val trimmed = name.trim()
        val beforeDot = trimmed.substringBeforeLast('.')
        return if (beforeDot.isNotBlank()) beforeDot else trimmed.ifBlank { "Imported Media" }
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

    internal enum class MediaKind {
        AUDIO,
        VIDEO
    }

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "ts", "m2ts", "mts",
            "m4v", "3gp", "flv", "wmv", "vob", "mpg", "mpeg", "ogv", "rmvb"
        )
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "m4a", "ogg", "oga", "opus",
            "wma", "alac", "aiff", "dsd", "dsf", "dff", "mka", "ac3", "eac3", "dts", "amr"
        )

        internal fun classifyMediaKind(mimeType: String, fileName: String): MediaKind? {
            val lowerMime = mimeType.lowercase(Locale.ROOT)
            if (lowerMime.startsWith("video/")) return MediaKind.VIDEO
            if (lowerMime.startsWith("audio/")) return MediaKind.AUDIO
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return when {
                ext in VIDEO_EXTENSIONS -> MediaKind.VIDEO
                ext in AUDIO_EXTENSIONS -> MediaKind.AUDIO
                lowerMime == "application/x-matroska" -> MediaKind.VIDEO
                lowerMime == "application/ogg" -> MediaKind.AUDIO
                else -> null
            }
        }

        internal fun extractFolderNameFromTreeDocId(docId: String?): String {
            if (docId.isNullOrBlank()) return "Imported Folder"
            val afterColon = docId.substringAfter(':', docId).trimEnd('/')
            val lastSegment = afterColon.substringAfterLast('/').trim()
            return if (lastSegment.isNotBlank()) lastSegment else "Imported Folder"
        }

        internal fun stableSafItemId(key: String): Long {
            var h = -0x340d631b7bdddcdbL
            for (ch in key) {
                h = h xor ch.code.toLong()
                h *= 0x100000001b3L
            }
            return (h and 0x3FFF_FFFF_FFFF_FFFFL) or 0x4000_0000_0000_0000L
        }

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

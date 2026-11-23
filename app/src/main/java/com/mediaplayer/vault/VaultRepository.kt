package com.mediaplayer.vault

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.mediaplayer.data.MediaItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class VaultRepository(context: Context) {

    private val tag = "VaultRepository"
    private val context: Context = context.applicationContext

    val vaultDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, ".secure_vault").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val nomedia = File(dir, ".nomedia")
            if (!nomedia.exists()) {
                try {
                    nomedia.createNewFile()
                } catch (e: Exception) {
                    Log.w(tag, "Failed to create .nomedia", e)
                }
            }
            return dir
        }

    private val metadataFile: File = File(context.filesDir, "vault_media.json")

    @Volatile
    private var inMemoryItems: List<VaultMediaItem>? = null

    suspend fun getVaultItems(): List<VaultMediaItem> = withContext(Dispatchers.IO) {
        val cached = inMemoryItems
        if (cached != null) return@withContext cached
        val loaded = loadFromDisk()
        inMemoryItems = loaded
        loaded
    }

    fun getVaultItemsSync(): List<VaultMediaItem> {
        return inMemoryItems ?: loadFromDisk().also { inMemoryItems = it }
    }

    fun findValidVaultFile(item: VaultMediaItem): File? {
        val direct = File(item.vaultFilePath)
        if (direct.exists() && direct.length() > 0L) return direct

        val inVaultDir = File(vaultDir, direct.name)
        if (inVaultDir.exists() && inVaultDir.length() > 0L) return inVaultDir

        // Search by UUID prefix in vault directory
        if (item.id.isNotEmpty()) {
            val matchById = vaultDir.listFiles()?.firstOrNull {
                it.name.startsWith(item.id) && it.length() > 0L && !it.name.endsWith(".tmp")
            }
            if (matchById != null) return matchById
        }

        // Search by original file name
        if (item.originalFileName.isNotEmpty()) {
            val matchByName = vaultDir.listFiles()?.firstOrNull {
                it.name.endsWith(item.originalFileName) && it.length() > 0L && !it.name.endsWith(".tmp")
            }
            if (matchByName != null) return matchByName
        }

        // Check if a completed .tmp file exists
        val tmpMatch = vaultDir.listFiles()?.firstOrNull {
            it.name.startsWith(item.id) && it.name.endsWith(".tmp") && it.length() > 0L
        }
        if (tmpMatch != null) {
            val proper = File(vaultDir, "${item.id}_${item.originalFileName.ifEmpty { "media" }}")
            if (tmpMatch.renameTo(proper) && proper.exists() && proper.length() > 0L) {
                return proper
            }
            return tmpMatch
        }

        // Check legacy public vault directory
        try {
            val legacyDir = File(Environment.getExternalStorageDirectory(), ".mediaplayer_vault")
            if (legacyDir.exists() && legacyDir.canRead()) {
                val legacyDirect = File(legacyDir, direct.name)
                if (legacyDirect.exists() && legacyDirect.length() > 0L) return legacyDirect

                if (item.id.isNotEmpty()) {
                    val legacyMatch = legacyDir.listFiles()?.firstOrNull {
                        it.name.startsWith(item.id) && it.length() > 0L
                    }
                    if (legacyMatch != null) return legacyMatch
                }
            }
        } catch (_: Exception) {}

        // Check if still in original folder
        if (item.originalFolderPath.isNotEmpty()) {
            val origFile = if (item.originalFileName.isNotEmpty()) {
                File(item.originalFolderPath, item.originalFileName)
            } else {
                File(item.originalFolderPath, item.title)
            }
            if (origFile.exists() && origFile.length() > 0L) return origFile

            // Check hidden dot file in original folder
            val cleanName = if (item.originalFileName.isNotEmpty()) item.originalFileName.removePrefix(".") else item.title.removePrefix(".")
            val hiddenFile = File(item.originalFolderPath, ".$cleanName")
            if (hiddenFile.exists() && hiddenFile.length() > 0L) return hiddenFile
        }

        return if (direct.exists()) direct else null
    }

    private fun loadFromDisk(): List<VaultMediaItem> {
        if (!metadataFile.exists() || metadataFile.length() == 0L) return emptyList()
        return try {
            val jsonStr = metadataFile.readText()
            val jsonArray = JSONArray(jsonStr)
            val items = mutableListOf<VaultMediaItem>()
            var needsSave = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                var item = VaultMediaItem.fromJson(obj)
                val resolved = findValidVaultFile(item)

                if (resolved != null && resolved.exists() && resolved.length() > 0L) {
                    if (resolved.absolutePath != item.vaultFilePath && !resolved.absolutePath.startsWith(item.originalFolderPath)) {
                        item = item.copy(vaultFilePath = resolved.absolutePath)
                        needsSave = true
                    }
                    items.add(item)
                } else {
                    // Keep item in vault list so user can see it and attempt restore or delete
                    items.add(item)
                }
            }

            if (needsSave) {
                saveToDisk(items)
            }

            items
        } catch (e: Exception) {
            Log.e(tag, "Failed to load vault metadata", e)
            emptyList()
        }
    }

    private fun saveToDisk(items: List<VaultMediaItem>) {
        try {
            val jsonArray = JSONArray()
            items.forEach { jsonArray.put(it.toJson()) }
            metadataFile.writeText(jsonArray.toString())
            inMemoryItems = items
        } catch (e: Exception) {
            Log.e(tag, "Failed to save vault metadata", e)
        }
    }

    private fun resolveSourceFile(item: MediaItemModel): File? {
        // 1. Direct file URI
        if (item.uri.scheme == "file") {
            item.uri.path?.let { p ->
                val f = File(p)
                if (f.exists() && f.isFile && f.length() > 0L) return f
            }
        }

        // 2. Query MediaStore DATA column
        try {
            val proj = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(item.uri, proj, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (dataIdx >= 0) {
                        val path = cursor.getString(dataIdx)
                        if (!path.isNullOrEmpty()) {
                            val f = File(path)
                            if (f.exists() && f.isFile && f.length() > 0L) return f
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to resolve file via DATA column: ${e.message}")
        }

        // 3. Match within folderPath if provided
        if (item.folderPath.isNotEmpty()) {
            val direct = File(item.folderPath, item.title)
            if (direct.exists() && direct.isFile && direct.length() > 0L) return direct

            val folder = File(item.folderPath)
            if (folder.exists() && folder.isDirectory) {
                val match = folder.listFiles()?.firstOrNull { f ->
                    f.isFile && f.length() > 0L && (f.name == item.title ||
                            f.nameWithoutExtension == item.title ||
                            f.name.equals(item.title, ignoreCase = true) ||
                            f.nameWithoutExtension.equals(item.title, ignoreCase = true))
                }
                if (match != null && match.exists() && match.length() > 0L) return match
            }
        }

        return null
    }

    private fun getRelativePathFromFolder(folderPath: String, isVideo: Boolean): String {
        if (folderPath.isEmpty()) {
            return if (isVideo) "${Environment.DIRECTORY_MOVIES}/" else "${Environment.DIRECTORY_MUSIC}/"
        }
        var clean = folderPath
            .replace(Regex("^/storage/emulated/\\d+/"), "")
            .replace(Regex("^/storage/[^/]+/"), "")
            .replace(Regex("^/sdcard/"), "")
            .trim('/')
        if (clean.isEmpty()) {
            clean = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
        }
        return "$clean/"
    }

    /**
     * Hides media files in-place by prepending a dot '.' to their filename in their original folder.
     * Keeps the file in its original location (0 bytes moved, 0ms instant speed),
     * completely hides it from Gallery (MediaStore ignores dot-prefixed files),
     * and hides it from other File Manager apps (standard Linux/Android hidden file).
     */
    suspend fun moveToVault(
        items: List<MediaItemModel>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<VaultMediaItem> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()
        val currentItems = getVaultItems().toMutableList()
        val successfullyMoved = mutableListOf<VaultMediaItem>()
        val total = items.size

        for ((index, item) in items.withIndex()) {
            try {
                val sourceFile = resolveSourceFile(item)
                val origFolderPath = sourceFile?.parent ?: item.folderPath.ifEmpty {
                    if (item.isVideo) "/storage/emulated/0/${Environment.DIRECTORY_MOVIES}"
                    else "/storage/emulated/0/${Environment.DIRECTORY_MUSIC}"
                }
                val origFolderName = sourceFile?.parentFile?.name ?: item.folderName.ifEmpty { "Media" }
                val origFileName = sourceFile?.name ?: if (item.title.contains('.')) item.title else {
                    val ext = when {
                        item.mimeType.contains("mp4") -> "mp4"
                        item.mimeType.contains("mkv") -> "mkv"
                        item.mimeType.contains("flac") -> "flac"
                        item.mimeType.contains("wav") -> "wav"
                        item.isVideo -> "mp4"
                        else -> "mp3"
                    }
                    "${item.title}.$ext"
                }

                val safeId = UUID.randomUUID().toString()

                // STRATEGY 1 (PRIMARY): Hide in-place in original folder without moving!
                var hideSuccess = false
                var finalVaultFile: File? = null

                if (sourceFile != null && sourceFile.exists() && sourceFile.length() > 0L) {
                    val parent = sourceFile.parentFile ?: File(origFolderPath)
                    val rawName = sourceFile.name.removePrefix(".")
                    val hiddenFileName = ".$rawName"
                    val hiddenFile = File(parent, hiddenFileName)

                    if (sourceFile.name.startsWith(".")) {
                        // Already hidden dot file
                        hideSuccess = true
                        finalVaultFile = sourceFile
                    } else {
                        try {
                            hideSuccess = sourceFile.renameTo(hiddenFile)
                            if (hideSuccess && hiddenFile.exists() && hiddenFile.length() > 0L) {
                                finalVaultFile = hiddenFile
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "In-place hide failed: ${e.message}")
                            hideSuccess = false
                        }
                    }
                }

                // STRATEGY 2 (FALLBACK): If in-place rename was not permitted on this volume,
                // securely store in vault directory
                if (!hideSuccess || finalVaultFile == null || !finalVaultFile.exists()) {
                    val destFile = File(vaultDir, "${safeId}_$origFileName")
                    val tempFile = File(vaultDir, "${safeId}_temp.tmp")

                    if (sourceFile != null && sourceFile.exists() && sourceFile.length() > 0L) {
                        try {
                            hideSuccess = sourceFile.renameTo(destFile)
                            if (hideSuccess && destFile.exists() && destFile.length() > 0L) {
                                finalVaultFile = destFile
                            }
                        } catch (_: Exception) {}
                    }

                    if (!hideSuccess) {
                        var bytesCopied = 0L
                        val inputStream = try {
                            context.contentResolver.openInputStream(item.uri)
                        } catch (_: Exception) { null }
                            ?: (if (sourceFile != null && sourceFile.exists()) {
                                try { FileInputStream(sourceFile) } catch (_: Exception) { null }
                            } else null)

                        if (inputStream != null) {
                            inputStream.use { inStream ->
                                FileOutputStream(tempFile).use { outStream ->
                                    val buffer = ByteArray(512 * 1024)
                                    var read: Int
                                    while (inStream.read(buffer).also { read = it } != -1) {
                                        outStream.write(buffer, 0, read)
                                        bytesCopied += read
                                    }
                                    outStream.flush()
                                }
                            }
                        }

                        if (tempFile.exists() && bytesCopied > 0L) {
                            if (destFile.exists()) destFile.delete()
                            if (tempFile.renameTo(destFile) && destFile.length() > 0L) {
                                hideSuccess = true
                                finalVaultFile = destFile
                            }
                        }

                        if (!hideSuccess && tempFile.exists()) {
                            tempFile.delete()
                        }
                    }
                }

                if (hideSuccess && finalVaultFile != null && finalVaultFile.exists() && finalVaultFile.length() > 0L) {
                    // Tell scanner that old unhidden file is removed from public media storage
                    if (sourceFile != null && sourceFile.absolutePath != finalVaultFile.absolutePath) {
                        try {
                            MediaScannerConnection.scanFile(context, arrayOf(sourceFile.absolutePath), null, null)
                        } catch (_: Exception) {}
                    }

                    // Delete from MediaStore index so it vanishes from public media immediately
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (e: Exception) {
                        Log.w(tag, "Could not delete MediaStore entry directly: ${e.message}")
                    }

                    val cleanOrigFileName = if (origFileName.startsWith(".")) origFileName.removePrefix(".") else origFileName

                    val vaultItem = VaultMediaItem(
                        id = safeId,
                        title = item.title.removePrefix("."),
                        originalUri = item.uri.toString(),
                        vaultFilePath = finalVaultFile.absolutePath,
                        isVideo = item.isVideo,
                        mimeType = item.mimeType,
                        durationMs = item.durationMs,
                        sizeBytes = finalVaultFile.length(),
                        dateLocked = System.currentTimeMillis(),
                        resolution = item.resolution,
                        originalFolderName = origFolderName,
                        originalFolderPath = origFolderPath,
                        originalFileName = cleanOrigFileName,
                        artist = item.artist,
                        album = item.album
                    )

                    successfullyMoved.add(vaultItem)
                    currentItems.add(0, vaultItem)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error hiding item ${item.title}", e)
            }
            onProgress(index + 1, total)
        }

        if (successfullyMoved.isNotEmpty()) {
            saveToDisk(currentItems)
        }

        successfullyMoved
    }

    /**
     * Restores/Unhides a file from the vault directly back to its original media folder location.
     * Instant in-place unhide if hidden as dot-file, or complete write for moved files.
     */
    suspend fun restoreFromVault(item: VaultMediaItem): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val fileName = item.originalFileName.ifEmpty {
                val ext = when {
                    item.mimeType.contains("mp4") -> "mp4"
                    item.mimeType.contains("mkv") -> "mkv"
                    item.mimeType.contains("flac") -> "flac"
                    item.mimeType.contains("wav") -> "wav"
                    item.isVideo -> "mp4"
                    else -> "mp3"
                }
                if (item.title.contains('.')) item.title else "${item.title}.$ext"
            }.removePrefix(".")

            val origFolder = if (item.originalFolderPath.isNotEmpty()) {
                File(item.originalFolderPath)
            } else {
                val defDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
                Environment.getExternalStoragePublicDirectory(defDir)
            }

            if (!origFolder.exists()) {
                try { origFolder.mkdirs() } catch (_: Exception) {}
            }

            val targetFile = File(origFolder, fileName)

            // Exact standard MIME type (never wildcard)
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val safeMime = when (ext) {
                "mp4", "m4v" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "avi" -> "video/avi"
                "mov" -> "video/quicktime"
                "ts" -> "video/mp2t"
                "flv" -> "video/x-flv"
                "3gp" -> "video/3gpp"
                "mp3" -> "audio/mpeg"
                "flac" -> "audio/flac"
                "wav" -> "audio/wav"
                "m4a", "aac" -> "audio/mp4"
                "ogg", "opus" -> "audio/ogg"
                else -> if (item.isVideo) "video/mp4" else "audio/mpeg"
            }

            // Strategy 0: In-place unhide! If the file is hidden in-place as a dot-file (e.g. .movie.mp4)
            val currentVaultFile = findValidVaultFile(item) ?: File(item.vaultFilePath)
            if (currentVaultFile.exists() && currentVaultFile.name.startsWith(".")) {
                var unhidden = false
                try {
                    unhidden = currentVaultFile.renameTo(targetFile)
                } catch (e: Exception) {
                    Log.w(tag, "In-place unhide failed: ${e.message}")
                    unhidden = false
                }
                if (unhidden && targetFile.exists() && targetFile.length() > 0L) {
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(safeMime), null)
                    val updated = getVaultItems().filter { it.id != item.id }
                    saveToDisk(updated)
                    return@withContext true
                }
            }

            // 1. Check if the original intact file is ALREADY in destination folder
            if (targetFile.exists() && targetFile.length() > 0L) {
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(safeMime), null)
                val vaultFile = findValidVaultFile(item)
                if (vaultFile != null && vaultFile.absolutePath != targetFile.absolutePath) {
                    try { vaultFile.delete() } catch (_: Exception) {}
                }
                val updated = getVaultItems().filter { it.id != item.id }
                saveToDisk(updated)
                return@withContext true
            }

            // 2. Remove dead 0-byte file at destination if left from earlier failed attempts
            if (targetFile.exists() && targetFile.length() == 0L) {
                try { targetFile.delete() } catch (_: Exception) {}
            }

            // 3. Find the valid vault file
            val validVaultFile = findValidVaultFile(item)
            if (validVaultFile == null || !validVaultFile.exists() || validVaultFile.length() == 0L) {
                Log.e(tag, "Vault file missing or 0 bytes: ${item.vaultFilePath}")
                return@withContext false
            }

            var restored = false

            // Strategy 1: Instant direct rename if filesystem allows (0 ms, MX Player style)
            if (targetFile.parentFile?.canWrite() == true) {
                try {
                    restored = validVaultFile.renameTo(targetFile)
                } catch (_: Exception) {
                    restored = false
                }
            }

            // Strategy 2: Direct file copy via temp file if destination folder is directly writable
            if (!restored && targetFile.parentFile?.canWrite() == true) {
                val tempDest = File(origFolder, ".restoring_${System.currentTimeMillis()}_$fileName")
                try {
                    var bytes = 0L
                    FileOutputStream(tempDest).use { out ->
                        FileInputStream(validVaultFile).use { input ->
                            val buf = ByteArray(512 * 1024)
                            var r: Int
                            while (input.read(buf).also { r = it } != -1) {
                                out.write(buf, 0, r)
                                bytes += r
                            }
                            out.flush()
                        }
                    }
                    if (tempDest.exists() && bytes > 0L) {
                        if (targetFile.exists()) targetFile.delete()
                        if (tempDest.renameTo(targetFile) && targetFile.exists() && targetFile.length() > 0L) {
                            restored = true
                        }
                    }
                } catch (_: Exception) {
                    restored = false
                } finally {
                    if (!restored && tempDest.exists()) {
                        try { tempDest.delete() } catch (_: Exception) {}
                    }
                }
            }

            // If Strategy 1 or Strategy 2 succeeded, scan file and complete
            if (restored && targetFile.exists() && targetFile.length() > 0L) {
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(safeMime), null)
                if (validVaultFile.exists() && validVaultFile.absolutePath != targetFile.absolutePath) {
                    try { validVaultFile.delete() } catch (_: Exception) {}
                }
                val updated = getVaultItems().filter { it.id != item.id }
                saveToDisk(updated)
                return@withContext true
            }

            // Strategy 3: Android MediaStore insert with IS_PENDING handling
            val contentResolver = context.contentResolver
            val relPath = getRelativePathFromFolder(item.originalFolderPath, item.isVideo)
            val isDownload = relPath.startsWith("Download", ignoreCase = true)

            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isDownload) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else if (item.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val safeRelPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (contentUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI &&
                    !relPath.startsWith("DCIM", true) && !relPath.startsWith("Movies", true) && !relPath.startsWith("Pictures", true)
                ) {
                    "${Environment.DIRECTORY_MOVIES}/"
                } else if (contentUri == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI &&
                    !relPath.startsWith("Music", true) && !relPath.startsWith("Podcasts", true) && !relPath.startsWith("Ringtones", true)
                ) {
                    "${Environment.DIRECTORY_MUSIC}/"
                } else {
                    relPath
                }
            } else {
                relPath
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, safeMime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, safeRelPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val targetUri = try {
                contentResolver.insert(contentUri, values)
            } catch (e: Exception) {
                Log.e(tag, "contentResolver.insert failed: ${e.message}")
                null
            }

            if (targetUri != null) {
                var bytesWritten = 0L
                val outStream = contentResolver.openOutputStream(targetUri, "w")
                if (outStream != null) {
                    outStream.use { out ->
                        FileInputStream(validVaultFile).use { inStream ->
                            val buffer = ByteArray(512 * 1024)
                            var read: Int
                            while (inStream.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                                bytesWritten += read
                            }
                            out.flush()
                        }
                    }
                }

                if (bytesWritten > 0L) {
                    // CRITICAL: Clear IS_PENDING on Android 10+ so other players (MX Player, VLC, Gallery) can immediately open and play the file!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val finishValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        try {
                            contentResolver.update(targetUri, finishValues, null, null)
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to update IS_PENDING: ${e.message}")
                        }
                    }

                    // Delete the vault file once restoration is verified
                    if (validVaultFile.exists()) {
                        try { validVaultFile.delete() } catch (_: Exception) {}
                    }
                    val updated = getVaultItems().filter { it.id != item.id }
                    saveToDisk(updated)

                    // Notify MediaScanner on real file path
                    try {
                        val proj = arrayOf(MediaStore.MediaColumns.DATA)
                        contentResolver.query(targetUri, proj, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                                if (dataIdx >= 0) {
                                    val restoredPath = cursor.getString(dataIdx)
                                    if (!restoredPath.isNullOrEmpty()) {
                                        MediaScannerConnection.scanFile(context, arrayOf(restoredPath), arrayOf(safeMime), null)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    true
                } else {
                    // Write failed, clean up pending entry
                    try { contentResolver.delete(targetUri, null, null) } catch (_: Exception) {}
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error restoring item from vault: ${item.title}", e)
            false
        }
    }

    /**
     * Permanently deletes a file from the vault.
     */
    suspend fun deletePermanently(item: VaultMediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = findValidVaultFile(item) ?: File(item.vaultFilePath)
            if (file.exists()) {
                file.delete()
            }
            val updated = getVaultItems().filter { it.id != item.id }
            saveToDisk(updated)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting item from vault: ${item.title}", e)
            false
        }
    }
}

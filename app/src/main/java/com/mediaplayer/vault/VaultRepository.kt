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

class VaultRepository(private val context: Context) {

    private val tag = "VaultRepository"

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
                var f = File(item.vaultFilePath)

                // If file is not accessible at recorded path, check candidate locations
                if (!f.exists() || f.length() == 0L) {
                    val candidatePublic = File(Environment.getExternalStorageDirectory(), ".mediaplayer_vault/${f.name}")
                    val candidatePrivate = File(vaultDir, f.name)

                    if (candidatePublic.exists() && candidatePublic.length() > 0L) {
                        try {
                            if (!candidatePrivate.exists()) {
                                if (!candidatePublic.renameTo(candidatePrivate)) {
                                    FileInputStream(candidatePublic).use { input ->
                                        FileOutputStream(candidatePrivate).use { output ->
                                            input.copyTo(output, bufferSize = 512 * 1024)
                                        }
                                    }
                                    if (candidatePrivate.exists() && candidatePrivate.length() > 0L) {
                                        candidatePublic.delete()
                                    }
                                }
                            }
                            if (candidatePrivate.exists() && candidatePrivate.length() > 0L) {
                                item = item.copy(vaultFilePath = candidatePrivate.absolutePath)
                                f = candidatePrivate
                                needsSave = true
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to migrate file to vaultDir: ${e.message}")
                        }
                    } else if (candidatePrivate.exists() && candidatePrivate.length() > 0L) {
                        item = item.copy(vaultFilePath = candidatePrivate.absolutePath)
                        f = candidatePrivate
                        needsSave = true
                    }
                }

                if (f.exists() && f.length() > 0L) {
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
     * Moves media files into the private vault directory.
     * Uses atomic temporary files to guarantee complete, uncorrupted files.
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
                val destFile = File(vaultDir, "${safeId}_$origFileName")
                val tempFile = File(vaultDir, "${safeId}_temp.tmp")

                var moveSuccess = false

                // 1. Try instant rename if possible (same directory/mount)
                if (sourceFile != null && sourceFile.exists() && sourceFile.length() > 0L) {
                    try {
                        moveSuccess = sourceFile.renameTo(destFile)
                    } catch (_: Exception) {
                        moveSuccess = false
                    }
                }

                // 2. If rename didn't work across boundaries, copy via stream to temp file
                if (!moveSuccess) {
                    var bytesCopied = 0L
                    val inputStream = context.contentResolver.openInputStream(item.uri)
                        ?: (if (sourceFile != null && sourceFile.exists()) FileInputStream(sourceFile) else null)

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
                            moveSuccess = true
                        }
                    }

                    if (!moveSuccess && tempFile.exists()) {
                        tempFile.delete()
                    }
                }

                if (moveSuccess && destFile.exists() && destFile.length() > 0L) {
                    // Tell scanner that old file was removed from public storage so it vanishes from gallery
                    if (sourceFile != null) {
                        try {
                            sourceFile.delete()
                            MediaScannerConnection.scanFile(context, arrayOf(sourceFile.absolutePath), null, null)
                        } catch (_: Exception) {}
                    }

                    // Delete from MediaStore index so it vanishes from public media immediately
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (e: Exception) {
                        Log.w(tag, "Could not delete MediaStore entry directly: ${e.message}")
                    }

                    val vaultItem = VaultMediaItem(
                        id = safeId,
                        title = item.title,
                        originalUri = item.uri.toString(),
                        vaultFilePath = destFile.absolutePath,
                        isVideo = item.isVideo,
                        mimeType = item.mimeType,
                        durationMs = item.durationMs,
                        sizeBytes = destFile.length(),
                        dateLocked = System.currentTimeMillis(),
                        resolution = item.resolution,
                        originalFolderName = origFolderName,
                        originalFolderPath = origFolderPath,
                        originalFileName = origFileName,
                        artist = item.artist,
                        album = item.album
                    )

                    successfullyMoved.add(vaultItem)
                    currentItems.add(0, vaultItem)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error moving item ${item.title} to vault", e)
            }
            onProgress(index + 1, total)
        }

        if (successfullyMoved.isNotEmpty()) {
            saveToDisk(currentItems)
        }

        successfullyMoved
    }

    /**
     * Restores a file from the vault directly back to its original media folder location.
     * Guaranteed complete write, IS_PENDING released, and scanned for all device players.
     */
    suspend fun restoreFromVault(item: VaultMediaItem): Boolean = withContext(Dispatchers.IO) {
        val vaultFile = File(item.vaultFilePath)
        val validVaultFile = if (vaultFile.exists() && vaultFile.length() > 0L) {
            vaultFile
        } else {
            val candidate = File(vaultDir, vaultFile.name)
            if (candidate.exists() && candidate.length() > 0L) candidate else null
        }
        if (validVaultFile == null || !validVaultFile.exists() || validVaultFile.length() == 0L) {
            Log.e(tag, "Cannot restore: vault file is missing or 0 bytes")
            return@withContext false
        }

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
            }

            val origFolder = if (item.originalFolderPath.isNotEmpty()) {
                File(item.originalFolderPath)
            } else {
                val defDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
                Environment.getExternalStoragePublicDirectory(defDir)
            }

            if (!origFolder.exists()) {
                try { origFolder.mkdirs() } catch (_: Exception) {}
            }

            val relPath = getRelativePathFromFolder(item.originalFolderPath, item.isVideo)
            val contentResolver = context.contentResolver
            val contentUri = if (item.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val mime = item.mimeType.ifEmpty {
                if (item.isVideo) "video/mp4" else "audio/mpeg"
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val targetUri = contentResolver.insert(contentUri, values)
            if (targetUri != null) {
                var bytesWritten = 0L
                val outStream = contentResolver.openOutputStream(targetUri)
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
                        contentResolver.update(targetUri, finishValues, null, null)
                    }

                    // Delete the vault file once restoration is verified
                    validVaultFile.delete()
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
                                        MediaScannerConnection.scanFile(context, arrayOf(restoredPath), arrayOf(mime), null)
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
            val file = File(item.vaultFilePath)
            if (file.exists()) {
                file.delete()
            }
            val inSecure = File(vaultDir, file.name)
            if (inSecure.exists()) {
                inSecure.delete()
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

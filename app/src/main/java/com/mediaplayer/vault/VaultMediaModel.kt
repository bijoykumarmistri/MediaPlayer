package com.mediaplayer.vault

import android.net.Uri
import android.os.Environment
import com.mediaplayer.data.MediaItemModel
import org.json.JSONObject
import java.io.File

data class VaultMediaItem(
    val id: String,
    val title: String,
    val originalUri: String,
    val vaultFilePath: String,
    val isVideo: Boolean,
    val mimeType: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateLocked: Long,
    val resolution: String = "",
    val originalFolderName: String = "Vault",
    val originalFolderPath: String = "",
    val originalFileName: String = "",
    val artist: String = "Unknown Artist",
    val album: String = "Privacy Vault",
) {
    fun toMediaItemModel(): MediaItemModel {
        val file = File(vaultFilePath)
        val validFile = when {
            (file.exists() && (file.length() > 0L)) -> file
            File(file.parentFile ?: File(""), file.name).let { it.exists() && it.length() > 0L } -> File(file.parentFile ?: File(""), file.name)
            File(Environment.getExternalStorageDirectory(), "Android/data/com.mediaplayer/files/.secure_vault/${file.name}").let { it.exists() && it.length() > 0L } ->
                File(Environment.getExternalStorageDirectory(), "Android/data/com.mediaplayer/files/.secure_vault/${file.name}")
            File(Environment.getExternalStorageDirectory(), ".mediaplayer_vault/${file.name}").let { it.exists() && it.length() > 0L } ->
                File(Environment.getExternalStorageDirectory(), ".mediaplayer_vault/${file.name}")
            originalFolderPath.isNotEmpty() && originalFileName.isNotEmpty() && File(originalFolderPath, originalFileName).let { it.exists() && it.length() > 0L } ->
                File(originalFolderPath, originalFileName)
            originalFolderPath.isNotEmpty() && File(originalFolderPath, title).let { it.exists() && it.length() > 0L } ->
                File(originalFolderPath, title)
            else -> file
        }

        return MediaItemModel(
            id = id.hashCode().toLong(),
            uri = Uri.fromFile(validFile),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            artworkUri = null,
            isVideo = isVideo,
            mimeType = "", // Keep empty for local file playback so ExoPlayer sniffs container headers automatically
            sizeBytes = if (validFile.exists()) validFile.length() else sizeBytes,
            resolution = resolution,
            folderName = "Privacy Vault",
            folderPath = validFile.parent ?: "",
            dateAdded = dateLocked
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("originalUri", originalUri)
            put("vaultFilePath", vaultFilePath)
            put("isVideo", isVideo)
            put("mimeType", mimeType)
            put("durationMs", durationMs)
            put("sizeBytes", sizeBytes)
            put("dateLocked", dateLocked)
            put("resolution", resolution)
            put("originalFolderName", originalFolderName)
            put("originalFolderPath", originalFolderPath)
            put("originalFileName", originalFileName)
            put("artist", artist)
            put("album", album)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): VaultMediaItem {
            return VaultMediaItem(
                id = json.optString("id", ""),
                title = json.optString("title", "Locked Media"),
                originalUri = json.optString("originalUri", ""),
                vaultFilePath = json.optString("vaultFilePath", ""),
                isVideo = json.optBoolean("isVideo", false),
                mimeType = json.optString("mimeType", ""),
                durationMs = json.optLong("durationMs", 0L),
                sizeBytes = json.optLong("sizeBytes", 0L),
                dateLocked = json.optLong("dateLocked", System.currentTimeMillis()),
                resolution = json.optString("resolution", ""),
                originalFolderName = json.optString("originalFolderName", "Vault"),
                originalFolderPath = json.optString("originalFolderPath", ""),
                originalFileName = json.optString("originalFileName", json.optString("title", "Locked Media")),
                artist = json.optString("artist", "Unknown Artist"),
                album = json.optString("album", "Privacy Vault")
            )
        }
    }
}

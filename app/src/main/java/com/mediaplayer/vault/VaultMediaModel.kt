package com.mediaplayer.vault

import android.net.Uri
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
    val album: String = "Privacy Vault"
) {
    fun toMediaItemModel(): MediaItemModel {
        val file = File(vaultFilePath)
        val validFile = if (file.exists() && file.canRead()) {
            file
        } else {
            val parent = file.parentFile
            val fallback = if (parent != null) File(parent, file.name) else file
            if (fallback.exists()) fallback else file
        }

        val cleanMime = when {
            mimeType.startsWith("video/") && mimeType != "video/*" -> mimeType
            mimeType.startsWith("audio/") && mimeType != "audio/*" -> mimeType
            isVideo -> {
                when {
                    validFile.name.endsWith(".mkv", true) -> "video/x-matroska"
                    validFile.name.endsWith(".mp4", true) -> "video/mp4"
                    validFile.name.endsWith(".webm", true) -> "video/webm"
                    validFile.name.endsWith(".ts", true) -> "video/mp2t"
                    validFile.name.endsWith(".avi", true) -> "video/avi"
                    else -> ""
                }
            }
            else -> {
                when {
                    validFile.name.endsWith(".mp3", true) -> "audio/mpeg"
                    validFile.name.endsWith(".flac", true) -> "audio/flac"
                    validFile.name.endsWith(".wav", true) -> "audio/wav"
                    validFile.name.endsWith(".m4a", true) || validFile.name.endsWith(".aac", true) -> "audio/mp4"
                    validFile.name.endsWith(".ogg", true) || validFile.name.endsWith(".opus", true) -> "audio/ogg"
                    else -> ""
                }
            }
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
            mimeType = cleanMime,
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

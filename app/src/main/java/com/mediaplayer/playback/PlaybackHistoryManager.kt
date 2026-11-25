package com.mediaplayer.playback

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PlaybackHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("media_playback_history_v2", Context.MODE_PRIVATE)
    private val cacheLock = Any()

    @Volatile
    private var playedIdsCache: Set<String>? = null

    @Volatile
    private var completedIdsCache: Set<String>? = null

    fun getPosition(mediaId: Long): Long {
        return prefs.getLong("pos_$mediaId", 0L)
    }

    fun savePosition(mediaId: Long, positionMs: Long, durationMs: Long) {
        val idStr = mediaId.toString()
        var updatedPlayed: Set<String>? = null
        var updatedCompleted: Set<String>? = null
        val isCompletedThreshold = (durationMs > 0) && (positionMs >= (durationMs * 0.90).toLong())

        synchronized(cacheLock) {
            val currentPlayed = getPlayedMediaIds()
            if (!currentPlayed.contains(idStr)) {
                updatedPlayed = currentPlayed + idStr
                playedIdsCache = updatedPlayed
            }
            if (isCompletedThreshold) {
                val currentCompleted = getCompletedMediaIds()
                if (!currentCompleted.contains(idStr)) {
                    updatedCompleted = currentCompleted + idStr
                    completedIdsCache = updatedCompleted
                }
            }
        }

        prefs.edit {
            putLong("pos_$mediaId", positionMs)
            putLong("time_$mediaId", System.currentTimeMillis())
            if (updatedPlayed != null) {
                putStringSet("played_ids", updatedPlayed)
            }
            if (updatedCompleted != null) {
                putStringSet("completed_ids", updatedCompleted)
            }
        }
    }

    fun markAsPlayed(mediaId: Long) {
        val idStr = mediaId.toString()
        val updatedPlayed = synchronized(cacheLock) {
            val currentPlayed = getPlayedMediaIds()
            if (currentPlayed.contains(idStr)) return
            (currentPlayed + idStr).also { playedIdsCache = it }
        }
        prefs.edit { putStringSet("played_ids", updatedPlayed) }
    }

    fun isPlayed(mediaId: Long): Boolean {
        return getPlayedMediaIds().contains(mediaId.toString())
    }

    @Suppress("unused")
    fun isCompleted(mediaId: Long): Boolean {
        return getCompletedMediaIds().contains(mediaId.toString())
    }

    fun clearPosition(mediaId: Long) {
        prefs.edit {
            remove("pos_$mediaId")
            remove("time_$mediaId")
        }
    }

    fun clearAllPositions() {
        prefs.edit {
            val keysToRemove = prefs.all.keys.filter { it.startsWith("pos_") || it.startsWith("time_") }
            for (key in keysToRemove) {
                remove(key)
            }
        }
    }

    fun getPlayedMediaIds(): Set<String> {
        playedIdsCache?.let { return it }
        return synchronized(cacheLock) {
            playedIdsCache ?: (prefs.getStringSet("played_ids", emptySet())?.toSet() ?: emptySet()).also {
                playedIdsCache = it
            }
        }
    }

    fun getCompletedMediaIds(): Set<String> {
        completedIdsCache?.let { return it }
        return synchronized(cacheLock) {
            completedIdsCache ?: (prefs.getStringSet("completed_ids", emptySet())?.toSet() ?: emptySet()).also {
                completedIdsCache = it
            }
        }
    }

    fun getAllPositions(): Map<Long, Long> {
        val map = mutableMapOf<Long, Long>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("pos_") && value is Long) {
                val idStr = key.removePrefix("pos_")
                idStr.toLongOrNull()?.let { id ->
                    map[id] = value
                }
            }
        }
        return map
    }

    fun getAllTimestamps(): Map<Long, Long> {
        val map = mutableMapOf<Long, Long>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("time_") && value is Long) {
                val idStr = key.removePrefix("time_")
                idStr.toLongOrNull()?.let { id ->
                    map[id] = value
                }
            }
        }
        return map
    }

    fun saveLastPlayedMediaId(mediaId: Long) {
        prefs.edit { putLong("last_played_media_id", mediaId) }
    }

    fun getLastPlayedMediaId(): Long? {
        val id = prefs.getLong("last_played_media_id", -1L)
        return if (id != -1L) id else null
    }

    @Suppress("unused")
    fun saveLastFolder(folderName: String?) {
        prefs.edit { putString("last_opened_folder", folderName) }
    }

    @Suppress("unused")
    fun getLastFolder(): String? {
        return prefs.getString("last_opened_folder", null)
    }

    fun saveLastTab(tabIndex: Int) {
        prefs.edit { putInt("last_tab_index", tabIndex) }
    }

    fun getLastTab(): Int {
        return prefs.getInt("last_tab_index", 0)
    }

    fun saveRecentStream(url: String, title: String, isVideo: Boolean) {
        try {
            val existing = getRecentStreams().toMutableList()
            existing.removeAll { (existingUrl) -> existingUrl.equals(url, ignoreCase = true) }
            existing.add(0, StreamHistoryItem(url, title, isVideo, System.currentTimeMillis()))
            val trimmed = existing.take(15)
            val jsonArray = org.json.JSONArray()
            for ((itemUrl, itemTitle, isItemVideo, itemTimestamp) in trimmed) {
                val obj = org.json.JSONObject().apply {
                    put("url", itemUrl)
                    put("title", itemTitle)
                    put("isVideo", isItemVideo)
                    put("timestamp", itemTimestamp)
                }
                jsonArray.put(obj)
            }
            prefs.edit { putString("recent_streams_json", jsonArray.toString()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecentStreams(): List<StreamHistoryItem> {
        val json = prefs.getString("recent_streams_json", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<StreamHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StreamHistoryItem(
                        url = obj.getString("url"),
                        title = obj.optString("title", "Stream"),
                        isVideo = obj.optBoolean("isVideo", true),
                        timestamp = obj.optLong("timestamp", 0L),
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearRecentStreams() {
        prefs.edit { remove("recent_streams_json") }
    }

    fun removeRecentStream(url: String) {
        val existing = getRecentStreams().toMutableList()
        existing.removeAll { (existingUrl) -> existingUrl.equals(url, ignoreCase = true) }
        val jsonArray = org.json.JSONArray()
        for ((itemUrl, itemTitle, isItemVideo, itemTimestamp) in existing) {
            val obj = org.json.JSONObject().apply {
                put("url", itemUrl)
                put("title", itemTitle)
                put("isVideo", isItemVideo)
                put("timestamp", itemTimestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString("recent_streams_json", jsonArray.toString()) }
    }
}

data class StreamHistoryItem(
    val url: String,
    val title: String,
    val isVideo: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

package com.mediaplayer.playback

import android.content.Context
import android.content.SharedPreferences

class PlaybackHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("media_playback_history_v2", Context.MODE_PRIVATE)

    fun getPosition(mediaId: Long): Long {
        return prefs.getLong("pos_$mediaId", 0L)
    }

    fun savePosition(mediaId: Long, positionMs: Long, durationMs: Long) {
        val editor = prefs.edit()
        editor.putLong("pos_$mediaId", positionMs)
        editor.putLong("time_$mediaId", System.currentTimeMillis())

        val idStr = mediaId.toString()
        val currentPlayed = getPlayedMediaIds()
        if (!currentPlayed.contains(idStr)) {
            val played = currentPlayed.toMutableSet()
            played.add(idStr)
            editor.putStringSet("played_ids", played)
        }

        // If watched past 90%, mark as completed
        if (durationMs > 0 && positionMs >= (durationMs * 0.90).toLong()) {
            val currentCompleted = getCompletedMediaIds()
            if (!currentCompleted.contains(idStr)) {
                val completed = currentCompleted.toMutableSet()
                completed.add(idStr)
                editor.putStringSet("completed_ids", completed)
            }
        }
        editor.apply()
    }

    fun markAsPlayed(mediaId: Long) {
        val played = getPlayedMediaIds().toMutableSet()
        if (played.add(mediaId.toString())) {
            prefs.edit().putStringSet("played_ids", played).apply()
        }
    }

    fun isPlayed(mediaId: Long): Boolean {
        return getPlayedMediaIds().contains(mediaId.toString())
    }

    fun isCompleted(mediaId: Long): Boolean {
        return getCompletedMediaIds().contains(mediaId.toString())
    }

    fun clearPosition(mediaId: Long) {
        prefs.edit().remove("pos_$mediaId").apply()
    }

    fun getPlayedMediaIds(): Set<String> {
        return prefs.getStringSet("played_ids", emptySet()) ?: emptySet()
    }

    fun getCompletedMediaIds(): Set<String> {
        return prefs.getStringSet("completed_ids", emptySet()) ?: emptySet()
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

    fun saveLastPlayedMediaId(mediaId: Long) {
        prefs.edit().putLong("last_played_media_id", mediaId).apply()
    }

    fun getLastPlayedMediaId(): Long? {
        val id = prefs.getLong("last_played_media_id", -1L)
        return if (id != -1L) id else null
    }

    fun saveLastFolder(folderName: String?) {
        prefs.edit().putString("last_opened_folder", folderName).apply()
    }

    fun getLastFolder(): String? {
        return prefs.getString("last_opened_folder", null)
    }

    fun saveLastTab(tabIndex: Int) {
        prefs.edit().putInt("last_tab_index", tabIndex).apply()
    }

    fun getLastTab(): Int {
        return prefs.getInt("last_tab_index", 0)
    }

    fun saveRecentStream(url: String, title: String, isVideo: Boolean) {
        try {
            val existing = getRecentStreams().toMutableList()
            existing.removeAll { it.url.equals(url, ignoreCase = true) }
            existing.add(0, StreamHistoryItem(url, title, isVideo, System.currentTimeMillis()))
            val trimmed = existing.take(15)
            val jsonArray = org.json.JSONArray()
            for (item in trimmed) {
                val obj = org.json.JSONObject().apply {
                    put("url", item.url)
                    put("title", item.title)
                    put("isVideo", item.isVideo)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("recent_streams_json", jsonArray.toString()).apply()
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
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearRecentStreams() {
        prefs.edit().remove("recent_streams_json").apply()
    }

    fun removeRecentStream(url: String) {
        val existing = getRecentStreams().toMutableList()
        existing.removeAll { it.url.equals(url, ignoreCase = true) }
        val jsonArray = org.json.JSONArray()
        for (item in existing) {
            val obj = org.json.JSONObject().apply {
                put("url", item.url)
                put("title", item.title)
                put("isVideo", item.isVideo)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("recent_streams_json", jsonArray.toString()).apply()
    }
}

data class StreamHistoryItem(
    val url: String,
    val title: String,
    val isVideo: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

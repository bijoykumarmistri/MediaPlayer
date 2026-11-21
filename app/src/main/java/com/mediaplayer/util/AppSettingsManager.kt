package com.mediaplayer.util

import android.content.Context
import android.content.SharedPreferences

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_customization", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DECODER_MODE = "pref_decoder_mode"
        const val KEY_DOUBLE_TAP_SEEK = "pref_double_tap_seek"
        const val KEY_AUTO_RESUME = "pref_auto_resume"
        const val KEY_BACKGROUND_PLAYBACK = "pref_background_playback"
        const val KEY_SWIPE_GESTURES = "pref_swipe_gestures"
        const val KEY_FAST_SCRUB = "pref_fast_scrub"
        const val KEY_HOLD_2X_SPEED = "pref_hold_2x_speed"
        const val KEY_DEFAULT_RESIZE = "pref_default_resize"
        const val KEY_SUBTITLE_SIZE = "pref_subtitle_size"
        const val KEY_SUBTITLE_COLOR = "pref_subtitle_color"
        const val KEY_HIDE_SHORT_VIDEOS = "pref_hide_short_videos"
        const val KEY_MIN_VIDEO_DURATION_SEC = "pref_min_video_duration_sec"
        const val KEY_HIGH_REFRESH_RATE = "pref_high_refresh_rate"
    }

    var defaultDecoderMode: String
        get() = prefs.getString(KEY_DECODER_MODE, "HW+") ?: "HW+"
        set(value) = prefs.edit().putString(KEY_DECODER_MODE, value).apply()

    var doubleTapSeekSeconds: Int
        get() = prefs.getInt(KEY_DOUBLE_TAP_SEEK, 10)
        set(value) = prefs.edit().putInt(KEY_DOUBLE_TAP_SEEK, value).apply()

    var autoResume: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESUME, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RESUME, value).apply()

    var backgroundPlayback: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_PLAYBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_PLAYBACK, value).apply()

    var swipeGesturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_GESTURES, true)
        set(value) = prefs.edit().putBoolean(KEY_SWIPE_GESTURES, value).apply()

    var fastScrubEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAST_SCRUB, true)
        set(value) = prefs.edit().putBoolean(KEY_FAST_SCRUB, value).apply()

    var hold2xSpeedEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOLD_2X_SPEED, true)
        set(value) = prefs.edit().putBoolean(KEY_HOLD_2X_SPEED, value).apply()

    var defaultResizeMode: String
        get() = prefs.getString(KEY_DEFAULT_RESIZE, "FIT") ?: "FIT"
        set(value) = prefs.edit().putString(KEY_DEFAULT_RESIZE, value).apply()

    var subtitleSize: Int
        get() = prefs.getInt(KEY_SUBTITLE_SIZE, 48)
        set(value) = prefs.edit().putInt(KEY_SUBTITLE_SIZE, value).apply()

    var subtitleColorHex: String
        get() = prefs.getString(KEY_SUBTITLE_COLOR, "#FFFFFF") ?: "#FFFFFF"
        set(value) = prefs.edit().putString(KEY_SUBTITLE_COLOR, value).apply()

    var hideShortVideos: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SHORT_VIDEOS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_SHORT_VIDEOS, value).apply()

    var minVideoDurationSec: Int
        get() = prefs.getInt(KEY_MIN_VIDEO_DURATION_SEC, 30)
        set(value) = prefs.edit().putInt(KEY_MIN_VIDEO_DURATION_SEC, value).apply()

    var highRefreshRateEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGH_REFRESH_RATE, true)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_REFRESH_RATE, value).apply()
}

package com.mediaplayer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_settings_customization"

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
        const val KEY_MUSIC_TIME_VIEW_REMAINING = "pref_music_time_view_remaining"
        const val KEY_PITCH_BLACK_THEME = "pref_pitch_black_theme"
        const val KEY_AUTO_ROTATE_VIDEO = "pref_auto_rotate_video"
        const val KEY_AUTO_PLAY_NEXT_VIDEO = "pref_auto_play_next_video"

        private val _pitchBlackState = MutableStateFlow(false)
        private val pitchBlackInitialized = AtomicBoolean(false)

        fun pitchBlackThemeFlow(context: Context): StateFlow<Boolean> {
            if (pitchBlackInitialized.compareAndSet(false, true)) {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _pitchBlackState.value = prefs.getBoolean(KEY_PITCH_BLACK_THEME, false)
            }
            return _pitchBlackState.asStateFlow()
        }
    }

    var pitchBlackTheme: Boolean
        get() = prefs.getBoolean(KEY_PITCH_BLACK_THEME, false)
        set(value) {
            prefs.edit { putBoolean(KEY_PITCH_BLACK_THEME, value) }
            pitchBlackInitialized.set(true)
            _pitchBlackState.value = value
        }

    var defaultDecoderMode: String
        get() = prefs.getString(KEY_DECODER_MODE, "HW+") ?: "HW+"
        set(value) = prefs.edit { putString(KEY_DECODER_MODE, value) }

    var doubleTapSeekSeconds: Int
        get() = prefs.getInt(KEY_DOUBLE_TAP_SEEK, 10)
        set(value) = prefs.edit { putInt(KEY_DOUBLE_TAP_SEEK, value) }

    var autoResume: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESUME, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_RESUME, value) }

    @Suppress("unused")
    var backgroundPlayback: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_PLAYBACK, true)
        set(value) = prefs.edit { putBoolean(KEY_BACKGROUND_PLAYBACK, value) }

    var swipeGesturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_GESTURES, true)
        set(value) = prefs.edit { putBoolean(KEY_SWIPE_GESTURES, value) }

    var fastScrubEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAST_SCRUB, true)
        set(value) = prefs.edit { putBoolean(KEY_FAST_SCRUB, value) }

    var hold2xSpeedEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOLD_2X_SPEED, true)
        set(value) = prefs.edit { putBoolean(KEY_HOLD_2X_SPEED, value) }

    var defaultResizeMode: String
        get() = prefs.getString(KEY_DEFAULT_RESIZE, "FIT") ?: "FIT"
        set(value) = prefs.edit { putString(KEY_DEFAULT_RESIZE, value) }

    var subtitleSize: Int
        get() = prefs.getInt(KEY_SUBTITLE_SIZE, 48)
        set(value) = prefs.edit { putInt(KEY_SUBTITLE_SIZE, value) }

    var subtitleColorHex: String
        get() = prefs.getString(KEY_SUBTITLE_COLOR, "#FFFFFF") ?: "#FFFFFF"
        set(value) = prefs.edit { putString(KEY_SUBTITLE_COLOR, value) }

    var hideShortVideos: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SHORT_VIDEOS, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_SHORT_VIDEOS, value) }

    var minVideoDurationSec: Int
        get() = prefs.getInt(KEY_MIN_VIDEO_DURATION_SEC, 30)
        set(value) = prefs.edit { putInt(KEY_MIN_VIDEO_DURATION_SEC, value) }

    var highRefreshRateEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGH_REFRESH_RATE, true)
        set(value) = prefs.edit { putBoolean(KEY_HIGH_REFRESH_RATE, value) }

    var musicTimeViewRemaining: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_TIME_VIEW_REMAINING, false)
        set(value) = prefs.edit { putBoolean(KEY_MUSIC_TIME_VIEW_REMAINING, value) }

    var autoRotateVideo: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ROTATE_VIDEO, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_ROTATE_VIDEO, value) }

    var autoPlayNextVideo: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PLAY_NEXT_VIDEO, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_PLAY_NEXT_VIDEO, value) }
}

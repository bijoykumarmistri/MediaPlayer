package com.mediaplayer.util

import java.util.Locale

object AudioFormatUtils {
    fun formatSampleRate(sampleRate: Int): String {
        return when {
            sampleRate >= 1000 && sampleRate % 1000 == 0 -> "${sampleRate / 1000} kHz"
            sampleRate >= 1000 -> String.format(Locale.US, "%.1f kHz", sampleRate / 1000.0)
            sampleRate > 0 -> "$sampleRate Hz"
            else -> "44.1 kHz"
        }
    }
}

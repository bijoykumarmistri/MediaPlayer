package com.mediaplayer.audio

data class UsbAudioStatus(
    val isUsbDacConnected: Boolean = false,
    val dacName: String = "No USB DAC",
    val supportedSampleRates: List<Int> = emptyList(),
    val supportedBitDepths: List<Int> = emptyList(),
    val isExclusiveBitPerfectEnabled: Boolean = true,
    val isMixerBypassActive: Boolean = false,
    val isBitPerfectSupported: Boolean = false,
    val sourceFormatText: String = "PCM 16-bit / 44.1 kHz",
    val outputFormatText: String = "System Mixer (Default)",
    val liveFormatTelemetry: String = "Internal Audio Engine • 44.1 kHz",
    val hasUsbPermission: Boolean = false,
    val sourceSampleRate: Int = 44100,
    val sourceBitDepth: Int = 16,
    val sourceChannels: Int = 2,
    val sourceCodec: String = "PCM",
    val outputSampleRate: Int = 44100,
    val outputBitDepth: Int = 16,
    val mixerBehavior: String = "Default Mixer",
    val isBitPerfectActive: Boolean = isMixerBypassActive,
    val isExclusiveRequested: Boolean = isExclusiveBitPerfectEnabled,
    val supportedEncodings: List<Int> = emptyList()
) {
    val pillText: String get() = liveFormatTelemetry

    companion object {
        fun formatSampleRate(sampleRate: Int): String {
            return when {
                sampleRate >= 1000 && sampleRate % 1000 == 0 -> "${sampleRate / 1000} kHz"
                sampleRate >= 1000 -> String.format(java.util.Locale.US, "%.1f kHz", sampleRate / 1000.0)
                sampleRate > 0 -> "$sampleRate Hz"
                else -> "44.1 kHz"
            }
        }
    }
}

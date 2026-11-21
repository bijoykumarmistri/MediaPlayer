package com.mediaplayer.audio

import android.app.Activity
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import com.bitperfect.usb.BitPerfectEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(UnstableApi::class)
class UsbBitPerfectAudioController(private val context: Context) {

    private val tag = "UsbBitPerfectAudio"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val engine = BitPerfectEngine(context)

    private val _status = MutableStateFlow(UsbAudioStatus())
    val status: StateFlow<UsbAudioStatus> = _status.asStateFlow()
    val telemetry: StateFlow<UsbAudioStatus> = status

    var onPlaybackPauseRequested: (() -> Unit)? = null
    var onActiveUsbAudioDeviceChanged: ((AudioDeviceInfo?) -> Unit)? = null

    private var activeUsbAudioDevice: AudioDeviceInfo? = null
    val currentUsbAudioDevice: AudioDeviceInfo? get() = activeUsbAudioDevice

    private var currentSourceSampleRate: Int = 44100
    private var currentSourceBitDepth: Int = 16
    private var currentSourceChannels: Int = 2
    private var currentSourceCodec: String = "PCM"
    private var isExclusiveRequested: Boolean = true

    private val audioDeviceCallback = object : android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            val dev = findUsbAudioDeviceInfo()
            if (dev != null) {
                activeUsbAudioDevice = dev
                onActiveUsbAudioDeviceChanged?.invoke(dev)
                scanAudioDevices()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val dev = findUsbAudioDeviceInfo()
            activeUsbAudioDevice = dev
            onActiveUsbAudioDeviceChanged?.invoke(dev)
            if (dev == null) {
                onPlaybackPauseRequested?.invoke()
            }
        }
    }

    init {
        engine.setListener(object : BitPerfectEngine.Listener {
            override fun onDacConnected(dacName: String, uacVersionHex: Int) {
                Log.i(tag, "DAC Connected: $dacName (UAC: 0x${uacVersionHex.toString(16)})")
                findUsbAudioDeviceInfo()?.let { dev ->
                    activeUsbAudioDevice = dev
                    onActiveUsbAudioDeviceChanged?.invoke(dev)
                }

                val uacVersionStr = when (uacVersionHex) {
                    0x0200 -> "UAC 2.0 (High-Speed)"
                    0x0100 -> "UAC 1.0 (Full-Speed)"
                    else -> "USB Audio Class"
                }

                _status.update {
                    it.copy(
                        isUsbDacConnected = true,
                        dacName = dacName,
                        isBitPerfectSupported = true,
                        isBitPerfectActive = isExclusiveRequested,
                        isMixerBypassActive = isExclusiveRequested,
                        isExclusiveBitPerfectEnabled = isExclusiveRequested,
                        hasUsbPermission = true,
                        mixerBehavior = "libusb Exclusive Isochronous ($uacVersionStr)",
                        outputFormatText = "Hardware Wire (libusb $uacVersionStr Direct)",
                        liveFormatTelemetry = "$dacName • Bit-Perfect Direct USB"
                    )
                }
            }

            override fun onDacDisconnected() {
                Log.i(tag, "DAC Disconnected")
                activeUsbAudioDevice = null
                onActiveUsbAudioDeviceChanged?.invoke(null)
                onPlaybackPauseRequested?.invoke()

                _status.update {
                    it.copy(
                        isUsbDacConnected = false,
                        dacName = "No USB DAC",
                        isBitPerfectSupported = false,
                        isBitPerfectActive = false,
                        isMixerBypassActive = false,
                        mixerBehavior = "System Mixer (Default)",
                        sourceFormatText = "PCM $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
                        outputFormatText = "System Standard Audio Track",
                        liveFormatTelemetry = "Internal Audio Engine • ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}"
                    )
                }
            }

            override fun onFormatChanged(
                sampleRateHz: Int,
                sourceBitDepth: Int,
                wireBitDepth: Int,
                channels: Int,
                codecName: String
            ) {
                Log.i(tag, "Format changed: $sampleRateHz Hz, $sourceBitDepth-bit -> $wireBitDepth-bit, $channels ch, $codecName")
                val telemetryText = "$wireBitDepth-bit / ${UsbAudioStatus.formatSampleRate(sampleRateHz)} $codecName • Bit-Perfect"
                _status.update {
                    it.copy(
                        sourceSampleRate = sampleRateHz,
                        sourceBitDepth = sourceBitDepth,
                        sourceChannels = channels,
                        sourceCodec = codecName,
                        outputSampleRate = sampleRateHz,
                        outputBitDepth = wireBitDepth,
                        sourceFormatText = "$codecName $sourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(sampleRateHz)} ($channels ch)",
                        outputFormatText = "Direct USB Isochronous ($wireBitDepth-bit / ${UsbAudioStatus.formatSampleRate(sampleRateHz)})",
                        liveFormatTelemetry = telemetryText
                    )
                }
            }

            override fun onPositionChanged(positionMs: Long, durationMs: Long) {
                // Real-time bit-perfect position callback
            }

            override fun onStatusMessage(message: String) {
                Log.d(tag, "Engine status: $message")
            }

            override fun onError(errorMessage: String) {
                Log.e(tag, "Engine error: $errorMessage")
            }

            override fun onPlaybackCompleted() {
                Log.i(tag, "Playback completed")
            }
        })

        // Initialize DAC scanning and listen for USB-C audio hardware hotplug events
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        } catch (_: Exception) {}

        scanAudioDevices()
    }

    fun scanAudioDevices() {
        val dev = findUsbAudioDeviceInfo()
        activeUsbAudioDevice = dev
        if (dev != null) {
            onActiveUsbAudioDeviceChanged?.invoke(dev)
            val rates = dev.sampleRates.toList().filter { it > 0 }.sorted()
            if (rates.isNotEmpty()) {
                _status.update { it.copy(supportedSampleRates = rates) }
            }
        }
        engine.initializeDac()
        applyBitPerfectMixerAttributes(_status.value.isExclusiveBitPerfectEnabled)
    }

    fun setExclusiveBitPerfect(enabled: Boolean) {
        isExclusiveRequested = enabled
        if (enabled) {
            engine.initializeDac()
        } else {
            engine.stop()
        }
        applyBitPerfectMixerAttributes(enabled)
        _status.update {
            it.copy(
                isExclusiveBitPerfectEnabled = enabled,
                isBitPerfectActive = enabled && it.isUsbDacConnected,
                isMixerBypassActive = enabled && it.isUsbDacConnected
            )
        }
    }

    fun setBackendMode(mode: BitPerfectEngine.BackendMode) {
        engine.setBackendMode(mode)
        applyBitPerfectMixerAttributes(_status.value.isExclusiveBitPerfectEnabled)
        _status.update {
            it.copy(
                mixerBehavior = when (mode) {
                    BitPerfectEngine.BackendMode.EXCLUSIVE_USB_LIBUSB -> "libusb Exclusive Isochronous (Kernel Bypass)"
                    BitPerfectEngine.BackendMode.ANDROID_14_NATIVE_BIT_PERFECT -> "Android 14+ Native Bit-Perfect (HAL Bypass)"
                }
            )
        }
    }

    fun requestUsbPermission(activity: Activity) {
        engine.initializeDac()
    }

    fun applyBitPerfectMixerAttributes(enabled: Boolean) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val dev = activeUsbAudioDevice ?: findUsbAudioDeviceInfo() ?: return

        try {
            val playbackAttrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            if (enabled && isExclusiveRequested) {
                val supported = audioManager.getSupportedMixerAttributes(dev)
                val bitPerfectList = supported.filter {
                    it.mixerBehavior == android.media.AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                }

                val chosen = bitPerfectList.firstOrNull { attr ->
                    (currentSourceSampleRate <= 0 || attr.format.sampleRate == currentSourceSampleRate) &&
                    (currentSourceChannels <= 0 || attr.format.channelCount == currentSourceChannels)
                } ?: bitPerfectList.firstOrNull { attr ->
                    attr.format.sampleRate == currentSourceSampleRate
                } ?: bitPerfectList.firstOrNull()

                if (chosen != null) {
                    val applied = audioManager.setPreferredMixerAttributes(playbackAttrs, dev, chosen)
                    Log.i(tag, "Applied Bit-Perfect mixer attributes on ${dev.productName}: $applied (format: ${chosen.format})")
                } else {
                    Log.i(tag, "Direct HAL routing active for ${dev.productName}")
                }
            } else {
                audioManager.clearPreferredMixerAttributes(playbackAttrs, dev)
                Log.i(tag, "Cleared preferred mixer attributes on ${dev.productName}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Mixer attribute configuration notice: ${e.message}")
        }
    }

    fun enableBitPerfectBypass(enabled: Boolean, trackFormat: Format?) {
        isExclusiveRequested = enabled
        if (trackFormat != null) {
            if (trackFormat.sampleRate > 0) currentSourceSampleRate = trackFormat.sampleRate
            val resolvedDepth = resolveBitDepth(trackFormat.pcmEncoding)
            if (resolvedDepth != 16 || currentSourceBitDepth == 16) {
                currentSourceBitDepth = resolvedDepth
            }
            if (trackFormat.channelCount > 0) currentSourceChannels = trackFormat.channelCount
            val codec = extractCodecName(trackFormat.sampleMimeType ?: "audio/pcm")
            if (codec.isNotEmpty() && codec != "PCM") currentSourceCodec = codec
        }

        applyBitPerfectMixerAttributes(enabled)

        _status.update {
            it.copy(
                isExclusiveBitPerfectEnabled = enabled,
                isBitPerfectActive = enabled && it.isUsbDacConnected,
                isMixerBypassActive = enabled && it.isUsbDacConnected,
                sourceSampleRate = currentSourceSampleRate,
                sourceBitDepth = currentSourceBitDepth,
                sourceChannels = currentSourceChannels,
                sourceCodec = currentSourceCodec,
                sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)} ($currentSourceChannels ch)"
            )
        }
    }

    fun updateMediaAudioFormat(sampleRate: Int, bitDepth: Int, channels: Int, codec: String) {
        if (sampleRate > 0) currentSourceSampleRate = sampleRate
        if (bitDepth in listOf(16, 24, 32)) currentSourceBitDepth = bitDepth
        if (channels in 1..8) currentSourceChannels = channels
        if (codec.isNotEmpty()) currentSourceCodec = codec

        applyBitPerfectMixerAttributes(_status.value.isExclusiveBitPerfectEnabled)

        _status.update {
            val telemetryText = if (it.isUsbDacConnected && it.isBitPerfectActive) {
                "${it.outputBitDepth}-bit / ${UsbAudioStatus.formatSampleRate(it.outputSampleRate)} $currentSourceCodec • Bit-Perfect"
            } else {
                "Internal Audio Engine • ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}"
            }
            it.copy(
                sourceSampleRate = currentSourceSampleRate,
                sourceBitDepth = currentSourceBitDepth,
                sourceChannels = currentSourceChannels,
                sourceCodec = currentSourceCodec,
                sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)} ($currentSourceChannels ch)",
                liveFormatTelemetry = telemetryText
            )
        }
    }

    fun setHardwareVolume(percent: Float) {
        engine.setHardwareVolume(percent)
    }

    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (_: Exception) {}
        engine.release()
    }

    private fun findUsbAudioDeviceInfo(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.firstOrNull {
            it.isSink && (
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
            )
        }
    }

    private fun resolveBitDepth(pcmEncoding: Int): Int {
        return when (pcmEncoding) {
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            C.ENCODING_PCM_16BIT -> 16
            else -> 16
        }
    }

    private fun extractCodecName(mimeType: String): String {
        return when {
            mimeType.contains("flac", ignoreCase = true) -> "FLAC"
            mimeType.contains("alac", ignoreCase = true) -> "ALAC"
            mimeType.contains("wav", ignoreCase = true) -> "WAV"
            mimeType.contains("aac", ignoreCase = true) -> "AAC"
            mimeType.contains("mp4", ignoreCase = true) -> "AAC/PCM"
            mimeType.contains("mp3", ignoreCase = true) -> "MP3"
            mimeType.contains("opus", ignoreCase = true) -> "OPUS"
            mimeType.contains("vorbis", ignoreCase = true) -> "VORBIS"
            mimeType.contains("eac3", ignoreCase = true) -> "E-AC3"
            mimeType.contains("ac3", ignoreCase = true) -> "AC3"
            mimeType.contains("dts", ignoreCase = true) -> "DTS"
            mimeType.contains("truehd", ignoreCase = true) -> "TrueHD"
            else -> "PCM"
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.bitperfect.usb.ACTION_USB_PERMISSION"
    }
}

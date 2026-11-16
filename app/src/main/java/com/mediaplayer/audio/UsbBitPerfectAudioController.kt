package com.mediaplayer.audio

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(UnstableApi::class)
class UsbBitPerfectAudioController(private val context: Context) {

    private val tag = "UsbBitPerfectAudio"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow(UsbAudioStatus())
    val status: StateFlow<UsbAudioStatus> = _status.asStateFlow()
    val telemetry: StateFlow<UsbAudioStatus> = status

    var onPlaybackPauseRequested: (() -> Unit)? = null
    var onActiveUsbAudioDeviceChanged: ((AudioDeviceInfo?) -> Unit)? = null

    private var activeUsbAudioDevice: AudioDeviceInfo? = null
    val currentUsbAudioDevice: AudioDeviceInfo? get() = activeUsbAudioDevice

    private var activeUsbHardwareDevice: UsbDevice? = null
    private var isExclusiveRequested: Boolean = true

    private var lastTrackFormat: Format? = null
    private var currentSourceSampleRate: Int = 44100
    private var currentSourceBitDepth: Int = 16
    private var currentSourceChannels: Int = 2
    private var currentSourceCodec: String = "PCM"

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    private var preferredMixerListener: AudioManager.OnPreferredMixerAttributesChangedListener? = null

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesAdded(addedDevices)
            Log.d(tag, "Audio devices added")
            scanAudioDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            super.onAudioDevicesRemoved(removedDevices)
            Log.d(tag, "Audio devices removed")
            val removedUsb = removedDevices?.any { isUsbAudioDeviceInfo(it) } == true
            if (removedUsb) {
                // USB-C DAC was unplugged: clean up routing and pause playback safely to prevent speaker blast
                clearBitPerfectRouting()
                onPlaybackPauseRequested?.invoke()
            }
            scanAudioDevices()
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(tag, "USB device attached broadcast received")
                    scanAudioDevices()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(tag, "USB device detached broadcast received")
                    clearBitPerfectRouting()
                    onPlaybackPauseRequested?.invoke()
                    scanAudioDevices()
                }
                ACTION_USB_PERMISSION -> {
                    Log.d(tag, "USB permission broadcast received")
                    scanAudioDevices()
                }
            }
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        ContextCompat.registerReceiver(context, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val listener = AudioManager.OnPreferredMixerAttributesChangedListener { _, device, mixerAttrs ->
                Log.d(tag, "onPreferredMixerAttributesChanged: device=${device.productName}, behavior=${mixerAttrs?.mixerBehavior}")
                mainHandler.post {
                    handleMixerAttributesChanged(device, mixerAttrs)
                }
            }
            preferredMixerListener = listener
            try {
                audioManager.addOnPreferredMixerAttributesChangedListener(
                    ContextCompat.getMainExecutor(context),
                    listener
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to register OnPreferredMixerAttributesChangedListener", e)
            }
        }

        scanAudioDevices()
    }

    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            context.unregisterReceiver(usbReceiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                preferredMixerListener?.let {
                    audioManager.removeOnPreferredMixerAttributesChangedListener(it)
                }
                preferredMixerListener = null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering receiver or callback", e)
        }
        clearBitPerfectRouting()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun handleMixerAttributesChanged(device: AudioDeviceInfo, mixerAttrs: AudioMixerAttributes?) {
        if (activeUsbAudioDevice?.id != device.id) return
        val dacName = device.productName?.toString() ?: activeUsbHardwareDevice?.productName ?: "USB-C DAC"
        val hasUsbPerm = activeUsbHardwareDevice?.let { usbManager.hasPermission(it) } ?: true

        if (mixerAttrs != null && mixerAttrs.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT) {
            val confirmedRate = mixerAttrs.format.sampleRate.takeIf { it > 0 } ?: currentSourceSampleRate
            val confirmedDepth = when (mixerAttrs.format.encoding) {
                AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32
                AudioFormat.ENCODING_PCM_16BIT -> 16
                else -> currentSourceBitDepth
            }
            val telemetryText = "$confirmedDepth-bit / ${UsbAudioStatus.formatSampleRate(confirmedRate)} $currentSourceCodec • Bit-Perfect"
            updateStatusInternal(
                isUsbDacConnected = true,
                dacName = dacName,
                isExclusiveBitPerfectEnabled = true,
                isMixerBypassActive = true,
                isBitPerfectSupported = true,
                sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)} ($currentSourceChannels ch)",
                outputFormatText = "Direct HAL Hardware Stream ($dacName)",
                liveFormatTelemetry = telemetryText,
                hasUsbPermission = hasUsbPerm,
                sourceSampleRate = currentSourceSampleRate,
                sourceBitDepth = currentSourceBitDepth,
                sourceChannels = currentSourceChannels,
                sourceCodec = currentSourceCodec,
                outputSampleRate = confirmedRate,
                outputBitDepth = confirmedDepth,
                mixerBehavior = "Bit-Perfect Direct Bypass (Verified)"
            )
        } else if (isExclusiveRequested) {
            updateStatusInternal(
                isUsbDacConnected = true,
                dacName = dacName,
                isExclusiveBitPerfectEnabled = true,
                isMixerBypassActive = false,
                sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
                outputFormatText = "USB Audio Direct Route ($dacName)",
                liveFormatTelemetry = "USB DAC: $dacName • Standard Route",
                hasUsbPermission = hasUsbPerm,
                mixerBehavior = "Standard AudioTrack Mixer"
            )
        }
    }

    fun scanAudioDevices() {
        var foundUsbAudioDevice: AudioDeviceInfo? = null
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        for (device in devices) {
            if (isUsbAudioDeviceInfo(device)) {
                foundUsbAudioDevice = device
                break
            }
        }

        val previousUsbAudioDevice = activeUsbAudioDevice
        activeUsbAudioDevice = foundUsbAudioDevice

        // Hardware USB device scan
        var foundUsbDevice: UsbDevice? = null
        val usbList = usbManager.deviceList
        for ((_, usbDev) in usbList) {
            if (isUsbAudioHardwareDevice(usbDev)) {
                foundUsbDevice = usbDev
                break
            }
        }
        activeUsbHardwareDevice = foundUsbDevice

        val deviceChanged = (previousUsbAudioDevice?.id != activeUsbAudioDevice?.id)

        if (activeUsbAudioDevice != null) {
            if (deviceChanged) {
                onActiveUsbAudioDeviceChanged?.invoke(activeUsbAudioDevice)
            }
            enableBitPerfectBypass(isExclusiveRequested, lastTrackFormat)
        } else {
            if (deviceChanged) {
                onActiveUsbAudioDeviceChanged?.invoke(null)
            }
            updateStatusInternal(
                isUsbDacConnected = false,
                dacName = "No USB DAC",
                isBitPerfectSupported = false,
                isMixerBypassActive = false,
                mixerBehavior = "System Mixer (Default)",
                sourceFormatText = "PCM $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
                outputFormatText = "System Standard Audio Track",
                liveFormatTelemetry = "Internal Audio Engine • ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}"
            )
        }
    }

    private fun isUsbAudioDeviceInfo(device: AudioDeviceInfo): Boolean {
        return (device.type == AudioDeviceInfo.TYPE_USB_DEVICE) ||
                (device.type == AudioDeviceInfo.TYPE_USB_HEADSET) ||
                (device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY)
    }

    private fun isUsbAudioHardwareDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO) return true
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    fun setExclusiveBitPerfect(enabled: Boolean) {
        enableBitPerfectBypass(enabled, lastTrackFormat)
    }

    fun requestUsbPermission(activity: Activity) {
        val device = activeUsbHardwareDevice ?: return
        if (usbManager.hasPermission(device)) return

        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val permissionIntent = PendingIntent.getBroadcast(activity, 0, intent, flags)
        usbManager.requestPermission(device, permissionIntent)
    }

    @OptIn(UnstableApi::class)
    fun enableBitPerfectBypass(enabled: Boolean, trackFormat: Format?) {
        isExclusiveRequested = enabled
        lastTrackFormat = trackFormat

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

        val usbDevice = activeUsbAudioDevice
        if (usbDevice == null) {
            clearBitPerfectRouting()
            updateStatusInternal(
                isUsbDacConnected = false,
                dacName = "No USB DAC",
                isBitPerfectSupported = false,
                isMixerBypassActive = false,
                isExclusiveBitPerfectEnabled = enabled,
                mixerBehavior = "System Mixer (Default)",
                sourceFormatText = "PCM $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
                outputFormatText = "Internal Audio Mixer (System)",
                liveFormatTelemetry = "Internal Audio Engine • ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}"
            )
            return
        }

        val dacName = usbDevice.productName?.toString()
            ?: activeUsbHardwareDevice?.productName
            ?: "External USB-C DAC"

        val hasUsbPerm = activeUsbHardwareDevice?.let { usbManager.hasPermission(it) } ?: true

        if (!enabled) {
            clearBitPerfectRouting()
            updateStatusInternal(
                isUsbDacConnected = true,
                dacName = dacName,
                isExclusiveBitPerfectEnabled = false,
                isMixerBypassActive = false,
                mixerBehavior = "System Standard Mixer",
                sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
                outputFormatText = "Standard AudioTrack Mixer ($dacName)",
                liveFormatTelemetry = "USB DAC: $dacName • Standard Mixer",
                hasUsbPermission = hasUsbPerm
            )
            return
        }

        // Apply bit-perfect bypass on Android 14+ (API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val supportedMixers = audioManager.getSupportedMixerAttributes(usbDevice)
                val bitPerfectSupported = supportedMixers.any {
                    it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                }

                val supportedSampleRates = mutableListOf<Int>()
                val supportedBitDepths = mutableListOf<Int>()
                val supportedEncodings = mutableListOf<Int>()

                // 1. Hardware capabilities from AudioDeviceInfo directly
                for (rate in usbDevice.sampleRates) {
                    if (rate > 0 && !supportedSampleRates.contains(rate)) {
                        supportedSampleRates.add(rate)
                    }
                }
                for (enc in usbDevice.encodings) {
                    if (enc > 0 && !supportedEncodings.contains(enc)) {
                        supportedEncodings.add(enc)
                        val depth = when (enc) {
                            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                            AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32
                            AudioFormat.ENCODING_PCM_16BIT -> 16
                            else -> 16
                        }
                        if (!supportedBitDepths.contains(depth)) {
                            supportedBitDepths.add(depth)
                        }
                    }
                }

                // 2. Hardware capabilities from supportedMixerAttributes
                for (mixer in supportedMixers) {
                    val rate = mixer.format.sampleRate
                    if (rate > 0 && !supportedSampleRates.contains(rate)) {
                        supportedSampleRates.add(rate)
                    }
                    val enc = mixer.format.encoding
                    if (enc > 0 && !supportedEncodings.contains(enc)) {
                        supportedEncodings.add(enc)
                        val depth = when (enc) {
                            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                            AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32
                            AudioFormat.ENCODING_PCM_16BIT -> 16
                            else -> 16
                        }
                        if (!supportedBitDepths.contains(depth)) {
                            supportedBitDepths.add(depth)
                        }
                    }
                }
                supportedSampleRates.sort()
                supportedBitDepths.sort()

                val channelMask = if (currentSourceChannels == 1) {
                    AudioFormat.CHANNEL_OUT_MONO
                } else {
                    AudioFormat.CHANNEL_OUT_STEREO
                }

                // Candidate encodings prioritizing hi-res 32-bit float / 32-bit int / 24-bit packed
                val candidateEncodings = when (currentSourceBitDepth) {
                    32 -> listOf(
                        AudioFormat.ENCODING_PCM_FLOAT,
                        AudioFormat.ENCODING_PCM_32BIT,
                        AudioFormat.ENCODING_PCM_24BIT_PACKED,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    24 -> listOf(
                        AudioFormat.ENCODING_PCM_24BIT_PACKED,
                        AudioFormat.ENCODING_PCM_FLOAT,
                        AudioFormat.ENCODING_PCM_32BIT,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    else -> listOf(
                        AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.ENCODING_PCM_FLOAT,
                        AudioFormat.ENCODING_PCM_24BIT_PACKED
                    )
                }

                var success = false

                // Step 1: Match exact BIT_PERFECT in supportedMixers matching source sample rate
                for (enc in candidateEncodings) {
                    val match = supportedMixers.firstOrNull { mixer ->
                        mixer.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                                mixer.format.sampleRate == currentSourceSampleRate &&
                                mixer.format.encoding == enc
                    }
                    if (match != null) {
                        val applied = audioManager.setPreferredMixerAttributes(audioAttributes, usbDevice, match)
                        if (applied) {
                            success = true
                            Log.d(tag, "Applied exact match bit-perfect mixer: enc=$enc, rate=$currentSourceSampleRate")
                            break
                        }
                    }
                }

                // Step 2: Build BIT_PERFECT AudioMixerAttributes directly for target candidate formats
                if (!success) {
                    for (enc in candidateEncodings) {
                        try {
                            val candidateFormat = AudioFormat.Builder()
                                .setSampleRate(currentSourceSampleRate)
                                .setEncoding(enc)
                                .setChannelMask(channelMask)
                                .build()
                            val candidateMixer = AudioMixerAttributes.Builder(candidateFormat)
                                .setMixerBehavior(AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
                                .build()
                            val applied = audioManager.setPreferredMixerAttributes(audioAttributes, usbDevice, candidateMixer)
                            if (applied) {
                                success = true
                                Log.d(tag, "Applied constructed bit-perfect mixer: enc=$enc, rate=$currentSourceSampleRate")
                                break
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Could not set constructed preferred mixer for enc=$enc", e)
                        }
                    }
                }

                // Step 3: Match any BIT_PERFECT supportedMixer matching sample rate
                if (!success) {
                    val rateMatch = supportedMixers.firstOrNull {
                        it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                                it.format.sampleRate == currentSourceSampleRate
                    }
                    if (rateMatch != null) {
                        val applied = audioManager.setPreferredMixerAttributes(audioAttributes, usbDevice, rateMatch)
                        if (applied) {
                            success = true
                            Log.d(tag, "Applied rate match bit-perfect mixer: rate=$currentSourceSampleRate")
                        }
                    }
                }

                // Step 4: Fallback to any BIT_PERFECT mixer advertised by hardware
                if (!success) {
                    val anyBitPerfect = supportedMixers.firstOrNull {
                        it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                    }
                    if (anyBitPerfect != null) {
                        val applied = audioManager.setPreferredMixerAttributes(audioAttributes, usbDevice, anyBitPerfect)
                        if (applied) {
                            success = true
                            Log.d(tag, "Applied hardware advertised bit-perfect mixer")
                        }
                    }
                }

                // VERIFY directly from the Android OS audio server:
                val confirmedPreferred = audioManager.getPreferredMixerAttributes(audioAttributes, usbDevice)
                val isVerifiedBitPerfect = success && (confirmedPreferred?.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)

                val confirmedSampleRate = confirmedPreferred?.format?.sampleRate?.takeIf { it > 0 } ?: currentSourceSampleRate
                val confirmedBitDepth = when (confirmedPreferred?.format?.encoding) {
                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                    AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32
                    AudioFormat.ENCODING_PCM_16BIT -> 16
                    else -> currentSourceBitDepth
                }

                onActiveUsbAudioDeviceChanged?.invoke(usbDevice)

                if (isVerifiedBitPerfect) {
                    val telemetryText = "$confirmedBitDepth-bit / ${UsbAudioStatus.formatSampleRate(confirmedSampleRate)} $currentSourceCodec • Bit-Perfect"
                    updateStatusInternal(
                        isUsbDacConnected = true,
                        dacName = dacName,
                        supportedSampleRates = supportedSampleRates,
                        supportedBitDepths = supportedBitDepths,
                        isExclusiveBitPerfectEnabled = true,
                        isMixerBypassActive = true,
                        isBitPerfectSupported = true,
                        sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)} ($currentSourceChannels ch)",
                        outputFormatText = "Direct HAL Hardware Stream ($dacName)",
                        liveFormatTelemetry = telemetryText,
                        hasUsbPermission = hasUsbPerm,
                        sourceSampleRate = currentSourceSampleRate,
                        sourceBitDepth = currentSourceBitDepth,
                        sourceChannels = currentSourceChannels,
                        sourceCodec = currentSourceCodec,
                        outputSampleRate = confirmedSampleRate,
                        outputBitDepth = confirmedBitDepth,
                        mixerBehavior = "Bit-Perfect Direct Bypass (Verified)",
                        supportedEncodings = supportedEncodings
                    )
                    return
                } else {
                    // Truthful status when bit-perfect mixer bypass could not be confirmed by OS
                    updateStatusInternal(
                        isUsbDacConnected = true,
                        dacName = dacName,
                        supportedSampleRates = supportedSampleRates,
                        supportedBitDepths = supportedBitDepths,
                        isExclusiveBitPerfectEnabled = true,
                        isMixerBypassActive = false,
                        isBitPerfectSupported = bitPerfectSupported,
                        sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
                        outputFormatText = "USB Audio Direct Route ($dacName)",
                        liveFormatTelemetry = "USB DAC: $dacName • Standard Route",
                        hasUsbPermission = hasUsbPerm,
                        sourceSampleRate = currentSourceSampleRate,
                        sourceBitDepth = currentSourceBitDepth,
                        sourceChannels = currentSourceChannels,
                        sourceCodec = currentSourceCodec,
                        outputSampleRate = currentSourceSampleRate,
                        outputBitDepth = currentSourceBitDepth,
                        mixerBehavior = if (bitPerfectSupported) "Mixer Bypass Standby" else "Direct USB Route",
                        supportedEncodings = supportedEncodings
                    )
                    return
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to apply bit-perfect mixer attributes", e)
            }
        }

        // Direct Routing fallback for API 29-33
        onActiveUsbAudioDeviceChanged?.invoke(usbDevice)
        val fallbackTelemetry = "USB DAC: $dacName • Direct Output"
        updateStatusInternal(
            isUsbDacConnected = true,
            dacName = dacName,
            isExclusiveBitPerfectEnabled = true,
            isMixerBypassActive = false,
            isBitPerfectSupported = false,
            sourceFormatText = "$currentSourceCodec $currentSourceBitDepth-bit / ${UsbAudioStatus.formatSampleRate(currentSourceSampleRate)}",
            outputFormatText = "USB Audio Direct ($dacName)",
            liveFormatTelemetry = fallbackTelemetry,
            hasUsbPermission = hasUsbPerm,
            sourceSampleRate = currentSourceSampleRate,
            sourceBitDepth = currentSourceBitDepth,
            sourceChannels = currentSourceChannels,
            sourceCodec = currentSourceCodec,
            outputSampleRate = currentSourceSampleRate,
            outputBitDepth = currentSourceBitDepth,
            mixerBehavior = "Direct Route (Legacy)"
        )
    }

    @OptIn(UnstableApi::class)
    fun updateMediaAudioFormat(sampleRate: Int, bitDepth: Int, channels: Int, codec: String) {
        if (sampleRate > 0) currentSourceSampleRate = sampleRate
        if (bitDepth in listOf(16, 24, 32)) currentSourceBitDepth = bitDepth
        if (channels in 1..8) currentSourceChannels = channels
        if (codec.isNotEmpty()) currentSourceCodec = codec

        val format = Format.Builder()
            .setSampleRate(currentSourceSampleRate)
            .setPcmEncoding(
                when (currentSourceBitDepth) {
                    24 -> C.ENCODING_PCM_24BIT
                    32 -> C.ENCODING_PCM_32BIT
                    else -> C.ENCODING_PCM_16BIT
                }
            )
            .setChannelCount(currentSourceChannels)
            .setSampleMimeType(
                if (currentSourceCodec.contains("FLAC", ignoreCase = true)) "audio/flac" else "audio/raw"
            )
            .build()

        enableBitPerfectBypass(isExclusiveRequested, format)
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

    private fun clearBitPerfectRouting() {
        val usbDevice = activeUsbAudioDevice
        if (usbDevice != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                audioManager.clearPreferredMixerAttributes(audioAttributes, usbDevice)
                Log.d(tag, "Cleared preferred mixer attributes for USB DAC")
            } catch (e: Exception) {
                Log.e(tag, "Failed to clear preferred mixer attributes", e)
            }
        }
        onActiveUsbAudioDeviceChanged?.invoke(null)
    }

    private fun updateStatusInternal(
        isUsbDacConnected: Boolean = _status.value.isUsbDacConnected,
        dacName: String = _status.value.dacName,
        supportedSampleRates: List<Int> = _status.value.supportedSampleRates,
        supportedBitDepths: List<Int> = _status.value.supportedBitDepths,
        isExclusiveBitPerfectEnabled: Boolean = _status.value.isExclusiveBitPerfectEnabled,
        isMixerBypassActive: Boolean = _status.value.isMixerBypassActive,
        isBitPerfectSupported: Boolean = _status.value.isBitPerfectSupported,
        sourceFormatText: String = _status.value.sourceFormatText,
        outputFormatText: String = _status.value.outputFormatText,
        liveFormatTelemetry: String = _status.value.liveFormatTelemetry,
        hasUsbPermission: Boolean = _status.value.hasUsbPermission,
        sourceSampleRate: Int = _status.value.sourceSampleRate,
        sourceBitDepth: Int = _status.value.sourceBitDepth,
        sourceChannels: Int = _status.value.sourceChannels,
        sourceCodec: String = _status.value.sourceCodec,
        outputSampleRate: Int = _status.value.outputSampleRate,
        outputBitDepth: Int = _status.value.outputBitDepth,
        mixerBehavior: String = _status.value.mixerBehavior,
        supportedEncodings: List<Int> = _status.value.supportedEncodings
    ) {
        _status.update {
            it.copy(
                isUsbDacConnected = isUsbDacConnected,
                dacName = dacName,
                supportedSampleRates = supportedSampleRates,
                supportedBitDepths = supportedBitDepths,
                isExclusiveBitPerfectEnabled = isExclusiveBitPerfectEnabled,
                isMixerBypassActive = isMixerBypassActive,
                isBitPerfectSupported = isBitPerfectSupported,
                sourceFormatText = sourceFormatText,
                outputFormatText = outputFormatText,
                liveFormatTelemetry = liveFormatTelemetry,
                hasUsbPermission = hasUsbPermission,
                sourceSampleRate = sourceSampleRate,
                sourceBitDepth = sourceBitDepth,
                sourceChannels = sourceChannels,
                sourceCodec = sourceCodec,
                outputSampleRate = outputSampleRate,
                outputBitDepth = outputBitDepth,
                mixerBehavior = mixerBehavior,
                supportedEncodings = supportedEncodings
            )
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.mediaplayer.USB_PERMISSION"
    }
}

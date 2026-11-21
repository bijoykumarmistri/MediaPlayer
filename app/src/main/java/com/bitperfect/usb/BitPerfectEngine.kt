package com.bitperfect.usb

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * High-level Kotlin facade for your UI.
 *
 * Manages the native libusb/UAC engine (default, bypasses Android AudioFlinger/ALSA completely)
 * and optionally delegates to [Android14BitPerfectFallback] when enabled on Android 14+ devices.
 */
class BitPerfectEngine(
    private val context: Context
) {
    interface Listener {
        fun onDacConnected(dacName: String, uacVersionHex: Int)
        fun onDacDisconnected()
        fun onFormatChanged(
            sampleRateHz: Int,
            sourceBitDepth: Int,
            wireBitDepth: Int,
            channels: Int,
            codecName: String
        )
        fun onPositionChanged(positionMs: Long, durationMs: Long)
        fun onStatusMessage(message: String)
        fun onError(errorMessage: String)
        fun onPlaybackCompleted()
    }

    enum class BackendMode {
        /** Direct libusb isochronous UAC 1.0/2.0 driver (bypasses AudioFlinger & ALSA). */
        EXCLUSIVE_USB_LIBUSB,

        /** Android 14+ native AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT fallback. */
        ANDROID_14_NATIVE_BIT_PERFECT
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var listener: Listener? = null

    @Volatile
    private var nativeHandle: Long = 0L

    @Volatile
    var backendMode: BackendMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        BackendMode.ANDROID_14_NATIVE_BIT_PERFECT
    } else {
        BackendMode.EXCLUSIVE_USB_LIBUSB
    }
        private set

    private val usbDacManager: UsbDacManager by lazy {
        UsbDacManager(
            context = context.applicationContext,
            onDacReady = { fd, deviceName ->
                ioScope.launch {
                    val ok = initializeNativeDac(fd)
                    if (!ok) {
                        postToMain { listener?.onError("Failed to initialize USB DAC: $deviceName") }
                    }
                }
            },
            onDacDetached = {
                // Stop native libusb threads BEFORE UsbDacManager closes the Android UsbDeviceConnection
                releaseNativeDacSync()
                postToMain {
                    listener?.onStatusMessage("USB DAC detached")
                    listener?.onDacDisconnected()
                }
            },
            onStatus = { msg -> postToMain { listener?.onStatusMessage(msg) } },
            onError = { err -> postToMain { listener?.onError(err) } }
        )
    }

    private val android14Fallback: Android14BitPerfectFallback? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Android14BitPerfectFallback(
                context = context.applicationContext,
                callback = object : Android14BitPerfectFallback.Callback {
                    override fun onFormatConfigured(sampleRate: Int, bitDepth: Int, channels: Int, codec: String) {
                        postToMain {
                            listener?.onFormatChanged(sampleRate, bitDepth, bitDepth, channels, codec)
                        }
                    }

                    override fun onPositionChanged(positionMs: Long, durationMs: Long) {
                        postToMain { listener?.onPositionChanged(positionMs, durationMs) }
                    }

                    override fun onStatus(message: String) {
                        postToMain { listener?.onStatusMessage(message) }
                    }

                    override fun onError(error: String) {
                        postToMain { listener?.onError(error) }
                    }

                    override fun onCompleted() {
                        postToMain { listener?.onPlaybackCompleted() }
                    }
                }
            )
        } else {
            null
        }
    }

    init {
        nativeHandle = nativeCreate()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * Switch between direct libusb Exclusive Mode and Android 14+ Native Bit-Perfect Mode.
     */
    fun setBackendMode(mode: BackendMode): Boolean {
        if (mode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            postToMain { listener?.onError("Native Android Bit-Perfect requires Android 14 (API 34)+") }
            return false
        }
        stop()
        if (mode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
            // Release exclusive libusb claim so Android's kernel snd-usb-audio driver can rebind
            releaseNativeDacSync()
            usbDacManager.closeConnectionOnly()
        }
        backendMode = mode
        postToMain { listener?.onStatusMessage("Backend switched to: $mode") }
        return true
    }

    /**
     * Scans for attached USB Audio Class DACs, requests USB Host permission if needed,
     * opens the UsbDeviceConnection, and initializes the native libusb engine.
     */
    fun initializeDac() {
        if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val usbDev = audioManager?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull {
                it.isSink && (
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_ACCESSORY
                )
            }
            if (usbDev != null) {
                val dacName = usbDev.productName?.toString()?.ifEmpty { "USB-C Audio DAC" } ?: "USB-C Audio DAC"
                postToMain {
                    listener?.onDacConnected(dacName, 0x0200)
                    listener?.onStatusMessage("Android Bit-Perfect DAC ready: $dacName")
                }
            }
            return
        }

        usbDacManager.register()
        usbDacManager.scanAndConnect()
    }

    fun loadFile(path: String) {
        ioScope.launch {
            if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android14Fallback?.loadFile(path)
                }
                return@launch
            }

            val handle = nativeHandle
            if (handle == 0L) return@launch
            nativeLoadFile(handle, path)
        }
    }

    fun play() {
        ioScope.launch {
            if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android14Fallback?.play()
                }
                return@launch
            }
            val handle = nativeHandle
            if (handle != 0L) nativePlay(handle)
        }
    }

    fun pause() {
        ioScope.launch {
            if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android14Fallback?.pause()
                }
                return@launch
            }
            val handle = nativeHandle
            if (handle != 0L) nativePause(handle)
        }
    }

    fun stop() {
        ioScope.launch {
            if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android14Fallback?.stop()
                }
                return@launch
            }
            val handle = nativeHandle
            if (handle != 0L) nativeStop(handle)
        }
    }

    fun seekTo(ms: Long) {
        ioScope.launch {
            if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android14Fallback?.seekTo(ms)
                }
                return@launch
            }
            val handle = nativeHandle
            if (handle != 0L) nativeSeekTo(handle, ms)
        }
    }

    /**
     * Sets DAC hardware volume (0.0f .. 1.0f) via UAC Feature Unit control transfer.
     * In Android 14+ fallback mode, delegates to AudioTrack gain (1.0f recommended for bit-perfect).
     */
    fun setHardwareVolume(percent: Float) {
        val clamped = percent.coerceIn(0f, 1f)
        ioScope.launch {
            if (backendMode == BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android14Fallback?.setVolume(clamped)
                }
                return@launch
            }
            val handle = nativeHandle
            if (handle != 0L) {
                val ok = nativeSetHardwareVolume(handle, clamped)
                if (!ok) {
                    Log.w(TAG, "DAC did not accept hardware volume command (may lack Feature Unit)")
                }
            }
        }
    }

    /**
     * Clean shutdown — call from your Service/ViewModel onCleared() or onDestroy().
     */
    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android14Fallback?.release()
        }
        releaseNativeDacSync()
        usbDacManager.unregisterAndClose()

        val handle = nativeHandle
        nativeHandle = 0L
        if (handle != 0L) {
            nativeDestroy(handle)
        }
        ioScope.cancel()
    }

    private fun initializeNativeDac(fd: Int): Boolean {
        val handle = nativeHandle
        if (handle == 0L) return false
        return nativeInitializeDac(handle, fd)
    }

    private fun releaseNativeDacSync() {
        val handle = nativeHandle
        if (handle != 0L) {
            nativeReleaseDac(handle)
        }
    }

    private fun postToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    // -------------------------------------------------------------------------
    // JNI Callbacks invoked from native C++ threads (BitPerfectJni.cpp)
    // -------------------------------------------------------------------------
    @Suppress("unused")
    private fun onNativePositionChanged(positionMs: Long, durationMs: Long) {
        postToMain { listener?.onPositionChanged(positionMs, durationMs) }
    }

    @Suppress("unused")
    private fun onNativeFormatChanged(
        sampleRate: Int,
        sourceBitDepth: Int,
        wireBitDepth: Int,
        channels: Int,
        codecName: String
    ) {
        postToMain {
            listener?.onFormatChanged(sampleRate, sourceBitDepth, wireBitDepth, channels, codecName)
        }
    }

    @Suppress("unused")
    private fun onNativeDacConnected(dacName: String, uacVersion: Int) {
        postToMain { listener?.onDacConnected(dacName, uacVersion) }
    }

    @Suppress("unused")
    private fun onNativeStatusMessage(message: String) {
        postToMain { listener?.onStatusMessage(message) }
    }

    @Suppress("unused")
    private fun onNativeError(error: String) {
        postToMain { listener?.onError(error) }
    }

    @Suppress("unused")
    private fun onNativePlaybackCompleted() {
        postToMain { listener?.onPlaybackCompleted() }
    }

    // -------------------------------------------------------------------------
    // Native JNI Declarations
    // -------------------------------------------------------------------------
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeInitializeDac(handle: Long, usbFd: Int): Boolean
    private external fun nativeReleaseDac(handle: Long)
    private external fun nativeLoadFile(handle: Long, path: String): Boolean
    private external fun nativePlay(handle: Long): Boolean
    private external fun nativePause(handle: Long)
    private external fun nativeStop(handle: Long)
    private external fun nativeSeekTo(handle: Long, positionMs: Long): Boolean
    private external fun nativeSetHardwareVolume(handle: Long, percent: Float): Boolean

    companion object {
        private const val TAG = "BitPerfectEngine"

        init {
            System.loadLibrary("bitperfect_usb")
        }
    }
}

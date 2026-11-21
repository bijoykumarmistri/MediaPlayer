package com.bitperfect.usb

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Android 14+ (API 34) Native Bit-Perfect USB Audio Fallback.
 *
 * Queries AudioManager.getSupportedMixerAttributes() for a connected USB DAC,
 * requests AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT via
 * AudioManager.setPreferredMixerAttributes(), and streams decoded PCM directly
 * through a routed AudioTrack.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Android14BitPerfectFallback(
    context: Context,
    private val callback: Callback
) {
    interface Callback {
        fun onFormatConfigured(sampleRate: Int, bitDepth: Int, channels: Int, codec: String)
        fun onPositionChanged(positionMs: Long, durationMs: Long)
        fun onStatus(message: String)
        fun onError(error: String)
        fun onCompleted()
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val playbackAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    @Volatile
    private var currentFilePath: String? = null

    @Volatile
    private var activeUsbDevice: AudioDeviceInfo? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var playbackThread: Thread? = null

    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val seekTargetUs = AtomicLong(-1L)

    @Volatile
    private var volumeGain: Float = 1.0f

    fun findBitPerfectUsbDeviceName(): String? {
        val dev = findUsbOutputDevice() ?: return null
        val supported = audioManager.getSupportedMixerAttributes(dev)
        val hasBitPerfect = supported.any {
            it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
        }
        return if (hasBitPerfect) dev.productName.toString() else null
    }

    fun loadFile(path: String) {
        stop()
        currentFilePath = path
        callback.onStatus("Prepared (Android 14+ Bit-Perfect): ${path.substringAfterLast('/')}")
    }

    fun play() {
        val path = currentFilePath
        if (path == null) {
            callback.onError("No file loaded")
            return
        }

        if (running.get()) {
            paused.set(false)
            audioTrack?.play()
            callback.onStatus("Playing (Android 14+ Native Bit-Perfect)")
            return
        }

        startPlaybackThread(path)
    }

    fun pause() {
        paused.set(true)
        try {
            audioTrack?.pause()
        } catch (_: IllegalStateException) {
        }
        callback.onStatus("Paused")
    }

    fun stop() {
        running.set(false)
        paused.set(false)
        seekTargetUs.set(-1L)

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (_: Exception) {
        }

        playbackThread?.join(1000)
        playbackThread = null

        releaseAudioTrackAndMixer()
        callback.onStatus("Stopped")
    }

    fun seekTo(ms: Long) {
        seekTargetUs.set((ms.coerceAtLeast(0L)) * 1000L)
    }

    fun setVolume(percent: Float) {
        volumeGain = percent.coerceIn(0f, 1f)
        audioTrack?.setVolume(volumeGain)
    }

    fun release() {
        stop()
    }

    private fun findUsbOutputDevice(): AudioDeviceInfo? {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { dev ->
            dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                dev.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    private fun selectBitPerfectMixerAttribute(
        device: AudioDeviceInfo,
        sampleRate: Int,
        pcmEncoding: Int,
        channelMask: Int
    ): AudioMixerAttributes? {
        val supported = audioManager.getSupportedMixerAttributes(device)
        val bitPerfectList = supported.filter {
            it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
        }

        // Exact match on sample rate, encoding, and channel mask
        bitPerfectList.firstOrNull { attr ->
            attr.format.sampleRate == sampleRate &&
                attr.format.encoding == pcmEncoding &&
                attr.format.channelMask == channelMask
        }?.let { return it }

        // Fallback: exact sample rate & channel mask with highest compatible bit depth
        return bitPerfectList
            .filter { it.format.sampleRate == sampleRate && it.format.channelMask == channelMask }
            .maxByOrNull { encodingBitDepth(it.format.encoding) }
    }

    private fun startPlaybackThread(path: String) {
        val usbDevice = findUsbOutputDevice()
        if (usbDevice == null) {
            callback.onError("No USB DAC connected for Android 14+ Bit-Perfect playback")
            return
        }

        running.set(true)
        paused.set(false)

        playbackThread = thread(name = "Android14BitPerfectThread", priority = Thread.MAX_PRIORITY) {
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null

            try {
                extractor.setDataSource(path)
                var audioTrackIndex = -1
                var inputFormat: MediaFormat? = null

                for (i in 0 until extractor.trackCount) {
                    val fmt = extractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        inputFormat = fmt
                        break
                    }
                }

                if (audioTrackIndex < 0 || inputFormat == null) {
                    callback.onError("No audio track found in file")
                    return@thread
                }

                extractor.selectTrack(audioTrackIndex)
                val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
                val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    inputFormat.getLong(MediaFormat.KEY_DURATION)
                } else {
                    0L
                }

                val requestedEncoding = if (inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                } else {
                    AudioFormat.ENCODING_PCM_16BIT
                }

                val channelMask = if (channels == 1) {
                    AudioFormat.CHANNEL_OUT_MONO
                } else {
                    AudioFormat.CHANNEL_OUT_STEREO
                }

                // Request MIXER_BEHAVIOR_BIT_PERFECT on the USB DAC
                val chosenMixer = selectBitPerfectMixerAttribute(
                    usbDevice, sampleRate, requestedEncoding, channelMask
                )

                val trackFormat: AudioFormat = if (chosenMixer != null) {
                    val applied = audioManager.setPreferredMixerAttributes(
                        playbackAttrs, usbDevice, chosenMixer
                    )
                    if (applied) {
                        activeUsbDevice = usbDevice
                        Log.i(TAG, "Enabled MIXER_BEHAVIOR_BIT_PERFECT: ${chosenMixer.format}")
                        chosenMixer.format
                    } else {
                        Log.w(TAG, "setPreferredMixerAttributes returned false; using standard AudioTrack")
                        buildStandardAudioFormat(sampleRate, requestedEncoding, channelMask)
                    }
                } else {
                    Log.w(TAG, "DAC/HAL has no MIXER_BEHAVIOR_BIT_PERFECT entry for $sampleRate Hz")
                    buildStandardAudioFormat(sampleRate, requestedEncoding, channelMask)
                }

                // Configure MediaCodec to output the matching PCM encoding when supported
                inputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, trackFormat.encoding)
                codec = MediaCodec.createDecoderByType(mime).apply {
                    configure(inputFormat, null, null, 0)
                    start()
                }

                val minBuf = AudioTrack.getMinBufferSize(
                    trackFormat.sampleRate, trackFormat.channelMask, trackFormat.encoding
                ).coerceAtLeast(8192)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(playbackAttrs)
                    .setAudioFormat(trackFormat)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(minBuf * 4)
                    .build()

                track.preferredDevice = usbDevice
                track.setVolume(volumeGain)
                track.play()
                audioTrack = track

                callback.onFormatConfigured(
                    trackFormat.sampleRate,
                    encodingBitDepth(trackFormat.encoding),
                    channels,
                    mime.substringAfter("audio/").uppercase()
                )
                callback.onStatus("Playing (Android 14+ Native Bit-Perfect)")

                val bufferInfo = MediaCodec.BufferInfo()
                var inputEof = false
                var outputEof = false
                var lastReportMs = 0L

                while (running.get() && !outputEof) {
                    // Handle seek
                    val targetUs = seekTargetUs.getAndSet(-1L)
                    if (targetUs >= 0L) {
                        extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        codec.flush()
                        track.flush()
                        inputEof = false
                    }

                    if (paused.get()) {
                        Thread.sleep(20)
                        continue
                    }

                    // Feed compressed input to MediaCodec
                    if (!inputEof) {
                        val inIdx = codec.dequeueInputBuffer(10_000)
                        if (inIdx >= 0) {
                            val inBuf: ByteBuffer = codec.getInputBuffer(inIdx)!!
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEof = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    // Drain decoded PCM and write directly to bit-perfect AudioTrack
                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            track.write(outBuf, bufferInfo.size, AudioTrack.WRITE_BLOCKING)
                        }
                        codec.releaseOutputBuffer(outIdx, false)

                        val posMs = bufferInfo.presentationTimeUs / 1000L
                        val now = System.currentTimeMillis()
                        if (now - lastReportMs >= 200L) {
                            callback.onPositionChanged(posMs, durationUs / 1000L)
                            lastReportMs = now
                        }

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputEof = true
                        }
                    }
                }

                if (outputEof && running.get()) {
                    callback.onPositionChanged(durationUs / 1000L, durationUs / 1000L)
                    callback.onCompleted()
                }
            } catch (e: Exception) {
                if (running.get()) {
                    Log.e(TAG, "Android 14+ Bit-Perfect playback error", e)
                    callback.onError("Android 14+ playback error: ${e.message}")
                }
            } finally {
                running.set(false)
                try {
                    codec?.stop()
                    codec?.release()
                } catch (_: Exception) {
                }
                extractor.release()
                releaseAudioTrackAndMixer()
            }
        }
    }

    private fun releaseAudioTrackAndMixer() {
        try {
            audioTrack?.release()
        } catch (_: Exception) {
        } finally {
            audioTrack = null
        }
        activeUsbDevice?.let { dev ->
            try {
                audioManager.clearPreferredMixerAttributes(playbackAttrs, dev)
            } catch (_: Exception) {
            }
        }
        activeUsbDevice = null
    }

    private fun buildStandardAudioFormat(sr: Int, enc: Int, mask: Int): AudioFormat {
        return AudioFormat.Builder()
            .setSampleRate(sr)
            .setEncoding(enc)
            .setChannelMask(mask)
            .build()
    }

    private fun encodingBitDepth(encoding: Int): Int = when (encoding) {
        AudioFormat.ENCODING_PCM_16BIT -> 16
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
        AudioFormat.ENCODING_PCM_32BIT -> 32
        AudioFormat.ENCODING_PCM_FLOAT -> 32
        else -> 16
    }

    companion object {
        private const val TAG = "Android14BitPerfect"
    }
}

#pragma once

#include "AudioDecoder.h"
#include "LockFreeRingBuffer.h"
#include "UsbAudioEngine.h"

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

namespace bitperfect {

struct PipelineCallbacks {
    std::function<void(int64_t positionMs, int64_t durationMs)> onPositionChanged;
    std::function<void(uint32_t sampleRate, uint32_t sourceBitDepth,
                       uint32_t wireBitDepth, uint32_t channels,
                       const std::string& codecName)> onFormatChanged;
    std::function<void(const std::string& dacName, uint16_t uacVersion)> onDacConnected;
    std::function<void(const std::string& status)> onStatusMessage;
    std::function<void(const std::string& error)> onError;
    std::function<void()> onPlaybackCompleted;
};

/**
 * Coordinates the AudioDecoder thread, wait-free SPSC LockFreeRingBuffer,
 * and UsbAudioEngine isochronous USB playback thread.
 */
class AudioPipeline {
public:
    AudioPipeline();
    ~AudioPipeline();

    AudioPipeline(const AudioPipeline&) = delete;
    AudioPipeline& operator=(const AudioPipeline&) = delete;

    void setCallbacks(PipelineCallbacks callbacks);

    bool initializeDac(int usbFd);
    void releaseDac();

    bool loadFile(const std::string& filePath);
    bool play();
    void pause();
    void stop();
    bool seekToMs(int64_t positionMs);
    bool setHardwareVolume(float percent);

    bool isDacReady() const noexcept { return usbEngine_.isOpen(); }

private:
    void startDecoderThread();
    void stopDecoderThread();
    void decoderThreadLoop();
    void reportPosition(bool force);

    UsbAudioEngine usbEngine_;
    LockFreeRingBuffer<int32_t> ringBuffer_; // 524,288 samples (~1.36s @ 192kHz stereo)
    std::unique_ptr<AudioDecoder> decoder_;

    PipelineCallbacks callbacks_;
    std::mutex apiMutex_;
    std::mutex decoderWakeMutex_;
    std::condition_variable decoderWakeCv_;

    std::thread decoderThread_;
    std::atomic<bool> decoderRunning_{false};
    std::atomic<bool> eofReached_{false};
    std::atomic<bool> completionNotified_{false};

    std::atomic<bool> seekRequested_{false};
    std::atomic<uint64_t> seekTargetFrame_{0};
    std::atomic<uint64_t> underrunCount_{0};

    AudioFormatInfo currentFormat_{};
};

} // namespace bitperfect

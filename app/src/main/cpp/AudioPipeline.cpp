#include "AudioPipeline.h"

#include <android/log.h>
#include <algorithm>
#include <chrono>
#include <vector>

#define LOG_TAG "AudioPipeline"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace bitperfect {

namespace {
constexpr size_t kRingBufferCapacitySamples = 524288; // Power of 2: 2 MB of int32_t
constexpr uint64_t kDecodeChunkFrames = 2048;
} // namespace

AudioPipeline::AudioPipeline()
    : ringBuffer_(kRingBufferCapacitySamples) {
    usbEngine_.setErrorCallback([this](const std::string& err) {
        if (callbacks_.onError) callbacks_.onError(err);
    });
    usbEngine_.setUnderrunCallback([this]() {
        underrunCount_.fetch_add(1, std::memory_order_relaxed);
    });
}

AudioPipeline::~AudioPipeline() {
    releaseDac();
}

void AudioPipeline::setCallbacks(PipelineCallbacks callbacks) {
    std::lock_guard<std::mutex> lock(apiMutex_);
    callbacks_ = std::move(callbacks);
}

bool AudioPipeline::initializeDac(int usbFd) {
    std::lock_guard<std::mutex> lock(apiMutex_);

    stopDecoderThread();
    usbEngine_.close();

    std::string err;
    if (!usbEngine_.open(usbFd, err)) {
        if (callbacks_.onError) callbacks_.onError(err);
        return false;
    }

    const auto& caps = usbEngine_.getCapabilities();
    std::string dacName = caps.manufacturer;
    if (!caps.productName.empty()) {
        if (!dacName.empty()) dacName += " ";
        dacName += caps.productName;
    }
    if (dacName.empty()) {
        dacName = "USB Audio DAC";
    }

    if (callbacks_.onDacConnected) {
        callbacks_.onDacConnected(dacName, static_cast<uint16_t>(caps.uacVersion));
    }
    if (callbacks_.onStatusMessage) {
        callbacks_.onStatusMessage("DAC Ready: " + dacName);
    }
    return true;
}

void AudioPipeline::releaseDac() {
    std::lock_guard<std::mutex> lock(apiMutex_);
    stopDecoderThread();
    usbEngine_.close();
    if (decoder_) {
        decoder_->close();
        decoder_.reset();
    }
    ringBuffer_.reset();
}

bool AudioPipeline::loadFile(const std::string& filePath) {
    std::lock_guard<std::mutex> lock(apiMutex_);

    if (!usbEngine_.isOpen()) {
        if (callbacks_.onError) callbacks_.onError("Cannot load file: USB DAC not initialized");
        return false;
    }

    stopDecoderThread();
    usbEngine_.stopStreaming();
    ringBuffer_.reset();

    auto newDecoder = createDecoderForFile(filePath);
    if (!newDecoder) {
        if (callbacks_.onError) callbacks_.onError("Unsupported or unreadable audio file: " + filePath);
        return false;
    }

    currentFormat_ = newDecoder->getFormat();

    std::string err;
    if (!usbEngine_.configureStream(currentFormat_.sampleRate,
                                    currentFormat_.bitDepth,
                                    currentFormat_.channels,
                                    err)) {
        if (callbacks_.onError) callbacks_.onError(err);
        return false;
    }

    decoder_ = std::move(newDecoder);
    eofReached_.store(false, std::memory_order_release);
    completionNotified_.store(false, std::memory_order_release);
    seekRequested_.store(false, std::memory_order_release);
    usbEngine_.setPlayedFrames(0);
    usbEngine_.setPaused(true);

    const auto& cfg = usbEngine_.getActiveConfig();
    if (callbacks_.onFormatChanged) {
        callbacks_.onFormatChanged(cfg.sampleRate, cfg.sourceBitDepth,
                                   cfg.wireBitDepth, cfg.channels,
                                   currentFormat_.codecName);
    }

    // Start decoder thread first to pre-fill the lock-free ring buffer before starting USB URBs
    startDecoderThread();

    // Brief pre-roll wait (up to 150 ms) so initial URBs have audio immediately on play()
    const auto preRollDeadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(150);
    const size_t preRollTarget = static_cast<size_t>(currentFormat_.sampleRate / 10) * currentFormat_.channels;
    while (ringBuffer_.availableRead() < preRollTarget &&
           !eofReached_.load(std::memory_order_acquire) &&
           std::chrono::steady_clock::now() < preRollDeadline) {
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }

    if (!usbEngine_.startStreaming(&ringBuffer_, err)) {
        stopDecoderThread();
        if (callbacks_.onError) callbacks_.onError(err);
        return false;
    }

    reportPosition(true);
    if (callbacks_.onStatusMessage) {
        callbacks_.onStatusMessage("Loaded: " + currentFormat_.codecName + " " +
                                   std::to_string(cfg.sampleRate) + " Hz / " +
                                   std::to_string(cfg.wireBitDepth) + "-bit");
    }
    return true;
}

bool AudioPipeline::play() {
    std::lock_guard<std::mutex> lock(apiMutex_);
    if (!usbEngine_.isOpen() || !decoder_) {
        return false;
    }

    if (!usbEngine_.isStreaming()) {
        std::string err;
        if (!usbEngine_.startStreaming(&ringBuffer_, err)) {
            if (callbacks_.onError) callbacks_.onError(err);
            return false;
        }
    }

    if (!decoderRunning_.load(std::memory_order_acquire)) {
        startDecoderThread();
    }

    usbEngine_.setPaused(false);
    decoderWakeCv_.notify_one();
    if (callbacks_.onStatusMessage) callbacks_.onStatusMessage("Playing (Bit-Perfect)");
    return true;
}

void AudioPipeline::pause() {
    std::lock_guard<std::mutex> lock(apiMutex_);
    usbEngine_.setPaused(true);
    if (callbacks_.onStatusMessage) callbacks_.onStatusMessage("Paused");
}

void AudioPipeline::stop() {
    std::lock_guard<std::mutex> lock(apiMutex_);
    usbEngine_.setPaused(true);
    stopDecoderThread();
    usbEngine_.stopStreaming();
    ringBuffer_.reset();
    if (decoder_) {
        decoder_->seekToFrame(0);
    }
    usbEngine_.setPlayedFrames(0);
    eofReached_.store(false, std::memory_order_release);
    completionNotified_.store(false, std::memory_order_release);
    reportPosition(true);
    if (callbacks_.onStatusMessage) callbacks_.onStatusMessage("Stopped");
}

bool AudioPipeline::seekToMs(int64_t positionMs) {
    std::lock_guard<std::mutex> lock(apiMutex_);
    if (!decoder_ || currentFormat_.sampleRate == 0) return false;

    const int64_t clampedMs = std::max<int64_t>(0, positionMs);
    uint64_t targetFrame =
        (static_cast<uint64_t>(clampedMs) * currentFormat_.sampleRate) / 1000ULL;
    if (currentFormat_.totalFrames > 0 && targetFrame >= currentFormat_.totalFrames) {
        targetFrame = currentFormat_.totalFrames - 1;
    }

    seekTargetFrame_.store(targetFrame, std::memory_order_release);
    seekRequested_.store(true, std::memory_order_release);
    eofReached_.store(false, std::memory_order_release);
    completionNotified_.store(false, std::memory_order_release);
    decoderWakeCv_.notify_one();
    return true;
}

bool AudioPipeline::setHardwareVolume(float percent) {
    return usbEngine_.setHardwareVolumePercent(percent);
}

void AudioPipeline::startDecoderThread() {
    stopDecoderThread();
    decoderRunning_.store(true, std::memory_order_release);
    decoderThread_ = std::thread(&AudioPipeline::decoderThreadLoop, this);
}

void AudioPipeline::stopDecoderThread() {
    decoderRunning_.store(false, std::memory_order_release);
    decoderWakeCv_.notify_all();
    if (decoderThread_.joinable()) {
        decoderThread_.join();
    }
}

void AudioPipeline::decoderThreadLoop() {
    const uint32_t inChannels = currentFormat_.channels > 0 ? currentFormat_.channels : 2;
    const uint32_t outChannels = usbEngine_.getActiveConfig().channels > 0 ? usbEngine_.getActiveConfig().channels : inChannels;
    const bool expandMonoToStereo = (inChannels == 1 && outChannels == 2);

    std::vector<int32_t> decodeBuf(static_cast<size_t>(kDecodeChunkFrames) * inChannels);
    std::vector<int32_t> stereoBuf(expandMonoToStereo ? static_cast<size_t>(kDecodeChunkFrames) * 2 : 0);

    auto lastPosReport = std::chrono::steady_clock::now();

    while (decoderRunning_.load(std::memory_order_acquire)) {
        // 1. Handle pending seek cleanly without stopping the USB bus
        if (seekRequested_.exchange(false, std::memory_order_acq_rel)) {
            const bool wasPaused = usbEngine_.isPaused();
            usbEngine_.setPaused(true);

            // Allow in-flight URB callback to observe paused state, then discard old buffered audio
            std::this_thread::sleep_for(std::chrono::milliseconds(15));
            ringBuffer_.consumeAll();

            const uint64_t target = seekTargetFrame_.load(std::memory_order_acquire);
            if (decoder_ && decoder_->seekToFrame(target)) {
                usbEngine_.setPlayedFrames(target);
            }
            if (!wasPaused) {
                usbEngine_.setPaused(false);
            }
            reportPosition(true);
        }

        // 2. Decode into wait-free ring buffer while space is available
        const size_t neededSpace = static_cast<size_t>(kDecodeChunkFrames) * outChannels;
        if (!eofReached_.load(std::memory_order_acquire) &&
            ringBuffer_.availableWrite() >= neededSpace) {
            const uint64_t framesRead =
                decoder_ ? decoder_->readFramesS32(decodeBuf.data(), kDecodeChunkFrames) : 0;

            if (framesRead > 0) {
                const int32_t* srcSamples = decodeBuf.data();
                size_t totalSamplesToWrite = static_cast<size_t>(framesRead) * inChannels;

                if (expandMonoToStereo) {
                    for (uint64_t i = 0; i < framesRead; ++i) {
                        stereoBuf[i * 2 + 0] = decodeBuf[i];
                        stereoBuf[i * 2 + 1] = decodeBuf[i];
                    }
                    srcSamples = stereoBuf.data();
                    totalSamplesToWrite = static_cast<size_t>(framesRead) * 2;
                }

                size_t written = 0;
                while (written < totalSamplesToWrite && decoderRunning_.load(std::memory_order_acquire)) {
                    written += ringBuffer_.write(srcSamples + written, totalSamplesToWrite - written);
                    if (written < totalSamplesToWrite) {
                        std::this_thread::sleep_for(std::chrono::milliseconds(2));
                    }
                }
            } else {
                eofReached_.store(true, std::memory_order_release);
            }
        } else {
            // Ring buffer is full or EOF reached; check completion & throttle CPU usage
            if (eofReached_.load(std::memory_order_acquire) &&
                ringBuffer_.availableRead() == 0 &&
                !usbEngine_.isPaused() &&
                !completionNotified_.exchange(true, std::memory_order_acq_rel)) {
                usbEngine_.setPaused(true);
                reportPosition(true);
                if (callbacks_.onPlaybackCompleted) {
                    callbacks_.onPlaybackCompleted();
                }
            }

            std::unique_lock<std::mutex> lk(decoderWakeMutex_);
            decoderWakeCv_.wait_for(lk, std::chrono::milliseconds(15));
        }

        // 3. Emit throttled UI position updates (~200 ms interval)
        const auto now = std::chrono::steady_clock::now();
        if (now - lastPosReport >= std::chrono::milliseconds(200)) {
            reportPosition(false);
            lastPosReport = now;
        }
    }
}

void AudioPipeline::reportPosition(bool force) {
    if (!callbacks_.onPositionChanged || currentFormat_.sampleRate == 0) return;
    if (!force && usbEngine_.isPaused()) return;

    const uint64_t played = usbEngine_.getPlayedFrames();
    const int64_t posMs =
        static_cast<int64_t>((played * 1000ULL) / currentFormat_.sampleRate);
    const int64_t durMs =
        static_cast<int64_t>((currentFormat_.totalFrames * 1000ULL) / currentFormat_.sampleRate);

    callbacks_.onPositionChanged(posMs, durMs);
}

} // namespace bitperfect

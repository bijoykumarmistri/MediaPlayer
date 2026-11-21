#pragma once

#include "LockFreeRingBuffer.h"
#include "UacDescriptorParser.h"

#include <libusb.h>
#include <atomic>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace bitperfect {

struct ActiveStreamConfig {
    uint32_t sampleRate = 0;
    uint32_t sourceBitDepth = 0;
    uint32_t wireBitDepth = 0;
    uint32_t wireSubslotSize = 0;
    uint32_t channels = 0;
    bool usingFeedback = false;
    IsoSyncType syncType = IsoSyncType::NONE;
};

/**
 * Core native USB Audio Class (UAC 1.0 / UAC 2.0) isochronous streaming engine.
 *
 * Uses libusb with LIBUSB_OPTION_NO_DEVICE_DISCOVERY + libusb_wrap_sys_device()
 * on the raw file descriptor obtained from Android's UsbDeviceConnection.
 */
class UsbAudioEngine {
public:
    using ErrorCallback = std::function<void(const std::string& message)>;
    using UnderrunCallback = std::function<void()>;

    UsbAudioEngine();
    ~UsbAudioEngine();

    UsbAudioEngine(const UsbAudioEngine&) = delete;
    UsbAudioEngine& operator=(const UsbAudioEngine&) = delete;

    /**
     * Wraps the Android USB file descriptor, parses UAC 1.0/2.0 descriptors,
     * detaches kernel drivers (e.g., snd-usb-audio), and claims Audio Control.
     */
    bool open(int usbFileDescriptor, std::string& outError);

    /**
     * Cleanly stops streaming, drains/frees all URBs, releases interfaces, and
     * exits libusb. Does NOT close the raw OS fd (Android UsbDeviceConnection owns it).
     */
    void close();

    bool isOpen() const noexcept { return isOpen_.load(std::memory_order_acquire); }
    bool isStreaming() const noexcept { return streaming_.load(std::memory_order_acquire); }

    const UacDeviceCapabilities& getCapabilities() const { return caps_; }
    const ActiveStreamConfig& getActiveConfig() const { return activeConfig_; }

    /**
     * Selects the best matching Alternate Setting, claims the Audio Streaming
     * interface, sets the alt setting, and programs the DAC hardware clock.
     */
    bool configureStream(uint32_t sampleRate, uint32_t bitDepth, uint32_t channels,
                         std::string& outError);

    /**
     * Starts multi-buffered asynchronous isochronous OUT transfers (and async
     * Feedback IN transfers if supported by the DAC) on a high-priority thread.
     */
    bool startStreaming(LockFreeRingBuffer<int32_t>* ringBuffer, std::string& outError);

    /**
     * Cancels all in-flight URBs, drains completions, joins the USB event thread,
     * and returns the AS interface to Alternate Setting 0 (zero bandwidth).
     */
    void stopStreaming();

    /**
     * When paused, the USB bus stays alive outputting zeroed digital silence
     * packets so the DAC never loses clock lock or pops.
     */
    void setPaused(bool paused) noexcept { paused_.store(paused, std::memory_order_release); }
    bool isPaused() const noexcept { return paused_.load(std::memory_order_acquire); }

    /**
     * Hardware volume control via UAC 1.0 / UAC 2.0 Feature Unit.
     * Accepts normalized percentage in [0.0f, 1.0f] mapped across the DAC's
     * hardware dB range [minDb, maxDb].
     */
    bool setHardwareVolumePercent(float percent);
    bool setHardwareMute(bool mute);
    bool supportsHardwareVolume() const noexcept { return volumeRangeValid_; }

    /**
     * Optional quirk toggle for rare DACs expecting 24-bit samples right-aligned
     * (LSB-justified) inside 4-byte subslots instead of USB-spec MSB-aligned.
     */
    void setRightAlign24In32(bool enable) noexcept {
        rightAlign24In32_.store(enable, std::memory_order_relaxed);
    }

    uint64_t getPlayedFrames() const noexcept {
        return playedFrames_.load(std::memory_order_acquire);
    }
    void setPlayedFrames(uint64_t frames) noexcept {
        playedFrames_.store(frames, std::memory_order_release);
    }

    void setErrorCallback(ErrorCallback cb) { errorCallback_ = std::move(cb); }
    void setUnderrunCallback(UnderrunCallback cb) { underrunCallback_ = std::move(cb); }

private:
    struct OutUrbContext {
        UsbAudioEngine* engine = nullptr;
        libusb_transfer* transfer = nullptr;
        std::vector<uint8_t> packetBuffer;
        std::vector<int32_t> pcmScratch; // Pre-allocated, zero heap allocs in RT path
        bool inFlight = false;
    };

    struct FeedbackUrbContext {
        UsbAudioEngine* engine = nullptr;
        libusb_transfer* transfer = nullptr;
        std::vector<uint8_t> packetBuffer;
        bool inFlight = false;
    };

    bool selectBestFormat(uint32_t sampleRate, uint32_t bitDepth, uint32_t channels,
                          UacStreamFormat& outFormat) const;
    bool programHardwareClock(const UacStreamFormat& fmt, uint32_t sampleRate);
    void queryHardwareVolumeRange();

    void usbEventLoop();
    bool fillAndSubmitOutUrb(OutUrbContext* ctx);
    bool submitFeedbackUrb(FeedbackUrbContext* ctx);

    static void LIBUSB_CALL onOutTransferComplete(libusb_transfer* transfer);
    static void LIBUSB_CALL onFeedbackTransferComplete(libusb_transfer* transfer);

    void packPcmToWire(const int32_t* srcInterleaved, uint32_t frameCount,
                       uint8_t* dstWire) const noexcept;

    // libusb state
    libusb_context* ctx_{nullptr};
    libusb_device_handle* devHandle_{nullptr};
    UacDeviceCapabilities caps_{};
    UacStreamFormat activeFormat_{};
    ActiveStreamConfig activeConfig_{};

    std::vector<uint8_t> claimedInterfaces_;
    std::vector<uint8_t> detachedInterfaces_;
    uint8_t currentAsInterface_{0xFF};

    // Hardware volume state (1/256 dB units per UAC spec)
    uint8_t activeFeatureUnitId_{0};
    bool volumeUseMaster_{true};
    uint8_t volumeChannelCount_{2};
    bool volumeRangeValid_{false};
    int16_t volMin256Db_{-12800}; // -50.0 dB default fallback
    int16_t volMax256Db_{0};      //   0.0 dB
    int16_t volRes256Db_{256};    //   1.0 dB

    // Streaming & timing state
    std::atomic<bool> isOpen_{false};
    std::atomic<bool> streaming_{false};
    std::atomic<bool> paused_{false};
    std::atomic<bool> rightAlign24In32_{false};
    std::atomic<int> activeOutUrbs_{0};
    std::atomic<int> activeFeedbackUrbs_{0};
    std::atomic<uint64_t> playedFrames_{0};

    LockFreeRingBuffer<int32_t>* ringBuffer_{nullptr};

    double outPacketsPerSecond_{8000.0};
    double nominalFramesPerPacket_{0.0};
    std::atomic<double> currentFramesPerPacket_{0.0};
    double frameAccumulator_{0.0}; // Accessed only on the single USB event thread
    uint32_t packetsPerUrb_{8};

    std::vector<std::unique_ptr<OutUrbContext>> outUrbs_;
    std::vector<std::unique_ptr<FeedbackUrbContext>> feedbackUrbs_;

    std::thread eventThread_;
    int eventLoopCompleted_{0};

    std::mutex controlMutex_;
    ErrorCallback errorCallback_;
    UnderrunCallback underrunCallback_;
};

} // namespace bitperfect

#include "UsbAudioEngine.h"

#include <android/log.h>
#include <sys/resource.h>
#include <unistd.h>
#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstring>

#define LOG_TAG "UsbAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace bitperfect {

namespace {
constexpr int kNumOutUrbs = 12;
constexpr int kNumFeedbackUrbs = 4;

// UAC 1.0 requests
constexpr uint8_t kUac1SetCur = 0x01;
constexpr uint8_t kUac1GetMin = 0x82;
constexpr uint8_t kUac1GetMax = 0x83;
constexpr uint8_t kUac1GetRes = 0x84;

// UAC 2.0 requests
constexpr uint8_t kUac2Cur = 0x01;
constexpr uint8_t kUac2Range = 0x02;

// Control Selectors
constexpr uint8_t kCsSamFreqControl = 0x01;
constexpr uint8_t kFuMuteControl = 0x01;
constexpr uint8_t kFuVolumeControl = 0x02;

inline int16_t readLe16Signed(const uint8_t* p) {
    return static_cast<int16_t>(
        static_cast<uint16_t>(p[0]) | (static_cast<uint16_t>(p[1]) << 8));
}
} // namespace

UsbAudioEngine::UsbAudioEngine() = default;

UsbAudioEngine::~UsbAudioEngine() {
    close();
}

bool UsbAudioEngine::open(int usbFileDescriptor, std::string& outError) {
    close();

    if (usbFileDescriptor < 0) {
        outError = "Invalid USB file descriptor";
        return false;
    }

    // CRITICAL FOR ANDROID: Disable sysfs/usbfs device enumeration before libusb_init()
    int rc = libusb_set_option(nullptr, LIBUSB_OPTION_NO_DEVICE_DISCOVERY);
    if (rc != LIBUSB_SUCCESS) {
        LOGW("libusb_set_option(NO_DEVICE_DISCOVERY) returned %s (continuing)",
             libusb_error_name(rc));
    }

    rc = libusb_init(&ctx_);
    if (rc != LIBUSB_SUCCESS || !ctx_) {
        outError = std::string("libusb_init failed: ") + libusb_error_name(rc);
        LOGE("%s", outError.c_str());
        return false;
    }

    rc = libusb_wrap_sys_device(ctx_, static_cast<intptr_t>(usbFileDescriptor), &devHandle_);
    if (rc != LIBUSB_SUCCESS || !devHandle_) {
        outError = std::string("libusb_wrap_sys_device failed: ") + libusb_error_name(rc);
        LOGE("%s", outError.c_str());
        libusb_exit(ctx_);
        ctx_ = nullptr;
        return false;
    }

    // Enable auto-detach where supported
    libusb_set_auto_detach_kernel_driver(devHandle_, 1);

    // Parse UAC 1.0 / UAC 2.0 descriptors
    if (!UacDescriptorParser::parse(devHandle_, caps_)) {
        outError = "No supported PCM AudioStreaming OUT interfaces found on USB DAC";
        LOGE("%s", outError.c_str());
        close();
        return false;
    }

    // Dump complete diagnostic info to Logcat
    UacDescriptorParser::dumpCapabilitiesToLogcat(devHandle_, caps_);

    if (!caps_.playbackFormats.empty()) {
        activeFeatureUnitId_ = caps_.playbackFormats[0].featureUnitId;
    }

    isOpen_.store(true, std::memory_order_release);
    return true;
}

void UsbAudioEngine::close() {
    stopStreaming();

    std::lock_guard<std::mutex> lock(controlMutex_);
    isOpen_.store(false, std::memory_order_release);

    if (devHandle_) {
        for (uint8_t ifaceNum : claimedInterfaces_) {
            libusb_release_interface(devHandle_, ifaceNum);
        }
        claimedInterfaces_.clear();

        for (uint8_t ifaceNum : detachedInterfaces_) {
            libusb_attach_kernel_driver(devHandle_, ifaceNum);
            LOGI("Reattached kernel driver to interface %u", ifaceNum);
        }
        detachedInterfaces_.clear();

        currentAsInterface_ = 0xFF;

        libusb_close(devHandle_);
        devHandle_ = nullptr;
    }

    if (ctx_) {
        libusb_exit(ctx_);
        ctx_ = nullptr;
    }

    caps_ = UacDeviceCapabilities{};
    activeFormat_ = UacStreamFormat{};
    activeConfig_ = ActiveStreamConfig{};
    volumeRangeValid_ = false;
}

bool UsbAudioEngine::selectBestFormat(uint32_t sampleRate, uint32_t bitDepth, uint32_t channels,
                                      UacStreamFormat& outFormat) const {
    int bestScore = -1;
    const UacStreamFormat* best = nullptr;
    const uint32_t targetChannels = (channels == 1) ? 2 : channels;

    for (const auto& fmt : caps_.playbackFormats) {
        if (fmt.channels != channels && fmt.channels != targetChannels) continue;

        // If a UAC2 DAC returned empty clock rates due to firmware quirk, still allow attempting it
        const bool hasRateList = !fmt.sampleRates.empty() || fmt.isContinuousRate;
        if (hasRateList && !fmt.supportsSampleRate(sampleRate)) {
            continue;
        }

        int score = 0;

        // Channel score: prefer exact match, otherwise allow mono-to-stereo expansion
        if (fmt.channels == channels) {
            score += 2000;
        } else if (fmt.channels == 2 && channels == 1) {
            score += 1000;
        }

        // 1. Prefer exact bit depth, then higher bit depth (lossless zero-padded LSBs),
        //    only fall back to lower bit depth if DAC has no >= source bit depth.
        if (fmt.bitResolution == bitDepth) {
            score += 1000;
        } else if (fmt.bitResolution > bitDepth) {
            // Prefer smallest container >= bitDepth (e.g., 24-bit over 32-bit for 16-bit source)
            score += 700 - static_cast<int>(fmt.bitResolution - bitDepth) * 5;
        } else {
            score += 200 + static_cast<int>(fmt.bitResolution);
        }

        // 2. Prefer Asynchronous sync mode (lowest jitter)
        if (fmt.syncType == IsoSyncType::ASYNCHRONOUS) {
            score += 100;
            if (fmt.hasFeedbackEndpoint) score += 50;
        } else if (fmt.syncType == IsoSyncType::ADAPTIVE) {
            score += 40;
        }

        // 3. Slight preference for tightly packed wire format when bit depths tie
        if (fmt.subslotSize * 8 == fmt.bitResolution) {
            score += 20;
        }

        if (score > bestScore) {
            bestScore = score;
            best = &fmt;
        }
    }

    if (!best) return false;
    outFormat = *best;
    return true;
}

bool UsbAudioEngine::configureStream(uint32_t sampleRate, uint32_t bitDepth, uint32_t channels,
                                     std::string& outError) {
    if (!isOpen()) {
        outError = "USB DAC is not open";
        return false;
    }

    stopStreaming();

    std::lock_guard<std::mutex> lock(controlMutex_);

    UacStreamFormat chosen{};
    if (!selectBestFormat(sampleRate, bitDepth, channels, chosen)) {
        outError = "DAC does not support requested format (" + std::to_string(sampleRate) +
                   " Hz, " + std::to_string(bitDepth) + "-bit, " + std::to_string(channels) + " ch)";
        LOGE("%s", outError.c_str());
        return false;
    }

    // Detach kernel drivers from AC and AS interfaces when configuring for direct streaming
    for (uint8_t ifaceNum : {caps_.acInterfaceNumber, chosen.asInterfaceNumber}) {
        if (ifaceNum == 0xFF) continue;
        int active = libusb_kernel_driver_active(devHandle_, ifaceNum);
        if (active == 1) {
            int rc = libusb_detach_kernel_driver(devHandle_, ifaceNum);
            if (rc == LIBUSB_SUCCESS) {
                if (std::find(detachedInterfaces_.begin(), detachedInterfaces_.end(), ifaceNum) == detachedInterfaces_.end()) {
                    detachedInterfaces_.push_back(ifaceNum);
                }
                LOGI("Detached kernel driver from interface %u for direct streaming", ifaceNum);
            }
        }
    }

    // Claim AC interface if not already claimed
    if (caps_.acInterfaceNumber != 0xFF &&
        std::find(claimedInterfaces_.begin(), claimedInterfaces_.end(), caps_.acInterfaceNumber) == claimedInterfaces_.end()) {
        int rc = libusb_claim_interface(devHandle_, caps_.acInterfaceNumber);
        if (rc == LIBUSB_SUCCESS) {
            claimedInterfaces_.push_back(caps_.acInterfaceNumber);
        }
    }

    // Claim Audio Streaming interface if not already claimed
    if (std::find(claimedInterfaces_.begin(), claimedInterfaces_.end(),
                  chosen.asInterfaceNumber) == claimedInterfaces_.end()) {
        int rc = libusb_claim_interface(devHandle_, chosen.asInterfaceNumber);
        if (rc != LIBUSB_SUCCESS) {
            outError = std::string("Failed to claim AS interface ") +
                       std::to_string(chosen.asInterfaceNumber) + ": " + libusb_error_name(rc);
            LOGE("%s", outError.c_str());
            return false;
        }
        claimedInterfaces_.push_back(chosen.asInterfaceNumber);
    }

    currentAsInterface_ = chosen.asInterfaceNumber;

    // Reset interface to Alt 0 before switching to target Alt Setting (clears stale FIFOs)
    libusb_set_interface_alt_setting(devHandle_, chosen.asInterfaceNumber, 0);

    int rc = libusb_set_interface_alt_setting(devHandle_, chosen.asInterfaceNumber, chosen.altSetting);
    if (rc != LIBUSB_SUCCESS) {
        outError = std::string("libusb_set_interface_alt_setting(iface=") +
                   std::to_string(chosen.asInterfaceNumber) + ", alt=" +
                   std::to_string(chosen.altSetting) + ") failed: " + libusb_error_name(rc);
        LOGE("%s", outError.c_str());
        return false;
    }

    // Program DAC hardware clock to exact track sample rate
    if (!programHardwareClock(chosen, sampleRate)) {
        outError = "Failed to set DAC hardware sample rate to " + std::to_string(sampleRate) + " Hz";
        LOGE("%s", outError.c_str());
        return false;
    }

    // Update Feature Unit if this format uses a different one
    if (chosen.featureUnitId != 0 && chosen.featureUnitId != activeFeatureUnitId_) {
        activeFeatureUnitId_ = chosen.featureUnitId;
        queryHardwareVolumeRange();
        setHardwareMute(false);
    }

    // Compute exact isochronous packet timing
    const uint8_t bInt = (chosen.outInterval >= 1 && chosen.outInterval <= 16)
                             ? chosen.outInterval : 1;
    const uint32_t intervalFactor = 1u << (bInt - 1);

    if (caps_.usbSpeed >= LIBUSB_SPEED_HIGH) {
        // USB 2.0 High-Speed: 8000 microframes/sec (125 us each)
        outPacketsPerSecond_ = 8000.0 / static_cast<double>(intervalFactor);
    } else {
        // USB 1.1 Full-Speed: 1000 frames/sec (1 ms each)
        outPacketsPerSecond_ = 1000.0 / static_cast<double>(intervalFactor);
    }

    nominalFramesPerPacket_ = static_cast<double>(sampleRate) / outPacketsPerSecond_;
    currentFramesPerPacket_.store(nominalFramesPerPacket_, std::memory_order_release);
    frameAccumulator_ = 0.0;

    // Target ~1–2 ms per URB (e.g., 8 packets/URB at 8000 pkt/s = 1 ms; 2 packets/URB at 1000 pkt/s = 2 ms)
    if (outPacketsPerSecond_ >= 4000.0) {
        packetsPerUrb_ = 8;
    } else if (outPacketsPerSecond_ >= 2000.0) {
        packetsPerUrb_ = 4;
    } else {
        packetsPerUrb_ = 2;
    }

    activeFormat_ = chosen;
    activeConfig_.sampleRate = sampleRate;
    activeConfig_.sourceBitDepth = bitDepth;
    activeConfig_.wireBitDepth = chosen.bitResolution;
    activeConfig_.wireSubslotSize = chosen.subslotSize;
    activeConfig_.channels = channels;
    activeConfig_.usingFeedback = chosen.hasFeedbackEndpoint;
    activeConfig_.syncType = chosen.syncType;

    LOGI("Configured USB Stream: %u Hz | %u-bit -> %u-bit (%u-byte subslot) | %u ch | "
         "AS=%u Alt=%u EP=0x%02X MaxPkt=%u | %.1f pkt/s | %.4f frames/pkt | FB=%s",
         sampleRate, bitDepth, chosen.bitResolution, chosen.subslotSize, channels,
         chosen.asInterfaceNumber, chosen.altSetting, chosen.outEndpointAddress,
         chosen.outMaxPacketSize, outPacketsPerSecond_, nominalFramesPerPacket_,
         chosen.hasFeedbackEndpoint ? "YES" : "NO");

    return true;
}

bool UsbAudioEngine::programHardwareClock(const UacStreamFormat& fmt, uint32_t sampleRate) {
    if (caps_.uacVersion == UacVersion::UAC_2_0) {
        // UAC 2.0: CUR control transfer (bmRequestType=0x21, bRequest=0x01, CS_SAM_FREQ_CONTROL)
        // directed to Clock Source Entity ID on the Audio Control interface.
        if (fmt.clockSourceId == 0) {
            LOGE("UAC 2.0 Clock Source Entity ID is 0");
            return false;
        }
        uint8_t data[4] = {
            static_cast<uint8_t>(sampleRate & 0xFF),
            static_cast<uint8_t>((sampleRate >> 8) & 0xFF),
            static_cast<uint8_t>((sampleRate >> 16) & 0xFF),
            static_cast<uint8_t>((sampleRate >> 24) & 0xFF)
        };
        const uint8_t bmRequestType =
            LIBUSB_ENDPOINT_OUT | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE; // 0x21
        const uint16_t wValue = static_cast<uint16_t>(kCsSamFreqControl << 8);
        const uint16_t wIndex =
            (static_cast<uint16_t>(fmt.clockSourceId) << 8) | caps_.acInterfaceNumber;

        int rc = libusb_control_transfer(devHandle_, bmRequestType, kUac2Cur,
                                         wValue, wIndex, data, sizeof(data), 1000);
        if (rc != 4) {
            LOGE("UAC2 SET ClockSource(%u) to %u Hz failed: %s (%d)",
                 fmt.clockSourceId, sampleRate, rc < 0 ? libusb_error_name(rc) : "short write", rc);
            return false;
        }
        LOGI("UAC2 Clock Source %u programmed to %u Hz", fmt.clockSourceId, sampleRate);
        return true;
    } else {
        // UAC 1.0: SET_CUR control transfer (bmRequestType=0x22, bRequest=0x01, SAMPLING_FREQ_CONTROL)
        // directed to the Isochronous OUT endpoint.
        uint8_t data[3] = {
            static_cast<uint8_t>(sampleRate & 0xFF),
            static_cast<uint8_t>((sampleRate >> 8) & 0xFF),
            static_cast<uint8_t>((sampleRate >> 16) & 0xFF)
        };
        const uint8_t bmRequestType =
            LIBUSB_ENDPOINT_OUT | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_ENDPOINT; // 0x22
        const uint16_t wValue = static_cast<uint16_t>(kCsSamFreqControl << 8);
        const uint16_t wIndex = fmt.outEndpointAddress;

        int rc = libusb_control_transfer(devHandle_, bmRequestType, kUac1SetCur,
                                         wValue, wIndex, data, sizeof(data), 1000);
        if (rc != 3) {
            if (!fmt.hasSamplingFreqControl && fmt.sampleRates.size() == 1) {
                // Single-rate UAC1 DACs often STALL SET_CUR because the rate is fixed in hardware
                LOGW("UAC1 single-rate DAC ignored SET_CUR (%s); continuing",
                     rc < 0 ? libusb_error_name(rc) : "short write");
                return true;
            }
            LOGE("UAC1 SET_CUR EP(0x%02X) to %u Hz failed: %s (%d)",
                 fmt.outEndpointAddress, sampleRate,
                 rc < 0 ? libusb_error_name(rc) : "short write", rc);
            return false;
        }
        LOGI("UAC1 Endpoint 0x%02X programmed to %u Hz", fmt.outEndpointAddress, sampleRate);
        return true;
    }
}

void UsbAudioEngine::queryHardwareVolumeRange() {
    volumeRangeValid_ = false;
    if (!devHandle_ || activeFeatureUnitId_ == 0) return;

    const UacFeatureUnit* fu = nullptr;
    for (const auto& u : caps_.featureUnits) {
        if (u.unitId == activeFeatureUnitId_) {
            fu = &u;
            break;
        }
    }
    if (!fu) return;

    if (fu->supportsMasterVolume) {
        volumeUseMaster_ = true;
    } else if (fu->supportsChannelVolume) {
        volumeUseMaster_ = false;
        volumeChannelCount_ = fu->numControlChannels > 0 ? fu->numControlChannels : 2;
    } else {
        return;
    }

    const uint8_t targetChannel = volumeUseMaster_ ? 0 : 1;
    const uint8_t bmReqIn =
        LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE; // 0xA1
    const uint16_t wValue = static_cast<uint16_t>((kFuVolumeControl << 8) | targetChannel);
    const uint16_t wIndex =
        (static_cast<uint16_t>(activeFeatureUnitId_) << 8) | caps_.acInterfaceNumber;

    if (caps_.uacVersion == UacVersion::UAC_2_0) {
        uint8_t buf[64] = {0};
        int rc = libusb_control_transfer(devHandle_, bmReqIn, kUac2Range,
                                         wValue, wIndex, buf, sizeof(buf), 500);
        if (rc >= 8) {
            // wNumSubRanges (2B) + wMIN (2B) + wMAX (2B) + wRES (2B)
            volMin256Db_ = readLe16Signed(buf + 2);
            volMax256Db_ = readLe16Signed(buf + 4);
            volRes256Db_ = std::max<int16_t>(1, readLe16Signed(buf + 6));
            volumeRangeValid_ = (volMax256Db_ > volMin256Db_);
        }
    } else {
        uint8_t bMin[2] = {0}, bMax[2] = {0}, bRes[2] = {0};
        int r1 = libusb_control_transfer(devHandle_, bmReqIn, kUac1GetMin, wValue, wIndex, bMin, 2, 500);
        int r2 = libusb_control_transfer(devHandle_, bmReqIn, kUac1GetMax, wValue, wIndex, bMax, 2, 500);
        int r3 = libusb_control_transfer(devHandle_, bmReqIn, kUac1GetRes, wValue, wIndex, bRes, 2, 500);
        if (r1 == 2 && r2 == 2) {
            volMin256Db_ = readLe16Signed(bMin);
            volMax256Db_ = readLe16Signed(bMax);
            volRes256Db_ = (r3 == 2) ? std::max<int16_t>(1, readLe16Signed(bRes)) : 256;
            volumeRangeValid_ = (volMax256Db_ > volMin256Db_);
        }
    }

    if (volumeRangeValid_) {
        LOGI("Hardware Volume FU=%u (%s): min=%.2f dB, max=%.2f dB, res=%.2f dB",
             activeFeatureUnitId_, volumeUseMaster_ ? "Master" : "Per-Channel",
             volMin256Db_ / 256.0, volMax256Db_ / 256.0, volRes256Db_ / 256.0);
    }
}

bool UsbAudioEngine::setHardwareVolumePercent(float percent) {
    std::lock_guard<std::mutex> lock(controlMutex_);
    if (!devHandle_ || !volumeRangeValid_ || activeFeatureUnitId_ == 0) {
        return false;
    }

    const float clamped = std::clamp(percent, 0.0f, 1.0f);
    if (clamped <= 0.001f) {
        return setHardwareMute(true);
    }

    // Map linear slider [0.0..1.0] to DAC dB range quantized to volRes256Db_
    const double raw256 = static_cast<double>(volMin256Db_) +
                          static_cast<double>(clamped) * (volMax256Db_ - volMin256Db_);
    const int32_t steps = static_cast<int32_t>(std::llround((raw256 - volMin256Db_) / volRes256Db_));
    const int16_t target256 = static_cast<int16_t>(
        std::clamp<int32_t>(volMin256Db_ + steps * volRes256Db_, volMin256Db_, volMax256Db_));

    uint8_t data[2] = {
        static_cast<uint8_t>(target256 & 0xFF),
        static_cast<uint8_t>((target256 >> 8) & 0xFF)
    };

    const uint8_t bmReqOut =
        LIBUSB_ENDPOINT_OUT | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE; // 0x21
    const uint8_t bRequest = (caps_.uacVersion == UacVersion::UAC_2_0) ? kUac2Cur : kUac1SetCur;
    const uint16_t wIndex =
        (static_cast<uint16_t>(activeFeatureUnitId_) << 8) | caps_.acInterfaceNumber;

    bool ok = true;
    if (volumeUseMaster_) {
        const uint16_t wValue = static_cast<uint16_t>((kFuVolumeControl << 8) | 0);
        ok = (libusb_control_transfer(devHandle_, bmReqOut, bRequest, wValue, wIndex, data, 2, 300) == 2);
    } else {
        for (uint8_t ch = 1; ch <= volumeChannelCount_; ++ch) {
            const uint16_t wValue = static_cast<uint16_t>((kFuVolumeControl << 8) | ch);
            int rc = libusb_control_transfer(devHandle_, bmReqOut, bRequest, wValue, wIndex, data, 2, 300);
            if (rc != 2) ok = false;
        }
    }

    if (ok) {
        // Ensure unmuted when raising volume above 0
        uint8_t unmute = 0;
        const uint16_t wValMute = static_cast<uint16_t>((kFuMuteControl << 8) | 0);
        libusb_control_transfer(devHandle_, bmReqOut, bRequest, wValMute, wIndex, &unmute, 1, 200);
    }
    return ok;
}

bool UsbAudioEngine::setHardwareMute(bool mute) {
    if (!devHandle_ || activeFeatureUnitId_ == 0) return false;
    uint8_t val = mute ? 1 : 0;
    const uint8_t bmReqOut =
        LIBUSB_ENDPOINT_OUT | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE;
    const uint8_t bRequest = (caps_.uacVersion == UacVersion::UAC_2_0) ? kUac2Cur : kUac1SetCur;
    const uint16_t wIndex =
        (static_cast<uint16_t>(activeFeatureUnitId_) << 8) | caps_.acInterfaceNumber;
    const uint16_t wValue = static_cast<uint16_t>((kFuMuteControl << 8) | 0);

    return libusb_control_transfer(devHandle_, bmReqOut, bRequest, wValue, wIndex, &val, 1, 300) == 1;
}

bool UsbAudioEngine::startStreaming(LockFreeRingBuffer<int32_t>* ringBuffer, std::string& outError) {
    if (!isOpen() || activeFormat_.outEndpointAddress == 0) {
        outError = "Stream not configured before startStreaming()";
        return false;
    }
    if (streaming_.load(std::memory_order_acquire)) {
        return true;
    }

    ringBuffer_ = ringBuffer;
    frameAccumulator_ = 0.0;
    currentFramesPerPacket_.store(nominalFramesPerPacket_, std::memory_order_release);
    activeOutUrbs_.store(0, std::memory_order_relaxed);
    activeFeedbackUrbs_.store(0, std::memory_order_relaxed);
    eventLoopCompleted_ = 0;

    // 1. Allocate Isochronous OUT URBs (all buffers pre-allocated here, zero allocs on RT thread)
    outUrbs_.clear();
    outUrbs_.reserve(kNumOutUrbs);
    const size_t maxBytesPerUrb =
        static_cast<size_t>(activeFormat_.outMaxPacketSize) * packetsPerUrb_;
    const uint32_t bytesPerFrame = activeFormat_.channels * activeFormat_.subslotSize;
    const size_t maxFramesPerPacket =
        (bytesPerFrame > 0) ? (activeFormat_.outMaxPacketSize / bytesPerFrame) : 0;
    const size_t maxSamplesPerUrb = maxFramesPerPacket * activeFormat_.channels * packetsPerUrb_;

    for (int i = 0; i < kNumOutUrbs; ++i) {
        auto urb = std::make_unique<OutUrbContext>();
        urb->engine = this;
        urb->packetBuffer.resize(maxBytesPerUrb, 0);
        urb->pcmScratch.resize(maxSamplesPerUrb, 0);
        urb->transfer = libusb_alloc_transfer(static_cast<int>(packetsPerUrb_));
        if (!urb->transfer) {
            outError = "libusb_alloc_transfer failed for OUT URB";
            stopStreaming();
            return false;
        }
        libusb_fill_iso_transfer(
            urb->transfer,
            devHandle_,
            activeFormat_.outEndpointAddress,
            urb->packetBuffer.data(),
            static_cast<int>(maxBytesPerUrb),
            static_cast<int>(packetsPerUrb_),
            &UsbAudioEngine::onOutTransferComplete,
            urb.get(),
            2000 // 2s timeout
        );
        outUrbs_.push_back(std::move(urb));
    }

    // 2. Allocate Asynchronous Feedback IN URBs (if DAC exposes feedback endpoint)
    feedbackUrbs_.clear();
    if (activeFormat_.hasFeedbackEndpoint) {
        feedbackUrbs_.reserve(kNumFeedbackUrbs);
        const uint16_t fbPktSize = std::max<uint16_t>(4, activeFormat_.feedbackMaxPacketSize);
        for (int i = 0; i < kNumFeedbackUrbs; ++i) {
            auto fb = std::make_unique<FeedbackUrbContext>();
            fb->engine = this;
            fb->packetBuffer.resize(fbPktSize, 0);
            fb->transfer = libusb_alloc_transfer(1);
            if (!fb->transfer) {
                LOGW("Failed to allocate Feedback URB %d", i);
                continue;
            }
            libusb_fill_iso_transfer(
                fb->transfer,
                devHandle_,
                activeFormat_.feedbackEndpointAddress,
                fb->packetBuffer.data(),
                fbPktSize,
                1,
                &UsbAudioEngine::onFeedbackTransferComplete,
                fb.get(),
                2000
            );
            libusb_set_iso_packet_lengths(fb->transfer, fbPktSize);
            feedbackUrbs_.push_back(std::move(fb));
        }
    }

    streaming_.store(true, std::memory_order_release);

    // 3. Submit initial batch of Feedback IN URBs
    for (auto& fb : feedbackUrbs_) {
        submitFeedbackUrb(fb.get());
    }

    // 4. Submit initial batch of Isochronous OUT URBs
    int submitted = 0;
    for (auto& urb : outUrbs_) {
        if (fillAndSubmitOutUrb(urb.get())) {
            submitted++;
        }
    }

    if (submitted == 0) {
        outError = "Failed to submit any isochronous OUT transfers to USB DAC";
        streaming_.store(false, std::memory_order_release);
        stopStreaming();
        return false;
    }

    // 5. Launch dedicated high-priority USB event loop thread
    eventThread_ = std::thread(&UsbAudioEngine::usbEventLoop, this);
    LOGI("Started USB iso streaming (%d OUT URBs, %zu FB URBs)", submitted, feedbackUrbs_.size());
    return true;
}

void UsbAudioEngine::stopStreaming() {
    const bool wasStreaming = streaming_.exchange(false, std::memory_order_acq_rel);

    // Cancel all in-flight transfers so libusb invokes their callbacks with CANCELLED
    for (auto& urb : outUrbs_) {
        if (urb && urb->transfer && urb->inFlight) {
            libusb_cancel_transfer(urb->transfer);
        }
    }
    for (auto& fb : feedbackUrbs_) {
        if (fb && fb->transfer && fb->inFlight) {
            libusb_cancel_transfer(fb->transfer);
        }
    }

    // Wait cleanly for all in-flight transfers to drain (bounded to survive abrupt unplug)
    if (eventThread_.joinable()) {
        const auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(800);
        while ((activeOutUrbs_.load(std::memory_order_acquire) > 0 ||
                activeFeedbackUrbs_.load(std::memory_order_acquire) > 0) &&
               std::chrono::steady_clock::now() < deadline) {
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }

        eventLoopCompleted_ = 1;
        eventThread_.join();
    }

    // Free all libusb_transfer structs now that event thread is joined
    for (auto& urb : outUrbs_) {
        if (urb && urb->transfer) {
            libusb_free_transfer(urb->transfer);
            urb->transfer = nullptr;
        }
    }
    outUrbs_.clear();

    for (auto& fb : feedbackUrbs_) {
        if (fb && fb->transfer) {
            libusb_free_transfer(fb->transfer);
            fb->transfer = nullptr;
        }
    }
    feedbackUrbs_.clear();

    // Return AS interface to Alt 0 (zero bandwidth) if device is still attached
    if (wasStreaming && devHandle_ && currentAsInterface_ != 0xFF) {
        libusb_set_interface_alt_setting(devHandle_, currentAsInterface_, 0);
    }

    // Release claimed interfaces and reattach kernel drivers
    if (devHandle_) {
        for (uint8_t ifaceNum : claimedInterfaces_) {
            libusb_release_interface(devHandle_, ifaceNum);
        }
        claimedInterfaces_.clear();

        for (uint8_t ifaceNum : detachedInterfaces_) {
            libusb_attach_kernel_driver(devHandle_, ifaceNum);
            LOGI("Reattached kernel driver to interface %u", ifaceNum);
        }
        detachedInterfaces_.clear();
    }
}

void UsbAudioEngine::usbEventLoop() {
    // Boost real-time priority for the USB transfer pump (ANDROID_PRIORITY_URGENT_AUDIO = -19)
    setpriority(PRIO_PROCESS, 0, -19);

    timeval tv{};
    tv.tv_sec = 0;
    tv.tv_usec = 50000; // 50 ms poll timeout for responsive shutdown

    while (!eventLoopCompleted_) {
        if (!streaming_.load(std::memory_order_acquire) &&
            activeOutUrbs_.load(std::memory_order_acquire) == 0 &&
            activeFeedbackUrbs_.load(std::memory_order_acquire) == 0) {
            break;
        }

        int rc = libusb_handle_events_timeout_completed(ctx_, &tv, &eventLoopCompleted_);
        if (rc != LIBUSB_SUCCESS && rc != LIBUSB_ERROR_INTERRUPTED) {
            LOGE("libusb_handle_events_timeout_completed error: %s", libusb_error_name(rc));
            if (rc == LIBUSB_ERROR_NO_DEVICE) {
                streaming_.store(false, std::memory_order_release);
                if (errorCallback_) {
                    errorCallback_("USB DAC disconnected during playback");
                }
                break;
            }
        }
    }
}

bool UsbAudioEngine::fillAndSubmitOutUrb(OutUrbContext* ctx) {
    if (!ctx || !ctx->transfer || !streaming_.load(std::memory_order_acquire)) {
        return false;
    }

    const uint32_t channels = activeFormat_.channels;
    const uint32_t subslot = activeFormat_.subslotSize;
    const uint32_t bytesPerFrame = channels * subslot;
    const uint32_t maxFramesPerPacket = activeFormat_.outMaxPacketSize / bytesPerFrame;

    // 1. Determine exact frame count per iso packet using feedback-adjusted rate
    const double framesPerPkt = currentFramesPerPacket_.load(std::memory_order_relaxed);
    uint32_t totalFramesInUrb = 0;
    uint32_t packetFrameCounts[32]; // packetsPerUrb_ <= 8

    for (uint32_t p = 0; p < packetsPerUrb_; ++p) {
        frameAccumulator_ += framesPerPkt;
        uint32_t f = static_cast<uint32_t>(frameAccumulator_);
        frameAccumulator_ -= static_cast<double>(f);

        if (f > maxFramesPerPacket) f = maxFramesPerPacket;
        packetFrameCounts[p] = f;
        totalFramesInUrb += f;
    }

    // 2. Pull PCM samples from lock-free ring buffer (zero locks, zero allocs)
    const size_t neededSamples = static_cast<size_t>(totalFramesInUrb) * channels;
    const bool isPaused = paused_.load(std::memory_order_acquire);
    size_t readSamples = 0;

    if (!isPaused && ringBuffer_ && neededSamples > 0) {
        readSamples = ringBuffer_->read(ctx->pcmScratch.data(), neededSamples);
    }

    if (readSamples < neededSamples) {
        // Output clean digital silence on pause or underrun
        std::memset(ctx->pcmScratch.data() + readSamples, 0,
                    (neededSamples - readSamples) * sizeof(int32_t));
        if (!isPaused && readSamples == 0 && underrunCallback_) {
            underrunCallback_();
        }
    }

    if (readSamples > 0) {
        playedFrames_.fetch_add(readSamples / channels, std::memory_order_release);
    }

    // 3. Pack canonical MSB-aligned int32_t PCM into exact DAC wire format
    uint32_t sampleOffset = 0;
    uint32_t byteOffset = 0;

    for (uint32_t p = 0; p < packetsPerUrb_; ++p) {
        const uint32_t f = packetFrameCounts[p];
        const uint32_t pktBytes = f * bytesPerFrame;

        packPcmToWire(ctx->pcmScratch.data() + sampleOffset, f,
                      ctx->packetBuffer.data() + byteOffset);

        ctx->transfer->iso_packet_desc[p].length = pktBytes;
        sampleOffset += f * channels;
        byteOffset += pktBytes;
    }

    ctx->transfer->length = static_cast<int>(byteOffset);
    ctx->inFlight = true;
    activeOutUrbs_.fetch_add(1, std::memory_order_acq_rel);

    const int rc = libusb_submit_transfer(ctx->transfer);
    if (rc != LIBUSB_SUCCESS) {
        ctx->inFlight = false;
        activeOutUrbs_.fetch_sub(1, std::memory_order_acq_rel);
        if (rc == LIBUSB_ERROR_NO_DEVICE) {
            streaming_.store(false, std::memory_order_release);
            if (errorCallback_) {
                errorCallback_("USB DAC unplugged (NO_DEVICE on OUT submit)");
            }
        } else {
            LOGE("libusb_submit_transfer(OUT) failed: %s", libusb_error_name(rc));
        }
        return false;
    }
    return true;
}

bool UsbAudioEngine::submitFeedbackUrb(FeedbackUrbContext* ctx) {
    if (!ctx || !ctx->transfer || !streaming_.load(std::memory_order_acquire)) {
        return false;
    }

    ctx->inFlight = true;
    activeFeedbackUrbs_.fetch_add(1, std::memory_order_acq_rel);

    const int rc = libusb_submit_transfer(ctx->transfer);
    if (rc != LIBUSB_SUCCESS) {
        ctx->inFlight = false;
        activeFeedbackUrbs_.fetch_sub(1, std::memory_order_acq_rel);
        if (rc != LIBUSB_ERROR_NO_DEVICE) {
            LOGW("libusb_submit_transfer(FB) failed: %s", libusb_error_name(rc));
        }
        return false;
    }
    return true;
}

void LIBUSB_CALL UsbAudioEngine::onOutTransferComplete(libusb_transfer* transfer) {
    auto* ctx = static_cast<OutUrbContext*>(transfer->user_data);
    UsbAudioEngine* engine = ctx->engine;

    ctx->inFlight = false;
    engine->activeOutUrbs_.fetch_sub(1, std::memory_order_acq_rel);

    if (transfer->status != LIBUSB_TRANSFER_COMPLETED) {
        if (transfer->status == LIBUSB_TRANSFER_NO_DEVICE ||
            transfer->status == LIBUSB_TRANSFER_ERROR) {
            engine->streaming_.store(false, std::memory_order_release);
            if (engine->errorCallback_) {
                engine->errorCallback_("USB DAC disconnected");
            }
        }
        return;
    }

    if (!engine->streaming_.load(std::memory_order_acquire)) {
        return;
    }

    // Resubmit immediately with the next batch of PCM packets
    engine->fillAndSubmitOutUrb(ctx);
}

void LIBUSB_CALL UsbAudioEngine::onFeedbackTransferComplete(libusb_transfer* transfer) {
    auto* ctx = static_cast<FeedbackUrbContext*>(transfer->user_data);
    UsbAudioEngine* engine = ctx->engine;

    ctx->inFlight = false;
    engine->activeFeedbackUrbs_.fetch_sub(1, std::memory_order_acq_rel);

    if (transfer->status == LIBUSB_TRANSFER_CANCELLED ||
        transfer->status == LIBUSB_TRANSFER_NO_DEVICE ||
        !engine->streaming_.load(std::memory_order_acquire)) {
        return;
    }

    if (transfer->status == LIBUSB_TRANSFER_COMPLETED && transfer->num_iso_packets > 0) {
        const auto& pkt = transfer->iso_packet_desc[0];
        if (pkt.status == LIBUSB_TRANSFER_COMPLETED && pkt.actual_length >= 3) {
            const uint8_t* d = transfer->buffer;
            double feedbackFramesPerOutPacket = 0.0;

            if (pkt.actual_length == 3) {
                // USB 1.1 Full-Speed 10.14 fixed-point (frames per 1 ms frame)
                const uint32_t raw24 = static_cast<uint32_t>(d[0]) |
                                       (static_cast<uint32_t>(d[1]) << 8) |
                                       (static_cast<uint32_t>(d[2]) << 16);
                const double framesPerMs = static_cast<double>(raw24) / 16384.0;
                feedbackFramesPerOutPacket = framesPerMs * (1000.0 / engine->outPacketsPerSecond_);
            } else if (pkt.actual_length >= 4) {
                // USB 2.0 High-Speed 16.16 fixed-point (frames per 125 us microframe)
                const uint32_t raw32 = static_cast<uint32_t>(d[0]) |
                                       (static_cast<uint32_t>(d[1]) << 8) |
                                       (static_cast<uint32_t>(d[2]) << 16) |
                                       (static_cast<uint32_t>(d[3]) << 24);
                const double framesPerMicroframe = static_cast<double>(raw32) / 65536.0;
                feedbackFramesPerOutPacket =
                    framesPerMicroframe * (8000.0 / engine->outPacketsPerSecond_);
            }

            // Sanity-clamp within +/-5% of nominal rate to reject malformed firmware bursts
            const double nom = engine->nominalFramesPerPacket_;
            if (feedbackFramesPerOutPacket >= nom * 0.95 &&
                feedbackFramesPerOutPacket <= nom * 1.05) {
                engine->currentFramesPerPacket_.store(
                    feedbackFramesPerOutPacket, std::memory_order_relaxed);
            }
        }
    }

    engine->submitFeedbackUrb(ctx);
}

void UsbAudioEngine::packPcmToWire(const int32_t* srcInterleaved, uint32_t frameCount,
                                   uint8_t* dstWire) const noexcept {
    const size_t totalSamples = static_cast<size_t>(frameCount) * activeFormat_.channels;
    const uint8_t subslot = activeFormat_.subslotSize;

    if (subslot == 2) {
        // S16_LE: take top 16 bits of MSB-aligned int32_t
        uint8_t* out = dstWire;
        for (size_t i = 0; i < totalSamples; ++i) {
            const uint32_t s = static_cast<uint32_t>(srcInterleaved[i]);
            out[0] = static_cast<uint8_t>((s >> 16) & 0xFF);
            out[1] = static_cast<uint8_t>((s >> 24) & 0xFF);
            out += 2;
        }
    } else if (subslot == 3) {
        // S24_3LE: tightly packed 24-bit little-endian (top 24 bits of MSB-aligned int32_t)
        uint8_t* out = dstWire;
        for (size_t i = 0; i < totalSamples; ++i) {
            const uint32_t s = static_cast<uint32_t>(srcInterleaved[i]);
            out[0] = static_cast<uint8_t>((s >> 8) & 0xFF);
            out[1] = static_cast<uint8_t>((s >> 16) & 0xFF);
            out[2] = static_cast<uint8_t>((s >> 24) & 0xFF);
            out += 3;
        }
    } else if (subslot == 4) {
        // S32_LE: 32-bit little-endian (MSB-aligned per USB Audio spec, or optional LSB-aligned 24-in-32)
        const bool rightAlign =
            rightAlign24In32_.load(std::memory_order_relaxed) && activeFormat_.bitResolution == 24;
        uint8_t* out = dstWire;
        if (!rightAlign) {
            for (size_t i = 0; i < totalSamples; ++i) {
                const uint32_t s = static_cast<uint32_t>(srcInterleaved[i]);
                out[0] = static_cast<uint8_t>(s & 0xFF);
                out[1] = static_cast<uint8_t>((s >> 8) & 0xFF);
                out[2] = static_cast<uint8_t>((s >> 16) & 0xFF);
                out[3] = static_cast<uint8_t>((s >> 24) & 0xFF);
                out += 4;
            }
        } else {
            for (size_t i = 0; i < totalSamples; ++i) {
                const uint32_t s = static_cast<uint32_t>(srcInterleaved[i] >> 8);
                out[0] = static_cast<uint8_t>(s & 0xFF);
                out[1] = static_cast<uint8_t>((s >> 8) & 0xFF);
                out[2] = static_cast<uint8_t>((s >> 16) & 0xFF);
                out[3] = static_cast<uint8_t>((s >> 24) & 0xFF);
                out += 4;
            }
        }
    }
}

} // namespace bitperfect

#pragma once

#include <libusb.h>
#include <cstdint>
#include <string>
#include <vector>

namespace bitperfect {

enum class UacVersion : uint16_t {
    UNKNOWN = 0x0000,
    UAC_1_0 = 0x0100,
    UAC_2_0 = 0x0200
};

enum class IsoSyncType : uint8_t {
    NONE = 0,
    ASYNCHRONOUS = 1,
    ADAPTIVE = 2,
    SYNCHRONOUS = 3
};

struct UacClockSource {
    uint8_t clockId = 0;
    uint8_t attributes = 0;       // bmAttributes (bits 1:0 = clock type)
    uint8_t controls = 0;         // bmControls (bits 1:0 = freq control, bits 3:2 = validity)
    uint8_t assocTerminal = 0;
    std::vector<uint32_t> discreteRates;
    bool hasContinuousRange = false;
    uint32_t minRate = 0;
    uint32_t maxRate = 0;
};

struct UacClockSelector {
    uint8_t selectorId = 0;
    std::vector<uint8_t> sourceIds;
};

struct UacInputTerminal {
    uint8_t terminalId = 0;
    uint16_t terminalType = 0;    // 0x0101 = USB Streaming
    uint8_t clockSourceId = 0;    // UAC2 only (may point to Clock Source or Clock Selector)
    uint8_t nrChannels = 0;
};

struct UacOutputTerminal {
    uint8_t terminalId = 0;
    uint16_t terminalType = 0;
    uint8_t sourceId = 0;
    uint8_t clockSourceId = 0;    // UAC2 only
};

struct UacFeatureUnit {
    uint8_t unitId = 0;
    uint8_t sourceId = 0;
    bool supportsMasterMute = false;
    bool supportsMasterVolume = false;
    bool supportsChannelMute = false;
    bool supportsChannelVolume = false;
    uint8_t numControlChannels = 0;
};

struct UacStreamFormat {
    uint8_t asInterfaceNumber = 0;
    uint8_t altSetting = 0;
    uint8_t terminalLink = 0;
    uint8_t channels = 0;
    uint8_t subslotSize = 0;          // Bytes per sample on wire: 2, 3, or 4
    uint8_t bitResolution = 0;        // Effective bits: 16, 24, or 32

    // Sample rates (from Format Type I in UAC1, or Clock Source RANGE in UAC2)
    std::vector<uint32_t> sampleRates;
    bool isContinuousRate = false;
    uint32_t minSampleRate = 0;
    uint32_t maxSampleRate = 0;

    // Isochronous OUT Data Endpoint
    uint8_t outEndpointAddress = 0;
    uint16_t outMaxPacketSize = 0;    // Decoded max bytes per microframe/frame
    uint8_t outInterval = 1;          // bInterval from descriptor
    IsoSyncType syncType = IsoSyncType::NONE;
    bool hasSamplingFreqControl = false; // UAC1 CS_ENDPOINT bmAttributes bit 0

    // Asynchronous Feedback IN Endpoint (optional)
    bool hasFeedbackEndpoint = false;
    uint8_t feedbackEndpointAddress = 0;
    uint16_t feedbackMaxPacketSize = 0;
    uint8_t feedbackInterval = 1;

    // Linked Control Entities
    uint8_t clockSourceId = 0;        // Resolved UAC2 Clock Source Entity ID
    uint8_t featureUnitId = 0;        // Resolved Feature Unit ID for volume/mute

    bool supportsSampleRate(uint32_t rate) const;
};

struct UacDeviceCapabilities {
    uint16_t vendorId = 0;
    uint16_t productId = 0;
    std::string manufacturer;
    std::string productName;
    int usbSpeed = LIBUSB_SPEED_UNKNOWN;

    UacVersion uacVersion = UacVersion::UNKNOWN;
    uint8_t acInterfaceNumber = 0xFF;

    std::vector<UacInputTerminal> inputTerminals;
    std::vector<UacOutputTerminal> outputTerminals;
    std::vector<UacClockSource> clockSources;
    std::vector<UacClockSelector> clockSelectors;
    std::vector<UacFeatureUnit> featureUnits;
    std::vector<UacStreamFormat> playbackFormats;

    // Non-audio interfaces that may hold conflicting kernel drivers (e.g., HID volume buttons)
    std::vector<uint8_t> allInterfaceNumbers;
};

class UacDescriptorParser {
public:
    /**
     * Parses active USB configuration descriptors and (for UAC 2.0) queries Clock Source
     * sample-rate ranges via control transfers.
     *
     * Note: For UAC 2.0 Clock Source RANGE queries, the Audio Control interface does not
     * strictly need to be claimed on all kernels, but claiming AC first or calling after
     * detaching kernel drivers is safest. This parser works both before and after detach.
     */
    static bool parse(libusb_device_handle* handle, UacDeviceCapabilities& outCaps);

    /**
     * Queries UAC 2.0 Clock Source supported sample rates using GET RANGE (0x02).
     * Can be re-invoked after claiming the AC interface if an initial query failed due
     * to a busy kernel driver.
     */
    static bool queryUac2ClockRates(libusb_device_handle* handle,
                                    uint8_t acInterfaceNumber,
                                    UacClockSource& clockSource);

    /**
     * Dumps complete parsed capabilities + raw class-specific descriptors to Android Logcat.
     */
    static void dumpCapabilitiesToLogcat(libusb_device_handle* handle,
                                         const UacDeviceCapabilities& caps);

private:
    static void parseAcExtraDescriptors(const uint8_t* extra, int extraLen,
                                        UacDeviceCapabilities& caps);

    static void resolveEntityLinks(libusb_device_handle* handle,
                                   UacDeviceCapabilities& caps);

    static uint16_t decodeMaxPacketSize(uint16_t rawMaxPacketSize, int usbSpeed);
    static const char* syncTypeToString(IsoSyncType sync);
    static const char* speedToString(int usbSpeed);
};

} // namespace bitperfect

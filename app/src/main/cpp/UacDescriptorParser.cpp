#include "UacDescriptorParser.h"

#include <android/log.h>
#include <algorithm>
#include <cstring>
#include <set>

#define LOG_TAG "UacParser"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace bitperfect {

namespace {
// USB Audio Class Constants
constexpr uint8_t kUsbClassAudio = 0x01;
constexpr uint8_t kSubclassAudioControl = 0x01;
constexpr uint8_t kSubclassAudioStreaming = 0x02;

constexpr uint8_t kDescCsInterface = 0x24;
constexpr uint8_t kDescCsEndpoint = 0x25;

// AC Interface Descriptor Subtypes
constexpr uint8_t kAcHeader = 0x01;
constexpr uint8_t kAcInputTerminal = 0x02;
constexpr uint8_t kAcOutputTerminal = 0x03;
constexpr uint8_t kAcFeatureUnit = 0x06;
constexpr uint8_t kAcClockSource = 0x0A;     // UAC 2.0
constexpr uint8_t kAcClockSelector = 0x0B;   // UAC 2.0

// AS Interface Descriptor Subtypes
constexpr uint8_t kAsGeneral = 0x01;
constexpr uint8_t kAsFormatType = 0x02;

// Format Type I / PCM
constexpr uint8_t kFormatTypeI = 0x01;
constexpr uint16_t kUac1FormatPcm = 0x0001;
constexpr uint32_t kUac2FormatPcmBit = 0x00000001;

// UAC 2.0 Clock Source Control Request
constexpr uint8_t kUac2ReqRange = 0x02;
constexpr uint8_t kUac2CsSamFreqControl = 0x01;

inline uint16_t readLe16(const uint8_t* p) {
    return static_cast<uint16_t>(p[0]) | (static_cast<uint16_t>(p[1]) << 8);
}

inline uint32_t readLe24(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) |
           (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16);
}

inline uint32_t readLe32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) |
           (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) |
           (static_cast<uint32_t>(p[3]) << 24);
}

std::string readUsbStringDescriptor(libusb_device_handle* handle, uint8_t index) {
    if (index == 0 || !handle) return "";
    unsigned char buf[256] = {0};
    int ret = libusb_get_string_descriptor_ascii(handle, index, buf, sizeof(buf) - 1);
    if (ret > 0) {
        buf[ret] = '\0';
        return std::string(reinterpret_cast<char*>(buf));
    }
    return "";
}
} // namespace

bool UacStreamFormat::supportsSampleRate(uint32_t rate) const {
    if (isContinuousRate && rate >= minSampleRate && rate <= maxSampleRate) {
        return true;
    }
    return std::find(sampleRates.begin(), sampleRates.end(), rate) != sampleRates.end();
}

uint16_t UacDescriptorParser::decodeMaxPacketSize(uint16_t rawMaxPacketSize, int usbSpeed) {
    // For High-Speed (USB 2.0) isochronous endpoints, bits 12:11 encode additional
    // transactions per microframe (0, 1, or 2 -> multiplier 1, 2, or 3).
    const uint16_t baseSize = rawMaxPacketSize & 0x07FF;
    if (usbSpeed >= LIBUSB_SPEED_HIGH) {
        const uint16_t mult = 1 + ((rawMaxPacketSize >> 11) & 0x03);
        return static_cast<uint16_t>(baseSize * mult);
    }
    return baseSize;
}

bool UacDescriptorParser::parse(libusb_device_handle* handle, UacDeviceCapabilities& outCaps) {
    outCaps = UacDeviceCapabilities{};
    if (!handle) {
        LOGE("parse() called with null libusb_device_handle");
        return false;
    }

    libusb_device* dev = libusb_get_device(handle);
    if (!dev) {
        LOGE("libusb_get_device() returned null");
        return false;
    }

    outCaps.usbSpeed = libusb_get_device_speed(dev);

    libusb_device_descriptor devDesc{};
    if (libusb_get_device_descriptor(dev, &devDesc) != LIBUSB_SUCCESS) {
        LOGE("Failed to get device descriptor");
        return false;
    }

    outCaps.vendorId = devDesc.idVendor;
    outCaps.productId = devDesc.idProduct;
    outCaps.manufacturer = readUsbStringDescriptor(handle, devDesc.iManufacturer);
    outCaps.productName = readUsbStringDescriptor(handle, devDesc.iProduct);

    libusb_config_descriptor* config = nullptr;
    int rc = libusb_get_active_config_descriptor(dev, &config);
    if (rc != LIBUSB_SUCCESS || !config) {
        rc = libusb_get_config_descriptor(dev, 0, &config);
        if (rc != LIBUSB_SUCCESS || !config) {
            LOGE("Failed to get USB config descriptor: %s", libusb_error_name(rc));
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Pass 1: Discover Audio Control (AC) interface and parse AC class descriptors
    // -------------------------------------------------------------------------
    for (uint8_t i = 0; i < config->bNumInterfaces; ++i) {
        const libusb_interface& iface = config->interface[i];
        if (iface.num_altsetting <= 0) continue;

        const libusb_interface_descriptor& alt0 = iface.altsetting[0];
        outCaps.allInterfaceNumbers.push_back(alt0.bInterfaceNumber);

        if (alt0.bInterfaceClass == kUsbClassAudio &&
            alt0.bInterfaceSubClass == kSubclassAudioControl) {
            outCaps.acInterfaceNumber = alt0.bInterfaceNumber;

            // Protocol 0x20 indicates UAC 2.0; confirm with AC HEADER bcdADC
            if (alt0.bInterfaceProtocol == 0x20) {
                outCaps.uacVersion = UacVersion::UAC_2_0;
            } else {
                outCaps.uacVersion = UacVersion::UAC_1_0;
            }

            if (alt0.extra && alt0.extra_length > 0) {
                parseAcExtraDescriptors(alt0.extra, alt0.extra_length, outCaps);
            }
        }
    }

    if (outCaps.acInterfaceNumber == 0xFF) {
        LOGE("No USB Audio Control interface found on device %04X:%04X",
             outCaps.vendorId, outCaps.productId);
        libusb_free_config_descriptor(config);
        return false;
    }

    // -------------------------------------------------------------------------
    // Pass 2: Discover Audio Streaming (AS) OUT interfaces & Alternate Settings
    // -------------------------------------------------------------------------
    for (uint8_t i = 0; i < config->bNumInterfaces; ++i) {
        const libusb_interface& iface = config->interface[i];

        for (int a = 0; a < iface.num_altsetting; ++a) {
            const libusb_interface_descriptor& alt = iface.altsetting[a];

            if (alt.bInterfaceClass != kUsbClassAudio ||
                alt.bInterfaceSubClass != kSubclassAudioStreaming) {
                continue;
            }

            // Alt setting 0 is the zero-bandwidth idle setting
            if (alt.bAlternateSetting == 0 || alt.bNumEndpoints == 0) {
                continue;
            }

            // Locate Isochronous OUT data endpoint (and optional Feedback IN endpoint)
            const libusb_endpoint_descriptor* outEp = nullptr;
            const libusb_endpoint_descriptor* fbEp = nullptr;

            for (uint8_t e = 0; e < alt.bNumEndpoints; ++e) {
                const libusb_endpoint_descriptor& ep = alt.endpoint[e];
                const uint8_t transferType = ep.bmAttributes & LIBUSB_TRANSFER_TYPE_MASK;
                if (transferType != LIBUSB_TRANSFER_TYPE_ISOCHRONOUS) {
                    continue;
                }

                const bool isIn = (ep.bEndpointAddress & LIBUSB_ENDPOINT_DIR_MASK) == LIBUSB_ENDPOINT_IN;
                const uint8_t usageType = (ep.bmAttributes >> 4) & 0x03;

                if (!isIn && usageType == 0x00) {
                    outEp = &ep;
                } else if (isIn && (usageType == 0x01 || usageType == 0x00)) {
                    // Explicit Feedback IN endpoint (usageType == 1) or synch-addressed IN ep
                    fbEp = &ep;
                }
            }

            // Skip Capture (IN-only) streaming interfaces
            if (!outEp) {
                continue;
            }

            UacStreamFormat fmt{};
            fmt.asInterfaceNumber = alt.bInterfaceNumber;
            fmt.altSetting = alt.bAlternateSetting;
            fmt.outEndpointAddress = outEp->bEndpointAddress;
            fmt.outMaxPacketSize = decodeMaxPacketSize(outEp->wMaxPacketSize, outCaps.usbSpeed);
            fmt.outInterval = outEp->bInterval;
            fmt.syncType = static_cast<IsoSyncType>((outEp->bmAttributes >> 2) & 0x03);

            // Check class-specific endpoint descriptor (UAC1 sampling freq control bit)
            if (outEp->extra && outEp->extra_length > 0) {
                int pos = 0;
                while (pos + 2 <= outEp->extra_length) {
                    uint8_t len = outEp->extra[pos];
                    uint8_t type = outEp->extra[pos + 1];
                    if (len == 0 || pos + len > outEp->extra_length) break;
                    if (type == kDescCsEndpoint && len >= 4) {
                        uint8_t bmAttr = outEp->extra[pos + 3];
                        fmt.hasSamplingFreqControl = (bmAttr & 0x01) != 0;
                    }
                    pos += len;
                }
            }

            // Match Feedback IN endpoint if present (or referenced via bSynchAddress)
            if (fbEp && (outEp->bSynchAddress == 0 ||
                         outEp->bSynchAddress == fbEp->bEndpointAddress)) {
                fmt.hasFeedbackEndpoint = true;
                fmt.feedbackEndpointAddress = fbEp->bEndpointAddress;
                fmt.feedbackMaxPacketSize = decodeMaxPacketSize(fbEp->wMaxPacketSize, outCaps.usbSpeed);
                fmt.feedbackInterval = fbEp->bInterval;
            }

            // Parse AS class-specific interface descriptors (AS_GENERAL & FORMAT_TYPE)
            bool isPcm = false;
            bool isFormatTypeI = false;

            if (alt.extra && alt.extra_length > 0) {
                int pos = 0;
                while (pos + 3 <= alt.extra_length) {
                    const uint8_t len = alt.extra[pos];
                    const uint8_t type = alt.extra[pos + 1];
                    const uint8_t subtype = alt.extra[pos + 2];
                    if (len == 0 || pos + len > alt.extra_length) break;

                    if (type == kDescCsInterface) {
                        const uint8_t* d = alt.extra + pos;

                        if (subtype == kAsGeneral) {
                            if (outCaps.uacVersion == UacVersion::UAC_1_0 && len >= 7) {
                                fmt.terminalLink = d[3];
                                const uint16_t formatTag = readLe16(d + 5);
                                isPcm = (formatTag == kUac1FormatPcm);
                            } else if (outCaps.uacVersion == UacVersion::UAC_2_0 && len >= 16) {
                                fmt.terminalLink = d[3];
                                const uint8_t formatType = d[5];
                                const uint32_t bmFormats = readLe32(d + 6);
                                fmt.channels = d[10];
                                isPcm = (formatType == kFormatTypeI) && ((bmFormats & kUac2FormatPcmBit) != 0);
                            }
                        } else if (subtype == kAsFormatType) {
                            if (outCaps.uacVersion == UacVersion::UAC_1_0 && len >= 8) {
                                const uint8_t formatType = d[3];
                                if (formatType == kFormatTypeI) {
                                    isFormatTypeI = true;
                                    fmt.channels = d[4];
                                    fmt.subslotSize = d[5];
                                    fmt.bitResolution = d[6];
                                    const uint8_t samFreqType = d[7];

                                    if (samFreqType == 0 && len >= 14) {
                                        fmt.isContinuousRate = true;
                                        fmt.minSampleRate = readLe24(d + 8);
                                        fmt.maxSampleRate = readLe24(d + 11);
                                    } else if (samFreqType > 0) {
                                        for (uint8_t r = 0; r < samFreqType; ++r) {
                                            const int off = 8 + r * 3;
                                            if (off + 3 <= len) {
                                                fmt.sampleRates.push_back(readLe24(d + off));
                                            }
                                        }
                                    }
                                }
                            } else if (outCaps.uacVersion == UacVersion::UAC_2_0 && len >= 6) {
                                const uint8_t formatType = d[3];
                                if (formatType == kFormatTypeI) {
                                    isFormatTypeI = true;
                                    fmt.subslotSize = d[4];
                                    fmt.bitResolution = d[5];
                                }
                            }
                        }
                    }
                    pos += len;
                }
            }

            if (isPcm && isFormatTypeI && fmt.channels > 0 &&
                (fmt.subslotSize == 2 || fmt.subslotSize == 3 || fmt.subslotSize == 4)) {
                outCaps.playbackFormats.push_back(std::move(fmt));
            }
        }
    }

    libusb_free_config_descriptor(config);

    // -------------------------------------------------------------------------
    // Pass 3: Resolve UAC2 Clock Source IDs, query Clock Rates, and link Feature Units
    // -------------------------------------------------------------------------
    resolveEntityLinks(handle, outCaps);

    return !outCaps.playbackFormats.empty();
}

void UacDescriptorParser::parseAcExtraDescriptors(const uint8_t* extra, int extraLen,
                                                  UacDeviceCapabilities& caps) {
    int pos = 0;
    while (pos + 3 <= extraLen) {
        const uint8_t len = extra[pos];
        const uint8_t type = extra[pos + 1];
        const uint8_t subtype = extra[pos + 2];
        if (len == 0 || pos + len > extraLen) break;

        if (type == kDescCsInterface) {
            const uint8_t* d = extra + pos;

            switch (subtype) {
                case kAcHeader: {
                    if (len >= 5) {
                        const uint16_t bcdAdc = readLe16(d + 3);
                        if (bcdAdc >= 0x0200) {
                            caps.uacVersion = UacVersion::UAC_2_0;
                        } else if (bcdAdc >= 0x0100) {
                            caps.uacVersion = UacVersion::UAC_1_0;
                        }
                    }
                    break;
                }

                case kAcInputTerminal: {
                    UacInputTerminal it{};
                    if (caps.uacVersion == UacVersion::UAC_1_0 && len >= 12) {
                        it.terminalId = d[3];
                        it.terminalType = readLe16(d + 4);
                        it.nrChannels = d[7];
                        caps.inputTerminals.push_back(it);
                    } else if (caps.uacVersion == UacVersion::UAC_2_0 && len >= 17) {
                        it.terminalId = d[3];
                        it.terminalType = readLe16(d + 4);
                        it.clockSourceId = d[7];
                        it.nrChannels = d[8];
                        caps.inputTerminals.push_back(it);
                    }
                    break;
                }

                case kAcOutputTerminal: {
                    UacOutputTerminal ot{};
                    if (caps.uacVersion == UacVersion::UAC_1_0 && len >= 9) {
                        ot.terminalId = d[3];
                        ot.terminalType = readLe16(d + 4);
                        ot.sourceId = d[7];
                        caps.outputTerminals.push_back(ot);
                    } else if (caps.uacVersion == UacVersion::UAC_2_0 && len >= 12) {
                        ot.terminalId = d[3];
                        ot.terminalType = readLe16(d + 4);
                        ot.sourceId = d[7];
                        ot.clockSourceId = d[8];
                        caps.outputTerminals.push_back(ot);
                    }
                    break;
                }

                case kAcClockSource: {
                    if (caps.uacVersion == UacVersion::UAC_2_0 && len >= 8) {
                        UacClockSource cs{};
                        cs.clockId = d[3];
                        cs.attributes = d[4];
                        cs.controls = d[5];
                        cs.assocTerminal = d[6];
                        caps.clockSources.push_back(cs);
                    }
                    break;
                }

                case kAcClockSelector: {
                    if (caps.uacVersion == UacVersion::UAC_2_0 && len >= 7) {
                        UacClockSelector sel{};
                        sel.selectorId = d[3];
                        const uint8_t nrInPins = d[4];
                        for (uint8_t p = 0; p < nrInPins && (5 + p) < len; ++p) {
                            sel.sourceIds.push_back(d[5 + p]);
                        }
                        caps.clockSelectors.push_back(sel);
                    }
                    break;
                }

                case kAcFeatureUnit: {
                    UacFeatureUnit fu{};
                    if (caps.uacVersion == UacVersion::UAC_1_0 && len >= 7) {
                        fu.unitId = d[3];
                        fu.sourceId = d[4];
                        const uint8_t ctrlSize = d[5];
                        if (ctrlSize > 0 && len >= 6 + ctrlSize) {
                            uint32_t masterCtrl = d[6];
                            fu.supportsMasterMute = (masterCtrl & 0x01) != 0;
                            fu.supportsMasterVolume = (masterCtrl & 0x02) != 0;

                            // Check per-channel controls (channel 1..N)
                            const int totalCtrlBytes = len - 7; // minus iFeature byte
                            const int numEntries = (ctrlSize > 0) ? (totalCtrlBytes / ctrlSize) : 0;
                            if (numEntries > 1) {
                                fu.numControlChannels = static_cast<uint8_t>(numEntries - 1);
                                uint32_t ch1Ctrl = d[6 + ctrlSize];
                                fu.supportsChannelMute = (ch1Ctrl & 0x01) != 0;
                                fu.supportsChannelVolume = (ch1Ctrl & 0x02) != 0;
                            }
                        }
                        caps.featureUnits.push_back(fu);
                    } else if (caps.uacVersion == UacVersion::UAC_2_0 && len >= 10) {
                        fu.unitId = d[3];
                        fu.sourceId = d[4];
                        const uint32_t masterCtrl = readLe32(d + 5);
                        // UAC2: 2 bits per control; 0b11 (3) = host programmable (R/W)
                        fu.supportsMasterMute = ((masterCtrl & 0x03) == 0x03);
                        fu.supportsMasterVolume = (((masterCtrl >> 2) & 0x03) == 0x03);

                        const int totalCtrlBytes = len - 6; // minus header(5) and iFeature(1)
                        const int numEntries = totalCtrlBytes / 4;
                        if (numEntries > 1) {
                            fu.numControlChannels = static_cast<uint8_t>(numEntries - 1);
                            const uint32_t ch1Ctrl = readLe32(d + 9);
                            fu.supportsChannelMute = ((ch1Ctrl & 0x03) == 0x03);
                            fu.supportsChannelVolume = (((ch1Ctrl >> 2) & 0x03) == 0x03);
                        }
                        caps.featureUnits.push_back(fu);
                    }
                    break;
                }

                default:
                    break;
            }
        }
        pos += len;
    }
}

bool UacDescriptorParser::queryUac2ClockRates(libusb_device_handle* handle,
                                              uint8_t acInterfaceNumber,
                                              UacClockSource& clockSource) {
    if (!handle || clockSource.clockId == 0) return false;

    // UAC 2.0 Section 5.2.3.1.1: GET RANGE on CS_SAM_FREQ_CONTROL
    uint8_t buf[512] = {0};
    const uint8_t bmRequestType =
        LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE;
    const uint16_t wValue = static_cast<uint16_t>(kUac2CsSamFreqControl << 8);
    const uint16_t wIndex =
        (static_cast<uint16_t>(clockSource.clockId) << 8) | acInterfaceNumber;

    const int transferred = libusb_control_transfer(
        handle, bmRequestType, kUac2ReqRange, wValue, wIndex, buf, sizeof(buf), 1000);

    if (transferred < 2) {
        LOGW("UAC2 RANGE query failed for ClockSource %u: %s (%d)",
             clockSource.clockId,
             transferred < 0 ? libusb_error_name(transferred) : "short read",
             transferred);
        return false;
    }

    const uint16_t numSubRanges = readLe16(buf);
    clockSource.discreteRates.clear();
    clockSource.hasContinuousRange = false;

    std::set<uint32_t> uniqueRates;
    int offset = 2;
    for (uint16_t i = 0; i < numSubRanges; ++i) {
        if (offset + 12 > transferred) break;
        const uint32_t dMin = readLe32(buf + offset);
        const uint32_t dMax = readLe32(buf + offset + 4);
        const uint32_t dRes = readLe32(buf + offset + 8);
        offset += 12;

        if (dMin == dMax || dRes == 0) {
            if (dMin > 0) uniqueRates.insert(dMin);
        } else if (dMax > dMin) {
            // If it's a large continuous range (e.g. 8000..384000 step 1), store range
            // and populate standard audiophile rates that fall within it.
            if (dRes == 1 || ((dMax - dMin) / dRes) > 64) {
                clockSource.hasContinuousRange = true;
                clockSource.minRate = (clockSource.minRate == 0) ? dMin : std::min(clockSource.minRate, dMin);
                clockSource.maxRate = std::max(clockSource.maxRate, dMax);
                static constexpr uint32_t kStdRates[] = {
                    44100, 48000, 88200, 96000, 176400, 192000, 352800, 384000, 705600, 768000
                };
                for (uint32_t sr : kStdRates) {
                    if (sr >= dMin && sr <= dMax) uniqueRates.insert(sr);
                }
            } else {
                for (uint32_t sr = dMin; sr <= dMax; sr += dRes) {
                    uniqueRates.insert(sr);
                }
            }
        }
    }

    clockSource.discreteRates.assign(uniqueRates.begin(), uniqueRates.end());
    return !clockSource.discreteRates.empty() || clockSource.hasContinuousRange;
}

void UacDescriptorParser::resolveEntityLinks(libusb_device_handle* handle,
                                             UacDeviceCapabilities& caps) {
    // 1. Query UAC 2.0 Clock Source sample rates
    if (caps.uacVersion == UacVersion::UAC_2_0) {
        for (auto& cs : caps.clockSources) {
            queryUac2ClockRates(handle, caps.acInterfaceNumber, cs);
        }
    }

    // 2. Link each playback format to its Clock Source (UAC2) and Feature Unit (UAC1/UAC2)
    for (auto& fmt : caps.playbackFormats) {
        // Find Input Terminal matching bTerminalLink
        uint8_t clockEntityId = 0;
        for (const auto& it : caps.inputTerminals) {
            if (it.terminalId == fmt.terminalLink) {
                clockEntityId = it.clockSourceId;
                break;
            }
        }

        // If clockEntityId points to a Clock Selector, resolve to its first Clock Source
        if (caps.uacVersion == UacVersion::UAC_2_0) {
            for (const auto& sel : caps.clockSelectors) {
                if (sel.selectorId == clockEntityId && !sel.sourceIds.empty()) {
                    clockEntityId = sel.sourceIds[0];
                    break;
                }
            }
            // Fallback: if still 0, use first discovered Clock Source
            if (clockEntityId == 0 && !caps.clockSources.empty()) {
                clockEntityId = caps.clockSources[0].clockId;
            }
            fmt.clockSourceId = clockEntityId;

            // Populate sample rates from the resolved Clock Source
            for (const auto& cs : caps.clockSources) {
                if (cs.clockId == fmt.clockSourceId) {
                    fmt.sampleRates = cs.discreteRates;
                    fmt.isContinuousRate = cs.hasContinuousRange;
                    fmt.minSampleRate = cs.minRate;
                    fmt.maxSampleRate = cs.maxRate;
                    break;
                }
            }
        }

        // Find Feature Unit fed directly by this Input Terminal (or fallback to first volume-capable FU)
        for (const auto& fu : caps.featureUnits) {
            if (fu.sourceId == fmt.terminalLink) {
                fmt.featureUnitId = fu.unitId;
                break;
            }
        }
        if (fmt.featureUnitId == 0) {
            for (const auto& fu : caps.featureUnits) {
                if (fu.supportsMasterVolume || fu.supportsChannelVolume) {
                    fmt.featureUnitId = fu.unitId;
                    break;
                }
            }
        }
    }
}

const char* UacDescriptorParser::syncTypeToString(IsoSyncType sync) {
    switch (sync) {
        case IsoSyncType::ASYNCHRONOUS: return "Asynchronous";
        case IsoSyncType::ADAPTIVE:     return "Adaptive";
        case IsoSyncType::SYNCHRONOUS:  return "Synchronous";
        default:                        return "None";
    }
}

const char* UacDescriptorParser::speedToString(int usbSpeed) {
    switch (usbSpeed) {
        case LIBUSB_SPEED_LOW:        return "Low-Speed (1.5 Mbps)";
        case LIBUSB_SPEED_FULL:       return "Full-Speed (12 Mbps)";
        case LIBUSB_SPEED_HIGH:       return "High-Speed (480 Mbps)";
        case LIBUSB_SPEED_SUPER:      return "SuperSpeed (5 Gbps)";
        case LIBUSB_SPEED_SUPER_PLUS: return "SuperSpeed+ (10 Gbps)";
        default:                      return "Unknown";
    }
}

void UacDescriptorParser::dumpCapabilitiesToLogcat(libusb_device_handle* handle,
                                                   const UacDeviceCapabilities& caps) {
    LOGI("================ USB DAC DIAGNOSTIC DUMP ================");
    LOGI("Device: %s %s (VID=0x%04X PID=0x%04X)",
         caps.manufacturer.c_str(), caps.productName.c_str(),
         caps.vendorId, caps.productId);
    LOGI("USB Speed: %s | UAC Version: %s | AC Interface: %u",
         speedToString(caps.usbSpeed),
         caps.uacVersion == UacVersion::UAC_2_0 ? "UAC 2.0 (0x0200)" :
         (caps.uacVersion == UacVersion::UAC_1_0 ? "UAC 1.0 (0x0100)" : "Unknown"),
         caps.acInterfaceNumber);

    for (const auto& cs : caps.clockSources) {
        std::string ratesStr;
        for (size_t i = 0; i < cs.discreteRates.size(); ++i) {
            if (i > 0) ratesStr += ", ";
            ratesStr += std::to_string(cs.discreteRates[i]);
        }
        LOGI("  [ClockSource ID=%u] attr=0x%02X ctrl=0x%02X continuous=%d (%u..%u) rates=[%s]",
             cs.clockId, cs.attributes, cs.controls,
             cs.hasContinuousRange ? 1 : 0, cs.minRate, cs.maxRate, ratesStr.c_str());
    }

    for (const auto& fu : caps.featureUnits) {
        LOGI("  [FeatureUnit ID=%u] src=%u masterMute=%d masterVol=%d chMute=%d chVol=%d chCount=%u",
             fu.unitId, fu.sourceId,
             fu.supportsMasterMute ? 1 : 0, fu.supportsMasterVolume ? 1 : 0,
             fu.supportsChannelMute ? 1 : 0, fu.supportsChannelVolume ? 1 : 0,
             fu.numControlChannels);
    }

    LOGI("Playback AudioStreaming Alt Settings (%zu found):", caps.playbackFormats.size());
    for (size_t i = 0; i < caps.playbackFormats.size(); ++i) {
        const auto& f = caps.playbackFormats[i];
        std::string ratesStr;
        for (size_t r = 0; r < f.sampleRates.size(); ++r) {
            if (r > 0) ratesStr += ", ";
            ratesStr += std::to_string(f.sampleRates[r]);
        }
        LOGI("  [%zu] AS_Iface=%u Alt=%u | %u ch, %u-bit in %u-byte subslot | TermLink=%u ClockID=%u FU=%u",
             i, f.asInterfaceNumber, f.altSetting, f.channels,
             f.bitResolution, f.subslotSize, f.terminalLink, f.clockSourceId, f.featureUnitId);
        LOGI("      OUT EP=0x%02X MaxPkt=%u bInterval=%u Sync=%s FreqCtrl=%d",
             f.outEndpointAddress, f.outMaxPacketSize, f.outInterval,
             syncTypeToString(f.syncType), f.hasSamplingFreqControl ? 1 : 0);
        if (f.hasFeedbackEndpoint) {
            LOGI("      FB  IN EP=0x%02X MaxPkt=%u bInterval=%u",
                 f.feedbackEndpointAddress, f.feedbackMaxPacketSize, f.feedbackInterval);
        } else {
            LOGI("      FB  IN EP: None (will use fractional frame accumulator)");
        }
        LOGI("      Rates: continuous=%d (%u..%u) discrete=[%s]",
             f.isContinuousRate ? 1 : 0, f.minSampleRate, f.maxSampleRate, ratesStr.c_str());
    }

    // Raw hex dump of class-specific extra descriptors for hardware quirk debugging
    if (handle) {
        libusb_device* dev = libusb_get_device(handle);
        libusb_config_descriptor* config = nullptr;
        if (dev && libusb_get_active_config_descriptor(dev, &config) == LIBUSB_SUCCESS && config) {
            for (uint8_t i = 0; i < config->bNumInterfaces; ++i) {
                const libusb_interface& iface = config->interface[i];
                for (int a = 0; a < iface.num_altsetting; ++a) {
                    const libusb_interface_descriptor& alt = iface.altsetting[a];
                    if (alt.extra_length > 0 && alt.extra) {
                        std::string hex;
                        hex.reserve(static_cast<size_t>(alt.extra_length) * 3);
                        char tmp[4];
                        for (int b = 0; b < alt.extra_length; ++b) {
                            std::snprintf(tmp, sizeof(tmp), "%02X ", alt.extra[b]);
                            hex += tmp;
                        }
                        LOGI("  Raw Extra [Iface %u Alt %u, %d bytes]: %s",
                             alt.bInterfaceNumber, alt.bAlternateSetting, alt.extra_length, hex.c_str());
                    }
                }
            }
            libusb_free_config_descriptor(config);
        }
    }
    LOGI("=========================================================");
}

} // namespace bitperfect

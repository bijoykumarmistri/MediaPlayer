#pragma once

#include <cstdint>
#include <memory>
#include <string>

namespace bitperfect {

struct AudioFormatInfo {
    uint32_t sampleRate = 0;       // Hz (e.g., 44100, 48000, 96000, 192000, 384000)
    uint32_t bitDepth = 0;         // Source bit depth (16, 24, 32)
    uint32_t channels = 0;         // Channel count (typically 2)
    uint64_t totalFrames = 0;      // Total PCM frames (0 if unknown)
    std::string codecName;         // "FLAC", "WAV", "MP3", etc.
};

/**
 * Abstract audio decoder interface.
 *
 * All implementations MUST output interleaved signed 32-bit PCM (`int32_t`)
 * in MSB-aligned (left-justified) full-scale format:
 *   - 16-bit source sample `s16` -> `static_cast<int32_t>(s16) << 16`
 *   - 24-bit source sample `s24` -> `static_cast<int32_t>(s24) << 8`
 *   - 32-bit source sample `s32` -> `s32`
 *
 * This guarantees bit-exact lossless preservation for 16/24/32-bit PCM while
 * allowing the USB engine to pack directly to S16_LE, S24_3LE, or S32_LE via
 * zero-allocation bit shifts.
 */
class AudioDecoder {
public:
    virtual ~AudioDecoder() = default;

    virtual bool open(const std::string& filePath) = 0;
    virtual void close() = 0;

    virtual const AudioFormatInfo& getFormat() const = 0;

    /**
     * Decode up to `frameCount` PCM frames into `outInterleavedS32`.
     * Buffer must hold at least `frameCount * format.channels` int32_t elements.
     * Returns actual number of frames decoded (0 indicates EOF or error).
     */
    virtual uint64_t readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) = 0;

    /**
     * Seek to the exact PCM frame index.
     */
    virtual bool seekToFrame(uint64_t frameIndex) = 0;

    /**
     * Current playback cursor in PCM frames.
     */
    virtual uint64_t getCurrentFrame() const = 0;
};

/**
 * Factory that inspects magic bytes / file extension and instantiates
 * the appropriate decoder (FLAC, WAV, MP3).
 */
std::unique_ptr<AudioDecoder> createDecoderForFile(const std::string& filePath);

} // namespace bitperfect

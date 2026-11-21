#pragma once

#include "AudioDecoder.h"
#include <vector>

#include "dr_flac.h"
#include "dr_wav.h"
#include "dr_mp3.h"

namespace bitperfect {

class FlacDecoder final : public AudioDecoder {
public:
    FlacDecoder() = default;
    ~FlacDecoder() override;

    bool open(const std::string& filePath) override;
    void close() override;
    const AudioFormatInfo& getFormat() const override { return format_; }
    uint64_t readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) override;
    bool seekToFrame(uint64_t frameIndex) override;
    uint64_t getCurrentFrame() const override { return currentFrame_; }

private:
    drflac* flac_{nullptr};
    AudioFormatInfo format_{};
    uint64_t currentFrame_{0};
};

class WavDecoder final : public AudioDecoder {
public:
    WavDecoder() = default;
    ~WavDecoder() override;

    bool open(const std::string& filePath) override;
    void close() override;
    const AudioFormatInfo& getFormat() const override { return format_; }
    uint64_t readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) override;
    bool seekToFrame(uint64_t frameIndex) override;
    uint64_t getCurrentFrame() const override { return currentFrame_; }

private:
    drwav* wav_{nullptr};
    AudioFormatInfo format_{};
    uint64_t currentFrame_{0};
};

class Mp3Decoder final : public AudioDecoder {
public:
    Mp3Decoder() = default;
    ~Mp3Decoder() override;

    bool open(const std::string& filePath) override;
    void close() override;
    const AudioFormatInfo& getFormat() const override { return format_; }
    uint64_t readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) override;
    bool seekToFrame(uint64_t frameIndex) override;
    uint64_t getCurrentFrame() const override { return currentFrame_; }

private:
    drmp3* mp3_{nullptr};
    AudioFormatInfo format_{};
    uint64_t currentFrame_{0};
    std::vector<float> floatScratch_;
};

} // namespace bitperfect

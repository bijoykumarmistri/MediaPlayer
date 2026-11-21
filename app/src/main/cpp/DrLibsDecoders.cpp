#include "DrLibsDecoders.h"

#define DR_FLAC_IMPLEMENTATION
#include "dr_flac.h"

#define DR_WAV_IMPLEMENTATION
#include "dr_wav.h"

#define DR_MP3_IMPLEMENTATION
#include "dr_mp3.h"

#include <android/log.h>
#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdio>
#include <cstring>

#define LOG_TAG "BitPerfectDecoder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace bitperfect {

// =============================================================================
// FlacDecoder
// =============================================================================
FlacDecoder::~FlacDecoder() {
    close();
}

bool FlacDecoder::open(const std::string& filePath) {
    close();
    flac_ = drflac_open_file(filePath.c_str(), nullptr);
    if (!flac_) {
        LOGE("drflac_open_file failed for: %s", filePath.c_str());
        return false;
    }

    format_.sampleRate = flac_->sampleRate;
    format_.bitDepth = flac_->bitsPerSample;
    format_.channels = flac_->channels;
    format_.totalFrames = flac_->totalPCMFrameCount;
    format_.codecName = "FLAC";
    currentFrame_ = 0;

    LOGI("Opened FLAC: %u Hz, %u-bit, %u ch, %llu frames",
         format_.sampleRate, format_.bitDepth, format_.channels,
         static_cast<unsigned long long>(format_.totalFrames));
    return true;
}

void FlacDecoder::close() {
    if (flac_) {
        drflac_close(flac_);
        flac_ = nullptr;
    }
    currentFrame_ = 0;
}

uint64_t FlacDecoder::readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) {
    if (!flac_ || !outInterleavedS32 || frameCount == 0) return 0;
    // drflac_read_pcm_frames_s32 outputs MSB-aligned 32-bit signed integers.
    const drflac_uint64 read = drflac_read_pcm_frames_s32(flac_, frameCount, outInterleavedS32);
    currentFrame_ += read;
    return read;
}

bool FlacDecoder::seekToFrame(uint64_t frameIndex) {
    if (!flac_) return false;
    if (format_.totalFrames > 0 && frameIndex >= format_.totalFrames) {
        frameIndex = format_.totalFrames - 1;
    }
    if (drflac_seek_to_pcm_frame(flac_, frameIndex)) {
        currentFrame_ = frameIndex;
        return true;
    }
    return false;
}

// =============================================================================
// WavDecoder
// =============================================================================
WavDecoder::~WavDecoder() {
    close();
}

bool WavDecoder::open(const std::string& filePath) {
    close();
    wav_ = new drwav();
    if (!drwav_init_file(wav_, filePath.c_str(), nullptr)) {
        LOGE("drwav_init_file failed for: %s", filePath.c_str());
        delete wav_;
        wav_ = nullptr;
        return false;
    }

    format_.sampleRate = wav_->sampleRate;
    format_.bitDepth = wav_->bitsPerSample;
    format_.channels = wav_->channels;
    format_.totalFrames = wav_->totalPCMFrameCount;
    format_.codecName = "WAV";
    currentFrame_ = 0;

    LOGI("Opened WAV: %u Hz, %u-bit, %u ch, %llu frames",
         format_.sampleRate, format_.bitDepth, format_.channels,
         static_cast<unsigned long long>(format_.totalFrames));
    return true;
}

void WavDecoder::close() {
    if (wav_) {
        drwav_uninit(wav_);
        delete wav_;
        wav_ = nullptr;
    }
    currentFrame_ = 0;
}

uint64_t WavDecoder::readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) {
    if (!wav_ || !outInterleavedS32 || frameCount == 0) return 0;
    // drwav_read_pcm_frames_s32 outputs MSB-aligned 32-bit signed integers.
    const drwav_uint64 read = drwav_read_pcm_frames_s32(wav_, frameCount, outInterleavedS32);
    currentFrame_ += read;
    return read;
}

bool WavDecoder::seekToFrame(uint64_t frameIndex) {
    if (!wav_) return false;
    if (format_.totalFrames > 0 && frameIndex >= format_.totalFrames) {
        frameIndex = format_.totalFrames - 1;
    }
    if (drwav_seek_to_pcm_frame(wav_, frameIndex)) {
        currentFrame_ = frameIndex;
        return true;
    }
    return false;
}

// =============================================================================
// Mp3Decoder
// =============================================================================
Mp3Decoder::~Mp3Decoder() {
    close();
}

bool Mp3Decoder::open(const std::string& filePath) {
    close();
    mp3_ = new drmp3();
    if (!drmp3_init_file(mp3_, filePath.c_str(), nullptr)) {
        LOGE("drmp3_init_file failed for: %s", filePath.c_str());
        delete mp3_;
        mp3_ = nullptr;
        return false;
    }

    format_.sampleRate = mp3_->sampleRate;
    format_.bitDepth = 24; // Decode float32 MP3 output to 24-bit effective precision
    format_.channels = mp3_->channels;
    format_.totalFrames = drmp3_get_pcm_frame_count(mp3_);
    format_.codecName = "MP3";
    currentFrame_ = 0;

    LOGI("Opened MP3: %u Hz, %u ch, %llu frames",
         format_.sampleRate, format_.channels,
         static_cast<unsigned long long>(format_.totalFrames));
    return true;
}

void Mp3Decoder::close() {
    if (mp3_) {
        drmp3_uninit(mp3_);
        delete mp3_;
        mp3_ = nullptr;
    }
    floatScratch_.clear();
    currentFrame_ = 0;
}

uint64_t Mp3Decoder::readFramesS32(int32_t* outInterleavedS32, uint64_t frameCount) {
    if (!mp3_ || !outInterleavedS32 || frameCount == 0) return 0;

    const size_t totalSamples = static_cast<size_t>(frameCount) * format_.channels;
    if (floatScratch_.size() < totalSamples) {
        floatScratch_.resize(totalSamples);
    }

    const drmp3_uint64 read = drmp3_read_pcm_frames_f32(mp3_, frameCount, floatScratch_.data());
    const size_t samplesRead = static_cast<size_t>(read) * format_.channels;

    // Convert float [-1.0, +1.0] to signed 32-bit PCM with symmetric clamping
    constexpr double kScale = 2147483647.0; // INT32_MAX
    for (size_t i = 0; i < samplesRead; ++i) {
        float s = floatScratch_[i];
        if (s >= 1.0f) {
            outInterleavedS32[i] = INT32_MAX;
        } else if (s <= -1.0f) {
            outInterleavedS32[i] = INT32_MIN;
        } else {
            outInterleavedS32[i] = static_cast<int32_t>(std::lrint(static_cast<double>(s) * kScale));
        }
    }

    currentFrame_ += read;
    return read;
}

bool Mp3Decoder::seekToFrame(uint64_t frameIndex) {
    if (!mp3_) return false;
    if (format_.totalFrames > 0 && frameIndex >= format_.totalFrames) {
        frameIndex = format_.totalFrames - 1;
    }
    if (drmp3_seek_to_pcm_frame(mp3_, frameIndex)) {
        currentFrame_ = frameIndex;
        return true;
    }
    return false;
}

// =============================================================================
// Decoder Factory (Magic-Byte Sniffing + Extension Fallback)
// =============================================================================
static std::string toLowerExtension(const std::string& path) {
    const auto pos = path.find_last_of('.');
    if (pos == std::string::npos) return "";
    std::string ext = path.substr(pos + 1);
    std::transform(ext.begin(), ext.end(), ext.begin(),
                   [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return ext;
}

std::unique_ptr<AudioDecoder> createDecoderForFile(const std::string& filePath) {
    // 1. Sniff first 12 bytes for magic signatures
    uint8_t header[12] = {0};
    FILE* f = std::fopen(filePath.c_str(), "rb");
    if (f) {
        const size_t n = std::fread(header, 1, sizeof(header), f);
        std::fclose(f);

        if (n >= 4 && std::memcmp(header, "fLaC", 4) == 0) {
            auto dec = std::make_unique<FlacDecoder>();
            if (dec->open(filePath)) return dec;
        }
        if (n >= 12 &&
            (std::memcmp(header, "RIFF", 4) == 0 || std::memcmp(header, "RF64", 4) == 0) &&
            std::memcmp(header + 8, "WAVE", 4) == 0) {
            auto dec = std::make_unique<WavDecoder>();
            if (dec->open(filePath)) return dec;
        }
        if (n >= 3 && (std::memcmp(header, "ID3", 3) == 0 ||
                       (header[0] == 0xFF && (header[1] & 0xE0) == 0xE0))) {
            auto dec = std::make_unique<Mp3Decoder>();
            if (dec->open(filePath)) return dec;
        }
    }

    // 2. Fallback to extension
    const std::string ext = toLowerExtension(filePath);
    if (ext == "flac") {
        auto dec = std::make_unique<FlacDecoder>();
        if (dec->open(filePath)) return dec;
    } else if (ext == "wav" || ext == "wave") {
        auto dec = std::make_unique<WavDecoder>();
        if (dec->open(filePath)) return dec;
    } else if (ext == "mp3") {
        auto dec = std::make_unique<Mp3Decoder>();
        if (dec->open(filePath)) return dec;
    }

    LOGE("No supported decoder could open file: %s", filePath.c_str());
    return nullptr;
}

} // namespace bitperfect

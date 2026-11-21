#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>
#include <algorithm>

namespace bitperfect {

/**
 * Wait-free Single-Producer Single-Consumer (SPSC) ring buffer.
 * - Producer: Audio decoder thread
 * - Consumer: Real-time libusb isochronous transfer thread
 *
 * Capacity is rounded up to the next power of two so wrap-around uses a single
 * bitwise AND mask instead of integer modulo.
 */
template <typename T>
class LockFreeRingBuffer {
public:
    explicit LockFreeRingBuffer(size_t minCapacityElements) {
        size_t cap = 1;
        while (cap < minCapacityElements) {
            cap <<= 1;
        }
        capacity_ = cap;
        mask_ = cap - 1;
        buffer_.resize(capacity_);
        writePos_.store(0, std::memory_order_relaxed);
        readPos_.store(0, std::memory_order_relaxed);
    }

    LockFreeRingBuffer(const LockFreeRingBuffer&) = delete;
    LockFreeRingBuffer& operator=(const LockFreeRingBuffer&) = delete;

    size_t capacity() const noexcept { return capacity_; }

    /**
     * Number of elements currently available to read (safe from consumer or monitoring).
     */
    size_t availableRead() const noexcept {
        const size_t w = writePos_.load(std::memory_order_acquire);
        const size_t r = readPos_.load(std::memory_order_relaxed);
        return w - r;
    }

    /**
     * Number of free element slots available to write (safe from producer).
     */
    size_t availableWrite() const noexcept {
        const size_t w = writePos_.load(std::memory_order_relaxed);
        const size_t r = readPos_.load(std::memory_order_acquire);
        return capacity_ - (w - r);
    }

    /**
     * Push up to `count` elements from `src`. Returns the actual count written.
     * Producer-only. Lock-free & wait-free.
     */
    size_t write(const T* src, size_t count) noexcept {
        if (count == 0 || src == nullptr) return 0;

        const size_t w = writePos_.load(std::memory_order_relaxed);
        const size_t r = readPos_.load(std::memory_order_acquire);
        const size_t freeSpace = capacity_ - (w - r);
        const size_t toWrite = std::min(count, freeSpace);

        if (toWrite == 0) return 0;

        const size_t startIdx = w & mask_;
        const size_t firstPart = std::min(toWrite, capacity_ - startIdx);
        const size_t secondPart = toWrite - firstPart;

        std::memcpy(buffer_.data() + startIdx, src, firstPart * sizeof(T));
        if (secondPart > 0) {
            std::memcpy(buffer_.data(), src + firstPart, secondPart * sizeof(T));
        }

        writePos_.store(w + toWrite, std::memory_order_release);
        return toWrite;
    }

    /**
     * Pop up to `count` elements into `dst`. Returns the actual count read.
     * Consumer-only. Lock-free & wait-free.
     */
    size_t read(T* dst, size_t count) noexcept {
        if (count == 0 || dst == nullptr) return 0;

        const size_t r = readPos_.load(std::memory_order_relaxed);
        const size_t w = writePos_.load(std::memory_order_acquire);
        const size_t avail = w - r;
        const size_t toRead = std::min(count, avail);

        if (toRead == 0) return 0;

        const size_t startIdx = r & mask_;
        const size_t firstPart = std::min(toRead, capacity_ - startIdx);
        const size_t secondPart = toRead - firstPart;

        std::memcpy(dst, buffer_.data() + startIdx, firstPart * sizeof(T));
        if (secondPart > 0) {
            std::memcpy(dst + firstPart, buffer_.data(), secondPart * sizeof(T));
        }

        readPos_.store(r + toRead, std::memory_order_release);
        return toRead;
    }

    /**
     * Consumer-only fast discard (used during seek/flush when synchronized with producer).
     */
    void consumeAll() noexcept {
        const size_t w = writePos_.load(std::memory_order_acquire);
        readPos_.store(w, std::memory_order_release);
    }

    /**
     * Hard reset — call ONLY when both producer and consumer are stopped/quiesced.
     */
    void reset() noexcept {
        writePos_.store(0, std::memory_order_relaxed);
        readPos_.store(0, std::memory_order_relaxed);
    }

private:
    static constexpr size_t kCacheLineSize = 64;

    size_t capacity_{0};
    size_t mask_{0};
    std::vector<T> buffer_;

    alignas(kCacheLineSize) std::atomic<size_t> writePos_{0};
    alignas(kCacheLineSize) std::atomic<size_t> readPos_{0};
};

} // namespace bitperfect

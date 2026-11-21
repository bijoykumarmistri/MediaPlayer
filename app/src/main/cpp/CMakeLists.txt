cmake_minimum_required(VERSION 3.22.1)
project(bitperfect_usb LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

set(LIBUSB_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/libusb")
set(DR_LIBS_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/dr_libs")

# ------------------------------------------------------------------------------
# 1. Static libusb-1.0 build for Android NDK (no udev, no device discovery)
# ------------------------------------------------------------------------------
if(NOT EXISTS "${LIBUSB_ROOT}/libusb/core.c")
    message(FATAL_ERROR
        "libusb sources not found at ${LIBUSB_ROOT}.\n"
        "Run: git submodule add https://github.com/libusb/libusb.git app/src/main/cpp/third_party/libusb")
endif()

set(LIBUSB_SOURCES
    "${LIBUSB_ROOT}/libusb/core.c"
    "${LIBUSB_ROOT}/libusb/descriptor.c"
    "${LIBUSB_ROOT}/libusb/hotplug.c"
    "${LIBUSB_ROOT}/libusb/io.c"
    "${LIBUSB_ROOT}/libusb/strerror.c"
    "${LIBUSB_ROOT}/libusb/sync.c"
    "${LIBUSB_ROOT}/libusb/os/events_posix.c"
    "${LIBUSB_ROOT}/libusb/os/threads_posix.c"
    "${LIBUSB_ROOT}/libusb/os/linux_usbfs.c"
    "${LIBUSB_ROOT}/libusb/os/linux_netlink.c"
)

add_library(usb1.0 STATIC ${LIBUSB_SOURCES})

target_include_directories(usb1.0
    PUBLIC
        "${LIBUSB_ROOT}/libusb"
    PRIVATE
        "${LIBUSB_ROOT}/android"
        "${LIBUSB_ROOT}/libusb/os"
)

# Android libusb configuration macros (matches libusb/android/config.h)
target_compile_definitions(usb1.0 PRIVATE
    PLATFORM_POSIX=1
    HAVE_CLOCK_GETTIME=1
    HAVE_EVENTFD=1
    HAVE_LINUX_NETLINK_H=1
    HAVE_PIPE2=1
    HAVE_POLL_NFDS_TYPE=nfds_t
    HAVE_PTHREAD_CONDATTR_SETCLOCK=1
    HAVE_PTHREAD_SETNAME_NP=1
    HAVE_STRING_H=1
    HAVE_STRUCT_TIMESPEC=1
    HAVE_SYSLOG=1
    HAVE_SYS_TIME_H=1
    HAVE_TIMERFD=1
    ENABLE_LOGGING=1
    _GNU_SOURCE=1
    DEFAULT_VISIBILITY=__attribute__\(\(visibility\(\"default\"\)\)\)
)

target_compile_options(usb1.0 PRIVATE
    -O3
    -fPIC
    -Wno-sign-compare
    -Wno-unused-function
    -Wno-unused-variable
)

# ------------------------------------------------------------------------------
# 2. Core Bit-Perfect USB Audio Engine Shared Library
# ------------------------------------------------------------------------------
add_library(bitperfect_usb SHARED
    DrLibsDecoders.cpp
    UacDescriptorParser.cpp
    UsbAudioEngine.cpp
    AudioPipeline.cpp
    BitPerfectJni.cpp
)

target_include_directories(bitperfect_usb PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${DR_LIBS_ROOT}"
    "${LIBUSB_ROOT}/libusb"
)

target_compile_options(bitperfect_usb PRIVATE
    -O3
    -fPIC
    -fvisibility=hidden
)

find_library(LOG_LIB log)

target_link_libraries(bitperfect_usb
    PRIVATE
        usb1.0
        ${LOG_LIB}
)

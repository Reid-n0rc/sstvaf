# SSTVAF native module.
#
# Builds a single libft8af.so containing:
#   - USB Audio Class capture/playback (libusb-backed)
#   - Waterfall FFT display (kissfft + fft_display)
#   - Audio resampler JNI (rational resampler / FIR decimator headers)
#
# The library is still named "ft8af" this phase so the Java-side
# System.loadLibrary("ft8af") calls (UsbAudioNative, FT8Resample,
# SpectrumFragment) stay unchanged; it is renamed together with the Java
# packages in a later PR. The FT8 DSP (ft8_lib + decode/encode glue) was
# removed in the SSTVAF transformation; the SSTV codec lands under
# cpp/sstv_lib/ + cpp/sstvaf_glue/ in a later PR.
#
# Why libusb: Android's UsbRequest API silently fails to drive
# isochronous transfers on some kernels (notably automotive Android
# variants), throwing IllegalStateException on first queue. libusb
# uses USBDEVFS ioctls directly and is the most battle-tested option
# for USB audio on Android.

cmake_minimum_required(VERSION 3.18.1)

project(ft8af_native)

# ---------------------------------------------------------------------------
# libusb 1.0.27 (static)
# ---------------------------------------------------------------------------
# Source list mirrors libusb/android/jni/libusb.mk — only the Linux/USBFS
# backend + POSIX threads/events; the macOS/Windows/Haiku/etc. backends in
# libusb/os/ are intentionally not compiled.

set(LIBUSB_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/libusb)

add_library(usb1.0 STATIC
        ${LIBUSB_ROOT}/libusb/core.c
        ${LIBUSB_ROOT}/libusb/descriptor.c
        ${LIBUSB_ROOT}/libusb/hotplug.c
        ${LIBUSB_ROOT}/libusb/io.c
        ${LIBUSB_ROOT}/libusb/sync.c
        ${LIBUSB_ROOT}/libusb/strerror.c
        ${LIBUSB_ROOT}/libusb/os/linux_usbfs.c
        ${LIBUSB_ROOT}/libusb/os/events_posix.c
        ${LIBUSB_ROOT}/libusb/os/threads_posix.c
        ${LIBUSB_ROOT}/libusb/os/linux_netlink.c
)

target_include_directories(usb1.0
        PUBLIC
        ${LIBUSB_ROOT}/libusb
        PRIVATE
        ${LIBUSB_ROOT}/android
        ${LIBUSB_ROOT}/libusb/os
)

# libusb assumes POSIX threads, eventfd, timerfd, etc. — all present on
# Android. Hidden visibility matches upstream's Android build so we don't
# accidentally re-export libusb symbols from our .so.
target_compile_options(usb1.0 PRIVATE
        -fvisibility=hidden
        -pthread
        -Wno-pointer-arith
        -Wno-deprecated-declarations
)

# ---------------------------------------------------------------------------
# libft8af.so — unified JNI bridge
# ---------------------------------------------------------------------------

add_library(ft8af SHARED
        # USB audio
        usb_audio.cpp
        usb_audio_capture.cpp
        # kissfft (waterfall FFT)
        kissfft/kiss_fft.c
        kissfft/kiss_fftr.c
        # JNI wrappers — FFT display (waterfall)
        ft8af_glue/ft8_fft_jni.cpp
        ft8af_glue/fft_display.c
        # JNI wrappers — audio resampler
        ft8af_glue/ft8_resample_jni.cpp
)

target_include_directories(ft8af PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/kissfft
)

find_library(log-lib log)

target_link_options(ft8af PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(ft8af
        usb1.0
        ${log-lib}
)

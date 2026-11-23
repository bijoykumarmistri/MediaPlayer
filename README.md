# Media Player Android App

A modern, high-performance Android media player built with **Jetpack Compose** and **Kotlin**, featuring high-fidelity media playback, seamless music/video controls, and a fast **Privacy Vault**.

[![Download APK](https://img.shields.io/badge/Download%20APK-v1.4-blue?logo=android&style=for-the-badge)](https://github.com/bijoykumarmistri/MediaPlayer/releases/latest/download/MediaPlayer-v1.4.apk)
[![Latest Release](https://img.shields.io/github/v/release/bijoykumarmistri/MediaPlayer?style=for-the-badge)](https://github.com/bijoykumarmistri/MediaPlayer/releases/latest)

---

## 📥 Download & Install

Anyone can download and install the latest APK directly on their Android device:
👉 **[Download MediaPlayer-v1.4.apk](https://github.com/bijoykumarmistri/MediaPlayer/releases/download/v1.4/MediaPlayer-v1.4.apk)**

*Compatible with Android 8.0 (API level 26) and above.*

---

## Key Features

- **Privacy Vault / Secret Folder**: Password/PIN and Security Question protected private vault to hide sensitive videos and audio from device gallery and other apps, with instant single-tap playback and direct restoration to original folders.
- **In-App GitHub Release Update Checker**: Automatically checks for newer versions on startup and provides an in-app dialog with changelog details and one-tap direct download.
- **Luxury OLED "About & Support"**: Re-engineered luxury OLED dialog featuring developer info, quick bug reporting, email contact, and release notes.
- **Multi-File Batch Selection, Sharing & Deletion**: Select multiple audio tracks or videos at once with long-press or menu actions to share via external apps or permanently delete from device storage with Scoped Storage consent.
- **Pinch-to-Zoom & Pan Video Playback**: Smooth hardware-accelerated pinch-to-zoom (1.0x to 5.0x) with pan repositioning, real-time zoom HUD percentage, and double-tap to reset.
- **Universal Media Playback**: Supports both local music and video playback with smooth playback controls.
- **High-Fidelity Audio Playback**: Direct audio routing via Android system mixer supporting high-resolution FLAC, ALAC, DSD, and MP3 formats.
- **Modern Jetpack Compose UI**: Clean, expressive Material 3 design with dynamic palette-based theming.
- **Floating Mini Player & Media Hub**: Persistent background playback with intuitive bottom mini-player controls.
- **High Refresh Rate Support**: Fluid navigation optimized for 90Hz / 120Hz displays.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Media Engine**: ExoPlayer / Android Media3
- **Audio Routing**: Android System Audio Mixer & ExoPlayer Default AudioTrack
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/bijoykumarmistri/MediaPlayer.git
   ```
2. Open the project in **Android Studio Ladybug (or newer)**.
3. Sync Gradle and build the app:
   ```bash
   ./gradlew assembleRelease
   ```
4. Run on an Android device running Android 8.0 (API level 26) or higher.

---

## License

This project is licensed under the Apache 2.0 License.

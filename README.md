# Media Player Android App

A modern, high-performance Android media player built with **Jetpack Compose** and **Kotlin**, featuring audiophile-grade **USB Bit-Perfect Audio** output and seamless music/video playback.

---

## Key Features

- **Universal Media Playback**: Supports both local music and video playback with smooth playback controls.
- **Audiophile USB Bit-Perfect Audio**: Direct USB DAC hardware access and real-time audio telemetry (sample rate, bit depth, format).
- **Modern Jetpack Compose UI**: Clean, expressive Material 3 design with dynamic palette-based theming.
- **Floating Mini Player & Media Hub**: Persistent background playback with intuitive bottom mini-player controls.
- **High Refresh Rate Support**: Fluid navigation optimized for 90Hz / 120Hz displays.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Media Engine**: ExoPlayer / Android Media3
- **Audio Routing**: Custom USB Bit-Perfect Audio & USB Host Service
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
   ./gradlew assembleDebug
   ```
4. Run on an Android device running Android 8.0 (API level 26) or higher.

---

## License

This project is licensed under the Apache 2.0 License.

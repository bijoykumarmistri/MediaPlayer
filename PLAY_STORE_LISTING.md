# Google Play Store Listing & Submission Guide for BKM Player

This document contains all the copy, declarations, and configurations needed to publish **BKM Player** on the Google Play Store.

---

## 1. Store Listing Metadata

### App Title
**BKM Player - Video & Music** *(or: BKM Player - HD Media Player)*

### Short Description (Max 80 characters)
> Powerful, fast HD video and music player with gesture controls and vault.

### Full Description (Max 4,000 characters)
```text
BKM Player is a modern, high-performance video and music player designed for Android. Built with advanced hardware acceleration, libmpv, and Google Jetpack Media3, BKM Player delivers smooth, battery-efficient playback for all your favorite media formats.

🎬 Advanced Video Playback
• Supports all popular video formats: MP4, MKV, AVI, MOV, FLV, TS, WebM, and more.
• Hardware-Accelerated Decoding: Powered by libmpv and FFmpeg for crystal-clear 4K and Full HD video without lag.
• Intuitive Gesture Controls: Swipe to adjust brightness (left side), volume (right side), and seek forward/backward with precision.
• Picture-in-Picture (PiP) Mode: Continue watching your videos in a floating window while multitasking across other apps.
• Background Video Playback: Listen to video audio in the background with convenient lock screen and notification controls.
• Aspect Ratio & Zoom: Switch between Fit to Screen, Crop, Stretch, 16:9, and 4:3 modes easily.
• High Refresh Rate Support: Enjoy fluid 90Hz / 120Hz smooth scrolling and UI animations.

🎵 Powerful Music Player
• Plays MP3, FLAC, AAC, WAV, OGG, M4A, and high-resolution audio formats.
• Sleek Now-Playing Screen: Dynamic color palette theming based on album artwork.
• Background Audio Service: Seamless playback with lock screen media controls and notification actions.
• Queue & Playlist Management: Shuffle, repeat, and organize your favorite songs effortlessly.

🔒 Secure Privacy Vault
• Hide personal videos and audio in a password-protected local vault.
• PIN Protection: Secured using industry-standard salted hashing.
• 100% Offline & Private: Your hidden media stays strictly on your device and is never uploaded anywhere.

⚡ Clean, Modern & Lightweight
• Pure Material 3 Design with Dark & Light theme support.
• Zero Tracking, Zero Ads, 100% Offline: We value your privacy and do not collect any personal data.

Download BKM Player today and enjoy the ultimate video and audio playback experience on Android!
```

---

## 2. Google Play Console Policy Answers

### A. Privacy Policy URL
Host `privacy_policy.html` on GitHub Pages (e.g. `https://bijoykumarmistri.github.io/MediaPlayer/privacy_policy.html`) or paste the raw markdown on a public site, then paste the URL into **App content > Privacy Policy**.

---

### B. Data Safety Form Answers
In the **App Content > Data Safety** section of Google Play Console, answer:

1. **Does your app collect or share any of the required user data types?**
   * Answer: **No**
2. **Is all user data collected by your app encrypted in transit?**
   * Answer: **Not applicable** (No data is collected or transmitted).
3. **Do you provide a way for users to request that their data is deleted?**
   * Answer: **Yes** (Deleting local app data or uninstalling the app deletes all local cache and vault data).

---

### C. Foreground Service Declaration (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`)
Google Play requires a written justification for using foreground services:

* **Foreground Service Type:** `Media Playback`
* **User-Facing Description (Copy & Paste):**
```text
BKM Player is a local audio and video media player. The app uses FOREGROUND_SERVICE_MEDIA_PLAYBACK to maintain uninterrupted playback of user audio and video files when the screen is turned off or when the user navigates away from the app. A persistent system notification with playback controls (play, pause, skip, and media progress) is provided to ensure the user has continuous control and awareness of the active media session.
```
* **Video Demonstration Link:** Provide a short screen recording link (e.g. unlisted YouTube link or Google Drive link) showing:
  1. Starting music or video playback in BKM Player.
  2. Pressing the Home button or locking the screen.
  3. Showing the ongoing playback and the notification controls in the Android status bar.

---

### D. App Category & Tags
* **Category:** Video Players & Editors (or Music & Audio)
* **Tags:** Video player, Music player, Media player, HD player, MP4 player, MP3 player
* **Target Audience:** Ages 13+ (Select "13 and older", "18 and older")
* **Ads:** Select **"No, my app does not contain ads"**

---

## 3. How to Build the Release App Bundle (.aab)

To upload to Google Play, you need a signed `.aab` file:

### Option 1: Via Android Studio Menu (Recommended)
1. In Android Studio, click **Build > Generate Signed Bundle / APK...**
2. Select **Android App Bundle (.aab)** and click **Next**.
3. Under **Key store path**, click **Create new...** if you don't have one:
   * Choose a location on your computer (e.g., `bkm-release-key.jks`).
   * Enter a password, alias name, and alias password.
   * Fill in your name/organization.
4. Click **OK**, then check **Remember passwords**, and click **Next**.
5. Select destination folder and choose **release** build variant.
6. Click **Create**. Android Studio will output the signed bundle file ready to upload to Play Console!

### Option 2: Via Gradle (`keystore.properties`)
1. Copy `keystore.properties.example` to `keystore.properties` in your project root.
2. Put your `.jks` keystore file and fill in your passwords and alias in `keystore.properties`.
3. Run the Gradle command:
   ```bash
   ./gradlew :app:bundleRelease
   ```
4. The output bundle will be generated at:
   `app/build/outputs/bundle/release/app-release.aab`

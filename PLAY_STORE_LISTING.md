# Google Play Store Listing & Submission Guide for BKM Player

This document contains everything needed to build and publish **BKM Player** (`com.bkm.player`) on the **Google Play Store**—including via an authorized friend's Google Play Developer account.

---

## 1. Two Build Flavors (`playstore` vs `github`)

The app is configured with two `distribution` flavors so your Play Store upload complies with all Google Play policies while keeping direct UPI & GitHub updates for sideloaded GitHub builds:

| Feature | `playstore` flavor (`.aab` for Google Play) | `github` flavor (`.apk` for GitHub Releases) |
|---|---|---|
| **Price & Subscriptions** | 100% Free, Ad-Free, **No Subscriptions** | 100% Free, Ad-Free, **No Subscriptions** |
| **Developer GitHub Link** | **Included** (`github.com/bijoykumarmistri`) | **Included** (`github.com/bijoykumarmistri`) |
| **Optional Donations** | **Google Play Billing** one-time tips (`tip_small`, `tip_medium`, `tip_large`, `tip_generous`) — auto-hidden if not configured in Play Console | Direct UPI (`bijoymistri367@okaxis`) |
| **Update Checker** | Official **Google Play In-App Updates** | GitHub Releases API |
| **Share App Link** | Google Play Store URL | GitHub Releases URL |

---

## 2. Publishing via Your Friend's Developer Account

1. **Best Setup (Collaborator Access):**
   - Ask your friend to open **Play Console > Users and permissions > Invite new users** and invite your Google account with **Release to production / Manage store presence** permissions for BKM Player (or they can upload the `.aab` directly).
2. **Store Contact Details:**
   - In **Play Console > Grow users > Store settings > Store listing contact details**, enter a valid support email address (either yours or a shared support email) so it aligns with the app's Privacy Policy and developer credit (`Bijoy Kumar Mistri`).
3. **12-Tester Rule (Personal Accounts Created After Nov 2023):**
   - If your friend has a **Personal** developer account created after Nov 2023, Google Play requires **at least 12 testers opted in to Closed Testing continuously for 14 days** before unlocking Production release.
4. **Optional One-Time Tip Jar Setup (No Subscriptions):**
   - Because Google Play prohibits external UPI/donation links inside Play-distributed apps, the `playstore` build uses **Google Play Billing one-time consumable products** (no subscriptions).
   - **Important:** Play Billing payouts go to the **Play Console owner's (your friend's) Merchant bank account**.
   - If your friend has a Merchant Profile and wants to enable optional tips, go to **Monetize > Products > In-app products** and create **one-time products** with these exact IDs:
     - `tip_small` (e.g. ₹50)
     - `tip_medium` (e.g. ₹100)
     - `tip_large` (e.g. ₹250)
     - `tip_generous` (e.g. ₹500)
   - If you **do not** create those products yet (or your friend doesn't have a Merchant Profile), the app automatically hides the tip purchase buttons and stays 100% free with zero broken UI.

---

## 3. Store Listing Metadata

### App Title
**BKM Player - Video & Music**

### Short Description (Max 80 characters)
> Fast, ad-free HD video and music player with gesture controls and vault.

### Full Description (Max 4,000 characters)
```text
BKM Player is a modern, high-performance video and music player designed for Android. Built with advanced hardware acceleration, libmpv, and Google Jetpack Media3, BKM Player delivers smooth, battery-efficient playback for all your favorite media formats — 100% free, ad-free, and with no subscriptions.

🎬 Advanced Video Playback
• Supports all popular video formats: MP4, MKV, AVI, MOV, FLV, TS, WebM, and more.
• Hardware-Accelerated Decoding: Powered by libmpv and FFmpeg for crystal-clear 4K and Full HD video without lag.
• Intuitive Gesture Controls: Swipe to adjust brightness (left side), volume (right side), and seek forward/backward with precision.
• Picture-in-Picture (PiP) Mode: Continue watching your videos in a floating window while multitasking across other apps.
• Background Video Playback: Listen to video audio in the background with convenient lock screen and notification controls.
• Aspect Ratio & Zoom: Pinch-to-zoom, crop, stretch, 16:9, and 4:3 modes with chapter & subtitle support.
• High Refresh Rate & Pitch Black OLED Mode: Enjoy 90Hz / 120Hz scrolling and pure #000000 OLED dark theme.

🎵 Powerful Music Player
• Plays MP3, FLAC, ALAC, AAC, WAV, OGG, M4A, and high-resolution audio formats.
• Sleek Now-Playing Screen: Dynamic color palette theming based on album artwork.
• Background Audio Service: Seamless playback with lock screen media controls and notification actions.
• Queue & Folder Management: Shuffle, repeat, rename files, and organize your favorite songs effortlessly.

🔒 Secure Privacy Vault
• Hide personal videos and audio in a password-protected local vault.
• PIN Protection: Secured using industry-standard salted SHA-256 hashing.
• 100% Offline & Private: Your hidden media stays strictly on your device and is never uploaded anywhere.

⚡ Clean, Modern & Lightweight
• Pure Material 3 Design with Dark, Light & Pitch Black OLED theme support.
• Zero Tracking, Zero Ads, No Subscriptions: We value your privacy and do not collect any personal data.

Download BKM Player today and enjoy a fast, private video and audio playback experience on Android!
```

---

## 4. Google Play Console Policy Declarations

### A. Privacy Policy URL
Enable **GitHub Pages** on `bijoykumarmistri/MediaPlayer` (main branch / root) so `privacy_policy.html` is publicly accessible at:
`https://bijoykumarmistri.github.io/MediaPlayer/privacy_policy.html`
Verify it loads with HTTP 200 in an incognito browser, then paste that URL in **App content > Privacy Policy**.

### B. App Access
* **All functionality in my app is available without any access restrictions** (no login or subscription required).

### C. Data Safety Form
1. **Does your app collect or share any of the required user data types?** -> **No**
2. **Is all user data collected by your app encrypted in transit?** -> **Not applicable**
3. **Do you provide a way for users to request that their data is deleted?** -> **Yes** (Uninstalling or clearing app data deletes all local preferences, cache, and vault data).

### D. Photo and Video Permissions (`READ_MEDIA_VIDEO`)
Google Play requires a declaration for `READ_MEDIA_VIDEO`:
* **Core functionality justification:** BKM Player is a dedicated offline local video and audio media player. Broad access to device videos (`READ_MEDIA_VIDEO`) is essential for the app's primary purpose: discovering, indexing by folder, displaying thumbnails, renaming, and playing all user video files stored across internal storage and SD cards.
* **Screen recording:** Attach a short video showing the Videos tab listing local device folders/videos and playing a video.

### E. Foreground Service Declaration (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`)
* **Foreground Service Type:** `Media Playback`
* **User-Facing Description:**
```text
BKM Player is a local audio and video media player. The app uses FOREGROUND_SERVICE_MEDIA_PLAYBACK to maintain uninterrupted playback of user audio and video files when the screen is turned off or when the user navigates away from the app. A persistent system notification with playback controls (play, pause, skip, and media progress) is provided to ensure the user has continuous control and awareness of the active media session.
```
* **Video Demonstration Link:** Show starting audio/video playback, pressing Home or locking the screen, and controlling playback via the media notification.

### F. App Category, Content & Ads
* **Category:** Video Players & Editors
* **Ads:** **No, my app does not contain ads**
* **In-app purchases:** Answer **Yes** only if you configure the optional one-time Tip products (`tip_small`, etc.) in Play Console; otherwise keep optional/free.
* **Target Audience:** Ages 13+ (or 18+)

---

## 5. How to Build the Signed Play Store App Bundle (`.aab`)

> **IMPORTANT:** Google Play **rejects** debug-signed builds. You MUST create a real release `.jks` keystore and keep a permanent backup of it (if lost, you can never update your app on Google Play).

### Step 1: Create Your Release Keystore
1. In Android Studio, click **Build > Generate Signed Bundle / APK...**
2. Select **Android App Bundle** -> **Next**.
3. Click **Create new...** to create a keystore (e.g., `C:\Users\bijoy\bkm-release-key.jks` — keep it outside git or ensure `.jks` is gitignored).
4. Set a strong keystore password, key alias (e.g., `bkmplayer`), and key password.

### Step 2: Configure `keystore.properties` (for Gradle builds)
1. Copy `keystore.properties.example` to `keystore.properties` in the project root (already gitignored).
2. Fill in your keystore path, passwords, and alias:
   ```properties
   storeFile=C:/Users/bijoy/bkm-release-key.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=bkmplayer
   keyPassword=YOUR_KEY_PASSWORD
   ```

### Step 3: Build the Play Store `.aab`
Run:
```bash
./gradlew :app:bundlePlaystoreRelease
```
Upload the generated file from:
`app/build/outputs/bundle/playstoreRelease/BKMPlayer-v1.9-playstore-release.aab`

*(To build the sideloadable GitHub APK with direct UPI & GitHub auto-updater, run `./gradlew :app:assembleGithubRelease`.)*

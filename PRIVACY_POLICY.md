# Privacy Policy for BKM Player

**Last Updated:** November 2026

**BKM Player** ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains our practices regarding data collection, usage, and disclosure when you use BKM Player.

---

### 1. Zero Personal Data Collection
BKM Player is designed as an offline-first local media player. **We do not collect, store, sell, or transmit any personal information, usage analytics, or identity data to external servers or third parties.**

---

### 2. Permissions and Their Usage
To provide media playback functionality, BKM Player requests specific Android permissions:

* **Photos and Videos (`READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE`):**
  Used exclusively to index, display, and play video files stored locally on your device.
* **Audio and Music (`READ_MEDIA_AUDIO`):**
  Used exclusively to scan, display, and play audio files and music tracks stored locally on your device.
* **Foreground Service (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`):**
  Used to keep audio and video playback playing continuously when the screen is turned off or when you navigate to other apps. A media control notification is shown while playback is active.
* **Notifications (`POST_NOTIFICATIONS`):**
  Required on Android 13 (API 33) and newer to show playback controls (play, pause, skip, artwork) in your notification shade and lock screen.
* **Network Access (`INTERNET`, `ACCESS_NETWORK_STATE`):**
  Used solely if you choose to stream an online media URL or check for official app updates. The app does not track your network identity or browse history.
* **Modify Audio Settings (`MODIFY_AUDIO_SETTINGS`):**
  Used to adjust volume, route audio to speakers/headphones, and handle audio focus (e.g. pausing media during incoming phone calls).

---

### 3. Privacy Vault (Hidden Media)
BKM Player includes an optional local Privacy Vault feature:
* Media moved to the Vault is stored securely in your device's private app storage.
* Your PIN and security answers are hashed using cryptographically secure algorithms (SHA-256 with random salt) and stored locally on your device.
* **We do not have access to your PIN, security answers, or vaulted files.** If you uninstall the app or clear app data, locally vaulted items may be removed by the Android operating system.

---

### 4. Optional Support & In-App Purchases
BKM Player is **100% free, ad-free, and does not require any subscriptions or paid features**.
* On Google Play, optional one-time developer tips (when enabled) are processed securely by **Google Play Billing**.
* BKM Player does not collect, process, or store your credit card, UPI, or billing details. All transactions are governed by [Google Play's Privacy Policy](https://policies.google.com/privacy).

---

### 5. Third-Party Libraries
BKM Player utilizes open-source libraries to ensure high-performance playback:
* **Jetpack Media3 & ExoPlayer:** Standard Android media framework developed by Google.
* **libmpv & FFmpeg:** High-performance hardware-accelerated video/audio decoding.
* **Coil:** Efficient on-device image and thumbnail caching.
* **Google Play Billing & App Update Libraries (Play Store build only):** Used solely for optional one-time tips and official Google Play update checks.

None of these libraries are used by BKM Player to collect or sell personal user data.

---

### 6. Children's Privacy
BKM Player does not target or knowingly collect data from children under the age of 13. The app is a standalone local media player with no user accounts or social interactions.

---

### 7. Changes to This Privacy Policy
We may update this Privacy Policy from time to time. Any revisions will be reflected with an updated "Last Updated" date.

---

### 8. Contact Us
If you have any questions or suggestions regarding this Privacy Policy, please contact:
* **Developer:** Bijoy Kumar Mistri
* **GitHub Profile:** [https://github.com/bijoykumarmistri](https://github.com/bijoykumarmistri)
* **Project & Support:** [https://github.com/bijoykumarmistri/MediaPlayer](https://github.com/bijoykumarmistri/MediaPlayer)

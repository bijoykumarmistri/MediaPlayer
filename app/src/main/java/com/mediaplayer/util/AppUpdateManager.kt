package com.mediaplayer.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val downloadUrl: String?,
    val isUpdateAvailable: Boolean,
    val currentVersionName: String,
)

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    const val GITHUB_REPO_OWNER = "bijoykumarmistri"
    const val GITHUB_REPO_NAME = "MediaPlayer"
    const val GITHUB_RELEASES_WEB_URL = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases"
    private const val GITHUB_API_LATEST_RELEASE_URL =
        "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
    const val PLAY_STORE_MARKET_URI = "market://details?id=com.bkm.player"
    const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.bkm.player"

    /**
     * Gets the currently installed app version name (e.g. "1.1").
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error reading package info", e)
            "1.0"
        }
    }

    /**
     * Checks GitHub for the latest release.
     */
    suspend fun checkForUpdates(context: Context): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersionName(context)
            val url = URL(GITHUB_API_LATEST_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MediaPlayer-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases published yet on this repository
                return@withContext Result.success(
                    AppReleaseInfo(
                        tagName = "v$currentVersion",
                        releaseTitle = "No remote releases yet",
                        releaseNotes = "No published releases found on GitHub yet.",
                        releaseUrl = GITHUB_RELEASES_WEB_URL,
                        downloadUrl = null,
                        isUpdateAvailable = false,
                        currentVersionName = currentVersion
                    )
                )
            } else if ((responseCode !in 200..299)) {
                return@withContext Result.failure(
                    Exception("GitHub API error: HTTP $responseCode")
                )
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val responseString = reader.use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseString)
            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "Bug fixes and performance enhancements.")
            val htmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL)

            val updateAvailable = isNewerVersion(remoteVersion = tagName, currentVersion = currentVersion)

            val releaseInfo = AppReleaseInfo(
                tagName = tagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                releaseUrl = htmlUrl,
                downloadUrl = PLAY_STORE_MARKET_URI,
                isUpdateAvailable = updateAvailable,
                currentVersionName = currentVersion
            )

            Result.success(releaseInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates from GitHub", e)
            Result.failure(e)
        }
    }

    /**
     * Compares semantic versions (e.g. "v1.2.0" vs "1.1").
     * Returns true if remote is strictly newer than current.
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val cleanRemote = remoteVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V").substringBefore("-")

            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            if (remoteParts.isEmpty() || currentParts.isEmpty()) {
                return cleanRemote != cleanCurrent && cleanRemote.isNotBlank()
            }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Error comparing versions '$remoteVersion' and '$currentVersion'", e)
            return false
        }
    }

    /**
     * Opens the app's Google Play Store listing directly.
     * Falls back to the web browser if Play Store is not installed.
     */
    fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, PLAY_STORE_MARKET_URI.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Play Store app not available on this device; fall back to browser
            openUrl(context, PLAY_STORE_WEB_URL)
        }
    }

    /**
     * Opens a web URL in external browser.
     */
    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
        }
    }
}

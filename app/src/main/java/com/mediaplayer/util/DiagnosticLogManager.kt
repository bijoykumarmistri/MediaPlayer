package com.mediaplayer.util

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.MediaCodecList
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Collects device diagnostics, recent playback errors, last uncaught crash trace,
 * process exit reasons (API 30+), and app-scoped logcat output into a shareable report.
 */
object DiagnosticLogManager {

    private const val TAG = "DiagnosticLogManager"
    private const val DIAGNOSTICS_DIR = "diagnostics"
    private const val CRASH_FILE_NAME = "last_crash.txt"
    private const val MAX_EVENTS = 250
    private const val MAX_LOGCAT_LINES = 1500
    private const val MAX_ANR_TRACE_CHARS = 12_000

    private val crashHandlerInstalled = AtomicBoolean(false)
    private val recentEvents = ArrayDeque<String>(MAX_EVENTS)
    private val eventLock = Any()

    private val sensitiveQueryRegex = Regex(
        """([?&](?:token|auth|key|apikey|api_key|sig|signature|password|pass|secret|cookie|session)=)([^&\s]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val userFilePathRegex = Regex("""(/storage/emulated/\d+/[^\s"'\]]+)""")

    fun installCrashHandler(application: Application) {
        if (!crashHandlerInstalled.compareAndSet(false, true)) return
        val appContext = application.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                persistCrashTrace(appContext, thread, throwable)
            } catch (_: Throwable) {
                // Never interfere with default crash handling
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun recordEvent(tag: String, message: String) {
        val timestamp = formatNow()
        val line = "$timestamp [$tag] ${sanitizeLine(message)}"
        synchronized(eventLock) {
            if (recentEvents.size >= MAX_EVENTS) {
                recentEvents.removeFirst()
            }
            recentEvents.addLast(line)
        }
    }

    fun hasSavedCrashReport(context: Context): Boolean {
        val file = File(File(context.filesDir, DIAGNOSTICS_DIR), CRASH_FILE_NAME)
        return (file.exists()) && (file.length() > 0L)
    }

    suspend fun exportDiagnosticReport(context: Context): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheDiagDir = File(context.cacheDir, DIAGNOSTICS_DIR).apply { mkdirs() }
            // Keep only the latest exported report in cache
            cacheDiagDir.listFiles()?.forEach { old ->
                if (old.isFile && old.name.startsWith("bkm_player_logs_")) {
                    old.delete()
                }
            }

            val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(cacheDiagDir, "bkm_player_logs_$fileStamp.txt")
            val reportText = buildDiagnosticReport(context.applicationContext)
            outFile.writeText(reportText, Charsets.UTF_8)
            outFile
        }
    }

    fun shareDiagnosticFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BKM Player Debug Logs (${Build.MANUFACTURER} ${Build.MODEL})")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached BKM Player diagnostic report from ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})."
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("BKM Player Debug Logs", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Debug Logs").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    internal fun sanitizeLine(raw: String): String {
        var sanitized = sensitiveQueryRegex.replace(raw) { match ->
            "${match.groupValues[1]}<REDACTED>"
        }
        sanitized = userFilePathRegex.replace(sanitized) { match ->
            val path = match.groupValues[1]
            val ext = path.substringAfterLast('.', "")
            if (ext.isNotEmpty() && ext.length <= 6 && !ext.contains('/')) {
                "<media_path>.$ext"
            } else {
                "<media_path>"
            }
        }
        return sanitized
    }

    private fun persistCrashTrace(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, DIAGNOSTICS_DIR).apply { mkdirs() }
        val crashFile = File(dir, CRASH_FILE_NAME)
        val (versionName, versionCode) = getVersionInfo(context)
        val content = buildString {
            appendLine("Timestamp: ${formatNow()}")
            appendLine("Version: $versionName ($versionCode)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name} (priority=${thread.priority})")
            appendLine()
            appendLine(Log.getStackTraceString(throwable))
        }
        crashFile.writeText(content, Charsets.UTF_8)
    }

    private fun buildDiagnosticReport(context: Context): String {
        val (versionName, versionCode) = getVersionInfo(context)
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val settings = AppSettingsManager(context)

        return buildString(capacity = 32 * 1024) {
            appendLine("==================================================")
            appendLine("BKM PLAYER DIAGNOSTIC REPORT")
            appendLine("==================================================")
            appendLine("Generated At : ${formatNow()}")
            appendLine("Package      : ${context.packageName}")
            appendLine("Version      : $versionName (code=$versionCode, debug=$isDebug)")
            appendLine()

            appendLine("--- DEVICE & DISPLAY ---")
            appendLine("Manufacturer : ${Build.MANUFACTURER}")
            appendLine("Brand / Model: ${Build.BRAND} / ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Hardware     : ${Build.HARDWARE} / Board: ${Build.BOARD}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appendLine("SoC          : ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}")
            }
            appendLine("Android OS   : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}, Patch=${Build.VERSION.SECURITY_PATCH})")
            appendLine("ABIs         : ${Build.SUPPORTED_ABIS.joinToString()}")
            appendMemoryInfo(context, this)
            appendDisplayInfo(context, this)
            appendLine()

            appendLine("--- PLAYBACK & UI SETTINGS ---")
            appendLine("Decoder Mode       : ${settings.defaultDecoderMode}")
            appendLine("High Refresh Rate  : ${settings.highRefreshRateEnabled}")
            appendLine("Auto Rotate Video  : ${settings.autoRotateVideo}")
            appendLine("Auto Play Next     : ${settings.autoPlayNextVideo}")
            appendLine("Default Resize     : ${settings.defaultResizeMode}")
            appendLine("Pitch Black Theme  : ${settings.pitchBlackTheme}")
            appendLine("Fast Scrub         : ${settings.fastScrubEnabled}")
            appendLine("Swipe Gestures     : ${settings.swipeGesturesEnabled}")
            appendLine()

            appendLine("--- DECODER CAPABILITIES ---")
            appendDecoderSummary(this)
            appendLine()

            val crashFile = File(File(context.filesDir, DIAGNOSTICS_DIR), CRASH_FILE_NAME)
            if (crashFile.exists() && crashFile.length() > 0L) {
                appendLine("--- LAST SAVED CRASH TRACE ---")
                runCatching {
                    appendLine(sanitizeLine(crashFile.readText(Charsets.UTF_8).trimEnd()))
                }.onFailure {
                    appendLine("Failed reading crash file: ${it.message}")
                }
                appendLine()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                appendProcessExitReasons(context, this)
            }

            val eventsSnapshot = synchronized(eventLock) { recentEvents.toList() }
            if (eventsSnapshot.isNotEmpty()) {
                appendLine("--- RECENT APP DIAGNOSTIC EVENTS (${eventsSnapshot.size}) ---")
                eventsSnapshot.forEach { appendLine(it) }
                appendLine()
            }

            appendLine("--- APP LOGCAT (LAST $MAX_LOGCAT_LINES LINES) ---")
            appendLogcatSnapshot(this)
        }
    }

    private fun appendMemoryInfo(context: Context, out: StringBuilder) {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalMb = memInfo.totalMem / (1024L * 1024L)
            val availMb = memInfo.availMem / (1024L * 1024L)
            val maxHeapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L)
            out.appendLine("RAM (Avail/Total): ${availMb}MB / ${totalMb}MB (LowMem=${memInfo.lowMemory}, AppMaxHeap=${maxHeapMb}MB)")
        }
    }

    private fun appendDisplayInfo(context: Context, out: StringBuilder) {
        runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return
            val currentMode = display.mode
            val modesSummary = display.supportedModes
                .asSequence()
                .map { "${it.physicalWidth}x${it.physicalHeight}@${"%.1f".format(Locale.US, it.refreshRate)}Hz" }
                .distinct()
                .joinToString(", ")
            out.appendLine("Display Mode : ${currentMode.physicalWidth}x${currentMode.physicalHeight} @ ${"%.1f".format(Locale.US, currentMode.refreshRate)}Hz")
            out.appendLine("Supported    : $modesSummary")
        }
    }

    private fun appendDecoderSummary(out: StringBuilder) {
        runCatching {
            val watchedTypes = listOf(
                "video/avc",
                "video/hevc",
                "video/x-vnd.on2.vp9",
                "video/av01",
                "audio/opus",
                "audio/mp4a-latm",
                "audio/ac3",
                "audio/eac3"
            )
            val codecInfos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            for (mime in watchedTypes) {
                val decoders = codecInfos.filter { info ->
                    !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
                }
                val formatted = if (decoders.isEmpty()) {
                    "none"
                } else {
                    decoders.joinToString(", ") { info ->
                        val hwTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (info.isHardwareAccelerated) "HW" else "SW"
                        } else {
                            "?"
                        }
                        "${info.name}($hwTag)"
                    }
                }
                out.appendLine("$mime: $formatted")
            }
        }.onFailure {
            out.appendLine("Unable to query MediaCodecList: ${it.message}")
        }
    }

    private fun appendProcessExitReasons(context: Context, out: StringBuilder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val exits = am.getHistoricalProcessExitReasons(context.packageName, 0, 5)
            if (exits.isEmpty()) return
            out.appendLine("--- HISTORICAL PROCESS EXITS ---")
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (info in exits) {
                val reasonLabel = exitReasonName(info.reason)
                val ts = fmt.format(Date(info.timestamp))
                out.appendLine("[$ts] reason=$reasonLabel status=${info.status} pss=${info.pss}KB rss=${info.rss}KB desc=${info.description ?: "-"}")
                if (info.reason == ApplicationExitInfo.REASON_ANR || info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                    runCatching {
                        info.traceInputStream?.bufferedReader()?.use { reader ->
                            val trace = reader.readText()
                            if (trace.isNotBlank()) {
                                out.appendLine("  Trace excerpt:")
                                out.appendLine(sanitizeLine(trace.take(MAX_ANR_TRACE_CHARS).trimEnd()))
                            }
                        }
                    }
                }
            }
            out.appendLine()
        }
    }

    private fun appendLogcatSnapshot(out: StringBuilder) {
        var process: Process? = null
        try {
            val pid = android.os.Process.myPid()
            process = ProcessBuilder(
                "logcat",
                "-d",
                "-v", "threadtime",
                "-t", MAX_LOGCAT_LINES.toString(),
                "--pid=$pid"
            )
                .redirectErrorStream(true)
                .start()

            val lines = mutableListOf<String>()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    lines.add(sanitizeLine(line))
                    line = reader.readLine()
                }
            }
            process.waitFor(3, TimeUnit.SECONDS)

            if (lines.isEmpty()) {
                out.appendLine("(No logcat lines captured for pid=$pid)")
            } else {
                lines.forEach { out.appendLine(it) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed capturing logcat", t)
            out.appendLine("Logcat capture failed: ${t.message}")
        } finally {
            runCatching { process?.destroy() }
        }
    }

    private fun exitReasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        else -> "OTHER($reason)"
    }

    private fun getVersionInfo(context: Context): Pair<String, Long> {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            (info.versionName ?: "1.0") to code
        } catch (_: Exception) {
            "1.0" to 0L
        }
    }

    private fun formatNow(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}

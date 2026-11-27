package com.mediaplayer.util

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.Display

import androidx.core.content.ContextCompat

/**
 * Manages display refresh rate matching for high refresh rate screens (90Hz, 120Hz, 144Hz, etc.)
 * Dynamically adapts to system display configuration changes and battery saver states.
 */
class HighRefreshRateManager(private val activity: Activity) {

    companion object {
        private const val TAG = "HighRefreshRateManager"
    }

    private val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val powerManager = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            val currentDisplayId = getTargetDisplay()?.displayId ?: Display.DEFAULT_DISPLAY
            if ((displayId == currentDisplayId) || (displayId == Display.DEFAULT_DISPLAY)) {
                applyOptimalRefreshRate()
            }
        }
    }

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED == intent?.action) {
                applyOptimalRefreshRate()
            }
        }
    }

    private var isListening = false

    fun start() {
        if (!isListening) {
            try {
                displayManager?.registerDisplayListener(displayListener, null)
                val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                ContextCompat.registerReceiver(activity, powerSaveReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                isListening = true
            } catch (e: Exception) {
                Log.w(TAG, "Error registering refresh rate listeners: ${e.message}")
            }
        }
        // Asynchronously post optimal refresh rate so window creation and cold start are not delayed
        activity.window?.decorView?.post {
            applyOptimalRefreshRate()
        }
    }

    fun stop() {
        if (isListening) {
            try {
                displayManager?.unregisterDisplayListener(displayListener)
            } catch (_: Exception) {}
            try {
                activity.unregisterReceiver(powerSaveReceiver)
            } catch (_: Exception) {}
            isListening = false
        }
    }

    fun applyOptimalRefreshRate() {
        try {
            if (activity.isFinishing || activity.isDestroyed) return
            val window = activity.window ?: return
            val layoutParams = window.attributes ?: return

            val highRefreshEnabled = AppSettingsManager(activity).highRefreshRateEnabled
            val isPowerSave = powerManager?.isPowerSaveMode == true
            if (!highRefreshEnabled || isPowerSave) {
                resetToSystemDefaultRefreshRate(window, layoutParams)
                return
            }

            val targetDisplay = getTargetDisplay() ?: return
            val currentMode = targetDisplay.mode ?: return
            val supportedModes = targetDisplay.supportedModes ?: return

            val (curW, curH) = minOf(currentMode.physicalWidth, currentMode.physicalHeight) to
                    maxOf(currentMode.physicalWidth, currentMode.physicalHeight)

            // Find all modes matching current screen resolution
            val matchingModes = supportedModes.filter { mode ->
                val (mW, mH) = minOf(mode.physicalWidth, mode.physicalHeight) to
                        maxOf(mode.physicalWidth, mode.physicalHeight)
                (mW == curW) && (mH == curH)
            }

            val distinctRates = matchingModes.map { kotlin.math.round(it.refreshRate) }.distinct()
            val highestMode = matchingModes.maxByOrNull { it.refreshRate } ?: currentMode
            val maxFps = highestMode.refreshRate

            // On 60Hz-only displays with a single refresh rate, leave system defaults untouched
            if (distinctRates.size <= 1 && maxFps <= 60.5f) {
                resetToSystemDefaultRefreshRate(window, layoutParams)
                return
            }

            var changed = false

            // 1. Set preferred display mode ID (essential for Motorola, Xiaomi, etc. to unlock high Hz)
            if ((highestMode.modeId > 0) && (layoutParams.preferredDisplayModeId != highestMode.modeId)) {
                layoutParams.preferredDisplayModeId = highestMode.modeId
                changed = true
            }

            // 2. Set preferred refresh rates for API 30+ (Android 11+)
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && (layoutParams.preferredRefreshRate != maxFps)) {
                layoutParams.preferredRefreshRate = maxFps
                changed = true
            }

            // 3. Android 15+ Adaptive Refresh Rate optimizations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                try {
                    @Suppress("UsePropertyAccessSyntax")
                    window.setFrameRateBoostOnTouchEnabled(true)
                    @Suppress("UsePropertyAccessSyntax")
                    window.setFrameRatePowerSavingsBalanced(true)
                } catch (_: Throwable) {}
            }

            if (changed) {
                window.attributes = layoutParams
                Log.d(TAG, "Applied display refresh rate: ${maxFps}Hz (mode ${highestMode.modeId})")
                DiagnosticLogManager.recordEvent(TAG, "Applied ${maxFps}Hz (mode ${highestMode.modeId})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply optimal refresh rate", e)
            DiagnosticLogManager.recordEvent(TAG, "Failed applying refresh rate: ${e.message}")
        }
    }

    private fun resetToSystemDefaultRefreshRate(
        window: android.view.Window,
        layoutParams: android.view.WindowManager.LayoutParams
    ) {
        var changed = false
        if (layoutParams.preferredDisplayModeId != 0) {
            layoutParams.preferredDisplayModeId = 0
            changed = true
        }
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && (layoutParams.preferredRefreshRate != 0f)) {
            layoutParams.preferredRefreshRate = 0f
            changed = true
        }
        if (changed) {
            window.attributes = layoutParams
        }
    }

    private fun getTargetDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display ?: displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }
    }
}

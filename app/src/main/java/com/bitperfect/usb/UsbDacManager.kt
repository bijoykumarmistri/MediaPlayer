package com.bitperfect.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Manages Android USB Host discovery, runtime permission prompts, raw file descriptor
 * extraction, and safe USB unplug handling without crashing native libusb threads.
 */
class UsbDacManager(
    private val context: Context,
    private val onDacReady: (fileDescriptor: Int, deviceName: String) -> Unit,
    private val onDacDetached: () -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    @Volatile
    private var receiverRegistered = false

    @Volatile
    private var activeDevice: UsbDevice? = null

    @Volatile
    private var activeConnection: UsbDeviceConnection? = null

    private val lock = Any()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = intent.getUsbDeviceExtra()
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted && device != null) {
                        openDevice(device)
                    } else {
                        onError("USB permission denied for ${device?.productName ?: "DAC"}")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getUsbDeviceExtra()
                    if (device != null && isUsbAudioDevice(device)) {
                        onStatus("USB Audio device attached: ${formatDeviceName(device)}")
                        requestPermissionOrOpen(device)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getUsbDeviceExtra()
                    val isCurrent = synchronized(lock) {
                        device != null && activeDevice?.deviceId == device.deviceId
                    }
                    if (isCurrent) {
                        Log.i(TAG, "Active USB DAC detached: ${formatDeviceName(device!!)}")
                        // 1. Notify native engine first so libusb cancels/drains URBs before fd closes
                        onDacDetached()
                        // 2. Now safely close the Android UsbDeviceConnection
                        closeConnectionOnly()
                    }
                }
            }
        }
    }

    fun register() {
        synchronized(lock) {
            if (receiverRegistered) return
            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    fun scanAndConnect() {
        val audioDevice = usbManager.deviceList.values.firstOrNull { isUsbAudioDevice(it) }
        if (audioDevice == null) {
            onStatus("No USB Audio Class DAC detected")
            return
        }
        requestPermissionOrOpen(audioDevice)
    }

    fun closeConnectionOnly() {
        synchronized(lock) {
            try {
                activeConnection?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Exception closing UsbDeviceConnection: ${e.message}")
            } finally {
                activeConnection = null
                activeDevice = null
            }
        }
    }

    fun unregisterAndClose() {
        synchronized(lock) {
            if (receiverRegistered) {
                try {
                    context.unregisterReceiver(usbReceiver)
                } catch (_: IllegalArgumentException) {
                }
                receiverRegistered = false
            }
        }
        closeConnectionOnly()
    }

    /**
     * Detects both pure USB Audio Class devices (deviceClass == USB_CLASS_AUDIO) and
     * composite / IAD UAC 2.0 DACs that expose an Audio Control interface (class 1, subclass 1).
     */
    fun isUsbAudioDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO) {
            return true
        }
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    private fun requestPermissionOrOpen(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            onStatus("Requesting USB permission for ${formatDeviceName(device)}...")
            // Explicit package intent is required for FLAG_MUTABLE on Android 14+ (API 34)
            val permIntent = Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, permIntent, flags)
            usbManager.requestPermission(device, pendingIntent)
        }
    }

    private fun openDevice(device: UsbDevice) {
        synchronized(lock) {
            if (activeDevice?.deviceId == device.deviceId && activeConnection != null) {
                // Already open
                val existingFd = activeConnection!!.fileDescriptor
                if (existingFd >= 0) {
                    onDacReady(existingFd, formatDeviceName(device))
                    return
                }
            }

            closeConnectionOnly()

            val conn = usbManager.openDevice(device)
            if (conn == null) {
                onError("UsbManager.openDevice() returned null for ${formatDeviceName(device)}")
                return
            }

            val fd = conn.fileDescriptor
            if (fd < 0) {
                conn.close()
                onError("Invalid file descriptor from UsbDeviceConnection")
                return
            }

            activeDevice = device
            activeConnection = conn
            Log.i(TAG, "Opened USB DAC ${formatDeviceName(device)} (fd=$fd)")
            onDacReady(fd, formatDeviceName(device))
        }
    }

    private fun formatDeviceName(device: UsbDevice): String {
        val mfg = device.manufacturerName?.trim().orEmpty()
        val prod = device.productName?.trim().orEmpty()
        return listOf(mfg, prod).filter { it.isNotEmpty() }.joinToString(" ")
            .ifEmpty { "USB DAC (%04X:%04X)".format(device.vendorId, device.productId) }
    }

    @Suppress("DEPRECATION")
    private fun Intent.getUsbDeviceExtra(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    companion object {
        private const val TAG = "UsbDacManager"
        private const val ACTION_USB_PERMISSION = "com.bitperfect.usb.ACTION_USB_PERMISSION"
    }
}

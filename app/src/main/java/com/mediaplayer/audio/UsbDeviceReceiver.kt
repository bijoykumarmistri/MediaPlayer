package com.mediaplayer.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.util.Log
import com.mediaplayer.MediaPlayerApplication

class UsbDeviceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("UsbDeviceReceiver", "Received USB intent action: $action")

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED,
            UsbManager.ACTION_USB_DEVICE_DETACHED,
            UsbBitPerfectAudioController.ACTION_USB_PERMISSION -> {
                try {
                    MediaPlayerApplication.instance.usbAudioController.scanAudioDevices()
                } catch (e: Exception) {
                    Log.e("UsbDeviceReceiver", "Error notifying USB audio controller", e)
                }
            }
        }
    }
}

package com.mediaplayer.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bitperfect.usb.BitPerfectEngine
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mediaplayer.audio.UsbAudioTelemetry
import com.mediaplayer.audio.UsbBitPerfectAudioController
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurface
import com.mediaplayer.ui.theme.OledSurfaceVariant

@Composable
fun UsbDacSettingsDialog(
    telemetry: UsbAudioTelemetry,
    controller: UsbBitPerfectAudioController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, OledBorder, RoundedCornerShape(28.dp)),
            color = OledSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Usb,
                                contentDescription = null,
                                tint = AudiophileCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "USB-C Bit-Perfect DAC",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Audiophile Hardware Bypass",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9EAE)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bit Perfect Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bit-Perfect Output Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bypass Android OS audio mixer, resampler & DSP processing for bit-accurate output.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0B0C0)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = telemetry.isExclusiveRequested,
                            onCheckedChange = { controller.setExclusiveBitPerfect(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AudiophileGold,
                                checkedTrackColor = Color(0x66FFD54F),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0x33888888)
                            )
                        )
                    }
                }

                if (telemetry.isExclusiveRequested) {
                    Spacer(modifier = Modifier.height(14.dp))
                    // Backend Engine Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OledSurfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "BIT-PERFECT ENGINE DRIVER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AudiophileGold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val isLibusb = controller.engine.backendMode == BitPerfectEngine.BackendMode.EXCLUSIVE_USB_LIBUSB
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLibusb) Color(0x1F00E5FF) else Color.Transparent)
                                    .clickable { controller.setBackendMode(BitPerfectEngine.BackendMode.EXCLUSIVE_USB_LIBUSB) }
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = isLibusb,
                                    onClick = { controller.setBackendMode(BitPerfectEngine.BackendMode.EXCLUSIVE_USB_LIBUSB) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AudiophileCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Direct USB libusb Core (Isochronous C++)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("Exclusive hardware control. Completely bypasses AudioFlinger, ALSA, and Android mixer.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0C0), fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val isAndroid14 = controller.engine.backendMode == BitPerfectEngine.BackendMode.ANDROID_14_NATIVE_BIT_PERFECT
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAndroid14) Color(0x1F00E5FF) else Color.Transparent)
                                    .clickable { controller.setBackendMode(BitPerfectEngine.BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) }
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = isAndroid14,
                                    onClick = { controller.setBackendMode(BitPerfectEngine.BackendMode.ANDROID_14_NATIVE_BIT_PERFECT) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AudiophileCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Android 14+ Native Bit-Perfect", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("AudioMixerAttributes HAL bypass via OS AudioTrack.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0C0), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Telemetry Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (telemetry.isBitPerfectActive) Color(0x1AFFD54F) else OledSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (telemetry.isBitPerfectActive) {
                        androidx.compose.foundation.BorderStroke(1.dp, AudiophileGold.copy(alpha = 0.5f))
                    } else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "HARDWARE TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isBitPerfectActive) AudiophileGold else AudiophileCyan,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (telemetry.isBitPerfectActive) Icons.Rounded.CheckCircle else Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = if (telemetry.isBitPerfectActive) AudiophileGreen else AudiophileCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (telemetry.isBitPerfectActive) "ACTIVE" else "CONNECTED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (telemetry.isBitPerfectActive) AudiophileGreen else AudiophileCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(12.dp))

                        TelemetryRow("Connected DAC", telemetry.dacName)
                        TelemetryRow("Mixer Behavior", telemetry.mixerBehavior)
                        TelemetryRow("Source Track", "${telemetry.sourceCodec} • ${telemetry.sourceBitDepth}-bit / ${UsbAudioTelemetry.formatSampleRate(telemetry.sourceSampleRate)}")
                        TelemetryRow("Output Signal", "${telemetry.outputBitDepth}-bit / ${UsbAudioTelemetry.formatSampleRate(telemetry.outputSampleRate)} (${telemetry.sourceChannels} ch)")

                        if (telemetry.supportedSampleRates.isNotEmpty()) {
                            val ratesText = telemetry.supportedSampleRates.joinToString(", ") { UsbAudioTelemetry.formatSampleRate(it) }
                            TelemetryRow("DAC Rates", ratesText)
                        }
                    }
                }

                if (telemetry.isUsbDacConnected && !telemetry.hasUsbPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (context is Activity) {
                                controller.requestUsbPermission(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grant USB Hardware Direct Access",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x15FFFFFF))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color(0xFF9E9EAE),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Android 14+ AudioMixerAttributes automatically matches the DAC hardware clock to the audio source format, delivering bit-perfect playback without resampling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9EAE),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8E8E9E)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

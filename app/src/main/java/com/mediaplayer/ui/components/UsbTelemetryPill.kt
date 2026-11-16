package com.mediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.audio.UsbAudioTelemetry
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold

@Composable
fun UsbTelemetryPill(
    telemetry: UsbAudioTelemetry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isBitPerfect = telemetry.isBitPerfectActive
    val isUsbConnected = telemetry.isUsbDacConnected

    val borderColor = when {
        isBitPerfect -> AudiophileGold
        isUsbConnected -> AudiophileCyan
        else -> Color(0xFF3A3D4D)
    }

    val bgBrush = when {
        isBitPerfect -> Brush.horizontalGradient(
            listOf(
                Color(0x33FFD54F),
                Color(0x1A00E676)
            )
        )
        isUsbConnected -> Brush.horizontalGradient(
            listOf(
                Color(0x2B00E5FF),
                Color(0x1A7C4DFF)
            )
        )
        else -> Brush.horizontalGradient(
            listOf(
                Color(0x22222634),
                Color(0x22161820)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, borderColor.copy(alpha = if (isBitPerfect) 0.9f else 0.7f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Indicator Dot / Icon
            if (isBitPerfect) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AudiophileGold)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (isUsbConnected) {
                Icon(
                    imageVector = Icons.Rounded.Usb,
                    contentDescription = "USB DAC",
                    tint = AudiophileCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            } else {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "Audio Engine",
                    tint = Color(0xFF9E9EAE),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }

            Text(
                text = telemetry.pillText,
                color = when {
                    isBitPerfect -> AudiophileGold
                    isUsbConnected -> AudiophileCyan
                    else -> Color(0xFFD4D4E0)
                },
                fontSize = 11.sp,
                fontWeight = if (isBitPerfect) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )
        }
    }
}

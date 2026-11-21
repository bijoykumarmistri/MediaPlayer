package com.mediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurface
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.util.AppSettingsManager

@Composable
fun VideoCustomizationDialog(
    settingsManager: AppSettingsManager,
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit = {}
) {
    var defaultDecoder by remember { mutableStateOf(settingsManager.defaultDecoderMode) }
    var doubleTapSeek by remember { mutableIntStateOf(settingsManager.doubleTapSeekSeconds) }
    var autoResume by remember { mutableStateOf(settingsManager.autoResume) }
    var defaultResize by remember { mutableStateOf(settingsManager.defaultResizeMode) }

    var swipeGestures by remember { mutableStateOf(settingsManager.swipeGesturesEnabled) }
    var fastScrub by remember { mutableStateOf(settingsManager.fastScrubEnabled) }
    var hold2x by remember { mutableStateOf(settingsManager.hold2xSpeedEnabled) }

    var subtitleSize by remember { mutableIntStateOf(settingsManager.subtitleSize) }
    var subtitleColor by remember { mutableStateOf(settingsManager.subtitleColorHex) }

    var hideShortVideos by remember { mutableStateOf(settingsManager.hideShortVideos) }
    var highRefreshRate by remember { mutableStateOf(settingsManager.highRefreshRateEnabled) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, OledBorder, RoundedCornerShape(26.dp)),
            color = OledSurface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Header: Minimalist & Clean
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Video Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Settings List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: PLAYBACK
                    SettingsGroup(title = "PLAYBACK") {
                        // Decoder Selector
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Decoder",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SegmentedBar(
                                items = listOf("HW+", "HW", "SW"),
                                selectedItem = defaultDecoder,
                                onSelect = { mode ->
                                    defaultDecoder = mode
                                    settingsManager.defaultDecoderMode = mode
                                    onSettingsChanged()
                                }
                            )
                        }

                        GroupDivider()

                        // Double Tap Seek
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Double-Tap Seek",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SegmentedBar(
                                items = listOf("5s", "10s", "15s", "30s"),
                                selectedItem = "${doubleTapSeek}s",
                                onSelect = { item ->
                                    val sec = item.removeSuffix("s").toIntOrNull() ?: 10
                                    doubleTapSeek = sec
                                    settingsManager.doubleTapSeekSeconds = sec
                                    onSettingsChanged()
                                }
                            )
                        }

                        GroupDivider()

                        // Auto-Resume Switch
                        SettingsSwitchRow(
                            title = "Resume Playback",
                            subtitle = "Continue from previous position",
                            checked = autoResume,
                            onCheckedChange = {
                                autoResume = it
                                settingsManager.autoResume = it
                                onSettingsChanged()
                            }
                        )

                        GroupDivider()

                        // Aspect Ratio
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Default Aspect Ratio",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SegmentedBar(
                                items = listOf("Fit", "Fill", "16:9", "4:3", "Stretch"),
                                selectedItem = when (defaultResize) {
                                    "CROP" -> "Fill"
                                    "16:9" -> "16:9"
                                    "4:3" -> "4:3"
                                    "STRETCH" -> "Stretch"
                                    else -> "Fit"
                                },
                                onSelect = { label ->
                                    val mode = when (label) {
                                        "Fill" -> "CROP"
                                        "16:9" -> "16:9"
                                        "4:3" -> "4:3"
                                        "Stretch" -> "STRETCH"
                                        else -> "FIT"
                                    }
                                    defaultResize = mode
                                    settingsManager.defaultResizeMode = mode
                                    onSettingsChanged()
                                }
                            )
                        }
                    }

                    // SECTION 2: GESTURES
                    SettingsGroup(title = "GESTURES") {
                        SettingsSwitchRow(
                            title = "Volume & Brightness",
                            subtitle = "Vertical swipe on screen edges",
                            checked = swipeGestures,
                            onCheckedChange = {
                                swipeGestures = it
                                settingsManager.swipeGesturesEnabled = it
                                onSettingsChanged()
                            }
                        )

                        GroupDivider()

                        SettingsSwitchRow(
                            title = "Horizontal Scrubbing",
                            subtitle = "Swipe sideways to seek",
                            checked = fastScrub,
                            onCheckedChange = {
                                fastScrub = it
                                settingsManager.fastScrubEnabled = it
                                onSettingsChanged()
                            }
                        )

                        GroupDivider()

                        SettingsSwitchRow(
                            title = "Hold for 2x Speed",
                            subtitle = "Touch and hold to speed up",
                            checked = hold2x,
                            onCheckedChange = {
                                hold2x = it
                                settingsManager.hold2xSpeedEnabled = it
                                onSettingsChanged()
                            }
                        )
                    }

                    // SECTION 3: SUBTITLES
                    SettingsGroup(title = "SUBTITLES") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Size",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SegmentedBar(
                                items = listOf("Small", "Normal", "Large", "Huge"),
                                selectedItem = when (subtitleSize) {
                                    36 -> "Small"
                                    60 -> "Large"
                                    72 -> "Huge"
                                    else -> "Normal"
                                },
                                onSelect = { label ->
                                    val size = when (label) {
                                        "Small" -> 36
                                        "Large" -> 60
                                        "Huge" -> 72
                                        else -> 48
                                    }
                                    subtitleSize = size
                                    settingsManager.subtitleSize = size
                                    onSettingsChanged()
                                }
                            )
                        }

                        GroupDivider()

                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text = "Color",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "#FFFFFF" to ("White" to Color.White),
                                    "#FFFF00" to ("Yellow" to Color(0xFFFFEB3B)),
                                    "#00E5FF" to ("Cyan" to AudiophileCyan),
                                    "#00E676" to ("Green" to AudiophileGreen)
                                ).forEach { (hex, pair) ->
                                    val (name, color) = pair
                                    val selected = subtitleColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) color.copy(alpha = 0.18f) else Color(0x0DFFFFFF))
                                            .border(
                                                1.dp,
                                                if (selected) color else Color(0x1AFFFFFF),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                subtitleColor = hex
                                                settingsManager.subtitleColorHex = hex
                                                onSettingsChanged()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) color else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compact Subtitle Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0A0C12))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor = when (subtitleColor.uppercase()) {
                                    "#FFFF00" -> Color(0xFFFFEB3B)
                                    "#00E5FF" -> AudiophileCyan
                                    "#00E676" -> AudiophileGreen
                                    else -> Color.White
                                }
                                Text(
                                    text = "Preview Subtitle Text",
                                    color = textColor,
                                    fontSize = (subtitleSize * 0.32f).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // SECTION 4: LIBRARY & DISPLAY
                    SettingsGroup(title = "LIBRARY & DISPLAY") {
                        SettingsSwitchRow(
                            title = "Hide Short Videos",
                            subtitle = "Exclude clips under 30 seconds",
                            checked = hideShortVideos,
                            onCheckedChange = {
                                hideShortVideos = it
                                settingsManager.hideShortVideos = it
                                onSettingsChanged()
                            }
                        )

                        GroupDivider()

                        SettingsSwitchRow(
                            title = "High Refresh Rate",
                            subtitle = "Optimize smooth display refresh",
                            checked = highRefreshRate,
                            onCheckedChange = {
                                highRefreshRate = it
                                settingsManager.highRefreshRateEnabled = it
                                onSettingsChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OledSurfaceVariant)
                .border(1.dp, OledBorder, RoundedCornerShape(16.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color(0x14FFFFFF)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E9E),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AudiophileCyan,
                uncheckedThumbColor = Color(0xFF8E8E9E),
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun SegmentedBar(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C0E14))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items.forEach { item ->
            val isSelected = item.equals(selectedItem, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AudiophileCyan.copy(alpha = 0.22f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) AudiophileCyan.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AudiophileCyan else Color(0xFF9E9EAE),
                    maxLines = 1
                )
            }
        }
    }
}

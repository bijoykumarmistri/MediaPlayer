package com.mediaplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.util.AppReleaseInfo
import com.mediaplayer.util.AppUpdateManager

@Composable
fun AppUpdateDialog(
    releaseInfo: AppReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13151F),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0x3300E5FF)), RoundedCornerShape(24.dp)),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (releaseInfo.isUpdateAvailable) Color(0x2600E5FF) else Color(0x2600E676)
                            )
                            .border(
                                1.dp,
                                if (releaseInfo.isUpdateAvailable) Color(0x6600E5FF) else Color(0x6600E676),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (releaseInfo.isUpdateAvailable) Icons.Rounded.CloudDownload else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (releaseInfo.isUpdateAvailable) AudiophileCyan else AudiophileGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (releaseInfo.isUpdateAvailable) "Update Available" else "App is Up to Date",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (releaseInfo.isUpdateAvailable) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "v${releaseInfo.currentVersionName}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E9E),
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = AudiophileCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = releaseInfo.tagName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AudiophileCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Text(
                                text = "v${releaseInfo.currentVersionName} • Latest version",
                                fontSize = 12.sp,
                                color = AudiophileGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (releaseInfo.isUpdateAvailable) {
                    // Release title & Changelog card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF181B26))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "WHAT'S NEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AudiophileCyan,
                                letterSpacing = 1.sp
                            )
                            if (releaseInfo.releaseTitle.isNotBlank() && releaseInfo.releaseTitle != releaseInfo.tagName) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = releaseInfo.releaseTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            val scrollState = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    text = releaseInfo.releaseNotes.trim().ifBlank { "Performance improvements and bug fixes." },
                                    fontSize = 12.sp,
                                    color = Color(0xFFC8CCDC),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verified badge row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Verified GitHub Release • Direct APK Install",
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E9E)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF181B26))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "You are running the newest release of Media Player with the latest playback features, codecs, and optimizations.",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A5B5),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (releaseInfo.isUpdateAvailable) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val targetUrl = releaseInfo.downloadUrl ?: releaseInfo.releaseUrl
                            AppUpdateManager.openUrl(context, targetUrl)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AudiophileCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download & Install",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                AppUpdateManager.openUrl(context, releaseInfo.releaseUrl)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Launch,
                                contentDescription = null,
                                tint = Color(0xFFA0A5B5),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GitHub Release",
                                color = Color(0xFFA0A5B5),
                                fontSize = 11.sp
                            )
                        }

                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Later",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x22FFFFFF),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    )
}

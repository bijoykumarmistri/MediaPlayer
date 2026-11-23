package com.mediaplayer.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.data.MediaItemModel
import com.mediaplayer.ui.theme.AudiophileCyan

data class VideoFolderItem(
    val name: String,
    val path: String,
    val videos: List<MediaItemModel>,
    val totalDurationMs: Long,
    val newCount: Int = 0
)

data class MusicFolderItem(
    val name: String,
    val path: String,
    val tracks: List<MediaItemModel>,
    val totalDurationMs: Long,
    val newCount: Int = 0
)

fun formatFolderDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun shareMediaFiles(ctx: Context, items: List<MediaItemModel>) {
    if (items.isEmpty()) return
    val uris = ArrayList<Uri>(items.map { it.uri })
    val mimeType = when {
        items.all { it.isVideo } -> "video/*"
        items.all { !it.isVideo } -> "audio/*"
        else -> "*/*"
    }
    val shareIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uris[0])
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    val chooser = Intent.createChooser(shareIntent, "Share Media Files").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        ctx.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(ctx, "No app available to share files", Toast.LENGTH_SHORT).show()
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Direct Stream"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

@Composable
fun PermissionOnboardingCard(
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1F00E5FF)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = AudiophileCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Media Storage Access", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Grant permission to browse device video and audiophile music, or pick single media files.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0C4))
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onPickFiles,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Pick Media File", color = AudiophileCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0x2200E5FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = AudiophileCyan, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E9E), modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = AudiophileCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Media Files", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FloatingDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    activeColor: Color = AudiophileCyan
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val itemColor = if (selected) activeColor else Color(0xFF9093A0)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = itemColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                letterSpacing = (-0.1).sp
            )
        }
    }
}

@Composable
fun DeleteMediaConfirmDialog(
    items: List<MediaItemModel>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (items.isEmpty()) return
    val isSingle = items.size == 1
    val singleItem = items.first()
    val totalBytes = items.sumOf { it.sizeBytes }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141722),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF453A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isSingle) "Delete Video / Audio?" else "Delete ${items.size} Selected Files?",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1F2E))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSingle) singleItem.title else "${items.size} items",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AudiophileCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatFileSize(totalBytes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA0AAB8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (!isSingle) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.take(4).forEach { item ->
                            Text(
                                text = "• ${item.title}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFFA0A8B8),
                                fontSize = 12.sp
                            )
                        }
                        if (items.size > 4) {
                            Text(
                                text = "... and ${items.size - 4} more files",
                                color = AudiophileCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "This will physically remove the file${if (isSingle) "" else "s"} from your device storage. This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888898),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
            ) {
                Text(
                    text = if (isSingle) "Delete Permanently" else "Delete ${items.size} Files",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = Color(0xFFA0A0B0))
            }
        }
    )
}

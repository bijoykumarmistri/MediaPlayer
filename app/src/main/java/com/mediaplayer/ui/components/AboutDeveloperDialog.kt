package com.mediaplayer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mediaplayer.ui.theme.AudiophileCyan
import com.mediaplayer.ui.theme.AudiophileGold
import com.mediaplayer.ui.theme.AudiophileGreen
import com.mediaplayer.ui.theme.OledBorder
import com.mediaplayer.ui.theme.OledSurfaceVariant
import com.mediaplayer.util.AppReleaseInfo
import com.mediaplayer.util.AppUpdateManager
import kotlinx.coroutines.launch

const val DEVELOPER_NAME = "Bijoy"
const val DEVELOPER_UPI_ID = "bijoymistri367@okaxis"

@Composable
fun AboutDeveloperDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember { AppUpdateManager.getCurrentVersionName(context) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateReleaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isUpiCopied by remember { mutableStateOf(false) }

    fun copyUpiId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Developer UPI ID", DEVELOPER_UPI_ID)
        clipboard.setPrimaryClip(clip)
        isUpiCopied = true
        Toast.makeText(context, "UPI ID copied: $DEVELOPER_UPI_ID", Toast.LENGTH_SHORT).show()
    }

    fun openUpiPayment() {
        try {
            val uri = Uri.parse("upi://pay?pa=$DEVELOPER_UPI_ID&pn=$DEVELOPER_NAME&tn=Support%20Media%20Player%20Development&cu=INR")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyUpiId()
            Toast.makeText(context, "No UPI app detected. UPI ID copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 500.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0A0C14))
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x6600E5FF),
                                Color(0x33FFD54F),
                                Color(0x15FFFFFF)
                            )
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        ) {
            // Ambient luminous aura background at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x2600E5FF),
                                Color(0x10FFD54F),
                                Color.Transparent
                            ),
                            radius = 450f
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar with Floating Glass Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AudiophileGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STUDIO ARCHITECTURE & SUPPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AudiophileGold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FFFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hero Emblem / Monogram
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AudiophileCyan, AudiophileGold)
                                )
                            )
                            .padding(2.5.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF080910)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = AudiophileCyan,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = DEVELOPER_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "LEAD ARCHITECT & AUDIO ENGINEER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AudiophileCyan,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version & Build Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AudiophileGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Media Player • v$currentVersion • Audiophile Edition",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD4D4E0),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. GITHUB RELEASES & UPDATE CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10131E)),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0x3300E5FF))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x2200E5FF))
                                        .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RocketLaunch,
                                        contentDescription = null,
                                        tint = AudiophileCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Official Releases & Updates",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Direct GitHub APK release channel",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA0A5B5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Check for newer builds with updated Jellyfin decoders, audio engines, and privacy vault improvements directly from GitHub.",
                                fontSize = 12.sp,
                                color = Color(0xFFB8BCC8),
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isCheckingUpdate) {
                                            isCheckingUpdate = true
                                            scope.launch {
                                                val result = AppUpdateManager.checkForUpdates(context)
                                                isCheckingUpdate = false
                                                result.onSuccess { info ->
                                                    if (info.isUpdateAvailable) {
                                                        updateReleaseInfo = info
                                                        showUpdateDialog = true
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "You're on the latest version (v${info.currentVersionName})!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }.onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        "Check failed: ${err.localizedMessage ?: "Network error"}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AudiophileCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    if (isCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Checking...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudDownload,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Check Updates", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        AppUpdateManager.openUrl(context, AppUpdateManager.GITHUB_RELEASES_WEB_URL)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AudiophileCyan),
                                    border = BorderStroke(1.dp, AudiophileCyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Launch,
                                        contentDescription = null,
                                        tint = AudiophileCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Releases", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. PATRONAGE & SUPPORT CARD (UPI / COFFEE)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16130D)),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0x55FFD54F))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33FFD54F))
                                        .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = AudiophileGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Support Ongoing Development ☕",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AudiophileGold
                                    )
                                    Text(
                                        text = "100% Ad-Free • Built for Audiophiles",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC0A870)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "If you appreciate the pure bit-perfect sound, cinematic video engine, and privacy folder, support future development with a small donation via UPI!",
                                fontSize = 12.sp,
                                color = Color(0xFFD4C8B0),
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // UPI ID Interactive Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x44000000))
                                    .border(1.dp, Color(0x44FFD54F), RoundedCornerShape(14.dp))
                                    .clickable { copyUpiId() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "UPI VPA ADDRESS (TAP TO COPY)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9E8E6A),
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = DEVELOPER_UPI_ID,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isUpiCopied) Color(0x3300E676) else Color(0x22FFD54F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isUpiCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy UPI ID",
                                            tint = if (isUpiCopied) AudiophileGreen else AudiophileGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Pay Button
                            Button(
                                onClick = { openUpiPayment() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AudiophileGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pay via UPI App (GPay / PhonePe / Paytm)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. CORE ARCHITECTURAL SPECS (LUXURY 2x2 GRID)
                    Text(
                        text = "CORE ARCHITECTURAL HIGHLIGHTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A5B5),
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecTile(
                            icon = Icons.Rounded.GraphicEq,
                            iconTint = AudiophileCyan,
                            title = "Bit-Perfect USB",
                            subtitle = "Direct DAC bypass of Android mixer & DSP",
                            modifier = Modifier.weight(1f)
                        )
                        SpecTile(
                            icon = Icons.Rounded.Tune,
                            iconTint = AudiophileGold,
                            title = "FFmpeg 6.0 Core",
                            subtitle = "Native DSD, FLAC, TrueHD, ALAC & MKV",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpecTile(
                            icon = Icons.Rounded.Movie,
                            iconTint = AudiophileGreen,
                            title = "Cinema Engine",
                            subtitle = "144Hz screen refresh, PiP & hardware AV1",
                            modifier = Modifier.weight(1f)
                        )
                        SpecTile(
                            icon = Icons.Rounded.Speed,
                            iconTint = Color(0xFFB388FF),
                            title = "True OLED M3",
                            subtitle = "Zero black emissions & dynamic palettes",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Crafted for audiophiles and cinema connoisseurs.",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom Close Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Close",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showUpdateDialog && updateReleaseInfo != null) {
        AppUpdateDialog(
            releaseInfo = updateReleaseInfo!!,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun SpecTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10121A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF8E92A0),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun DeveloperSupportCard(
    onOpenFullDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isUpiCopied by remember { mutableStateOf(false) }

    fun copyUpiId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Developer UPI ID", DEVELOPER_UPI_ID)
        clipboard.setPrimaryClip(clip)
        isUpiCopied = true
        Toast.makeText(context, "UPI ID copied: $DEVELOPER_UPI_ID", Toast.LENGTH_SHORT).show()
    }

    fun openUpiPayment() {
        try {
            val uri = Uri.parse("upi://pay?pa=$DEVELOPER_UPI_ID&pn=$DEVELOPER_NAME&tn=Support%20Media%20Player%20Development&cu=INR")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyUpiId()
            Toast.makeText(context, "No UPI app detected. UPI ID copied to clipboard!", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10121A)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AudiophileCyan, AudiophileGold)
                                )
                            )
                            .padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C0D14)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = AudiophileCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = DEVELOPER_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Lead Architect & Audio Engineer",
                            fontSize = 11.sp,
                            color = AudiophileCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenFullDialog,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1FFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "About",
                        tint = AudiophileGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0x1FFFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Support Ongoing Development ☕",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AudiophileGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ad-free, open, and built for audiophiles. Support future bit-perfect engine & codec upgrades with a small contribution!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB0B0C0)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { openUpiPayment() },
                    colors = ButtonDefaults.buttonColors(containerColor = AudiophileGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payment,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Donate via UPI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenFullDialog,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AudiophileCyan),
                    border = BorderStroke(1.dp, AudiophileCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
